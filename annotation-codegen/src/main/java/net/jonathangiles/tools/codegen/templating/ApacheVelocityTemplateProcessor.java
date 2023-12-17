package net.jonathangiles.tools.codegen.templating;

import net.jonathangiles.tools.codegen.models.TemplateInput;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Properties;

public class ApacheVelocityTemplateProcessor implements TemplateProcessor {
    @Override
    public void process(TemplateInput templateInput, ProcessingEnvironment processingEnv) {
        try {
            String serviceInterfaceImplFQN = templateInput.getServiceInterfaceFQN() + "Impl";

            System.out.println("Creating source file: " + serviceInterfaceImplFQN);
            JavaFileObject serviceInterfaceImplOutputFile = processingEnv.getFiler().createSourceFile(serviceInterfaceImplFQN);

            try (PrintWriter out = new PrintWriter(serviceInterfaceImplOutputFile.openWriter())) {
                Properties props = new Properties();
                URL url = this.getClass().getClassLoader().getResource("velocity.properties");
                props.load(url.openStream());

                VelocityEngine ve = new VelocityEngine(props);
                ve.init();

                VelocityContext vc = new VelocityContext();
                vc.put("imports", templateInput.getImports().keySet());
                vc.put("packageName", templateInput.getPackageName());
                vc.put("serviceInterfaceShortName", templateInput.getServiceInterfaceShortName());
                vc.put("serviceInterfaceImplShortName", templateInput.getServiceInterfaceImplShortName());
                vc.put("methods", templateInput.getHttpRequestContexts());

                Template vt = ve.getTemplate("serviceInterfaceImpl.vm");

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                        "Applying velocity template: " + vt.getName());

                // take the output from the template and format it
//                StringWriter writer = new StringWriter();
                vt.merge(vc, out);

//                System.out.println("Formatting source file: " + serviceInterfaceImplFQN);
//
//                String source = writer.toString();
//                Formatter formatter = new Formatter();
//                String formattedSource = formatter.formatSource(source);
//
//                try (PrintWriter out2 = new PrintWriter(serviceInterfaceImplOutputFile.openWriter())) {
//                    out2.write(formattedSource);
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
