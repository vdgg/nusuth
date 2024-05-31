package com.azoft.nusuth.management;

import java.io.InputStream;
import java.rmi.RemoteException;

import com.azoft.nusuth.management.rmi.*;

public class GuiCallbackWrapper implements GuiCallback {
    private RmiGuiCallback callback;


    public GuiCallbackWrapper(RmiGuiCallback callback) {
        this.callback = callback;
    }


    private void processRemoteException(RemoteException rex) throws ManagementException {
        if (rex.detail instanceof RemoteException)
            rex = (RemoteException) rex.detail;
        if (rex.detail instanceof ManagementException) {
            throw (ManagementException) rex.detail;
        } else
            throw new ManagementException(rex.getMessage());
    }


    public void refreshApplicationDeployment() throws ManagementException {
        try {
            callback.refreshApplicationsDeployment();
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }


    public void refreshApplicationDeploymentErrors() throws ManagementException {
        try {
            callback.refreshApplicationDeploymentErrors();
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }


    public void refreshComponentSettings(String componentType, String componentId) throws ManagementException {
        try {
            callback.refreshComponentSettings(componentType, componentId);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }


    public void refreshComponentsList() throws ManagementException {
        try {
            callback.refreshComponentsList();
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }


    public void showComponentRegisterError(String componentType, String componentName, String errorMessage) throws ManagementException {
        try {
            callback.showComponentRegisterError(componentType, componentName, errorMessage);
        } catch (RemoteException rex) {
            processRemoteException(rex);
        }
    }
}