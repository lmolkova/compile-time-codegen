package net.jonathangiles.tools.codegen.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(PARAMETER)
public @interface HeaderParam {
    /**
     * The name of the variable in the endpoint uri template which will be replaced with the value of the parameter
     * annotated with this annotation.
     *
     * @return The name of the variable in the endpoint uri template which will be replaced with the value of the
     * parameter annotated with this annotation.
     */
    String value();
}