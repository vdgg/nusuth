package com.azoft.nusuth.management.rmi;

import java.rmi.Remote;

import com.azoft.nusuth.management.*;
import com.azoft.nusuth.management.security.*;
import com.azoft.nusuth.deployment.*;

public interface RmiClusterManagerProxy {
    void addRegisteredComponent(ComponentInfo cinfo) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    void addRegisteredComponent(String host, int port) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    CompositeNusuthWebAppElement getApplicationsDeployment() throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    CompositeNusuthWebAppElement getComponentSettings(String componentType, String componentId) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    ContainerState getContainerState(String containerId) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    DistributorState getDistributorState(String distributorId) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    ComponentInfo[] getRegisteredComponents() throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    void login(String user, String encodedPassword) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    void removeRegisteredComponent(ComponentInfo cinfo) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    void setApplicationsDeployment(CompositeNusuthWebAppElement element) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    void setComponentSettings(String componentType, String componentId, CompositeNusuthWebAppElement componentSettings) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    void setServerLocation(String host, int port) throws ManagementException;

    public void addServerEventListener(ServerEventListener eventListener);

    public void removeServerEventListener(ServerEventListener eventListener);

    CompositeNusuthWebAppElement getSecuritySettings() throws UnauthorizedAccessException, ManagementException, AccessDeniedException;

    void setSecuritySettings(CompositeNusuthWebAppElement newSettings) throws UnauthorizedAccessException, ManagementException, AccessDeniedException;
}
