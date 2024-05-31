package com.azoft.nusuth.container;

import com.azoft.nusuth.server.TcpConnectionHandler;
import com.azoft.nusuth.server.NusuthTcpServer;
import com.azoft.nusuth.server.NusuthSslServer;
import com.azoft.nusuth.management.ContainerManager;
import com.azoft.nusuth.core.NusuthContext;
import com.azoft.nusuth.util.StrBuffer;
import com.azoft.nusuth.session.NusuthSession;
import com.azoft.nusuth.session.SessionManager;

import java.util.*;
import java.net.SocketException;

/**
 * This class represents connection handler for admin purposes.
 * @author skilz
 * @version 1.1
 * @since Nusuth1.0
 */
public class NusuthAdminRequestHandler extends TcpConnectionHandler {
    private static Class adapterClass;
    private static Hashtable appContext;
    private static InvocationCache cache = new InvocationCache();
    private static ContainerManager container;
    private static List adminIP = new ArrayList();
    private static char[] SLASH_CHARACTER = {'/'};
    private ProtocolAdapter adapter;
    private String contextPath;
    private ContainerInvocationCacheElement cacheElement;
    private NusuthContext context;
    private StrBuffer urlBuf;
    private StrBuffer idBuf = new StrBuffer();
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth."
            + "container");

    /**
     * Constructor.
     */
    public NusuthAdminRequestHandler() {
        super();
        try {
            adapter = (ProtocolAdapter) adapterClass.newInstance();
        } catch (Exception ex) {
            cat.error("FATAL: Cannot instantiate ProtocolAdapter, nested: " + ex);
            throw new RuntimeException("Cannot instantiate ProtocolAdapter, nested: "
                    + ex);
        }
    }

    /**
     * Initialize handler.
     * @param server Tcp server.
     */
    protected void init(NusuthTcpServer server) {
        super.init(server);
        adapter.setSecure(false);
    }

    /**
     *This method processes the request (or requests if it is keep alive
     * connection) with the use of the protocol adapter. It searches for the
     * required context defined by the request url and passes request to this
     * context.
     * @exception Exception if the bad request is
     * given to the @see ProtocolAdapter.
     */
    public void execute() {
        try {
            //Logger.log("Start handler", -1);
            cat.debug("Start request handler");
            adapter.setSocket(socket);
            if (!adapter.isGood()) {
                throw new Exception("Bad request");
            }
            //If it is keep alive connection than this cycle will parse all requests
            //in the bounds of this connection.
            //for each request it will search for the needed context depending
            //on the requests URL and then process request.
            do {
                adapter.parse();
                idBuf.clear();
                idBuf.append(adapter.getRequest().getRequestURI());
                if (!adapter.isGood()) {
                    throw new Exception("Bad request");
                }
                boolean old = false;
                NusuthSession session = null;
                cacheElement = (ContainerInvocationCacheElement) cache.find(idBuf);
                if (!adapter.isKeepAliveAllowed() && requestKeepAlive()) {
                    adapter.allowKeepAlive();
                }
                if (cacheElement == null) {
                    Object sr = null;
                    boolean cont = idBuf.containsChar('/');
                    StrBuffer idurlBuf = idBuf.cloneBuf();
                    do {
                        sr = appContext.get(idBuf.toString());
                        cat.debug("Searching for " + idBuf.toString());
                        if (sr == null) {
                            if (cont = idBuf.containsChar('/')) {
                                idBuf.cutToChar('/', true);
                            } else {
                                sr = appContext.get(idBuf.toString() + "/");
                                contextPath = "";
                            }
                        } else {
                            contextPath = idBuf.cutToEnd(SLASH_CHARACTER, true);
                            if (contextPath.length() == 1) {
                                contextPath = "";
                            }
                        }
                    } while (sr == null && cont);
                    if (sr == null) {
                        //there is no context associated with the requests URL
                        //and there is no default context so the error
                        //response is processed.
                        //Logger.log("Context not found", 1);
                        cat.debug("Context not found");
                        adapter.processError(ProtocolAdapter.ERROR_SERVLET_NOT_FOUND);
                        if (adapter.isStandAlone()) {
                            throw new Exception("Context not found");
                        } else {
                            adapter.partialCleanup();
                            continue;
                        }
                    }
                    //Sets the context defined by the requests URL to the ProtocolAdapter.
                    context = (NusuthContext) sr;
                    cacheElement = adapter.setContext(context, contextPath);
                    idBuf.append(cacheElement.contextPath);
                    if (!adapter.isGood()) {
                        throw new Exception("Bad request");
                    }
                    cache.add(idurlBuf, cacheElement);
                    //System.out.println("Start processing");
                    //Calls method that passes request to the corresponding
                    //web application.
                } else {
                    idBuf.cutToFirstChar('/', false);
                    idBuf.append(cacheElement.contextPath);
                    context = cacheElement.context;
                    adapter.setContext(cacheElement);
                    if (!adapter.isGood()) {
                        throw new Exception("Bad request");
                    }
                }
                if (adapter.isKeepAliveAllowed() && !adapter.isKeepAlive()) {
                    freeKeepAlive();
                }
                context.processRequest(adapter);
                if (adapter.isKeepAlive()) {
                    adapter.partialCleanup();
                }
            } while (adapter.isKeepAlive() || !adapter.isStandAlone());
            if (adapter.isKeepAlive()) {
                freeKeepAlive();
            }
        } catch (Exception ex) {
            cat.debug("Request processing error", ex);
            if (adapter.isKeepAliveAllowed()) {
                freeKeepAlive();
            }
        }
    }

    public void cleanup() {
        adapter.cleanup();
        super.cleanup();
        cacheElement = null;
        context = null;
//??????????????????????????????????????????
        contextPath = null;
//??????????????????????????????????????????
    }

    /**
     * @param className the protocol adapter
     * adapter class name (See @see ProtocolAdapter).
     * @exception ClassNotFoundException if there is no class with such a name.
     */
    public static void setAdapterClass(String className) throws
            ClassNotFoundException {
        adapterClass = Class.forName(className);
    }

    /** @param context the jndi dir context that contains web application
     * Contexts bound up with the url names. */
    public static void setApplicationContext(Hashtable context) {
        appContext = context;
    }

    /**
     * This method shutown admin context.
     */
    private static void shutdownContexts() {
        org.apache.log4j.Category cat
                = org.apache.log4j.Category.getInstance("com.azoft.nusuth."
                + "container");
        cat.info("Shutdown contexts...");
        Enumeration enum = appContext.keys();
        NusuthContext context;
        while (enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            context = (NusuthContext) appContext.get(name);
            try {
                context.shutdownContext(false);
            } catch (Exception ex) {
                cat.error("Cannot shutdown context " + name, ex);
            }
        }
    }

    public static void setContainer(ContainerManager cont) {
        container = cont;
    }

    private static void shutdownAll() {
        shutdownContexts();
        container.fullShutdown();
    }

    public static void setAdminIP(List aip) {
        adminIP = aip;
    }

    protected void writeDeny() {
        adapter.writeDeny();
    }

    protected void setSoTimeout() throws SocketException {
        if (ProtocolAdapter.isStandAlone()) {
            super.setSoTimeout();
        }
    }

    public static Hashtable getAppContext() {
        return appContext;
    }
}
