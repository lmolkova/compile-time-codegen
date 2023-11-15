package net.jonathangiles.tools.codegen.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to annotate a parameter to send to a REST endpoint as HTTP Request content.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface BodyParam {
    /**
     * Gets the Content-Type for the body.
     *
     * @return The Content-Type for the body.
     */
    String value();
}