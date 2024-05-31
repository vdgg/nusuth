package com.azoft.nusuth.management.rmi;

import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiComponentManager
        extends Remote {
    RmiApplicationInputStream getSettings() throws RemoteException;

    void setSettings(RmiApplicationInputStream settings) throws RemoteException;

    DistributedJNDIContext getDistributedContext() throws RemoteException;
}

