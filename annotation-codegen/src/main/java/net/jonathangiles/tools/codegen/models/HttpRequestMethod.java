package net.jonathangiles.tools.codegen.models;

import net.jonathangiles.tools.codegen.annotations.HttpMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpRequestMethod {
    private String methodName;
    private String methodReturnType;
    private final List<MethodParameter> parameters = new ArrayList<>();
    private String endpoint;
    private HttpMethod httpMethod;
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> queryParams = new HashMap<>();
    private Body body;

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodReturnType() {
        return methodReturnType;
    }

    public void setMethodReturnType(String methodReturnType) {
        this.methodReturnType = methodReturnType;
    }

    public String getSimplifiedParameterString(boolean withTypes) {
        return parameters.stream()
            .map(param -> (withTypes ? param.type + " " : "") + param.name)
            .collect(Collectors.joining(", "));
    }

    public void addParameter(MethodParameter parameter) {
        this.parameters.add(parameter);
    }

    public List<MethodParameter> getParameters() {
        return parameters;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void addQueryParam(String key, String value) {
        queryParams.put(key, value);
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public Body getBody() {
        return body;
    }

    public static class MethodParameter {
        private String type;
        private String name;

        public MethodParameter(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    public static class Body {
        // This is the content type as specified in the @BodyParam annotation
        private final String contentType;

        // This is the type of the parameter that has been annotated with @BodyParam.
        // This is used to determine which setBody method to call on HttpRequest.
        private final String parameterType;

        // This is the parameter name, so we can refer to it when setting the body on the HttpRequest.
        private final String parameterName;

        public Body(String contentType, String parameterType, String parameterName) {
            this.contentType = contentType;
            this.parameterType = parameterType;
            this.parameterName = parameterName;
        }

        public String getContentType() {
            return contentType;
        }

        public String getParameterType() {
            return parameterType;
        }

        public String getParameterName() {
            return parameterName;
        }
    }
}
