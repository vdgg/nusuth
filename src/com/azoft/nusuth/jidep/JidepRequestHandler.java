package com.azoft.nusuth.jidep;

import com.azoft.nusuth.management.security.AdminPortListener;
import com.azoft.nusuth.management.ComponentManager;
import com.azoft.nusuth.management.ContainerManager;
import com.azoft.nusuth.core.LocalContainer;
import com.azoft.nusuth.util.RsaUtil;

import java.io.*;
import java.net.SocketException;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * This class intended for handlig and processing JIDEP requests.
 * @author skilz
 * @version 1.6
 * @since Nusuth1.0
 */
public class JidepRequestHandler extends Thread {

    private ServerJidepAdapter adapter;
    private ComponentManager manager;
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jidep");
    private boolean authenticated = false;
    private String authenticationKey = null;
    private AdminPortListener listener;
    private RsaUtil rsa = new RsaUtil();
    private HashMap commandHandlers = new HashMap();

    public JidepRequestHandler(ServerJidepAdapter adapter,
                               ComponentManager manager, String key,
                               AdminPortListener listener) {
        init(adapter, manager, key, listener);
    }

    private void init(ServerJidepAdapter adapter, ComponentManager manager,
                      String key, AdminPortListener listener) {
        this.adapter = adapter;
        this.manager = manager;
        this.authenticationKey = key;
        this.listener = listener;
        SessionCommandsHandler sesHandler = new SessionCommandsHandler(manager);
        ListenerCommandsHandler lisHandler = new ListenerCommandsHandler(listener);
        JndiCommandHandler jndiHandler = new JndiCommandHandler(manager);
        LoginCommandHandler loginHandler = new LoginCommandHandler(manager);
        SubscribeCommandHandler subHandler = new SubscribeCommandHandler(manager);
        commandHandlers.put("containers", sesHandler);
        commandHandlers.put("update", sesHandler);
        commandHandlers.put("remove", sesHandler);
        commandHandlers.put("down", sesHandler);
        commandHandlers.put("accept", lisHandler);
        commandHandlers.put("jndioperation", jndiHandler);
        commandHandlers.put("login", loginHandler);
        commandHandlers.put("subscribe", subHandler);
    }

    public void run() {
        authenticate();
        if (authenticated) {
            do {
                try {
                    adapter.parseRequest();
                    String command = adapter.getCommand();
                    CommandHandler handler = (CommandHandler) commandHandlers.get(command);
                    if (handler != null) {
                        handler.service(adapter);
                    } else {
                        adapter.setStatus(404);
                        adapter.endResponse();
                        adapter.cleanup();
                    }
                } catch (SocketException e) {
                    cat.debug("Client closed socket", e);
                    break;
                } catch (Exception e) {
                    cat.error("Request processing error", e);
                    try {
                        adapter.setStatus(500);
                        ObjectOutputStream oos =
                                new ObjectOutputStream(adapter.getOutputStream());
                        oos.reset();
                        oos.writeObject(e.getMessage());
                        adapter.endResponse();
                        adapter.cleanup();
                    } catch (IOException ex) {
                        cat.debug("Cannot send response", ex);
                        break;
                    }
                    continue;
                }
            } while (true);
        }
    }

    /**
     * This method authenticate client. If authentication passed successfully then
     * response with 200 (OK) code returns to the client, if authentication
     * failure then response with 404 (Not authenticated) code returns to the
     * client.
     */
    private void authenticate() {
        try {
            adapter.parseRequest();
            String command = adapter.getCommand();
            if (!command.equals("getpublickey")) {
                adapter.setStatus(404);
                adapter.endResponse();
                adapter.cleanup();
                return;
            } else {
                OutputStream os = adapter.getOutputStream();
                os.write(rsa.e.toByteArray().length);
                os.write(rsa.e.toByteArray());
                os.write(rsa.n.toByteArray().length);
                os.write(rsa.n.toByteArray());
                adapter.endResponse();
                adapter.cleanup();
            }
            adapter.parseRequest();
            command = adapter.getCommand();
            if (!command.equals("authenticate")) {
                adapter.setStatus(404);
                adapter.endResponse();
            } else {
                InputStream istr = adapter.getInputStream();
                BigInteger integer = null;
                int len = istr.read();
                byte[] keyArr = new byte[len];
                int readed = 0;
                while (readed != -1) {
                    readed = istr.read(keyArr, readed, keyArr.length - readed);
                }
                integer = new BigInteger(keyArr);
                String key = new String(rsa.decrypt(integer).toByteArray());
                if (key.equals(authenticationKey)) {
                    adapter.endResponse();
                    authenticated = true;
                } else {
                    adapter.setStatus(404);
                    adapter.endResponse();
                }
            }
        } catch (IOException e) {
            cat.error("Cannot authenticate", e);
            authenticated = false;
        } finally {
            adapter.cleanup();
        }
    }

}