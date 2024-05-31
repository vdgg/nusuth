package com.azoft.nusuth.management.rmi;

import java.rmi.server.UnicastRemoteObject;

import com.azoft.nusuth.management.*;

import java.rmi.RemoteException;

/**
 * Implementation of RmiClusterController interface
 * @author vdgg, igork
 * @since Nusuth1.0
 * @version 1.7
 */
public class RmiClusterControllerImpl
        extends UnicastRemoteObject
        implements RmiClusterController {

    /**
     * @associates <{com.azoft.nusuth.management.ClusterManager}>
     * @link aggregationByValue
     * @supplierCardinality 1
     */
    private ClusterManager clusterManager;


    /**
     * Insert the method's description here. Creation date: (04.12.00 22:34:13)
     * @exception java.rmi.RemoteException The exception description.
     */
    public RmiClusterControllerImpl(ClusterManager manager) throws RemoteException {
        clusterManager = manager;
    }


    /**
     * getContainerStub method comment.
     */
    public RmiContainerManager getContainerStub(String deployerId,
                                                String containerId)
            throws RemoteException {

        try {
            return new RmiContainerManagerImpl(
                    clusterManager.getContainerStub(deployerId, containerId));
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public synchronized void registerInvokedComponent(String host, int port,
                                                      String componentId,
                                                      RmiComponentManager manager)
            throws RemoteException {
        System.out.print("Register component from \"" + host + ':' + port
                + "\"... ");
        ComponentManager wrapper;
        if (manager instanceof RmiContainerManager)
            wrapper = new ContainerManagerWrapper((RmiContainerManager) manager,
                    componentId);
        else if (manager instanceof RmiDistributorManager)
            wrapper = new DistributorManagerWrapper((RmiDistributorManager) manager,
                    componentId);
        else if (manager instanceof RmiDeployer)
            wrapper = new DeployerWrapper((RmiDeployer) manager, componentId);
        else {
            System.out.println("failed - unknown component type");
            return;
        }

        try {
            clusterManager.registerInvokedComponent(host, port, wrapper);
        } catch (Exception mex) {
            System.out.println("failed");
            throw new RemoteException("", mex);
        }
        System.out.println("Ok");
    }

}
