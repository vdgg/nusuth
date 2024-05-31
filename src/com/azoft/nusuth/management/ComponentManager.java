package com.azoft.nusuth.management;

import com.azoft.nusuth.jndi.DistributedJNDIContext;

import java.io.InputStream;

public interface ComponentManager {
    InputStream getSettings() throws ManagementException;

    void setSettings(InputStream settings) throws ManagementException;

    String getComponentType();

    String getComponentId();

    DistributedJNDIContext getDistributedContext() throws ManagementException;
}
