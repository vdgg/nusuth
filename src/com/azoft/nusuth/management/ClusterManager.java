package com.azoft.nusuth.management;

import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;
import com.azoft.nusuth.deployment.ParserException;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.ApplicationDeploymentEntityResolver;
import com.azoft.nusuth.deployment.ApplicationDeploymentErrorsEntityResolver;
import com.azoft.nusuth.deployment.ClusterEntityResolver;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.NusuthAppConfigFactory;
import com.azoft.nusuth.management.rmi.*;
import com.azoft.nusuth.management.security.NusuthPermission;
import com.azoft.nusuth.management.security.SecurityManager;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.distributor.connectionfactory.ContainerAddress;
import com.azoft.nusuth.util.StrBuffer;
import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.io.*;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.net.InetAddress;

/**
 * Cluster manager class.
 * @author vdgg, igork, skilz
 * @since Nusuth1.0
 * @version 1.24
 */
public class ClusterManager
        extends ComponentManagerImpl {
    private class ComponentStatesCacheElement {
        final static long EXPIRATION_TIME = 1000;
        ServerState state;
        long timestamp;

        ComponentStatesCacheElement(ServerState state) {
            this.state = state;
            touch();
        }

        void touch() {
            long timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            long currentTime = System.currentTimeMillis();
            return currentTime - timestamp > EXPIRATION_TIME;
        }
    }

    private class ComponentStatesCache {
        private Map cache = new HashMap();

        private String createName(String cType, String cId) {
            return cType + '/' + cId;
        }

        ServerState get(String cType, String cId) {
            String name = createName(cType, cId);
            ComponentStatesCacheElement elem = (ComponentStatesCacheElement) cache.get(name);
            if (elem != null) {
                if (!elem.isExpired()) {
                    //elem.touch();
                    return elem.state;
                } else {
                    cache.remove(name);
                    return null;
                }
            } else
                return null;
        }

        void put(String cType, String cId, ServerState cState) {
            cache.put(createName(cType, cId), new ComponentStatesCacheElement(cState));
        }
    }

    private final static String SEE_ACTION = "see";
    private final static String SEE_CONFIG_ACTION = "see config";
    private final static String SET_CONFIG_ACTION = "set config";
    private final static String SEE_SECURITY_ACTION = "see security";
    private final static String SET_SECURITY_ACTION = "set security";
    private final static String MONITOR_ACTION = "monitor";
    private final static String ADD_ACTION = "add";
    private final static String REMOVE_ACTION = "remove";

    /**
     * @associates <{VirtualHostInfo}>
     * @link aggregation
     * @supplierCardinality 0..*
     */
    private HashMap hosts = new HashMap();
    private SecurityManager securityManager = null;
    private List callbacks = new Vector();

    /**
     * @associates <{ContainerManager}>
     * @link aggregation
     * @supplierCardinality 0..*
     */
    private Hashtable containerManagers = new Hashtable();

    /**
     * @associates <{Deployer}>
     * @link aggregation
     * @supplierCardinality 0..*
     */
    private Hashtable deployers = new Hashtable();
    private Hashtable deployerInfos = new Hashtable();

    /**
     * @associates <{DistributorManager}>
     * @link aggregation
     * @supplierCardinality 0..*
     */
    private Hashtable distributorManagers = new Hashtable();

    private HashSet registeredComponents = new HashSet();
    private HashMap invokedComponents = new HashMap();

    private boolean applicationsNotChecked = true;

    private File webInfStorage;

    private CompositeNusuthWebAppElement errorsElement;

    private ComponentStatesCache stateCache = new ComponentStatesCache();

    /* used for locking methods called by components */
    private Object componentLock = new Object();

    /* used for locking methods called by GUI */
    private Object userLock = new Object();

    private final static boolean isUseCallbacks = false;

    private Map containerAddresses = new HashMap();


    /**
     * Construcor for cluster manager.
     * @param clusterConfig
     */
    public ClusterManager(String clusterConfig)
            throws ManagementException, FileNotFoundException, DeploymentException {
        NusuthAppConfigFactory.addEntityResolver(getComponentType(),
                new ClusterEntityResolver());

        errorsElement
                = NusuthAppConfigFactory.
                createConfig(ManagementUtil.APPLICATION_DEPLOYMENT_ERRORS_TYPE,
                        new ByteArrayInputStream(
                                ManagementUtil.EMPTY_APPLICATION_ERROR_XML.
                getBytes()));

        configFileName = ManagementUtil.getConfigFile(clusterConfig,
                "cluster.xml").
                getAbsolutePath();
        settings = NusuthAppConfigFactory.
                createConfig(getComponentType(),
                        new BufferedInputStream(
                                new FileInputStream(configFileName)));
        webInfStorage = getWebInfStorage();

        loadLogger(ManagementUtil.getCompositeElement(settings, "logger"));
        logger.info("Starting cluster manager");

        CompositeNusuthWebAppElement managerNode
                = ManagementUtil.getCompositeElement(settings, "manager");
        int clusterRmiPort = ManagementUtil.getSimpleInt(managerNode, "port");

        securityManager =
                new SecurityManager(settings);

        Registry registry = null;
        RmiClusterController controller = null;
        try {
            controller = new RmiClusterControllerImpl(this);
        } catch (Exception ex) {
            throw new ManagementException("Coudn't create RmiClusterController, "
                    + "nested:" + ex.getMessage());
        }
        try {
            registry = LocateRegistry.getRegistry("localhost", clusterRmiPort);
            registry.rebind("Cluster Manager Controller", controller);
        } catch (RemoteException rex) {
            try {
                registry = LocateRegistry.createRegistry(clusterRmiPort);
                registry.rebind("Cluster Manager Controller", controller);
            } catch (RemoteException another_rex) {
                throw new ManagementException("Coudn't locate or create RMI registry, "
                        + "nested:" + another_rex.getMessage());
            }
        }
        try {
            registry.rebind("Cluster Manager", new RmiClusterManagerImpl(this));
        } catch (RemoteException rex) {
            throw new ManagementException("Coudn't locate or create RMI registry, "
                    + "nested:" + rex.getMessage());
        }

        for (Enumeration i = settings.getCompositeChild("host");
             i.hasMoreElements();) {
            VirtualHostInfo newHost
                    = new VirtualHostInfo((CompositeNusuthWebAppElement) i.
                    nextElement());
            hosts.put(newHost.getName(), newHost);
            logger.info("\"" + newHost.getName() + "\" host added");
        }

        synchronized (componentLock) {
            for (Enumeration components = settings.getCompositeChild("component");
                 components.hasMoreElements();) {
                CompositeNusuthWebAppElement component
                        = (CompositeNusuthWebAppElement) components.nextElement();

                int port = ManagementUtil.getSimpleInt(component, "port");
                String host = ManagementUtil.getSimpleString(component, "host-name");

                try {
                    System.out.println(
                            "Invoke component on " + host + ':' + port);
                    invokeComponent(host, port);
                } catch (IOException ioex) {
                    logger.warn("Invoke component on " + host + ':' + port
                            + " failed", ioex);
                    System.out.println("Invoke component on " + host + ':'
                            + port + " failed");
                }
            }
        }
        logger.info("Cluster manager started");
    }


    public void addComponent(String sessionId, String host, int port)
            throws ManagementException, UnauthorizedAccessException, AccessDeniedException {
        synchronized (userLock) {
            logger.debug("Add component on host \"" + host + ':' + port);
            if (securityManager.checkPermission(sessionId, new NusuthPermission("*", "*", ADD_ACTION))) {
                try {
                    ComponentInfo cinfo = invokeComponent(host, port);
                    synchronized (cinfo) {
                        cinfo.wait(30000);
                        String componentIdInInvokedComponentsPool = getComponentIdInInvokedComponentsPool(cinfo.getHost(), cinfo.getAdminPort());
                        if (invokedComponents.containsKey(componentIdInInvokedComponentsPool) || !registeredComponents.contains(cinfo)) {
                            throw new ManagementException("Couldn't invoke component on " + host + ':' + port + "\", nested: Invoke timeout");
                        }
                    }
                } catch (InterruptedException iex) {
                } catch (IOException ioex) {
                    logger.warn("Couldn't invoke component on " + host + ':' + port, ioex);
                    throw new ManagementException("Couldn't invoke component on " + host + ':' + port + "\", nested:" + ioex.getMessage());
                }
            } else {
                throw new AccessDeniedException("<unknown>", host + ':' + port, ADD_ACTION);
            }
        }
    }


    private void checkApplicationsConfiguration()
            throws ManagementException {
        if (isUseCallbacks) {
            applicationsNotChecked = true;
            logger.debug("Check applications configuration");

            HashMap errorsAdded = new HashMap();
            HashMap errorsRemoved = new HashMap();

            HashMap eadd = new HashMap();
            HashMap erem = new HashMap();

            HashMap distributorsHosts = collectApplicationsFromDistributors();
            for (Iterator i = distributorsHosts.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next(); //distributor name
                HashMap dhosts = (HashMap) distributorsHosts.get(name);
                HashMap added = new HashMap();
                HashMap removed = new HashMap();
                getHostsDiff(hosts, dhosts, added, removed);
                if (!added.isEmpty())
                    eadd.put(name, added);
                if (!removed.isEmpty())
                    erem.put(name, removed);
            }

            if (!eadd.isEmpty())
                errorsAdded.put(ComponentType.SDISTRIBUTOR, eadd);
            if (!erem.isEmpty())
                errorsRemoved.put(ComponentType.SDISTRIBUTOR, erem);

            eadd = new HashMap();
            erem = new HashMap();

            HashMap containersHosts = collectApplicationsFromContainers();
            for (Iterator i = containersHosts.keySet().iterator(); i.hasNext();) {
                String name = (String) i.next();
                HashMap chosts = (HashMap) containersHosts.get(name);
                HashMap added = new HashMap();
                HashMap removed = new HashMap();
                getHostsDiff(hosts, chosts, added, removed);
                if (!added.isEmpty())
                    eadd.put(name, added);
                if (!removed.isEmpty())
                    erem.put(name, removed);
            }

            if (!eadd.isEmpty())
                errorsAdded.put(ComponentType.SCONTAINER, eadd);
            if (!erem.isEmpty())
                errorsRemoved.put(ComponentType.SCONTAINER, erem);

            errorsElement = putErrorsToCompositeElement(errorsAdded, errorsRemoved);
            if (!errorsAdded.isEmpty() || !errorsRemoved.isEmpty())
                refreshDeploymentErrors();

            applicationsNotChecked = false;
        }
    }


    private HashMap collectApplicationsFromContainers()
            throws ManagementException {
        logger.debug("Collect application infos from containers");
        HashMap result = new HashMap();
        for (Iterator i = containerManagers.keySet().iterator(); i.hasNext();) {
            String cmanagerName = (String) i.next();
            ContainerManager cmanager = (ContainerManager) containerManagers.get(cmanagerName);
            try {
                CompositeNusuthWebAppElement hosts = NusuthAppConfigFactory.createConfig("application-deployment", cmanager.getVirtualHosts());
                HashMap resultHosts = new HashMap();
                for (Enumeration j = hosts.getCompositeChild("host"); j.hasMoreElements();) {
                    VirtualHostInfo vhost = new VirtualHostInfo((CompositeNusuthWebAppElement) j.nextElement());
                    resultHosts.put(vhost.getName(), vhost);
                }
                result.put(cmanagerName, resultHosts);
            } catch (ParserException pex) {
                logger.error("Couldn't get applications info from container \"" + cmanagerName + "\"", pex);
                throw new ManagementException("Couldn't get applications info from container \"" + cmanagerName + "\", nested:" + pex.getMessage());
            } catch (DeploymentException dex) {
                logger.error("Couldn't get applications info from container \"" + cmanagerName + "\"", dex);
                throw new ManagementException("Couldn't get applications info from container \"" + cmanagerName + "\", nested:" + dex.getMessage());
            }
        }
        return result;
    }


    private HashMap collectApplicationsFromDistributors()
            throws ManagementException {
        logger.debug("Collect application infos from distributors");
        HashMap result = new HashMap();
        for (Iterator i = distributorManagers.keySet().iterator(); i.hasNext();) {
            String dmanagerName = (String) i.next();
            DistributorManager dmanager = (DistributorManager) distributorManagers.get(dmanagerName);
            try {
                CompositeNusuthWebAppElement hosts = NusuthAppConfigFactory.createConfig(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, dmanager.getApplicationsDeployment());
                HashMap resultHosts = new HashMap();
                for (Enumeration j = hosts.getCompositeChild("host"); j.hasMoreElements();) {
                    VirtualHostInfo vhost = new VirtualHostInfo((CompositeNusuthWebAppElement) j.nextElement());
                    resultHosts.put(vhost.getName(), vhost);
                }
                result.put(dmanagerName, resultHosts);
            } catch (ParserException pex) {
                logger.error("Couldn't get applications info from distributor \"" + dmanagerName + "\"", pex);
                throw new ManagementException("Couldn't get applications info from distributor \"" + dmanagerName + "\", nested:" + pex.getMessage());
            } catch (DeploymentException dex) {
                logger.error("Couldn't get applications info from distributor \"" + dmanagerName + "\"", dex);
                throw new ManagementException("Couldn't get applications info from distributor \"" + dmanagerName + "\", nested:" + dex.getMessage());
            }
        }
        return result;
    }


    private void deployHostsToContainers(HashMap added)
            throws ManagementException {
        Vector errors = new Vector();
        errors.addAll(added.values());
        for (Iterator i = deployers.values().iterator(); !errors.isEmpty() && i.hasNext();) {
            Deployer deployer = (Deployer) i.next();
            try {
                deployer.addApplication(errors);
                errors.clear();
            } catch (DeploingException dex) {
                errors.clear();
                if (dex.failedContainers != null)
                    errors.addAll(dex.failedContainers);
                if (dex.notFindedContainers != null)
                    errors.addAll(dex.notFindedContainers);
            }
        }
        if (!errors.isEmpty()) {
            logger.error("Couldn't deploy hosts: " + errors);
            throw new ManagementException("Couldn't deploy: " + errors);
        }
    }


    public InputStream getApplicationsDeployment(String sessionId)
            throws UnauthorizedAccessException, ManagementException {
        synchronized (userLock) {
            logger.debug("Get applications deployment");
            if (applicationsNotChecked)
                checkApplicationsConfiguration();

            try {
                CompositeNusuthWebAppElement result = NusuthAppConfigFactory.createConfig(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, new ByteArrayInputStream(ManagementUtil.EMPTY_APPLICATION_DEPLOYMENT_XML.getBytes()));
                HashMap restrictedHosts = restrictHosts(sessionId, hosts, SEE_ACTION);
                for (Iterator i = restrictedHosts.values().iterator(); i.hasNext();)
                    ((VirtualHostInfo) i.next()).addCompositeChild(result);

                return new ByteArrayInputStream(result.compose(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, ManagementUtil.APPLICATION_DEPLOYMENT_TYPE + ".dtd").getBytes());

            } catch (DeploymentException dex) {
                logger.error("Couldn't collect errors to CompositeElement", dex);
                throw new ManagementException("Couldn't collect errors to CompositeElement, nested:" + dex.getMessage());
            }
        }
    }


    public InputStream getComponentSettings(String sessionId, ComponentType componentType, String componentId)
            throws ManagementException, UnauthorizedAccessException, AccessDeniedException {
        synchronized (userLock) {
            logger.debug("Get " + componentType.toString() + " \"" + componentId + "\" settings");
            if (securityManager.checkPermission(sessionId, new NusuthPermission(componentType.toString(), componentId, SEE_CONFIG_ACTION))) {
                switch (componentType.getType()) {
                    case ComponentType.CONTAINER:
                        {
                            ContainerManager cmanager = (ContainerManager) containerManagers.get(componentId);
                            if (cmanager != null) {
                                InputStream containerSettingsStream = cmanager.getSettings();
                                return containerSettingsStream;
                            } else {
                                logger.error("Get " + componentType.toString() + " \"" + componentId + "\" settings: " + componentType.toString() + " \"" + componentId + "\" not registered");
                                throw new ManagementException("Container \"" + componentId + "\" not registered");
                            }
                        }
                    case ComponentType.DISTRIBUTOR:
                        {
                            DistributorManager dmanager = (DistributorManager) distributorManagers.get(componentId);
                            if (dmanager != null) {
                                return dmanager.getSettings();
                            } else {
                                logger.error("Get " + componentType.toString() + " \"" + componentId + "\" settings: " + componentType.toString() + " \"" + componentId + "\" not registered");
                                throw new ManagementException("Distributor \"" + componentId + "\" not registered");
                            }
                        }
                    case ComponentType.DEPLOYER:
                        {
                            Deployer deployer = (Deployer) deployers.get(componentId);
                            if (deployer != null) {
                                return deployer.getSettings();
                            } else {
                                logger.error("Get " + componentType.toString() + " \"" + componentId + "\" settings: " + componentType.toString() + " \"" + componentId + "\" not registered");
                                throw new ManagementException("Deployer \"" + componentId + "\" not registered");
                            }
                        }
                    case ComponentType.APPLICATION:
                        {
                            String hostName = componentId.substring(0, componentId.indexOf('/'));
                            VirtualHostInfo vhost = (VirtualHostInfo) hosts.get(hostName);
                            if (vhost == null) {
                                logger.error("Get " + componentType.toString() + " \"" + componentId + "\" settings: " +
                                        "Couldn't get web application info, nested: unknown virtual host \"" + hostName + '"');
                                throw new ManagementException("Couldn't get web application info, nested: unknown virtual host \"" + hostName + '"');
                            }
                            String appName = componentId.substring(componentId.indexOf('/'));
                            ApplicationInfoImpl app = vhost.getApplication(appName);
                            if (app == null) {
                                logger.error("Get " + componentType.toString() + " \"" + componentId + "\" settings: " +
                                        "Couldn't get web application info, nested: unknown application \"" + appName + "\" on host \"" + hostName + "\"");
                                throw new ManagementException("Couldn't get web application info, nested: unknown application \"" + appName + "\" on host \"" + hostName + "\"");
                            }
                            String appLocation = app.getLocation();
                            ZipInputStream result = new ZipInputStream(getWebInf(hostName, vhost.getDocBase(), appName, appLocation));
                            try {
                                result.getNextEntry();
                            } catch (IOException ioex) {
                                logger.error("Get " + componentType.toString() + " \"" + componentId + "\" settings: " +
                                        "Couldn't get web application info, nested:",
                                        ioex);
                                throw new ManagementException("Couldn't get web application info, nested:" + ioex);
                            }
                            return result;
                        }
                    default :
                        logger.error("Unknown component type for component \"" + componentId + "\"");
                        throw new ManagementException("Unknown component type for component \"" + componentId + "\"");
                }
            } else {
                throw new AccessDeniedException(componentType.toString(), componentId, SEE_CONFIG_ACTION);
            }
        }
    }


    public String getComponentType() {
        return "cluster";
    }


    public ContainerState getContainerState(String sessionId, String containerId)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        synchronized (userLock) {
            logger.debug("Get container \"" + containerId + "\" state");
            if (securityManager.checkPermission(sessionId, new NusuthPermission(ComponentType.SCONTAINER, containerId, MONITOR_ACTION))) {
                ContainerState state = (ContainerState) stateCache.get(ComponentType.SCONTAINER, containerId);
                if (state != null) {
                    return state;
                } else {
                    ContainerManager cmanager = (ContainerManager) containerManagers.get(containerId);
                    if (cmanager != null) {
                        state = cmanager.getState();
                        stateCache.put(ComponentType.SCONTAINER, containerId, state);
                        return state;
                    } else {
                        logger.error("Get container \"" + containerId + "\" state: container not registered");
                        throw new ManagementException("Container \"" + containerId + "\" not registered");
                    }
                }
            } else {
                throw new AccessDeniedException(ComponentType.SCONTAINER, containerId, MONITOR_ACTION);
            }
        }
    }


    public ContainerManager getContainerStub(String deployerId,
                                             String containerId)
            throws ManagementException {
        synchronized (componentLock) {
            logger.debug("Get container \"" + containerId + " stub from deployer \""
                    + deployerId + '"');
            ComponentInfo cinfo = (ComponentInfo) deployerInfos.get(deployerId);
            if (cinfo != null
                    && securityManager.checkComponent(cinfo.getHost(),
                            cinfo.getAdminPort())) {
                return (ContainerManager) containerManagers.get(containerId);
            } else {
                return null;
            }
        }
    }


    public DistributorState getDistributorState(String sessionId, String distributorId)
            throws ManagementException, UnauthorizedAccessException, AccessDeniedException {
        synchronized (userLock) {
            logger.debug("Get distributor \"" + distributorId + "\" state");
            if (securityManager.checkPermission(sessionId, new NusuthPermission(ComponentType.SDISTRIBUTOR, distributorId, MONITOR_ACTION))) {
                DistributorState state = (DistributorState) stateCache.get(ComponentType.SDISTRIBUTOR, distributorId);
                if (state != null) {
                    return state;
                } else {
                    DistributorManager dmanager = (DistributorManager) distributorManagers.get(distributorId);
                    if (dmanager != null) {
                        state = dmanager.getState();
                        stateCache.put(ComponentType.SDISTRIBUTOR, distributorId, state);
                        return state;
                    } else {
                        logger.debug("Get distributor \"" + distributorId + "\" state: Distributor \"" + distributorId + "\" not registered");
                        throw new ManagementException("Distributor \"" + distributorId + "\" not registered");
                    }
                }
            } else {
                throw new AccessDeniedException(ComponentType.SDISTRIBUTOR, distributorId, MONITOR_ACTION);
            }
        }
    }


    private void getHostsDiff(HashMap oldHosts, HashMap newHosts, HashMap addedHosts, HashMap removedHosts) {
        HashMap notChanged = (HashMap) oldHosts.clone();
        HashMap added = (HashMap) newHosts.clone();
        HashMap removed = (HashMap) oldHosts.clone();

        notChanged.keySet().retainAll(newHosts.keySet());
        added.keySet().removeAll(oldHosts.keySet());
        removed.keySet().removeAll(newHosts.keySet());

        for (Iterator i = notChanged.values().iterator(); i.hasNext();) {
            VirtualHostInfo host = (VirtualHostInfo) i.next();
            String hostName = host.getName();
            VirtualHostInfo newHost = (VirtualHostInfo) newHosts.get(hostName);

            HashMap apps = (HashMap) host.getApplications().clone();
            HashMap addedApps = (HashMap) newHost.getApplications().clone();
            HashMap removedApps = (HashMap) host.getApplications().clone();

            apps.keySet().retainAll(newHost.getApplications().keySet());
            addedApps.keySet().removeAll(host.getApplications().keySet());
            removedApps.keySet().removeAll(newHost.getApplications().keySet());

            if (!addedApps.isEmpty())
                added.put(hostName, new VirtualHostInfo(hostName, newHost.getDocBase(), addedApps));
            if (!removedApps.isEmpty())
                removed.put(hostName, new VirtualHostInfo(hostName, host.getDocBase(), removedApps));

            for (Iterator j = apps.keySet().iterator(); j.hasNext();) {
                String url = (String) j.next();
                ApplicationInfoImpl app = (ApplicationInfoImpl) apps.get(url);
                ApplicationInfoImpl newApp = (ApplicationInfoImpl) newHost.getApplications().get(url);

                HashSet ncprots = (HashSet) app.getProtocols().clone();
                HashSet aprots = (HashSet) newApp.getProtocols().clone();
                HashSet rprots = (HashSet) app.getProtocols().clone();
                ncprots.retainAll(newApp.getProtocols());
                aprots.removeAll(app.getProtocols());
                rprots.removeAll(newApp.getProtocols());

                HashSet nconts = (HashSet) app.getContainers().clone();
                HashSet aconts = (HashSet) newApp.getContainers().clone();
                HashSet rconts = (HashSet) app.getContainers().clone();
                nconts.retainAll(newApp.getContainers());
                aconts.removeAll(app.getContainers());
                rconts.removeAll(newApp.getContainers());

                if (!aconts.isEmpty()) {
                    // add containers with new protocols
                    VirtualHostInfo tmp = (VirtualHostInfo) added.get(hostName);
                    if (tmp == null) {
                        tmp = new VirtualHostInfo(hostName, newHost.getDocBase(), new HashMap());
                        added.put(hostName, tmp);
                    }
                    tmp.getApplications().put(url, new ApplicationInfoImpl(newApp.isRunning(), aconts, newApp.getProtocols(), newApp.getLocation()));
                }
                if (!rconts.isEmpty()) {
                    // remove containers with old protocols
                    VirtualHostInfo tmp = (VirtualHostInfo) removed.get(hostName);
                    if (tmp == null) {
                        tmp = new VirtualHostInfo(hostName, host.getDocBase(), new HashMap());
                        removed.put(hostName, tmp);
                    }
                    tmp.getApplications().put(url, new ApplicationInfoImpl(app.isRunning(), rconts, app.getProtocols(), app.getLocation()));
                }

                /*                if (!aprots.isEmpty()) {
                 // add added protocols to not changed containers
                 VirtualHostInfo tmp = (VirtualHostInfo) added.get(hostName);
                 if (tmp == null) {
                 tmp = new VirtualHostInfo(hostName, new HashMap());
                 added.put(hostName, tmp);
                 }
                 tmp.getApplications().put(url, new ApplicationInfoImpl(newApp.isRunning(), nconts, aprots, newApp.getLocation()));
                 }
                 if (!rprots.isEmpty()) {
                 // remove removed protocols from not changed containers
                 VirtualHostInfo tmp = (VirtualHostInfo) removed.get(hostName);
                 if (tmp == null) {
                 tmp = new VirtualHostInfo(hostName, new HashMap());
                 removed.put(hostName, tmp);
                 }
                 tmp.getApplications().put(url, new ApplicationInfoImpl(app.isRunning(), nconts, rprots, app.getLocation()));
                 }*/
            }
        }

        addedHosts.putAll(added);
        removedHosts.putAll(removed);
    }


    public Vector getRegisteredComponents(String sessionId)
            throws UnauthorizedAccessException, ManagementException {
        synchronized (userLock) {
            logger.debug("get registered components");
            Vector components = new Vector();

            for (Iterator i = registeredComponents.iterator(); i.hasNext();) {
                ComponentInfo cinfo = (ComponentInfo) i.next();
                if (securityManager.checkPermission(sessionId, new NusuthPermission(cinfo.getComponentTypeName(), cinfo.getComponentId(), SEE_ACTION))) {
                    components.add(cinfo);
                }
            }

            return components;
        }
    }


    private File getWebInfStorage() throws ManagementException {
        String path;
        try {
            path = ((SimpleNusuthWebAppElement) settings.getSimpleChild("web-inf-storage").nextElement()).getContent();
        } catch (DeploymentException dex) {
            logger.error("Couldn't get web-inf storage path", dex);
            throw new ManagementException("Bad config, nested:" + dex.getMessage());
        }

        File result = new File(jbirdHome, path);
        if (!result.exists())
            result = new File(path);
        if (!result.exists()) {
            result = new File(jbirdHome, path);
            if (!result.mkdirs()) {
                logger.error("Couldn't find or create directory \"" + result.getAbsolutePath() + "\" for web-infs storage");
                throw new ManagementException("Couldn't find or create directory \"" + result.getAbsolutePath() + "\" for web-infs storage");
            }
        }
        if (!result.isDirectory() || !result.canRead() || !result.canWrite()) {
            logger.error("Couldn't find or use directory \"" + result.getAbsolutePath() + "\" for web-infs storage");
            throw new ManagementException("Couldn't find or use directory \"" + result.getAbsolutePath() + "\" for web-infs storage");
        }

        return result;
    }


    private ComponentInfo invokeComponent(String address, int adminPort)
            throws UnknownHostException, IOException, ManagementException {
        synchronized (componentLock) {
            synchronized (invokedComponents) {
                if (address.equalsIgnoreCase("localhost") || address.equals("127.0.0.1"))
                    address = InetAddress.getLocalHost().getHostAddress();
                else
                    address = InetAddress.getByName(address).getHostAddress();

                String key = address + ':' + adminPort;
                if (invokedComponents.keySet().contains(key))
                    throw new ComponentUnavailableException("<unknown>", key, "Component on " + key + " already invoked to reconnect");

                securityManager.invokeComponent(address, adminPort);
                ComponentInfo cinfo = new ComponentInfo(address, adminPort);
                invokedComponents.put(key, cinfo);

                logger.info("Component on " + address + ':' + adminPort + " sucessfully invoked");
                return cinfo;
            }
        }
    }


    public String login(String user, String encodedPassword) {
        synchronized (userLock) {
            return securityManager.login(user, encodedPassword);
        }
    }


    private void putWebInf(String hostName, String appName, InputStream webInf, boolean zipped)
            throws ManagementException {
        String infName = convertCharactersInName(hostName + appName);
        File infFile = new File(webInfStorage, infName);
        try {
            OutputStream ostream = new FileOutputStream(infFile);
            if (zipped)
                ostream = new BufferedOutputStream(ostream);
            else {
                ostream = new ZipOutputStream(ostream);
                ((ZipOutputStream) ostream).putNextEntry(new ZipEntry("WEB-INF" + File.separator + "web.xml"));
            }

            byte[] buff = new byte[2048];
            for (int readed = webInf.read(buff); readed > -1; readed = webInf.read(buff))
                ostream.write(buff, 0, readed);
            ostream.flush();
            ostream.close();
        } catch (IOException ioex) {
            logger.error("Couldn't save web application configuration for application \"" + appName + "\" on host \"" + hostName + "\"", ioex);
            throw new ManagementException("Couldn't save web application configuration for application \"" + appName + "\" on host \"" + hostName + "\"");
        }
    }


    private void refreshComponents() {
        logger.debug("Refresh components");
        for (Iterator i = callbacks.iterator(); i.hasNext();) {
            try {
                ((GuiCallback) i.next()).refreshComponentsList();
            } catch (ManagementException mex) {
            }
        }
    }


    private void refreshDeployment() {
        logger.debug("Refresh applications deployment");
        for (Iterator i = callbacks.iterator(); i.hasNext();) {
            try {
                ((GuiCallback) i.next()).refreshApplicationDeployment();
            } catch (ManagementException mex) {
                logger.debug("Refresh applications deployment failed ", mex);
            }
        }
    }


    private void refreshSettings(ComponentType componentType, String componentId) {
        logger.debug("Refresh settings for " + componentType + " \"" + componentId + "\"");
        for (Iterator i = callbacks.iterator(); i.hasNext();) {
            try {
                ((GuiCallback) i.next()).refreshComponentSettings(componentType.toString(), componentId);
            } catch (ManagementException mex) {
                logger.debug("Refresh settings for " + componentType + " \"" + componentId + "\" failed", mex);
            }
        }
    }

    private String getComponentIdInInvokedComponentsPool(String address, int port) {
        return address + ':' + port;
    }

    private void setContainersOnDistributors(String distributorId)
            throws ManagementException {
        if (distributorId == null) {
            for (Iterator i = distributorManagers.values().iterator(); i.hasNext();) {
                DistributorManager dmanager = (DistributorManager) i.next();
                dmanager.setContainers(containerAddresses);
            }
        } else {
            DistributorManager dmanager = (DistributorManager) distributorManagers.get(distributorId);
            if (dmanager != null)
                dmanager.setContainers(containerAddresses);
        }

    }

    private void addContainer(ComponentInfo containerInfo, ContainerManager containerStub)
            throws ManagementException {
        String name = containerInfo.getComponentId();
        containerManagers.put(name, containerStub);
        containerAddresses.put(name, new ContainerAddress(containerInfo.getHost(), containerStub.getHttpPort(), containerInfo.getAdminPort()));

        if (!isUseCallbacks)
        // it means that GUI callbacks system not used
        {
            // remove container from stored hosts
            // it needs to be sure that no hosts or apps will be associated with this container
            Collection hostsToDelete = new Vector();
            for (Iterator i = hosts.keySet().iterator(); i.hasNext();) {
                String hostName = (String) i.next();
                VirtualHostInfo host = (VirtualHostInfo) hosts.get(hostName);
                Map apps = host.getApplications();
                Collection appsToDelete = new Vector();
                for (Iterator j = apps.keySet().iterator(); j.hasNext();) {
                    String appName = (String) j.next();
                    ApplicationInfo app = (ApplicationInfo) apps.get(appName);
                    Set appContainers = app.getContainers();
                    appContainers.remove(name);
                    if (appContainers.isEmpty())
                        appsToDelete.add(appName);
                }
                for (Iterator j = appsToDelete.iterator(); j.hasNext();) {
                    apps.remove(j.next());
                }
                if (apps.isEmpty())
                    hostsToDelete.add(hostName);
            }
            for (Iterator i = hostsToDelete.iterator(); i.hasNext();) {
                hosts.remove(i.next());
            }

            try {
                // add container to stored hosts
                CompositeNusuthWebAppElement newHosts = NusuthAppConfigFactory.createConfig("application-deployment", containerStub.getVirtualHosts());
                for (Enumeration i = newHosts.getCompositeChild("host"); i.hasMoreElements();) {
                    VirtualHostInfo vhost = new VirtualHostInfo((CompositeNusuthWebAppElement) i.nextElement());
                    VirtualHostInfo oldHost = (VirtualHostInfo) hosts.get(vhost.getName());
                    if (oldHost == null)
                        hosts.put(vhost.getName(), vhost);
                    else
                        oldHost.merge(vhost);
                }

                setContainersOnDistributors(null);
                setAppsDeploymentOnDistributors(hosts, null);
            } catch (DeploymentException dex) {
                logger.error("Couldn't collect applications from container \"" + name + "\" (" + containerInfo.getHost() + containerInfo.getAdminPort() + ")", dex);
            }
        } else
        // GUI callbacks is used
        {
            applicationsNotChecked = true;
        }
    }

    private void addDistributor(ComponentInfo distributorInfo, DistributorManager distributorStub)
            throws ManagementException {
        String name = distributorInfo.getComponentId();
        distributorManagers.put(name, distributorStub);
        if (!isUseCallbacks) {
            setContainersOnDistributors(name);
            setAppsDeploymentOnDistributors(hosts, name);
        } else {
            applicationsNotChecked = true;
        }
    }

    public void registerInvokedComponent(String address, int port,
                                         ComponentManager componentStub)
            throws ManagementException {
        synchronized (componentLock) {
            logger.info("Register invoked component on host " + address + ':' + port);
            synchronized (invokedComponents) {
                String componentIdInInvokedComponentsPool = getComponentIdInInvokedComponentsPool(address, port);
                ComponentInfo cinfo = (ComponentInfo) invokedComponents.get(componentIdInInvokedComponentsPool);
                if (securityManager.checkComponent(address, port) && cinfo != null) {
                    synchronized (cinfo) {
                        String invokedComponentType = componentStub.getComponentType();
                        cinfo.setComponentType(new ComponentType(invokedComponentType));
                        cinfo.setComponentId(componentStub.getComponentId());
                        switch (cinfo.getComponentType().getType()) {
                            case ComponentType.CONTAINER:
                                {
                                    addContainer(cinfo, (ContainerManager) componentStub);
                                    break;
                                }
                            case ComponentType.DISTRIBUTOR:
                                {
                                    addDistributor(cinfo, (DistributorManager) componentStub);
                                    break;
                                }
                            case ComponentType.DEPLOYER:
                                {
                                    deployers.put(cinfo.getComponentId(), componentStub);
                                    deployerInfos.put(cinfo.getComponentId(), cinfo);
                                    break;
                                }
                            default :
                                logger.error("Trying for register component with unknown type \"" + invokedComponentType + "\" invoked on host " + address + ':' + port);
                                throw new ManagementException("Unknown component type for component \"" + componentId + '"');
                        }
                        registeredComponents.add(cinfo);
                        invokedComponents.remove(componentIdInInvokedComponentsPool);
                        refreshComponents();
                        if (cinfo.getComponentType().getType() != ComponentType.DEPLOYER)
                            refreshDeployment();
                        saveSettings();
                        logger.info("Invoked " + cinfo.getComponentTypeName() + " \"" + cinfo.getComponentId() + "\" on host " + address + ':' + port + " registered sucessfully");
                        cinfo.notifyAll();
                    }
                } else {
                    logger.info("Component (type " + cinfo.getComponentType() + ") \"" + cinfo.getComponentId() + "\" not registered, wrong answer");
                    if (cinfo != null)
                        showComponentRegisterError(cinfo.getComponentTypeName(), cinfo.getComponentId() + " on " + address + ':' + port, "Wrong answer");
                    else
                        showComponentRegisterError("<unknown>", address + ':' + port, "Unexpected attempt to register");
                }
            }
        }
    }


    public void removeComponent(String sessionId, ComponentInfo cinfo)
            throws ManagementException, UnauthorizedAccessException, AccessDeniedException {
        synchronized (userLock) {
            logger.info("Remove " + cinfo.getComponentTypeName() + " \"" + cinfo.getComponentId() + "\" on host " + cinfo.getHost() + ':' + cinfo.getAdminPort());
            if (securityManager.checkPermission(sessionId, new NusuthPermission(cinfo.getComponentTypeName(), cinfo.getComponentId(), REMOVE_ACTION))) {
                switch (cinfo.getComponentType().getType()) {
                    case ComponentType.DISTRIBUTOR:
                        {
                            DistributorManager manager = (DistributorManager) distributorManagers.get(cinfo.getComponentId());
                            if (manager == null) {
                                logger.error("Remove " + cinfo.getComponentTypeName() + " \"" + cinfo.getComponentId() + "\" on host " + cinfo.getHost() + ':' + cinfo.getAdminPort() + " failed, unknown distributor");
                                throw new ManagementException("Unknown " + cinfo.getComponentType() + " \"" + cinfo.getComponentId() + '"');
                            }
                            manager.stopServer();
                            distributorManagers.remove(cinfo.getComponentId());
                            break;
                        }
                    case ComponentType.CONTAINER:
                        {
                            ContainerManager manager = (ContainerManager) containerManagers.get(cinfo.getComponentId());
                            if (manager == null) {
                                logger.error("Remove " + cinfo.getComponentTypeName() + " \"" + cinfo.getComponentId() + "\" on host " + cinfo.getHost() + ':' + cinfo.getAdminPort() + " failed, unknown container");
                                throw new ManagementException("Unknown " + cinfo.getComponentType() + " \"" + cinfo.getComponentId() + '"');
                            }
                            manager.stopServer();
                            containerManagers.remove(cinfo.getComponentId());
                            break;
                        }
                    case ComponentType.DEPLOYER:
                        {
                            Deployer manager = (Deployer) deployers.get(cinfo.getComponentId());
                            if (manager == null) {
                                logger.error("Remove " + cinfo.getComponentTypeName() + " \"" + cinfo.getComponentId() + "\" on host " + cinfo.getHost() + ':' + cinfo.getAdminPort() + " failed, unknown deployer");
                                throw new ManagementException("Unknown " + cinfo.getComponentType() + " \"" + cinfo.getComponentId() + '"');
                            }
                            deployers.remove(cinfo.getComponentId());
                            break;
                        }
                }
                registeredComponents.remove(cinfo);
                saveSettings();
                refreshComponents();
            } else
                throw new AccessDeniedException(cinfo.getComponentTypeName(), cinfo.getComponentId(), REMOVE_ACTION);
        }
    }


    private void removeHostsFromContainers(HashMap removed)
            throws ManagementException {
        for (Iterator i = removed.values().iterator(); i.hasNext();) {
            VirtualHostInfo host = (VirtualHostInfo) i.next();
            String hostName = host.getName();
            HashMap applications = host.getApplications();
            for (Iterator j = applications.keySet().iterator(); j.hasNext();) {
                String url = (String) j.next();
                ApplicationInfoImpl app = (ApplicationInfoImpl) applications.get(url);
                for (Iterator k = app.getContainers().iterator(); k.hasNext();) {
                    String containerId = (String) k.next();
                    ContainerManager cmanager = (ContainerManager) containerManagers.get(containerId);
                    cmanager.removeApplication(hostName, url);
                }
            }
        }
    }


    protected void saveSettings()
            throws ManagementException {
        try {
            // reset virtual hosts information
            while (settings.getCompositeChild("host").hasMoreElements())
                settings.removeCompositeChild("host", (CompositeNusuthWebAppElement) settings.getCompositeChild("host").nextElement());

            for (Iterator i = hosts.values().iterator(); i.hasNext();)
                ((VirtualHostInfo) i.next()).addCompositeChild(settings);

            // reset components information
            while (settings.getCompositeChild("component").hasMoreElements())
                settings.removeCompositeChild("component", (CompositeNusuthWebAppElement) settings.getCompositeChild("component").nextElement());

            for (Iterator i = registeredComponents.iterator(); i.hasNext();)
                ((ComponentInfo) i.next()).addCompositeChild(settings);

        } catch (DeploymentException dex) {
            logger.error("Couldnt save settings", dex);
            throw new ManagementException("Couldnt save settings, nested:" + dex.getMessage());
        }

        super.saveSettings();
        logger.debug("Settings saved");
    }


    public void setApplicationsDeployment(String sessionId, InputStream newApps)
            throws ManagementException, UnauthorizedAccessException {
        synchronized (userLock) {
            logger.debug("Set applications deployment");
            applicationsNotChecked = true;

            HashMap newHosts = new HashMap();
            try {
                CompositeNusuthWebAppElement newAppsNode = NusuthAppConfigFactory.createConfig(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, newApps);
                for (Enumeration i = newAppsNode.getCompositeChild("host"); i.hasMoreElements();) {
                    VirtualHostInfo host = new VirtualHostInfo((CompositeNusuthWebAppElement) i.nextElement());
                    newHosts.put(host.getName(), host);
                }
            } catch (Exception ex) {
                logger.error("Couldn't set new applications deployment", ex);
                throw new ManagementException("Couldn't set new applications deployment, nested:" + ex.getMessage());
            }

            HashMap added = new HashMap();
            HashMap removed = new HashMap();
            getHostsDiff(hosts, newHosts, added, removed);
            added = restrictHosts(sessionId, added, ADD_ACTION);
            removed = restrictHosts(sessionId, removed, REMOVE_ACTION);

            try {
                if (!added.isEmpty())
                    deployHostsToContainers(added);

                patchWebInfs(added);

                setAppsDeploymentOnDistributors(newHosts, null);

                if (!removed.isEmpty())
                    removeHostsFromContainers(removed);

                hosts = newHosts;
                saveSettings();
            } finally {
                refreshDeployment();
            }
        }
    }


    private void setAppsDeploymentOnDistributors(HashMap newHosts, String distributorManagerId)
            throws ManagementException {
        if (!distributorManagers.isEmpty()) {
            CompositeNusuthWebAppElement deployment;
            try {
                deployment = NusuthAppConfigFactory.createConfig(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, new ByteArrayInputStream(ManagementUtil.EMPTY_APPLICATION_DEPLOYMENT_XML.getBytes()));
            } catch (DeploymentException dex) {
                logger.error("application-deployment.dtd is changed", dex);
                throw new ManagementException("application-deployment.dtd is changed, nested: " + dex.getMessage());
            }

            for (Iterator i = newHosts.values().iterator(); i.hasNext();) {
                try {
                    ((VirtualHostInfo) i.next()).addCompositeChild(deployment);
                } catch (DeploymentException dex) {
                    logger.debug("Couldn't add virtual host to Application-Deployment node", dex);
                }
            }

            byte[] bytes = deployment.compose(ManagementUtil.APPLICATION_DEPLOYMENT_TYPE, ManagementUtil.APPLICATION_DEPLOYMENT_TYPE + ".dtd").getBytes();

            if (distributorManagerId == null) {
                for (Iterator i = distributorManagers.values().iterator(); i.hasNext();) {
                    DistributorManager dmanager = (DistributorManager) i.next();
                    dmanager.setApplicationsDeployment(new ByteArrayInputStream(bytes));
                }
            } else {
                DistributorManager dmanager = (DistributorManager) distributorManagers.get(distributorManagerId);
                if (dmanager != null)
                    dmanager.setApplicationsDeployment(new ByteArrayInputStream(bytes));
            }
        }
    }


    public void setComponentSettings(String sessionId,
                                     ComponentType componentType,
                                     String componentId,
                                     InputStream componentSettings)
            throws ManagementException, UnauthorizedAccessException, AccessDeniedException {
        synchronized (userLock) {
            logger.debug("Set " + componentType + " \"" + componentId + "\" settings");
            if (securityManager.checkPermission(sessionId, new NusuthPermission(componentType.toString(), componentId, SET_CONFIG_ACTION))) {
                switch (componentType.getType()) {
                    case ComponentType.CONTAINER:
                        {
                            ContainerManager cmanager = (ContainerManager) containerManagers.get(componentId);
                            if (cmanager != null) {
                                cmanager.setSettings(componentSettings);
                            } else {
                                logger.error("Couldn't set container settings: container \"" + componentId + "\" not registered");
                                throw new ManagementException("Container \"" + componentId + "\" not registered");
                            }
                            break;
                        }
                    case ComponentType.DISTRIBUTOR:
                        {
                            DistributorManager dmanager = (DistributorManager) distributorManagers.get(componentId);
                            if (dmanager != null) {
                                dmanager.setSettings(componentSettings);
                            } else {
                                logger.error("Couldn't set distributor settings: distributor \"" + componentId + "\" not registered");
                                throw new ManagementException("Distributor \"" + componentId + "\" not registered");
                            }
                            break;
                        }
                    case ComponentType.DEPLOYER:
                        {
                            Deployer deployer = (Deployer) deployers.get(componentId);
                            if (deployer != null) {
                                deployer.setSettings(componentSettings);
                            } else {
                                logger.error("Couldn't set deployer settings: deployer \"" + componentId + "\" not registered");
                                throw new ManagementException("Deployer \"" + componentId + "\" not registered");
                            }
                            break;
                        }
                    case ComponentType.APPLICATION:
                        {
                            String hostName = componentId.substring(0, componentId.indexOf('/'));
                            String appName = componentId.substring(componentId.indexOf('/'));
                            putWebInf(hostName, appName, componentSettings, false);
                            VirtualHostInfo vhost = (VirtualHostInfo) hosts.get(hostName);
                            if (vhost == null) {
                                logger.error("Couldn't set web application settings: virtual host \"" + vhost.getName() + "\" not registered");
                                throw new ManagementException("Host \"" + hostName + "\" not found");
                            }
                            ApplicationInfoImpl app = (ApplicationInfoImpl) vhost.getApplications().get(appName);
                            String appLocation = app.getLocation();
                            if (app == null) {
                                logger.error("Couldn't set web application settings: web application \"" + appName + "\" on virtual host \"" + hostName + "\" not registered");
                                throw new ManagementException("Application \"" + appName + "\" on virtual host \"" + hostName + "\" not found");
                            }
                            for (Iterator i = app.getContainers().iterator(); i.hasNext();) {
                                patchWebInf(hostName, vhost.getDocBase(), appName, appLocation, (String) i.next());
                            }
                            break;
                        }
                    default :
                        logger.error("Couldn't set component settings: unknown component type for component \"" + componentId + '"');
                        throw new ManagementException("Unknown component type for component \"" + componentId + '"');
                }
                refreshSettings(componentType, componentId);
            } else
                throw new AccessDeniedException(componentType.toString(), componentId, SET_CONFIG_ACTION);
        }
    }


    public void setSettings(InputStream settings) throws ManagementException {
        synchronized (componentLock) {
        }
    }


    /**
     * Replace "web.xml" files on containers by file taked from WebInfStorage
     * Creation date: (22.02.2001 18:08:52)
     * @param: HashMap hostsToPath - virtual hosts tree for replace web-info
     * @throws ManagementException if any errors occured
     */
    private void patchWebInfs(HashMap hostsToPath)
            throws ManagementException {
        HashMap errors = new HashMap();
        for (Iterator i = hostsToPath.values().iterator(); i.hasNext();) {
            VirtualHostInfo vhost = (VirtualHostInfo) i.next();
            String hostName = vhost.getName();
            HashMap applications = vhost.getApplications();
            for (Iterator j = applications.keySet().iterator(); j.hasNext();) {
                String appName = (String) j.next();
                ApplicationInfoImpl app = (ApplicationInfoImpl) applications.get(appName);
                for (Iterator k = app.getContainers().iterator(); k.hasNext();) {
                    String containerName = (String) k.next();
                    try {
                        patchWebInf(hostName, vhost.getDocBase(), appName, app.getLocation(), containerName);
                    } catch (ManagementException mex) {
                        HashMap errHost = (HashMap) errors.get(hostName);
                        if (errHost == null) {
                            errHost = new HashMap();
                            errors.put(hostName, errHost);
                        }
                        HashSet errApp = (HashSet) errHost.get(appName);
                        if (errApp == null) {
                            errApp = new HashSet();
                            errHost.put(appName, errApp);
                        }
                        errApp.add(containerName);
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            logger.error("Couldn't set web application infos for applications: " + errors);
            throw new ManagementException("Couldn't set web application infos for applications: " + errors);
        }
    }


    public InputStream getApplicationsDeploymentErrors(String sessionId)
            throws ManagementException, UnauthorizedAccessException {
        synchronized (userLock) {
            logger.debug("Get applications deployment errors");
            return new ByteArrayInputStream(errorsElement.compose(ManagementUtil.APPLICATION_DEPLOYMENT_ERRORS_TYPE, ManagementUtil.APPLICATION_DEPLOYMENT_ERRORS_TYPE + ".dtd").getBytes());
        }
    }


    private InputStream getWebInf(String host, String docBase, String app, String location)
            throws ManagementException {
        String infName = convertCharactersInName(host + app);
        File infFile = new File(webInfStorage, infName);
        if (!infFile.exists())
            searchWebInf(host, docBase, app, location);
        try {
            return new BufferedInputStream(new FileInputStream(infFile));
        } catch (FileNotFoundException fnfex) {
            logger.error("Couldn't find web application config for application \"" + app + " on virtual host \"" + host + "\"", fnfex);
            throw new ManagementException("Couldn't find web application config for application \"" + app + " on virtual host \"" + host + "\"");
        }
    }


    private void patchWebInf(String host, String docBase, String app, String location, String container)
            throws ManagementException {
        ContainerManager cmanager = (ContainerManager) containerManagers.get(container);
        if (cmanager == null) {
            logger.error("Couldn't patch web-inf of application \"" + app + "\" on container \"" + container + "\": unknown container");
            throw new ManagementException("Unknown container \"" + container + '"');
        }
        cmanager.patchApplication(host, app, getWebInf(host, docBase, app, location), true);
    }


    private CompositeNusuthWebAppElement putErrorsToCompositeElement(HashMap errorsAdded, HashMap errorsRemoved)
            throws ManagementException {
        try {
            CompositeNusuthWebAppElement result = NusuthAppConfigFactory.createConfig(ManagementUtil.APPLICATION_DEPLOYMENT_ERRORS_TYPE, new ByteArrayInputStream(ManagementUtil.EMPTY_APPLICATION_ERROR_XML.getBytes()));
            putErrorToCompositeElement(result.setCompositeChild("unnecessary-hosts-error"), errorsAdded);
            putErrorToCompositeElement(result.setCompositeChild("missing-hosts-error"), errorsRemoved);
            return result;
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't collect errors to CompositeElement, nested:" + dex.getMessage());
        }
    }


    private void putErrorToCompositeElement(CompositeNusuthWebAppElement errorNode, HashMap errors)
            throws ManagementException {
        try {
            for (Iterator i = errors.keySet().iterator(); i.hasNext();) {
                String componentType = (String) i.next();
                HashMap componentMap = (HashMap) errors.get(componentType);
                for (Iterator j = (componentMap).keySet().iterator(); j.hasNext();) {
                    String componentName = (String) j.next();
                    CompositeNusuthWebAppElement componentNode = errorNode.addCompositeChild("component");
                    componentNode.setSimpleChild("type").setContent(componentType);
                    componentNode.setSimpleChild("name").setContent(componentName);
                    HashMap hostsMap = (HashMap) componentMap.get(componentName);
                    for (Iterator k = (hostsMap).values().iterator(); k.hasNext();)
                        ((VirtualHostInfo) k.next()).addCompositeChild(componentNode);
                }
            }
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't collect errors to CompositeElement, nested:" + dex.getMessage());
        }
    }


    private void refreshDeploymentErrors() {
        for (Iterator i = callbacks.iterator(); i.hasNext();) {
            try {
                ((GuiCallback) i.next()).refreshApplicationDeploymentErrors();
            } catch (ManagementException mex) {
            }
        }
    }


    public void registerCallback(GuiCallback gui) {
        synchronized (userLock) {
            logger.debug("Register GUI callback");
            callbacks.add(gui);
        }
    }


    private void restrict(String sessionId, Collection names, String componentType, String action)
            throws UnauthorizedAccessException, ManagementException {
        for (Iterator i = (new Vector(names)).iterator(); i.hasNext();) {
            String name = (String) i.next();
            if (!securityManager.checkPermission(sessionId, new NusuthPermission(componentType, name, action))) {
                names.remove(name);
            }
        }
    }


    private void searchWebInf(String hostName, String docBase, String appName, String location)
            throws ManagementException {
        InputStream inf = null;
        for (Iterator i = deployers.values().iterator(); inf == null && i.hasNext();) {
            try {
                inf = ((Deployer) i.next()).getWebInf(docBase, location);
                break;
            } catch (ManagementException mex) {
            }
        }
        if (inf == null) {
            logger.error("Couldn't locate web application configuration for application \"" + appName + "\" on host \"" + hostName + "\"");
            throw new ManagementException("Couldn't locate web application configuration for application \"" + appName + "\" on host \"" + hostName + "\"");
        }

        putWebInf(hostName, appName, inf, true);
    }


    protected void showComponentRegisterError(String componentType, String componentId, String message) {
        logger.debug("Show component register error");
        for (Iterator i = callbacks.iterator(); i.hasNext();) {
            try {
                ((GuiCallback) i.next()).showComponentRegisterError(componentType, componentId, message);
            } catch (ManagementException mex) {
            }
        }
    }


    private HashMap restrictHosts(String sessionId, HashMap vhosts, String action)
            throws UnauthorizedAccessException, ManagementException {
        HashMap result = new HashMap(vhosts.size());
        for (Iterator i = vhosts.values().iterator(); i.hasNext();) {
            VirtualHostInfo vhost = ((VirtualHostInfo) i.next());
            String hostName = vhost.getName();
            VirtualHostInfo newHost = new VirtualHostInfo(vhost.getName(), vhost.getDocBase(), new HashMap());
            HashMap oldApps = vhost.getApplications();
            HashMap newApps = newHost.getApplications();
            for (Iterator j = oldApps.keySet().iterator(); j.hasNext();) {
                String appName = (String) j.next();
                String appUrl = hostName + (appName.startsWith("/") ? appName : '/' + appName);
                if (securityManager.checkPermission(sessionId, new NusuthPermission(ComponentType.SAPPLICATION, appUrl, action)))
                    newApps.put(appName, oldApps.get(appName));
            }
            if (newHost.getApplications().size() > 0)
                result.put(hostName, newHost);
        }
        return result;
    }


    public InputStream getSecuritySettings(String sessionId)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        synchronized (userLock) {
            logger.debug("Get security settings");
            if (securityManager.checkPermission(sessionId, new NusuthPermission("*", "*", SEE_SECURITY_ACTION))) {
                return new ByteArrayInputStream(
                        securityManager.getSettings().compose("securityConfig", "security.dtd").getBytes()
                );
            } else {
                throw new AccessDeniedException("*", "*", SEE_SECURITY_ACTION);
            }
        }
    }


    public void setSecuritySettings(String sessionId, InputStream newSettings)
            throws UnauthorizedAccessException, ManagementException {
        synchronized (userLock) {
            logger.debug("Set security settings");
            CompositeNusuthWebAppElement securitySettingsNode;
            try {
                securitySettingsNode = NusuthAppConfigFactory.createConfig("securityConfig", newSettings);
            } catch (ParserException pex) {
                logger.error("Couldn't set new security settings", pex);
                throw new ManagementException("Couldn't set new security settings, nested:" + pex.getMessage());
            }

            if (securityManager.checkPermission(sessionId, new NusuthPermission("*", "*", SET_SECURITY_ACTION))) {
                securityManager.setSettings(securitySettingsNode);
            } else {
                securityManager.setUserPassword(sessionId, securitySettingsNode);
            }
        }
    }

    private ComponentInfo searchRegisteredComponent(String componentType, String componentId) {
        for (Iterator i = registeredComponents.iterator(); i.hasNext();) {
            ComponentInfo cinfo = (ComponentInfo) i.next();
            if (cinfo.getComponentTypeName().equals(componentType) && cinfo.getComponentId().equals(componentId))
                return cinfo;
        }
        return null;
    }

    public void reconnect(String sessionId, String componentType, String componentId)
            throws UnauthorizedAccessException, AccessDeniedException, ManagementException {
        synchronized (userLock) {
            logger.debug("Reconnect to " + componentType + " \"" + componentId + "\"");
            if (securityManager.checkPermission(sessionId, new NusuthPermission(componentType, componentId, SEE_ACTION))) {
                try {
                    ComponentInfo cinfo = searchRegisteredComponent(componentType, componentId);
                    if (cinfo != null)
                        invokeComponent(cinfo.getHost(), cinfo.getAdminPort());
                    else {
                        logger.error("Couldn't reconnect to " + componentType + " \"" + componentId + "\": component not registered");
                        throw new ComponentUnavailableException(componentType, componentId, componentType + " \"" + componentId + "\" not registered");
                    }
                } catch (UnknownHostException uhex) {
                    logger.error("Couldn't reconnect to " + componentType + " \"" + componentId + "\"", uhex);
                    throw new ComponentUnavailableException(componentType, componentId, "Couldn't reconnect to " + componentType + " \"" + componentId + "\", nested: " + uhex.getMessage());
                } catch (IOException ioex) {
                    logger.error("Couldn't reconnect to " + componentType + " \"" + componentId + "\"", ioex);
                    throw new ComponentUnavailableException(componentType, componentId, "Couldn't reconnect to " + componentType + " \"" + componentId + "\", nested: " + ioex.getMessage());
                }
            } else
                throw new AccessDeniedException(componentType, componentId, SEE_ACTION);
        }
    }

    public DistributedJNDIContext getDistributedContext() {
        return null;
    }
}
