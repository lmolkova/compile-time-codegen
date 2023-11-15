package net.jonathangiles.tools.codegen;

import net.jonathangiles.tools.codegen.annotations.*;
import net.jonathangiles.tools.codegen.models.HttpRequestMethod;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("net.jonathangiles.tools.codegen.annotations.ServiceInterface")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationProcessor extends AbstractProcessor {
    // a map of fully-qualified class names to their short names
    private final Map<String, String> imports = new TreeMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // We iterate through each interface annotated with @ServiceInterface separately.
        // This outer for-loop is not strictly necessary, as we only have one annotation that we care about
        // (@ServiceInterface), but we'll leave it here for now
        for (TypeElement serviceInterfaceAnnotation : annotations) {
            // Now we get all elements annotated with @ServiceInterface
            Set<? extends Element> serviceInterfaceInterfaces =
                    roundEnv.getElementsAnnotatedWith(serviceInterfaceAnnotation);

            // set up some lists for capturing the important information we seek
            List<ExecutableElement> httpRequestMethods = null;
            String serviceInterfaceFQN = "";

            // iterate through all methods in the service interface, looking for appropriate sub-annotations
            for (Element serviceInterface : serviceInterfaceInterfaces) {
                // check if the annotated element is an interface, because that is what we expect
                if (serviceInterface.getKind().isInterface()) {
                    serviceInterfaceFQN = serviceInterface.asType().toString();

                    // iterate through all methods in the interface, collecting all that are @HttpRequestInformation
                    // and then generating a method implementation for each one
                    httpRequestMethods = serviceInterface.getEnclosedElements().stream()
                            .filter(element -> element.getKind() == ElementKind.METHOD)
                            .filter(element -> element.getAnnotation(HttpRequestInformation.class) != null)
                            .map(ExecutableElement.class::cast)
                            .collect(Collectors.toList());
                }
            }

            // we have gather the useful information, now perform some checks, and then generated the necessary code
            if (httpRequestMethods == null || httpRequestMethods.isEmpty()) {
                continue;
            }

            writeServiceInterfaceImplementationFile(serviceInterfaceFQN, httpRequestMethods);
        }

        return true;
    }

    private void writeServiceInterfaceImplementationFile(String serviceInterfaceFQN,
                                                         List<ExecutableElement> httpRequestMethods) {
        addImport("com.azure.core.util.Context");
        addImport("com.azure.core.http.HttpHeaders");
        addImport("com.azure.core.http.HttpPipeline");
        addImport("com.azure.core.http.HttpMethod");
        addImport("com.azure.core.http.HttpResponse");
        addImport("com.azure.core.http.HttpRequest");
        addImport("java.util.Map");
        addImport("java.util.HashMap");

        try {
            String packageName = null;
            int lastDot = serviceInterfaceFQN.lastIndexOf('.');
            if (lastDot > 0) {
                packageName = serviceInterfaceFQN.substring(0, lastDot);
            }

            String serviceInterfaceShortName = serviceInterfaceFQN.substring(lastDot + 1);
            String serviceInterfaceImplFQN = serviceInterfaceFQN + "Impl";
            String serviceInterfaceImplShortName = serviceInterfaceImplFQN.substring(lastDot + 1);

            System.out.println("Creating source file: " + serviceInterfaceImplFQN);
            JavaFileObject serviceInterfaceImplOutputFile = processingEnv.getFiler().createSourceFile(serviceInterfaceImplFQN);

            try (PrintWriter out = new PrintWriter(serviceInterfaceImplOutputFile.openWriter())) {
                // iterate over all HTTP request methods, generating a method implementation for each one,
                // as well as a separate implementation that supports a custom HttpPipeline
                List<HttpRequestMethod> methods = new ArrayList<>();
                httpRequestMethods.forEach(requestMethod -> {
                    HttpRequestMethod method = new HttpRequestMethod();
                    method.setMethodName(requestMethod.getSimpleName().toString());
                    method.setEndpoint(requestMethod.getAnnotation(HttpRequestInformation.class).path());
                    method.setHttpMethod(requestMethod.getAnnotation(HttpRequestInformation.class).method());

                    addImport(requestMethod.getReturnType());
                    method.setMethodReturnType(requestMethod.getReturnType().toString());
                    requestMethod.getParameters().forEach(param -> {
                        // check if the parameter has an annotation, and if so, add it to the appropriate list
                        if (param.getAnnotation(HostParam.class) != null) {
                            // TODO
                            //method.setEndpoint(param.getSimpleName().toString());
                        } else if (param.getAnnotation(HeaderParam.class) != null) {
                            method.addHeader(param.getAnnotation(HeaderParam.class).value(), param.getSimpleName().toString());
                        } else if (param.getAnnotation(QueryParam.class) != null) {
                            method.addQueryParam(param.getAnnotation(QueryParam.class).value(), param.getSimpleName().toString());
                        } else if (param.getAnnotation(BodyParam.class) != null) {
                            // This is the content type as specified in the @BodyParam annotation
                            String contentType = param.getAnnotation(BodyParam.class).value();

                            // This is the type of the parameter that has been annotated with @BodyParam.
                            // This is used to determine which setBody method to call on HttpRequest.
                            String parameterType = param.asType().toString();

                            // This is the parameter name, so we can refer to it when setting the body on the HttpRequest.
                            String parameterName = param.getSimpleName().toString();

                            method.setBody(new HttpRequestMethod.Body(contentType, parameterType, parameterName));
                        }

                        String shortImportName = addImport(param.asType());
                        method.addParameter(new HttpRequestMethod.MethodParameter(shortImportName, param.getSimpleName().toString()));
                    });
                    methods.add(method);
                });

                Properties props = new Properties();
                URL url = this.getClass().getClassLoader().getResource("velocity.properties");
                props.load(url.openStream());

                VelocityEngine ve = new VelocityEngine(props);
                ve.init();

                VelocityContext vc = new VelocityContext();

                vc.put("imports", imports.keySet());
                vc.put("packageName", packageName);
                vc.put("serviceInterfaceShortName", serviceInterfaceShortName);
                vc.put("serviceInterfaceImplShortName", serviceInterfaceImplShortName);
                vc.put("methods", methods);

                Template vt = ve.getTemplate("serviceInterfaceImpl.vm");

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "Applying velocity template: " + vt.getName());

                vt.merge(vc, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String toShortName(String fqcn) {
        int lastDot = fqcn.lastIndexOf('.');
        if (lastDot > 0) {
            return fqcn.substring(lastDot + 1);
        }
        return fqcn;
    }

    // returns the short name of the class
    private String addImport(String importFQN) {
        if (importFQN != null && !importFQN.isEmpty()) {
            String shortName = toShortName(importFQN);
            imports.put(importFQN, shortName);
            return shortName;
        }
        return null;
    }

    // returns the short name of the class
    private String addImport(TypeMirror type) {
        String longName = type.toString();
        String shortName = null;

        if (type.getKind().isPrimitive()) {
            shortName = toShortName(longName);
            imports.put(longName, shortName);
        } else if (imports.containsKey(type.toString())) {
            shortName = imports.get(longName);
        } else if (type.getKind() == TypeKind.DECLARED) {
            // now we need to check if this type is a generic type, and if it is, we need to recursively check
            // the type arguments
            TypeElement typeElement = (TypeElement) ((DeclaredType) type).asElement();
            List<? extends TypeMirror> typeArguments = ((DeclaredType) type).getTypeArguments();
            if (typeArguments != null && !typeArguments.isEmpty()) {
                longName = typeElement.getQualifiedName().toString();
                shortName = toShortName(typeElement.getQualifiedName().toString());
                imports.put(longName, shortName);
            } else {
                shortName = toShortName(longName);
                imports.put(longName, shortName);
            }
        }

        return shortName;
    }
}
