/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.server;

import java.net.Socket;
import java.net.SocketException;

public abstract class TcpConnectionHandler implements Runnable {

    protected Socket socket;
    protected NusuthTcpServer server;
    private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("com.azoft.nusuth.server");

    public TcpConnectionHandler() {
    }


    public void cleanup() {

        try {
            socket.close();
        } catch (Throwable t) {

            //Logger.log(t, 0);
            cat.error("Cannot close socket", t);
        } finally {
            socket = null;
        }
    }

    public abstract void execute();

    protected void freeKeepAlive() {
        server.freeKeepAlive();
    }

    protected void init(NusuthTcpServer server) {
        this.server = server;
    }

    protected boolean requestKeepAlive() {
        return server.requestKeepAlive();
    }


    public void run() {
        if (server == null) {
            cat.error("TcpConnectionHandler not inited");

            throw new IllegalStateException("TcpConnectionHandler not inited");
        }

        try {

            //System.out.println("Handler "+name+" waiting...");
            //      lastAccess = System.currentTimeMillis();
            while ((socket = server.getSocket()) != null) {
                //System.out.println("Handler "+name+" executing...");
                //System.out.println(socket.getClass().getName());
                if (server.isAllowed(socket)) {
                    setSoTimeout();
                    server.incActive();
                    execute();
                    server.decActive();
                } else {
                    writeDeny();
                }

                cleanup();

                //        lastAccess = System.currentTimeMillis();
                //System.out.println("Handler "+name+" waiting...");
            }
        } catch (Throwable t) {

            //Logger.log(t, 1);
            cat.error("Server runtime error", t);
        } finally {
            if (socket != null) {
                cleanup();
            }

            server.stopConnectionHandler();
        }
    }

    protected void setSoTimeout() throws SocketException {
        socket.setSoTimeout(server.getSoTimeout());
    }

    protected abstract void writeDeny();
}