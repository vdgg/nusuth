package com.azoft.nusuth.management;

import java.util.*;

import sun.java2d.loops.*;

public interface ServerEventListener extends EventListener {


    void refreshApplicationDeploymentErrors();


    void refreshApplicationsDeployment();


    void refreshComponentSettings(String componentType, String componentId);


    void refreshComponentsList();


    void showComponentRegisterError(String componentType, String componentId, String errorMessage);
}