package com.azoft.nusuth.management;

import java.io.Serializable;

import com.azoft.nusuth.deployment.*;

public class ComponentInfo implements java.io.Serializable {
    private int adminPort;
    private String componentId;
    private ComponentType componentType;
    private String host;

    // specially for GUI
    public ComponentInfo(String componentId, ComponentType componentType) throws NullPointerException {
        if (componentType == null)
            throw new NullPointerException("Unknown component type");
        this.adminPort = -1;
        this.componentId = componentId;
        this.componentType = componentType;
        this.host = "localhost";
    }

    // specially for GUI
    public ComponentInfo(String host, int adminPort) throws NullPointerException {
        if (host == null || host.length() == 0 || adminPort < 1)
            throw new IllegalArgumentException("Unknown host or port < 1");

        this.adminPort = adminPort;
        this.componentId = null;
        this.componentType = null;
        this.host = host;
    }

    public int getAdminPort() {
        return adminPort;
    }

    public String getComponentId() {
        return componentId;
    }

    protected ComponentType getComponentType() {
        return componentType;
    }

    public String getComponentTypeName() {
        return componentType.toString();
    }

    public java.lang.String getHost() {
        return host;
    }

    protected void setAdminPort(int newAdminPort) {
        adminPort = newAdminPort;
    }

    protected void setComponentId(java.lang.String newComponentId) {
        componentId = newComponentId;
    }

    protected void setComponentType(ComponentType newComponentType) {
        componentType = newComponentType;
    }

    protected void setHost(java.lang.String newHost) {
        host = newHost;
    }

    public ComponentInfo(int adminPort, String componentId, ComponentType componentType, String host) throws NullPointerException {
        if (componentType == null)
            throw new NullPointerException("Unknown component type");
        this.adminPort = adminPort;
        this.componentId = componentId;
        this.componentType = componentType;
        this.host = host;
    }

    public final void addCompositeChild(CompositeNusuthWebAppElement node) throws DeploymentException {
        CompositeNusuthWebAppElement componentNode = node.addCompositeChild("component");
        componentNode.setSimpleChild("name").setContent(componentId);
        componentNode.setSimpleChild("type").setContent(componentType.toString());
        componentNode.setSimpleChild("host-name").setContent(host);
        componentNode.setSimpleChild("port").setContent("" + adminPort);
    }

    public boolean equals(Object obj) {
        if (obj instanceof ComponentInfo) {
            ComponentInfo o = (ComponentInfo) obj;
            return
                    adminPort == o.adminPort
                    && componentId != null && componentId.equals(o.componentId)
                    && componentType != null && componentType.equals(o.componentType)
                    && host != null && host.equals(o.host);
        } else
            return false;
    }

    public int hashCode() {
        return (componentType + componentId).hashCode();
    }
}
