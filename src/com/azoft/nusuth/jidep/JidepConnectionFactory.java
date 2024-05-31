package com.azoft.nusuth.jidep;

import java.util.Hashtable;
import java.net.Socket;
import java.io.IOException;

/**
 * Connection factory for JIDEP protocol
 * @author skilz
 * @version 1.2
 * @since Nusuth1.0
 */
public class JidepConnectionFactory {

    private static String authKey = null;
    private static Hashtable pool = new Hashtable();

    /**
     * Sets the authentication key
     * @param key Authentication key
     */
    public static void setKey(String key) {
        authKey = key;
    }

    /**
     * Return authentication key
     * @return Authentication key
     */
    public static String getKey() {
        return authKey;
    }

    /**
     * Create new connection or get it from pool
     * @param host Host
     * @param port Port
     * @return ClientJidepAdapter Clients part of adapter
     */
    public static ClientJidepAdapter getClientAdapter(String host, int port)
            throws IOException {
        synchronized (pool) {
            if (!pool.containsKey(host + ":" + port)) {
                return JidepProtocolAdapter.getClientSide(new Socket(host, port),
                        authKey);
            } else {
                return (ClientJidepAdapter) pool.remove(host + ":" + port);
            }
        }
    }

    /**
     * Return given adapter to pool
     * @param host Host
     * @param port Port
     * @param adapter Clients part of adapter
     */
    public static void returnClientAdapter(String host, int port,
                                           ClientJidepAdapter adapter) {
        synchronized (pool) {
            if (!pool.containsKey(host + ":" + port)) {
                pool.put(host + ":" + port, adapter);
            }
        }
    }

}
