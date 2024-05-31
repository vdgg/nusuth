package com.azoft.nusuth.management;

import java.util.Vector;
import java.rmi.RemoteException;

import com.azoft.nusuth.management.rmi.RmiContainerManager;
import com.azoft.nusuth.management.rmi.RmiApplicationInputStream;
import com.azoft.nusuth.management.rmi.RmiApplicationInputStreamImpl;
import com.azoft.nusuth.core.LocalContainer;
import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.io.*;

public class ContainerManagerWrapper
        extends ComponentManagerWrapper
        implements ContainerManager {
    private RmiContainerManager manager;

    public ContainerManagerWrapper(RmiContainerManager manager, String containerId) {
        super();
        this.manager = manager;
        componentId = containerId;
    }

    public void addApplication(String virtualHost, String appUrl, InputStream application) throws ManagementException {
        try {
            manager.addApplication(virtualHost, appUrl, new RmiApplicationInputStreamImpl(application));
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public InputStream getSettings() throws ManagementException {
        try {
            RmiApplicationInputStream stream = manager.getSettings();
            return new ApplicationInputStreamWrapper(stream);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
        return null;
    }

    public ContainerState getState() throws ManagementException {
        try {
            return manager.getState();
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
        return null;
    }

    public InputStream getVirtualHosts() throws ManagementException {
        try {
            return new ApplicationInputStreamWrapper(manager.getVirtualHosts());
        } catch (RemoteException rex) {
            processRemoteException(rex);
            return null;
        }
    }

    public void patchApplication(String virtualHost, String appUrl, InputStream patch, boolean overwrite) throws ManagementException {
        try {
            manager.patchApplication(virtualHost, appUrl, new RmiApplicationInputStreamImpl(patch), overwrite);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public void removeApplication(String virtualHost, String appUrl) throws ManagementException {
        try {
            manager.removeApplication(virtualHost, appUrl);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public void replaceApplicationContent(String virtualHost, String appUrl, InputStream content) throws ManagementException {
        try {
            manager.replaceApplicationContent(virtualHost, appUrl, new RmiApplicationInputStreamImpl(content));
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

    public void startApplication(String virtualHost, String appUrl) throws ManagementException {
        try {
            manager.startApplication(virtualHost, appUrl);
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

    public void stopApplication(String virtualHost, String appUrl) throws ManagementException {
        try {
            manager.stopApplication(virtualHost, appUrl);
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
        return "container";
    }

    public void fullShutdown() {
    }

    public int getHttpPort()
            throws ManagementException {
        try {
            return manager.getHttpPort();
        } catch (RemoteException rex) {
            processRemoteException(rex);
            return -1;
        }
    }

    public LocalContainer getLocalContainer() {
        return null;
    }

    public void setDistributorIdToContexts(String id) {
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
