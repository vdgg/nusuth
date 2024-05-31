package com.azoft.nusuth.distributor.connectionfactory;

import java.io.ByteArrayInputStream;

import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.NusuthAppConfigFactory;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import javax.naming.*;
import java.net.*;
import java.util.*;
import javax.naming.directory.*;

import com.azoft.nusuth.util.*;
import com.azoft.nusuth.container.InvocationCache;
import com.azoft.nusuth.jndi.*;
import com.azoft.nusuth.management.*;
import com.azoft.nusuth.jidep.*;
import com.azoft.nusuth.core.RemoteContainer;
import com.azoft.nusuth.distributor.ContainerWorkListener;
import org.apache.log4j.Category;

/**
 * This class is responsible for distributing sockets to the containers
 * and getting the appropriate container names.
 * As far as each container has a sosket stack which contaions sockets to this
 * containers this class contains methods that are able to give socket by
 * the container name and to give container name (container ID) by url
 * (where the request was send) here the load balancing mechanism is working.
 * And methods that allow to return used sockets to the socket stack of
 * the corresponding container.
 * @author VDGG, igork
 * @version 1.0
 * @since JBird 1.0
 */
public abstract class ContainerConnectionFactory implements Manageable {
    private final static String HTTP_CONTAINERS_ATTRIBUTE_NAME = "http";
    private final static String HTTPS_CONTAINERS_ATTRIBUTE_NAME = "https";

    private DirContext appContext = null;
    private DirContext clusterContext = null;
    private InvocationCache requestCache = new InvocationCache();

    protected Hashtable containerID2number = new Hashtable();
    protected Stack[] containerNumber2socketsStack = new Stack[0];
    protected StrBuffer[] containerNumber2containerID = new StrBuffer[0];
    protected ContainerAddress[] containerNumber2ContainerAddress =
            new ContainerAddress[0];

    protected Category logger = Category.getInstance(this.getClass().getName());
    protected LogCategoryProxy logProxy =
            LogCategoryProxy.getInstance(this.getClass().getName());

    protected Map parameters = new HashMap();

    private Hashtable contId2Adapter = new Hashtable();
    private Hashtable localToRemote = new Hashtable();
    private String distributorId = null;
    private String containersKey = null;
    private ContainerWorkListener listener;
    private Hashtable containerName2listOfContext = new Hashtable();
    private Hashtable contextPath2ContainerAdresses = new Hashtable();


    public ContainerConnectionFactory() throws ManagementException {
        super();
        appContext = new NusuthJNDIDirContext();
    }

    public void setParameters(Map newParameters) {
        parameters = newParameters;
    }

    public Map getParemeters() {
        return parameters;
    }

    private void addApplication(String url, DistributorApplicationInfo app)
            throws DeploymentException {
        reallyBindApplication(url);
        reallyBindContainers(url, app.getContainers(), app.getProtocols());
    }


    protected void addContainer(String name, ContainerAddress addr) {
        int oldsize = containerNumber2ContainerAddress.length;
        int newsize = oldsize + 1;

        ContainerAddress[] tmpa = new ContainerAddress[newsize];
        System.arraycopy(containerNumber2ContainerAddress, 0, tmpa, 0, oldsize);
        tmpa[oldsize] = addr;
        containerNumber2ContainerAddress = tmpa;

        StrBuffer[] tmpb = new StrBuffer[newsize];
        System.arraycopy(containerNumber2containerID, 0, tmpb, 0, oldsize);
        tmpb[oldsize] = new StrBuffer();
        tmpb[oldsize].append(name);
        containerNumber2containerID = tmpb;

        Stack[] tmpc = new Stack[newsize];
        System.arraycopy(containerNumber2socketsStack, 0, tmpc, 0, oldsize);
        tmpc[oldsize] = new Stack();
        containerNumber2socketsStack = tmpc;

        containerID2number.put(tmpb[oldsize], new Integer(oldsize));
    }


    protected void addHost(VirtualHostInfo host) throws DeploymentException {
        HashMap apps = host.getApplications();
        for (Iterator i = apps.keySet().iterator(); i.hasNext();) {
            String url = (String) i.next();
            DistributorApplicationInfo dapi =
                    (DistributorApplicationInfo) apps.get(url);
            if (url.startsWith("/"))
                url = host.getName() + url;
            else
                url = host.getName() + "/" + url;
            addApplication(url, dapi);
        }
    }

