package com.azoft.nusuth.management.rmi;

import java.rmi.Remote;

import com.azoft.nusuth.management.*;

import java.rmi.RemoteException;

/**
 * Remote cluster controller.
 * @author vdgg, igork
 * @version 1.5
 * @since Nusuth1.0
 */
public interface RmiClusterController extends Remote {
    /**
     * @return com.azoft.nusuth.management.rmi.RmiContainerManager
     * @param deployerId String Caller deployer ID
     * @param containerId String Needed cintainer ID
     */
    RmiContainerManager getContainerStub(String deployerId, String containerId)
            throws RemoteException;

    void registerInvokedComponent(String host, int port, String componentId,
                                  RmiComponentManager manager)
            throws RemoteException;
}
