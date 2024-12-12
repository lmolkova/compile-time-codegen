package net.jonathangiles.tools.codegen.templating;

import com.squareup.javapoet.*;
import net.jonathangiles.tools.codegen.models.HttpRequestContext;
import net.jonathangiles.tools.codegen.models.TemplateInput;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JavaPoetTemplateProcessor implements TemplateProcessor {
    private ClassName INTERFACE_TYPE;
    private final ClassName HTTP_PIPELINE = ClassName.get("com.azure.core.http", "HttpPipeline");
    private final ClassName HTTP_REQUEST = ClassName.get("com.azure.core.http", "HttpRequest");
    private final ClassName HTTP_RESPONSE = ClassName.get("com.azure.core.http", "HttpResponse");
    private final ClassName HTTP_METHOD = ClassName.get("com.azure.core.http", "HttpMethod");
    private final ClassName CONTEXT = ClassName.get("com.azure.core.util", "Context");
    private final ClassName INSTRUMENTATION_SCOPE = ClassName.get("com.azure.core.util.tracing", "InstrumentationScope");

    private TypeSpec.Builder classBuilder;

    @Override
    public void process(TemplateInput templateInput, ProcessingEnvironment processingEnv) {
        String serviceInterfaceImplFQN = templateInput.getServiceInterfaceFQN() + "Impl";
        String packageName = templateInput.getPackageName();
        String serviceInterfaceImplShortName = templateInput.getServiceInterfaceImplShortName();
        String serviceInterfaceShortName = templateInput.getServiceInterfaceShortName();

        INTERFACE_TYPE = ClassName.get(packageName, serviceInterfaceShortName);

        // Create the INSTANCE_MAP field
        TypeName mapType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                HTTP_PIPELINE,
                INTERFACE_TYPE
        );
        FieldSpec instanceMap = FieldSpec.builder(mapType, "INSTANCE_MAP", Modifier.PRIVATE, Modifier.STATIC)
                .initializer("new $T<>()", HashMap.class)
                .build();

        // Create the defaultPipeline field
        FieldSpec defaultPipeline = FieldSpec.builder(HTTP_PIPELINE, "defaultPipeline", Modifier.PRIVATE, Modifier.FINAL)
                .build();

        // Create the getInstance method
        MethodSpec getInstance = MethodSpec.methodBuilder("getInstance")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(packageName, serviceInterfaceShortName))
                .addParameter(HTTP_PIPELINE, "defaultPipeline")
                .addStatement("return INSTANCE_MAP.computeIfAbsent(defaultPipeline, pipeline -> new $N(defaultPipeline))", serviceInterfaceImplShortName)
                .build();

        // Create the constructor
        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
                .addParameter(HTTP_PIPELINE, "defaultPipeline")
                .addStatement("this.defaultPipeline = defaultPipeline")
                .build();

        classBuilder = TypeSpec.classBuilder(serviceInterfaceImplShortName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(INTERFACE_TYPE)
                .addField(instanceMap)
                .addField(defaultPipeline)
                .addMethod(getInstance)
                .addMethod(constructor);

        for (HttpRequestContext method : templateInput.getHttpRequestContexts()) {
            generateForwardingMethod(method);
            generateMethod(method);
        }

        TypeSpec typeSpec = classBuilder.build();

        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
                .indent("    ") // four spaces
                .build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateForwardingMethod(HttpRequestContext method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getMethodName())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("", method.getMethodReturnType()));

        // add method parameters, with Context at the end
        for (HttpRequestContext.MethodParameter parameter : method.getParameters()) {
            methodBuilder.addParameter(TypeName.get(parameter.getTypeMirror()), parameter.getName());
        }

        // add call to the overloaded version of this method, passing in the default http pipeline
        String params = method.getParameters().stream().map(HttpRequestContext.MethodParameter::getName).reduce((a, b) -> a + ", " + b).orElse("");
        if (!"void".equals(method.getMethodReturnType())) {
            methodBuilder.addStatement("return $L(defaultPipeline, $L)", method.getMethodName(), params);
        } else {
            methodBuilder.addStatement("$L(defaultPipeline, $L)", method.getMethodName(), params);
        }

        classBuilder.addMethod(methodBuilder.build());
    }

    private void generateMethod(HttpRequestContext method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get("", method.getMethodReturnType()));

        // add method parameters, as well as the HttpPipeline at the front
        methodBuilder.addParameter(HTTP_PIPELINE, "pipeline");
        for (HttpRequestContext.MethodParameter parameter : method.getParameters()) {
            methodBuilder.addParameter(TypeName.get(parameter.getTypeMirror()), parameter.getName());
        }

        methodBuilder
                .addStatement("$T scope = defaultPipeline.getInstrumentation().startScope(\"$L\", context)", INSTRUMENTATION_SCOPE, method.getMethodName())
                .beginControlFlow("try");

        methodBuilder
                .addStatement("String host = $L", method.getHost())
                .addCode("\n")
                .addStatement("// create the request")
                .addStatement("$T httpRequest = new $T($T.$L, host)", HTTP_REQUEST, HTTP_REQUEST, HTTP_METHOD, method.getHttpMethod());

        // add headers
        if (!method.getHeaders().isEmpty()) {
            methodBuilder
                    .addCode("\n")
                    .addStatement("// set the headers")
                    .addStatement("$T headers = new $T()", ClassName.get("com.azure.core.http", "HttpHeaders"), ClassName.get("com.azure.core.http", "HttpHeaders"));
            for (Map.Entry<String, String> header : method.getHeaders().entrySet()) {
                methodBuilder.addStatement("headers.add($S, $S)", header.getKey(), header.getValue());
            }
            methodBuilder.addStatement("httpRequest.setHeaders(headers)");
        }

        // set the body
        if (method.getBody() != null) {
            methodBuilder
                    .addCode("\n")
                    .addStatement("// set the body")
                    .addStatement("httpRequest.setBody($L)", method.getBody().getParameterName());
        }

        // send request through pipeline
        methodBuilder
                .addCode("\n")
                .addStatement("// send the request through the pipeline")
                .addStatement("$T response = pipeline.sendSync(httpRequest, context)", HTTP_RESPONSE);

        // check for expected status codes
        if (!method.getExpectedStatusCodes().isEmpty()) {
            methodBuilder
                    .addCode("\n")
                    .addStatement("final int responseCode = response.getStatusCode()");
            if (method.getExpectedStatusCodes().size() == 1) {
                methodBuilder.addStatement("boolean expectedResponse = responseCode == $L", method.getExpectedStatusCodes().get(0));
            } else {
                methodBuilder.addStatement("boolean expectedResponse = $T.binarySearch(new int[] {$L}, responseCode) > -1", Arrays.class, method.getExpectedStatusCodes().toString());
            }
            methodBuilder.beginControlFlow("if (!expectedResponse)")
                    .addStatement("throw new $T(\"Unexpected response code: \" + responseCode)", RuntimeException.class)
                    .endControlFlow();
        }

        // add return statement if method return type is not "void"
        if (!"void".equals(method.getMethodReturnType())) {
            methodBuilder.addStatement("return $T.asList(\"Hello\", \"World!\")", Arrays.class);
        }

        methodBuilder
                .nextControlFlow("catch ($T e)", RuntimeException.class)
                .addStatement("scope.setError(e)")
                .addStatement("throw e")
                .nextControlFlow("finally")
                .addStatement("scope.close()")
                .endControlFlow();

        classBuilder.addMethod(methodBuilder.build());
    }
}
