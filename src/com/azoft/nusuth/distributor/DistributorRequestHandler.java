/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.distributor;

import com.azoft.nusuth.distributor.connectionfactory.*;
import com.azoft.nusuth.management.ContainerCountInfo;
import com.azoft.nusuth.management.ApplicationInfo;
import com.azoft.nusuth.server.*;
import com.azoft.nusuth.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

import com.azoft.nusuth.core.*;
import org.apache.log4j.Category;

/**
 *This class is used to handle requests and responses i.e.  works like proxy between client and container.
 * Uses the instance of the @see ContainerConnectionFactory class to get reference to the corresponding container.
 * Uses the instance of the @see DistributorRequestAdapter class to work with the request, response streams
 * between container and client.
 * @author VDGG (vdgg@azoft.com)
 * @modifyed by IgorK (igork@novosoft.ru)
 * @version 1.15
 * @since 1.0
 */
public class DistributorRequestHandler
        extends TcpConnectionHandler {

    private DistributorRequestAdapter adapter = null;
    private static ContainerConnectionFactory connectionFactory;
    private static Class adapterClass = null;
    protected Category logger = Category.getInstance(this.getClass());
    protected LogCategoryProxy logProxy = LogCategoryProxy.getInstance(this.getClass().getName());
    private static Hashtable containerRequestCount = new Hashtable();
    private StrBuffer containerId = new StrBuffer();
    private StrBuffer sessionId = new StrBuffer();
    private Socket containerSocket = null;
    private static int failedRequests = 0;
    private static Object failedRequestsLock = new Object();
    private boolean keepaliveRequested = false;
    private StrBuffer prevContainerId = new StrBuffer();
    private StrBuffer prevSessionId = new StrBuffer();
    private Socket prevContainerSocket = null;
    private StrBuffer first = new StrBuffer();
    private StrBuffer second = new StrBuffer();
    private static int startedRequest = 0;
    private static java.lang.Object startedRequestLock = new Object();
    private static int sucessfulRequests = 0;
    private static java.lang.Object sucessfulRequestsLock = new Object();


    public DistributorRequestHandler() {

        super();

        try {
            this.adapter = (DistributorRequestAdapter) adapterClass.newInstance();
        } catch (Throwable e) {
            logger.debug("Can't create adapter", e);

            throw new RuntimeException("Can't create adapter. See log for details.");
        }
    }


    /**
     * Insert the method's description here. Creation date: (11.01.01 1:01:41)
     * @param container com.azoft.nusuth.util.StrBuffer
     */
    private static void decContainerCount(StrBuffer container) {
        IncrementableInt counter = (IncrementableInt) containerRequestCount.get(container);
        if (counter == null)
            containerRequestCount.put(container.cloneBuf(), counter = new IncrementableInt(0));

        synchronized (counter) {
            counter.dec();
        }
    }


    /**
     *This method processes the request and response with the help of the request adapter (@see
     * com.azoft.distributor.DistributorRequestAdapter) It finds the corresponding container and then works like proxy
     * between client and container. Supports keep alive connection.
     * If any error occurs while working (some errors occurs while adapter's work , can't find corresponding container) then it
     * simply interrupts the work.
     */
    public void execute() {
        try {
            adapter.setClientSocket(socket);
            int protocol = (server instanceof NusuthSslServer) ? ApplicationInfo.HTTPS_PROTOCOL : ApplicationInfo.HTTP_PROTOCOL;

            do {
                if (logProxy.isDebugEnabled())
                    incStartedRequest();
                adapter.parseRequest();

                if (!keepaliveRequested && adapter.isClientKeepAlive()) {
                    keepaliveRequested = this.requestKeepAlive();
                }
                adapter.setClientKeepAlive(keepaliveRequested);
/*
        adapter.getContainerID(containerId);
        if (containerId.length() == 0 || !prevContainerId.equals(containerId)) {
          connectionFactory.returnSocket(prevContainerId, prevContainerSocket);
          prevContainerSocket = null;
          prevContainerId.clear();

          if (containerId.length() > 0)
            containerSocket = connectionFactory.createConnectionByContainerID(containerId, protocol);
          else
            containerSocket = connectionFactory.createConnectionByURI(adapter.getRequestURI(), containerId, protocol);

          if (containerSocket == null)
            throw new DistributorException("Container not found", null, DistributorRequestAdapter.ERROR_NotFound);

          adapter.setContainerSocket(containerSocket);
          prevContainerId.append(containerId);
          prevContainerSocket = containerSocket;
        }
*/

                adapter.getSessionID(sessionId);
                if (sessionId.length() == 0) {
                    connectionFactory.returnSocket(prevContainerId, prevContainerSocket);
                    prevContainerSocket = null;
                    prevContainerId.clear();
                    prevSessionId.clear();

                    containerSocket = connectionFactory.createConnectionByURI(adapter.getRequestURI(), containerId, protocol);
                    if (containerSocket == null) {
                        connectionFactory.clearRequestCache();
                        connectionFactory.onContainerDown(containerId);
                        containerId.clear();
                        containerSocket = connectionFactory.createConnectionByURI(adapter.getRequestURI(), containerId, protocol);
                        if (containerSocket == null)
                            throw new DistributorException("Container not found", null, DistributorRequestAdapter.ERROR_NotFound);
                    }
                    adapter.setContainerSocket(containerSocket);
                    prevContainerId.append(containerId);
                    prevContainerSocket = containerSocket;
                } else if (!sessionId.equals(prevSessionId)) {
                    connectionFactory.returnSocket(prevContainerId, prevContainerSocket);
                    prevContainerSocket = null;
                    prevContainerId.clear();
                    prevSessionId.clear();
                    first.clear();
                    second.clear();
                    first = sessionId.subbuffer(sessionId.indexOf('$') + 1, sessionId.lastIndexOf('$'));
                    second = sessionId.subbuffer(sessionId.lastIndexOf('$') + 1, sessionId.lastIndexOf('_'));
                    containerSocket = connectionFactory.createConnectionByContainerID(first, protocol);
                    containerId.clear();
                    containerId.append(first);
                    if (containerSocket == null) {
                        connectionFactory.clearRequestCache();
                        connectionFactory.onContainerDown(first);
                        if (second.length() != 0)
                            containerSocket = connectionFactory.createConnectionByContainerID(second, protocol);
                        else
                            containerSocket = connectionFactory.createConnectionByURI(adapter.getRequestURI(), containerId, protocol);
                        if (containerSocket == null)
                            throw new DistributorException("Container not found", null, DistributorRequestAdapter.ERROR_NotFound);
                        if (second.length() != 0) {
                            containerId.clear();
                            containerId.append(second);
                        }
                    }
                    adapter.setContainerSocket(containerSocket);
                    prevContainerId.append(containerId);
                    prevContainerSocket = containerSocket;
                    prevSessionId.append(sessionId);
                }

                incContainerCount(containerId);
                try {
                    adapter.process();
                } catch (DistributorException ex) {
                    if (ex.getErrorCode() == DistributorRequestAdapter.ERROR_BadContainerSocket) {
                        connectionFactory.clearRequestCache();
                        if (second.length() != 0) {
                            connectionFactory.onContainerDown(first);
                            containerSocket = connectionFactory.createConnectionByContainerID(second, protocol);
                            containerId.clear();
                            containerId.append(second);
                        } else {
                            connectionFactory.onContainerDown(containerId);
                            containerId.clear();
                            containerSocket = connectionFactory.createConnectionByURI(adapter.getRequestURI(), containerId, protocol);
                        }
                        adapter.setContainerSocket(containerSocket);
                        prevContainerId.append(containerId);
                        prevContainerSocket = containerSocket;
                        incContainerCount(containerId);
                        adapter.process();
                    } else {
                        throw ex;
                    }
                } finally {
                    decContainerCount(containerId);
                }

                adapter.partiallyCleanup();
                if (logProxy.isDebugEnabled())
                    incSucessfulRequests();

            } while (adapter.isClientKeepAlive());
            connectionFactory.returnSocket(containerId, containerSocket);
        } catch (DistributorException dex) {
            if (logProxy.isDebugEnabled())
                incFailedRequests();
            if (dex.getErrorCode() != DistributorRequestAdapter.ERROR_BadContainerSocket && dex.getErrorCode() != DistributorRequestAdapter.ERROR_BadResponse)
                connectionFactory.returnSocket(containerId, containerSocket);
            if (logProxy.isDebugEnabled())
                logger.debug("DistributorException ", dex);
            adapter.processError(dex.getErrorCode(), dex.toString());
        } catch (Throwable t) {
            if (logProxy.isDebugEnabled())
                incFailedRequests();
            logger.error("Exception:", t);
            if (logProxy.isDebugEnabled())
                adapter.processError(DistributorRequestAdapter.ERROR_UnknownError, "Unknown internal server error: " + t.getMessage());
            else
                adapter.processError(DistributorRequestAdapter.ERROR_UnknownError, "Unknown internal server error");
        } finally {
            if (keepaliveRequested) {
                freeKeepAlive();
                keepaliveRequested = false;
            }

            if (logProxy.isDebugEnabled())
                logger.debug("Requests: " + getSucessfulRequests() + '+' + getFailedRequests() + '/' + getStartedRequest());
            adapter.cleanup();

            containerId.clear();
            containerSocket = null;
            prevContainerId.clear();
            prevSessionId.clear();
            prevContainerSocket = null;
            keepaliveRequested = false;
        }
    }


    /**
     * Insert the method's description here. Creation date: (11.01.01 14:58:58)
     * @return java.util.Hashtable
     */
    public static Vector getContainerRequestCount() {

        Vector result = new Vector();

        for (Enumeration i = containerRequestCount.keys(); i.hasMoreElements();) {
            StrBuffer key = (StrBuffer) i.nextElement();
            IncrementableInt value = (IncrementableInt) containerRequestCount.get(key);
            ContainerCountInfo counter = new ContainerCountInfo();

            counter.setContainer(key.toString());
            counter.setSubject("Total");
            synchronized (value) {
                counter.setCount(value.getValue());
            }
            result.add(counter);
        }

        return result;
    }


    /**
     * Insert the method's description here.
     * Creation date: (04.02.2001 23:00:23)
     * @return int
     */
    public static int getFailedRequests() {
        synchronized (failedRequestsLock) {
            return failedRequests;
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (03.02.2001 20:54:09)
     * @return int
     */
    public static int getStartedRequest() {
        synchronized (startedRequestLock) {
            return startedRequest;
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (03.02.2001 20:57:01)
     * @return int
     */
    public static int getSucessfulRequests() {
        synchronized (sucessfulRequestsLock) {
            return sucessfulRequests;
        }
    }


    /**
     * Insert the method's description here. Creation date: (11.01.01 1:01:41)
     * @param container com.azoft.nusuth.util.StrBuffer
     */
    private static void incContainerCount(StrBuffer container) {
        IncrementableInt counter = (IncrementableInt) containerRequestCount.get(container);
        if (counter == null)
            containerRequestCount.put(container.cloneBuf(), counter = new IncrementableInt(0));

        synchronized (counter) {
            counter.inc();
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (04.02.2001 23:00:23)
     * @return int
     */
    private static void incFailedRequests() {
        synchronized (failedRequestsLock) {
            failedRequests++;
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (03.02.2001 20:54:09)
     * @param newStartedRequest int
     */
    private static void incStartedRequest() {
        synchronized (startedRequestLock) {
            startedRequest++;
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (03.02.2001 20:57:01)
     * @param newSucessfulRequests int
     */
    private static void incSucessfulRequests() {
        synchronized (sucessfulRequestsLock) {
            sucessfulRequests++;
        }
    }


    /**
     * Insert the method's description here. Creation date: (11.01.01 1:01:41)
     * @param container com.azoft.nusuth.util.StrBuffer
     */
    public static void resetContainerCount() {
        containerRequestCount = new Hashtable();
    }


    /**
     * Sets the reference to the adapter class. Must be called before any instance of this class will be created
     * Creation date: (14.11.00 18:44:21)
     */
    public static void setAdapterClass(Class newAdapterClass)
            throws ClassCastException {
        if (DistributorRequestAdapter.class.isAssignableFrom(newAdapterClass)) {
            adapterClass = newAdapterClass;
        } else {
            throw new ClassCastException("Wrong distributor request adapter class");
        }
    }


    public static void setConnectionFactory(ContainerConnectionFactory newConnectionFactory) {
        connectionFactory = newConnectionFactory;
    }


    protected void writeDeny() {
        adapter.processError(403, "You do not have permission to access this server");
    }

    /**
     * Inits this handler to work with specified TCP server. If
     * <code>server</code> is SSL server, that adapter initializes as secure, and
     * not if not.
     * @param server
     * @see #adapter
     * @see DistributorRequestAdapter
     */
    protected void init(NusuthTcpServer server) {
        super.init(server);
        if (server instanceof NusuthSslServer) {
            adapter.setSecure(true);
        } else {
            adapter.setSecure(false);
        }
    }
}
