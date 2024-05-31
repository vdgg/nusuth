package com.azoft.nusuth.management.security;

import org.apache.log4j.Category;

import com.azoft.nusuth.core.ComponentDistributionListener;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.jidep.*;
import com.azoft.nusuth.management.*;
import com.azoft.nusuth.management.rmi.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.*;
import java.util.Hashtable;

/**
 * This class intend for admin purposes.
 * @author vdgg, igork, skilz
 * @since Nusuth1.0
 * @version 1.14
 */
public class AdminPortListener
        extends Thread
        implements com.azoft.nusuth.management.Manageable {
    private Remote componentStub;
    private int port = -1;
    private String componentId;
    private String authKey = null;
    private RmiClusterController clusterController = null;
    private CompositeNusuthWebAppElement keystoreNode;
    private ComponentManager component;
    private byte[] addr;
    private boolean checked = false;
    private Category logger = Category.getInstance(this.getClass());
    private Hashtable listeners = new Hashtable();

    public AdminPortListener(CompositeNusuthWebAppElement settings,
                             ComponentManager componentManager)
            throws ManagementException {
        this.setDaemon(true);
        this.setName("Admin port listener");
        component = componentManager;

        Remote stub = null;
        try {
            if (componentManager instanceof ContainerManager)
                stub = new RmiContainerManagerImpl((ContainerManager) componentManager);
            else if (componentManager instanceof DistributorManager)
                stub = new RmiDistributorManagerImpl(
                        (DistributorManager) componentManager);
            else if (componentManager instanceof Deployer)
                stub = new RmiDeployerImpl((Deployer) componentManager);
        } catch (RemoteException rex) {
            if (rex.detail instanceof RemoteException)
                rex = (RemoteException) rex.detail;
            if (rex.detail instanceof ManagementException) {
                logger.error("Couldn't create admin port listener for \""
                        + componentId + "\"", rex.detail);
                throw (ManagementException) rex.detail;
            } else {
                logger.error("Couldn't create admin port listener for \""
                        + componentId + "\"", rex.detail);
                throw new ManagementException("Can't create admin port listener for \""
                        + componentId + "\", nested:"
                        + rex.detail.getMessage());
            }
        }
        this.componentStub = stub;

        try {
            CompositeNusuthWebAppElement managerNode
                    = ManagementUtil.getCompositeElement(settings, "manager");
            port = ManagementUtil.getSimpleInt(managerNode, "port");
            applySettings(settings);
        } catch (DeploymentException dex) {
            logger.error("Couldn't create admin port listener for \""
                    + componentId + "\"", dex);
            throw new ManagementException(dex.getMessage());
        }
    }

    /**
     * This method invokes then received request to invoke component
     * @param istream Stream that contains address.
     */
    public synchronized void acceptInvokeRequest(InputStream istream) {
        try {
            ObjectInputStream input = new ObjectInputStream(istream);
            addr = new byte[input.readInt()];
            input.read(addr);
            checked = true;
        } catch (IOException ioex) {
            logger.error("Accept invoke request failed", ioex);
            System.out.println("Accept invoke request failed:");
            ioex.printStackTrace();
        }
    }

    public synchronized void registerComponent() {
        if (checked) {
            registerComponent(new String(addr));
        }
    }

    public final synchronized ContainerManager
            getContainerStub(String deployerId, String containerId) {
        logger.debug("Get container \"" + containerId + "\" stub by deployer \""
                + deployerId + "\"");
        if (clusterController != null) {
            try {
                RmiContainerManager stub
                        = clusterController.getContainerStub(deployerId, containerId);
                if (stub != null)
                    return new ContainerManagerWrapper(stub, deployerId);
                else
                    return null;
            } catch (Exception ex) {
                logger.error("Couldn't get container \"" + containerId
                        + "\" stub by deployer \"" + deployerId + "\"", ex);
                System.out.println("Couldn't get container stub, nested:"
                        + ex.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public ComponentManager getComponentManager() {
        return component;
    }

    /**
     * This method register given component.
     * @param address Address of component.
     */
    private void registerComponent(String address) {
        logger.info("Registering component on \"" + address + '"');
        System.out.println("Registering component");

        try {
            clusterController = (RmiClusterController) Naming.lookup(address);
            clusterController.registerInvokedComponent(
                    java.net.InetAddress.getLocalHost().getHostAddress(),
                    port, componentId, (RmiComponentManager) componentStub);
        } catch (NotBoundException nbex) {
            logger.error("Couldn't register component on \"" + address + "\"", nbex);
            nbex.printStackTrace();
        } catch (java.net.MalformedURLException mfuex) {
            logger.error("Couldn't register component on \"" + address + "\"", mfuex);
            mfuex.printStackTrace();
        } catch (java.net.UnknownHostException uhex) {
            logger.error("Couldn't register component on \"" + address + "\"", uhex);
            uhex.printStackTrace();
        } catch (RemoteException rex) {
            logger.error("Couldn't register component on \"" + address + "\"", rex);
            rex.printStackTrace();
        }
    }

    public final void run() {
        try {
            ServerSocket socket = new ServerSocket(port);
//      Socket client = null;
            socket.setSoTimeout(100);
            logger.info("Admin port listener started at port " + port);
            System.out.println("Admin port listener started at port " + port);

            do {
                try {
                    Socket client = null;
                    client = socket.accept();
                    client.setSoTimeout(0);
                    (new JidepRequestHandler(JidepProtocolAdapter.getServerSide(client),
                            getComponentManager(),
                            JidepConnectionFactory.getKey(),
                            this)).start();
                } catch (InterruptedIOException iioex) {
                    // server socket accept timeout - nothing to do
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } while (!isInterrupted());
            socket.close();
            logger.info("Admin port listener stopped at port " + port);
            System.out.println("Admin port listener stopped at port " + port);
        } catch (IOException ioex) {
            logger.error("Admin port listener stopped at port " + port, ioex);
            ioex.printStackTrace();
        }
    }

    public void
            registerComponentDistributionListener(String path,
                                                  ComponentDistributionListener
            listener) {
        listeners.put(path, listener);
    }

    public synchronized void
            applySettings(CompositeNusuthWebAppElement newSettings)
            throws DeploymentException {
        logger.info("Applying new settings");
        this.componentId = ManagementUtil.getSimpleString(newSettings, "name");
        CompositeNusuthWebAppElement newManagerNode
                = ManagementUtil.getCompositeElement(newSettings, "manager");
        this.authKey = ManagementUtil.getSimpleString(newManagerNode, "auth-key");
        JidepConnectionFactory.setKey(authKey);
        if (port != ManagementUtil.getSimpleInt(newManagerNode, "port"))
            throw new DeploymentException("Need restart to apply settings");
    }

    public synchronized boolean
            isRestartNeeded(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        return ManagementUtil.getSimpleInt(
                ManagementUtil.getCompositeElement(settings, "manager"), "port")
                != port;
    }

    public synchronized final void stopListener() {
        logger.info("Stopping admin port listener");
        if (this.isAlive())
            this.interrupt();
    }

    public int getPort() {
        return port;
    }
}
