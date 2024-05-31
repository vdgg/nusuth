/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.container;

import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;

import com.azoft.nusuth.deployment.*;

public class CheckIfContainerRunning {

    public static void main(String[] args) {

        try {
            String configPath = args.length == 0 ? null : args[args.length - 1];
            if (System.getProperty("nusuth.home") == null) {
                throw new Exception("System property nusuth.home not specified");
            }
            File jbirdHome = new File(System.getProperty("nusuth.home"));
            if (!jbirdHome.exists() || !jbirdHome.isDirectory()) {
                throw new Exception("Cannot find directory " + System.getProperty("nusuth.home"));
            }
            if (configPath == null) {
                File confFile = new File(jbirdHome, "admin" + File.separator + "container.xml");
                if (!confFile.exists()) {
                    throw new Exception("Cannot find config");
                }
                configPath = confFile.getPath();
            }

            ContainerEntityResolver resolver = new ContainerEntityResolver();
            NusuthAppConfigFactory.addEntityResolver("container", resolver);
            CompositeNusuthWebAppElement conf = NusuthAppConfigFactory.createConfig("container", configPath);
            CompositeNusuthWebAppElement tcpServer = (CompositeNusuthWebAppElement) conf.getCompositeChild("tcp-server").nextElement();
            String portStr = ((SimpleNusuthWebAppElement) tcpServer.getSimpleChild("port").nextElement()).getContent();
            int port = Integer.parseInt(portStr);
            Socket client = null;
            try {
                System.out.print("Ping...");
                client = new Socket("localhost", port);
                OutputStream stream = client.getOutputStream();
                stream.write(("GET / HTTP/1.1\r\nHost: localhost:" + port + "\r\nConnection: close\r\n\r\n").getBytes());
                stream.flush();
                InputStream is = client.getInputStream();
                StringBuffer in = new StringBuffer();
                try {
                    int b;
                    while ((b = is.read()) != -1) {
                        in.append((char) b);
                    }
                } catch (Exception ex) {
                }
                if (in.toString().startsWith("HTTP/1.1")) {
                    System.out.println(" pong!");
                } else {
                    System.out.println(" bad response!");
                    System.exit(-1);
                }
            } catch (Exception ex) {
                System.out.println("Cannot ping container, nested: " + ex);
                System.exit(-1);
            } finally {
                if (client != null) {
                    try {
                        client.close();
                    } catch (Exception ex) {
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