    /**
     * This method sets contaners and hosts to connetion factory and tell to all
     * containers about all other containers wich has the same application that
     * current has.
     * @param settings Distributor settings.
     * @exception DeploymentException Throws if any errors occures during parsing
     * <i>settings</i>.
     */
    public void applySettings(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        distributorId = ((SimpleNusuthWebAppElement) settings.
                getSimpleChild("name").nextElement()).getContent().trim();
        containersKey = ((SimpleNusuthWebAppElement)
                ((CompositeNusuthWebAppElement) settings.
                getCompositeChild("manager").nextElement()).
                getSimpleChild("auth-key").nextElement()).getContent().trim();
        /*// set containers:
        HashMap containers = new HashMap();
        for (Enumeration containersEnum
            = settings.getCompositeChild("container-info");
             containersEnum.hasMoreElements();) {
          CompositeNusuthWebAppElement curr
              = (CompositeNusuthWebAppElement) containersEnum.nextElement();
          String name = getSimpleString(curr, "name");
          containers.put(name, new ContainerAddress(
              getSimpleString(curr, "container-host"),
              getSimpleInt(curr, "port"), getSimpleInt(curr, "admin-port")));
        }
        setContainers(containers);
        // set hosts:
        HashMap hosts = new HashMap();
        for (Enumeration hostsEnum = settings.getCompositeChild("host");
             hostsEnum.hasMoreElements();) {
          VirtualHostInfo vhost
              = new VirtualHostInfo((CompositeNusuthWebAppElement)
                                    hostsEnum.nextElement());
          hosts.put(vhost.getName(), vhost);
        }
        setHosts(hosts);*/

        sendInformationAboutContainers(this.getHosts(), this.getContainers());
    }

    private void sendInformationAboutContainers(Map containers, Map hosts) {
        Iterator iterator = hosts.keySet().iterator();
        while (iterator.hasNext()) {
            String hostName = (String) iterator.next();
            VirtualHostInfo host = (VirtualHostInfo) hosts.get(hostName);
            HashMap appls = host.getApplications();
            Iterator appIter = appls.keySet().iterator();
            while (appIter.hasNext()) {
                String appName = (String) appIter.next();
                Set conts = ((ApplicationInfo) appls.get(appName)).getContainers();
                Iterator contIterator1 = conts.iterator();
                while (contIterator1.hasNext()) {
                    Iterator contIterator = conts.iterator();
                    String contName1 = (String) contIterator1.next();
                    if (containerName2listOfContext.get(contName1) != null) {
                        ((LinkedList) containerName2listOfContext.get(contName1)).
                                add(hostName + appName);
                    } else {
                        LinkedList l = new LinkedList();
                        l.add(hostName + appName);
                        containerName2listOfContext.put(contName1, l);
                    }
                    if (contextPath2ContainerAdresses.get(hostName + appName) != null) {
                        LinkedList l = (LinkedList) contextPath2ContainerAdresses.
                                get(hostName + appName);
                        l.add(contName1);
                        l.add(containers.get(contName1));
                    } else {
                        LinkedList l = new LinkedList();
                        l.add(contName1);
                        l.add(containers.get(contName1));
                        contextPath2ContainerAdresses.put(hostName + appName, l);
                    }
                    try {
                        Socket socket
                                = new Socket(((ContainerAddress) containers.
                                get(contName1)).host,
                                        ((ContainerAddress) containers.
                                get(contName1)).adminPort);
                        ClientJidepAdapter adapter
                                = JidepProtocolAdapter.getClientSide(socket, containersKey);
                        adapter.processAuthenticate();
                        if (adapter.getResponseCode() == 200) {
                            adapter.setCommand("containers");
                            ObjectOutputStream stream
                                    = new ObjectOutputStream(adapter.getOutputStream());
                            stream.writeObject(distributorId);
                            stream.writeObject(hostName + appName);
                            LinkedList list = new LinkedList();
                            LinkedList listOfCont = new LinkedList();
                            while (contIterator.hasNext()) {
                                String contName = (String) contIterator.next();
                                if (!contName.equals(contName1)) {
                                    list.add(contName);
                                    list.add(containers.get(contName));
                                    listOfCont.
                                            add(new RemoteContainer(
                                                    ((ContainerAddress) containers.
                                            get(contName)).host,
                                                    ((ContainerAddress) containers.
                                            get(contName)).adminPort,
                                                    ((ContainerAddress) containers.
                                            get(contName)).port, contName));
                                }
                                localToRemote.put(contName1, listOfCont.clone());
                            }
                            stream.writeObject(list);
                            adapter.endRequest();
                            adapter.parseResponse();
                            adapter.close();
                        } else {
                            logger.debug("Server not authenticate request");
                        }
                    } catch (IOException e) {
                        logger.error("Cannot send information about containers", e);
                    }
                }
            }
        }
    }

