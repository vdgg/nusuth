package com.azoft.nusuth.management;

import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.rmi.*;
import java.net.ConnectException;

public abstract class ComponentManagerWrapper implements ComponentManager {
    protected String componentId;

    protected void processRemoteException(RemoteException rex) throws ComponentUnavailableException, ManagementException {
        if (rex.detail instanceof RemoteException)
            rex = (RemoteException) rex.detail;

        if (rex.detail instanceof ManagementException)
            throw (ManagementException) rex.detail;

        if (rex.detail instanceof ConnectException)
            throw new ComponentUnavailableException(getComponentType(), getComponentId(), "Connection to component is broken");

        throw new ManagementException(rex.getMessage());
    }

    public String getComponentId() {
        return componentId;
    }

    public DistributedJNDIContext getDistributedContext()
            throws ManagementException {
        return null;
    }
}
