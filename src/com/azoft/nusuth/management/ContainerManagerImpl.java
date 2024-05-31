package com.azoft.nusuth.management;

import java.util.*;

import com.azoft.nusuth.management.security.AdminPortListener;
import com.azoft.nusuth.container.*;
import com.azoft.nusuth.deployment.*;

import java.security.*;
import java.security.acl.*;
import java.io.*;

import com.azoft.nusuth.jsp.JspLoader;
import com.azoft.nusuth.jsp.JikesJspCompiler;
import com.azoft.nusuth.core.NusuthContext;
import com.azoft.nusuth.core.LocalContainer;
import com.azoft.nusuth.server.*;
import org.apache.log4j.Category;

import java.util.zip.*;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.InetAddress;

import com.azoft.nusuth.util.*;
import com.azoft.nusuth.container.http.HttpProtocolAdapter;
import com.azoft.nusuth.session.DistributedSessionManager;
import com.azoft.nusuth.session.SessionManager;
import com.azoft.nusuth.session.DefaultSessionManager;
import com.azoft.nusuth.jidep.*;
import com.azoft.nusuth.jndi.DistributedJNDIContext;
import com.azoft.nusuth.jndi.JndiNameConverter;
import com.azoft.nusuth.security.*;
import com.azoft.nusuth.jidep.JidepConnectionFactory;
import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.naming.*;
import javax.naming.directory.*;

/**
 * This class implements ContainerManager class.
 *
 * @author vdgg, skilz, igork
 * @version 1.51
 * @since Nusuth1.0
 */