    /**
     * This method send information to all containers which knew about container
     * with name <i>contId</i> about its down
     * @param contId Dead container id
     */
    public synchronized void onContainerDown(StrBuffer contId) {
        int number = -1;
        if (containerID2number.get(contId) != null) {
            number = ((Integer) containerID2number.remove(contId)).intValue();
            Enumeration enum = containerID2number.keys();
            while (enum.hasMoreElements()) {
                StrBuffer key = (StrBuffer) enum.nextElement();
                int value = ((Integer) containerID2number.get(key)).intValue();
                if (value > number) {
                    containerID2number.put(key, new Integer(value - 1));
                }
            }
        }
        if (number != -1) {
            ContainerAddress addr = containerNumber2ContainerAddress[number];
            listener.addDeadContainer(new RemoteContainer(addr.host, addr.adminPort,
                    addr.port,
                    contId.toString()));
            {
                ContainerAddress[] temp
                        = new ContainerAddress[containerNumber2ContainerAddress.
                        length - 1];
                System.arraycopy(containerNumber2ContainerAddress, 0, temp, 0, number);
                System.arraycopy(containerNumber2ContainerAddress, number + 1, temp,
                        number,
                        containerNumber2ContainerAddress.length - number - 1);
                containerNumber2ContainerAddress = temp;
            }
            {
                StrBuffer[] temp = new StrBuffer[containerNumber2containerID.length - 1];
                System.arraycopy(containerNumber2containerID, 0, temp, 0, number);
                System.arraycopy(containerNumber2containerID, number + 1, temp, number,
                        containerNumber2socketsStack.length - number - 1);
                containerNumber2containerID = temp;
            }
            {
                Stack[] temp = new Stack[containerNumber2socketsStack.length - 1];
                System.arraycopy(containerNumber2socketsStack, 0, temp, 0, number);
                System.arraycopy(containerNumber2socketsStack, number + 1, temp, number,
                        containerNumber2socketsStack.length - number - 1);
                containerNumber2socketsStack = temp;
            }

            try {
                List list = (List) localToRemote.get(contId.toString());
                for (int i = 0; i < list.size(); i++) {
                    RemoteContainer remote = (RemoteContainer) list.get(i);
                    ClientJidepAdapter adapter
                            = JidepProtocolAdapter.getClientSide(
                                    new Socket(remote.getHost(), remote.getAdminPort()),
                                    containersKey);
                    adapter.processAuthenticate();
                    adapter.setCommand("down");
                    ObjectOutputStream stream
                            = new ObjectOutputStream(adapter.getOutputStream());
                    stream.writeObject(distributorId);
                    stream.writeObject(contId.toString());
                    adapter.endRequest();
                    adapter.parseResponse();
                }
            } catch (IOException e) {
                logger.error("Cannot send information about containers down", e);
            }
        }
    }

    /**
     * This method send information to all containers which know about
     * <i>cont</i>.
     * about container come alive.
     * @param cont Container thah come alive.
     */
    public synchronized void onContainerUp(RemoteContainer cont) {
        StrBuffer contId = new StrBuffer();
        contId.append(cont.getName());
        int number = containerID2number.size();
        containerID2number.put(contId, new Integer(number));
        {
            ContainerAddress[] temp
                    = new ContainerAddress[containerNumber2ContainerAddress.length + 1];
            System.arraycopy(containerNumber2ContainerAddress, 0, temp, 0,
                    containerNumber2ContainerAddress.length);
            temp[containerNumber2ContainerAddress.length]
                    = new ContainerAddress(cont.getHost(), cont.getPort(),
                            cont.getAdminPort());
            containerNumber2ContainerAddress = temp;
        }
        {
            StrBuffer[] temp = new StrBuffer[containerNumber2containerID.length + 1];
            System.arraycopy(containerNumber2containerID, 0, temp, 0,
                    containerNumber2containerID.length);
            temp[containerNumber2containerID.length] = contId;
            containerNumber2containerID = temp;
        }
        {
            Stack[] temp = new Stack[containerNumber2socketsStack.length + 1];
            System.arraycopy(containerNumber2socketsStack, 0, temp, 0,
                    containerNumber2socketsStack.length);
            temp[containerNumber2socketsStack.length] = new Stack();
            containerNumber2socketsStack = temp;
        }
        try {
            ClientJidepAdapter adap
                    = JidepProtocolAdapter.getClientSide(new Socket(cont.getHost(),
                            cont.
                    getAdminPort()),
                            containersKey);
            adap.processAuthenticate();
            List list = (List) localToRemote.get(contId.toString());
            for (int i = 0; i < list.size(); i++) {
                RemoteContainer remote = (RemoteContainer) list.get(i);
                ClientJidepAdapter adapter
                        = JidepProtocolAdapter.getClientSide(
                                new Socket(remote.getHost(), remote.getAdminPort()),
                                containersKey);
                adapter.processAuthenticate();
                List l = (List) containerName2listOfContext.get(remote.getName());
                for (int j = 0; j < l.size(); j++) {
                    String path = (String) l.get(j);
                    adap.setCommand("containers");
                    ObjectOutputStream os
                            = new ObjectOutputStream(adap.getOutputStream());
                    os.writeObject(distributorId);
                    os.writeObject(path);
                    os.writeObject(contextPath2ContainerAdresses.get(path));
                    adap.endRequest();
                    adap.parseResponse();
                    adapter.setCommand("containers");
                    ObjectOutputStream stream
                            = new ObjectOutputStream(adapter.getOutputStream());
                    stream.writeObject(distributorId);
                    stream.writeObject(path);
                    LinkedList container = new LinkedList();
                    container.add(contId.toString());
                    container.add(containerNumber2ContainerAddress
                            [containerNumber2ContainerAddress.length - 1]);
                    stream.writeObject(container);
                    adapter.endRequest();
                    adapter.parseResponse();
                }
            }
        } catch (IOException e) {
            logger.error("Cannot send information about containers down", e);
        }
    }

