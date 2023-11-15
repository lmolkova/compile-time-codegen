package net.jonathangiles.enduserapp;

import net.jonathangiles.clientlibrary.ClientLibrary;

import java.util.List;

/**
 * Hello world!
 *
 */
public class ClientApp {
    public static void main( String[] args ) {
        ClientLibrary clientLibrary = new ClientLibrary();
        List<String> secretKeys = clientLibrary.getKeys("secretKeys");
        System.out.println(secretKeys);
    }
}