public class ContainerManagerImpl
        extends ComponentManagerImpl
        implements ContainerManager, Manageable {
    private class ClusterContextListener extends AbstractClusterListener {
        public ClusterContextListener(Set listenedPaths) {
            super(listenedPaths);
        }

        public void notify(JidepNotification notification) {
            logger.debug("Notify: "
                    + NotificationType.names[notification.notificationType]
                    + " on \"" + notification.name + '"');
            if (notification.name.startsWith("components/hosts")) {
                processHosts(notification);
            } else if (notification.name.startsWith("components/containers")) {
                processConfig(notification);
            }
        }

        private void processHosts(JidepNotification notification) {
            logger.debug("Cluster Listener: process hosts");
            try {
                String s1 = notification.name.substring("components/hosts/".length());
                int idx = s1.indexOf('/');
                if (idx == -1) {
                    String encHostName = s1;
                    if (notification.notificationType == NotificationType.DELETED) {
                        String hostName = JndiNameConverter.decode(encHostName);
                        removeHost(JndiNameConverter.decode(hostName));
                        CompositeNusuthWebAppElement localHostConfig =
                                ManagementUtil.getCompositeElement(settings, "host",
                                        "id", hostName);
                        if (localHostConfig != null) {
                            settings.removeCompositeChild("host", localHostConfig);
                            saveSettings();
                        }
                    }
                    processSubscription(notification);
                } else {
                    String encHostName = s1.substring(0, idx);
                    String hostName = JndiNameConverter.decode(encHostName);
                    String s2 = s1.substring(idx + 1);
                    if (s2.equals("config")) {
                        switch (notification.notificationType) {
                            case NotificationType.DELETED:
                            case NotificationType.ATTRCHANGED:
                                break;
                            case NotificationType.CREATED: // add new host
                            case NotificationType.REBINDED: // host config changed
                                CompositeNusuthWebAppElement localHostConfig =
                                        ManagementUtil.getCompositeElement(settings, "host",
                                                "id", hostName);
                                if (localHostConfig != null) {
                                    if (notification.notificationType == NotificationType.CREATED) {
                                        logger.warn("Add new host \"" + hostName + "\", but "
                                                + "it's already presented. Local has been "
                                                + "removed and replaced with cluster version.");
                                    }
                                    removeHost(hostName);
                                    settings.removeCompositeChild("host", localHostConfig);
                                }
                                localHostConfig = settings.addCompositeChild("host");
                                CompositeNusuthWebAppElement clusterHostConfig =
                                        (CompositeNusuthWebAppElement) clusterContext.lookup(
                                                "components/hosts/" + encHostName + "/config");
                                ManagementUtil.copyCompositeElement(clusterHostConfig,
                                        localHostConfig);
                                localHostConfig.setSimpleChild("id").setContent(hostName);
                                logger.debug("Cluster Listener: new host \"" + hostName
                                        + "\" config:\n" + localHostConfig);
                                addVirtualHost(localHostConfig);
                                saveSettings();
                                break;
                            default:
                                logger.debug("Unknown notification type : "
                                        + notification.notificationType);
                        }
                    } else if (s2.startsWith("webapps")) {
                        processWebapps(notification, hostName, s2);
                    }
                }
            } catch (Exception e) {
                logger.error("Couldn't process notify ("
                        + NotificationType.names[notification.notificationType]
                        + " on \"" + notification.name
                        + "\")", e);
            }
        }

        private void processWebapps(JidepNotification notification,
                                    String hostName,
                                    String tile) {
            logger.debug("Cluster Listener: process webapps");
            String s1 = tile.substring("webapps/".length());
            int idx = s1.indexOf('/');
            String encAppName = (idx > -1) ? s1.substring(0, idx) : s1;
            String appName = JndiNameConverter.decode(encAppName);
            if (idx > -1) {
                String s2 = s1.substring(idx + 1);
                switch (notification.notificationType) {
                    case NotificationType.CREATED:
                        tryToDownloadApp(hostName, appName);
                        break;
                    case NotificationType.DELETED:
                        break;
                    case NotificationType.ATTRCHANGED:
                    case NotificationType.REBINDED:
                        try {
                            if (s2.startsWith("config")) {
                                restartApp(hostName, appName);
                                saveSettings();
                            } else if (s2.equals("source/src")) {
                                updateLocalAppSource(hostName, appName);
                                restartApp(hostName, appName);
                                saveSettings();
                            }
                        } catch (ManagementException e) {
                            logger.error("Couldn't update local app \"" + hostName + '/'
                                    + appName + "\"from cluster context", e);
                        }
                        break;
                    default:
                        logger.debug("Unknown notification type : "
                                + notification.notificationType);
                }
                if (s2.length() == 0 || s2.equals("config")) {
                    processSubscription(notification);
                }
            } else {
                switch (notification.notificationType) {
                    case NotificationType.ATTRCHANGED:
                        break;
                    case NotificationType.CREATED:
                        break;
                    case NotificationType.DELETED:
                        try {
                            removeApp(hostName, appName);
                            CompositeNusuthWebAppElement host =
                                    ManagementUtil.getCompositeElement(settings, "host", "id",
                                            hostName);
                            CompositeNusuthWebAppElement app =
                                    ManagementUtil.getCompositeElement(host, "context", "path", appName);
                            host.removeCompositeChild("context", app);
                            saveSettings();
                        } catch (Exception e) {
                            logger.error("Couldn't delete application \"" + hostName + '/'
                                    + appName + '"',
                                    e);
                        }
                        break;
                    case NotificationType.REBINDED:
                        break;
                    default:
                        logger.debug("Unknown notification type : "
                                + notification.notificationType);
                }
                processSubscription(notification);
            }
        }

        private void tryToDownloadApp(String hostName, String appName) {
            try {
                DirContext cac =
                        (DirContext) clusterContext.lookup("components/hosts/"
                        + JndiNameConverter
                        .encode(hostName)
                        + "/webapps/"
                        + JndiNameConverter
                        .encode(appName));
                CompositeNusuthWebAppElement localHostConfig =
                        ManagementUtil.getCompositeElement(settings, "host",
                                "id", hostName);
                downloadApp(hostName, appName, localHostConfig, cac);
                saveSettings();
            } catch (Exception e) {
                logger.debug("couldn't download app \"" + hostName + '/' + appName
                        + "\" from cluster context");
            }
        }

        private void processConfig(JidepNotification notification) {
            logger.debug("Cluster Listener: process config");
            String s = "components/containers/" + JndiNameConverter.encode(componentId);
            if (notification.name.equals(s)
                    && notification.notificationType == NotificationType.ATTRCHANGED) {
                try {
                    Attributes attrs =
                            clusterContext
                            .getAttributes(s, new String[]{"port", "admin-port"});
                    if (attrs != null) {
                        Attribute portAttr = attrs.get("port");
                        Attribute aportAttr = attrs.get("admin-port");
                        if (portAttr != null && aportAttr != null) {
                            String port = (String) portAttr.get();
                            String aport = (String) aportAttr.get();

                            CompositeNusuthWebAppElement newSettings =
                                    (CompositeNusuthWebAppElement) settings.clone();

                            CompositeNusuthWebAppElement mn =
                                    ManagementUtil.getCompositeElement(newSettings, "manager");
                            ManagementUtil.getSimpleElement(mn, "port").setContent(aport);
                            CompositeNusuthWebAppElement tn =
                                    ManagementUtil.getCompositeElement(newSettings, "tcp-server");
                            ManagementUtil.getSimpleElement(tn, "port").setContent(port);

                            applySettings(newSettings);
                            saveSettings();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Couldn't process notify ("
                            + NotificationType.names[notification.notificationType]
                            + " on \"" + notification.name
                            + "\")", e);
                }
            } else if (notification.name.equals(s + "/config")
                    && notification.notificationType == NotificationType.REBINDED) {
                try {
                    CompositeNusuthWebAppElement newSettings =
                            (CompositeNusuthWebAppElement) clusterContext.lookup(s + "/config");

                    applySettings(newSettings);
                    saveSettings();

                    CompositeNusuthWebAppElement managerNode =
                            ManagementUtil.getCompositeElement(newSettings, "manager");
                    int aport = ManagementUtil.getSimpleInt(managerNode, "port");
                    CompositeNusuthWebAppElement tcpNode =
                            ManagementUtil.getCompositeElement(newSettings, "tcp-server");
                    int port = ManagementUtil.getSimpleInt(tcpNode, "port");


                    Attributes attrs = new BasicAttributes();
                    attrs.put("port", String.valueOf(port));
                    attrs.put("admin-port", String.valueOf(aport));
                    clusterContext.modifyAttributes(s, DirContext.REPLACE_ATTRIBUTE, attrs);
                } catch (Exception e) {
                    logger.error("Couldn't process notify ("
                            + NotificationType.names[notification.notificationType]
                            + " on \"" + notification.name
                            + "\")", e);
                }
            }
        }
    }

    private static CompositeNusuthWebAppElement hostConfigSample;
    private ApplicationManager lnkApplicationManager;
    //  private JBirdContainer container = null;
    private final String defaultProtocolAdapter = "com.azoft.nusuth.container.http.HttpProtocolAdapter";
    private static final Set deprecated_app_config_element = new HashSet();
    private String authKey = "";
    private String name;
    private boolean isStandAlone = true;
    private String protocolAdapter;
    private String workDir;
    private NusuthTcpServer tcpServer;
    private NusuthSslServer sslServer;
    //  private Category logger;
    private HashSet loadedAppsLocation = new HashSet();
    private Hashtable applications = new Hashtable();
    private Hashtable adminApplications = new Hashtable();
    private HashSet allHosts = new HashSet();
    private HashSet httpContexts = new HashSet();
    private HashSet httpsContexts = new HashSet();
    private NusuthContext configContext = null;
    private Hashtable contextID2config = new Hashtable();
    private Hashtable contextID2context = new Hashtable();
    private CompositeNusuthWebAppElement commonConfig = null;
    private LocalContainer localContainer;
    private String warRepository = null;
    File repositoryDir = null;

    static {
        deprecated_app_config_element.add("path");
        deprecated_app_config_element.add("location");
        deprecated_app_config_element.add("session-backup");
    }

    /**
     * Constructor for this class. It runs TcpServer and SslServer(if defined in
     * config); binds virtual hosts and applications to the servers.
     * @param configPath Config file name.
     * @exception Exception Throws if any errors orrures.
     */
    public ContainerManagerImpl(String configPath)
            throws Exception {
        super();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is =
                cl.getResourceAsStream("com/azoft/nusuth/deployment/container.xml");
        CompositeNusuthWebAppElement tmp =
                null;
        try {
            tmp = NusuthAppConfigFactory.createConfig("container", is);
            hostConfigSample = tmp.addCompositeChild("host");
        } catch (Exception e) {
            System.err.println("Couldn't create default container config, nested: "
                    + e.getMessage());
            e.printStackTrace(System.err);
            throw new IllegalStateException("Couldn't create default container "
                    + "config, nested: " + e.getMessage());
        }

        if (configPath == null) {
            File confFile = new File(jbirdHome, "admin" + File.separator
                    + "container.xml");
            if (!confFile.exists()) {
                throw new Exception("Cannot find config");
            }
            this.configFileName = confFile.getPath();
        } else {
            this.configFileName = configPath;
        }
        javax.servlet.jsp.JspFactory.setDefaultFactory(
                new com.azoft.nusuth.jsp.NusuthJspFactory());
        settings = NusuthAppConfigFactory.createConfig("container",
                this.configFileName);

        // starting logger
        CompositeNusuthWebAppElement composite
                = (CompositeNusuthWebAppElement) settings.getCompositeChild("logger").
                nextElement();
        loadLogger(composite, "nusuth.log");
        logger.info("Starting container manager");

        Enumeration enum = composite.getSimpleChild("location");
        try {
            File keyFile = new File(logsLocation, "nusuth.key");
            if (!keyFile.exists()) {
                keyFile.createNewFile();
            }
            FileOutputStream kfos = new FileOutputStream(keyFile);
            String key = generateSecretKey();
            kfos.write(key.getBytes());
            kfos.close();
            NusuthRequestHandler.setSecretKey("/" + key);
        } catch (IOException ioex) {
            System.out.println("{Nusuth} Can't create specifed log file - log file "
                    + "will be created in current directory, nested: "
                    + ioex);
        }

        // get container id
        enum = settings.getSimpleChild("name");
        name = enum.hasMoreElements()
                ? ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim()
                : null;
        setComponentId(name);

        // get StandAlone parameter
        String standAloneStr = ((SimpleNusuthWebAppElement) settings.
                getSimpleChild("standalone").nextElement()).getContent().trim();
        if (standAloneStr.equalsIgnoreCase("true")
                || standAloneStr.equalsIgnoreCase("on")
                || standAloneStr.equalsIgnoreCase("yes")) {
            isStandAlone = true;
        } else if (standAloneStr.equalsIgnoreCase("false")
                || standAloneStr.equalsIgnoreCase("off")
                || standAloneStr.equalsIgnoreCase("no")) {
            isStandAlone = false;
        } else {
            logger.error("Cannot recognize value " + standAloneStr
                    + " of standalone element; use true or false");
            throw new IllegalArgumentException("Cannot recognize value "
                    + standAloneStr + " of standalone element"
                    + "; use true or false");
        }
        if (!isStandAlone && name == null) {
            logger.error("You MUST specify name for non-standalone Nusuth engine");
            throw new IllegalArgumentException("You MUST specify name for "
                    + "non-standalone Nusuth engine");
        } else {
            ProtocolAdapter.setContainerID(name);
            ProtocolAdapter.setStandAlone(isStandAlone);
        }


        // starting protocol adapter
        enum = settings.getSimpleChild("protocol-adapter");
        protocolAdapter = enum.hasMoreElements()
                ? ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim()
                : defaultProtocolAdapter;
        NusuthRequestHandler.setAdapterClass(protocolAdapter);

        // set manager properties
        if (settings.getCompositeChild("manager").hasMoreElements()) {
            CompositeNusuthWebAppElement elem
                    = (CompositeNusuthWebAppElement) settings.
                    getCompositeChild("manager").nextElement();
            authKey = ((SimpleNusuthWebAppElement) elem.getSimpleChild("auth-key").
                    nextElement()).getContent().trim();
            JidepConnectionFactory.setKey(authKey);
        }
        localContainer = new LocalContainer(this, authKey);

        //
        enum = settings.getSimpleChild("admin-ip");
        NusuthRequestHandler.setAdminIP(convertAdminIp(enum.hasMoreElements()
                ? ((SimpleNusuthWebAppElement)
                enum.nextElement()).
                getContent()
                : null));
        NusuthRequestHandler.setContainer(this);

        // set JSP parameters
        enum = settings.getCompositeChild("jsp");
        String compilerClassName = "com.azoft.nusuth.jsp.JavacJspCompiler";
        if (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement jspConf
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            JspLoader.setJspRefresh(ManagementUtil.getSimpleTime(jspConf, "refresh"));
            Enumeration enum1 = jspConf.getSimpleChild("compiler");
            String jspCompilerStr = ((SimpleNusuthWebAppElement) enum1.nextElement()).
                    getContent().trim();
            if (jspCompilerStr.equalsIgnoreCase("javac")) {
                compilerClassName = "com.azoft.nusuth.jsp.JavacJspCompiler";
            } else if (jspCompilerStr.toLowerCase().indexOf("jikes") != -1) {
                compilerClassName = "com.azoft.nusuth.jsp.JikesJspCompiler";
                JikesJspCompiler.setExecutable(jspCompilerStr);
            } else {
                logger.error("Cannot recognize value " + jspCompilerStr + " of jsp:compiler"
                        + " element; use javac or jikes");
                throw new IllegalArgumentException("Cannot recognize value "
                        + jspCompilerStr + " of jsp:compiler "
                        + "element; use javac or jikes");
            }
        }
        JspLoader.setCompilerClass(Class.forName(compilerClassName));

        // set workdir
        enum = settings.getSimpleChild("work-dir");
        workDir = enum.hasMoreElements()
                ? ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim()
                : (new File(jbirdHome, "work")).getCanonicalPath();
        File wdFile = new File(workDir);
        if ((wdFile.exists() && !wdFile.isDirectory())
                || (!wdFile.exists() && !wdFile.mkdirs())) {
            logger.error("Cannot find or create working directory " + workDir);
            throw new Exception("Cannot find or create working directory " + workDir);
        }

        //set default web config
        enum = settings.getSimpleChild("default-web-config");
        File commonRepositoryFile = enum.hasMoreElements()
                ? new File(((SimpleNusuthWebAppElement) enum.nextElement()).
                getContent())
                : new File(jbirdHome, "admin/web.xml");
        if (commonRepositoryFile.exists()) {
            commonConfig = NusuthAppConfigFactory.createConfig("web-app",
                    commonRepositoryFile.
                    getCanonicalPath());
            NusuthContext.setCommonConfig(commonConfig);
        }

        //creating and starting TCP Server
        tcpServer = new NusuthTcpServer((CompositeNusuthWebAppElement) settings.
                getCompositeChild("tcp-server").
                nextElement(),
                Class.forName("com.azoft.nusuth.container."
                + "NusuthRequestHandler"));
        if (!isStandAlone) {
            tcpServer.setMaxKeepAlive(tcpServer.getMaxHandlers());
        }

        enum = settings.getCompositeChild("host");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement hostElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String id = ManagementUtil.getSimpleString(hostElement, "id");
            tcpServer.addVirtualHost(id);
        }


        tcpServer.startServer();

        //creating and starting SSL Server (if needed)
        enum = settings.getCompositeChild("ssl-server");
        if (enum.hasMoreElements()) {
            sslServer = new NusuthSslServer((CompositeNusuthWebAppElement) enum.
                    nextElement(), Class.forName("com.azoft"
                    + ".nusuth.container.NusuthRequestHandler"));
            if (!isStandAlone) {
                sslServer.setMaxKeepAlive(sslServer.getMaxHandlers());
            }
            enum = settings.getCompositeChild("host");
            while (enum.hasMoreElements()) {
                CompositeNusuthWebAppElement hostElement
                        = (CompositeNusuthWebAppElement) enum.nextElement();
                String id = ManagementUtil.getSimpleString(hostElement, "id");
                sslServer.addVirtualHost(id);
            }
            sslServer.startServer();
            NusuthRequestHandler.sslEnabled = true;
        }

        if (isStandAlone) {
            ProtocolAdapter.setServerParameters(InetAddress.getLocalHost().
                    getHostName(), tcpServer.getPort());
            if (sslServer != null) {
                ProtocolAdapter.setSSLPort(sslServer.getPort());
            }
        }

        //start admin server
        enum = settings.getCompositeChild("admin-config");
        if (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement adminConfig
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            CompositeNusuthWebAppElement adminServerElement
                    = ManagementUtil.getCompositeElement(adminConfig, "admin-server");
            Class conHandler
                    = Class.forName("com.azoft.nusuth.container."
                    + "NusuthAdminRequestHandler");
            NusuthAdminRequestHandler.setAdapterClass(protocolAdapter);
            NusuthAdminRequestHandler.setApplicationContext(adminApplications);
            String contextLocation
                    = ManagementUtil.getSimpleString(adminConfig,
                            "admin-context-location");
            setAdminContext(contextLocation);
            NusuthTcpServer adminServer = new NusuthTcpServer(adminServerElement,
                    conHandler);
            adminServer.setName("Admin");
            adminServer.addVirtualHost("*");
            adminServer.startServer();
        }

        // start AdminPortListener
        enum = settings.getCompositeChild("manager");
        if (enum.hasMoreElements()) {
            listener = new AdminPortListener(settings, this);
            listener.start();
        } else if (!isStandAlone) {
            logger.error("You MUST specify manager parameters "
                    + "for non-standalone Nusuth engine");
            throw new IllegalArgumentException("You MUST specify manager parameters "
                    + "for non-standalone Nusuth engine");
        }

        //creating virtual hosts defined
//Commented in order that apache and iis connectors can work correctly
/*
    try {
      if (!isStandAlone) {
        //System.out.println("settings = \n" + settings.toString() + '\n');
        normalizeDocBase(settings);
        //System.out.println("normalizeDocBase | settings = \n" + settings.toString() + '\n');
        clusterContext = connectToClusterContext(settings);
        //System.out.println("connectToClusterContext | settings = \n" + settings.toString() + '\n');
        bindPrivateConfigToClusterContext();
        System.out.println("bindPrivateConfigToClusterContext | settings = \n" + settings.toString() + '\n');
        checkHosts();
        System.out.println("checkHosts | settings = \n" + settings.toString() + '\n');
        checkApps();
        System.out.println("checkApps settings = \n" + settings.toString() + '\n');
        saveSettings();
        System.out.println("saveSettings settings = \n" + settings.toString() + '\n');
      } else {
        clusterContext = null;
      }
    } catch (DeploymentException e) {
    } catch (ManagementException e) {
    }
*/

        warRepository = ManagementUtil.getSimpleString(settings, "war-repository");
        if (warRepository == null || warRepository.length() == 0) {
            repositoryDir = new File(jbirdHome, "repository");
            if (!repositoryDir.exists()) {
                repositoryDir.mkdirs();
            }
        } else {
            repositoryDir = new File(jbirdHome, warRepository);
            if (!repositoryDir.exists()) {
                repositoryDir.mkdirs();
            }
        }
        warRepository = repositoryDir.getAbsolutePath();
        CompositeNusuthWebAppElement deployListElement
                = ManagementUtil.getCompositeElement(settings, "deploy-list");
        if (deployListElement != null) {
            enum = deployListElement.getCompositeChild("deploy-item");
            while (enum.hasMoreElements()) {
                CompositeNusuthWebAppElement deployItemElement
                        = (CompositeNusuthWebAppElement) enum.nextElement();
                String warName
                        = ManagementUtil.getSimpleString(deployItemElement, "war-name");
                String hostToDeploy
                        = ManagementUtil.getSimpleString(deployItemElement, "host-id");
                deployWar(hostToDeploy, warName);
            }
        }

        enum = settings.getCompositeChild("host");
        while (enum.hasMoreElements()) {
            addVirtualHost((CompositeNusuthWebAppElement) enum.nextElement());
        }

        HttpProtocolAdapter.setAllHosts(allHosts);
//Commented in order that apache and iis connectors can work correctly
/*
    if (!isStandAlone) {
      updateRunningAppsMarksOnClusterContext();
      updateRunningAppsMarksOnClusterContext();
      setClusterListener();
      logger.info("Container manager started");
      logContext(clusterContext);
    }
*/
    }

    private void setClusterListener() {
        Set paths = new HashSet();
        String s = "components/";
        paths.add(s + "containers");
        System.out.println("*** Listener: add " + s + "containers");
        paths.add(s + "containers/" + JndiNameConverter.encode(componentId));
        System.out.println("*** Listener: add " + s + "containers/" + JndiNameConverter.encode(componentId));
        paths.add(s += "hosts");
        System.out.println("*** Listener: add " + s);
        try {
            for (NamingEnumeration i = clusterContext.list(s); i.hasMore();) {
                NameClassPair pair = (NameClassPair) i.next();
                String hostName = pair.getName();
                String ss = s + '/' + hostName;
                paths.add(ss);
                System.out.println("*** Listener: add " + ss);
                paths.add(ss += "/webapps");
                System.out.println("*** Listener: add " + ss);
                for (NamingEnumeration j = clusterContext.list(ss); j.hasMore();) {
                    NameClassPair pair2 = (NameClassPair) j.next();
                    String appName = pair2.getName();
                    paths.add(ss + '/' + appName);
                    System.out.println("*** Listener: add " + ss + '/' + appName);
                    paths.add(ss + '/' + appName + "/config");
                    System.out.println("*** Listener: add " + ss + '/' + appName + "/config");
                }
            }
        } catch (NamingException e) {
            logger.error("Couldnt add listener to cluster context", e);
        }
        ClusterContextListener l = new ClusterContextListener(paths);
        clusterContext.subscribe((String[]) paths.toArray(new String[0]), l);
    }

    private void normalizeDocBase(CompositeNusuthWebAppElement badSettings) {
        if (!isStandAlone) {
            try {
                for (Enumeration i = badSettings.getCompositeChild("host"); i.hasMoreElements();) {
                    try {
                        CompositeNusuthWebAppElement hostElem = (CompositeNusuthWebAppElement) i.nextElement();
                        String hostName = ManagementUtil.getSimpleString(hostElem, "id");
                        String encHostName = JndiNameConverter.encode(hostName);
                        hostElem.setSimpleChild("doc-base").setContent(new File(jbirdHome, "webapps/" + encHostName).getAbsolutePath());
                    } catch (DeploymentException e) {
                        logger.error("Couldn't set doc-base for save", e);
                    }
                }
                badSettings.setAllRequiredChilds();
            } catch (DeploymentException e) {
                logger.error("Couldn't set hosts doc-base for save", e);
            }
        }
    }

    private void bindPrivateConfigToClusterContext() throws ManagementException {
        try {
            DirContext c = (DirContext) clusterContext.lookup("components/containers");
            Attributes attrs = new BasicAttributes();
            attrs.put("port", String.valueOf(tcpServer.getPort()));
            attrs.put("ip", InetAddress.getLocalHost().getHostAddress());
            attrs.put("admin-port", String.valueOf(listener.getPort()));
            attrs.put(createEmptyAclAttribute());
            attrs.put("Replicable", new Boolean(false));
            //attrs.put("Node", "localhost:"+listener.getPort());
            DirContext curr = null;
            String encName = JndiNameConverter.encode(componentId);
            try {
                curr = c.createSubcontext(encName, attrs);
            } catch (NamingException e) {
                c.unbind(encName);
                curr = c.createSubcontext(encName, attrs);
            }
            curr.bind("config", settings);
        } catch (NamingException e) {
            logger.error("Couldn't add own config to cluster context", e);
            throw new ManagementException("Couldn't add own config to "
                    + "cluster context" + e.getMessage());
        } catch (UnknownHostException e) {
            logger.debug("Cannot take localhost ip", e);
            throw new ManagementException("Couldn't add own config to cluster context"
                    + ", nested: couldn't get localhost ip, "
                    + "nested: " + e.getMessage());
        }
        logContext(clusterContext);
    }

    private void checkHosts() throws ManagementException {
        Map clusterHosts = getHostsFromClusterContext();
        Map localHosts = getLocalHosts();

        Attributes attrs = new BasicAttributes();
        attrs.put("Replicable", new Boolean(true));
        attrs.put(createEmptyAclAttribute());

        // check and merge cluster and local hosts (hosts only, not webapps
        for (Iterator hostsIter = localHosts.keySet().iterator();
             hostsIter.hasNext();
                ) {
            String hostName = (String) hostsIter.next();
            if (!clusterHosts.keySet().contains(hostName)) {
                try {
                    DirContext hostContext =
                            clusterContext.createSubcontext(
                                    "components/hosts/"
                            + JndiNameConverter.encode(hostName), attrs);
                    hostContext.createSubcontext("webapps", attrs);
                    CompositeNusuthWebAppElement hostConfig = (CompositeNusuthWebAppElement) localHosts.get(hostName);
                    String encHostName = JndiNameConverter.encode(hostName);
                    hostContext.bind("config", hostConfig);

                    clusterHosts.put(hostName, hostConfig);
                } catch (NamingException e) {
                    throw new ManagementException("Couldn't bind new host \"" + hostName + "\" to cluster context" + e.getMessage());
                }
            }
        }

        for (Iterator hostsIter = clusterHosts.keySet().iterator();
             hostsIter.hasNext();
                ) {
            String hostName = (String) hostsIter.next();
            CompositeNusuthWebAppElement hostConfig =
                    (CompositeNusuthWebAppElement) localHosts.get(hostName);
            if (!localHosts.keySet().contains(hostName)) {
                CompositeNusuthWebAppElement clusterHostConfig = (CompositeNusuthWebAppElement) clusterHosts.get(hostName);
                try {
                    CompositeNusuthWebAppElement newHost = settings.addCompositeChild("host");
                    String encHostName = JndiNameConverter.encode(hostName);
                    newHost.setSimpleChild("id").setContent(hostName);
                    newHost.setSimpleChild("doc-base").setContent(new File(jbirdHome, "webapps/" + encHostName).getAbsolutePath());
                    newHost.setSimpleChild("autoload").setContent(ManagementUtil.getSimpleString(clusterHostConfig, "autoload"));
                    CompositeNusuthWebAppElement accessLogElement = ManagementUtil.getCompositeElement(clusterHostConfig, "access-log");
                    if (accessLogElement != null) {
                        newHost.addCompositeChild("access-log", accessLogElement);
                    }

                    //newHost.setAllRequiredChilds();
                    System.out.println("newHost = " + newHost.toString());

                    localHosts.put(hostName, newHost);
                } catch (DeploymentException e) {
                    logger.error("Couldn't add new host to local config", e);
                }
            }
        }
    }

    /**
     * Retrieves virtual hosts configs from cluster JNDI context
     * @return Map host_id -> host_config(CompositeNusuthWebAppElement)
     */
    private Map getHostsFromClusterContext() throws ManagementException {
        try {
            Map result = new HashMap();
            DirContext hostsContext =
                    (DirContext) clusterContext.lookup("components/hosts");
            NamingEnumeration hostsEnum = hostsContext.list("");
            while (hostsEnum.hasMore()) {
                String hostName = "<unknown>";
                try {
                    NameClassPair ncp = (NameClassPair) hostsEnum.next();
                    hostName = ncp.getName();
                    CompositeNusuthWebAppElement hostConfig =
                            (CompositeNusuthWebAppElement) hostsContext.lookup(hostName
                            + "/config");
                    result.put(JndiNameConverter.decode(hostName), hostConfig);
                } catch (NamingException e) {
                    logger.warn("Couldn't get host \"" + hostName
                            + "\" config from cluster context", e);
                }
            }
            return result;
        } catch (NamingException e) {
            logger.error("Couldn't get hosts from cluster context", e);
            throw new ManagementException("Couldn't get hosts from cluster context"
                    + ", nested: " + e.getMessage());
        }
    }

    private Map getLocalHosts() throws ManagementException {
        Map result = new HashMap();
        try {
            Enumeration enum = settings.getCompositeChild("host");
            while (enum.hasMoreElements()) {
                CompositeNusuthWebAppElement hostElem =
                        (CompositeNusuthWebAppElement) enum.nextElement();
                String hostName = ManagementUtil.getSimpleString(hostElem, "id");
                String autoload = ManagementUtil.getSimpleString(hostElem, "autoload");
                if (autoload.length() == 0) {
                    autoload = "true";
                }
                byte[] source = ("<cluster-host>"
                        + (autoload.length() > 0
                        ? "<autoload>" + autoload + "</autoload>"
                        : "")
                        + "</cluster-host>"
                        ).getBytes();
                CompositeNusuthWebAppElement accessLogElem =
                        ManagementUtil.getCompositeElement(hostElem, "access-log");

                CompositeNusuthWebAppElement hostConfig =
                        NusuthAppConfigFactory.createConfig(
                                ManagementUtil.DISTRIBUTED_JNDI_HOST_CONFIG,
                                new ByteArrayInputStream(source));
                if (accessLogElem != null) {
                    hostConfig.addCompositeChild("access-log", accessLogElem);
                }

                result.put(hostName, hostConfig);
            }
        } catch (DeploymentException e) {
            logger.warn("Couldn't get hosts config from local stored config", e);
        }
        return result;
    }

    private void checkApps() throws ManagementException {
        try {
            for (Enumeration i = settings.getCompositeChild("host"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement hostElem = (CompositeNusuthWebAppElement) i.nextElement();
                String hostId = ManagementUtil.getSimpleString(hostElem, "id");
                CompositeNusuthWebAppElement hostConfig = (CompositeNusuthWebAppElement) clusterContext.lookup("components/hosts/" + JndiNameConverter.encode(hostId) + "/config");
                checkApps(hostId, hostConfig, hostElem);
            }
        } catch (DeploymentException e) {
            logger.error("Couldn't check webapps", e);
            throw new ManagementException("Couldn't check webapps " + e.getMessage());
        } catch (NamingException e) {
            logger.error("Couldn't check webapps", e);
            throw new ManagementException("Couldn't check webapps " + e.getMessage());
        } catch (ManagementException e) {
            logger.error("Couldn't check webapps", e);
            throw e;
        }
    }

    private void checkApps(String hostId, CompositeNusuthWebAppElement clusterHostConfig, CompositeNusuthWebAppElement localHostConfig)
            throws ManagementException {
        boolean autoload = false;
        try {
            autoload = ManagementUtil.getSimpleBoolean(clusterHostConfig, "autoload", true);
        } catch (DeploymentException e) {
            throw new ManagementException("Couldn't get autoload parameter from host \"" + hostId + "\" from cluster config, nested: " + e.getMessage());
        }

        try {
            DirContext webappsContext = (DirContext) clusterContext.lookup("components/hosts/" + JndiNameConverter.encode(hostId) + "/webapps");
            Map localApps = getLocalAppsConfigs(localHostConfig);
            Map clusterApps = new HashMap();
            for (NamingEnumeration appsEnum = webappsContext.list("");
                 appsEnum.hasMore();
                    ) {
                NameClassPair ncp = (NameClassPair) appsEnum.next();
                String encodedAppName = ncp.getName();
                String appName = JndiNameConverter.decode(encodedAppName);
                DirContext clusterAppContext = (DirContext) webappsContext.lookup(encodedAppName);
                CompositeNusuthWebAppElement clusterAppConfig = (CompositeNusuthWebAppElement) clusterAppContext.lookup("config/nusuth");
                clusterApps.put(appName, clusterAppConfig);
                String clusterSerialNumber =
                        ManagementUtil.getSimpleString(clusterAppConfig, "serial-number");
                CompositeNusuthWebAppElement localAppConfig =
                        (CompositeNusuthWebAppElement) localApps.get(appName);
                if (localAppConfig != null) {
                    String localSerialNumber =
                            ManagementUtil.getSimpleString(localAppConfig, "serial-number");
                    updateLocalAppConfig(hostId, appName, localAppConfig, clusterAppContext);
                    if (!localSerialNumber.equals(clusterSerialNumber)) {
                        updateLocalAppSource(hostId, appName);
                    }
                } else {
                    downloadApp(hostId, appName, localHostConfig, clusterAppContext);
                }
            }

            for (Iterator i = localApps.keySet().iterator(); i.hasNext();) {
                String appName = (String) i.next();
                CompositeNusuthWebAppElement localAppConfig = (CompositeNusuthWebAppElement) localApps.get(appName);
                logger.debug("local app: " + appName + "\n" + localAppConfig);
                if (!clusterApps.keySet().contains(appName)) {
                    uploadApp(hostId, appName, webappsContext, localAppConfig, autoload);
                }
            }
        } catch (NamingException e) {
            logger.error("Couldn't merge cluster and local applications for host \"" + hostId + "\"", e);
            throw new ManagementException("Couldn't merge cluster and local applications for host \"" + hostId + "\", nested: " + e.getMessage());
        } catch (ManagementException e) {
            logger.error("Couldn't merge cluster and local applications for host \"" + hostId + "\"", e);
            throw new ManagementException("Couldn't merge cluster and local applications for host \"" + hostId + "\", nested: " + e.getMessage());
        } catch (DeploymentException e) {
            logger.error("Couldn't merge cluster and local applications for host \"" + hostId + "\"", e);
            throw new ManagementException("Couldn't merge cluster and local applications for host \"" + hostId + "\", nested: " + e.getMessage());
        }
    }

    private Map getLocalAppsConfigs(CompositeNusuthWebAppElement localHostConfig)
            throws ManagementException {
        Map result = new HashMap();
        try {
            Enumeration appsEnum = localHostConfig.getCompositeChild("context");
            while (appsEnum.hasMoreElements()) {
                CompositeNusuthWebAppElement appElem =
                        (CompositeNusuthWebAppElement) appsEnum.nextElement();
                String name = ManagementUtil.getSimpleString(appElem, "path");
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                if (name.endsWith("/")) {
                    name = name.substring(0, name.length() - 1);
                }
                CompositeNusuthWebAppElement appConfig =
                        NusuthAppConfigFactory.createConfig(
                                ManagementUtil.DISTRIBUTED_JNDI_APP_JBIRD_CONFIG,
                                Thread.currentThread().getContextClassLoader().
                        getResourceAsStream(
                                "com/azoft/nusuth/deployment/"
                        + ManagementUtil.DISTRIBUTED_JNDI_APP_JBIRD_CONFIG + ".xml"));

                for (Enumeration i = appElem.getSimpleChildrenNames(); i.hasMoreElements();) {
                    String childName = (String) i.nextElement();
                    if (!deprecated_app_config_element.contains(childName)) {
                        ManagementUtil.removeSimpleChild(appConfig, childName);
                        ManagementUtil.copySimpleChild(appElem, appConfig, childName);
                    }
                }
                for (Enumeration i = appElem.getCompositeChildrenNames(); i.hasMoreElements();) {
                    String childName = (String) i.nextElement();
                    if (!deprecated_app_config_element.contains(childName)) {
                        ManagementUtil.removeCompositeChild(appConfig, childName);
                        ManagementUtil.copyCompositeChild(appElem, appConfig, childName);
                    }
                }
                result.put(name, appConfig);
            }
        } catch (DeploymentException e) {
            logger.error("Couldn't get local webapp configs", e);
            throw new ManagementException("Couldn't get local webapp configs" + e.getMessage());
        }
        return result;
    }

    private void updateLocalAppConfig(String hostId, String appName, CompositeNusuthWebAppElement localAppConfig, DirContext clusterAppContext)
            throws ManagementException {
        try {
            CompositeNusuthWebAppElement clusterAppConfig = (CompositeNusuthWebAppElement) clusterAppContext.lookup("config/nusuth");

            ManagementUtil.copyCompositeElement(clusterAppConfig, localAppConfig);

            CompositeNusuthWebAppElement appWebConfig = (CompositeNusuthWebAppElement) clusterAppContext.lookup("config/web");
            CompositeNusuthWebAppElement appUsersConfig = (CompositeNusuthWebAppElement) clusterAppContext.lookup("config/users");
            String encHostId = JndiNameConverter.encode(hostId);
            String encAppName = JndiNameConverter.encode(appName);
            byte[] appSource = (byte[]) clusterAppContext.lookup("source/src");
            File appFile = getAppFile(encHostId, encAppName);
            File tmpAppFile = new File(appFile.getAbsolutePath() + ".tmp");
            ZipInputStream zis = new ZipInputStream(new FileInputStream(appFile));
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tmpAppFile));
            zos.setLevel(9);
            byte[] buf = new byte[8196];
            boolean webSaved = false;
            boolean usersSaved = false;
            for (ZipEntry zentry = zis.getNextEntry(); zentry != null; zentry = zis.getNextEntry()) {
                if (zentry.getName().equals("WEB-INF/web.xml")) {
                    if (appWebConfig != null) {
                        zos.putNextEntry(new ZipEntry("WEB-INF/web.xml"));
                        zos.write(appWebConfig.toString().getBytes());
                        zos.closeEntry();
                    }
                } else if (zentry.getName().equals("WEB-INF/users.xml")) {
                    if (appUsersConfig != null) {
                        zos.putNextEntry(new ZipEntry("WEB-INF/users.xml"));
                        zos.write(appUsersConfig.toString().getBytes());
                        zos.closeEntry();
                    }
                } else {
                    zos.putNextEntry(new ZipEntry(zentry.getName()));
                    for (int readed = zis.read(buf, 0, buf.length); readed > 0; readed = zis.read(buf, 0, buf.length)) {
                        zos.write(buf, 0, readed);
                    }
                    zos.closeEntry();
                }
            }
            zis.close();
            zos.close();

            appFile.delete();
            tmpAppFile.renameTo(appFile);
        } catch (NamingException e) {
            logger.error("Couldn't update webapp local config", e);
            throw new ManagementException("Couldn't update webapp local config" + e.getMessage());
        } catch (DeploymentException e) {
            logger.error("Couldn't update webapp local config", e);
            throw new ManagementException("Couldn't update webapp local config" + e.getMessage());
        } catch (IOException e) {
            logger.error("Couldn't update webapp local config", e);
            throw new ManagementException("Couldn't update webapp local config" + e.getMessage());
        }
    }

    private void updateLocalAppSource(String hostId, String appName)
            throws ManagementException {
        String encHostId = JndiNameConverter.encode(hostId);
        String encAppName = JndiNameConverter.encode(appName);
        try {
            byte[] appSource = (byte[]) clusterContext.lookup("components/hosts/" + encHostId + "/webapps/" + encAppName + "/source/src");
            File appFile = getAppFile(encHostId, encAppName);
            if (appFile.exists()) {
                appFile.delete();
            }
            OutputStream os = new FileOutputStream(appFile);
            os.write(appSource);
            os.close();
        } catch (NamingException e) {
            logger.error("Couldn't update webapp local source ", e);
            throw new ManagementException("Couldn't update webapp local source" + e.getMessage());
        } catch (IOException e) {
            logger.error("Couldn't update webapp local source ", e);
            throw new ManagementException("Couldn't update webapp local source" + e.getMessage());
        }
    }

    private File getAppFile(String encHostId, String encAppName)
            throws ManagementException {
        File webappStorageFile = new File(jbirdHome, "webapps");
        File hostFile = new File(webappStorageFile, encHostId);
        if (!hostFile.exists()) {
            hostFile.mkdirs();
        }
        return new File(hostFile, encAppName + ".war");
    }

    private void downloadApp(String hostId, String appName, CompositeNusuthWebAppElement localHostConfig, DirContext clusterAppContext)
            throws ManagementException {
        updateLocalAppSource(hostId, appName);
        try {
            CompositeNusuthWebAppElement localAppConfig = localHostConfig.addCompositeChild("context");
            updateLocalAppConfig(hostId, appName, localAppConfig, clusterAppContext);
            localAppConfig.setSimpleChild("path").setContent(appName);
            localAppConfig.setSimpleChild("location").setContent(appName + ".war");
        } catch (DeploymentException e) {
            logger.error("Couldn't download webapp", e);
            throw new ManagementException("Couldn't download webapp, nested: " + e.getMessage());
        }
    }

    private void uploadApp(String hostId, String appName, DirContext webappsContext, CompositeNusuthWebAppElement localAppConfig, boolean autoload)
            throws ManagementException {

        try {
            Vector protocolNames = ManagementUtil.parseCommaList(ManagementUtil.getSimpleString(localAppConfig, "protocol"));
            Map appConfigsAndSource = getLocalAppConfigsAndSource(hostId, appName);
            CompositeNusuthWebAppElement appWebConfig = (CompositeNusuthWebAppElement) appConfigsAndSource.get("web");
            CompositeNusuthWebAppElement appUsersConfig = (CompositeNusuthWebAppElement) appConfigsAndSource.get("users");
            byte[] appSource = (byte[]) appConfigsAndSource.get("source");

            SimpleNusuthWebAppElement ver =
                    ManagementUtil.getSimpleElement(localAppConfig, "serial-number");
            if (ver == null) {
                ver = localAppConfig.setSimpleChild("serial-number");
            }
            if (ver.getContent().trim().length() == 0) {
                ver.setContent("1.0");
            }

            Attribute replicable = new BasicAttribute("Replicable", new Boolean(true));
            Attribute containers = new BasicAttribute("containers", this.componentId);
            Attribute containersToDeploy = new BasicAttribute("containers-to-deploy", autoload ? "all" : this.componentId);
            Attribute protocols = new BasicAttribute("protocols");
            for (Iterator i = protocolNames.iterator(); i.hasNext();) {
                String protocolName = (String) i.next();
                protocols.add(protocolName);
            }
            Attributes attrs = new BasicAttributes();
            attrs.put(replicable);
            attrs.put(containers);
            attrs.put(containersToDeploy);
            attrs.put(protocols);
            attrs.put(createEmptyAclAttribute());

            Attributes attrsEmpty = new BasicAttributes();
            attrsEmpty.put(replicable);
            attrsEmpty.put(createEmptyAclAttribute());

            Attributes attrsAcl = new BasicAttributes();
            attrsAcl.put(createEmptyAclAttribute());
            attrsAcl.put("Replicable", new Boolean(false));
            //attrsAcl.put("Node", "localhost:"+listener.getPort());

            DirContext appContext = (DirContext) webappsContext.createSubcontext(appName, attrsEmpty);
            DirContext srcContext = (DirContext) appContext.createSubcontext("source", attrsAcl);
            DirContext configContext = (DirContext) appContext.createSubcontext("config", attrs);

            srcContext.bind("src", appSource);
            configContext.bind("nusuth", localAppConfig);
            configContext.bind("web", appWebConfig);
            configContext.bind("users", appUsersConfig);
            logger.debug("upload app: config:\n" + localAppConfig + "\nupload app: users:\n" + appUsersConfig);
            logContext(clusterContext);
        } catch (DeploymentException e) {
            logger.error("Couldn't upload webapp", e);
            throw new ManagementException("Couldn't upload webapp, nested: " + e.getMessage());
        } catch (ManagementException e) {
            logger.error("Couldn't upload webapp", e);
            throw e;
        } catch (NamingException e) {
            logger.error("Couldn't upload webapp", e);
            throw new ManagementException("Couldn't upload webapp, nested: " + e.getMessage());
        }
    }

    private Map getLocalAppConfigsAndSource(String hostId, String appName)
            throws ManagementException {
        try {
            Map result = new HashMap();
            String encHostId = JndiNameConverter.encode(hostId);
            String encAppName = JndiNameConverter.encode(appName);
            File appFile = getAppFile(encHostId, encAppName);
            if (!appFile.exists()) {
                throw new ManagementException("Web application \"" + appName + "\" on host \"" + hostId + "\" (" + appFile.getAbsolutePath() + ") not found locally");
            }
            if (appFile.isDirectory()) {
                throw new ManagementException("Web application \"" + appName + "\" on host \"" + hostId + "\" (" + appFile.getAbsolutePath() + ") is not .war file");
            }
            ZipFile war = new ZipFile(appFile);
            ZipEntry zentry = war.getEntry("WEB-INF/web.xml");
            if (zentry != null) {
                result.put("web", NusuthAppConfigFactory.createConfig("web-app", war.getInputStream(zentry)));
            } else {
                result.put("web", NusuthContext.getCommonConfig());
            }
            zentry = war.getEntry("WEB-INF/users.xml");
            if (zentry != null) {
                result.put("users", NusuthAppConfigFactory.createConfig("web-app-users", war.getInputStream(zentry)));
            } else {
                result.put("users", NusuthAppConfigFactory.createConfig("web-app-users", Thread.currentThread().getContextClassLoader().getResourceAsStream("com/azoft/nusuth/deployment/web-app-users.xml")));
            }
            war.close();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream is = new FileInputStream(appFile);
            byte[] buf = new byte[8196];
            for (int readed = is.read(buf); readed > 0; readed = is.read(buf)) {
                bos.write(buf, 0, readed);
            }
            result.put("source", bos.toByteArray());
            bos.close();

            return result;
        } catch (ManagementException e) {
            logger.error("Couldn't get webapp local configs", e);
            throw e;
        } catch (IOException e) {
            logger.error("Couldn't get webapp local configs", e);
            throw new ManagementException("Couldn't get webapp local configs, nested: " + e.getMessage());
        } catch (ParserException e) {
            logger.error("Couldn't get webapp local configs", e);
            throw new ManagementException("Couldn't get webapp local configs, nested: " + e.getMessage());
        }
    }

    private Map getClusterAppConfigsAndSource(String hostName, String appName)
            throws ManagementException {
        try {
            String appPath = "components/hosts/" + JndiNameConverter.encode(hostName)
                    + "/webapps/" + JndiNameConverter.encode(appName);
            Context configsCtx = (Context) clusterContext.lookup(appPath + "/config");
            Context sourceCtx = (Context) clusterContext.lookup(appPath + "/source");
            Map result = new HashMap();
            result.put("nusuth", configsCtx.lookup("nusuth"));
            result.put("web", configsCtx.lookup("web"));
            result.put("users", configsCtx.lookup("users"));
            result.put("source", sourceCtx.lookup("src"));
            return result;
        } catch (NamingException e) {
            logger.warn("Couldn't get app (" + hostName + '/' + appName
                    + ") config from cluster context", e);
            throw new ManagementException("Couldn't get app (" + hostName + '/' + appName
                    + ") config from cluster context" + e.getMessage());
        }
    }

    private void updateRunningAppsMarksOnClusterContext()
            throws ManagementException {
        Set contexts = (Set) NusuthRequestHandler.getHttpContexts().clone();
        contexts.addAll(NusuthRequestHandler.getHttpsContexts());

        try {
            Context hostsContext =
                    (Context) clusterContext.lookup("components/hosts");
            for (NamingEnumeration i = hostsContext.list(""); i.hasMore();) {
                NameClassPair hostNcp = (NameClassPair) i.next();
                String encHostName = hostNcp.getName();
                String hostName = JndiNameConverter.decode(encHostName);
                Context appsContext =
                        (Context) hostsContext.lookup(encHostName + "/webapps");
                for (NamingEnumeration j = appsContext.list(""); j.hasMore();) {
                    NameClassPair appNcp = (NameClassPair) j.next();
                    String encAppName = appNcp.getName();
                    String appName = JndiNameConverter.decode(encAppName);
                    String hap = hostName + '/' + appName;
                    if (contexts.contains(hap)) {
                        markAppAsRunning(encHostName, encAppName);
                    } else {
                        unmarkAppAsRunning(encHostName, encAppName);
                    }
                }
            }
        } catch (NamingException e) {
            logger.error("Couldn't update run marks on webapps", e);
            throw new ManagementException("Couldn't update run marks on webapps,"
                    + " nested: " + e.getMessage());
        }
    }

    private void markAppAsRunning(String encHostName, String encAppName)
            throws NamingException {
        String path = "components/hosts/" + encHostName + "/webapps/"
                + encAppName + "/config";
        Attributes attrs = clusterContext.getAttributes(path,
                new String[]{
                    "containers",
                    "containers-to-deploy"
                }
        );
        Attribute containers = attrs.get("containers");
        if (containers == null) {
            containers = new BasicAttribute("containers");
        }
        containers.add(componentId);
        Attribute containers2deploy = attrs.get("containers-to-deploy");
        if (containers2deploy == null) {
            containers2deploy = new BasicAttribute("containers-to-deploy");
        }
        containers2deploy.add(componentId);
        attrs.put(containers);
        attrs.put(containers2deploy);
        clusterContext.modifyAttributes(path, DirContext.REPLACE_ATTRIBUTE, attrs);
    }

    private void unmarkAppAsRunning(String encHostName, String encAppName)
            throws NamingException {
        String path = "components/hosts/" + encHostName + "/webapps/"
                + encAppName + "/config";
        Attributes attrs = clusterContext.getAttributes(path,
                new String[]{
                    "containers",
                    "containers-to-deploy"
                }
        );
        Attribute containers = attrs.get("containers");
        if (containers == null) {
            containers = new BasicAttribute("containers");
        }
        containers.remove(componentId);
        Attribute containers2deploy = attrs.get("containers-to-deploy");
        if (containers2deploy == null) {
            containers2deploy = new BasicAttribute("containers-to-deploy");
        }
        containers2deploy.remove(componentId);
        attrs.put(containers);
        attrs.put(containers2deploy);
        clusterContext.modifyAttributes(path, DirContext.REPLACE_ATTRIBUTE, attrs);
    }

    private CompositeNusuthWebAppElement
            getFullHostConfigFromClusterContext(String hostId)
            throws ManagementException {
        CompositeNusuthWebAppElement result =
                (CompositeNusuthWebAppElement) hostConfigSample.clone();

        try {
            result.setSimpleChild("id").setContent(hostId);

            String prefix = "components/hosts/" + JndiNameConverter.encode(hostId);
            CompositeNusuthWebAppElement hostClusterConfig =
                    (CompositeNusuthWebAppElement) clusterContext.lookup(prefix + "/config");
            ManagementUtil.copyCompositeElement(hostClusterConfig, result);

            Context webappsContext =
                    (Context) clusterContext.lookup("components/hosts/"
                    + JndiNameConverter.encode(hostId)
                    + "/webapps");
            Map clusterApps = new HashMap();
            for (NamingEnumeration appsEnum = webappsContext.list("");
                 appsEnum.hasMore();
                    ) {
                NameClassPair ncp = (NameClassPair) appsEnum.next();
                String encodedAppName = ncp.getName();
                String appName = JndiNameConverter.decode(encodedAppName);
                logger.debug("Lookup for: " + encodedAppName);
                Context clusterAppContext =
                        (Context) webappsContext.lookup(encodedAppName);
                CompositeNusuthWebAppElement clusterAppConfig =
                        (CompositeNusuthWebAppElement)
                        clusterAppContext.lookup("config/nusuth");
                logger.debug("  Get apps: " + hostId + '/' + appName + "\n" + clusterAppConfig);
                CompositeNusuthWebAppElement zzz = result.addCompositeChild("context");
                ManagementUtil.copyCompositeElement(clusterAppConfig, zzz);
            }
        } catch (Exception e) {
            logger.error("Couldn't get host config from cluster context", e);
            throw new ManagementException("Couldn't get host config from cluster "
                    + "context, nested: " + e.getMessage());
        }

        return result;
    }

    private void restartApp(String hostName, String appName)
            throws ManagementException {
        String encHostName = JndiNameConverter.encode(hostName);
        String encAppName = JndiNameConverter.encode(appName);
        try {
            DirContext cac =
                    (DirContext) clusterContext.lookup("components/hosts/"
                    + encHostName
                    + "/webapps/"
                    + encAppName);
            CompositeNusuthWebAppElement localHostConfig =
                    ManagementUtil.getCompositeElement(settings, "host",
                            "id", hostName);
            CompositeNusuthWebAppElement localAppConfig =
                    ManagementUtil.getCompositeElement(localHostConfig, "context",
                            "path", appName);
            if (localAppConfig == null) {
                localAppConfig = localHostConfig.addCompositeChild("context");
                localAppConfig.setSimpleChild("path").setContent(appName);
                localAppConfig.setSimpleChild("location").setContent(appName + ".war");
            }
            updateLocalAppConfig(hostName, appName, localAppConfig, cac);
            removeApp(hostName, appName);
            addApplication(localAppConfig, hostName,
                    getAppFile(encHostName, encAppName).getAbsolutePath(),
                    "");
        } catch (Exception e) {
            logger.error("Couldn't restart app \"" + hostName + '/' + appName + '"',
                    e);
            throw new ManagementException("Couldn't restart app \"" + hostName + '/'
                    + appName + '"' + e.getMessage());
        }
    }


    public CompositeNusuthWebAppElement getCommonConfig() {
        return (CompositeNusuthWebAppElement) commonConfig.clone();
    }

    public CompositeNusuthWebAppElement getContextConfigById(String id) {
        if (contextID2config.get(id) != null) {
            return (CompositeNusuthWebAppElement)
                    ((CompositeNusuthWebAppElement) contextID2config.get(id)).clone();
        } else {
            return null;
        }
    }

    public NusuthContext getContextById(String id) {
        return (NusuthContext) contextID2context.get(id);
    }

    public Enumeration getContextIds() {
        return contextID2config.keys();
    }

    public void addApplication(String virtualHost, String appUrl, InputStream application)
            throws ManagementException {
        System.out.println("addApplication(virtualHost=\"" + virtualHost + "\", url=\"" + appUrl + "\")");
        throw new ManagementException("Method not yet implemented");
    }

    private void setAdminContext(String location)
            throws ManagementException, DeploymentException {
        String path = "/";
        NusuthContext con
                = new NusuthContext((new File(location)).getAbsolutePath(),
                        convertName(path), workDir,
                        logsLocation.getAbsolutePath(), null,
                        "never");
        con.setAttribute("container", this);
        configContext = con;
        if (isStandAlone) {
            con.setSessionManager(new DefaultSessionManager(con, true));
        } else {
            con.setSessionManager(new DistributedSessionManager(con));
        }
        adminApplications.put(path, con);
    }

    private void addVirtualHost(CompositeNusuthWebAppElement vhostSettings) throws ManagementException, DeploymentException {
        Enumeration enum = vhostSettings.getSimpleChild("id");
        String hostId = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim().toLowerCase();

        enum = vhostSettings.getSimpleChild("autoload");
        boolean autoload = false;
        if (enum.hasMoreElements()) {
            String autoloadStr = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim();
            if (autoloadStr.equalsIgnoreCase("true") || autoloadStr.equalsIgnoreCase("on") || autoloadStr.equalsIgnoreCase("yes")) {
                autoload = true;
            } else if (autoloadStr.equalsIgnoreCase("false") || autoloadStr.equalsIgnoreCase("off") || autoloadStr.equalsIgnoreCase("no")) {
                autoload = false;
            } else {
                throw new IllegalArgumentException("Cannot recognize value " + autoloadStr + " of autoload element; use true or false");
            }
        }

        String docBase = ((SimpleNusuthWebAppElement) vhostSettings.getSimpleChild("doc-base").nextElement()).getContent().trim();
        String sessionBackup = ManagementUtil.getSimpleString(vhostSettings, "session-backup");
        if (sessionBackup == null || sessionBackup.trim().length() == 0) {
            sessionBackup = "never";
        } else if (!sessionBackup.trim().equalsIgnoreCase("never")
                && !sessionBackup.trim().equalsIgnoreCase("always")
                && !sessionBackup.trim().equalsIgnoreCase("shutdown")) {
            throw new IllegalArgumentException("Cannot recognize value "
                    + sessionBackup + " for session-backup"
                    + " element");
        }

        File root = new File(docBase);
        if (!root.exists() || !root.isDirectory()) {
            throw new ManagementException("Cannot find doc-base \"" + docBase + "\" for host " + hostId);
        }


        processAccessLogForHost(vhostSettings, hostId);
/*
    enum = vhostSettings.getCompositeChild("access-log");

    //here the default values for access logger:
    //type - COMMON,
    //location - $LOGGER_DIR/access.$hostId.log (the same defined for common logger),
    //resolve - false

    int type = AccessLogWrapper.TYPE_COMMON;
    File accessLogFile = new File(logsLocation, "access"+((hostId.equals("*")) ? "" : "."+hostId)+".log");
    boolean resolve = false;

    if (enum.hasMoreElements()) {
      CompositeNusuthWebAppElement accessLog = (CompositeNusuthWebAppElement)enum.nextElement();
      Enumeration logSubElementEnum = accessLog.getSimpleChild("type");
      if (logSubElementEnum.hasMoreElements()) {
        String logType = ((SimpleNusuthWebAppElement)logSubElementEnum.nextElement()).getContent().trim();
        if (logType.equalsIgnoreCase("none")) {
          type = AccessLogWrapper.TYPE_NONE;
        } else if (logType.equalsIgnoreCase("common")) {
          type = AccessLogWrapper.TYPE_COMMON;
        } else if (logType.equalsIgnoreCase("extended")) {
          type = AccessLogWrapper.TYPE_EXTENDED;
        } else {
          throw new ManagementException("Unrecognized value \""+logType+"\" of type element in access-log for host "+hostId);
        }
      }

      logSubElementEnum = accessLog.getSimpleChild("location");
      if (logSubElementEnum.hasMoreElements()) {
        String newLocation = ((SimpleNusuthWebAppElement)logSubElementEnum.nextElement()).getContent().trim();
        File logsDir = new File(newLocation);
        if ((logsDir.exists() && !logsDir.isDirectory()) || (!logsDir.exists() && !logsDir.mkdirs())) {
          logger.warn("Cannot find or create log dir "+newLocation+" for host "+hostId+", using default");
        } else {
          accessLogFile = new File(new File(newLocation),  "access"+((hostId.equals("*")) ? "" : "."+hostId)+".log");
        }
      }

      logSubElementEnum = accessLog.getSimpleChild("resolve");
      if (logSubElementEnum.hasMoreElements()) {
        String newResolve = ((SimpleNusuthWebAppElement)logSubElementEnum.nextElement()).getContent().trim();
        if (newResolve.equalsIgnoreCase("true") || newResolve.equalsIgnoreCase("on") || newResolve.equalsIgnoreCase("yes")) {
          resolve = true;
        } else if (newResolve.equalsIgnoreCase("false") || newResolve.equalsIgnoreCase("off") || newResolve.equalsIgnoreCase("no")) {
          resolve = false;
        } else {
          throw new ManagementException("Cannot recognize value \""+newResolve+"\" of resolve element for host "+hostId+"; use true or false");
        }
      }
    }
    String accessLogLocation = null;
    try {
      if (!accessLogFile.exists() && !accessLogFile.createNewFile()) {
        throw new ManagementException("Cannot create access log file "+accessLogFile+" for host "+hostId);
      }
      accessLogLocation = accessLogFile.getCanonicalPath();
    } catch(IOException ioex) {
      throw new ManagementException("Error while creating access log file "+accessLogFile+" for host "+hostId+", nested: "+ioex);
    }
    AccessLogWrapper wrapper = new AccessLogWrapper(accessLogLocation, type, resolve);
    try {
      wrapper.setCategory(new AccessLogger(accessLogFile, 20000000, 10));
    } catch(IOException ioex) {
      throw new ManagementException("Error while opening access log file "+accessLogFile+" for host "+hostId+", nested: "+ioex);
    }

    StrBuffer strHostId = new StrBuffer(hostId.length());
    strHostId.append(hostId);
    HttpProtocolAdapter.addCategory(strHostId, wrapper);
*/


        StrBuffer strHostId = new StrBuffer(hostId.length());
        strHostId.append(hostId);
        allHosts.add(strHostId);

        logger.info("Virtual host " + hostId + " successfully bound with doc-base " + docBase);

        enum = vhostSettings.getCompositeChild("context");
        while (enum.hasMoreElements()) {
            addApplication((CompositeNusuthWebAppElement) enum.nextElement(), hostId, docBase, sessionBackup.trim().toLowerCase());
        }

        if (autoload) {
            doAutoload(new File(docBase), hostId, sessionBackup.trim().toLowerCase());
        }

        NusuthRequestHandler.setApplicationContext(applications);
        NusuthRequestHandler.setHTTPContexts(httpContexts);
        NusuthRequestHandler.setHTTPSContexts(httpsContexts);
    }

    private void processAccessLogForHost(CompositeNusuthWebAppElement
            vhostSettings, String hostId)
            throws ManagementException, DeploymentException {
        Enumeration enum = vhostSettings.getCompositeChild("access-log");

        //here the default values for access logger:
        //type - COMMON,
        //location - $LOGGER_DIR/access.$hostId.log (the same defined for common logger),
        //resolve - false

        int type = AccessLogWrapper.TYPE_COMMON;
        File accessLogFile = new File(logsLocation, "access" + ((hostId.equals("*")) ? "" : "." + hostId) + ".log");
        boolean resolve = false;

        if (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement accessLog = (CompositeNusuthWebAppElement) enum.nextElement();
            Enumeration logSubElementEnum = accessLog.getSimpleChild("type");
            if (logSubElementEnum.hasMoreElements()) {
                String logType = ((SimpleNusuthWebAppElement) logSubElementEnum.nextElement()).getContent().trim();
                if (logType.equalsIgnoreCase("none")) {
                    type = AccessLogWrapper.TYPE_NONE;
                } else if (logType.equalsIgnoreCase("common")) {
                    type = AccessLogWrapper.TYPE_COMMON;
                } else if (logType.equalsIgnoreCase("extended")) {
                    type = AccessLogWrapper.TYPE_EXTENDED;
                } else {
                    throw new ManagementException("Unrecognized value \"" + logType + "\" of type element in access-log for host " + hostId);
                }
            }

            logSubElementEnum = accessLog.getSimpleChild("location");
            if (logSubElementEnum.hasMoreElements()) {
                String newLocation = ((SimpleNusuthWebAppElement) logSubElementEnum.nextElement()).getContent().trim();
                File logsDir = new File(newLocation);
                if ((logsDir.exists() && !logsDir.isDirectory()) || (!logsDir.exists() && !logsDir.mkdirs())) {
                    logger.warn("Cannot find or create log dir " + newLocation + " for host " + hostId + ", using default");
                } else {
                    accessLogFile = new File(new File(newLocation), "access" + ((hostId.equals("*")) ? "" : "." + hostId) + ".log");
                }
            }

            logSubElementEnum = accessLog.getSimpleChild("resolve");
            if (logSubElementEnum.hasMoreElements()) {
                String newResolve = ((SimpleNusuthWebAppElement) logSubElementEnum.nextElement()).getContent().trim();
                if (newResolve.equalsIgnoreCase("true") || newResolve.equalsIgnoreCase("on") || newResolve.equalsIgnoreCase("yes")) {
                    resolve = true;
                } else if (newResolve.equalsIgnoreCase("false") || newResolve.equalsIgnoreCase("off") || newResolve.equalsIgnoreCase("no")) {
                    resolve = false;
                } else {
                    throw new ManagementException("Cannot recognize value \"" + newResolve + "\" of resolve element for host " + hostId + "; use true or false");
                }
            }
        }
        String accessLogLocation = null;
        try {
            if (!accessLogFile.exists() && !accessLogFile.createNewFile()) {
                throw new ManagementException("Cannot create access log file " + accessLogFile + " for host " + hostId);
            }
            accessLogLocation = accessLogFile.getCanonicalPath();
        } catch (IOException ioex) {
            throw new ManagementException("Error while creating access log file " + accessLogFile + " for host " + hostId + ", nested: " + ioex);
        }
        AccessLogWrapper wrapper = new AccessLogWrapper(accessLogLocation, type, resolve);
        try {
            wrapper.setCategory(new AccessLogger(accessLogFile, 20000000, 10));
//            wrapper.setCategory(new AccessLogger(accessLogFile, 2000, 3));
        } catch (IOException ioex) {
            throw new ManagementException("Error while opening access log file " + accessLogFile + " for host " + hostId + ", nested: " + ioex);
        }

        StrBuffer strHostId = new StrBuffer(hostId.length());
        strHostId.append(hostId);
        HttpProtocolAdapter.addCategory(strHostId, wrapper);
    }

    /**
     * This method bind application to the corresponding virtual host.
     * @param appSettings Application settings config
     * @param vhostId Virtual host id to which application will bound
     * @param docBase Application docbase.
     * @param backup Indicates the level of session failover.
     */
    private void addApplication(CompositeNusuthWebAppElement appSettings,
                                String vhostId, String docBase, String backup)
            throws ManagementException, DeploymentException {
        logger.info("Add application to virtual host \"" + vhostId + '"');
        boolean isHttpContext = true;
        boolean isHttpsContext = false;
        boolean precompile = false;
        boolean needLoad
                = ManagementUtil.getSimpleBoolean(appSettings,
                        "load-on-startup", true);
        String appPath = ((SimpleNusuthWebAppElement) appSettings.
                getSimpleChild("path").nextElement()).getContent().trim();
        if (!appPath.startsWith("/")) {
            appPath = "/" + appPath;
        }
        String appLocation = ((SimpleNusuthWebAppElement) appSettings.
                getSimpleChild("location").nextElement()).getContent().trim();
        String sessionBackup = ManagementUtil.getSimpleString(appSettings,
                "session-backup");
        if (sessionBackup.trim().length() == 0) {
            sessionBackup = "never";
        } else if (!sessionBackup.trim().equalsIgnoreCase("never")
                && !sessionBackup.trim().equalsIgnoreCase("always")
                && !sessionBackup.trim().equalsIgnoreCase("shutdown")) {
            throw new IllegalArgumentException("Cannot recognize value "
                    + sessionBackup + " for session-backup"
                    + " element");
        }
        createApplication(docBase, appLocation);
        boolean allow = true;
        if (!appPath.equals("/")) {
            Enumeration enum = applications.keys();
            while (enum.hasMoreElements()) {
                String path = (String) enum.nextElement();
                if (!path.substring(path.indexOf("/")).equals("/")) {
                    if (path.startsWith(vhostId + appPath)) {
                        String tmpPath = path.substring((vhostId + appPath).length());
                        if (tmpPath.startsWith("/") || tmpPath.length() == 0) {
                            allow = false;
                            break;
                        }
                    } else if ((vhostId + appPath).startsWith(path)) {
                        String tmpPath = (vhostId + appPath).substring(path.length());
                        if (tmpPath.startsWith("/") || tmpPath.length() == 0) {
                            allow = false;
                            break;
                        }
                    }
                }
            }
        }
        if (allow) {
            Enumeration enum = appSettings.getSimpleChild("protocol");
            if (enum.hasMoreElements()) {
                String protocol = ((SimpleNusuthWebAppElement) enum.nextElement()).
                        getContent().trim().toLowerCase();
                StringTokenizer tokenizer = new StringTokenizer(protocol, ",");
                isHttpContext = false;
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken().trim();
                    if (token.equals("http")) {
                        isHttpContext = true;
                    } else if (token.equals("https")) {
                        if (sslServer != null) {
                            isHttpsContext = true;
                        } else {
                            logger.warn("SSL server not defined - cannot use https for "
                                    + "application " + appPath + " in host " + vhostId);
                        }
                    } else {
                        logger.warn("Unknown protocol \"" + token
                                + "\" defined for application "
                                + appPath + " - skipped");
                    }
                }
            }
            enum = appSettings.getSimpleChild("jsp-precompile");
            if (enum.hasMoreElements()) {
                String precompileStr = ((SimpleNusuthWebAppElement) enum.nextElement())
                        .getContent().trim();
                if (precompileStr.equalsIgnoreCase("true")
                        || precompileStr.equalsIgnoreCase("on")) {
                    precompile = true;
                } else if (precompileStr.equalsIgnoreCase("false")
                        || precompileStr.equalsIgnoreCase("off")) {
                    precompile = false;
                } else {
                    logger.warn("Unknown value \"" + precompileStr + "\" of jsp-precompile "
                            + "element in application " + appPath
                            + ", using false by default");
                }
            }

            if (isHttpContext) {
                StrBuffer buff = new StrBuffer();
                buff.append(vhostId + (appPath.equals("/") ? "" : appPath));
                httpContexts.add(buff);
            }

            if (isHttpsContext) {
                StrBuffer buff = new StrBuffer();
                buff.append(vhostId + (appPath.equals("/") ? "" : appPath));
                httpsContexts.add(buff);
            }

            File appLocationFile;
            try {
                appLocationFile = new File(docBase, appLocation);
                if (!appLocationFile.exists() ||
                        !(appLocationFile.isDirectory()
                        || appLocationFile.getName().endsWith(".war"))) {
                    appLocationFile = new File(appLocation);
                    appLocation = appLocationFile.getAbsolutePath();
                    loadedAppsLocation.add(appLocation);
                } else {
                    appLocation = appLocationFile.getAbsolutePath();
                    loadedAppsLocation.add(appLocation);
                }


                if (!needLoad) {
                    CompositeNusuthWebAppElement config
                            = NusuthAppConfigFactory.createConfig("web-app",
                                    appLocation
                            + File.separator
                            + "WEB-INF" + File.separator
                            + "web.xml");
                    contextID2config.put(vhostId + appPath, config);
                    return;
                }

/*
        if (appLocationFile.getName().endsWith(".war")) {
          String dirName = convertName(appLocationFile.getCanonicalPath());
          File warLocFile = new File(workDir, dirName);
          handleWar(warLocFile, appLocationFile);
          appLocation = warLocFile.getAbsolutePath();
        }
*/
                NusuthContext context =
                        new NusuthContext(appLocation, vhostId + appPath,
                                workDir, logsLocation.getAbsolutePath(),
                                appSettings,
                                sessionBackup != null
                        ? sessionBackup.trim().toLowerCase()
                        : backup);
                if (!isStandAlone && context.isDistributable()) {
                    DistributedSessionManager sm = new DistributedSessionManager(context);
                    localContainer.registerManager(convertName(vhostId + appPath), sm);
                    context.setSessionManager(sm);
                    listener.registerComponentDistributionListener(vhostId + appPath, sm);
                } else {
                    context.setSessionManager(new DefaultSessionManager(context, true));
                }
                context.setContainerId(componentId);
                if (precompile) {
                    compileJsp(context);
                }
                applications.put(vhostId + appPath, context);

                logger.info("Application " + appPath + " bound in host " + vhostId);
//******************************************************************************
//        if (context.getServletContextName() == null
//                || !context.getServletContextName().equals("_config_")) {
                contextID2config.put(vhostId + appPath, context.getConfig().clone());
                contextID2context.put(vhostId + appPath, context);
//        }
//        if (context.getServletContextName() != null
//                && context.getServletContextName().equals("_config_")) {
//          context.setAttribute("container", this);
//          configContext = context;
//        }
//******************************************************************************
            } catch (Exception mex) {
                //Logger.log("Cannot bind application", mex, 1);
                logger.error("Cannot bind application", mex);
            }
        } else {
            logger.error("Cannot bind application " + vhostId + appPath
                    + ", since conflict occured in this URL namespace "
                    + "with other contexts.");
        }
    }

    private void createApplication(String docBase, String appLocation) {
        File f1 = new File(appLocation);
        File appDir = new File(docBase, appLocation);
        if ((f1.exists() && f1.isDirectory())
                || (appDir.exists() && appDir.isDirectory())) {
            return;
        }
        if (f1.isAbsolute()) {
            appDir = new File(appLocation);
        }
        if (!appDir.exists()) {
            if (!appDir.mkdirs()) {
                logger.error("Cannot create directory " + appDir.getAbsolutePath());
                return;
            }
            File webinfDir = new File(appDir, "WEB-INF");
            if (!webinfDir.mkdirs()) {
                logger.error("Cannot create directory " + webinfDir.getAbsolutePath());
                return;
            }
            File web = new File(webinfDir, "web.xml");
            try {
                FileWriter wr = new FileWriter(web);
                wr.write("<web-app></web-app>");
                wr.close();
            } catch (IOException e) {
                logger.error("Cannot create web.xml file: ", e);
            }
        } else {
            File webinfDir = new File(appDir, "WEB-INF");
            if (!webinfDir.exists()) {
                if (!webinfDir.mkdirs()) {
                    logger.error("Cannot create directory " + webinfDir.getAbsolutePath());
                    return;
                }
                File web = new File(webinfDir, "web.xml");
                try {
                    FileWriter wr = new FileWriter(web);
                    wr.write("<web-app></web-app>");
                    wr.close();
                } catch (IOException e) {
                    logger.error("Cannot create web.xml file: ", e);
                }
            }
        }
    }

    private void removeHost(String id) {
        logger.info("Remove host \"" + id + '"');
        allHosts.remove(id);
        Enumeration enum = applications.keys();
        while (enum.hasMoreElements()) {
            String key = (String) enum.nextElement();
            if (key.startsWith(id)) {
                NusuthContext context = (NusuthContext) applications.remove(key);
                context.shutdownContext(true);
            }
        }
    }

    private void removeApp(String hostName, String appName) {
        logger.info("Remove app \"" + appName + "\" on host \"" + hostName + '"');
        NusuthContext context =
                (NusuthContext) applications.remove(hostName + '/' + appName);
        if (context != null) {
            context.shutdownContext(true);
        } else {
            logger.debug("Couldn't find app \"" + appName + "\" on host \""
                    + hostName + "\" for delete");
        }
    }

    public Hashtable getContexts() {
        return contextID2context;
    }

    public CompositeNusuthWebAppElement getContainerElement() {
        return (CompositeNusuthWebAppElement) settings.clone();
    }

    public final String getComponentType() {
        return "container";
    }


    public ContainerState getState() {
        logger.debug("Get state");
        ContainerState containerState = new ContainerState();
        containerState.setActiveHandlers(tcpServer.getNowActive());
        containerState.setTotalHandlers(tcpServer.getNowHandlers());
        containerState.setActiveKeepAlives(tcpServer.getNowKeepalive());
        CountInfo[] requestCount = new CountInfo[applications.size()];
        CountInfo[] sessionCount = new CountInfo[applications.size()];
        Enumeration enum = applications.keys();
        int count = 0;
        while (enum.hasMoreElements()) {
            String appName = (String) enum.nextElement();
            NusuthContext context = (NusuthContext) applications.get(appName);
            requestCount[count] = new CountInfo();
            requestCount[count].setSubject(appName);
            requestCount[count].setCount(context.getCurrentRequestsCount());
            sessionCount[count] = new CountInfo();
            sessionCount[count].setSubject(appName);
            sessionCount[count].setCount(context.getCurrentSessionsCount());
            count++;
        }
        containerState.setRequestCount(requestCount);
        containerState.setSessionCount(sessionCount);
        return containerState;
    }


    public InputStream getVirtualHosts()
            throws ManagementException {
        logger.debug("Get virtual hosts");
        CompositeNusuthWebAppElement result;
        try {
            result = NusuthAppConfigFactory.createConfig(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, new ByteArrayInputStream(ManagementUtil.EMPTY_APPLICATION_DEPLOYMENT_XML.getBytes()));
            HashSet containers = new HashSet(1);
            containers.add(ManagementUtil.getSimpleString(settings, "name"));
            HashSet protocols = new HashSet(1);
            protocols.add("http");

            for (Enumeration hosts = settings.getCompositeChild("host"); hosts.hasMoreElements();) {
                CompositeNusuthWebAppElement hostNode = (CompositeNusuthWebAppElement) hosts.nextElement();
                VirtualHostInfo vhost = new VirtualHostInfo(hostNode);
                for (Iterator i = vhost.getApplications().values().iterator(); i.hasNext();) {
                    ApplicationInfoImpl app = (ApplicationInfoImpl) i.next();
                    app.getContainers().addAll(containers);
                    app.getProtocols().addAll(protocols);
                }
                vhost.addCompositeChild(result);
            }
        } catch (DeploymentException dex) {
            logger.error("Couldn't get contexts info from container", dex);
            throw new ManagementException("Couldn't get contexts info from container, nested:" + dex.getMessage());
        }
        return new ByteArrayInputStream(result.compose(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, ManagementUtil.APPLICATION_DEPLOYMENT_TYPE + ".dtd").getBytes());
    }


    public void patchApplication(String virtualHost, String appUrl, InputStream patch, boolean overwrite)
            throws ManagementException {
        logger.info("patchApplication(virtualHost=\"" + virtualHost + "\", url=\"" + appUrl + "\", ..., overwrite = " + overwrite + ")");
        //System.out.println("patchApplication(virtualHost=\"" + virtualHost + "\", url=\"" + appUrl + "\", ..., overwrite = " + overwrite + ")");
        ZipInputStream zis = new ZipInputStream(patch);
        try {
            ZipEntry entry = null;
            do {
                entry = zis.getNextEntry();
                if (entry != null) {
                    String entryName = entry.getName();
                    if (entry.isDirectory()) {
                        //System.out.println("Received directory "+entryName);
                    } else {
                        //System.out.println("Received file "+entryName);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buff = new byte[1024];
                        int readed = -1;
                        while ((readed = zis.read(buff)) >= 0) {
                            baos.write(buff, 0, readed);
                        }
                        //System.out.println(baos.toString());
                    }
                }
            } while (entry != null);
        } catch (IOException ioex) {
            logger.error("Error occured while reading patch", ioex);
            throw new ManagementException("Error occured while reading patch, nested: " + ioex);
        }
    }


    public void removeApplication(String virtualHost, String appUrl)
            throws ManagementException {
        System.out.println("removeApplication(virtualHost=\"" + virtualHost + "\", url=\"" + appUrl + "\")");
        throw new ManagementException("Method not yet implemented");
    }


    public void replaceApplicationContent(String virtualHost, String appUrl, InputStream content)
            throws ManagementException {
        System.out.println("replaceApplicationContent(virtualHost=\"" + virtualHost + "\", url=\"" + appUrl + "\")");
        throw new ManagementException("Method not yet implemented");
    }


    public void setSettings(InputStream newConfig)
            throws ManagementException {
        logger.info("Set settings");
        CompositeNusuthWebAppElement newSettings = null;
        try {
            newSettings = NusuthAppConfigFactory.createConfig(getComponentType(), newConfig);
        } catch (ParserException pex) {
            logger.error("Couldn't get new settings", pex);
            throw new ManagementException("Couldn't get new settings, nested:" + pex.getMessage());
        }
        try {
            if (isRestartNeeded(newSettings)) {
                logger.error("Cannot apply settings - restart needed");
                throw new ManagementException("Cannot apply settings - restart needed");
            }
            applySettings(newSettings);
        } catch (DeploymentException dex) {
            logger.error("Cannot apply settings", dex);
            throw new ManagementException("Cannot apply settings, nested: " + dex);
        }
        this.settings = newSettings;
        saveSettings();
    }


    public void startApplication(String virtualHost, String appUrl)
            throws ManagementException {
        System.out.println("startApplication(virtualHost=\"" + virtualHost + "\", url=\"" + appUrl + "\")");
        throw new ManagementException("Method not yet implemented");
    }


    public void startServer() {
        System.out.println("startServer()");
    }


    public void stopApplication(String virtualHost, String appUrl)
            throws ManagementException {
        System.out.println("stopApplication(virtualHost=\"" + virtualHost + "\", url=\"" + appUrl + "\")");
        throw new ManagementException("Method not yet implemented");
    }


    public void stopServer() {
        System.out.println("stopServer()");
    }

    private String generateSecretKey() {
        return Long.toHexString(System.currentTimeMillis()) + Long.toHexString((long) (Math.random() * 1e16));
    }

    private List convertAdminIp(String adminIp) {
        List result = new ArrayList();
        if (adminIp != null) {
            StringTokenizer st = new StringTokenizer(adminIp.trim(), ";");
            while (st.hasMoreTokens()) {
                result.add(st.nextToken().trim());
            }
        }
        if (!result.contains("127.0.0.1")) {
            result.add("127.0.0.1");
        }
        try {
            String la = java.net.InetAddress.getLocalHost().getHostAddress();
            if (!result.contains(la)) {
                result.add(la);
            }
        } catch (Exception ex) {
        }
        return result;
    }

    private void compileJsp(NusuthContext context) {
        String fullPath = context.getDocBase();
        File root = new File(fullPath);
        try {
            context.getJspLoader().loadJsp(fullPath, root, true);
        } catch (Exception e) {
        }
    }

    /**
     * This method deploy war File to given directory.
     * @param whereDir Directory to deploy.
     * @param warFile war file name.
     * @throws IOException Throws if any errors occurs during deploying.
     */
    private void deployWar(File whereDir, File warFile) throws IOException {
        if (whereDir.exists()) {
            logger.warn("Application \"" + warFile.getName() + "\" already deployed.");
            return;
        }
        ZipInputStream zipStream
                = new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(warFile)));
        ZipEntry entry = zipStream.getNextEntry();
        byte[] buf = new byte[1024];
        while (entry != null) {
            String name = entry.getName();
            File file = new File(whereDir, name);
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
                BufferedOutputStream bos = new BufferedOutputStream(
                        new FileOutputStream(file));
                int len = -1;
                do {
                    len = zipStream.read(buf, 0, 1024);
                    if (len != -1) {
                        bos.write(buf, 0, len);
                    }
                } while (len > -1);
                bos.flush();
                bos.close();
            }
            zipStream.closeEntry();
            entry = zipStream.getNextEntry();
        }
    }

    /**
     * This method deploy war File with given name to given host.
     * @param hostId host to deploy.
     * @param warFileName war file name.
     * @throws DeploymentException Throws if any errors occurs during deploying.
     */
    public void deployWar(String hostId, String warFileName)
            throws DeploymentException {
        CompositeNusuthWebAppElement hostElement
                = ManagementUtil.getCompositeElement(settings, "host",
                        "id", hostId);
        if (hostElement != null) {
            String base = ManagementUtil.getSimpleString(hostElement, "doc-base");
            File dirToDeploy
                    = new File(base, warFileName.substring(0,
                            warFileName.length() - 4));
            File warFile = new File(repositoryDir, warFileName);
            try {
                deployWar(dirToDeploy, warFile);
            } catch (IOException e) {
                logger.error("Cannot deploy \"" + warFileName + "\" to host \""
                        + hostId + "\", nested :", e);
            }
        }
    }

    /**
     * This method undeploy (remove) application with given name from given host.
     * @param dirName Directory name where application deployed.
     * @param hostId Host id from which remove application.
     * @throws DeploymentException Throws if any errors ocuurs during
     * undpeploying.
     */
    public void undeployApplication(String hostId, String dirName)
            throws DeploymentException {
        CompositeNusuthWebAppElement hostElement
                = ManagementUtil.getCompositeElement(settings, "host",
                        "id", hostId);
        if (hostElement != null) {
            String base = ManagementUtil.getSimpleString(hostElement, "doc-base");
            File dir = new File(base, dirName);
            if (dir.exists()) {
                if (!dir.delete()) {
                    logger.error("Cannot remove \"" + dir.getAbsolutePath() + "\" from disk");
                }
            } else {
                logger.error("Cannot undeploy \"" + dirName
                        + "\", nested: Directory not found");
            }
        } else {
            logger.error("Cannot undeploy \"" + dirName
                    + "\", nested: Host \"" + hostId + "\" not found");
        }
    }

    /**
     * This method add war file to war repository.
     * @param warName war file name.
     * @param is Input stream from which possible to read data.
     * @throws IOException Throws if any errors occurs during reading or writing
     * data.
     */
    public void addWarToRepository(String warName, InputStream is)
            throws IOException {
        byte[] buf = new byte[1024];
        File resultFile = new File(warRepository, warName);
        if (resultFile.exists()) {
            throw new IllegalArgumentException("Cannot add war file \"" + warName
                    + "\" to repository. File with the "
                    + "same name already exist");
        } else {
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(resultFile);
                int readed = 0;
                while ((readed = is.read(buf, 0, buf.length)) != -1) {
                    os.write(buf, 0, readed);
                }
            } finally {
                os.close();
            }
        }
    }

    /**
     * This method return an array of all war file names which can be found
     * in war repository.
     * @return An array of all war file names which can be found in war
     * repository.
     */
    public String[] listWars() {
        return (new File(warRepository)).list(new WarFilenameFilter());
    }

    /**
     * This method start application with given contextPath on given virtual host
     * or do nothing if such application already started or cannot be found.
     * @param hostId Host id.
     * @param contextPath Context path of application.
     */
    public void runApplication(String hostId, String contextPath)
            throws DeploymentException {
        if (applications.get(hostId + contextPath) != null) {
            logger.error("Cannot start application \"" + contextPath
                    + "\" on virtual host \"" + hostId + "\". Application with this "
                    + "name already started");
            return;
        }
        CompositeNusuthWebAppElement hostElement
                = ManagementUtil.getCompositeElement(settings, "host",
                        "id", hostId);
        if (hostElement != null) {
            CompositeNusuthWebAppElement contextElement =
                    ManagementUtil.getCompositeElement(hostElement, "context",
                            "path", contextPath);
            if (contextElement != null) {
                String docBase = ManagementUtil.getSimpleString(hostElement,
                        "doc-base");
                String sessionBackup = ManagementUtil.getSimpleString(hostElement,
                        "session-backup");
                if (sessionBackup == null || sessionBackup.trim().length() == 0) {
                    sessionBackup = "never";
                } else if (!sessionBackup.trim().equalsIgnoreCase("never")
                        && !sessionBackup.trim().equalsIgnoreCase("always")
                        && !sessionBackup.trim().equalsIgnoreCase("shutdown")) {
                    throw new IllegalArgumentException("Cannot recognize value "
                            + sessionBackup
                            + " for session-backup element");
                }
                try {
                    addApplication(contextElement, hostId, docBase, sessionBackup);
                } catch (ManagementException e) {
                    logger.error("Cannot start application \"" + contextPath
                            + "\" on virtual host \"" + hostId
                            + "\", nested: ", e);
                }
            } else {
                logger.error("Cannot start application \"" + contextPath
                        + "\" on virtual host \"" + hostId
                        + "\". Context element not found");
            }
        } else {
            logger.error("Cannot start application \"" + contextPath
                    + "\" on virtual host \"" + hostId
                    + "\". Host element not found");
        }
    }

    /**
     * This method shutdown application with given context path on given virtual
     * host or do nothing if such application can not be found.
     * @param contextPath Context path of application.
     * @param hostId Virtual host id.
     */
    public void shutdownApplication(String hostId, String contextPath) {
        if (applications.get(hostId + contextPath) == null) {
            logger.warn("Cannot shutdown application \"" + contextPath
                    + "\" on virtual host \"" + hostId
                    + "\". It doesn't exist or already stopped.");
            return;
        }
        NusuthContext context
                = (NusuthContext) applications.remove(hostId + contextPath);
        context.shutdownContext(true);
    }

    /**
     * This method returns true if application with given contextPath running on
     * given virtual host, false otherwise.
     * @return true if application with given contextPath running on
     * given virtual host, false otherwise.
     */
    public boolean isApplicationRunning(String hostId, String contextPath) {
        return applications.containsKey(hostId + contextPath);
    }


