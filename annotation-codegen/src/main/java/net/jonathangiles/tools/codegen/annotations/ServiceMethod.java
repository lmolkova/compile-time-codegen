package net.jonathangiles.tools.codegen.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to give the service interfaces a name that correlates to the service that is usable in a programmatic
 * way.
 */
@Retention(CLASS)
@Target(METHOD)
public @interface ServiceMethod {
    /**
     * Name of the service - this must be short and without spaces.
     *
     * @return the service name given to the interface.
     */
    String name();
}