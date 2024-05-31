package com.azoft.nusuth.server;

import java.net.ServerSocket;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import javax.net.*;
import javax.net.ssl.*;

import com.sun.net.ssl.*;
import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

/**
 * This class represents SSL server.
 * @author skilz, vdgg, igork
 * @version 1.10
 * @since Nusuth1.0
 */
public class NusuthSslServer extends NusuthTcpServer {

    private String keystoreLocation = null;
    private String keystorePassword = null;
    private String trustedstoreLocation = null;
    private String trustedstorePassword = null;

    /**
     * Constructor for SSL Server.
     * @param settings SSL Server settings.
     * @param connHandlerClass Connetion handler class.
     */
    public NusuthSslServer(CompositeNusuthWebAppElement settings, Class connHandlerClass) throws DeploymentException {
        super(settings, connHandlerClass);
        serverName = "SSL";
        Enumeration enum = settings.getCompositeChild("keystore");
        if (!enum.hasMoreElements()) {
            throw new DeploymentException("Element keystore not found in ssl-server");
        }
        CompositeNusuthWebAppElement keystore = (CompositeNusuthWebAppElement) enum.nextElement();
        enum = keystore.getSimpleChild("location");
        if (!enum.hasMoreElements()) {
            throw new DeploymentException("Element location not found in keystore");
        }
        keystoreLocation = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim();
        enum = keystore.getSimpleChild("password");
        if (!enum.hasMoreElements()) {
            throw new DeploymentException("Element password not found in keystore");
        }
        keystorePassword = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim();

        // taking trustedstore
        boolean isAuthNeeded = settings.getCompositeChild("trustedstore").hasMoreElements();
        if (isAuthNeeded) {
            enum = settings.getCompositeChild("trustedstore");
            CompositeNusuthWebAppElement trustedstoreElem = (CompositeNusuthWebAppElement) enum.nextElement();
            enum = trustedstoreElem.getSimpleChild("location");
            if (!enum.hasMoreElements()) {
                throw new DeploymentException("Element location not found in trustedstore");
            }
            trustedstoreLocation = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim();
            enum = trustedstoreElem.getSimpleChild("password");
            if (!enum.hasMoreElements()) {
                throw new DeploymentException("Element password not found in trustedstore");
            }
            trustedstorePassword = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim();
        }
    }

