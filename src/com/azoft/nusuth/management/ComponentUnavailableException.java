/**
 * ComponentUnavailableException.java
 *
 * @author Created by Omnicore CodeGuide
 */

package com.azoft.nusuth.management;

public class ComponentUnavailableException extends ManagementException {
    private String componentType;
    private String componentId;

    public ComponentUnavailableException(String componentType, String componentId, String message) {
        super(message);
        this.componentType = componentType;
        this.componentId = componentId;
    }

    public String getComponentType() {
        return componentType;
    }

    public String getComponentId() {
        return componentId;
    }
}

