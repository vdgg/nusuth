package com.azoft.nusuth.distributor;

import com.azoft.nusuth.distributor.cache.Cache;
import com.azoft.nusuth.util.LogCategoryProxy;
import com.azoft.nusuth.util.StrBuffer;

import org.apache.log4j.Category;

import java.io.*;
import java.net.Socket;

/**
 * This abstract class represents methods for process client requests.
 *
 * @author VDGG
 * @author IgorK
 * @author Skillz
 * @version 1.11
 * @see HttpDistributorRequestAdapter
 * @see DistributorRequestHandler
 * @since JBird 1.0
 */
public abstract class DistributorRequestAdapter {
    final static int OK = 0;
    final static int ERROR_BadClientSocket = 1;
    final static int ERROR_BadContainerSocket = 2;
    final static int ERROR_BadRequest = 3;
    final static int ERROR_BadResponse = 4;
    final static int ERROR_NotFound = 5;
    final static int ERROR_OnRead = 6;
    final static int ERROR_OnWrite = 7;
    final static int ERROR_UnknownError = 255;

    protected InputStream clientIS = null;
    protected OutputStream clientOS = null;
    protected Socket clientSocket = null;

    protected InputStream containerIS = null;
    protected OutputStream containerOS = null;
    protected Socket containerSocket = null;

    protected StrBuffer containerID = new StrBuffer();
    protected StrBuffer sessionID = new StrBuffer();
    protected StrBuffer requestURI = new StrBuffer(256);
    protected String clientAddress = "";
    protected boolean clientKeepAlive = false;
    protected Category logger = Category.getInstance("com.azoft.nusuth.distributor");
    protected LogCategoryProxy logProxy = LogCategoryProxy.getInstance("com.azoft.nusuth.distributor");
    private boolean secure = false;

    protected static StrBuffer distributorPort = new StrBuffer();

    protected static Cache cache;

    static {
        distributorPort.append(":80");
    }


    public static void setCache(Cache newCache) {
        cache = newCache;
    }

    public static Cache getCache() {
        return cache;
    }

    public void cleanup() {
        clientIS = null;
        clientOS = null;
        clientSocket = null;

        containerIS = null;
        containerOS = null;
        containerSocket = null;

        containerID.clear();
        sessionID.clear();
        requestURI.clear();
        clientAddress = "";
        clientKeepAlive = false;
    }


    public void getContainerID(StrBuffer containerID) {
        containerID.clear();
        containerID.append(this.containerID);
    }

    public void getSessionID(StrBuffer sessionID) {
        sessionID.clear();
        sessionID.append(this.sessionID);
    }


    /**
     * Insert the method's description here.
     * Creation date: (03.02.2001 19:05:53)
     * @return int
     */
    public static StrBuffer getDistributorPort() {
        synchronized (distributorPort) {
            return distributorPort;
        }
    }


    public StrBuffer getRequestURI() {
        return requestURI;
    }


    public boolean isClientKeepAlive() {
        return clientKeepAlive;
    }


    public abstract void parseRequest() throws DistributorException;


    public abstract void partiallyCleanup();


    public abstract void process() throws DistributorException;


    /**
     *This method creates and sends the error response to the client with the error message that depends on
     * the given error code value.
     * @param sc the error code.
     */
    public abstract void processError(int errorCode, String message);


    public void setClientKeepAlive(boolean clientKeepAlive) {
        if (logProxy.isDebugEnabled())
            logger.debug("Set client keepalive " + clientKeepAlive);
        this.clientKeepAlive = clientKeepAlive;
    }


    public void setClientSocket(Socket newClientSocket) throws DistributorException {
        try {
            clientSocket = newClientSocket;
            clientIS = clientSocket.getInputStream();
            clientOS = clientSocket.getOutputStream();
            clientAddress = clientSocket.getInetAddress().getHostAddress();
        } catch (InterruptedIOException inex) {
            throw new DistributorException("Bad client socket (timeout)", inex, ERROR_BadClientSocket);
        } catch (IOException ioex) {
            throw new DistributorException("Bad client socket", ioex, ERROR_BadClientSocket);
        } catch (NullPointerException npex) {
            throw new DistributorException("Client socket is null", null, ERROR_BadClientSocket);
        }
    }


    public void setContainerSocket(Socket newContainerSocket) throws DistributorException {
        try {
            containerSocket = newContainerSocket;
            containerIS = containerSocket.getInputStream();
            containerOS = containerSocket.getOutputStream();
        } catch (InterruptedIOException inex) {
            throw new DistributorException("Bad container socket (timeout)", inex, ERROR_BadContainerSocket);
        } catch (IOException ioex) {
            throw new DistributorException("Bad container socket", ioex, ERROR_BadContainerSocket);
        } catch (NullPointerException npex) {
            throw new DistributorException("Container socket is null", null, ERROR_BadContainerSocket);
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (03.02.2001 19:05:53)
     * @param newDistributorPort int
     */
    public static void setDistributorPort(int newDistributorPort) {
        synchronized (distributorPort) {
            distributorPort.clear();
            distributorPort.append(":" + newDistributorPort);
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (03.02.2001 19:05:53)
     * @param newDistributorPort int
     */
    public static void setDistributorPort(StrBuffer newDistributorPort) {
        synchronized (distributorPort) {
            distributorPort = newDistributorPort;
        }
    }

    /**
     * Sets that this adapter works under SSL server.
     * @param isSecure true if it works under SSL server, false if not
     */
    protected void setSecure(boolean isSecure) {
        secure = isSecure;
    }

    /**
     * @return true, if this adapter works under SSL server.
     */
    protected boolean isSecure() {
        return secure;
    }
}
