package net.jonathangiles.tools.codegen.templating;

import net.jonathangiles.tools.codegen.models.TemplateInput;

import javax.annotation.processing.ProcessingEnvironment;

public interface TemplateProcessor {
    static TemplateProcessor getInstance() {
//        return new ApacheVelocityTemplateProcessor();
        return new JavaPoetTemplateProcessor();
    }

    void process(TemplateInput templateInput, ProcessingEnvironment processingEnv);
}