    /**
     * Initialize all defined server sockets and start threads for each socket.
     */
    public void startServerSockets() {
        SSLContext context = null;

        try {
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            char[] pass = keystorePassword.toCharArray();
            context = SSLContext.getInstance("SSL");
            KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
            KeyStore store = KeyStore.getInstance("JKS");
            store.load(new java.io.FileInputStream(keystoreLocation), pass);
            factory.init(store, pass);

            if (trustedstoreLocation != null && trustedstorePassword != null) {
                KeyStore trustStore = KeyStore.getInstance("jks");
                trustStore.load(new java.io.FileInputStream(trustedstoreLocation),
                        trustedstorePassword.toCharArray());
                TrustManagerFactory trustManagerFactory
                        = TrustManagerFactory.getInstance("SunX509");
                trustManagerFactory.init(trustStore);
                context.init(factory.getKeyManagers(),
                        trustManagerFactory.getTrustManagers(), null);
            } else {
                context.init(factory.getKeyManagers(), null, null);
            }
        } catch (Exception e) {
            cat.error("Cannot start server", e);
            throw new RuntimeException("Server fatal error: cannot "
                    + "start server, see log for details");
        }

        //context.init(factory.getKeyManagers(), null, null);
        SSLServerSocketFactory ssf = context.getServerSocketFactory();

/*
    for (int i=0; i<allHosts.size(); i++) {
      String host = (String)allHosts.get(i);
      ServerSocket serverSocket = null;
      try {
        if (!host.equals("*")) {
//          serverSocket = new ServerSocket(port, 50,
//                                          InetAddress.getByName(host));
          serverSocket = ssf.createServerSocket(port, 50,
                                                InetAddress.getByName(host));
        } else {
          serverSocket = ssf.createServerSocket(port);
        }
        if (trustedstoreLocation != null && trustedstorePassword != null) {
            ((SSLServerSocket) serverSocket).setNeedClientAuth(true);
        }
        (new NusuthSocketHandler(this, serverSocket)).start();
      } catch (IOException e) {
        cat.error("Cannot start server", e);
        throw new RuntimeException("Server fatal error: cannot "
                                   +"start server, see log for details");
      }
    }
*/


        ServerSocket serverSocket = null;
        try {
            if (allHosts.contains("*") && !startedHandlers.contains("*")) {
                if (startedHandlers.size() > 0) {
                    for (int i = 0; i < startedHandlers.size(); i++) {
                        InetAddress addr = (InetAddress) startedHandlers.get(i);
                        NusuthSocketHandler handler
                                = (NusuthSocketHandler) ip2handler.get(addr.getHostAddress());
                        handler.shutdown();
                    }
                    startedHandlers.clear();
                }
//        serverSocket = new ServerSocket(port);
                serverSocket = ssf.createServerSocket(port);
                NusuthSocketHandler handler
                        = new NusuthSocketHandler(this, serverSocket);
                handler.start();
                startedHandlers.add("*");
                ip2handler.put("*", handler);
                return;
            } else if (!allHosts.contains("*")) {
                for (int i = 0; i < allHosts.size(); i++) {
                    String host = (String) allHosts.get(i);
                    InetAddress addr = InetAddress.getByName(host);
                    String ip = addr.getHostAddress();
                    if (startedHandlers.contains(addr)) {
                        List list = (List) ip2listOfHosts.get(ip);
                        if (!list.contains(host)) {
                            list.add(host);
                        }
                    } else {
//            serverSocket = new ServerSocket(port, 50,
//                                            InetAddress.getByName(host));
                        serverSocket = ssf.createServerSocket(port, 50,
                                InetAddress.getByName(host));
                        NusuthSocketHandler handler
                                = new NusuthSocketHandler(this, serverSocket);
                        ip2handler.put(ip, handler);
                        handler.start();
                        startedHandlers.add(addr);
                        List list = new ArrayList();
                        list.add(host);
                        ip2listOfHosts.put(ip, list);
                    }
                }
            }
        } catch (IOException e) {
            cat.error("Cannot start server socket", e);
            throw new RuntimeException("Server fatal error: cannot "
                    + "start server, see log for details");
        }




/*
    serverSocket = ssf.createServerSocket(port);

    if (trustedstoreLocation != null && trustedstorePassword != null) {
        ((SSLServerSocket) serverSocket).setNeedClientAuth(true);
    }
*/
    }

    public void applySettings(CompositeNusuthWebAppElement settings) throws DeploymentException {
        if (isRestartNeeded(settings)) {
            throw new IllegalArgumentException("Cannot apply settings - restart needed");
        }
        super.applySettings(settings);
    }


    public boolean isRestartNeeded(CompositeNusuthWebAppElement settings) throws DeploymentException {
        Enumeration enum = settings.getCompositeChild("keystore");
        if (!enum.hasMoreElements()) {
            throw new DeploymentException("Cannot find keystore in ssl server settings");
        }
        CompositeNusuthWebAppElement keystore = (CompositeNusuthWebAppElement) enum.nextElement();
        enum = keystore.getSimpleChild("location");
        if (!enum.hasMoreElements()) {
            throw new DeploymentException("Cannot find keystore location in keystore in ssl server settings");
        }
        String newKeystoreLocation = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim();
        enum = keystore.getSimpleChild("password");
        if (!enum.hasMoreElements()) {
            throw new DeploymentException("Cannot find keystore password in keystore in ssl server settings");
        }
        String newKeystorePassword = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim();
        return !newKeystoreLocation.equals(keystoreLocation) || !newKeystorePassword.equals(keystorePassword) ||
                super.isRestartNeeded(settings);
    }
}
