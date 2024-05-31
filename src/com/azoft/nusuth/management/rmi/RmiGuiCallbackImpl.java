package com.azoft.nusuth.management.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import com.azoft.nusuth.management.*;

public class RmiGuiCallbackImpl extends UnicastRemoteObject implements RmiGuiCallback {
    private GuiCallback callback;


    public RmiGuiCallbackImpl(GuiCallback callback) throws RemoteException {
        this.callback = callback;
    }


    public void refreshApplicationDeploymentErrors() throws RemoteException {
        try {
            callback.refreshApplicationDeploymentErrors();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public void refreshApplicationsDeployment() throws RemoteException {
        try {
            callback.refreshApplicationDeployment();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public void refreshComponentSettings(String componentType, String componentId) throws RemoteException {
        try {
            callback.refreshComponentSettings(componentType, componentId);
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public void refreshComponentsList() throws RemoteException {
        try {
            callback.refreshComponentsList();
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }


    public void showComponentRegisterError(String componentType, String componentName, String errorMessage) throws RemoteException {
        try {
            callback.showComponentRegisterError(componentType, componentName, errorMessage);
        } catch (ManagementException mex) {
            throw new RemoteException(mex.getMessage(), mex);
        }
    }
}