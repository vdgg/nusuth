package com.azoft.nusuth.management.rmi;

import com.azoft.nusuth.management.ComponentType;

import java.rmi.Remote;
import java.rmi.*;

import com.azoft.nusuth.deployment.*;

public interface RmiGuiCallback extends Remote {


    void refreshApplicationDeploymentErrors() throws RemoteException;


    void refreshApplicationsDeployment() throws RemoteException;


    void refreshComponentSettings(String componentType, String componentId) throws RemoteException;


    void refreshComponentsList() throws RemoteException;


    void showComponentRegisterError(String componentType, String componentName, String errorMessage) throws RemoteException;
}