package com.azoft.nusuth.core;

import com.azoft.nusuth.container.ProtocolAdapter;
import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.session.*;

import java.io.*;
import java.net.URL;
import java.net.SocketException;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.*;
import javax.servlet.http.*;

import com.azoft.nusuth.session.DistributedNusuthSession;
import com.azoft.nusuth.jsp.*;
import com.azoft.nusuth.jndi.NusuthJNDIContext;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.management.Manageable;
import com.azoft.nusuth.webappsecurity.*;
import com.azoft.nusuth.webappsecurity.impl.WebAppSecurityManagerImpl;
import com.azoft.nusuth.container.http.HttpProtocolAdapter;
import com.azoft.nusuth.container.ContainerInvocationCacheElement;
import com.azoft.nusuth.container.NusuthRequestHandler;
import sun.security.acl.PrincipalImpl;

import java.security.Principal;

import com.azoft.nusuth.util.StrBuffer;
import com.azoft.nusuth.util.Utils;
import org.apache.log4j.*;
import tyrex.naming.EnvContext;
import tyrex.naming.MemoryContext;

/**
 *This class realizes the servlet context. The Servlet Context defines a
 * servlet’s view of the web application with in which the servlet is running.
 * The Servlet Context also allows a servlet to access resources available to
 * it. Using such an object, a servlet can log events, obtain URL references to
 * resources, and set and store attributes that other servlets in the context
 * can use. The Container Provider is responsible for providing an
 * implementation of theServletContext interface in the servlet container.
 * @author vdgg, skilz, igork
 * @version 1.84
 * @since Nusuth1.0
 */
