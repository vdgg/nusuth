package com.azoft.nusuth.management;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

import java.io.InputStream;
import java.util.Map;

public interface DistributorManager
        extends ComponentManager {
    public InputStream getApplicationsDeployment() throws ManagementException;

    public DistributorState getState() throws ManagementException;

    public void setApplicationsDeployment(InputStream element) throws ManagementException;

    public void startServer() throws ManagementException;

    public void stopServer() throws ManagementException;

    public void setContainers(Map newContainers) throws ManagementException;
}
