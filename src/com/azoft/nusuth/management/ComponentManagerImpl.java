package com.azoft.nusuth.management;

import com.azoft.nusuth.management.rmi.RmiApplicationInputStream;
import com.azoft.nusuth.management.security.AdminPortListener;
import com.azoft.nusuth.util.LogCategoryProxy;

import java.io.*;
import java.rmi.Remote;
import java.security.*;
import java.security.Permission;
import java.security.acl.*;
import java.util.*;
import java.net.*;

import org.apache.log4j.Category;
import org.xml.sax.EntityResolver;
import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.gui.MD5;
import com.azoft.nusuth.jidep.*;
import com.azoft.nusuth.security.*;

import javax.naming.*;
import javax.naming.directory.*;

import com.azoft.nusuth.jndi.*;

/**
 * This is parent for all component managers.
 * Creation date: (16.01.01 22:06:08)
 * @author: Igork (igork@novosoft.ru)
 */
public abstract class ComponentManagerImpl
        implements ComponentManager {
    protected Category logger = null;
    protected LogCategoryProxy loggerProxy = null;
    protected Properties logConfigProps = new Properties();
    protected AdminPortListener listener = null;
    protected String configFileName = null;
    protected String jbirdHome = "..";
    protected com.azoft.nusuth.deployment.CompositeNusuthWebAppElement settings = null;
    protected File logsLocation;
    protected String componentId;
    protected DistributedJNDIContext clusterContext;

    protected abstract class AbstractClusterListener
            implements DistributedJNDIContextListener {
        private Set listenedPaths;

        public AbstractClusterListener(Set listenedPaths) {
            this.listenedPaths = listenedPaths;
            logger.debug("*** Cluster Listener: START " + listenedPaths);
        }

        protected void addSubscription(String name) {
            if (!listenedPaths.contains(name)) {
                clusterContext.subscribe(new String[]{name}, this);
                listenedPaths.add(name);
                System.out.println("*** Listener: add " + name);
                logger.debug("*** Cluster Listener: add " + name);
            }
        }

        protected void removeSubscription(String name) {
            if (listenedPaths.contains(name)) {
                // !!!
                //clusterContext.unsubscribe(new String[] {name});
                listenedPaths.remove(name);
                System.out.println("*** Listener: del " + name);
                logger.debug("*** Cluster Listener: del " + name);
            }
        }

        protected void processSubscription(JidepNotification notification) {
            logger.debug("*** Cluster Listener: process subscription");
            if (notification.notificationType == NotificationType.DELETED) {
                removeSubscription(notification.name);
            } else {
                addSubscription(notification.name);
            }
        }
    }

    public ComponentManagerImpl()
            throws ParserException {
        super();
        String jbhome = System.getProperty("nusuth.home");
        if (jbhome != null && jbhome.length() > 0)
            jbirdHome = jbhome;
        NusuthAppConfigFactory.addEntityResolver(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, new ApplicationDeploymentEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(ManagementUtil.APPLICATION_DEPLOYMENT_ERRORS_TYPE, new ApplicationDeploymentErrorsEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(ManagementUtil.DISTRIBUTED_JNDI_CONFIG_TYPE, new SecurityConfigEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(ManagementUtil.DISTRIBUTED_JNDI_HOST_CONFIG, new ClusterHostEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(ManagementUtil.DISTRIBUTED_JNDI_APP_JBIRD_CONFIG, new NusuthWebAppConfigEntityResolver());
        NusuthAppConfigFactory.addEntityResolver(ManagementUtil.WEB_APP_USERS_CONFIG, new WebAppUsersEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("web-app", new WebEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("container",
                new ContainerEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("taglib", new JspEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("distributor", new DistributorEntityResolver());
    }

    protected void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public abstract String getComponentType();

    public InputStream getSettings()
            throws ManagementException {
        return new ByteArrayInputStream(settings.compose(getComponentType(), getComponentType() + ".dtd").getBytes());
    }

    protected void loadLogger(CompositeNusuthWebAppElement loggerNode)
            throws DeploymentException {
        loadLogger(loggerNode, "nusuth_" + getComponentType() + ".log");
    }

    protected void loadLogger(CompositeNusuthWebAppElement loggerNode, String logName)
            throws DeploymentException {
        String settingsLoggerConfig = ManagementUtil.getSimpleString(loggerNode, "config");

        if ((settingsLoggerConfig == null) || settingsLoggerConfig.equals("")) {
            loadLoggerPropsByDefault();
        } else {
            loadLoggerPropsFromLocation(settingsLoggerConfig);
        }

        setLogLevel(ManagementUtil.getSimpleString(loggerNode, "level"));

        try {
            File logdir = null;
            String logLocation = ManagementUtil.getSimpleString(loggerNode, "location");
            logdir = ((logLocation == null) || logLocation.equals("")) ? new File(jbirdHome, "logs") : new File(logLocation);

            if (!logdir.exists()) {
                logdir.mkdirs();
            }

            if (logdir.exists() && logdir.isDirectory()) {
                File logFile = new File(logdir, logName);
                logsLocation = logdir;

                logConfigProps.setProperty("log4j.appender.A1.File", logFile.toString());
                System.out.println("{Nusuth} Log file created in " + logFile.getCanonicalPath());
            } else {
                System.out.println("{Nusuth} Directory " + logLocation + " doesn't exist or is not directory - log file will be created in directory nusuth.home /logs");
                logsLocation = new File(jbirdHome + File.separator + "logs");
            }
        } catch (IOException ioex) {
            System.out.println("{Nusuth} Can't create specifed log file - log file will be created in current directory, nested: " + ioex);
        }

        org.apache.log4j.PropertyConfigurator.configure(logConfigProps);

        logger = Category.getInstance(this.getClass());
        loggerProxy = loggerProxy.getInstance(this.getClass().getName());
    }


    protected void loadLoggerPropsByDefault() {
        try {
            logConfigProps.load(getClass().getClassLoader().getResourceAsStream("com/azoft/nusuth/util/nusuthlog.properties"));
        } catch (Exception e) {
            System.out.println("Cannot load default logger properties file");
            System.out.println("com/azoft/nusuth/util/nusuthlog.properties");
            System.out.println("logging functions will be unavailable, nested");
            e.printStackTrace();
        }
    }


    protected void loadLoggerPropsFromLocation(String location) {
        try {
            File dir = new File(location);
            File file = new File(dir, "nusuthlog.properties");
            FileInputStream in = new FileInputStream(file);

            logConfigProps.load(in);
        } catch (Exception cnfe) {
            System.out.println("The directory " + location + " does not exist or");
            System.out.println("it not contain file nusuthlog.properties");
            System.out.println("logger will be configured by default");
            loadLoggerPropsByDefault();
        }
    }


    protected void setLogLevel(String level) {
        boolean unknown = true;
        String s = logConfigProps.getProperty("log4j.category.com.azoft.nusuth");
        int i = s.indexOf(",");

        s = s.substring(i);

        if (level.equals("DEBUG") || level.equals("TRACE")) {
            logConfigProps.setProperty("log4j.category.com.azoft.nusuth", "DEBUG" + s);

            unknown = false;
        }

        if (level.equals("INFO") || level.equals("INFORMATIONAL")) {
            logConfigProps.setProperty("log4j.category.com.azoft.nusuth", "INFO" + s);

            unknown = false;
        }

        if (level.equals("WARN") || level.equals("WARNING")) {
            logConfigProps.setProperty("log4j.category.com.azoft.nusuth", "WARN" + s);

            unknown = false;
        }

        if (level.equals("ERROR") || level.equals("ERR")) {
            logConfigProps.setProperty("log4j.category.com.azoft.nusuth", "ERROR" + s);

            unknown = false;
        }

        if (unknown) {
            System.out.println("Unknown log level - setting to ERROR");
        }
    }


    protected final String convertCharactersInName(String name) {
        if (name == null || name.length() == 0)
            return name;
        StringBuffer buf = new StringBuffer(name);
        for (int i = 0; i < buf.length(); i++) {
            char c = buf.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')))
                buf.replace(i, i + 1, Integer.toHexString((int) c));
        }
        return buf.toString();
    }


    protected void saveSettings()
            throws ManagementException {
        OutputStream ostream = null;
        try {
            ostream = new BufferedOutputStream(new FileOutputStream(configFileName));
            ostream.write(settings.compose(getComponentType(), getComponentType() + ".dtd").getBytes());
            ostream.flush();
            ostream.close();
        } catch (FileNotFoundException fnfex) {
            logger.debug("Couldn't write config", fnfex);
            throw new ManagementException("Couldn't write config, nested:" + fnfex.getMessage());
        } catch (IOException ioex) {
            logger.debug("Couldn't write config", ioex);
            throw new ManagementException("Couldn't write config, nested:" + ioex.getMessage());
        }
        logger.debug("Settings saved");
    }


    public abstract void setSettings(InputStream settings) throws ManagementException;


    public DistributedJNDIContext getDistributedContext() {
        return clusterContext;
    }

    protected DistributedJNDIContext connectToClusterContext(CompositeNusuthWebAppElement settings)
            throws DeploymentException, ManagementException {
        String localIP;
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("Couldn't get localhost ip", e);
            throw new ManagementException("Couldn't get localhost ip, nested: "
                    + e.getMessage());
        }
        CompositeNusuthWebAppElement managerElement = ManagementUtil.getCompositeElement(settings, "manager");
        String userName = ManagementUtil.getSimpleString(managerElement, "user-name");
        String password = ManagementUtil.getSimpleString(managerElement, "password");
        int adminPort = ManagementUtil.getSimpleInt(managerElement, "port");
        JidepDistributedJNDIContext.setLocalParameters(userName,
                password,
                localIP,
                adminPort);
        for (Enumeration enum = managerElement.getCompositeChild("cluster-host"); enum.hasMoreElements();) {
            CompositeNusuthWebAppElement hostElement = (CompositeNusuthWebAppElement) enum.nextElement();
            int port = ManagementUtil.getSimpleInt(hostElement, "port");
            String host = ManagementUtil.getSimpleString(hostElement, "host-name");
            try {
                return new JidepDistributedJNDIContext(host, port);
            } catch (AuthorizationRequiredException e) {
                logger.error("Cannot connect to cluster context on \"" + host + ':' + port
                        + "\": Wrong username/password", e);
                throw new ManagementException("Cannot connect to cluster context on \""
                        + host + ':' + port
                        + "\": Wrong username/password, nested: "
                        + e.getMessage());
            } catch (SocketException e) {
                if (loggerProxy.isDebugEnabled()) {
                    logger.info("Cannot connect to cluster context on \"" + host + ':' + port + '"', e);
                } else {
                    logger.info("Cannot connect to cluster context on \"" + host + ':' + port + '"');
                }
            } catch (Exception e) {
                logger.info("Cannot connect to cluster context on \"" + host + ':' + port + '"', e);
            }
        }

        try {
            Attributes attrs = new BasicAttributes();
            attrs.put("Replicable", new Boolean(true));
            attrs.put(createEmptyAclAttribute());
            DistributedJNDIContext dc =
                    new JidepDistributedJNDIContext();
            dc.modifyAttributes("", DirContext.ADD_ATTRIBUTE, attrs);
            dc.modifyAttributes("internal", DirContext.ADD_ATTRIBUTE, attrs);
            dc.bind("usersroles",
                    NusuthAppConfigFactory.createConfig(
                            ManagementUtil.DISTRIBUTED_JNDI_CONFIG_TYPE,
                            new FileInputStream("../admin/" + ManagementUtil.DISTRIBUTED_JNDI_CONFIG_TYPE + ".xml")
                    )
            );
            DirContext componentsContext = dc.createSubcontext("components", attrs);
            componentsContext.createSubcontext("distributors", attrs);
            componentsContext.createSubcontext("containers", attrs);
            componentsContext.createSubcontext("hosts", attrs);
            return dc;
        } catch (FileNotFoundException e) {
            logger.error("Cannot create cluster context", e);
            throw new ManagementException("Cannot create cluster context, nested: " + e.getMessage());
        } catch (NamingException e) {
            logger.error("Cannot create cluster context", e);
            throw new ManagementException("Cannot create cluster context, nested: " + e.getMessage());
        }
    }

    protected void logContext(DirContext context) {
        if (loggerProxy.isDebugEnabled()) {
            logger.debug("******** Log Context");
            if (context != null) {
                try {
                    NamingEnumeration enum = context.list(new NusuthJNDIName());
                    while (enum.hasMore()) {
                        NameClassPair ncp = (NameClassPair) enum.next();
                        logContext_internal("*  ", new CompositeName(ncp.getName()), context);
                    }
                } catch (NamingException e) {
                    logger.debug("Couldn't do logContext", e);
                } catch (Throwable t) {
                    logger.debug("COULDN'T DO LOGCONTEXT", t);
                }
            } else {
                logger.debug("*** Cluster context is null!!!");
            }
        }
    }

    private void logContext_internal(String prefix, Name currName, DirContext c) {
        try {
            Attributes attrs = c.getAttributes(currName.toString());
            String aList = "";
            for (NamingEnumeration enum = attrs.getAll(); enum.hasMore();) {
                Attribute a = (Attribute) enum.next();
                aList += '<' + a.getID() + ':';
                for (NamingEnumeration eas = a.getAll(); eas.hasMore();) {
                    aList += eas.next().toString() + (eas.hasMore() ? ", " : "");
                }
                aList += '>';
                if (enum.hasMore())
                    aList += ", ";
            }
            logger.debug(prefix + currName.get(currName.size() - 1) + " [" + aList + ']');
            Object o = c.lookup(currName.toString());
            if (o instanceof Context) {
                NamingEnumeration enum = c.list(currName.toString());
                String newPrefix = prefix + "  ";
                while (enum.hasMore()) {
                    NameClassPair ncp = (NameClassPair) enum.next();
                    Name newName = new CompositeName();
                    newName.addAll(currName);
                    newName.add(ncp.getName());
                    logContext_internal(newPrefix, newName, c);
                }
            }
        } catch (NamingException e) {
            logger.debug(prefix + "Error in logContext", e);
        }
    }

    protected Attribute createEmptyAclAttribute() {
        Principal ownerPrincipal = new NusuthPrincipal("component");
        Acl acl = new NusuthAcl(ownerPrincipal);
        AclEntry aclEntry = new NusuthAclEntry();
        aclEntry.setPrincipal(ownerPrincipal);
        aclEntry.addPermission(new NusuthPermission("read"));
        aclEntry.addPermission(new NusuthPermission("write"));
        try {
            acl.addEntry(ownerPrincipal, aclEntry);
        } catch (NotOwnerException e) {
            logger.error("couldn't create empty acl", e);
        }

        return new BasicAttribute("ACL", acl);
    }
}
