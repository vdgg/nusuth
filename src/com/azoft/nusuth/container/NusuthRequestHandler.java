package com.azoft.nusuth.container;

import com.azoft.nusuth.container.http.*;
import com.azoft.nusuth.core.*;
import com.azoft.nusuth.util.*;
import com.azoft.nusuth.server.*;

import java.net.*;
import java.util.*;
import java.io.OutputStream;

import com.azoft.nusuth.management.ContainerManager;
import com.azoft.nusuth.session.SessionManager;
import com.azoft.nusuth.session.NusuthSession;

/**
 *This class is responsible for passing request for the corresponding context
 * where it will be processed. Uses sub classes of the @see ProtocolAdapter
 * class to parse request and to get it's URL.
 * @author vdgg, skilz
 * @version 1.28
 * @since Nusuth1.0
 */
public class NusuthRequestHandler extends TcpConnectionHandler {
    private static Class adapterClass;
    private static Hashtable appContext;
    private static InvocationCache cache = new InvocationCache();
    private static ContainerManager container;
    private static String secretKey = "";
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
    public static boolean sslEnabled = false;
    protected static HashSet httpContexts;
    protected static HashSet httpsContexts;
    protected static Hashtable contextName2setOfSessions = new Hashtable();
    protected static Hashtable contextName2oldContexts = new Hashtable();

    /**
     *Constructor.
     * @param socket client socket if there is no distributor or distributor's
     * socket if distributor is present.
     */
    public NusuthRequestHandler() {
        super();
        try {
            adapter = (ProtocolAdapter) adapterClass.newInstance();
        } catch (Exception ex) {
            cat.error("FATAL: Cannot instantiate ProtocolAdapter, nested: " + ex);
            throw new RuntimeException("Cannot instantiate ProtocolAdapter, nested: "
                    + ex);
        }
    }

    protected void init(NusuthTcpServer server) {
        super.init(server);
        if (server instanceof NusuthSslServer) {
            adapter.setSecure(true);
        } else {
            adapter.setSecure(false);
        }
    }