    private int[] attribute2containerNumbers(Attribute attr) {
        if (attr == null) {
            return null;
        }
        int attr_size = attr.size();
        if (attr_size == 0)
            return null;
        int[] result = new int[attr_size];
        int all = 0;
        try {
            for (int i = 0; i < attr_size; i++) {
                if (containerID2number.get(attr.get(i)) != null) {
                    result[i] =
                            ((Integer) (containerID2number.get(attr.get(i)))).intValue();
                    all++;
                }
            }
        } catch (NamingException nex) {
            return null;
        }
        int[] temp = new int[all];
        System.arraycopy(result, 0, temp, 0, all);
        return temp;
    }


    protected abstract int balanceLoading(int[] containers);

    public Socket createConnectionByContainerID(StrBuffer containerID,
                                                int protocol) {
        Integer container = (Integer) containerID2number.get(containerID);
        if (container == null) {
            logger.error("Error occured while connecting to container "
                    + containerID);
            return null;
        }
        return createConnectionByContainerNumber(container.intValue());
    }


    private Socket createConnectionByContainerNumber(int container) {
        Stack sockets;
        synchronized (containerNumber2socketsStack) {
            sockets = containerNumber2socketsStack[container];
            if (sockets == null) {
                logger.error("Error occured while adding container number " + container);
                sockets = new Stack();
                containerNumber2socketsStack[container] = sockets;
                return null;
            }
        }

        synchronized (sockets) {
            if (sockets.empty()) {
                try {
                    if (logProxy.isDebugEnabled())
                        logger.debug("Create new connection to container #" + container);
                    return new Socket(containerNumber2ContainerAddress[container].host, containerNumber2ContainerAddress[container].port);
                } catch (Exception ex) {
                    logger.error("Error while creating socket to container " + containerNumber2ContainerAddress[container].host + ":" + containerNumber2ContainerAddress[container].port, ex);
                    return null;
                }
            } else {
                return (Socket) sockets.pop();
            }
        }
    }


    public Socket createConnectionByURI(StrBuffer uri, StrBuffer containerId,
                                        int protocol) {
        RequestCacheElement rce = (RequestCacheElement) requestCache.find(uri);
        if (rce == null) {
            rce = new RequestCacheElement();
            rce.containerNumbers = findContainers(uri, protocol);
            requestCache.add(uri, rce);
        }
        rce.lastAccess = System.currentTimeMillis();

        if (rce.containerNumbers == null || rce.containerNumbers.length == 0)
            return null;

        int container = balanceLoading(rce.containerNumbers);

        containerId.clear();
        containerId.append(containerNumber2containerID[container]);
        return createConnectionByContainerNumber(container);
    }

    public void clearRequestCache() {
        requestCache.clear();
    }


    /**
     * Insert the method's description here. Creation date: (16.11.00 12:15:15)
     * @return java.util.Vector
     * @param url java.lang.String
     */
    private int[] findContainers(StrBuffer uri, int protocol) {
        if (logProxy.isDebugEnabled())
            logger.debug("Find container numbers for uri \"" + uri.toString() + '"');

        String protocolName = null;
        switch (protocol) {
            case ApplicationInfo.HTTP_PROTOCOL:
                protocolName = HTTP_CONTAINERS_ATTRIBUTE_NAME;
                break;
            case ApplicationInfo.HTTPS_PROTOCOL:
                protocolName = HTTPS_CONTAINERS_ATTRIBUTE_NAME;
                break;
            default :
                if (logProxy.isDebugEnabled())
                    logger.debug("Find container numbers for unknown protocol #"
                            + protocol);
                return null;
        }

        // find requested uri
        StrBuffer currUri = uri.cloneBuf();
        while (currUri.length() > 0) {
            try {
                Attributes resultAttrs = appContext.getAttributes(currUri.toString());
                if (resultAttrs != null && resultAttrs.size() > 0) {
                    Attribute protocols = resultAttrs.get("protocols");
                    if (protocols.contains(protocolName)) {
                        Attribute containers = resultAttrs.get("contexts");
                        if (containers != null && containers.size() > 0)
                            return attribute2containerNumbers(containers);
                    } else
                        break;
                }
            } catch (NamingException nex) {
            }
            currUri.cutToChar('/', true);
        }

        // requested uri not found, try default host
        String tmp = uri.toString();
        tmp = "*" + tmp.substring(tmp.indexOf('/'));
        currUri = new StrBuffer(tmp.length());
        currUri.append(tmp);
        while (currUri.length() > 0) {
            try {
                Attributes resultAttrs = appContext.getAttributes(currUri.toString());
                if (resultAttrs != null && resultAttrs.size() > 0) {
                    Attribute protocols = resultAttrs.get("protocols");
                    if (protocols.contains(protocolName)) {
                        Attribute containers = resultAttrs.get("contexts");
                        if (containers != null && containers.size() > 0)
                            return attribute2containerNumbers(containers);
                    } else
                        return null;
                }
            } catch (NamingException nex) {
            }
            currUri.cutToChar('/', true);
        }
        return null;
    }


