/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.management.rmi;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

import java.util.Map;
import java.rmi.Remote;

import com.azoft.nusuth.management.DistributorState;

import java.rmi.RemoteException;

public interface RmiDistributorManager
        extends RmiComponentManager {
    public RmiApplicationInputStream getApplicationsDeployment() throws RemoteException;

    public DistributorState getState() throws RemoteException;

    public void setApplicationsDeployment(RmiApplicationInputStream apps) throws RemoteException;

    public void startServer() throws RemoteException;

    public void stopServer() throws RemoteException;

    public void setContainers(Map newContainers) throws RemoteException;
}