/*
  private void handleWar(File whereDir, File warFile) throws IOException {
    ZipInputStream zipStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(warFile)));
    ZipEntry entry = zipStream.getNextEntry();
    byte[] buf = new byte[1024];
    while (entry != null) {
      String name = entry.getName();
      File file = new File(whereDir, name);
      if (entry.isDirectory()) {
        file.mkdirs();
      } else {
        if (!file.getParentFile().exists()) {
          file.getParentFile().mkdirs();
        }
        file.createNewFile();
        BufferedOutputStream bos = new BufferedOutputStream(
          new FileOutputStream(file));
        int len = -1;
        do {
          len = zipStream.read(buf, 0, 1024);
          if (len != -1) {
            bos.write(buf, 0, len);
          }
        } while (len > -1);
        bos.flush();
        bos.close();
      }
      zipStream.closeEntry();
      entry = zipStream.getNextEntry();
    }
  }
*/

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

    public void fullShutdown() {
        logger.info("Shutdown container...");
        tcpServer.stopServer();
        tcpServer = null;
        if (sslServer != null) {
            sslServer.stopServer();
            sslServer = null;
        }
        System.exit(0);
    }

    public LocalContainer getLocalContainer() {
        return localContainer;
    }

    /**
     * This method automatically load all web applications which placed in
     * doc-base directory and archived to war archive but not already bounded
     * to any host.
     * @param root Docbase directory.
     * @param vhostId Virtual host id to which bind application.
     * @param backup Indicates the level of session failover.
     */
    public void doAutoload(File root, String vhostId, String backup) {
        File[] autoCon = root.listFiles();
        for (int i = 0; i < autoCon.length; i++) {
            if (autoCon[i].isDirectory()) {
                String appPath = "/" + autoCon[i].getName();
                String appLocation = autoCon[i].getAbsolutePath();
                String realName = appLocation;
/*
        if (appPath.endsWith(".war")) {
          appPath = appPath.substring(0, appPath.length() - 4);
          String dirName = convertName(appLocation);
          File warLocFile = new File(workDir, dirName);
          if (loadedAppsLocation.contains(warLocFile.getAbsolutePath())) {
            continue;
          }
          try {
            handleWar(warLocFile, autoCon[i]);
          } catch(IOException ioex) {
            logger.error("Cannot load application "+appPath+".war", ioex);
          }
          appLocation = warLocFile.getAbsolutePath();
        }
*/
                if (!loadedAppsLocation.contains(appLocation)) {
                    logger.info("Automatic binding of application " + realName);
                    try {
                        NusuthContext context
                                = new NusuthContext(appLocation,
                                        vhostId + appPath, workDir,
                                        logsLocation.getAbsolutePath(), null,
                                        backup);
                        if (!isStandAlone && context.isDistributable()) {
                            DistributedSessionManager sm
                                    = new DistributedSessionManager(context);
                            localContainer.registerManager(convertName(vhostId + appPath), sm);
                            context.setSessionManager(sm);
                            listener.registerComponentDistributionListener(vhostId + appPath,
                                    sm);
                        } else {
                            context.setSessionManager(new DefaultSessionManager(context,
                                    true));
                        }
                        context.setContainerId(componentId);
                        applications.put(vhostId + appPath, context);
                        StrBuffer buff = new StrBuffer();
                        buff.append(vhostId + (appPath.equals("/") ? "" : appPath));
                        httpContexts.add(buff);
                        logger.debug("Bound " + vhostId + appPath);
                        //                appContext.bind(id, conContext);
                    } catch (Exception mex) {
                        //Logger.log("Cannot bind application", mex, 1);
                        logger.error("Cannot bind application", mex);
                    }
                }
            }
        }
    }