    public CompositeNusuthWebAppElement getApplicationsDeployment()
            throws ManagementException {
        CompositeNusuthWebAppElement result = null;
        try {
            result = NusuthAppConfigFactory.createConfig(
                    ManagementUtil.APPLICATION_DEPLOYMENT_TYPE,
                    new ByteArrayInputStream(
                            ManagementUtil.EMPTY_APPLICATION_DEPLOYMENT_XML.getBytes()
                    )
            );
        } catch (DeploymentException dex) {
            throw new ManagementException("\"application-deployment\" DTD is changed,"
                    + " nested: " + dex.getMessage());
        }
        try {
            for (NamingEnumeration hosts = appContext.list(new CompositeName());
                 hosts.hasMore();) {
                NameClassPair ncp = (NameClassPair) hosts.next();
                String hostName = ncp.getName();
                HashMap apps = new HashMap();
                retrieveApplications(hostName, apps);
                VirtualHostInfo host = new VirtualHostInfo(hostName, "", apps);
                host.addCompositeChild(result);
            }
        } catch (NamingException nex) {
            if (logProxy.isDebugEnabled())
                logger.debug("Couldn't retrieve hosts from ContainerConnectionFactory,"
                        + " nested:", nex);
        } catch (DeploymentException dex) {
            if (logProxy.isDebugEnabled())
                logger.debug("Couldn't retrieve hosts from ContainerConnectionFactory,"
                        + " nested:" + dex);
        }
        return result;
    }


    public HashMap getHosts() {
        HashMap result = new HashMap();
        try {
            for (NamingEnumeration hosts = appContext.list(new CompositeName());
                 hosts.hasMore();) {
                NameClassPair ncp = (NameClassPair) hosts.next();
                String hostName = ncp.getName();
                HashMap apps = new HashMap();
                retrieveApplications(hostName, apps);
                result.put(hostName, new VirtualHostInfo(hostName, "", apps));
            }
        } catch (NamingException nex) {
            if (logProxy.isDebugEnabled())
                logger.debug("Couldn't retrieve hosts from ContainerConnectionFactory,"
                        + " nested:", nex);
        }
        return result;
    }


    private int getSimpleInt(CompositeNusuthWebAppElement settings,
                             String elementName)
            throws DeploymentException {
        Enumeration simpleEnumeration = settings.getSimpleChild(elementName);
        if (!simpleEnumeration.hasMoreElements()) {
            throw new DeploymentException("Cannot find " + elementName
                    + " element, wrong config");
        }
        try {
            return Integer.parseInt(((SimpleNusuthWebAppElement)
                    simpleEnumeration.nextElement()).getContent());
        } catch (Exception ex) {
            throw new DeploymentException("Cannot convert element " + elementName
                    + " to int");
        }
    }


    private final String getSimpleString(CompositeNusuthWebAppElement node,
                                         String name)
            throws DeploymentException {
        Enumeration enum = node.getSimpleChild(name);
        if (enum.hasMoreElements())
            return ((SimpleNusuthWebAppElement) enum.nextElement()).getContent();
        else
            return "";
    }


    public boolean isRestartNeeded(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        return false;
    }


