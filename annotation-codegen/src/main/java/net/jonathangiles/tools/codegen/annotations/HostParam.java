package net.jonathangiles.tools.codegen.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to annotate replacement of parameterized segments in a dynamic {@link Host}.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface HostParam {
    /**
     * The name of the variable in the endpoint uri template which will be replaced with the value of the parameter
     * annotated with this annotation.
     *
     * @return The name of the variable in the endpoint uri template which will be replaced with the value of the
     * parameter annotated with this annotation.
     */
    String value();

    /**
     * A value true for this argument indicates that value of {@link HostParam#value()} is already encoded hence engine
     * should not encode it, by default value will be encoded.
     *
     * @return Whether this argument is already encoded.
     */
    boolean encoded() default true;
}