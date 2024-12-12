package net.jonathangiles.clientlibrary;

import com.azure.core.http.HttpPipeline;
import com.azure.core.util.Context;
import com.azure.core.util.tracing.InstrumentationScope;
import net.jonathangiles.clientlibrary.implementation.GeneratedServiceAPI;

import java.util.List;

// This is the (commonly hand-written, but not always) convenience layer that the end user will interact with.
public class ClientLibrary {

    private final HttpPipeline pipeline = null;
    // NOTE: Normally these properties would be set by the user in a builder, but for now
    // they are simply hard coded here.
    // FIXME empty pipeline is not so useful here...
    private final GeneratedServiceAPI serviceAPI = GeneratedServiceAPI.getInstance(pipeline);

    private final String endpoint = "foo";

    public List<String> getKeys(String name) {
        InstrumentationScope scope = pipeline.getInstrumentation().startScope("getKeys", Context.NONE);
        try {
            return serviceAPI.getKeys(endpoint, name, "vals", "pathValueHere", "bar", Context.NONE);
        } catch (RuntimeException e) {
            scope.setError(e);
            throw e;
        } finally {
            scope.close();
        }
    }
}
