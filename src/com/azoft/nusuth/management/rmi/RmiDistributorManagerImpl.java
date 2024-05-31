package com.azoft.nusuth.management.rmi;

import java.io.ByteArrayInputStream;
import java.util.Map;

import com.azoft.nusuth.management.security.AdminPortListener;

import java.rmi.server.*;

import com.azoft.nusuth.management.*;
import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.rmi.RemoteException;

public class RmiDistributorManagerImpl
        extends UnicastRemoteObject
        implements RmiDistributorManager {
    /**
     * @link aggregationByValue
     * @directed
     * @supplierCardinality 1
     */
    private DistributorManager distributorManager;


    /**
     * Insert the method's description here. Creation date: (04.12.00 22:24:30)
     * @exception java.rmi.RemoteException The exception description.
     */
    public RmiDistributorManagerImpl(DistributorManager manager) throws RemoteException {
        distributorManager = manager;
    }


    public RmiApplicationInputStream getSettings() throws RemoteException {
        try {
            return new RmiApplicationInputStreamImpl(distributorManager.getSettings());
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public DistributorState getState() throws RemoteException {
        try {
            return distributorManager.getState();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RemoteException("", ex);
        }
    }


    public void setSettings(RmiApplicationInputStream settings) throws RemoteException {
        try {
            distributorManager.setSettings(new ApplicationInputStreamWrapper(settings));
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public void startServer() throws RemoteException {
        try {
            distributorManager.startServer();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public void stopServer() throws RemoteException {
        try {
            distributorManager.stopServer();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public RmiApplicationInputStream getApplicationsDeployment() throws java.rmi.RemoteException {
        try {
            return new RmiApplicationInputStreamImpl(distributorManager.getApplicationsDeployment());
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public void setApplicationsDeployment(RmiApplicationInputStream apps) throws java.rmi.RemoteException {
        try {
            distributorManager.setApplicationsDeployment(new ApplicationInputStreamWrapper(apps));
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public void setContainers(Map newContainers)
            throws RemoteException {
        try {
            distributorManager.setContainers(newContainers);
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }

    public DistributedJNDIContext getDistributedContext() throws RemoteException {
        try {
            return distributorManager.getDistributedContext();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }
}
