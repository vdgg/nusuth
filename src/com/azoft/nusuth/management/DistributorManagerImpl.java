package com.azoft.nusuth.management;

import com.azoft.nusuth.deployment.ApplicationDeploymentEntityResolver;
import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.NusuthAppConfigFactory;
import com.azoft.nusuth.deployment.DistributorEntityResolver;
import com.azoft.nusuth.distributor.cache.Cache;
import com.azoft.nusuth.distributor.connectionfactory.ContainerConnectionFactory;
import com.azoft.nusuth.distributor.connectionfactory.ContainerAddress;
import com.azoft.nusuth.management.security.AdminPortListener;
import com.azoft.nusuth.util.StrBuffer;

import java.io.*;
import java.rmi.*;
import java.security.*;
import java.util.*;
import java.net.SocketException;

import com.azoft.nusuth.distributor.*;
import com.azoft.nusuth.jndi.*;
import com.azoft.nusuth.jidep.*;

import javax.naming.*;
import javax.naming.directory.*;

public class DistributorManagerImpl
        extends ComponentManagerImpl
        implements DistributorManager {
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
                processContainers(notification);
            } else if (notification.name.startsWith("components/distributors")) {
                processDistributors(notification);
            }
        }

        private void processHosts(JidepNotification notification) {
            logger.debug("*** Cluster Listener: process hosts");
            String name = notification.name.substring("components/hosts/".length());
            if (name.indexOf('/') > -1
                    && name.substring(name.indexOf('/') + 1).startsWith("webapps")) {
                processWebapp(notification, name);
            } else {
                try {
                    if (name.indexOf('/') == -1) {
                        processSubscription(notification);
                    } else if (name.endsWith("/config")) {
                        factory.checkHosts();
                    }
                } catch (DeploymentException e) {
                    logger.warn("Couldn't check new hosts", e);
                }
            }
        }

        private void processWebapp(JidepNotification notification, String hostName) {
            logger.debug("*** Cluster Listener: process webapp");
            if (hostName.endsWith("/webapps")) {
                processSubscription(notification);
            } else {
                String name = hostName.substring(hostName.indexOf('/')
                        + "/webapps/".length());
                if (name.indexOf('/') == -1) {
                    processSubscription(notification);
                } else if (name.endsWith("/config")) {
                    try {
                        factory.checkHosts();
                    } catch (DeploymentException e) {
                        logger.warn("Couldn't check webapps", e);
                    }
                }
            }
        }

        private void processContainers(JidepNotification notification) {
            logger.debug("*** Cluster Listener: process Containers");
            if (!notification.name.endsWith("/config")) {
                try {
                    factory.checkContainers();
                } catch (DeploymentException e) {
                    logger.warn("Couldn't check new containers", e);
                }
            }
        }

        private void processDistributors(JidepNotification notification) {
            logger.debug("*** Cluster Listener: process distributors");
            String path = "components/distributors/"
                    + JndiNameConverter.encode(componentId) + "/config";
            if (notification.name.equals(path)) {
                try {
                    applyNewSettings(
                            (CompositeNusuthWebAppElement) clusterContext.lookup(path));
                } catch (ManagementException e) {
                    logger.debug("Couldn't apply new settings", e);
                } catch (DeploymentException e) {
                    logger.debug("Couldn't apply new settings", e);
                } catch (NamingException e) {
                    logger.debug("Couldn't get new distributor settings", e);
                }
                try {
                    saveSettings();
                } catch (ManagementException e) {
                    logger.debug("Couldn't save new settings", e);
                }
            }
        }
    }

    private ServerManager serverManager;
    private ContainerConnectionFactory factory;

    public DistributorManagerImpl(String configFileName)
            throws ManagementException, DeploymentException,
            FileNotFoundException {
        super();

        // load settings
        this.configFileName = ManagementUtil.getConfigFile(configFileName, "distributor.xml").getAbsolutePath();
        settings = NusuthAppConfigFactory.createConfig(getComponentType(), new BufferedInputStream(new FileInputStream(this.configFileName)));

        // load logger
        loadLogger(ManagementUtil.getCompositeElement(settings, "logger"));
        logger.info("Starting distributor manager");

        // load connection factory
        try {
            CompositeNusuthWebAppElement cfNode = ManagementUtil.getCompositeElement(settings, "connection-factory");
            String connectionFactoryName = "com.azoft.nusuth.distributor.connectionfactory.RandomFactory";
            HashMap parameters = new HashMap();
            if (cfNode != null) {
                connectionFactoryName = ManagementUtil.getSimpleString(cfNode, "class-name");
                for (Enumeration result = cfNode.getCompositeChild("init-parameter"); result.hasMoreElements();) {
                    CompositeNusuthWebAppElement parameterNode = (CompositeNusuthWebAppElement) result.nextElement();
                    parameters.put(ManagementUtil.getSimpleString(parameterNode, "name"), ManagementUtil.getSimpleString(parameterNode, "value"));
                }
            }
            factory = (ContainerConnectionFactory) Class.forName(connectionFactoryName).newInstance();
            factory.setParameters(parameters);
            ContainerWorkListener listener = new ContainerWorkListener(factory);
            listener.start();
            DistributorRequestHandler.setConnectionFactory(factory);
            String protocolAdapter = ManagementUtil.getSimpleString(settings, "protocol-adapter");
            if (protocolAdapter.length() == 0)
                protocolAdapter = "com.azoft.nusuth.distributor.HttpDistributorRequestAdapter";
            DistributorRequestHandler.setAdapterClass(Class.forName(protocolAdapter));
        } catch (ClassNotFoundException cnfex) {
            logger.error("Couldn't create DistributorManager. Class \"" + ManagementUtil.getSimpleString(settings, "connection-factory") + "\" not found.", cnfex);
            throw new ManagementException("Couldn't create DistributorManager. Class \"" + ManagementUtil.getSimpleString(settings, "connection-factory") + "\" not found.");
        } catch (Exception ex) {
            logger.error("Couldn't create DistributorManager", ex);
            throw new ManagementException("Couldn't create DistributorManager. Nested:" + ex.getMessage());
        }

        factory.applySettings(settings);

        DistributorRequestAdapter.setCache(new Cache());
        DistributorRequestAdapter.getCache().applySettings(settings);

        // create server manager
        this.serverManager = new ServerManager(settings);

        // start admin port listener
        listener = new AdminPortListener(settings, this);
        listener.start();
        this.startServer();

        setComponentId(ManagementUtil.getSimpleString(this.settings, "name"));
        clusterContext = connectToClusterContext(settings);
        bindPrivateConfigToClusterContext();
        factory.setClusterContext(clusterContext);
        factory.checkContainers();
        factory.checkHosts();
        setFirstClusterListener();
        logger.info("Distributor manager started");
        logContext(clusterContext);
    }

    private void setFirstClusterListener()
            throws ManagementException {
        Set paths = new HashSet();
        paths.add("components/containers");
        System.out.println("*** Listener: add components/containers");
        paths.add("components/hosts");
        System.out.println("*** Listener: add components/hosts");
        paths.add("components/distributors");
        System.out.println("*** Listener: add components/distributors");
        try {
            for (NamingEnumeration i = clusterContext.list("components/hosts");
                 i.hasMore();) {
                NameClassPair pair = (NameClassPair) i.next();
                String s = "components/hosts/" + pair.getName();
                paths.add(s);
                System.out.println("*** Listener: add " + s);
                paths.add(s += "/webapps");
                System.out.println("*** Listener: add " + s);
                s += '/';
                for (NamingEnumeration j = clusterContext.list(s); j.hasMore();) {
                    NameClassPair appPair = (NameClassPair) j.next();
                    String appName = appPair.getName();
                    paths.add(s + appName);
                    System.out.println("*** Listener: add " + s + appName);
                }
            }
        } catch (NamingException e) {
            logger.error("Cuoldn't set listeners to cluster contex ", e);
            throw new ManagementException("Cuoldn't set listeners to cluster "
                    + "contex, nested: " + e.getMessage());
        }
        clusterContext.subscribe((String[]) paths.toArray(new String[0]),
                new ClusterContextListener(paths));
    }

    private void bindPrivateConfigToClusterContext()
            throws ManagementException {
        logContext(clusterContext);
        try {
            Attributes attrs = new BasicAttributes();
            attrs.put(createEmptyAclAttribute());
            attrs.put("Replicable", new Boolean(false));
            attrs.put("Node", "localhost:" + listener.getPort());
            String ctxName = "components/distributors/"
                    + JndiNameConverter.encode(componentId);
            try {
                if (clusterContext.lookup(ctxName) != null) {
                    clusterContext.unbind(ctxName);
                }
            } catch (NamingException e) {
            }
            DirContext ctx =
                    clusterContext
                    .createSubcontext(ctxName,
                            attrs);
            ctx.bind("config", settings);
        } catch (NamingException e) {
            throw new ManagementException("Couldn't bind private config, nested: "
                    + e.getMessage());
        }
    }

    /**
     * Applies new settings to distributor. It recreate TcpServer with new
     * settings, starts up new ConnectionFactory if needed,
     * and so on... Creation date: (08.01.01 19:54:50)
     * @param newSettings com.azoft.nusuth.management.DistributorSettings
     */
    private void applyNewSettings(CompositeNusuthWebAppElement newSettings)
            throws ManagementException, DeploymentException {
        logger.debug("Apply new settings");
        // connection factory
        CompositeNusuthWebAppElement newCfNode = ManagementUtil.getCompositeElement(newSettings, "connection-factory");
        CompositeNusuthWebAppElement cfNode = ManagementUtil.getCompositeElement(settings, "connection-factory");
        String newConnectionFactoryName = "com.azoft.nusuth.distributor.connectionfactory.RandomFactory";
        HashMap newParameters = new HashMap();
        if (newCfNode != null) {
            newConnectionFactoryName = ManagementUtil.getSimpleString(newCfNode, "class-name");
            for (Enumeration result = newCfNode.getCompositeChild("init-parameter"); result.hasMoreElements();) {
                CompositeNusuthWebAppElement parameterNode = (CompositeNusuthWebAppElement) result.nextElement();
                newParameters.put(ManagementUtil.getSimpleString(parameterNode, "name"), ManagementUtil.getSimpleString(parameterNode, "value"));
            }
        }
        if (!newConnectionFactoryName.equals(ManagementUtil.getSimpleString(cfNode, "class-name"))) {
            try {
                factory = (ContainerConnectionFactory) Class.forName(newConnectionFactoryName).newInstance();
                DistributorRequestHandler.setConnectionFactory(factory);
            } catch (ClassNotFoundException cnfex) {
                logger.error("Couldn't apply new settings to DistributorManager. Class \"" + newConnectionFactoryName + "\" not found.", cnfex);
                throw new ManagementException("Couldn't apply new settings to DistributorManager. Class \"" + newConnectionFactoryName + "\" not found.");
            } catch (Exception ex) {
                logger.error("Couldn't apply new settings to DistributorManager", ex);
                throw new ManagementException("Couldn't apply new settings to DistributorManager. Nested:" + ex.getMessage());
            }
        }
        factory.setParameters(newParameters);
        DistributorRequestAdapter.getCache().applySettings(newSettings);

        if (serverManager.isRestartNeeded(newSettings)) {
            serverManager.stopServer();
            serverManager = new ServerManager(newSettings);
        } else
            serverManager.applySettings(newSettings);

        // admin port listener (to work with ClusterManager)
        CompositeNusuthWebAppElement newManagerNode = ManagementUtil.getCompositeElement(newSettings, "manager");
        CompositeNusuthWebAppElement newKeystoreNode = ManagementUtil.getCompositeElement(newManagerNode, "keystore");

        if (listener.isRestartNeeded(newSettings)) {
            listener.stopListener();
            listener = new AdminPortListener(newSettings, this);
            listener.start();
        } else
            listener.applySettings(newSettings);

        // logger
        CompositeNusuthWebAppElement newLoggerNode = ManagementUtil.getCompositeElement(newSettings, "logger");
        CompositeNusuthWebAppElement loggerNode = ManagementUtil.getCompositeElement(settings, "logger");
        if (!ManagementUtil.getSimpleString(loggerNode, "config").equals(ManagementUtil.getSimpleString(newLoggerNode, "config"))
                || !ManagementUtil.getSimpleString(loggerNode, "level").equals(ManagementUtil.getSimpleString(newLoggerNode, "level"))
                || !ManagementUtil.getSimpleString(loggerNode, "location").equals(ManagementUtil.getSimpleString(newLoggerNode, "location"))) {
            loadLogger(newLoggerNode);
        } // store new settings
        settings = newSettings;
        logger.debug("New settings applied");
    }


    public InputStream getApplicationsDeployment()
            throws ManagementException {
        if (loggerProxy.isDebugEnabled())
            logger.debug("Get applications deployment");
        String source = factory.getApplicationsDeployment().compose(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, ManagementUtil.APPLICATION_DEPLOYMENT_TYPE + ".dtd");
        return new ByteArrayInputStream(source.getBytes());
    }


    public final String getComponentType() {
        return "distributor";
    }


    public DistributorState getState() {
        if (loggerProxy.isDebugEnabled())
            logger.debug("Get state");
        DistributorState state = new DistributorState();
        ServerState serverState = serverManager.getState();
        state.setActiveHandlers(serverState.getActiveHandlers());
        state.setActiveKeepAlives(serverState.getActiveKeepAlives());
        state.setTotalHandlers(serverState.getTotalHandlers());
        Vector ccis = DistributorRequestHandler.getContainerRequestCount();
        state.setRequestCount((ContainerCountInfo[]) ccis.toArray(new ContainerCountInfo[0]));
        return state;
    }


    protected final void saveSettings()
            throws ManagementException {
        if (loggerProxy.isDebugEnabled())
            logger.debug("Save settings");
        try {
            while (settings.getCompositeChild("host").hasMoreElements())
                settings.removeCompositeChild("host", (CompositeNusuthWebAppElement) settings.getCompositeChild("host").nextElement());

            for (Enumeration hosts = factory.getApplicationsDeployment().getCompositeChild("host"); hosts.hasMoreElements();) {
                CompositeNusuthWebAppElement host = (CompositeNusuthWebAppElement) hosts.nextElement();
                CompositeNusuthWebAppElement newHost = settings.addCompositeChild("host");
                newHost.setSimpleChild("id").setContent(ManagementUtil.getSimpleString(host, "id"));
                for (Enumeration apps = host.getCompositeChild("context"); apps.hasMoreElements();) {
                    CompositeNusuthWebAppElement app = (CompositeNusuthWebAppElement) apps.nextElement();
                    CompositeNusuthWebAppElement newApp = newHost.addCompositeChild("context");
                    newApp.setSimpleChild("url").setContent(ManagementUtil.getSimpleString(app, "url"));
                    newApp.setSimpleChild("protocol").setContent(ManagementUtil.getSimpleString(app, "protocol"));
                    for (Enumeration conts = app.getSimpleChild("container"); conts.hasMoreElements();) {
                        String context = ((SimpleNusuthWebAppElement) conts.nextElement()).getContent();
                        newApp.addSimpleChild("container").setContent(context);
                    }
                }
            }

            while (settings.getCompositeChild("container-info").hasMoreElements())
                settings.removeCompositeChild("container-info", (CompositeNusuthWebAppElement) settings.getCompositeChild("container-info").nextElement());

            Map containers = factory.getContainers();
            for (Iterator i = containers.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                ContainerAddress addr = (ContainerAddress) containers.get(name);
                CompositeNusuthWebAppElement newContainer = settings.addCompositeChild("container-info");
                newContainer.setSimpleChild("name").setContent(name.toString());
                newContainer.setSimpleChild("container-host").setContent(addr.host);
                newContainer.setSimpleChild("port").setContent(String.valueOf(addr.port));
                newContainer.setSimpleChild("admin-port").setContent(String.valueOf(addr.adminPort));
            }

        } catch (DeploymentException dex) {
            logger.error("Couldn't save distributor config", dex);
            throw new ManagementException("Couldn't save distributor config, nested:" + dex.getMessage());
        }
        super.saveSettings();
    }


    public void setApplicationsDeployment(InputStream apps)
            throws ManagementException {
        if (loggerProxy.isDebugEnabled())
            logger.debug("Set applications deployment");
        try {
            CompositeNusuthWebAppElement element = NusuthAppConfigFactory.createConfig("application-deployment", apps);
            HashMap newHosts = new HashMap();
            try {
                for (Enumeration hostsEnum = element.getCompositeChild("host"); hostsEnum.hasMoreElements();) {
                    VirtualHostInfo vhost = new VirtualHostInfo((CompositeNusuthWebAppElement) hostsEnum.nextElement());
                    newHosts.put(vhost.getName(), vhost);
                }
            } catch (NullPointerException nex) {
                // no hosts given
            }
            factory.setHosts(newHosts);
        } catch (DeploymentException dex) {
            logger.error("Couldn't set applications deployment", dex);
            throw new ManagementException(dex.getMessage());
        }
        saveSettings();
    }


    public void setSettings(InputStream newSettings)
            throws ManagementException {
        if (loggerProxy.isDebugEnabled())
            logger.debug("Set settings");
        try {
            CompositeNusuthWebAppElement newConfig = NusuthAppConfigFactory.createConfig(getComponentType(), newSettings);
            applyNewSettings(newConfig);
            saveSettings();
        } catch (DeploymentException dex) {
            logger.error("Couldn't set new settings", dex);
            throw new ManagementException("Couldn't set new settings, nested: \"" + dex.getMessage() + '"');
        }
    }


    public void startServer()
            throws ManagementException {
        try {
            serverManager.startServer();
        } catch (DeploymentException dex) {
            throw new ManagementException(dex.getMessage());
        }
    }


    public void stopServer() {
        serverManager.stopServer();
    }

    public void setContainers(Map newContainers)
            throws ManagementException {
        try {
            factory.setContainers(newContainers);
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't set containers, nested: " + dex.getMessage());
        }
        saveSettings();
    }
}
