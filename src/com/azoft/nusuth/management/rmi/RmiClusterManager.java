/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.management.rmi;

import java.rmi.Remote;
import java.util.Vector;

import com.azoft.nusuth.management.*;
import com.azoft.nusuth.management.security.*;

import java.rmi.RemoteException;

public interface RmiClusterManager extends Remote {
    void addComponent(String sessionId, String host, int port) throws RemoteException;

    RmiApplicationInputStream getApplicationsDeployment(String sessionId) throws RemoteException;

    RmiApplicationInputStream getComponentSettings(String sessionId, String componentType, String componentId) throws RemoteException;

    ContainerState getContainerState(String sessionId, String containerId) throws RemoteException;

    DistributorState getDistributorState(String sessionId, String distributorId) throws RemoteException;

    Vector getRegisteredComponents(String sessionId) throws RemoteException;

    String login(String user, String encodedPassword) throws RemoteException;

    void removeComponent(String sessionId, ComponentInfo cinfo) throws RemoteException;

    void setApplicationsDeployment(String sessionId, RmiApplicationInputStream applicationsDeployment) throws RemoteException;

    void setComponentSettings(String sessionId, String componentType, String componentId, RmiApplicationInputStream componentSettings) throws RemoteException;

    RmiApplicationInputStream getApplicationsDeploymentErrors(String sessionId) throws RemoteException;

    void registerCallback(RmiGuiCallback gui) throws RemoteException;

    RmiApplicationInputStream getSecuritySettings(String sessionId) throws RemoteException;

    void setSecuritySettings(String sessionId, RmiApplicationInputStream newSettings) throws RemoteException;

    void reconnect(String sessionId, String componentType, String componentId) throws RemoteException;
}