/*
  public void doAutoload(File root, String vhostId, String backup) {
    File[] autoCon = root.listFiles();
    for (int i = 0; i < autoCon.length; i++) {
      if (autoCon[i].isDirectory() || autoCon[i].getName().endsWith(".war")) {
        String appPath = "/" + autoCon[i].getName();
        String appLocation = autoCon[i].getAbsolutePath();
        String realName = appLocation;
        if (appPath.endsWith(".war")) {
          appPath = appPath.substring(0, appPath.length() - 4);
          String dirName = convertName(appLocation);
          File warLocFile = new File(workDir, dirName);
          if (loadedAppsLocation.contains(warLocFile.getAbsolutePath())) {
            continue;
          }
          try {
            handleWar(warLocFile, autoCon[i]);
          } catch(IOException ioex) {
            logger.error("Cannot load application "+appPath+".war", ioex);
          }
          appLocation = warLocFile.getAbsolutePath();
        }
        if (!loadedAppsLocation.contains(appLocation)) {
          logger.info("Automatic binding of application " + realName);
          try {
            NusuthContext context
                    = new NusuthContext(appLocation,
                                       vhostId+appPath, workDir,
                                       logsLocation.getAbsolutePath(), null,
                                       backup);
            if (!isStandAlone && context.isDistributable()) {
              DistributedSessionManager sm
                      = new DistributedSessionManager(context);
              localContainer.registerManager(convertName(vhostId+appPath), sm);
              context.setSessionManager(sm);
              listener.registerComponentDistributionListener(vhostId+appPath,
                                                             sm);
            } else {
              context.setSessionManager(new DefaultSessionManager(context,
                                                                  true));
            }
            context.setContainerId(componentId);
            applications.put(vhostId+appPath, context);
            StrBuffer buff = new StrBuffer();
            buff.append(vhostId + (appPath.equals("/") ? "" : appPath));
            httpContexts.add(buff);
            logger.debug("Bound "+vhostId+appPath);
            //                appContext.bind(id, conContext);
          } catch (Exception mex) {
            //Logger.log("Cannot bind application", mex, 1);
            logger.error("Cannot bind application", mex);
          }
        }
      }
    }
  }
*/
    public boolean isRestartNeeded(CompositeNusuthWebAppElement newSettings) throws DeploymentException {
        CompositeNusuthWebAppElement newTcpServer = (CompositeNusuthWebAppElement) newSettings.getCompositeChild("tcp-server").nextElement();
        Enumeration enum = newSettings.getCompositeChild("ssl-server");
        CompositeNusuthWebAppElement newSslServer = null;
        if (enum.hasMoreElements()) {
            newSslServer = (CompositeNusuthWebAppElement) enum.nextElement();
        }
        enum = newSettings.getSimpleChild("protocol-adapter");
        String newProtocolAdapter = enum.hasMoreElements() ? ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim() : defaultProtocolAdapter;
        return tcpServer.isRestartNeeded(newTcpServer) || (sslServer != null && newSslServer != null && sslServer.isRestartNeeded(newSslServer)) ||
                !protocolAdapter.equals(newProtocolAdapter);
    }

    /**
     * Applies new private container settings. It's not modify virtual hosts info.
     * This method used only in cluster mode
     * @param newSettings new settings (information about virtual hosts will
     * be ignored)
     */
    public void applySettings(CompositeNusuthWebAppElement newSettings)
            throws DeploymentException {
        logger.debug("Apply new settings");
        NusuthRequestHandler.clearCache();
        CompositeNusuthWebAppElement newLoggerNode
                = ManagementUtil.getCompositeElement(newSettings, "logger");
        CompositeNusuthWebAppElement loggerNode
                = ManagementUtil.getCompositeElement(settings, "logger");
        if (!ManagementUtil.getSimpleString(loggerNode, "config").
                equals(ManagementUtil.getSimpleString(newLoggerNode, "config"))
                || !ManagementUtil.getSimpleString(loggerNode, "level").
                equals(ManagementUtil.getSimpleString(newLoggerNode, "level"))
                || !ManagementUtil.getSimpleString(loggerNode, "location").
                equals(ManagementUtil.getSimpleString(newLoggerNode, "location"))) {
//      loadLogger(newLoggerNode);
            loadLogger(newLoggerNode, "nusuth.log");
            logger.info("New logging settings applied");
        }

        Enumeration enum = newSettings.getSimpleChild("name");
        String newName
                = enum.hasMoreElements()
                ? ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim()
                : null;

        String standAloneStr
                = ((SimpleNusuthWebAppElement) newSettings.
                getSimpleChild("standalone").nextElement()).getContent().trim();
        boolean newIsStandAlone = false;
        if (standAloneStr.equalsIgnoreCase("true")
                || standAloneStr.equalsIgnoreCase("on")
                || standAloneStr.equalsIgnoreCase("yes")) {
            newIsStandAlone = true;
        } else if (standAloneStr.equalsIgnoreCase("false")
                || standAloneStr.equalsIgnoreCase("off")
                || standAloneStr.equalsIgnoreCase("no")) {
            newIsStandAlone = false;
        } else {
            logger.error("Cannot recognize value " + standAloneStr
                    + " of standalone element; use true or false");
            throw new DeploymentException("Cannot recognize value "
                    + standAloneStr + " of standalone element; "
                    + "use true or false");
        }
        if (!newIsStandAlone && newName == null) {
            logger.error("You MUST specify name for non-standalone Nusuth engine");
            throw new DeploymentException("You MUST specify name for "
                    + "non-standalone Nusuth engine");
        } else {
            if ((name != null && newName != null && !name.equals(newName))
                    || (name == null && newName != null)) {
                ProtocolAdapter.setContainerID(newName);
                logger.info("Conateiner name set to " + newName);
            }
            name = newName;
            if (!((isStandAlone && newIsStandAlone)
                    || (!isStandAlone && !newIsStandAlone))) {
                isStandAlone = newIsStandAlone;
                ProtocolAdapter.setStandAlone(isStandAlone);
                logger.info("Standalone set to " + isStandAlone);
            }
        }

        enum = newSettings.getSimpleChild("admin-ip");
        NusuthRequestHandler.setAdminIP(
                convertAdminIp(enum.hasMoreElements()
                ? ((SimpleNusuthWebAppElement) enum.nextElement()).
                getContent()
                : null));

        CompositeNusuthWebAppElement jspNode
                = ManagementUtil.getCompositeElement(settings, "jsp");
        CompositeNusuthWebAppElement newJspNode
                = ManagementUtil.getCompositeElement(newSettings, "jsp");
        if (jspNode != null && newJspNode != null) {
            if (!ManagementUtil.getSimpleString(jspNode, "compiler").
                    equals(ManagementUtil.getSimpleString(newJspNode, "compiler"))) {
                String jspCompilerStr
                        = ManagementUtil.getSimpleString(newJspNode, "compiler");
                String compilerClassName = null;
                if (jspCompilerStr.equalsIgnoreCase("javac")) {
                    compilerClassName = "com.azoft.nusuth.jsp.JavacJspCompiler";
                } else if (jspCompilerStr.toLowerCase().indexOf("jikes") != -1) {
                    compilerClassName = "com.azoft.nusuth.jsp.JikesJspCompiler";
                    JikesJspCompiler.setExecutable(jspCompilerStr);
                } else {
                    logger.error("Cannot recognize value " + jspCompilerStr
                            + " of jsp:compiler element; use javac or jikes");
                    throw new DeploymentException("Cannot recognize value "
                            + jspCompilerStr + " of jsp:compiler "
                            + "element; use javac or jikes");
                }
                try {
                    JspLoader.setCompilerClass(Class.forName(compilerClassName));
                } catch (Throwable t) {
                    logger.error("Cannot set new compiler for jsp", t);
                    throw new DeploymentException("Cannot set new compiler "
                            + "for jsp, nested: " + t);
                }
            }
            if (ManagementUtil.getSimpleTime(jspNode, "refresh")
                    != ManagementUtil.getSimpleTime(newJspNode, "refresh")) {
                JspLoader.setJspRefresh(ManagementUtil.getSimpleTime(newJspNode,
                        "refresh"));
            }
        } else if (jspNode == null && newJspNode != null) {
            String jspCompilerStr
                    = ManagementUtil.getSimpleString(newJspNode, "compiler");
            String compilerClassName = null;
            if (jspCompilerStr.equalsIgnoreCase("javac")) {
                compilerClassName = "com.azoft.nusuth.jsp.JavacJspCompiler";
            } else if (jspCompilerStr.toLowerCase().indexOf("jikes") != -1) {
                compilerClassName = "com.azoft.nusuth.jsp.JikesJspCompiler";
                JikesJspCompiler.setExecutable(jspCompilerStr);
            } else {
                logger.error("Cannot recognize value " + jspCompilerStr
                        + " of jsp:compiler element; use javac or jikes");
                throw new DeploymentException("Cannot recognize value " + jspCompilerStr
                        + " of jsp:compiler element; "
                        + "use javac or jikes");
            }
            try {
                JspLoader.setCompilerClass(Class.forName(compilerClassName));
            } catch (Throwable t) {
                logger.error("Cannot set new compiler for jsp", t);
                throw new DeploymentException("Cannot set new compiler "
                        + "for jsp, nested: " + t);
            }
            JspLoader.setJspRefresh(ManagementUtil.getSimpleTime(newJspNode,
                    "refresh"));
        } else if (jspNode != null && newJspNode == null) {
            String compilerClassName = "com.azoft.nusuth.jsp.JavacJspCompiler";
            try {
                JspLoader.setCompilerClass(Class.forName(compilerClassName));
            } catch (Throwable t) {
                logger.error("Cannot set new compiler for jsp", t);
                throw new DeploymentException("Cannot set new compiler for"
                        + " jsp, nested: " + t);
            }
            JspLoader.setJspRefresh(1000);
        }

        enum = newSettings.getSimpleChild("work-dir");
        String newWorkDir = null;
        try {
            newWorkDir = enum.hasMoreElements()
                    ? ((SimpleNusuthWebAppElement) enum.nextElement()).
                    getContent().trim()
                    : (new File(jbirdHome, "work")).getCanonicalPath();
        } catch (IOException ioex) {
            logger.error("Cannot handle new working directory", ioex);
            throw new DeploymentException("Cannot handle new working directory, "
                    + "nested: " + ioex);
        }
        if (workDir != newWorkDir) {
            File wdFile = new File(newWorkDir);
            if ((wdFile.exists() && !wdFile.isDirectory())
                    || (!wdFile.exists() && !wdFile.mkdirs())) {
                logger.error("Cannot find or create working directory " + workDir);
                throw new DeploymentException("Cannot find or create working directory "
                        + workDir);
            }
            workDir = newWorkDir;
        }

        String oldWebConf = ManagementUtil.getSimpleString(settings,
                "default-web-config");
        String newWebConf = ManagementUtil.getSimpleString(newSettings,
                "default-web-config");
        if (oldWebConf == null) {
            if (newWebConf != null) {
                File commonRepositoryFile = new File(newWebConf);
                if (commonRepositoryFile.exists()) {
                    try {
                        NusuthContext.setCommonConfig(
                                NusuthAppConfigFactory.
                                createConfig("web-app",
                                        commonRepositoryFile.getCanonicalPath()));
                    } catch (IOException ioex) {
                        logger.error("Cannot set new common config " + newWebConf, ioex);
                    }
                }
            }
        } else {
            if (newWebConf == null || oldWebConf.equals(newWebConf)) {
                File commonRepositoryFile
                        = newWebConf != null
                        ? new File(newWebConf)
                        : new File(jbirdHome, "admin/web.xml");
                if (commonRepositoryFile.exists()) {
                    try {
                        NusuthContext.
                                setCommonConfig(
                                        NusuthAppConfigFactory.
                                createConfig("web-app",
                                        commonRepositoryFile.
                                getCanonicalPath()));
                    } catch (IOException ioex) {
                        logger.error("Cannot set new common config " + newWebConf, ioex);
                    }
                }
            }
        }

        tcpServer.applySettings(ManagementUtil.getCompositeElement(newSettings,
                "tcp-server"));

        CompositeNusuthWebAppElement sslServerNode
                = ManagementUtil.getCompositeElement(newSettings, "ssl-server");
        if (sslServer == null) {
            if (sslServerNode != null) {
                sslServer = new NusuthSslServer(sslServerNode,
                        NusuthRequestHandler.class);
                enum = settings.getCompositeChild("host");
                while (enum.hasMoreElements()) {
                    CompositeNusuthWebAppElement hostElement
                            = (CompositeNusuthWebAppElement) enum.nextElement();
                    String id = ManagementUtil.getSimpleString(hostElement, "id");
                    sslServer.addVirtualHost(id);
                }
                sslServer.startServer();
                NusuthRequestHandler.sslEnabled = true;
            }
        } else {
            if (sslServerNode == null) {
                sslServer.stopServer();
                sslServer = null;
            } else {
                sslServer.applySettings(sslServerNode);
            }
        }
        if (listener != null) {
            if (newSettings.getCompositeChild("manager").hasMoreElements()) {
                if (listener.isRestartNeeded(newSettings)) {
                    listener.stopListener();
                    try {
                        listener = new AdminPortListener(newSettings, this);
                    } catch (Exception ex) {
                        logger.error("Cannot restart admin listener", ex);
                        throw new DeploymentException("Cannot restart "
                                + "admin listener, nested: " + ex);
                    }
                    listener.start();
                } else {
                    listener.applySettings(newSettings);
                }
            } else {
                listener.stopListener();
                listener = null;
            }
        } else if (newSettings.getCompositeChild("manager").hasMoreElements()) {
            try {
                listener = new AdminPortListener(newSettings, this);
            } catch (Exception ex) {
                logger.error("Cannot restart admin listener", ex);
                throw new DeploymentException("Cannot restart "
                        + "admin listener, nested: " + ex);
            }
            listener.start();
        }

        enum = settings.getCompositeChild("host");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement hostElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String hostId = ManagementUtil.getSimpleString(hostElement, "id");
            if (!newSettings.containsCompositeChild("host", "id", hostId)) {
                // remove deleted host
                removeVirtualHost(hostId);
            }
        }
        enum = newSettings.getCompositeChild("host");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement hostElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String hostId = ManagementUtil.getSimpleString(hostElement, "id");
            if (!settings.containsCompositeChild("host", "id", hostId)) {
                //add new host
                try {
                    addVirtualHost(hostElement);
                    tcpServer.addVirtualHost(hostId);
                    tcpServer.startServerSockets();
                } catch (ManagementException e) {
                    logger.error("Cannot add virtual host \"" + hostId + "\", nested: ", e);
                }
            } else {
                // merge hosts with same id
                CompositeNusuthWebAppElement oldHostElement
                        = ManagementUtil.getCompositeElement(settings, "host",
                                "id", hostId);
                String newDocBase
                        = ManagementUtil.getSimpleString(hostElement, "doc-base");
                String oldDocBase
                        = ManagementUtil.getSimpleString(oldHostElement, "doc-base");
                if (!newDocBase.equalsIgnoreCase(oldDocBase)) {
                    removeVirtualHost(hostId);
                    try {
                        addVirtualHost(hostElement);
                    } catch (ManagementException e) {
                        logger.error("Cannot add virtual host \"" + hostId + "\", nested: ", e);
                    }
                    tcpServer.addVirtualHost(hostId);
                    tcpServer.startServerSockets();
                } else {
                    //merge hosts with same id and doc-base
                    CompositeNusuthWebAppElement newAcLog
                            = ManagementUtil.getCompositeElement(hostElement,
                                    "access-log");
                    CompositeNusuthWebAppElement oldAcLog
                            = ManagementUtil.getCompositeElement(oldHostElement,
                                    "access-log");
                    if ((newAcLog != null && oldAcLog == null) ||
                            (newAcLog == null && oldAcLog != null) ||
                            (newAcLog != null && oldAcLog != null
                            && !newAcLog.equals(oldAcLog))) {
                        StrBuffer strHostId = new StrBuffer(hostId.length());
                        strHostId.append(hostId);
                        allHosts.add(strHostId);
                        HttpProtocolAdapter.removeCategory(strHostId);
                        try {
                            processAccessLogForHost(hostElement, hostId);
                        } catch (ManagementException e) {
                            logger.error("Cannot process access log config for host \""
                                    + hostId + "\"");
                        }
                    }
                    mergeAllContexts(oldHostElement, hostElement);
                }
            }
        }
