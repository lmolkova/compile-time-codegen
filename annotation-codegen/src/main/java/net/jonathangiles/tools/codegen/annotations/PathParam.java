package net.jonathangiles.tools.codegen.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to annotate replacement for a named path segment in REST endpoint URL.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface PathParam {
    /**
     * The name of the variable in the endpoint uri template which will be replaced with the value
     * of the parameter annotated with this annotation.
     * @return The name of the variable in the endpoint uri template which will be replaced with the
     *     value of the parameter annotated with this annotation.
     */
    String value();
    /**
     * A value true for this argument indicates that value of {@link PathParam#value()} is already encoded
     * hence engine should not encode it, by default value will be encoded.
     * @return Whether or not this path parameter is already encoded.
     */
    boolean encoded() default false;
}