    public static void clearCache() {
        cache.clear();
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
                idBuf.append(adapter.getHostWithUri());
                if (!adapter.isGood()) {
                    throw new Exception("Bad request");
                }
                boolean old = false;
                NusuthSession session = null;
                urlBuf = adapter.getSecretKeyCandidate();
                if (urlBuf.equals(secretKey)) {
                    try {
                        String rhost = socket.getInetAddress().getHostAddress();
                        cat.info("Attempt of shutdown from host " + rhost);
                        if (adminIP.contains(rhost)) {
                            shutdownContexts();
                            adapter.processError(ProtocolAdapter.ERROR_OK);
                            adapter.close();
                            container.fullShutdown();
                        } else {
                            cat.warn("Attention!!! Attempt of unauthorized access from host "
                                    + rhost);
                        }
                    } catch (Exception ex1) {
                        cat.info("Cannot shutdown", ex1);
                    }
                }
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
                    synchronized (contextName2setOfSessions) {
                        if (contextName2setOfSessions.size() != 0) {
                            String hostWithContextPath = adapter.getHost()
                                    + (contextPath.length() == 0
                                    ? "/" : contextPath.toString());
                            if (contextName2setOfSessions.containsKey(hostWithContextPath)) {
                                Set set = (Set) contextName2setOfSessions.
                                        get(hostWithContextPath);
                                Iterator iter = set.iterator();
                                while (iter.hasNext()) {
                                    NusuthSession ses = (NusuthSession) iter.next();
                                    if (ses.getId().equals(
                                            adapter.getRequest().getRequestedSessionId())) {
                                        old = true;
                                        session = ses;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    cacheElement = adapter.setContext(context, contextPath);
                    if (old) {
                        adapter.setContext((NusuthContext) session.getServletContext(),
                                contextPath);
                    }
//          cacheElement = adapter.setContext(context, contextPath);
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

                    synchronized (contextName2setOfSessions) {
                        if (contextName2setOfSessions.size() != 0) {
                            String hostWithContextPath
                                    = adapter.getHost()
                                    + (cacheElement.contextPath.length() == 0
                                    ? "/"
                                    : cacheElement.contextPath.toString());
                            if (contextName2setOfSessions.containsKey(hostWithContextPath)) {
                                Set set = (Set) contextName2setOfSessions.
                                        get(hostWithContextPath);
                                Iterator iter = set.iterator();
                                while (iter.hasNext()) {
                                    NusuthSession ses = (NusuthSession) iter.next();
                                    if (ses.getId().equals(
                                            adapter.getRequest().getRequestedSessionId())) {
                                        old = true;
                                        session = ses;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    context = cacheElement.context;
                    if (old) {
                        adapter.setContext((NusuthContext) session.getServletContext(),
                                cacheElement.contextPath.toString());
                    } else {
                        adapter.setContext(cacheElement);
                    }
//          context = cacheElement.context;
//          adapter.setContext(cacheElement);
                    if (!adapter.isGood()) {
                        throw new Exception("Bad request");
                    }
                }
                if (adapter.isKeepAliveAllowed() && !adapter.isKeepAlive()) {
                    freeKeepAlive();
                }
                if (!sslEnabled ||
                        (httpContexts.contains(idBuf)
                        && !(server instanceof NusuthSslServer))
                        || (httpsContexts.contains(idBuf)
                        && (server instanceof NusuthSslServer))) {
                    if (!old) {
                        context.processRequest(adapter);
                    } else {
                        ((NusuthContext) session.getServletContext()).processRequest(adapter);
                    }
                } else {
                    if (sslEnabled) {
                        if ((server instanceof NusuthSslServer)
                                && (!httpsContexts.contains(idBuf)
                                && httpContexts.contains(idBuf))) {
                            adapter.sendRedirect("http", ProtocolAdapter.getPort());
                        } else {
                            if (!(server instanceof NusuthSslServer)
                                    && (httpsContexts.contains(idBuf)
                                    && !httpContexts.contains(idBuf))) {
                                adapter.sendRedirect("https", ProtocolAdapter.getSSLPort());
                            }
                        }
                    }
                }
                if (old) {
                    cacheElement.context = context;
                }
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

    /**
     * This method replace context with given contextName (hostId+contextPath) by
     * given context. Old context will be used for requests which sessions were
     * created it that context.
     * @param contextName Context name (hostId+contextPath).
     * @param context New context.
     */
    public static void startNewContext(String contextName,
                                       NusuthContext context) {
        NusuthContext oldContext = (NusuthContext) appContext.get(contextName);
        synchronized (contextName2setOfSessions) {
            if (contextName2setOfSessions.get(contextName) == null) {
                Set setOfSessions = new HashSet();
                SessionManager sm = oldContext.getSessionManager();
                Enumeration enum = sm.getSessionsKeys();
                while (enum.hasMoreElements()) {
                    setOfSessions.add(sm.getSession((String) enum.nextElement()));
                }
                contextName2setOfSessions.put(contextName, setOfSessions);
            } else {
                Set setOfSessions
                        = (HashSet) contextName2setOfSessions.get(contextName);
                SessionManager sm = oldContext.getSessionManager();
                Enumeration enum = sm.getSessionsKeys();
                while (enum.hasMoreElements()) {
                    setOfSessions.add(sm.getSession((String) enum.nextElement()));
                }
            }
        }
        clearCache();
        appContext.put(contextName, context);
        synchronized (contextName2oldContexts) {
            if (contextName2oldContexts.get(contextName) != null) {
                Set set = (Set) contextName2oldContexts.get(contextName);
                set.add(oldContext);
            } else {
                Set set = new HashSet();
                set.add(oldContext);
                contextName2oldContexts.put(contextName, set);
            }
        }
    }

    public static void startNewContextInternal(String contextName, NusuthContext context) {
        clearCache();
        appContext.put(contextName, context);
    }

    /**
     * This method remove expired session from hashtable what contains
     * old context to old sessions mapping.
     * @param session Session to remove.
     */
    public static void removeEpiredSession(NusuthSession session) {
        synchronized (contextName2setOfSessions) {
            Enumeration keys = contextName2setOfSessions.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                Set set = (Set) contextName2setOfSessions.get(key);
                if (set.contains(session)) {
                    set.remove(session);
                    return;
                }
            }
        }
    }

    /**
     * This method check if old context with given name has some sessions or not.
     * If it has then do nothing else shutdown this context.
     * @param contextName Name of context.
     */
    public static void shutDownOldContexts(String contextName) {
        synchronized (contextName2oldContexts) {
            if (contextName2oldContexts.get(contextName) != null) {
                boolean down = false;
                Set set = (Set) contextName2oldContexts.get(contextName);
                NusuthContext context = null;
                Iterator iter = (set).iterator();
                while (iter.hasNext()) {
                    context = (NusuthContext) iter.next();
                    if (!context.getSessionManager().getSessionsKeys().hasMoreElements()) {
                        context.shutdownContext(true);
                        down = true;
                        break;
                    }
                }
                if (context != null && down) {
                    set.remove(context);
                }
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

    public static void setSecretKey(String key) {
        secretKey = key;
    }

    private static void shutdownAll() {
        shutdownContexts();
        container.fullShutdown();
    }

    public static void setAdminIP(List aip) {
        adminIP = aip;
    }

    public static void setHTTPContexts(HashSet cont) {
        httpContexts = cont;
    }

    public static void setHTTPSContexts(HashSet cont) {
        httpsContexts = cont;
    }

    protected void writeDeny() {
        adapter.writeDeny();
    }

    protected void setSoTimeout() throws SocketException {
        if (ProtocolAdapter.isStandAlone()) {
            super.setSoTimeout();
        }
    }

    public static HashSet getHttpContexts() {
        return httpContexts;
    }

    public static HashSet getHttpsContexts() {
        return httpsContexts;
    }

    public static Hashtable getAppContext() {
        return appContext;
    }
}