/*
<---------------------------Commented by skilz....----------------------------->

    // writes new hosts info

    // removes hosts info from new settings
    while ((enum = newSettings.getCompositeChild("host")).hasMoreElements()) {
      CompositeNusuthWebAppElement hostElement = (CompositeNusuthWebAppElement)enum.nextElement();
      newSettings.removeCompositeChild("host", hostElement);
    }
    // copy hosts info from old to new settings
    enum = settings.getCompositeChild("host");
    while (enum.hasMoreElements()) {
      CompositeNusuthWebAppElement hostElement = (CompositeNusuthWebAppElement)enum.nextElement();
      newSettings.addCompositeChild("host", hostElement);
    }
*/
        normalizeDocBase(newSettings);
        settings = (CompositeNusuthWebAppElement) newSettings.clone();
        File file = new File(this.configFileName);
        try {
            FileWriter wr = new FileWriter(file);
            wr.write(settings.toString());
            wr.close();
        } catch (IOException e) {
            logger.error("Cannot write to \"" + file.getAbsolutePath() + "\"", e);
        }
    }

    /**
     * This method is auxiliary.It use in applySettings() method to comparing and
     * applying all contexsts changes in concrete host element.
     * @param oldHost Old config for host
     * @param newHost New config for host
     * @throws DeploymentException Throws if any errors occurs while merging.
     */
    private void mergeAllContexts(CompositeNusuthWebAppElement oldHost,
                                  CompositeNusuthWebAppElement newHost)
            throws DeploymentException {
        String hostId = ManagementUtil.getSimpleString(oldHost, "id");
        Enumeration enum = oldHost.getCompositeChild("context");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement contextElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String path = ManagementUtil.getSimpleString(contextElement, "path");
            String location
                    = ManagementUtil.getSimpleString(contextElement, "location");
            if (!newHost.containsCompositeChild("context", "path", path)) {
                removeContext(hostId, path);
            }
        }
        enum = newHost.getCompositeChild("context");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement contextElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String path = ManagementUtil.getSimpleString(contextElement, "path");
            if (!oldHost.containsCompositeChild("context", "path", path)) {
                String docBase = ManagementUtil.getSimpleString(newHost, "doc-base");
                String backup = ManagementUtil.getSimpleString(contextElement,
                        "session-backup");
                if (backup == null) {
                    backup = ManagementUtil.getSimpleString(newHost, "session-backup");
                }
                try {
                    addApplication(contextElement, hostId, docBase, backup);
                } catch (ManagementException e) {
                    logger.error("Cannot bind application to host \"" + hostId + "\"", e);
                }
            } else {
                CompositeNusuthWebAppElement oldContext
                        = ManagementUtil.getCompositeElement(oldHost, "context",
                                "path", path);
                String docBase = ManagementUtil.getSimpleString(newHost, "doc-base");
                String backup = ManagementUtil.getSimpleString(newHost,
                        "session-backup");
                mergeContexts(oldContext, contextElement, hostId, docBase, backup);
            }
        }
    }

    /**
     * This method is auxiliary.It use in applySettings() method to comparing and
     * applying contexsts changes with same contextPath and doc-base.
     * @param oldContext Old config for context
     * @param newContext New config for context
     * @param hostId Host id.
     * @param docBase Doc-base for both contexts.
     * @param backup content of session-backup element from host.
     * @throws DeploymentException Throws if any errors occurs while merging.
     */
    private void mergeContexts(CompositeNusuthWebAppElement oldContext,
                               CompositeNusuthWebAppElement newContext,
                               String hostId, String docBase, String backup)
            throws DeploymentException {
        String contextPath = ManagementUtil.getSimpleString(oldContext, "path");
        String key = hostId + (contextPath.equals("/") ? "" : contextPath);
        StrBuffer keyBuf = new StrBuffer();
        keyBuf.append(key);
        String oldLocation
                = ManagementUtil.getSimpleString(oldContext,
                        "location").toLowerCase().trim();
        String newLocation
                = ManagementUtil.getSimpleString(newContext,
                        "location").toLowerCase().trim();
        if (!oldLocation.equals(newLocation)) {
            removeContext(hostId, contextPath);
            try {
                addApplication(newContext, hostId, docBase, backup);
            } catch (ManagementException e) {
                logger.error("Cannot add application to host \"" + hostId + "\"", e);
            }
        } else {
            // protocol comparing
            httpContexts.remove(keyBuf);
            httpsContexts.remove(keyBuf);
            Enumeration enum = newContext.getSimpleChild("protocol");
            boolean isHttpContext = true;
            boolean isHttpsContext = false;
            if (enum.hasMoreElements()) {
                String protocol = ((SimpleNusuthWebAppElement) enum.nextElement()).
                        getContent().trim().toLowerCase();
                StringTokenizer tokenizer = new StringTokenizer(protocol, ",");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken().trim();
                    if (token.equals("http")) {
                        isHttpContext = true;
                    } else if (token.equals("https")) {
                        if (sslServer != null) {
                            isHttpsContext = true;
                        } else {
                            logger.warn("SSL server not defined - cannot use https for "
                                    + "application " + contextPath + " in host " + hostId);
                        }
                    } else {
                        logger.warn("Unknown protocol \"" + token
                                + "\" defined for application "
                                + contextPath + " - skipped");
                    }
                }
            }
            if (isHttpContext) {
                httpContexts.add(keyBuf);
            }
            if (isHttpsContext) {
                httpsContexts.add(keyBuf);
            }
            //session-backup comparing
            String oldBackup
                    = ManagementUtil.getSimpleString(oldContext,
                            "session-backup");
            String newBackup
                    = ManagementUtil.getSimpleString(newContext,
                            "session-backup");
            if ((oldBackup != null && newBackup != null
                    && !oldBackup.equals(newBackup))) {
                ((NusuthContext) applications.get(hostId + contextPath)).setSessionBackup(newBackup);
            } else if (oldBackup != null && newBackup == null) {
                ((NusuthContext) applications.get(hostId + contextPath)).setSessionBackup(backup == null
                        ? "never"
                        : backup);
            } else if (oldBackup == null && newBackup != null) {
                ((NusuthContext) applications.get(hostId + contextPath)).setSessionBackup(newBackup);
            } else if (newBackup.trim().length() == 0
                    && backup.trim().length() != 0) {
                ((NusuthContext) applications.get(hostId + contextPath)).setSessionBackup(backup == null
                        ? "never"
                        : backup);
            }
            // jsp-precompile comparing
            boolean oldJspPrecompile
                    = ManagementUtil.getSimpleBoolean(oldContext, "jsp-precompile",
                            false);
            boolean newJspPrecompile
                    = ManagementUtil.getSimpleBoolean(newContext, "jsp-precompile",
                            false);
            if (newJspPrecompile && !oldJspPrecompile) {
                compileJsp((NusuthContext) applications.get(hostId + contextPath));
            }
            ((NusuthContext) applications.get(hostId + contextPath)).
                    applyExternalSettings(newContext);
        }
    }

    /**
     * This method remove virtual host with specified id.
     * @param hostId Id of host to remove.
     * @throws DeploymentException Throws if any errors occurs while removing.
     */
    private void removeVirtualHost(String hostId) throws DeploymentException {
        logger.info("Removing virtual host \"" + hostId + "\"");
        StrBuffer hostIdBuf = new StrBuffer();
        hostIdBuf.append(hostId);
        HttpProtocolAdapter.removeCategory(hostIdBuf);
        allHosts.remove(hostIdBuf);
        CompositeNusuthWebAppElement hostElement
                = ManagementUtil.getCompositeElement(settings, "host",
                        "id", hostId);
        Enumeration enum = hostElement.getCompositeChild("context");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement contextElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String appPath = ManagementUtil.getSimpleString(contextElement, "path");
            removeContext(hostId, appPath);
        }
        tcpServer.virtualHostRemoved(hostId);
        logger.info("Virtual host \"" + hostId + "\" removed");
    }

    private void removeContext(String hostId, String appPath)
            throws DeploymentException {
        logger.info("Removing application \"" + appPath
                + "\" from host \"" + hostId + "\"");
        String key = hostId + (appPath.equals("/") ? "" : appPath);
        StrBuffer keyBuf = new StrBuffer();
        keyBuf.append(key);
        httpContexts.remove(keyBuf);
        httpsContexts.remove(keyBuf);
        contextID2config.remove(key);
        contextID2context.remove(key);
        NusuthContext context =
                (NusuthContext) applications.remove(hostId + appPath);
        if (context != null) {
            context.shutdownContext(true);
        } else {
            logger.debug("Couldn't find app \"" + appPath + "\" on host \""
                    + hostId + "\" for delete");
        }
    }

