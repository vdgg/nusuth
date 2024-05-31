package com.azoft.nusuth.core;

import com.azoft.nusuth.session.DistributedNusuthSession;
import com.azoft.nusuth.jidep.ClientJidepAdapter;
import com.azoft.nusuth.jidep.JidepProtocolAdapter;

import java.util.Stack;
import java.util.Hashtable;
import java.net.Socket;
import java.net.SocketException;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * This class represent remote container.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.3
 */
public class RemoteContainer {

    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.core");
    private String host;
    private String name;
    private String authKey = null;
    private int adminPort;
    private int port;
    private Hashtable sessionId2adapters = new Hashtable();

    public RemoteContainer(String host, int adminPort, int port, String name) {
        this.host = host;
        this.adminPort = adminPort;
        this.port = port;
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public int getAdminPort() {
        return adminPort;
    }

    public void setLocalAuthKey(String key) {
        authKey = key;
    }

    /**
     * This method send <i>session</i> to the real container and tell it to store
     * given session.
     * @param session Session to stored.
     * @exception Exception Throw if any errors occures during sending session,
     * creating new connection or in other cases.
     */
    public void updateBackupSession(DistributedNusuthSession session)
            throws Exception {
        if (sessionId2adapters.get(session.getId()) != null) {
            Stack adapters = (Stack) sessionId2adapters.get(session.getId());
            synchronized (adapters) {
                ClientJidepAdapter adapter = null;
                if (!adapters.isEmpty()) {
                    adapter = (ClientJidepAdapter) adapters.pop();
                } else {
                    Socket socket = new Socket(host, adminPort);
                    adapter = JidepProtocolAdapter.getClientSide(socket, authKey);
                    adapter.processAuthenticate();

                    if (adapter.getResponseCode() != 200) {
                        cat.debug("Server not authenticate request");
                        return;
                    }
                }
                adapter.setCommand("update");
                session.writeObject(adapter.getOutputStream());
                adapter.endRequest();
                adapter.parseResponse();
                adapters.push(adapter);
            }
        } else {
            Socket socket = new Socket(host, adminPort);
            ClientJidepAdapter adapter = JidepProtocolAdapter.getClientSide(socket,
                    authKey);
            adapter.processAuthenticate();
            if (adapter.getResponseCode() == 200) {
                adapter.setCommand("update");
                session.writeObject(adapter.getOutputStream());
                adapter.endRequest();
                adapter.parseResponse();
                Stack adapters = new Stack();
                adapters.push(adapter);
                sessionId2adapters.put(session.getId(), adapters);
            } else {
                cat.debug("Server not authenticate request");
            }
        }
    }

    /**
     * This method tell to the real container to remove stored session
     * @param sessionId Session id
     * @exception Exception Throws if any errors occeres during sending
     * information, creating new connection or in other cases.
     */
    public void removeBackupSession(String sessionId) throws Exception {
        if (sessionId2adapters.get(sessionId) != null) {
            Stack adapters = (Stack) sessionId2adapters.get(sessionId);
            synchronized (adapters) {
                ClientJidepAdapter adapter = null;
                if (!adapters.isEmpty()) {
                    adapter = (ClientJidepAdapter) adapters.pop();
                } else {
                    Socket socket = new Socket(host, adminPort);
                    adapter = JidepProtocolAdapter.getClientSide(socket, authKey);
                    adapter.processAuthenticate();
                    if (adapter.getResponseCode() != 200) {
                        cat.debug("Server not authenticate request");
                        return;
                    }
                }
                adapter.setCommand("remove");
                (new ObjectOutputStream(adapter.getOutputStream())).
                        writeObject(sessionId);
                adapter.endRequest();
                adapter.parseResponse();
                adapters.push(adapter);
            }
        } else {
            Socket socket = new Socket(host, adminPort);
            ClientJidepAdapter adapter = JidepProtocolAdapter.getClientSide(socket,
                    authKey);
            adapter.processAuthenticate();

            if (adapter.getResponseCode() != 200) {
                cat.debug("Server not authenticate request");
            } else {
                adapter.setCommand("remove");
                (new ObjectOutputStream(adapter.getOutputStream())).
                        writeObject(sessionId);
                adapter.endRequest();
                adapter.parseResponse();
                Stack adapters = new Stack();
                adapters.push(adapter);
                sessionId2adapters.put(sessionId, adapters);
            }
        }
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

}