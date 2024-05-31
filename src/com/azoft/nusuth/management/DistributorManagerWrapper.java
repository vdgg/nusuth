package com.azoft.nusuth.management;

import java.util.Map;
import java.io.InputStream;
import java.rmi.RemoteException;

import com.azoft.nusuth.management.rmi.RmiDistributorManager;
import com.azoft.nusuth.management.rmi.RmiApplicationInputStreamImpl;
import com.azoft.nusuth.jndi.DistributedJNDIContext;

public class DistributorManagerWrapper
        extends ComponentManagerWrapper
        implements DistributorManager {
    private RmiDistributorManager manager;

    public DistributorManagerWrapper(RmiDistributorManager manager, String distributorId) {
        super();
        this.manager = manager;
        componentId = distributorId;
    }

    public InputStream getApplicationsDeployment() throws ManagementException {
        try {
            return new ApplicationInputStreamWrapper(manager.getApplicationsDeployment());
        } catch (RemoteException rex) {
            processRemoteException(rex);
            return null;
        }
    }

    public InputStream getSettings() throws ManagementException {
        try {
            return new ApplicationInputStreamWrapper(manager.getSettings());
        } catch (RemoteException rex) {
            processRemoteException(rex);
            return null;
        }
    }

    public DistributorState getState() throws ManagementException {
        try {
            return manager.getState();
        } catch (RemoteException rex) {
            processRemoteException(rex);
            return null;
        }
    }

    public void setApplicationsDeployment(InputStream apps) throws ManagementException {
        try {
            manager.setApplicationsDeployment(new RmiApplicationInputStreamImpl(apps));
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public void setSettings(InputStream settings) throws ManagementException {
        try {
            manager.setSettings(new RmiApplicationInputStreamImpl(settings));
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public void startServer() throws ManagementException {
        try {
            manager.startServer();
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public void stopServer() throws ManagementException {
        try {
            manager.stopServer();
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public String getComponentType() {
        return "distributor";
    }

    public void setContainers(Map newContainers)
            throws ManagementException {
        try {
            manager.setContainers(newContainers);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public DistributedJNDIContext getDistributedContext()
            throws ManagementException {
        try {
            return manager.getDistributedContext();
        } catch (RemoteException rex) {
            processRemoteException(rex);
            return null;
        }
    }
}
