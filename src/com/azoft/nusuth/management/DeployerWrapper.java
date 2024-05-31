package com.azoft.nusuth.management;

import java.io.InputStream;
import java.rmi.RemoteException;

import com.azoft.nusuth.management.rmi.RmiDeployer;
import com.azoft.nusuth.management.rmi.RmiApplicationInputStreamImpl;
import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.util.Vector;

public class DeployerWrapper extends ComponentManagerWrapper implements Deployer {
    private RmiDeployer manager;

    public DeployerWrapper(RmiDeployer manager, String deployerId) {
        super();
        this.manager = manager;
        componentId = deployerId;
    }

    public InputStream getSettings() throws ManagementException {
        try {
            return new ApplicationInputStreamWrapper(manager.getSettings());
        } catch (RemoteException rex) {
            processRemoteException(rex);
            return null;
        }
    }

    public void setSettings(InputStream settings) throws ManagementException {
        try {
            manager.setSettings(new RmiApplicationInputStreamImpl(settings));
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public void addApplication(Vector hosts) throws ManagementException {
        try {
            manager.addApplication(hosts);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public InputStream getWebInf(String docBase, String location) throws ManagementException {
        try {
            return new ApplicationInputStreamWrapper(manager.getWebInf(docBase, location));
        } catch (RemoteException rex) {
            processRemoteException(rex);
            return null;
        }
    }

    public void patchApplication(Vector hosts, boolean overwrite) throws ManagementException {
        try {
            manager.patchApplication(hosts, overwrite);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public void replaceContent(Vector hosts) throws ManagementException {
        try {
            manager.replaceContent(hosts);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }

    public String getComponentType() {
        return "deployer";
    }

    public DistributedJNDIContext getDistributedContext() {
        return null;
    }
}