/*
  private void removeHost(String id)
  {
    logger.info("Remove host \""+id+'"');
    allHosts.remove(id);
    Enumeration enum = applications.keys();
    while (enum.hasMoreElements()) {
      String key = (String)enum.nextElement();
      if (key.startsWith(id)) {
        NusuthContext context = (NusuthContext)applications.remove(key);
        context.shutdownContext(true);
      }
    }
  }

  private void removeApp(String hostName, String appName) {
    logger.info("Remove app \"" + appName + "\" on host \"" + hostName + '"');
    NusuthContext context =
        (NusuthContext)applications.remove(hostName + '/' + appName);
    if (context != null) {
      context.shutdownContext(true);
    } else {
      logger.debug("Couldn't find app \"" + appName + "\" on host \""
                   + hostName + "\" for delete");
    }
  }
*/

    public void applyDefaultWebSettings(
            CompositeNusuthWebAppElement newWebSettings)
            throws DeploymentException {
        if (commonConfig.equals(newWebSettings)) {
            System.out.println("Ok !");
            return;
        }
        Enumeration enum = settings.getSimpleChild("default-web-config");
        File commonRepositoryFile = enum.hasMoreElements()
                ? new File(((SimpleNusuthWebAppElement) enum.nextElement()).
                getContent())
                : new File(jbirdHome, "admin/web.xml");
        if (newWebSettings != null) {
            boolean created = true;
            if (!commonRepositoryFile.exists()) {
                try {
                    if (!commonRepositoryFile.createNewFile()) {
                        logger.error("Cannot create file "
                                + commonRepositoryFile.getAbsolutePath());
                        created = false;
                    }
                } catch (IOException e) {
                    logger.error("Cannot create file "
                            + commonRepositoryFile.getAbsolutePath());
                    created = false;
                }
            }
            if (created) {
                try {
                    FileWriter wr = new FileWriter(commonRepositoryFile);
                    wr.write(newWebSettings.toString());
                    wr.close();
                } catch (IOException e) {
                    logger.error("Cannot write to \""
                            + commonRepositoryFile.getAbsolutePath() + "\"", e);
                }
            }
        }
        commonConfig = (CompositeNusuthWebAppElement) newWebSettings.clone();
        NusuthContext.setCommonConfig(commonConfig);

        enum = settings.getCompositeChild("host");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement hostElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String id = ManagementUtil.getSimpleString(hostElement, "id");
            removeVirtualHost(id);
        }

        enum = settings.getCompositeChild("host");
        while (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement hostElement
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String id = ManagementUtil.getSimpleString(hostElement, "id");
            try {
                addVirtualHost(hostElement);
                tcpServer.addVirtualHost(id);
                tcpServer.startServerSockets();
            } catch (ManagementException e) {
                logger.error("Cannot add virtual host \"" + id + "\", nested: ", e);
            }
        }

    }

    public int getHttpPort() {
        return tcpServer.getPort();
    }

    public void setDistributorIdToContexts(String distributorId) {
        Iterator i = applications.values().iterator();
        while (i.hasNext()) {
            ((NusuthContext) i.next()).setDistributorId(distributorId);
        }
    }

    class WarFilenameFilter implements FilenameFilter {
        /** Simple constructor. */
        WarFilenameFilter() {
            super();
        }

        /**
         *The filtering method.
         * @param name the file name.
         * @return true if the given name != null and it have .war
         * extension, otherwise False.
         */
        public boolean accept(File dir, String name) {
            return name != null && (name.endsWith(".war"));
        }
    }

}
