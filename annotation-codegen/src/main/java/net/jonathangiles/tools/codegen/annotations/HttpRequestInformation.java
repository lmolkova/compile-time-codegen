package net.jonathangiles.tools.codegen.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface HttpRequestInformation {
    HttpMethod method();
    String path() default "";
    String[] requestHeaders() default {};
    int[] expectedStatusCodes() default {};
    Class<?> returnValueWireType() default Void.class;
}