public class NusuthContext extends NusuthJNDIContext
        implements ServletContext, Manageable {

    private final static String dirListingClassName
            = "com.azoft.nusuth.core.DirServlet";

    /**
     * @shapeType AssociationLink
     * @clientCardinality 1
     * @supplierCardinality 0..n
     * @label
     */
    private Hashtable servletConfigs = new Hashtable();

    /**
     * @shapeType AssociationLink
     * @clientCardinality 0..n
     * @label
     */
    private Hashtable servlets = new Hashtable();
    private Hashtable singleThreadServlets = new Hashtable();
    private Hashtable single2instance = new Hashtable();
    private Hashtable servletMappings = new Hashtable();
    private HashSet singleThreadServletNames = new HashSet();

    /**
     * @shapeType AssociationLink
     * @label
     * @supplierRole config
     * @supplierCardinality 1
     */
    private CompositeNusuthWebAppElement config = null;
    private Hashtable attributes = new Hashtable();
    private Hashtable initParameters = new Hashtable();
    private Hashtable init2unav = new Hashtable();
    private Hashtable service2unav = new Hashtable();
    private Hashtable service2ex = new Hashtable();
    private Hashtable mimeTypes = new Hashtable();
    private Hashtable filters = new Hashtable();
    private Hashtable filterMappings = new Hashtable();
    private Hashtable filter2params = new Hashtable();
    private Hashtable filterForDispatchers = new Hashtable();
    private Hashtable filterName2filterElement = new Hashtable();
    private Hashtable servletName2servletElement = new Hashtable();
    private List errorMapings = new LinkedList();
    private int defaultSessionLifetime = 1800;
//  private Hashtable sessions = new Hashtable();
    private ServletLoader loader = new ServletLoader();
    private int currentRequests = 0;
    private boolean closed = false;
    private boolean contextWasChanged = false;
    private boolean distributable = false;
    private boolean shutdown = false;
    private String docBase;
    private String contextName;
    private String notConvertedContextName;
    private String workDir;
    private JspLoader jspLoader;
    private Hashtable taglibs = new Hashtable();
    private static CompositeNusuthWebAppElement commonConfig = null;
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.core");
    private TagLibraryRepository rep = new TagLibraryRepository(this);
    private CustomTagFactory tagFactory;
    private LinkedList contextListeners = new LinkedList();
    private LinkedList contextAttrlisteners = new LinkedList();
    private LinkedList sessionListeners = new LinkedList();
    private LinkedList sessionActListeners = new LinkedList();
    private LinkedList sessionAttrListeners = new LinkedList();
    private LinkedList sessionWorkListeners = new LinkedList();
    private Hashtable allListeners = new Hashtable();
    private String displayName = null;
    private WebAppSecurityManager securityManager;
    private org.apache.log4j.Category logCat = null;
    private int defaultAuthMethod = AuthenticationData.AUTH_METHOD_BASIC;
    private String authRealm;
    private String distributorId;
    private String containerId;
    private StrBuffer formLoginPage;
    private StrBuffer formErrorPage;
    private SessionManager sessionManager;

    private Stack filtersNames = new Stack();
    private List welcomeFilesList = new ArrayList();

    private Context namingContext = null;
    private boolean useJTA = false;

    private long killingTimeout = 0;
    private String sessionBackup = null;
    private boolean reloading = false;
    private Hashtable resource2time = new Hashtable();
    private ContextCompileListener compileListener = null;
    private ContextInternalReloadListener internalReloadListener = null;
    private ContextLazyReloadListener lazyReloadListener = null;
    private String logLocation = null;
    private CompositeNusuthWebAppElement appSettings = null;

    /**
     *This is the constructor method of the NusuthContext class.
     * @param location the platform independent path to the root folder of the
     * web application.
     * @exception DeploymentException  is thrown if method cann't get the full
     * path to the deployment descriptor
     * file or if it can't open it.
     */
    public NusuthContext(String location, String contextName,
                         String workDir, String logLocation,
                         CompositeNusuthWebAppElement appSettings,
                         String sessionBackup)
            throws DeploymentException {
        this.contextName = convertName(contextName);
        this.notConvertedContextName = contextName;
        this.logLocation = logLocation;
        this.appSettings = appSettings;
        logCat = Category.getInstance("application." + this.contextName);
        try {
            logCat.addAppender(
                    new RollingFileAppender(
                            new PatternLayout("[%-5p] [%d{ISO8601}] [%c] [%t] %m%n"),
                            (logLocation.endsWith(File.separator)
                    ? logLocation
                    : logLocation + File.separator) + this.contextName + ".log",
                            true));
        } catch (Exception e) {
        }
        docBase = location;
        this.workDir = workDir;
        this.sessionBackup = sessionBackup;
        File workDirFile = new File(workDir);
        if (!workDirFile.exists() && !workDirFile.mkdirs()) {
            cat.warn("Cannot create working directory " + workDir + ", can cause errors");
        }
        //Reads the xml configuration file for the web application.
        config = NusuthAppConfigFactory.createConfig("web-app",
                location + File.separator
                + "WEB-INF" + File.separator
                + "web.xml");
        Enumeration enum = config.getSimpleChild(Tags.DISPLAY_NAME);
        if (enum.hasMoreElements()) {
            displayName = ((SimpleNusuthWebAppElement) enum.nextElement()).
                    getContent().trim();
        }
        enum = config.getSimpleChild(Tags.DISTRIBUTABLE);
        while (enum.hasMoreElements()) {
            distributable = ((SimpleNusuthWebAppElement) enum.nextElement()).
                    getContent().trim().equalsIgnoreCase("true");
        }
        List startupServlets = new ArrayList();
        if (commonConfig != null) {
            simpleLoad(commonConfig, servletMappings, Tags.SERVLET_MAPPING,
                    Tags.URL_PATTERN, Tags.SERVLET_NAME);
            simpleLoad(commonConfig, mimeTypes, Tags.MIME_MAPPING, Tags.EXTENSION,
                    Tags.MIME_TYPE);
            loadTaglibs(commonConfig, taglibs);
            loadServlets(commonConfig, startupServlets);
            loadSessionConfig(commonConfig);
            loadWelcomeFilesList(commonConfig, welcomeFilesList);
            loadErrorMapings(commonConfig, errorMapings);
        }
        simpleLoad(config, initParameters, Tags.CONTEXT_PARAM, Tags.PARAM_NAME,
                Tags.PARAM_VALUE);
        simpleLoad(config, servletMappings, Tags.SERVLET_MAPPING, Tags.URL_PATTERN,
                Tags.SERVLET_NAME);
        simpleLoad(config, mimeTypes, Tags.MIME_MAPPING, Tags.EXTENSION,
                Tags.MIME_TYPE);
        loadTaglibs(config, taglibs);
        loadErrorMapings(config, errorMapings);
        addClassPath(loader);
        Thread.currentThread().setContextClassLoader(loader);

        // added by igork
        if (appSettings != null) {
            try {
                CompositeNusuthWebAppElement jndiLinkElement =
                        ManagementUtil.getCompositeElement(appSettings, "jdbc-link");
                useJTA = (jndiLinkElement != null)
                        ? ManagementUtil.getSimpleBoolean(appSettings, "jta", false)
                        : false;
                namingContext = NusuthAppJndiContextLoader.
                        load(config, appSettings, cat, useJTA);
            } catch (Throwable t) {
                cat.error("Couldn't instantiate class \"InitialContext\"...", t);
                try {
                    namingContext = new MemoryContext(null);
                    Context tempCtx = namingContext.createSubcontext("comp");
                    tempCtx = tempCtx.createSubcontext("env");
                } catch (NamingException nex) {
                    cat.error("Couldn't create empty initial context", t);
                    namingContext = null;
                }
            }

            if (appSettings.getSimpleChild("destroy-timeout").hasMoreElements()) {
                killingTimeout = ManagementUtil.getSimpleTime(appSettings,
                        "destroy-timeout");
            }
        } else {
            try {
                namingContext = new MemoryContext(null);
                Context tempCtx = namingContext.createSubcontext("comp");
                tempCtx = tempCtx.createSubcontext("env");
            } catch (Throwable t) {
                cat.error("Couldn't create empty initial context", t);
                namingContext = null;
            }
        }

        loadServlets(config, startupServlets);
        loadSessionConfig(config);
        File tempDir
                = new File(workDir, "_" + this.contextName + File.separator + "temp");
        if (!tempDir.exists()) {
            if (!tempDir.mkdirs()) {
                cat.error("Cannot create temprorary directory");
            }
        }
        attributes.put("javax.servlet.context.tempdir", tempDir);
        loadPackagedTaglibs();
        loadWelcomeFilesList(config, welcomeFilesList);
        processStartupServlets(startupServlets);
        securityManager = new WebAppSecurityManagerImpl(new File(docBase));
        loadFilters(config);
        loadElement(config, filterName2filterElement,
                Tags.FILTER, Tags.FILTER_NAME);
        if (commonConfig != null) {
            loadFilters(commonConfig);
            loadElement(commonConfig, filterName2filterElement,
                    Tags.FILTER, Tags.FILTER_NAME);
        }
        loadServletUrls();
        if (commonConfig != null) {
            loadFilterMappings(commonConfig);
        }
        loadFilterMappings(config);
        loadListeners(config);
        for (int i = 0; i < contextListeners.size(); i++) {
            ((ServletContextListener) contextListeners.get(i)).
                    contextInitialized(new ServletContextEvent(this));
        }
        Thread.currentThread().
                setContextClassLoader(ClassLoader.getSystemClassLoader());
        if (appSettings != null) {
            CompositeNusuthWebAppElement autoReloadNode
                    = ManagementUtil.getCompositeElement(appSettings, "auto-reload");
            if (autoReloadNode != null) {
                processAutoReloadNode(autoReloadNode, appSettings);
            }
            CompositeNusuthWebAppElement compileNode
                    = ManagementUtil.getCompositeElement(appSettings, "auto-compile");
            if (compileNode != null) {
                processCompileNode(compileNode);
            }
        }
    }

    public boolean containsResource(String path) {
        String realPath = getRealPath(path);
        if (realPath == null) {
            return false;
        }
        String cand = path.replace('/', File.separatorChar);
        if (cand.endsWith(File.separator)) {
            cand = cand.substring(0, cand.length() - 1);
        }
        return realPath.endsWith(cand);
    }

    private void processAutoReloadNode(CompositeNusuthWebAppElement autoReloadNode,
                                       CompositeNusuthWebAppElement conf)
            throws DeploymentException {
        String type = ManagementUtil.getSimpleString(autoReloadNode, "type");
        if (type.equalsIgnoreCase("internal")) {
            String checkTime
                    = ManagementUtil.getSimpleString(autoReloadNode,
                            "check-time");
            internalReloadListener
                    = new ContextInternalReloadListener(this,
                            Utils.parseTimeToMillis(checkTime), workDir, logLocation, conf, notConvertedContextName);
            internalReloadListener.start();
        } else if (type.equalsIgnoreCase("lazy")) {
            String checkTime
                    = ManagementUtil.getSimpleString(autoReloadNode,
                            "check-time");
            lazyReloadListener
                    = new ContextLazyReloadListener(this,
                            Utils.parseTimeToMillis(
                                    checkTime),
                            workDir, logLocation,
                            conf,
                            notConvertedContextName);
            lazyReloadListener.start();
        } else {
            cat.error("Uknow type \"" + type + "\" in auto-reload node");
        }
    }

    /**
     * This method process lazy reload for current context.
     */
    public void processLalyReload() {
        try {
            NusuthContext newContext = new NusuthContext(docBase,
                    notConvertedContextName,
                    workDir, logLocation,
                    appSettings,
                    sessionBackup);
            if (getSessionManager() instanceof DefaultSessionManager) {
                newContext.setSessionManager(new DefaultSessionManager(newContext,
                        false));
            } else {
                newContext.setSessionManager(
                        new DistributedSessionManager(newContext));
            }
            NusuthRequestHandler.startNewContext(notConvertedContextName, newContext);
        } catch (DeploymentException e) {
            cat.error("Cannot create context", e);
        }
    }

    private void processCompileNode(CompositeNusuthWebAppElement compileNode)
            throws DeploymentException {
        boolean srcExist = true;
        String srcDir = ManagementUtil.getSimpleString(compileNode, "src-dir");
        if (srcDir == null) {
            srcDir = "/WEB-INF/classes";
        }
        File srcDirFile = new File(docBase, srcDir);
        if (!srcDirFile.exists()) {
            cat.error("Cannot find source directory "
                    + srcDirFile.getAbsolutePath());
            srcExist = false;
        }
        File output = new File(docBase, "/WEB-INF/classes");
        if (srcExist) {
            String compilerString
                    = ManagementUtil.getSimpleString(compileNode, "compiler");
            if (compilerString != null) {
                if (!compilerString.equalsIgnoreCase("javac")
                        && !compilerString.equalsIgnoreCase("jikes")) {
                    cat.warn("Uknown compiler " + compilerString
                            + ", javac will be used");
                    compilerString = "javac";
                }
            } else {
                compilerString = "javac";
            }
            JspCompiler compiler = null;
            if (compilerString.toLowerCase().trim().equals("javac")) {
                compiler = new JavacJspCompiler();
            } else {
                compiler = new JikesJspCompiler();
            }
            String encoding = ManagementUtil.getSimpleString(compileNode,
                    "encoding");
            compiler.setOutputDir(output.getAbsolutePath());
            String classPath = "";
            Iterator iter = loader.getClassPathes();
            String cpath;
            String sep = File.pathSeparator;
            while (iter.hasNext()) {
                cpath = (String) iter.next();
                if (classPath.length() > 0) {
                    classPath += sep;
                }
                classPath += cpath;
            }
            String syscp = System.getProperty("java.class.path");
            if (syscp != null && syscp.length() > 0) {
                classPath += sep;
                classPath += syscp;
            }
            compiler.setClassPath(classPath);
            boolean outSet = true;
            File compilerOut = new File(workDir, convertName(this.contextName));
            if (!compilerOut.exists() && !compilerOut.mkdirs()) {
                outSet = false;
            }
            compilerOut = new File(compilerOut, "compiler_out.log");
            try {
                compiler.setErrorOut(new FileOutputStream(compilerOut));
            } catch (FileNotFoundException e) {
                outSet = false;
                cat.error("Cannot set compiler output", e);
            }
            String checkTime
                    = ManagementUtil.getSimpleString(compileNode,
                            "check-time");
            if (encoding != null) {
                compiler.setEncoding(encoding);
            }
            if (outSet) {
                compileListener =
                        new ContextCompileListener(this,
                                srcDirFile.getAbsolutePath(),
                                Utils.parseTimeToMillis(
                                        checkTime),
                                compiler);
                compileListener.start();
            }
        }
    }

    /**
     * This method apply "external" context settings such as compile listener
     * changes, reload listener changes etc.
     * @param conf External config
     * @throws DeploymentException Throws if any errors occurs during applying
     * new settings.
     */
    public void applyExternalSettings(CompositeNusuthWebAppElement conf)
            throws DeploymentException {
        // destroy-timeout comparing
        long newTime = -1;
        if (conf.getSimpleChild("destroy-timeout").hasMoreElements()) {
            if (ManagementUtil.getSimpleString(conf,
                    "destroy-timeout").trim().length() > 0) {
                newTime = ManagementUtil.getSimpleTime(conf, "destroy-timeout");
            }
        }
        if (newTime != -1) {
            killingTimeout = newTime;
        }
        // auto-compile comparing
        Enumeration enum = conf.getCompositeChild("auto-compile");
        if (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement compileElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            if (compileListener != null) {
                compileListener.shutDown();
                processCompileNode(compileElement);
            } else {
                processCompileNode(compileElement);
            }
        } else if (compileListener != null) {
            compileListener.shutDown();
        }
        // auto-reload comparing
        CompositeNusuthWebAppElement newAutoReload
                = ManagementUtil.getCompositeElement(conf, "auto-reload");
        if (newAutoReload != null) {
            if (lazyReloadListener != null) {
                lazyReloadListener.shutDown();
                lazyReloadListener = null;
            } else if (internalReloadListener != null) {
                internalReloadListener.shutDown();
                internalReloadListener = null;
            }
            processAutoReloadNode(newAutoReload, conf);
        } else {
            if (lazyReloadListener != null) {
                lazyReloadListener.shutDown();
                lazyReloadListener = null;
            } else if (internalReloadListener != null) {
                internalReloadListener.shutDown();
                internalReloadListener = null;
            }
        }
        // jndi
        boolean needJNDITreeReload = false;
        boolean oldUseJta = ManagementUtil.getSimpleBoolean(appSettings, "jta",
                false);
        boolean newUseJta = ManagementUtil.getSimpleBoolean(conf, "jta", false);
        needJNDITreeReload = (oldUseJta != newUseJta);
        if (needJNDITreeReload) {
            NusuthAppJndiContextLoader.reload(this.config, conf, cat, newUseJta,
                    namingContext);
            return;
        }
        String oldERCU = ManagementUtil.getSimpleString(appSettings,
                "external-rmi-codebase-url");
        String newERCU = ManagementUtil.getSimpleString(conf,
                "external-rmi-codebase-url");
        if (!oldERCU.equalsIgnoreCase(newERCU)) {
            NusuthAppJndiContextLoader.reload(this.config, config, cat, newUseJta,
                    namingContext);
            return;
        }
        enum = conf.getCompositeChild("jndi-link");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement el
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            if (!appSettings.containsCompositeChild(el, "jndi-link")) {
                needJNDITreeReload = true;
            }
        }
        enum = appSettings.getCompositeChild("jndi-link");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement el
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            if (!conf.containsCompositeChild(el, "jndi-link")) {
                needJNDITreeReload = true;
            }
        }
        enum = conf.getCompositeChild("jdbc-link");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement el
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            if (!appSettings.containsCompositeChild(el, "jdbc-link")) {
                needJNDITreeReload = true;
            }
        }
        enum = appSettings.getCompositeChild("jdbc-link");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement el
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            if (!conf.containsCompositeChild(el, "jdbc-link")) {
                needJNDITreeReload = true;
            }
        }
        if (needJNDITreeReload) {
            NusuthAppJndiContextLoader.reload(this.config, conf, cat, newUseJta,
                    namingContext);
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Check if library or class files was changed.
     * @return true if one or more files was changed since last checking.
     */
    public boolean isReloadNeeded() {
        File classesDir = new File(docBase, "WEB-INF" + File.separator + "classes");
        File libDir = new File(docBase, "WEB-INF" + File.separator + "lib");
        boolean b1 = reloadFromClassesNeed(classesDir);
        boolean b2 = reloadFromLibNeed(libDir);
        return (b1 || b2);
    }

    /**
     * Check if class files was changed.
     * @return true if one or more files was changed since last checking.
     */
    private boolean reloadFromClassesNeed(File dir) {
        if (!dir.exists()) {
            return false;
        }
        File[] files = dir.listFiles();
        boolean realNeed = false;
        boolean need = false;
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String path = files[i].getAbsolutePath();
                if (files[i].isFile()) {
                    if (path.endsWith(".class")) {
                        if (!resource2time.containsKey(path)) {
                            resource2time.put(path, new Long(files[i].lastModified()));
                        } else {
                            need = (((Long) resource2time.get(path)).longValue()
                                    != files[i].lastModified());
                        }
                    }
                } else {
                    need = reloadFromClassesNeed(files[i]);
                }
                if (need) {
                    resource2time.put(path, new Long(files[i].lastModified()));
                    realNeed = true;
                }
            }
        }
        return realNeed;
    }

    /**
     * Check if library files was changed.
     * @return true if one or more files was changed since last checking.
     */
    private boolean reloadFromLibNeed(File dir) {
        boolean realNeed = false;
        boolean need = false;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles(
                    new JarZipFilenameFilter());
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    String path = files[i].getAbsolutePath();
                    if (!resource2time.containsKey(path)) {
                        resource2time.put(path, new Long(files[i].lastModified()));
                    } else {
                        need = (((Long) resource2time.get(path)).longValue()
                                != files[i].lastModified());
                    }
                    if (need) {
                        realNeed = true;
                        resource2time.put(path, new Long(files[i].lastModified()));
                    }
                }
            }
        }
        return realNeed;
    }

    /**
     * This method reload context.
     */
    public void reloadContext() {
        if (isReloadNeeded()) {
            cat.debug("Start reloading context");
            reloading = true;
            loader = new ServletLoader();
            if (tagFactory != null) {
                tagFactory.clearPool();
                tagFactory.setClassLoader(loader);
            }
            if (jspLoader != null) {
                jspLoader.clearCache();
            }
            addClassPath(loader);
            Thread.currentThread().setContextClassLoader(loader);
            contextListeners.clear();
            contextAttrlisteners.clear();
            sessionListeners.clear();
            sessionAttrListeners.clear();
            sessionActListeners.clear();
            allListeners.clear();
            filters.clear();
            filter2params.clear();
            filterMappings.clear();
            servletConfigs.clear();
            servletName2servletElement.clear();
            singleThreadServletNames.clear();
            singleThreadServlets.clear();
            single2instance.clear();
            servlets.clear();
            try {
                loadListeners(config);
            } catch (DeploymentException e) {
                cat.error("Cannot reload listeners", e);
            }
            try {
                loadFilters(config);
            } catch (DeploymentException e) {
                cat.error("Cannot reload filters", e);
            }
            List startupServlets = new ArrayList();
            try {
                loadServlets(commonConfig, startupServlets);
                loadServlets(config, startupServlets);
            } catch (DeploymentException e) {
                cat.error("Cannot load servlets", e);
            }
            processStartupServlets(startupServlets);
            try {
                loadServletUrls();
            } catch (DeploymentException e) {
                cat.error("Cannot load servlet urls", e);
            }
            if (commonConfig != null) {
                try {
                    loadFilterMappings(commonConfig);
                } catch (DeploymentException e) {
                    cat.error("Cannot load filter mappings", e);
                }
            }
            try {
                loadFilterMappings(config);
            } catch (DeploymentException e) {
                cat.error("Cannot load filter mappings", e);
            }
            Thread.currentThread().setContextClassLoader(
                    ClassLoader.getSystemClassLoader());
            reloading = false;
            cat.debug("Context reloaded");
        }
    }

    /**
     * This method adds context classpath to loader.
     * @param loader Servlet loader.
     */
    private void addClassPath(ServletLoader loader) {
        //Adds the path to the classes dir of this wab application to the loader
        //object with the type of ServletLoader class.
        File waClassesLoc = new File(docBase + File.separator + "WEB-INF"
                + File.separator + "classes");
        if (waClassesLoc.exists() && waClassesLoc.isDirectory()) {
            try {
                loader.addClassPath(waClassesLoc.getCanonicalPath());
            } catch (IOException ioex) {
                //Logger.log("Error while adding classpath", ioex, 1);
                cat.error("Error while adding classpath", ioex);
            }
        }
        //Adds the absolute lib names used in this wab application to the
        //loader object with the type of ServletLoader class.
        File libsPath = new File(docBase + File.separator + "WEB-INF"
                + File.separator + "lib");
        if (libsPath.exists() && libsPath.isDirectory()) {
            String[] libs = libsPath.list(
                    new JarZipFilenameFilter());
            if (libs != null) {
                for (int i = 0; i < libs.length; i++) {
                    loader.addClassPath(docBase + File.separator + "WEB-INF"
                            + File.separator + "lib" + File.separator
                            + libs[i]);
                }
            }
        }
    }

    private void loadPackagedTaglibs() {
        try {
            File webinf = new File(docBase, "WEB-INF" + File.separator + "lib");
            File[] zips = webinf.listFiles(new JarZipFilenameFilter());
            if (zips != null) {
                for (int i = 0; i < zips.length; i++) {
                    ZipFile zipFile = new ZipFile(zips[i]);
                    Enumeration enum = zipFile.entries();
                    while (enum.hasMoreElements()) {
                        ZipEntry entry = (ZipEntry) enum.nextElement();
                        if (entry.getName().equals("META-INF/taglib.tld")) {
                            CompositeNusuthWebAppElement config = NusuthAppConfigFactory.
                                    createConfig("taglib",
                                            loader.getResourceAsStream(entry.getName()));
                            String uri
                                    = zips[i].getAbsolutePath().substring(docBase.length());
                            uri = uri.replace(File.separatorChar, '/');
                            if (uri != null) {
                                taglibs.put(uri,
                                        loader.getResource(entry.getName()).toString());
                                rep.addDefinedTagLib(uri, loader.getResource(entry.getName()).
                                        toString());
                            }
                        } else if (entry.getName().startsWith("META-INF")
                                && entry.getName().endsWith(".tld")) {
                            CompositeNusuthWebAppElement config = NusuthAppConfigFactory.
                                    createConfig("taglib",
                                            loader.getResourceAsStream(entry.getName()));
                            String uri = ManagementUtil.getSimpleString(config, "uri");
                            if (uri != null) {
                                taglibs.put(uri,
                                        loader.getResource(entry.getName()).toString());
                                rep.addDefinedTagLib(uri, loader.getResource(entry.getName()).
                                        toString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            cat.error("Cannot load packaged taglib", e);
        }
    }

    public CompositeNusuthWebAppElement getConfig() {
        return config;
    }

    private void processStartupServlets(List startupServlets) {
        Collections.sort(startupServlets);
        Iterator iter = startupServlets.iterator();
        while (iter.hasNext()) {
            ServletStartup ss = (ServletStartup) iter.next();
            try {
                findServlet(ss.servletName);
            } catch (Exception ex) {
                //Logger.log("Cannot init servlet " + ss.servletName, ex, 1);
                cat.error("Cannot init servlet " + ss.servletName, ex);
            }
        }
    }

    public boolean isRestartNeeded(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        Hashtable params = new Hashtable();
        Enumeration enum;
        simpleLoad(settings, params, Tags.CONTEXT_PARAM, Tags.PARAM_NAME,
                Tags.PARAM_VALUE);
        enum = initParameters.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!params.containsKey(key)) {
                return true;
                //        initParameters.remove(key);
            } else if (!params.get(key).equals(initParameters.get(key))) {
                return true;
                //        initParameters.put(key, params.get(key));
            }
        }
        enum = params.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!initParameters.containsKey(key))
                return true;
            initParameters.put(key, params.get(key));
        }
        params.clear();
        return false;
    }

    public boolean isDistributable() {
        return distributable;
    }

    public void applySettings(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        if (isRestartNeeded(settings)) {
            throw new IllegalArgumentException("Cannot apply settings - "
                    + "restart needed");
        }
        NusuthRequestHandler.clearCache();
        String content = null;
        Hashtable params = new Hashtable();
        Enumeration enum = null;
        Enumeration enum1 = null;
        Enumeration enum2 = null;
        CompositeNusuthWebAppElement compElement = null;
        SimpleNusuthWebAppElement simpleElement;
        Hashtable defaultServletMappings = new Hashtable();
        Hashtable defaultMimeTypes = new Hashtable();
        Hashtable defaultTaglibs = new Hashtable();
        Hashtable defaultServletElements = new Hashtable();
        List defaultWelcomeFilesList = new ArrayList();
        List defaultErrorMappings = new ArrayList();

        simpleLoad(commonConfig, defaultServletMappings, Tags.SERVLET_MAPPING,
                Tags.URL_PATTERN, Tags.SERVLET_NAME);
        simpleLoad(commonConfig, defaultMimeTypes, Tags.MIME_MAPPING,
                Tags.EXTENSION, Tags.MIME_TYPE);
        loadTaglibs(commonConfig, defaultTaglibs);
        loadWelcomeFilesList(commonConfig, defaultWelcomeFilesList);
        loadErrorMapings(commonConfig, defaultErrorMappings);
        loadElement(commonConfig, defaultServletElements,
                Tags.SERVLET, Tags.SERVLET_NAME);

        enum = settings.getSimpleChild(Tags.DISPLAY_NAME);
        while (enum.hasMoreElements()) {
            content = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().
                    trim();
        }
        if (content != null)
            displayName = content;

        enum = settings.getCompositeChild(Tags.WELCOME_FILE_LIST);
        if (enum.hasMoreElements()) {
            compElement = (CompositeNusuthWebAppElement) enum.nextElement();
            enum1 = ((Enumeration) compElement.getSimpleChild(Tags.WELCOME_FILE));
            int count = 0;
            List welFiles = new ArrayList();
            while (enum1.hasMoreElements()) {
                content = ((SimpleNusuthWebAppElement) enum1.nextElement()).getContent().
                        trim();
                welFiles.add(count++, content);
            }
            for (int i = 0; i < welcomeFilesList.size(); i++) {
                if (!welFiles.contains(welcomeFilesList.get(i))
                        && !defaultWelcomeFilesList.contains(welcomeFilesList.get(i)))
                    welcomeFilesList.remove(i);
            }
            for (int i = 0; i < welFiles.size(); i++) {
                if (!welcomeFilesList.contains(welFiles.get(i)))
                    welcomeFilesList.add(i, welFiles.get(i));
            }
        } else {
            for (int i = 0; i < welcomeFilesList.size(); i++) {
                if (!defaultWelcomeFilesList.contains(welcomeFilesList.get(i)))
                    welcomeFilesList.remove(i);
            }
        }

        List errors = new ArrayList();
        loadErrorMapings(settings, errors);
        for (int i = 0; i < errorMapings.size(); i++) {
            if (!errors.contains(errorMapings.get(i))
                    && !defaultErrorMappings.contains(errorMapings.get(i)))
                errorMapings.remove(i);
        }
        for (int i = 0; i < errors.size(); i++) {
            if (!errorMapings.contains(errors.get(i)))
                errorMapings.add(i, errors.get(i));
        }

        loadTaglibs(settings, params);
        enum = taglibs.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!params.containsKey(key)) {
                if (!defaultTaglibs.containsKey(key)) {
                    taglibs.remove(key);
                    contextWasChanged = true;
                }
            } else if (!params.get(key).equals(taglibs.get(key))) {
                taglibs.put(key, params.get(key));
                contextWasChanged = true;
            }
        }
        enum = params.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!taglibs.containsKey(key)) {
                taglibs.put(key, params.get(key));
                contextWasChanged = true;
            }
        }
        params.clear();

        simpleLoad(settings, params, Tags.MIME_MAPPING, Tags.EXTENSION,
                Tags.MIME_TYPE);
        enum = mimeTypes.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!params.containsKey(key)) {
                if (!defaultMimeTypes.containsKey(key))
                    mimeTypes.remove(key);
            } else if (!params.get(key).equals(mimeTypes.get(key))) {
                mimeTypes.put(key, params.get(key));
            }
        }
        enum = params.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!mimeTypes.containsKey(key))
                mimeTypes.put(key, params.get(key));
        }
        params.clear();

        simpleLoad(settings, params, Tags.SERVLET_MAPPING, Tags.URL_PATTERN,
                Tags.SERVLET_NAME);
        enum = servletMappings.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!params.containsKey(key)) {
                if (!defaultServletMappings.containsKey(key))
                    servletMappings.remove(key);
            } else if (!params.get(key).equals(servletMappings.get(key))) {
                servletMappings.put(key, params.get(key));
            }
        }
        enum = params.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!servletMappings.containsKey(key))
                servletMappings.put(key, params.get(key));
        }
        params.clear();

        loadElement(settings, params, Tags.FILTER, Tags.FILTER_NAME);
        enum = filterName2filterElement.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            Hashtable iparams = new Hashtable();
            if (!params.containsKey(key)) {
                Filter filter = (Filter) filters.remove(key);
                filter.destroy();
                filterName2filterElement.remove(key);
            } else if (!params.get(key).equals(filterName2filterElement.get(key))) {
                String filterName = (String) key;
                String filterClass = ((SimpleNusuthWebAppElement)
                        ((CompositeNusuthWebAppElement) params.get(key)).
                        getSimpleChild(Tags.FILTER_CLASS).nextElement()).
                        getContent().trim();
                CompositeNusuthWebAppElement oldElement
                        = (CompositeNusuthWebAppElement)
                        filterName2filterElement.get(key);
                CompositeNusuthWebAppElement newElement
                        = (CompositeNusuthWebAppElement) params.get(key);
                String oldClass = ((SimpleNusuthWebAppElement)
                        oldElement.getSimpleChild(Tags.SERVLET_CLASS).nextElement()).
                        getContent().trim();
                String newClass = ((SimpleNusuthWebAppElement) newElement.
                        getSimpleChild(Tags.SERVLET_CLASS).nextElement()).
                        getContent().trim();
                enum1 = newElement.getCompositeChild(Tags.INIT_PARAM);
                while (enum1.hasMoreElements()) {
                    CompositeNusuthWebAppElement paramElement
                            = (CompositeNusuthWebAppElement) enum1.nextElement();
                    String paramName = ((SimpleNusuthWebAppElement) paramElement.
                            getSimpleChild(Tags.PARAM_NAME).nextElement()).
                            getContent().trim();
                    String paramValue = ((SimpleNusuthWebAppElement) paramElement.
                            getSimpleChild(Tags.PARAM_VALUE).nextElement()).
                            getContent().trim();
                    iparams.put(paramName, paramValue);
                }
                if (!oldClass.equals(newClass)) {
                    try {
                        Filter filter = (Filter) loader.loadClass(filterClass).newInstance();
                        filter.init(new NusuthFilterConfig(iparams, filterName, this));
                        Filter filter1 = (Filter) filters.put(filterName, filter);
                        filter1.destroy();
                        filterName2filterElement.put(filterName, compElement);
                    } catch (InstantiationException e) {
                        cat.error("Cannot instantiate filter " + filterName);
                        Filter filter1 = (Filter) filters.remove(filterName);
                        if (filter1 != null)
                            filter1.destroy();
                    } catch (IllegalAccessException e) {
                        cat.error("Cannot access filter " + filterName);
                        Filter filter1 = (Filter) filters.remove(filterName);
                        if (filter1 != null)
                            filter1.destroy();
                    } catch (ServletException e) {
                        cat.error("Cannot initialize filter " + filterName);
                        Filter filter1 = (Filter) filters.remove(filterName);
                        if (filter1 != null)
                            filter1.destroy();
                    } catch (ClassNotFoundException e) {
                        cat.error("Cannot find class " + filterClass
                                + " for filter " + filterName);
                        Filter filter1 = (Filter) filters.remove(filterName);
                        if (filter1 != null)
                            filter1.destroy();
                    }
                } else {
                    Filter filter = (Filter) filters.remove(filterName);
                    filter.destroy();
                    try {
                        filter.init(new NusuthFilterConfig(iparams, filterName, this));
                        filters.put(filterName, filter);
                        filterName2filterElement.put(filterName, compElement);
                    } catch (ServletException e) {
                        cat.error("Cannot init filter " + filterName, e);
                    }
                }
            }
        }
        enum = params.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!filters.containsKey(key)) {
                String filterName = (String) key;
                String filterClass = ((SimpleNusuthWebAppElement)
                        ((CompositeNusuthWebAppElement) params.get(key)).
                        getSimpleChild(Tags.FILTER_CLASS).nextElement()).
                        getContent().trim();
                Hashtable iparams = new Hashtable();
                compElement = (CompositeNusuthWebAppElement) params.get(filterName);
                if (((SimpleNusuthWebAppElement) ((Enumeration) compElement.
                        getSimpleChild(Tags.FILTER_NAME)).nextElement()).
                        getContent().trim().equals(key)) {
                    simpleLoad(compElement, iparams, Tags.INIT_PARAM,
                            Tags.PARAM_NAME, Tags.PARAM_VALUE);
                }
                try {
                    Filter filter = (Filter) loader.loadClass(filterClass).newInstance();
                    filter.init(new NusuthFilterConfig(iparams, filterName, this));
                    filters.put(filterName, filter);
                    filterName2filterElement.put(filterName, compElement);
                } catch (InstantiationException e) {
                    cat.error("Cannot instantiate filter " + filterName);
                } catch (IllegalAccessException e) {
                    cat.error("Cannot access filter " + filterName);
                } catch (ServletException e) {
                    cat.error("Cannot initialize filter " + filterName);
                } catch (ClassNotFoundException e) {
                    cat.error("Cannot find class " + filterClass + " for filter " + filterName);
                }
            }
        }
        params.clear();
