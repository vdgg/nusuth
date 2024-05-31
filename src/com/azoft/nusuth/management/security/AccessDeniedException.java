package com.azoft.nusuth.management.security;

import com.azoft.nusuth.management.ManagementException;
import org.apache.log4j.Category;

public class AccessDeniedException extends ManagementException {
    private String componentType;
    private String componentId;
    private String permission;
    private Category logger = Category.getInstance(this.getClass());

    public AccessDeniedException(String componentType, String componentId, String permission) {
        super("Access denied: \"" + permission + "\" permission needed to access \"" + componentId + "\" " + componentType);
        this.componentType = componentType;
        this.componentId = componentId;
        this.permission = permission;
        logger.warn("Access denied: \"" + permission + "\" permission needed to access \"" + componentId + "\" " + componentType);
    }

    public String getComponentType() {
        return componentType;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getPermission() {
        return permission;
    }
}

