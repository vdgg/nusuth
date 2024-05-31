package com.azoft.nusuth.management;

import com.azoft.nusuth.deployment.ParserException;
import com.azoft.nusuth.deployment.NusuthAppConfigFactory;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

import java.io.InputStream;
import java.util.*;

public class GuiCallbackImpl implements GuiCallback {
    private List listeners = new Vector();


    public void addEventListener(ServerEventListener eventListener) {
        listeners.add(eventListener);
    }


    public void refreshApplicationDeployment() {
        System.out.println("GUI CallBack: refreshApplicationsDeployment");
        for (Iterator i = listeners.iterator(); i.hasNext();)
            ((ServerEventListener) i.next()).refreshApplicationsDeployment();
    }


    public void refreshApplicationDeploymentErrors() {
        System.out.println("GUI CallBack: refreshApplicationsDeploymentErrors");
        for (Iterator i = listeners.iterator(); i.hasNext();)
            ((ServerEventListener) i.next()).refreshApplicationDeploymentErrors();
    }


    public void refreshComponentSettings(String componentType, String componentId) {
        System.out.println("GUI CallBack: refreshComponentSettings(\"" + componentType + "\", \"" + componentId + "\")");
        for (Iterator i = listeners.iterator(); i.hasNext();)
            ((ServerEventListener) i.next()).refreshComponentSettings(componentType, componentId);
    }


    public void refreshComponentsList() {
        System.out.println("GUI CallBack: refreshComponentsList");
        for (Iterator i = listeners.iterator(); i.hasNext();)
            ((ServerEventListener) i.next()).refreshComponentsList();
    }


    public void removeEventListener(ServerEventListener eventListener) {
        listeners.remove(eventListener);
    }


    public void showComponentRegisterError(String componentType, String componentName, String errorMessage) {
        System.out.println("GUI CallBack: showComponentRegisterError(\"" + componentType + "\", \"" + componentName + "\", \"" + errorMessage + "\")");
        for (Iterator i = listeners.iterator(); i.hasNext();)
            ((ServerEventListener) i.next()).showComponentRegisterError(componentType, componentName, errorMessage);
    }
}