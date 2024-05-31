package com.azoft.nusuth.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

/**
 * This class represents socket handler. It open server sockect. Then it
 * listen for connection. Then it put accepted socket to tcp server.
 * @author skilz
 * @version 1.2
 * @since Nusuth1.0
 */
public class NusuthSocketHandler extends Thread {

    /**Tcp server*/
    private NusuthTcpServer server = null;
    /**Server socket*/
    private ServerSocket serverSocket = null;
    /**Logger*/
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.server");

    private boolean down = false;

    /**
     * Constructor.
     * @param server Tcp server.
     * @param serverSocket Server Socket.
     */
    public NusuthSocketHandler(NusuthTcpServer server, ServerSocket serverSocket)
            throws UnknownHostException {
        this.server = server;
        this.serverSocket = serverSocket;
    }

    /**
     * Listen for connection, and put accepted socket to tcp server.
     */
    public void run() {
        Socket socket = null;
        while (server.isRunning() && !down) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                cat.error("Cannot accept socket", e);
                socket = null;
            }
            if (socket == null) {
                continue;
            }
            server.addSocketToQueue(socket);
        }
        if (serverSocket != null && !down) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                cat.warn("Cannot close server socket", e);
            }
        }
        if (down) {
            cat.info("Server socket closed");
        }
    }

    public void shutdown() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            cat.warn("Cannot close server socket", e);
        }
        down = true;
    }

}
