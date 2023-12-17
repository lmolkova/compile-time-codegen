package net.jonathangiles.clientlibrary;

import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.util.Context;
import net.jonathangiles.clientlibrary.implementation.GeneratedServiceAPI;

import java.util.List;

// This is the (commonly hand-written, but not always) convenience layer that the end user will interact with.
public class ClientLibrary {

    // NOTE: Normally these properties would be set by the user in a builder, but for now
    // they are simply hard coded here.
    // FIXME empty pipeline is not so useful here...
    private final GeneratedServiceAPI serviceAPI = GeneratedServiceAPI.getInstance(null);

    private final String endpoint = "foo";

    public List<String> getKeys(String name) {
        return serviceAPI.getKeys(endpoint, name, "vals", "pathValueHere", "bar", Context.NONE);
    }
}
