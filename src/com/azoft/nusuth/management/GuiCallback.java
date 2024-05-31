package com.azoft.nusuth.management;

public interface GuiCallback {


    void refreshApplicationDeployment() throws ManagementException;


    void refreshApplicationDeploymentErrors() throws ManagementException;


    void refreshComponentSettings(String componentType, String componentId) throws ManagementException;


    void refreshComponentsList() throws ManagementException;


    void showComponentRegisterError(String componentType, String componentName, String errorMessage) throws ManagementException;
}