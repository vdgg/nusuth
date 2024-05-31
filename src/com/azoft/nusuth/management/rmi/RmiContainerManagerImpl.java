package com.azoft.nusuth.management.rmi;

import java.rmi.server.UnicastRemoteObject;

import com.azoft.nusuth.management.security.AdminPortListener;

import java.rmi.RemoteException;

import com.azoft.nusuth.management.*;
import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.io.InputStream;
import java.util.Vector;

public class RmiContainerManagerImpl
        extends UnicastRemoteObject
        implements RmiContainerManager {
    private ContainerManager containerManager;

    /**
     * Insert the method's description here. Creation date: (04.12.00 22:30:02)
     * @exception java.rmi.RemoteException The exception description.
     */
    public RmiContainerManagerImpl(ContainerManager manager) throws RemoteException {
        containerManager = manager;
    }

    public RmiApplicationInputStream getSettings() throws RemoteException {
        try {
            InputStream result = containerManager.getSettings();
            return new RmiApplicationInputStreamImpl(result);
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public ContainerState getState() throws RemoteException {
        try {
            return containerManager.getState();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void setSettings(RmiApplicationInputStream settings) throws RemoteException {
        try {
            containerManager.setSettings(new ApplicationInputStreamWrapper(settings));
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void startServer() throws RemoteException {
        try {
            containerManager.startServer();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void stopServer() throws RemoteException {
        try {
            containerManager.stopServer();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void addApplication(String virtualHost, String appUrl, RmiApplicationInputStream application) throws RemoteException {
        try {
            containerManager.addApplication(virtualHost, appUrl, new ApplicationInputStreamWrapper(application));
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public RmiApplicationInputStream getVirtualHosts() throws RemoteException {
        try {
            return new RmiApplicationInputStreamImpl(containerManager.getVirtualHosts());
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void patchApplication(String virtualHost, String appUrl, RmiApplicationInputStream patch, boolean overwrite) throws RemoteException {
        try {
            containerManager.patchApplication(virtualHost, appUrl, new ApplicationInputStreamWrapper(patch), overwrite);
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void removeApplication(String virtualHost, String appUrl) throws RemoteException {
        try {
            containerManager.removeApplication(virtualHost, appUrl);
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void replaceApplicationContent(String virtualHost, String appUrl, RmiApplicationInputStream content) throws RemoteException {
        try {
            containerManager.replaceApplicationContent(virtualHost, appUrl, new ApplicationInputStreamWrapper(content));
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void startApplication(String virtualHost, String appUrl) throws RemoteException {
        try {
            containerManager.startApplication(virtualHost, appUrl);
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void stopApplication(String virtualHost, String appUrl) throws RemoteException {
        try {
            containerManager.stopApplication(virtualHost, appUrl);
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public int getHttpPort()
            throws RemoteException {
        try {
            return containerManager.getHttpPort();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public DistributedJNDIContext getDistributedContext() throws RemoteException {
        try {
            return containerManager.getDistributedContext();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }
}