    private void reallyBindApplication(String url) throws DeploymentException {
        if (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

        Name name;
        try {
            name = new CompositeName(url);
        } catch (InvalidNameException inex) {
            throw new DeploymentException("Invalid uri \"" + url + "\", nested:"
                    + inex.getMessage());
        }
        int name_size = name.size();
        Context currContext = appContext;
        String currName = null;
        for (int i = 0; i < name_size; i++) {
            currName = name.get(i);
            boolean notBinded = false;
            Context tmpContext = null;
            try {
                notBinded = (tmpContext = (DirContext) currContext.lookup(currName))
                        == null;
            } catch (NamingException nex) {
                notBinded = true;
            }
            if (notBinded) {
                try {
                    currContext.bind(currName, tmpContext = new NusuthJNDIDirContext());
                } catch (NamingException nex) {
                    throw new DeploymentException("Couldn't bind \"" + currName
                            + "\", nested:" + nex.getMessage());
                }
            }
            currContext = tmpContext;
        }
    }


    protected void removeApplication(String url) throws DeploymentException {
        try {
            appContext.modifyAttributes(url, DirContext.REMOVE_ATTRIBUTE,
                    appContext.getAttributes(url));
            appContext.unbind(url);
        } catch (NamingException nex) {
            throw new DeploymentException("Couldn't remove virtual host \"" + url
                    + "\", nested:" + nex.getMessage());
        }
    }


    protected void removeContainer(String name) {
        StrBuffer nameBuf = new StrBuffer();
        nameBuf.append(name);
        int number = ((Integer) containerID2number.remove(nameBuf)).intValue();
        int size = containerNumber2ContainerAddress.length - 1;

        Stack socks = containerNumber2socketsStack[number];
        while (!socks.isEmpty())
            try {
                ((Socket) socks.pop()).close();
            } catch (Throwable t) {
            }

        ContainerAddress[] tmpa = new ContainerAddress[size];
        System.arraycopy(containerNumber2ContainerAddress, 0, tmpa, 0, number);
        System.arraycopy(containerNumber2ContainerAddress, number + 1, tmpa, number,
                size - number);
        containerNumber2ContainerAddress = tmpa;

        StrBuffer[] tmpb = new StrBuffer[size];
        System.arraycopy(containerNumber2containerID, 0, tmpb, 0, number);
        System.arraycopy(containerNumber2containerID, number + 1, tmpb, number,
                size - number);
        containerNumber2containerID = tmpb;

        Stack[] tmpc = new Stack[size];
        System.arraycopy(containerNumber2socketsStack, 0, tmpc, 0, number);
        System.arraycopy(containerNumber2socketsStack, number + 1, tmpc, number,
                size - number);
        containerNumber2socketsStack = tmpc;

        for (Iterator i = containerID2number.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            int value = ((Integer) containerID2number.get(key)).intValue();
            if (value > number)
                containerID2number.put(key, new Integer(value - 1));
        }
    }


    protected void removeHost(VirtualHostInfo host) throws DeploymentException {
        HashMap applicationsMap = host.getApplications();
        for (Iterator apps = applicationsMap.keySet().iterator(); apps.hasNext();) {
            String url = (String) apps.next();
            if (url.startsWith("/"))
                url = host.getName() + url;
            else
                url = host.getName() + "/" + url;
            removeApplication(url);
        }
    }


    private void retrieveApplications(String uri, HashMap apps)
            throws NamingException {
        NamingEnumeration bindings = appContext.list(uri);
        if (bindings.hasMore()) {
            while (bindings.hasMore()) {
                NameClassPair ncp = (NameClassPair) bindings.next();
                retrieveApplications(uri + '/' + ncp.getName(), apps);
            }
        }
        Attributes result = appContext.getAttributes(uri);
        if (result != null && result.size() > 0) {
            HashSet protocols = new HashSet();
            HashSet containers = new HashSet();
            Attribute protocolsAttr = result.get("protocols");
            if (protocolsAttr != null)
                for (NamingEnumeration enum = protocolsAttr.getAll(); enum.hasMore();) {
                    protocols.add(enum.next());
                }
            Attribute contextsAttr = result.get("contexts");
            if (contextsAttr != null)
                for (NamingEnumeration enum = contextsAttr.getAll(); enum.hasMore();) {
                    containers.add(enum.next().toString());
                }
            int slashPos = uri.indexOf('/');
            String appUri = slashPos > -1 ? uri.substring(slashPos) : "/";
            apps.put(appUri, new ApplicationInfoImpl(true, containers,
                    protocols, ""));
        }
    }


    public void returnSocket(StrBuffer containerID, Socket socket) {
        if (containerID == null || socket == null || containerID.length() == 0)
            return;
        Integer containerNumber = (Integer) containerID2number.get(containerID);
        if (containerNumber == null) {
            logger.error("Attempt to return socket to unexisting container "
                    + containerID);
            try {
                socket.close();
            } catch (IOException ioex) {
                logger.error("Error occured while closing socket", ioex);
            }
        } else {
            if (logProxy.isDebugEnabled())
                logger.debug("Return socket:" + containerID);
            Stack sockets = containerNumber2socketsStack[containerNumber.intValue()];
            synchronized (sockets) {
                sockets.push(socket);
            }
        }
    }


    public void setContainers(Map newConts)
            throws DeploymentException {
        HashSet newContNames = new HashSet();
        newContNames.addAll(newConts.keySet());

        Set contNames = containerID2number.keySet();

        HashSet notChanged = (HashSet) newContNames.clone();
        HashSet added = (HashSet) newContNames.clone();
        HashSet deleted = new HashSet(contNames.size());
        for (Iterator i = contNames.iterator(); i.hasNext();)
            deleted.add(i.next().toString());

        notChanged.retainAll(contNames);
        added.removeAll(contNames);
        deleted.removeAll(newContNames);

        for (Iterator i = deleted.iterator(); i.hasNext();)
            removeContainer((String) i.next());

        for (Iterator i = added.iterator(); i.hasNext();) {
            String name = (String) i.next();
            addContainer(name, (ContainerAddress) newConts.get(name));
        }

        for (Iterator i = notChanged.iterator(); i.hasNext();) {
            String name = (String) i.next();
            StrBuffer nameBuf = new StrBuffer();
            nameBuf.append(name);
            int number = ((Integer) containerID2number.get(nameBuf)).intValue();
            ContainerAddress cont = containerNumber2ContainerAddress[number];
            ContainerAddress newCont = (ContainerAddress) newConts.get(name);
            if (!cont.host.equals(newCont.host) || cont.port != newCont.port) {
                Stack socks = containerNumber2socketsStack[number];
                while (!socks.isEmpty()) {
                    Socket socket = (Socket) socks.pop();
                    try {
                        socket.close();
                    } catch (Throwable t) {
                    }
                }
                cont.host = newCont.host;
                cont.port = newCont.port;
            }
        }
    }

    public Map getContainers()
            throws DeploymentException {
        HashMap result = new HashMap();

        for (Iterator i = containerID2number.keySet().iterator(); i.hasNext();) {
            StrBuffer name = (StrBuffer) i.next();
            int number = ((Integer) containerID2number.get(name)).intValue();
            ContainerAddress cont = containerNumber2ContainerAddress[number];
            result.put(name.toString(), cont);
        }
        return result;
    }

    public void setHosts(HashMap newHosts) throws DeploymentException {

        HashMap oldHosts = getHosts();

        HashMap notChanged = (HashMap) oldHosts.clone();
        HashMap added = (HashMap) newHosts.clone();
        HashMap deleted = (HashMap) oldHosts.clone();

        notChanged.keySet().retainAll(newHosts.keySet());
        added.keySet().removeAll(oldHosts.keySet());
        deleted.keySet().removeAll(newHosts.keySet());

        for (Iterator i = added.values().iterator(); i.hasNext();)
            addHost((VirtualHostInfo) i.next());

        for (Iterator i = deleted.values().iterator(); i.hasNext();)
            removeHost((VirtualHostInfo) i.next());

        for (Iterator i = notChanged.values().iterator(); i.hasNext();) {
            VirtualHostInfo oldHost = (VirtualHostInfo) i.next();
            VirtualHostInfo newHost =
                    (VirtualHostInfo) newHosts.get(oldHost.getName());

            HashMap notChangedApps = (HashMap) oldHost.getApplications().clone();
            HashMap addedApps = (HashMap) newHost.getApplications().clone();
            HashMap deletedApps = (HashMap) oldHost.getApplications().clone();

            notChangedApps.keySet().retainAll(newHost.getApplications().keySet());
            addedApps.keySet().removeAll(oldHost.getApplications().keySet());
            deletedApps.keySet().removeAll(newHost.getApplications().keySet());

            for (Iterator j = addedApps.keySet().iterator(); j.hasNext();) {
                String name = (String) j.next();
                ApplicationInfoImpl app = (ApplicationInfoImpl) addedApps.get(name);
                if (!name.startsWith("/"))
                    name = "/" + name;
                addApplication(oldHost.getName() + name, app);
            }

            for (Iterator j = deletedApps.keySet().iterator(); j.hasNext();) {
                String name = (String) j.next();
                ApplicationInfoImpl app = (ApplicationInfoImpl) deletedApps.get(name);
                if (!name.startsWith("/"))
                    name = "/" + name;
                removeApplication(oldHost.getName() + name);
            }

            for (Iterator j = notChangedApps.keySet().iterator(); j.hasNext();) {
                String name = (String) j.next();
                ApplicationInfoImpl oldApp =
                        (ApplicationInfoImpl) oldHost.getApplications().get(name);
                ApplicationInfoImpl newApp =
                        (ApplicationInfoImpl) newHost.getApplications().get(name);
                if (!oldApp.getContainers().equals(newApp.getContainers())
                        || !oldApp.getProtocols().equals(newApp.getProtocols())) {
                    if (!name.startsWith("/"))
                        name = "/" + name;
                    removeApplication(oldHost.getName() + name);
                    addApplication(oldHost.getName() + name, newApp);
                }
            }

        }
    }


    private void reallyBindContainers(String application, HashSet containers,
                                      HashSet protocols)
            throws DeploymentException {
        if (application.endsWith("/"))
            application = application.substring(0, application.length() - 1);

        Attributes attrs = null;
        try {
            attrs = appContext.getAttributes(application);
        } catch (NamingException nex) {
            throw new DeploymentException("Couldn't bind containers \"" + containers
                    + "\" to application \"" + application
                    + "\"");
        }
        if (attrs == null)
            attrs = new BasicAttributes();

        Attribute protocolsAttribute = attrs.get("protocols");
        Attribute contextsAttribute = attrs.get("contexts");

        for (Iterator i = protocols.iterator(); i.hasNext();) {
            if (protocolsAttribute == null)
                protocolsAttribute = new BasicAttribute("protocols", (String) i.next());
            else
                protocolsAttribute.add((String) i.next());
        }

        for (Iterator i = containers.iterator(); i.hasNext();) {
            String containerName = (String) i.next();
            StrBuffer containerID = new StrBuffer(containerName.length());
            containerID.append(containerName);
            if (!containerID2number.containsKey(containerID))
                throw new DeploymentException("Couldn't bind container \""
                        + containerID.toString()
                        + "\" - doesn't registered");
            if (contextsAttribute == null)
                contextsAttribute = new BasicAttribute("contexts", containerID);
            else
                contextsAttribute.add(containerID);
        }

        if (protocolsAttribute != null)
            attrs.put(protocolsAttribute);
        if (contextsAttribute != null)
            attrs.put(contextsAttribute);

        try {
            appContext.modifyAttributes(application, DirContext.REPLACE_ATTRIBUTE,
                    attrs);
        } catch (NamingException nex) {
            throw new DeploymentException("Couldn't bind containers \"" + containers
                    + "\" to application \"" + application
                    + "\"");
        }
    }

    public void setContainerWorkListener(ContainerWorkListener listener) {
        this.listener = listener;
    }

    public void setClusterContext(DirContext newClusterContext) {
        clusterContext = newClusterContext;
    }

    public void checkContainers() throws DeploymentException {
        // set containers:
        logger.debug("check containers");
        HashMap containers = new HashMap();
        try {
            NamingEnumeration enum = clusterContext.list("components/containers");
            while (enum.hasMore()) {
                NameClassPair ncp = (NameClassPair) enum.next();
                String encContainerId = ncp.getName();
                String contId = JndiNameConverter.decode(encContainerId);
                try {
                    Attributes attrs =
                            clusterContext.getAttributes("components/containers/"
                            + encContainerId);
                    if (attrs == null) {
                        continue;
                    }
                    Attribute portAttr = attrs.get("port");
                    if (portAttr == null) {
                        continue;
                    }
                    int port = Integer.parseInt((String) portAttr.get());
                    Attribute aportAttr = attrs.get("admin-port");
                    if (aportAttr == null) {
                        continue;
                    }
                    int aport = Integer.parseInt((String) aportAttr.get());
                    Attribute hostAttr = attrs.get("ip");
                    if (hostAttr == null) {
                        continue;
                    }
                    String host = (String) hostAttr.get();
                    ContainerAddress addr = new ContainerAddress(host, port, aport);
                    containers.put(contId, addr);
                    logger.debug("Container \"" + contId + "\" on " + addr
                            + " added.");
                } catch (NumberFormatException e) {
                    logger.warn("Couldn't add container \"" + contId
                            + "\"to config: misformatted port or admin-port value",
                            e);
                } catch (NamingException e) {
                    logger.warn("Couldn't add container \"" + contId + "\"to config",
                            e);
                }
            }
        } catch (NamingException e) {
            logger.error("Couldn't get container infos from cluster context", e);
        }
        setContainers(containers);
        if (logProxy.isDebugEnabled()) {
            logger.debug("check containers result: " + containers);
        }
    }

    public void checkHosts() throws DeploymentException {
        try {
            logger.debug("check hosts");
            HashMap hosts = new HashMap();
            NamingEnumeration hostsEnum = clusterContext.list("components/hosts");
            while (hostsEnum.hasMore()) {
                NameClassPair hostNcp = (NameClassPair) hostsEnum.next();
                String hostName = hostNcp.getName();
                HashMap appsMap = null;
                try {
                    NamingEnumeration appsEnum = clusterContext.list("components/hosts/"
                            + hostName
                            + "/webapps");
                    appsMap = new HashMap();
                    while (appsEnum.hasMore()) {
                        NameClassPair appNcp = (NameClassPair) appsEnum.next();
                        String appName = '/' + appNcp.getName();
                        Attributes appAttrs =
                                clusterContext.getAttributes("components/hosts/" + hostName
                                + "/webapps" + appName
                                + "/config");
                        Attribute containersAttr = appAttrs.get("containers");
                        HashSet containersSet = new HashSet();
                        if (containersAttr != null) {
                            for (NamingEnumeration enum = containersAttr.getAll();
                                 enum.hasMore();) {
                                containersSet.add(enum.next());
                            }
                        }
                        Attribute protocolsAttr = appAttrs.get("protocols");
                        HashSet protocolsSet = new HashSet();
                        if (protocolsAttr != null) {
                            for (NamingEnumeration enum = protocolsAttr.getAll();
                                 enum.hasMore();) {
                                protocolsSet.add(enum.next());
                            }
                        } else {
                            protocolsSet.add("http");
                        }
                        ApplicationInfo appInfo = new ApplicationInfoImpl(true,
                                containersSet,
                                protocolsSet,
                                null);
                        appsMap.put(appName, appInfo);
                    }

                    VirtualHostInfo vhost
                            = new VirtualHostInfo(hostName, null, appsMap);

                    hosts.put(vhost.getName(), vhost);
                } catch (NamingException e) {
                    logger.error("Couldn't get virtual host \"" + hostName
                            + "\" config from cluster context", e);
                }
            }
            setHosts(hosts);
            if (logProxy.isDebugEnabled()) {
                logger.debug("check hosts result: " + hosts);
            }
        } catch (NamingException e) {
            logger.error("Couldn't get virtual hosts list", e);
        }
    }
}