/*
    filterMappings.clear();
    loadServletUrls();
    loadFilterMappings(settings);
*/
        loadElement(settings, params, Tags.LISTENER, Tags.LISTENER_CLASS);
        enum = allListeners.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!params.containsKey(key)) {
                removeListener(key);
            }
        }
        enum = params.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!allListeners.containsKey(key)) {
                String listenerClass = (String) key;
                try {
                    Object listener = loader.loadClass(listenerClass).newInstance();
                    addListener(listener);
                } catch (InstantiationException e) {
                    cat.error("Cannot instantinate listener " + listenerClass);
                } catch (IllegalAccessException e) {
                    cat.error("Cannot access listener " + listenerClass);
                } catch (ClassNotFoundException e) {
                    cat.error("Cannot find listener class " + listenerClass);
                }
            }
        }
        params.clear();

        loadElement(settings, params, Tags.SERVLET, Tags.SERVLET_NAME);
        enum = servletName2servletElement.keys();
        List startupServlets = new ArrayList();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!params.containsKey(key)) {
                if (!defaultServletElements.containsKey(key)) {
                    Servlet servlet = (Servlet) servlets.remove(key);
                    if (servlet != null) {
                        servlet.destroy();
                    }
                    servletConfigs.remove(key);
                    servletName2servletElement.remove(key);
                }
            } else if (!params.get(key).equals(servletName2servletElement.get(key))) {
                CompositeNusuthWebAppElement oldElement
                        = (CompositeNusuthWebAppElement) servletName2servletElement.
                        remove(key);
                CompositeNusuthWebAppElement newElement
                        = (CompositeNusuthWebAppElement) params.get(key);
                String oldClass = (oldElement.getSimpleChild(Tags.SERVLET_CLASS).
                        hasMoreElements())
                        ? ((SimpleNusuthWebAppElement) oldElement.
                        getSimpleChild(Tags.SERVLET_CLASS).nextElement()).
                        getContent().trim()
                        : null;
                String newClass = (newElement.getSimpleChild(Tags.SERVLET_CLASS).
                        hasMoreElements())
                        ? ((SimpleNusuthWebAppElement) newElement.
                        getSimpleChild(Tags.SERVLET_CLASS).nextElement()).
                        getContent().trim() : null;
                String oldJsp = (oldElement.getSimpleChild(Tags.JSP_FILE).
                        hasMoreElements())
                        ? ((SimpleNusuthWebAppElement) oldElement.
                        getSimpleChild(Tags.JSP_FILE).nextElement()).getContent().trim()
                        : null;
                String newJsp = (newElement.getSimpleChild(Tags.JSP_FILE).
                        hasMoreElements())
                        ? ((SimpleNusuthWebAppElement) newElement.
                        getSimpleChild(Tags.JSP_FILE).nextElement()).getContent().trim()
                        : null;
                if ((oldClass != null && newClass != null && !oldClass.equals(newClass))
                        || (oldJsp != null && newJsp != null && !oldJsp.equals(newJsp))
                        || (oldClass != null && newJsp != null)
                        || (oldJsp != null && newClass != null)) {
                    Servlet servlet = (Servlet) servlets.remove(key);
                    if (servlet != null) {
                        servlet.destroy();
                    }
                    servletConfigs.remove(key);
                    loadServlet(newElement, startupServlets);
                } else {
                    servletName2servletElement.put(key, newElement);
                    Hashtable newParams = new Hashtable();
                    String paramName = null;
                    String paramValue = null;
                    enum1 = newElement.getCompositeChild(Tags.INIT_PARAM);
                    while (enum1.hasMoreElements()) {
                        CompositeNusuthWebAppElement paramElement
                                = (CompositeNusuthWebAppElement) enum1.nextElement();
                        paramName = ((SimpleNusuthWebAppElement) paramElement.
                                getSimpleChild(Tags.PARAM_NAME).nextElement()).
                                getContent().trim();
                        paramValue = ((SimpleNusuthWebAppElement) paramElement.
                                getSimpleChild(Tags.PARAM_VALUE).nextElement()).
                                getContent().trim();
                        newParams.put(paramName, paramValue);
                    }
                    Servlet servlet = (Servlet) servlets.remove(key);
                    if (servlet != null) {
                        servlet.destroy();
                    }
                    ServletConfig newConfig
                            = new NusuthServletConfig(newParams, this, (String) key,
                                    new ClassOrJsp((String) key,
                                            ((NusuthServletConfig)
                            servletConfigs.
                            get(key)).isForJsp()
                                    ));
                    try {
                        if (servlet == null) {
                            try {
                                servlet = findServlet(newConfig.getServletName());
                            } catch (Exception e) {
                                cat.error("Cannot load sevlet " + newConfig.getServletName(), e);
                            }
                        }
                        if (servlet != null) {
                            servlet.init(newConfig);
                            servletConfigs.put(key, newConfig);
                            servlets.put(key, servlet);
                        }
                    } catch (ServletException e) {
                        cat.error("Cannot init servlet " + (String) key, e);
                    }
                }
            }
        }
        enum = params.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            if (!servletName2servletElement.containsKey(key)) {
                //        loadServlet(compElement, startupServlets);
                loadServlet((CompositeNusuthWebAppElement) params.get(key),
                        startupServlets);
            }
        }
        processStartupServlets(startupServlets);
        params.clear();

        filterMappings.clear();
        loadServletUrls();
        loadFilterMappings(settings);

        int sesTimeOut = 1800;
        if (settings.getCompositeChild(Tags.SESSION_CONFIG).hasMoreElements()) {
            compElement = (CompositeNusuthWebAppElement) settings.
                    getCompositeChild(Tags.SESSION_CONFIG).nextElement();
            simpleElement = (SimpleNusuthWebAppElement) compElement.
                    getSimpleChild(Tags.SESSION_TIMEOUT).nextElement();
            try {
                sesTimeOut = 60 * Integer.parseInt(simpleElement.getContent());
            } catch (NumberFormatException nfex) {
                cat.error("Cannot parse session timeout value", nfex);
            }
        } else {
            enum = sessionManager.getSessionsKeys();
            while (enum.hasMoreElements()) {
                Object id = enum.nextElement();
                ((HttpSession) sessionManager.getSession((String) id)).
                        setMaxInactiveInterval(sesTimeOut);
            }
        }
        if (defaultSessionLifetime != sesTimeOut) {
            enum = sessionManager.getSessionsKeys();
            while (enum.hasMoreElements()) {
                Object id = enum.nextElement();
                ((HttpSession) sessionManager.getSession((String) id)).
                        setMaxInactiveInterval(sesTimeOut);
            }
            defaultSessionLifetime = sesTimeOut;
        }
        config = settings;
        File file = new File(docBase + File.separator + "WEB-INF"
                + File.separator + "web.xml");
        try {
            FileWriter wr = new FileWriter(file);
            wr.write(config.toString());
            wr.close();
        } catch (IOException e) {
            cat.error("Cannot write to \"" + file.getAbsolutePath() + "\"", e);
        }
        securityManager = new WebAppSecurityManagerImpl(new File(docBase));
    }

    public void applySettingsWithNewUsers(CompositeNusuthWebAppElement newSettings,
                                          CompositeNusuthWebAppElement newUsers)
            throws DeploymentException {
        if (newUsers != null) {
            File file = new File(docBase + File.separator + "WEB-INF"
                    + File.separator + "users.xml");
            boolean created = true;
            if (!file.exists()) {
                try {
                    if (!file.createNewFile()) {
                        cat.error("Cannot create file " + file.getAbsolutePath());
                        created = false;
                    }
                } catch (IOException e) {
                    cat.error("Cannot create file " + file.getAbsolutePath());
                    created = false;
                }
            }
            if (created) {
                try {
                    FileWriter wr = new FileWriter(file);
                    wr.write(newUsers.toString());
                    wr.close();
                } catch (IOException e) {
                    cat.error("Cannot write to \"" + file.getAbsolutePath() + "\"", e);
                }
            }
        }
        applySettings(newSettings);
    }

    public CompositeNusuthWebAppElement getUsersConfig() {
        return securityManager.getUsersConfig();
    }

    private void loadServletUrls() throws DeploymentException {
        Enumeration enum = servletMappings.keys();
        while (enum.hasMoreElements()) {
            filterMappings.put(enum.nextElement(), new ResourceChain());
        }
        loadSecurityParameters();
    }

    private void loadSecurityParameters() throws DeploymentException {
        defaultAuthMethod = AuthenticationData.AUTH_METHOD_BASIC;
        Enumeration enum = config.getCompositeChild("login-config");
        if (!enum.hasMoreElements()) return;
        CompositeNusuthWebAppElement loginNode
                = (CompositeNusuthWebAppElement) enum.nextElement();

        enum = loginNode.getSimpleChild("auth-method");
        if (enum.hasMoreElements()) {
            String authMethod = ((SimpleNusuthWebAppElement) enum.nextElement()).
                    getContent().trim();
            defaultAuthMethod = AuthenticationData.methodName2int(authMethod);
            if (defaultAuthMethod == -1)
                defaultAuthMethod = AuthenticationData.AUTH_METHOD_BASIC;
        } else
            defaultAuthMethod = AuthenticationData.AUTH_METHOD_BASIC;

        enum = loginNode.getSimpleChild("realm-name");
        if (enum.hasMoreElements())
            authRealm = ((SimpleNusuthWebAppElement) enum.nextElement()).
                    getContent().trim();
        else
            authRealm = this.getContextName();

        if (defaultAuthMethod == AuthenticationData.AUTH_METHOD_FORM) {
            enum = loginNode.getCompositeChild("form-login-config");
            if (enum.hasMoreElements()) {
                CompositeNusuthWebAppElement formNode
                        = (CompositeNusuthWebAppElement) enum.nextElement();

                String tmp = ((SimpleNusuthWebAppElement) formNode.
                        getSimpleChild("form-login-page").nextElement()).
                        getContent().trim();
                formLoginPage = new StrBuffer(tmp.length());
                if (!tmp.startsWith("/"))
                    formLoginPage.append('/');
                formLoginPage.append(tmp);
                tmp = ((SimpleNusuthWebAppElement) formNode.
                        getSimpleChild("form-error-page").nextElement()).
                        getContent().trim();
                formErrorPage = new StrBuffer();
                if (!tmp.startsWith("/"))
                    formErrorPage.append('/');
                formErrorPage.append(tmp);
            } else {
                defaultAuthMethod = AuthenticationData.AUTH_METHOD_BASIC;
            }
        }
    }

    private void simpleLoad(CompositeNusuthWebAppElement config,
                            Hashtable destination, String rootTagName,
                            String fromTagName, String toTagName)
            throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        SimpleNusuthWebAppElement simpleElem1;
        SimpleNusuthWebAppElement simpleElem2;
        Enumeration enum = config.getCompositeChild(rootTagName);
        while (enum.hasMoreElements()) {
            compElem = (CompositeNusuthWebAppElement) enum.nextElement();
            simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(fromTagName).nextElement();
            simpleElem2 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(toTagName).nextElement();
            destination.put(simpleElem1.getContent().trim(),
                    simpleElem2.getContent().trim());
        }
    }

    private void loadTaglibs(CompositeNusuthWebAppElement config,
                             Hashtable destination)
            throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        SimpleNusuthWebAppElement simpleElem1;
        SimpleNusuthWebAppElement simpleElem2;
        Enumeration enum = config.getCompositeChild(Tags.TAGLIB);
        while (enum.hasMoreElements()) {
            compElem = (CompositeNusuthWebAppElement) enum.nextElement();
            simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.TAGLIB_URI).nextElement();
            simpleElem2 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.TAGLIB_LOCATION).nextElement();
            String location = simpleElem2.getContent().trim();
            if (!location.startsWith("/")) {
                location = "/WEB-INF/" + location;
            } else {
                if (!location.startsWith("/WEB-INF/")) {
                    cat.error("Tag library descriptor files must always be "
                            + "in the \"WEB-INF\" directory, or some "
                            + "subdirectory of it.");
                    continue;
                }
            }
            destination.put(simpleElem1.getContent().trim(), location);
        }
    }

    private void loadElement(CompositeNusuthWebAppElement config,
                             Hashtable destination, String rootTagName,
                             String key) throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        SimpleNusuthWebAppElement simpleElem1;
        Enumeration enum = config.getCompositeChild(rootTagName);
        while (enum.hasMoreElements()) {
            compElem = (CompositeNusuthWebAppElement) enum.nextElement();
            simpleElem1 = (SimpleNusuthWebAppElement) compElem.getSimpleChild(key).
                    nextElement();
            destination.put(simpleElem1.getContent().trim(), compElem);
        }
    }

    private void loadErrorMapings(CompositeNusuthWebAppElement config,
                                  List destination)
            throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        Enumeration enum = config.getCompositeChild(Tags.ERROR_PAGE);
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement el
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            SimpleNusuthWebAppElement simple
                    = (SimpleNusuthWebAppElement) el.getSimpleChild(Tags.LOCATION).
                    nextElement();
            if (!simple.getContent().startsWith("/")) {
                simple.setContent("/" + simple.getContent());
            }
            destination.add(el);
        }
    }

    private void loadFilters(CompositeNusuthWebAppElement config)
            throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        SimpleNusuthWebAppElement simpleElem1 = null;
        SimpleNusuthWebAppElement simpleElem2 = null;
        Enumeration enum = config.getCompositeChild(Tags.FILTER);
        while (enum.hasMoreElements()) {
            compElem = (CompositeNusuthWebAppElement) enum.nextElement();
            simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.FILTER_NAME).nextElement();
            simpleElem2 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.FILTER_CLASS).nextElement();
            String filterName = simpleElem1.getContent().trim();
            String filterClass = simpleElem2.getContent().trim();
            Hashtable iparams = new Hashtable();
            simpleLoad(compElem, iparams, Tags.INIT_PARAM, Tags.PARAM_NAME,
                    Tags.PARAM_VALUE);
            try {
                Filter filter = (Filter) loader.loadClass(filterClass).newInstance();
                filter.init(new NusuthFilterConfig(iparams, filterName, this));
                filters.put(filterName, filter);
                filter2params.put(filterName, iparams);
            } catch (InstantiationException e) {
                cat.error("Cannot instantiate filter " + filterName);
            } catch (IllegalAccessException e) {
                cat.error("Cannot access filter " + filterName);
            } catch (ServletException e) {
                cat.error("Cannot initialize filter " + filterName);
            } catch (ClassNotFoundException e) {
                cat.error("Cannot find class " + filterClass + " for filter " + filterName);
            }
        }
    }

    private void loadFilterMappings(CompositeNusuthWebAppElement config)
            throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        int count = 0;
        Enumeration enum = config.getCompositeChild(Tags.FILTER_MAPPING);
        while (enum.hasMoreElements()) {
            boolean cont = true;
            count++;
            compElem = (CompositeNusuthWebAppElement) enum.nextElement();
            loadFilterMapping(compElem, count);
        }
        findServletsForResourceChains();
    }

    /**
     * This method find servlet and it parameters such as servlet name,
     * servlet path, servlet class, servlet config and put their to all
     * constructed ResourseChains.
     */
    private void findServletsForResourceChains() {
        Enumeration enum = filterMappings.keys();
        while (enum.hasMoreElements()) {
            String url = (String) enum.nextElement();
            String[] servletInfo = findServletPath(url);
            if (servletInfo != null) {
                ServletConfig sConfig
                        = (ServletConfig) servletConfigs.get(servletInfo[1]);
                if (sConfig != null) {
                    ((ResourceChain) filterMappings.get(url)).setServletName(servletInfo[1]);
                    ((ResourceChain) filterMappings.get(url)).
                            setServletPath((servletInfo[0].length() == 0)
                            ? "/" : servletInfo[0]);
                    ((ResourceChain) filterMappings.get(url)).
                            setServletClass(((NusuthServletConfig) servletConfigs.
                            get(servletInfo[1])).getServletClass());
                    ((ResourceChain) filterMappings.get(url)).
                            setServletConfig(((NusuthServletConfig) servletConfigs.
                            get(servletInfo[1])));
                    Servlet servlet = null;
                    servlet = (Servlet) servlets.get(servletInfo[1]);
                    if (servlet != null) {
                        ((ResourceChain) filterMappings.get(url)).setServlet(servlet);
                    }
                } else {
                    cat.error("Cannot set sevlet parameters to ResourceChain, "
                            + "servlet with \"" + servletInfo[1]
                            + "\" name not found in web.xml");
                }
            }
        }
    }

    /**
     * This method construct ResourceChain for for given mapping or change all
     * constructed chains according to given mapping.
     * @param compElement Element that represent filter mapping.
     * @param count Ordiecutive number of given mapping.
     */
    private void loadFilterMapping(CompositeNusuthWebAppElement compElem,
                                   int count) throws DeploymentException {
        SimpleNusuthWebAppElement simpleElem1 = null;
        SimpleNusuthWebAppElement simpleElem2 = null;
        boolean cont = true;
        simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                getSimpleChild(Tags.FILTER_NAME).nextElement();
        boolean isurl = compElem.getSimpleChild(Tags.URL_PATTERN).hasMoreElements();
        simpleElem2 = isurl
                ? (SimpleNusuthWebAppElement) compElem.
                getSimpleChild(Tags.URL_PATTERN).nextElement()
                : (SimpleNusuthWebAppElement) compElem.
                getSimpleChild(Tags.SERVLET_NAME).nextElement();
        String filterName = simpleElem1.getContent().trim();
        String filterMapping = simpleElem2.getContent().trim();
        Filter filter = (Filter) filters.get(filterName);
        if (filter == null) {
            cat.error("Cannot load filter mapping for filter \"" + filterName
                    + "\", filter declaration not found in web.xml");
            return;
        }
        FilterMappingWrapper wrapper = null;
        String nameForDisp = null;
        if (!isurl) {
            nameForDisp = getServletMapping(filterMapping);
            if (nameForDisp != null) {
                filterMapping = nameForDisp;
                wrapper = new FilterMappingWrapper(filter, count, true, filterMapping);
            } else {
                cont = false;
            }
        } else {
            if (!filterMappings.containsKey(filterMapping)) {
                ResourceChain chain = new ResourceChain();
                filterMappings.put(filterMapping, chain);
            }
            wrapper = new FilterMappingWrapper(filter, count, false, filterMapping);
        }
        if (cont) {
            Enumeration e = filterMappings.keys();
            while (e.hasMoreElements()) {
                String url = (String) e.nextElement();
                if (isInclude(filterMapping, url)) {
                    ((ResourceChain) filterMappings.get(url)).addWrapper(wrapper);
                }
            }
            ((ResourceChain) filterMappings.get(filterMapping)).addWrapper(wrapper);
            if (!servletMappings.containsKey(filterMapping)) {
                e = filterMappings.keys();
                String includeName = null;
                int includeSize = 0;
                while (e.hasMoreElements()) {
                    String url = (String) e.nextElement();
                    if (isInclude(url, filterMapping)) {
                        if (((ResourceChain) filterMappings.get(url)).getNumberOfFilters()
                                > includeSize) {
                            includeName = url;
                            includeSize = ((ResourceChain) filterMappings.get(url)).
                                    getNumberOfFilters();
                        }
                    }
                }
                if (includeSize > 0) {
                    ((ResourceChain) filterMappings.get(filterMapping)).
                            addAll((ResourceChain) filterMappings.get(includeName));
                }
            }
        } else {
            if (filterForDispatchers.get(filterMapping) != null) {
                ((LinkedList) filterForDispatchers.get(filterMapping)).
                        add(filters.get(filterName));
            } else {
                LinkedList l = new LinkedList();
                l.add(filters.get(filterName));
                filterForDispatchers.put(filterMapping, l);
            }
        }
    }

    private boolean isInclude(String url1, String url2) {
        if (url1.equals("/*") && url2.equals("/")) {
            return true;
        }
        if (url1.equals(url2)) {
            return false;
        }
        if (url1.equals(url2 + "/*")) {
            return true;
        }
        StringTokenizer tok1 = new StringTokenizer(url1, "/", false);
        StringTokenizer tok2 = new StringTokenizer(url2, "/", false);
        while (tok1.hasMoreTokens() && tok2.hasMoreTokens()) {
            String token1 = tok1.nextToken();
            String token2 = tok2.nextToken();
            if (token1.equals("*") && !url2.startsWith("*.")) {
                return true;
            } else if (!token1.equals(token2)) {
                return false;
            }
        }
        return false;
    }

    private String getServletMapping(String servletName)
            throws DeploymentException {
        Enumeration enum = servletMappings.keys();
        while (enum.hasMoreElements()) {
            String url = (String) enum.nextElement();
            if (servletMappings.get(url).equals(servletName)) {
                return url;
            }
        }
        return null;
    }

    private void loadListeners(CompositeNusuthWebAppElement config)
            throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        SimpleNusuthWebAppElement simpleElem;
        Enumeration enum = config.getCompositeChild(Tags.LISTENER);
        while (enum.hasMoreElements()) {
            compElem = (CompositeNusuthWebAppElement) enum.nextElement();
            String listenerClass = ((SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.LISTENER_CLASS).nextElement()).
                    getContent().trim();
            try {
                Object listener = loader.loadClass(listenerClass).newInstance();
                addListener(listener);
            } catch (InstantiationException e) {
                cat.error("Cannot instantinate listener " + listenerClass);
            } catch (IllegalAccessException e) {
                cat.error("Cannot access listener " + listenerClass);
            } catch (ClassNotFoundException e) {
                cat.error("Cannot find listener class " + listenerClass);
            }
        }
    }

    public void addListener(Object listener) {
        if (listener instanceof ServletContextListener) {
            contextListeners.add(listener);
        }
        if (listener instanceof ServletContextAttributesListener) {
            contextAttrlisteners.add(listener);
        }
        if (listener instanceof HttpSessionListener) {
            sessionListeners.add(listener);
        }
        if (listener instanceof HttpSessionAttributesListener) {
            sessionAttrListeners.add(listener);
        }
        if (listener instanceof HttpSessionActivationListener) {
            sessionActListeners.add(listener);
        }
        allListeners.put(listener.getClass().getName(), listener);
    }

    private void removeListener(Object listener) {
        if (listener instanceof ServletContextListener) {
            contextListeners.remove(listener);
        }
        if (listener instanceof ServletContextAttributesListener) {
            contextAttrlisteners.remove(listener);
        }
        if (listener instanceof HttpSessionListener) {
            sessionListeners.remove(listener);
        }
        if (listener instanceof HttpSessionAttributesListener) {
            sessionAttrListeners.remove(listener);
        }
        if (listener instanceof HttpSessionActivationListener) {
            sessionActListeners.remove(listener);
        }
        allListeners.remove(listener.getClass().getName());
    }

    private void loadServlets(CompositeNusuthWebAppElement config,
                              List startupServlets) throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        SimpleNusuthWebAppElement simpleElem1;
        SimpleNusuthWebAppElement simpleElem2;
        //Gets all complex sub elements of the root element, with
        //the Tags.SERVLET tag.
        Enumeration enum = config.getCompositeChild(Tags.SERVLET);
        while (enum.hasMoreElements()) {
            compElem = (CompositeNusuthWebAppElement) enum.nextElement();
            loadServlet(compElem, startupServlets);
        }
    }

    private void loadServlet(CompositeNusuthWebAppElement compElem,
                             List startupServlets) throws DeploymentException {
        SimpleNusuthWebAppElement simpleElem1;
        SimpleNusuthWebAppElement simpleElem2;
        if (compElem.getSimpleChild(Tags.SERVLET_CLASS).hasMoreElements()) {
            //If there is simple sub element with the Tags.SERVLET_CLASS tag name
            //then it gets the simple sub element with the Tags.SERVLET_CLASS and
            //Tags.SERVLET_NAME tag names
            simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.SERVLET_CLASS).nextElement();
            simpleElem2 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.SERVLET_NAME).nextElement();
            String servletName = simpleElem2.getContent();
            String servletClass = simpleElem1.getContent();
            //Gets the load on start up value and adds this information
            //to the startupServlets array.
            if (compElem.getSimpleChild(Tags.LOAD_ON_STARTUP).hasMoreElements()) {
                simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                        getSimpleChild(Tags.LOAD_ON_STARTUP).nextElement();
                try {
                    int lOnStartup = Integer.parseInt(simpleElem1.getContent());
                    if (lOnStartup > 0) {
                        ServletStartup startup = new ServletStartup(servletName,
                                lOnStartup);
                        startupServlets.add(startup);
                    }
                } catch (NumberFormatException ex) {
                    cat.error("Cannot parse LOAD_ON_STARTUP value", ex);
                }
            }
            //gets the init param's information and adds it to the iparams hashtable :
            Hashtable iparams = new Hashtable();
            simpleLoad(compElem, iparams, Tags.INIT_PARAM, Tags.PARAM_NAME,
                    Tags.PARAM_VALUE);
            //Adds information gathered for servlet to the servletConfigs Hashtable.
            servletConfigs.put(servletName,
                    new NusuthServletConfig(iparams, this, servletName,
                            new ClassOrJsp(servletClass,
                                    false)));
            servletName2servletElement.put(servletName, compElem);
        } else {
            simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.JSP_FILE).nextElement();
            simpleElem2 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.SERVLET_NAME).nextElement();
            String servletName = simpleElem2.getContent();
            String servletClass = simpleElem1.getContent();
            //Gets the load on start up value and adds this information to the
            //startupServlets array.
            if (compElem.getSimpleChild(Tags.LOAD_ON_STARTUP).hasMoreElements()) {
                simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                        getSimpleChild(Tags.LOAD_ON_STARTUP).nextElement();
                try {
                    int lOnStartup = Integer.parseInt(simpleElem1.getContent());
                    if (lOnStartup > 0) {
                        ServletStartup startup = new ServletStartup(servletName,
                                lOnStartup);
                        startupServlets.add(startup);
                    }
                } catch (NumberFormatException ex) {
                    cat.error("Cannot parse LOAD_ON_STARTUP value", ex);
                }
            }
            //gets the init param's information and adds it to the iparams hashtable :
            Hashtable iparams = new Hashtable();
            simpleLoad(compElem, iparams, Tags.INIT_PARAM, Tags.PARAM_NAME,
                    Tags.PARAM_VALUE);
            //Adds information gathered for servlet to the servletConfigs Hashtable.
            servletConfigs.put(servletName,
                    new NusuthServletConfig(iparams, this, servletName,
                            new ClassOrJsp(servletClass,
                                    true)));
            servletName2servletElement.put(servletName, compElem);
        }
    }

    private void loadSessionConfig(CompositeNusuthWebAppElement config)
            throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        SimpleNusuthWebAppElement simpleElem1;
        //Gets the session configuration options.
        if (config.getCompositeChild(Tags.SESSION_CONFIG).hasMoreElements()) {
            compElem = (CompositeNusuthWebAppElement) config.
                    getCompositeChild(Tags.SESSION_CONFIG).nextElement();
            simpleElem1 = (SimpleNusuthWebAppElement) compElem.
                    getSimpleChild(Tags.SESSION_TIMEOUT).nextElement();
            try {
                defaultSessionLifetime
                        = 60 * Integer.parseInt(simpleElem1.getContent());
            } catch (NumberFormatException nfex) {
                //Logger.log("Cannot parse session timeout value", 1);
                //Logger.log(nfex, 1);
                cat.error("Cannot parse session timeout value", nfex);
            }
        }
    }

    private AuthenticationData processAuthentication(ProtocolAdapter adapter) {
        NusuthRequest request = adapter.getRequest();
        AuthenticationData authData = request.getAuthenticationData();
        if (authData != null) {
            Principal userPrincipal =
                    authData.getAuthType() == AuthenticationData.AUTH_METHOD_CERT
                    ? securityManager.login(authData.getCertificate())
                    : securityManager.login(authData.getUserName(),
                            authData.getEncodedPassword());
            if (userPrincipal != null) {
                NusuthSession session = (NusuthSession) getSession(request, true);
                session.setUser(userPrincipal);
                session.setAuthType(authData.getAuthType());
                if (authData.getAuthType() == AuthenticationData.AUTH_METHOD_FORM) {
                    adapter.restoreRequest();
                }
            } else {
                adapter.processError(ProtocolAdapter.ERROR_AUTHENTICATION_FAILED);
                adapter.getResponse().close();
            }
        }
        return authData;
    }

    private boolean isAccessToResourcePermitted(ProtocolAdapter adapter,
                                                boolean isAuthProcessed) {
        if (!securityManager.isEnabled()) {
            return true;
        }
        ResourceSecurityRecord resource = adapter.getResourceSecurityRecord();
        if (defaultAuthMethod == AuthenticationData.AUTH_METHOD_FORM
                && (resource.getPath2Resource().equals(formLoginPage)
                || resource.getPath2Resource().equals(formErrorPage)
                ))
            return true;
        NusuthRequest request = adapter.getRequest();
        String userName = getRemoteUser((HttpNusuthRequest) request);
        Principal user = userName == null ? null : new PrincipalImpl(userName);
        //    ResourceSecurityRecord resource = adapter.getResourceSecurityRecord();
        int result = securityManager.checkAccessRights(resource, request.isSecure(),
                user);
        switch (result) {
            case WebAppSecurityManager.ACCESS_GRANTED:
                return true;
            case WebAppSecurityManager.ACCESS_DENIED:
                cat.debug("Access denied for user \"" + user + "\" to \""
                        + resource.getMethod()
                        + (request.isSecure()
                        ? " https:/"
                        : " http:/") + getContextName() + resource.getPath2Resource());
                adapter.processError(ProtocolAdapter.ERROR_FORBIDDEN);
                adapter.getResponse().close();
                return false;
            case WebAppSecurityManager.ACCESS_AUTENTICATION_NEEDED:
                cat.debug("Autentication needed for user \"" + user + "\" to \""
                        + resource.getMethod()
                        + (request.isSecure()
                        ? " https:/"
                        : " http:/") + getContextName() + resource.getPath2Resource());
                if (isAuthProcessed)
                    adapter.processError(ProtocolAdapter.ERROR_AUTHENTICATION_FAILED);
                else
                    adapter.processError(ProtocolAdapter.ERROR_UNAUTHORIZED);
                adapter.getResponse().close();
                return false;
            default:
                cat.debug("Internal error in WebAppSecurityManager: checkAccessRights"
                        + "({\"" + resource.getPath2Resource() + "\", \""
                        + resource.getMethod() + "\"}, " + request.isSecure() + ", \""
                        + (user == null ? "<null>" : user.getName()) + "\") returns \""
                        + result + "\"");
                adapter.processError(ProtocolAdapter.ERROR_HANDLER_INTERNAL);
                adapter.getResponse().close();
                return false;
        }
    }

    /**
     *This method processes the incoming request. It searches for the needed
     * servlet and then runs it's service method.
     * @param adapter the protocol adapter object @see ProtocolAdapter.
     */
    public void processRequest(ProtocolAdapter adapter) {
        synchronized (this) {
            currentRequests++;
        }

        // added by igork
        AuthenticationData authData = processAuthentication(adapter);
        if (adapter.getResponse().isCommitted()
                || !isAccessToResourcePermitted(adapter, authData != null)) {
            synchronized (this) {
                currentRequests--;
            }
            return;
        }
        if (reloading) {
            adapter.processUnavailable(60, "Context reload");
            adapter.getResponse().close();
            return;
        }
        String servletName = adapter.getServletName();
        ResourceChain chain = adapter.getResourceChain();
        Thread.currentThread().setContextClassLoader(loader);
        Servlet servlet = null;
        try {
            if (!init2unav.containsKey(servletName) ||
                    (((Long) init2unav.get(servletName)).longValue()
                    <= System.currentTimeMillis()
                    && ((Long) init2unav.get(servletName)).longValue() != -1)) {
                servlet = findServlet(servletName);
                chain.setServlet(servlet);
                if (servlet == null) {
                    processErrorWithFilterchain("Servlet not found", 404, adapter);
                    adapter.getResponse().close();
                    throw new Exception("Servlet not found");
                }
            } else {
                processErrorWithFilterchain("Service unavailable permanently",
                        503, adapter);
                adapter.getResponse().close();
            }
        } catch (UnavailableException ex) {
            long time = (ex.getUnavailableSeconds() != -1)
                    ? (ex.getUnavailableSeconds() * 1000 + System.currentTimeMillis())
                    : -1;
            init2unav.put(servletName, new Long(time));
            processErrorWithFilterchain("Servlet initializing error", 500, adapter);
            adapter.getResponse().close();
        } catch (ServletException sex) {
            //Logger.log(sex, 1);
            cat.error("Cannot init servlet", sex);
            //      adapter.processError(ProtocolAdapter.ERROR_CANNOT_INIT_SERVLET);
            processErrorWithFilterchain("Servlet initializing error", 500, adapter);
            adapter.getResponse().close();
            synchronized (this) {
                currentRequests--;
            }
            return;
        } catch (Throwable ex) {
            //Logger.log(ex, 1);
            cat.error("Cannot find servlet", ex);
            //      adapter.processError(ProtocolAdapter.ERROR_SERVLET_NOT_FOUND);
            processErrorWithFilterchain("Servlet not found", 404, adapter);
            adapter.getResponse().close();
            synchronized (this) {
                currentRequests--;
            }
            return;
        }

        try {
            if (!service2unav.containsKey(servletName) ||
                    (((Long) service2unav.get(servletName)).longValue()
                    <= System.currentTimeMillis()
                    && ((Long) service2unav.get(servletName)).longValue() != -1)) {
                if (servlet instanceof SingleThreadModel) {
                    synchronized (servlet) {
                        chain.getFilterChain().doFilter(adapter.getRequest(),
                                adapter.getResponse());
                    }
                    Stack stack = (Stack) singleThreadServlets.get(servletName);
                    synchronized (stack) {
                        if (stack.size() < 10) {
                            stack.push(servlet);
                        }
                    }
                } else {
                    chain.getFilterChain().doFilter(adapter.getRequest(),
                            adapter.getResponse());
                }
            } else {
                if (((Long) service2unav.get(servletName)).longValue() != -1) {
                    int time = (int) (((Long) service2unav.get(servletName)).longValue()
                            - System.currentTimeMillis()) / 1000;
                    UnavailableException ex
                            = (UnavailableException) service2ex.get(servletName);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    ex.printStackTrace(ps);
                    adapter.processUnavailable(time, "<pre>" + baos.toString() + "<pre>");
                    adapter.getResponse().close();
                } else {
                    processError(adapter.getRequest(), adapter.getResponse(), 503, null,
                            "Service unavailable permanently", null,
                            adapter.getRequest().getRequestURI(),
                            adapter.getRequest().getServletPath());
                }
            }
            if (sessionWorkListeners.size() > 0
                    && getSession(adapter.getRequest(), false) != null) {
                for (int i = 0; i < sessionWorkListeners.size(); i++) {
                    HttpSession session = (HttpSession) getSession(adapter.getRequest(),
                            false);
                    String oldId = session.getId();
                    boolean changed = ((SessionWorkOverListener)
                            sessionWorkListeners.get(i)).changeId(oldId, containerId);
                    if (changed) {
                        adapter.getResponse().setSessionID(session.getId(),
                                adapter.getRequest());
                        oldId = session.getId();
                    }
                    try {
                        ((SessionWorkOverListener) sessionWorkListeners.get(i)).
                                onSessionWorkOver(oldId);
                    } catch (SocketException e) {
                        ((SessionWorkOverListener) sessionWorkListeners.get(i)).
                                changeRemoteId(oldId);
                        adapter.getResponse().setSessionID(session.getId(),
                                adapter.getRequest());
                    }
                }
            }
            adapter.getResponse().close();
        } catch (UnavailableException ex) {
            long time = (ex.getUnavailableSeconds() != -1)
                    ? (ex.getUnavailableSeconds() * 1000 + System.currentTimeMillis())
                    : -1;
            if (time == -1) {
                if (servlet instanceof SingleThreadModel) {
                    synchronized (servlet) {
                        servlet.destroy();
                    }
                } else {
                    servlet.destroy();
                }
            }
            service2unav.put(servletName, new Long(time));
            service2ex.put(servletName, ex);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            ex.printStackTrace(ps);
            adapter.processUnavailable(ex.getUnavailableSeconds(),
                    "<pre>" + baos.toString() + "<pre>");
            adapter.getResponse().close();
        } catch (Throwable ex) {
            processError(adapter.getRequest(), adapter.getResponse(), 500,
                    ex.getClass(), "Servlet processing error", ex,
                    adapter.getRequest().getRequestURI(),
                    adapter.getRequest().getServletPath());
            adapter.getResponse().close();
        } finally {
            synchronized (this) {
                currentRequests--;
            }
            /*if (useJTA)
             EnvContext.unsetEnvContext();*/
        }
    }

    private void processErrorWithFilterchain(String message, int code,
                                             ProtocolAdapter adapter) {
        try {
            ResourceChain chain = adapter.getResourceChain();
            ServletRequest req = adapter.getRequest();
            req.setAttribute("javax.servlet.error.status_code", new Integer(code));
            req.setAttribute("javax.servlet.error.message", message);
            chain.setServlet(new ErrorServlet());
            chain.getFilterChain().doFilter(req, adapter.getResponse());
        } catch (Exception e) {
            cat.error("Error occured while processing FilterChain", e);
            processError(adapter.getRequest(), adapter.getResponse(), 500,
                    e.getClass(), "Error occured while processing FilterChain",
                    e, adapter.getRequest().getRequestURI(),
                    adapter.getRequest().getServletPath());
        }
    }

    /**
     * This method process error if occured.
     * @param request ServletRequest.
     * @param response ServletResponse.
     * @param errorCode Code of error.
     * @param exceptionType Type of exception or null.
     * @param message Message to send.
     * @param exception Exception that occured or null.
     * @param req_uri Request URI.
     */
    public void processError(ServletRequest request, ServletResponse response,
                             int errorCode, Class exceptionType, String message,
                             Throwable exception, String req_uri,
                             String servletPath) {
        boolean alreadyError
                = request.getAttribute("javax.servlet.error.status_code") != null;
        if (alreadyError) {
            cat.error("Cannot process request on error-page", exception);
            request.setAttribute("javax.servlet.error.additional", "");
            try {
                if (response.isCommitted()) {
                    getNamedDispatcher("_nusuth_error_servlet").include(request, response);
                } else {
                    getNamedDispatcher("_nusuth_error_servlet").forward(request, response);
                }
            } catch (Exception e) {
                cat.debug("Error occured while processing error", e);
                return;
            }
            return;
        }
        request.removeAttribute("javax.servlet.error.status_code");
        request.removeAttribute("javax.servlet.error.exception_type");
        request.removeAttribute("javax.servlet.error.message");
        request.removeAttribute("javax.servlet.error.exception");
        request.removeAttribute("javax.servlet.error.request_uri");
        request.removeAttribute("javax.servlet.error.servlet_name");
        Integer code = new Integer(errorCode);
        String[] servletPathAndName = findServletPath(servletPath);
        request.setAttribute("javax.servlet.error.servlet_name",
                servletPathAndName[1]);
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            try {
                response.flushBuffer();
            } catch (IOException e) {
                cat.error("Cannot flush buffer");
            }
        }
        if (exceptionType != null) {
            request.setAttribute("javax.servlet.error.exception_type", exceptionType);
        }
        if (exception != null) {
            if (exception instanceof ServletException
                    && ((ServletException) exception).getRootCause() != null) {
                exception = ((ServletException) exception).getRootCause();
                request.setAttribute("javax.servlet.error.exception_type",
                        exception.getClass());
            }
            request.setAttribute("javax.servlet.error.exception", exception);
//      message = message + " : "+exception.getMessage();
            cat.error(message, exception);
        }
        request.setAttribute("javax.servlet.error.status_code", code);
        request.setAttribute("javax.servlet.error.request_uri", req_uri);
        request.setAttribute("javax.servlet.error.message", message);
        boolean found = false;
        String error_code = null;
        String exception_type = null;
        String location = null;
        if (errorMapings.size() > 0) {
            ListIterator iterator = null;
            iterator = errorMapings.listIterator(0);
            try {
                while (!found && iterator.hasNext()) {
                    CompositeNusuthWebAppElement compElem
                            = (CompositeNusuthWebAppElement) iterator.next();
                    Enumeration simpleElem1 = compElem.getSimpleChild("error-code");
                    Enumeration simpleElem2 = compElem.getSimpleChild("exception-type");
                    Enumeration simpleElem3 = compElem.getSimpleChild("location");
                    if (simpleElem1.hasMoreElements()) {
                        error_code = ((SimpleNusuthWebAppElement) simpleElem1.nextElement()).
                                getContent();
                    }
                    if (simpleElem2.hasMoreElements()) {
                        exception_type
                                = ((SimpleNusuthWebAppElement) simpleElem2.nextElement()).
                                getContent();
                    }
                    location = ((SimpleNusuthWebAppElement) simpleElem3.nextElement()).
                            getContent();
                    if ((error_code != null)) {
                        if (error_code.equals(code.toString())) {
                            found = true;
                        }
                    }
                    if (exceptionType != null && exception != null) {
                        Class cl = exception.getClass();
                        for (; cl != null; cl = cl.getSuperclass()) {
                            if (exception_type != null
                                    && exception_type.equals(cl.getName())) {
                                found = true;
                            }
                        }
                    }
                }
            } catch (DeploymentException ex) {
                cat.error("Error occured while processing error", ex);
            }
        }
        if (found) {
            try {
                if (response.isCommitted()) {
                    getRequestDispatcher(location).include(request, response);
                } else {
                    getRequestDispatcher(location).forward(request, response);
                }
            } catch (Exception e) {
                cat.debug("Error occured while processing error", e);
                return;
            }
        } else {
            try {
                if (response.isCommitted()) {
                    getNamedDispatcher("_nusuth_error_servlet").include(request, response);
                } else {
                    getNamedDispatcher("_nusuth_error_servlet").forward(request, response);
                }
            } catch (Exception e) {
                cat.debug("Error occured while processing error", e);
                return;
            }
        }
    }

    /**
     *This method returnes the context attribute with the given name.
     * @param name the attribute name.
     * @return the attribute object.
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     *This method returns names of the context attributes.
     * @return the Enumeration that consists of Strings - the attribute names.
     */
    public Enumeration getAttributeNames() {
        return attributes.keys();
    }

    /**
     *This method returnes ServletContext that is associated with the given uri
     * @depricated always returns NULL.
     * @param uriPath URI
     * @return associated servlet context.
     */
    public ServletContext getContext(String uriPath) {
        return null;
    }

    /**
     * @param name the init parameter name.
     * @return the init parameter value.
     */
    public String getInitParameter(String name) {
        return (String) initParameters.get(name);
    }

    /** @return the Enumeration of Strings - the init parameters names. */
    public Enumeration getInitParameterNames() {
        return initParameters.keys();
    }

    /** @return the major version number. */
    public int getMajorVersion() {
        return 2;
    }

    /**
     * @peram file the file name
     * @return the mime type of this file or null if this file name doesn't
     * have extension.
     */
    public String getMimeType(String file) {
        int c = file.lastIndexOf(".");
        if (c < 0) {
            return null;
        }
        return (String) mimeTypes.get(file.substring(c + 1, file.length()));
    }

    /** @return the minor version number. */
    public int getMinorVersion() {
        return 3;
    }

    /**
     *This method takes a servlet name known to the NusuthContext. If a servlet
     * is known to the NusuthContext, by the given name, then this method wrapes
     * it with the RequestDispatcher and returns.
     * @param name servlet name.
     * @return the servlet wrapped with the object of
     * the @see NusuthRequestDispatcher class type, or null if there is no servlet
     * that is associated with the given name.
     */
    public RequestDispatcher getNamedDispatcher(String name) {
        Servlet servlet = null;
        try {
            servlet = findServlet(name);
        } catch (Exception ex) {
            //log("", ex);
            cat.info("Cannot find servlet" + name, ex);
        }
        if (servlet == null) {
            return null;
        }
        LinkedList list = (LinkedList) filterForDispatchers.get(name);
        if (list == null) {
            list = new LinkedList();
        }
        list.add(servlet);
        NusuthFilterChain chain = new NusuthFilterChain(list);
        return new NusuthRequestDispatcher(chain, null, name,
                true);
    }

    public String getRealPath(String path) {
        File file = new File(docBase, path);
        try {
            return file.getCanonicalPath();
        } catch (IOException ioex) {
            //Logger.log(ioex, 0);
            cat.info("Cannot get real path", ioex);
            return null;
        }
    }

    /**
     *This method takes a String argument described a path within the scope
     * of the NusuthContext
     * This path must be relative to the root of the NusuthContext.This path is
     * used to look up a servlet,
     * wrap it with the RequestDispatcher and return it.
     * @param name servlet path in the NusuthContext.
     * @return the servlet wrapped with the object of the RequestDispatcher class
     * type, or null if no servlet is
     * associated with the given name.
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        Servlet servlet = null;
        String[] names = null;
        Object[] chains = null;
        String query = "";
        if (path.indexOf('?') != -1) {
            query = path.substring(path.indexOf('?'), path.length());
            path = path.substring(0, path.indexOf('?'));
        }
        try {
            names = findServletPath(path);
            chains = findResourceChain(path);
            servlet = findServlet(names[1]);
        } catch (Exception ex) {
            //log("", ex);
            cat.info("Cannot find servlet", ex);
        }
        if (servlet == null) {
            return null;
        }
        if (chains[1] == null) {
            LinkedList list = new LinkedList();
            list.add(servlet);
            return new NusuthRequestDispatcher(new NusuthFilterChain(list),
                    names[0], path + query, false);
        } else {
            ((ResourceChain) chains[1]).setServlet(servlet);
            return new NusuthRequestDispatcher(
                    ((ResourceChain) chains[1]).getFilterChain(),
                    names[0], path + query, false);
        }
    }

    public URL getResource(String path) {
        String realPath = getRealPath(path);
        if (realPath == null) {
            return null;
        } else {
            try {
                File file = new File(realPath);
                return (file.exists() ? file.toURL() : null);
            } catch (java.net.MalformedURLException me) {
                //Logger.log(me, 0);
                cat.warn("", me);
                return null;
            }
        }
    }

    public InputStream getResourceAsStream(String path) {
        String realPath = getRealPath(path);
        if (realPath == null) {
            return null;
        } else {
            try {
                return new FileInputStream(realPath);
            } catch (FileNotFoundException fnfe) {
                //Logger.log(fnfe, 0);
                cat.info("", fnfe);
                return null;
            }
        }
    }

    /**
     *This method returns the server info.
     * @return the server info
     */
    public String getServerInfo() {
        return "Nusuth/1.0b";
    }

    /**
     *This method returns the servlet class object with the given name.
     * @deprecated In this version always returns null.
     * @param name the servlet name.
     * @return servlet class object.
     */
    public Servlet getServlet(String name) {
        return null;
    }

    /**
     *This method returns the Enumeration of the servlet names of this context.
     * @deprecated In this version always returns null.
     * @return the Enumeration object which contains Strings - the servlet names.
     */
    public Enumeration getServletNames() {
        return null;
    }

    /**
     *This method returnes Enumeration of servlet class objects associated with
     * this context.
     * @deprecated In this version always returns null.
     * @return Enumeration of Servlet class objects.
     */
    public Enumeration getServlets() {
        return null;
    }

    /**
     *This method is used to log information.
     * @param exception exception we wont add to log file.
     * @param message message we wont add to log file.
     */

    public void log(Exception exception, String message) {
        log(message, exception);
    }


    /**
     *This method is used to log information.
     * @param message a message this method will print to the standard output
     * stream.
     */

    public void log(String message) {
        logCat.info(message);
    }


    /**
     *This method is used to log information.
     * @param message message this method will print to the standard output
     * stream.
     * @param error error this method will print to the error output stream.
     */

    public void log(String message, Throwable error) {
        logCat.info(message, error);
    }


    /**
     *This method removes attribute object with the given name from this
     * JBirdServletContext class type context.
     * @param the attribute name.
     */
    public void removeAttribute(String name) {
        Object obj = attributes.remove(name);
        if (obj != null) {
            for (int i = 0; i < contextAttrlisteners.size(); i++) {
                ((ServletContextAttributesListener) contextAttrlisteners.get(i)).
                        attributeRemoved(new ServletContextAttributeEvent(this,
                                name, obj));
            }
        }
    }

    /**
     *This method adds the given attribute object with the given name to this
     * JBirdServletContext class type context.
     * @param name the attribute name.
     * @param value the attribute object.
     */
    public void setAttribute(String name, Object value) {
        if ((attributes.put(name, value)) != null) {
            for (int i = 0; i < contextAttrlisteners.size(); i++) {
                ((ServletContextAttributesListener) contextAttrlisteners.get(i)).
                        attributeReplaced(new ServletContextAttributeEvent(this, name,
                                value));
            }
        } else {
            for (int i = 0; i < contextAttrlisteners.size(); i++) {
                ((ServletContextAttributesListener) contextAttrlisteners.get(i)).
                        attributeAdded(new ServletContextAttributeEvent(this,
                                name, value));
            }
        }
    }

    public int getDefaultAuthType() {
        return defaultAuthMethod;
    }

    /**
     * @return name of authentication type if this
     * request contains authentication data, or <code>null</code> if not.
     * @see AuthenticationData#AUTH_METHOD_BASIC_NAME
     * @see AuthenticationData#AUTH_METHOD_CERT_NAME
     * @see AuthenticationData#AUTH_METHOD_DIGEST_NAME
     * @see AuthenticationData#AUTH_METHOD_FORM_NAME
     */
    public String getAuthType(NusuthRequest request) {
        NusuthSession session = (NusuthSession) getSession(request, false);
        if (session != null)
            return AuthenticationData.int2methodName(session.getAuthType());
        else
            return null;
    }

    /**
     * @return <code>int</code> that represent authentication type if this request
     * contains authentication data, or -1 if not.
     * @see AuthenticationData#AUTH_METHOD_BASIC
     * @see AuthenticationData#AUTH_METHOD_CERT
     * @see AuthenticationData#AUTH_METHOD_DIGEST
     * @see AuthenticationData#AUTH_METHOD_FORM
     */
    public int getAuthTypeInt(NusuthRequest request) {
        NusuthSession session = (NusuthSession) getSession(request, false);
        if (session != null)
            return session.getAuthType();
        else
            return -1;
    }

    public String getRemoteUser(NusuthRequest request) {
        NusuthSession session = (NusuthSession) getSession(request, false);
        if (session != null) {
            Principal user = session.getUser();
            return user == null ? null : user.getName();
        } else
            return null;
    }

    public Principal getUserPrincipal(NusuthRequest request) {
        NusuthSession session = (NusuthSession) getSession(request, false);
        return session == null ? null : session.getUser();
    }

    public boolean isUserInRole(NusuthRequest request, String role) {
        NusuthSession session = (NusuthSession) getSession(request, false);
        return session == null || securityManager == null ?
                false :
                securityManager.isUserInRole(request.getServletPath(),
                        session.getUser(), role);
    }

    /**
     *This method returnes session bound up with the client, depending on the
     * client request.
     * @param request client's request.
     * @param create if True then this method will add new session if there is no
     * session bound up with this client.
     * @return session.
     */
    public HttpSession getSession(NusuthRequest request, boolean create) {
        String id = request.getCurrentSessionId();
        //System.out.println("REQUESTED SESSION ID: "+id);
        if (id == null) {
            // If this is a request from the new client.
            if (create) {
                //Creating new session.
                NusuthSession session = (NusuthSession) sessionManager.createSession();
                session.setMaxInactiveInterval(defaultSessionLifetime);
//        sessions.put(session.getId(), session);
                if (request.getSessionCreationListener() != null) {
                    request.getSessionCreationListener().sessionCreated(session, request);
                }
                request.setCurrentSessionId(session.getId());
                return session;
            }
        } else {
            NusuthSession session = (NusuthSession) sessionManager.getSession(id);
            if (session != null && session.isValid()) {
                if (request.getRequestedSessionId() != null
                        && request.getRequestedSessionId().equals(session.getId())) {
                    session.touch();
                }
                return session;
            } else {
                if (create) {
                    //Creating new session.
                    session = (NusuthSession) sessionManager.createSession();
                    session.setMaxInactiveInterval(defaultSessionLifetime);
//          sessions.put(session.getId(), session);
                    if (request.getSessionCreationListener() != null) {
                        request.getSessionCreationListener().sessionCreated(session,
                                request);
                    }
                    request.setCurrentSessionId(session.getId());
                    return session;
                }
            }
        }
        return null;
    }

    /**
     * This method returns session timeout.
     * @return Session timeout for this context.
     */
    public int getSessionTimeOut() {
        return defaultSessionLifetime;
    }

    /**
     *This method is used to check if session bound up with this request's client
     * is Valid or not.
     * @param request request.
     * @return True if there is a valid session bound up with this request's
     * client, otherwise False.
     */
    public boolean isSessionValid(NusuthRequest request) {
        String id = request.getRequestedSessionId();
        if (id == null) {
            return false;
        }
        NusuthSession session = (NusuthSession) sessionManager.getSession(id);
        return session != null && session.isValid();
    }

    public void registerSessionWorkOverListener(SessionWorkOverListener listener) {
        sessionWorkListeners.add(listener);
    }

    public void deleteSessionWorkOverListener(SessionWorkOverListener listener) {
        sessionWorkListeners.remove(listener);
    }

    public void setSessionManager(SessionManager manager) {
        this.sessionManager = manager;
    }

    /**
     * This method returns the value of "session-backup" element from container's
     * config. If there is no such element, it returns "never"
     * @return value of "session-backup" element from container's config
     */
    public String getSessionBackup() {
        return sessionBackup;
    }

    public void setSessionBackup(String sessionBackup) {
        this.sessionBackup = sessionBackup;
        if (sessionManager instanceof DefaultSessionManager) {
            ((DefaultSessionManager) sessionManager).sessionBackupChanged();
        }
    }

    /**
     * This method causes "Hard" shutdown of this NusuthContext class.
     * It simply calles the shutdownContext method of this class.
     */
    protected void finalize() {
        if (!closed) {
            shutdownContext(false);
        }
    }

    /**
     * @param path the context path to the servlet.
     * @return array where first element is a mapped servlet path and second is
     * a servlet name, returnes null if there is no
     * appropriate servlet on the given path .
     */
    public String[] findServletPath(String path) {
        String servletPath = path;
        String servletName = (String) servletMappings.get(servletPath);
        if (servletName == null) {
            int f = path.length();
            int k;
            while (f >= 0) {
                servletPath = path.substring(0, f);
                servletName = (String) servletMappings.get(servletPath.length() == 1
                        ? servletPath
                        : servletPath + "/*");
                if (servletName != null) {
                    break;
                }
                k = path.lastIndexOf("/", f - 1);
                f = k;
            }
            if (servletName == null) {
                f = path.lastIndexOf(".", path.length());
                if (f != -1) {
                    String tempPath = "*." + path.substring(f + 1, path.length());
                    int index = -1;
                    if ((index = tempPath.indexOf('/')) != -1) {
                        tempPath = tempPath.substring(0, index);
                    }
//        servletName = (String)servletMappings.
//                get("*." + path.substring(f + 1, path.length()));
                    servletName = (String) servletMappings.
                            get(tempPath);
//          servletPath = path;
                    if (path.indexOf('/', f + 1) != -1) {
                        servletPath = path.substring(0, path.indexOf('/', f + 1));
                    } else {
                        servletPath = path;
                    }
                }
            }
        }
        if (servletName == null) {
            servletName = (String) servletMappings.get("/");
            servletPath = "";
        }
        if (servletPath.length() == 1) {
            servletPath = "";
        }
        String[] res = {servletPath, servletName};
        return servletName == null ? null : res;
    }

    public Object[] findResourceChain(String path) {
        String[] servPath = findServletPath(path);
        ResourceChain chain1 = (ResourceChain) filterMappings.get(path);
        ResourceChain chain2 = null;
        String url = path;
        if (chain1 == null) {
            int f = url.length();
            int k;
            while (f >= 0) {
                url = url.substring(0, f);
                chain1 = (ResourceChain) filterMappings.get(url.length() == 1
                        ? url : url + "/*");
                if (chain1 != null) {
                    break;
                }
                k = url.lastIndexOf("/", f - 1);
                f = k;
            }
        } else if (chain1.isDefaultServlet()) {
            int f = path.lastIndexOf(".", path.length());
            if (f != -1) {
                String tempPath = "*." + path.substring(f + 1, path.length());
                int index = -1;
                if ((index = tempPath.indexOf('/')) != -1) {
                    tempPath = tempPath.substring(0, index);
                }
                chain2 = (ResourceChain) filterMappings.
                        get(tempPath);
//      chain2 = (ResourceChain)filterMappings.
//              get("*." + path.substring(f + 1, path.length()));
            }
            if (chain2 != null && !chain2.isDefaultServlet()) {
                url = path;
                if (url.length() == 1)
                    url = "";
                ResourceChain resultChain = merge(chain1, chain2);
                Object[] result = {servPath[0], resultChain};
                return result;
            }
        }
        if (chain1 == null) {
            chain1 = (ResourceChain) filterMappings.get("/");
        } else if (chain1.isDefaultServlet()) {
            int f = path.lastIndexOf(".", path.length());
            if (f != -1) {
                String tempPath = "*." + path.substring(f + 1, path.length());
                int index = -1;
                if ((index = tempPath.indexOf('/')) != -1) {
                    tempPath = tempPath.substring(0, index);
                }
                chain2 = (ResourceChain) filterMappings.
                        get(tempPath);
//      chain2 = (ResourceChain)filterMappings.
//              get("*." + path.substring(f + 1, path.length()));
            }
            if (chain2 != null && !chain2.isDefaultServlet()) {
                url = path;
                if (url.length() == 1)
                    url = "";
                ResourceChain resultChain = merge(chain1, chain2);
                Object[] result = {servPath[0], resultChain};
                return result;
            }
        }
        int f = path.lastIndexOf(".", path.length());
        if (f != -1) {
            String tempPath = "*." + path.substring(f + 1, path.length());
            int index = -1;
            if ((index = tempPath.indexOf('/')) != -1) {
                tempPath = tempPath.substring(0, index);
            }
            chain2 = (ResourceChain) filterMappings.
                    get(tempPath);
//    chain2 = (ResourceChain)filterMappings.
//            get("*." + path.substring(f + 1, path.length()));
        }
        if (chain1 == null && chain2 == null) {
            return null;
        } else if (chain1 != null && chain2 == null) {
            if (url.length() == 1)
                url = "";
            Object[] result = {servPath[0], chain1};
            return result;
        } else if (chain1 == null && chain2 != null) {
            if (url.length() == 1)
                url = "";
            Object[] result = {servPath[0], chain2};
            return result;
        }
        url = path;
        if (url.length() == 1)
            url = "";
        ResourceChain rc = merge(chain1, chain2);
        Object[] result = {servPath[0], rc};
        return result;
    }

    private ResourceChain merge(ResourceChain chain1, ResourceChain chain2) {
        ResourceChain result = new ResourceChain();
        result.addAll(chain1);
        result.addAll(chain2);
        result.setServlet(chain2.getServlet());
        result.setServletName(chain2.getServletName());
        result.setServletPath(chain2.getServletPath());
        return result;
    }

    /**
     *This method searches and loads the servlet class with the given name.
     * @param servletName servlet name.
     * @return loaded servlet.
     * @exception Exception if any error occures while searching and loading
     * class with the given name or the is no servlet
     * class with the given name.
     */
    private Servlet findServlet(String servletName) throws Exception {
        if (singleThreadServletNames.contains(servletName)) {
            if (singleThreadServlets.get(servletName) != null) {
                Stack stack = (Stack) singleThreadServlets.get(servletName);
                synchronized (stack) {
                    if (!stack.empty()) {
                        return (Servlet) stack.pop();
                    }
                }
            }
        }
        Servlet servlet = (Servlet) servlets.get(servletName);
        if (servlet == null) {
            NusuthServletConfig scon
                    = (NusuthServletConfig) servletConfigs.get(servletName);
            if (scon != null) {
                String className = scon.getServletClass();
                synchronized (className) {
                    if (servlets.get(servletName) != null) {
                        return (Servlet) servlets.get(servletName);
                    }
                    //Logger.log("Start servlet loading: " + className, 0);
                    cat.info("Start servlet loading: " + className);
                    if (scon.isForJsp()) {
                        if (servletName.equals("_nusuth_dir_servlet")) {
                            File jspFile = new File(workDir + File.separator + "DirServlet.jsp");
                            try {
                                InputStream is = getClass().getClassLoader().
                                        getResourceAsStream("com/azoft/nusuth/core/"
                                        + "DirServlet.jsp");
                                OutputStream os = new BufferedOutputStream(new FileOutputStream(
                                        workDir + File.separator + "DirServlet.jsp"));
                                byte[] buf = new byte[1024];
                                int readed = 0;
                                while ((readed = is.read(buf, 0, buf.length)) > -1) {
                                    os.write(buf, 0, readed);
                                }
                                os.close();
                                JspLoader jspLoader = new JspLoader(workDir, this, rep);
                                servlet = jspLoader.loadJsp(className, jspFile, false);
                            } catch (Throwable t) {
                                cat.warn("Cannot load jsp for directory listing - "
                                        + "using precompiled version");
                                servlet = loader.loadServlet(dirListingClassName);
                            } finally {
                                if (jspFile.exists()) {
                                    jspFile.delete();
                                }
                            }
                        } else {
                            JspLoader jspLoader = new JspLoader(workDir, this, rep);
                            String cname = className.replace('/', File.separatorChar);
                            File jspFile = null;
                            if (cname.startsWith(File.separator)) {
                                jspFile = new File(docBase + cname);
                            } else {
                                jspFile = new File(docBase + File.separator + cname);
                            }
                            servlet = jspLoader.loadJsp(className, jspFile, false);
                        }
                    } else {
                        servlet = loader.loadServlet(className);
                    }
                    //Logger.log(" done", 0);
                    cat.info(" done");
                    //Logger.log("Init servlet " + className, 0);
                    cat.info("Init servlet " + className);
                    try {
                        servlet.init(scon);
                    } catch (Exception e) {
                        cat.error("Cannot init servlet " + servletName, e);
                        throw new Exception("Cannot init servlet " + servletName);
                    }
                    //Logger.log("done", 0);
                    cat.info(" done");
                    if (!(servlet instanceof SingleThreadModel)) {
                        servlets.put(servletName, servlet);
                    } else {
                        if (single2instance.get(servletName) == null) {
                            single2instance.put(servletName, new Integer(1));
                        } else if (((Integer) single2instance.get(servletName)).intValue()
                                == 10) {
                            servlets.put(servletName, servlet);
                        } else {
                            single2instance.put(servletName,
                                    new Integer(((Integer) single2instance.
                                    get(servletName)).intValue() + 1));
                        }
                    }
                }
            } else {
                throw new Exception("Servlet " + servletName + " not found");
            }
        }
        if (!singleThreadServletNames.contains(servletName)) {
            if (servlet != null && servlet instanceof SingleThreadModel) {
                synchronized (singleThreadServlets) {
                    singleThreadServletNames.add(servletName);
                    singleThreadServlets.put(servletName, new Stack());
                }
            }
        }
        return servlet;
    }

    /**
     * This method return true if containers is shutting down now else false.
     * @return true if containers is shutting down now else false.
     */
    public boolean isShuttingDown() {
        return shutdown;
    }

    /**
     *This method is used to shutdown servlet context. There are two variants:
     * soft shutdown when NusuthContext class waits while all requests will be
     * processed and "Hard" shutdown , when all servlets are deleted immediately.
     * @param soft True if this is a "Soft" variant of context shutdown, False
     * in other case.
     */
    public void shutdownContext(boolean soft) {
        //Logger.log("Start of shutdown context "+contextName, 1);
        cat.info("Start of shutdown context " + contextName);
        shutdown = true;
        if (soft) {
            // Will wait until all requests will be processed.
            while (currentRequests > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException iex) {
                    cat.info("Waiting for current requests interrupted");
                }
            }
        }
        synchronized (filters) {
            Enumeration enum = filters.keys();
            String filterName;
            Filter filter;
            while (enum.hasMoreElements()) {
                filterName = (String) enum.nextElement();
                filter = (Filter) filters.get(filterName);
                Thread killerThread = new KillerThread(filter, filterName);
                killerThread.start();
                try {
                    killerThread.join(killingTimeout);
                } catch (InterruptedException ie) {
                    cat.error(ie);
                }
                if (killerThread.isAlive()) {
                    cat.warn("Cannot destroy filter " + filterName + " in "
                            + killingTimeout / 1000 + " sec.");
                }
            }
        }
        /// Will destroy all servlets.
        synchronized (servlets) {
            Enumeration enum = servlets.keys();
            String servletName;
            Servlet servlet;
            while (enum.hasMoreElements()) {
                servletName = enum.nextElement().toString();
                servlet = (Servlet) servlets.get(servletName);
                Thread killerThread = new KillerThread(servlet, servletName);
                killerThread.start();
                try {
                    killerThread.join(killingTimeout);
                } catch (InterruptedException ie) {
                    cat.error(ie);
                }
                if (killerThread.isAlive()) {
                    cat.warn("Cannot destroy servlet " + servletName + " in "
                            + killingTimeout / 1000 + " sec.");
                }
            }
        }
        Enumeration enum = sessionManager.getSessionsKeys();
        while (enum.hasMoreElements()) {
            String id = (String) enum.nextElement();
            sessionManager.removeSession(id);
        }
        for (int i = contextListeners.size() - 1; i >= 0; i--) {
            ((ServletContextListener) contextListeners.get(i)).
                    contextDestroyed(new ServletContextEvent(this));
        }
        loader.close();
        //Logger.log("End of shutdown context "+contextName, 1);
        cat.info("End of shutdown context " + contextName);
        closed = true;
        if (jspLoader != null) {
            jspLoader.releaseValidators();
        }
    }

    public String getDocBase() {
        return docBase;
    }

    private void loadWelcomeFilesList(CompositeNusuthWebAppElement settings,
                                      List destination) {
        try {
            Enumeration en = settings.getCompositeChild("welcome-file-list");
            CompositeNusuthWebAppElement wf;
            if (en.hasMoreElements()) {
                wf = (CompositeNusuthWebAppElement) en.nextElement();
                en = wf.getSimpleChild("welcome-file");
                while (en.hasMoreElements()) {
                    destination.add(((SimpleNusuthWebAppElement) en.nextElement()).
                            getContent());
                }
            }
            if (commonConfig != null) {
                en = commonConfig.getCompositeChild("welcome-file-list");
                if (en.hasMoreElements()) {
                    wf = (CompositeNusuthWebAppElement) en.nextElement();
                    en = wf.getSimpleChild("welcome-file");
                    while (en.hasMoreElements()) {
                        destination.add(((SimpleNusuthWebAppElement) en.nextElement()).
                                getContent());
                    }
                }
            }
        } catch (DeploymentException de) {
            if (cat.isInfoEnabled()) cat.info("Deployment error", de);
        }
    }

    public List getWelcomeFiles() {
        return welcomeFilesList;
    }

    public Hashtable getMimeTypes() {
        return mimeTypes;
    }

    public ServletLoader getServletLoader() {
        return loader;
    }

    public String getContextName() {
        return contextName;
    }

    public static void setCommonConfig(CompositeNusuthWebAppElement cConfig) {
        commonConfig = cConfig;
    }

    public static CompositeNusuthWebAppElement getCommonConfig() {
        return commonConfig;
    }

    public JspLoader getJspLoader() throws IOException {
        if (jspLoader == null || contextWasChanged) {
            //      TagLibraryRepository repository = new TagLibraryRepository(this);
            Enumeration enum = taglibs.keys();
            while (enum.hasMoreElements()) {
                String key = (String) enum.nextElement();
                String value = (String) taglibs.get(key);
                rep.addDefinedTagLib(key, value);
            }
            jspLoader = new JspLoader(workDir, this, rep);
            contextWasChanged = false;
            rep.registerTagLibraryListener(jspLoader);
        }
        return jspLoader;
    }

    public String getWorkDir() {
        return workDir;
    }

    public Set getResourcePaths(String path) {
        if (!path.startsWith("/"))
            throw new IllegalStateException("Parameter \"path\" must starts with"
                    + " \'/\' character");
        Set set = new HashSet();
        File file = new File(docBase, path);
        if (!file.exists())
            return null;
        File[] list = file.listFiles();
        if (list == null || list.length == 0)
            return null;
        String resource = null;
        int len = docBase.length();
        for (int i = 0; i < list.length; i++) {
            resource = (list[i].getAbsolutePath()).substring(len);
            resource = resource.replace(File.separatorChar, '/')
                    + (list[i].isDirectory() ? "/" : "");
            set.add(resource);
        }
        return set;
    }

    public String getServletContextName() {
        return displayName;
    }

    public CustomTagFactory getCustomTagFactory() {
        if (tagFactory == null) {
            tagFactory = new CustomTagFactory(getServletLoader(), rep);
            rep.registerTagLibraryListener(tagFactory);
        }
        return tagFactory;
    }

    public int getCurrentRequestsCount() {
        return currentRequests;
    }

    public int getCurrentSessionsCount() {
        return sessionManager.getCurrentSessionSize();
    }

    public LinkedList getSessionListeners() {
        return sessionListeners;
    }

    public LinkedList getSessionAttrListeners() {
        return sessionAttrListeners;
    }

    /** This class is used to filter jar  and zip files. */
    class JarZipFilenameFilter implements FilenameFilter {
        /** Simple constructor. */
        JarZipFilenameFilter() {
            super();
        }

        /**
         *The filtering method.
         * @param name the file name.
         * @return true if the given name != null and it have .zip or .jar
         * extension, otherwise False.
         */
        public boolean accept(File dir, String name) {
            return name != null && (name.endsWith(".zip") || name.endsWith(".jar"));
        }
    }


    /** This class is used to store servlet name and its startup status.
     * This classes are comparable.
     *
     */
    class ServletStartup implements Comparable {
        String servletName;
        int loadOnStartup;

        /**
         *Constructor.
         * @param servletName servlet name.
         * @param loadOnStartup the start up priority value.
         */
        ServletStartup(String servletName, int loadOnStartup) {
            this.servletName = servletName;
            this.loadOnStartup = loadOnStartup;
        }

        /** Method that compares two objects of this class. */
        public int compareTo(Object x) {
            return loadOnStartup - ((ServletStartup) x).loadOnStartup;
        }
    }

    public StrBuffer getFormLoginPage() {
        return formLoginPage;
    }

    public StrBuffer getFormErrorPage() {
        return formErrorPage;
    }

    public String getAuthRealm() {
        return authRealm;
    }

    public void setContainerId(String id) {
        this.containerId = id;
    }

    public void setDistributorId(String id) {
        this.distributorId = id;
    }

    public String getDistributorId() {
        return distributorId;
    }

    public String getContainerId() {
        return containerId;
    }

    private String convertName(String name) {
        StringBuffer sb = new StringBuffer();
        char tmp;
        for (int i = 0; i < name.length(); i++) {
            tmp = name.charAt(i);
            if ((tmp >= 'a' && tmp <= 'z') || (tmp >= 'A' && tmp <= 'Z') ||
                    (tmp >= '0' && tmp <= '9') || tmp == '_') {
                sb.append(tmp);
            } else {
                sb.append("d" + (int) tmp);
            }
        }
        return sb.toString();
    }

    class KillerThread extends Thread {

        Servlet servlet;
        Filter filter;
        String servletName;
        String destroyingName;

        KillerThread(Servlet servlet, String servletName) {
            super("Killer of " + contextName + "/" + servletName);
            setDaemon(true);
            this.servlet = servlet;
            this.servletName = servletName;
            destroyingName = "servlet ";
        }

        KillerThread(Filter filter, String servletName) {
            super("Killer of " + contextName + "/" + servletName);
            setDaemon(true);
            this.filter = filter;
            this.servletName = servletName;
            destroyingName = "filter ";
        }

        public void run() {
            cat.info("Destroying " + destroyingName + servletName);
            try {
                if (servlet == null) {
                    filter.destroy();
                } else {
                    servlet.destroy();
                }
                cat.info(destroyingName + servletName + " destroyed successfully");
            } catch (Exception ex) {
                cat.error("Cannot destroy " + destroyingName + servletName, ex);
            }
        }

    }
}

