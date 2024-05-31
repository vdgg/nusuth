package com.azoft.nusuth.management;

import java.io.InputStream;
import java.util.Vector;

import com.azoft.nusuth.core.LocalContainer;

public interface ContainerManager
        extends ComponentManager {
    /** @link dependency */

    /*#VirtualHostInfo lnkVirtualHostInfo;*/

    void addApplication(String virtualHost, String appUrl, InputStream application) throws ManagementException;

    public ContainerState getState() throws ManagementException;

    void patchApplication(String virtualHost, String appUrl, InputStream patch, boolean overwrite) throws ManagementException;

    void replaceApplicationContent(String virtualHost, String appUrl, InputStream content) throws ManagementException;

    void startApplication(String virtualHost, String appUrl) throws ManagementException;

    void startServer() throws ManagementException;

    void stopApplication(String virtualHost, String appUrl) throws ManagementException;

    void stopServer() throws ManagementException;

    InputStream getVirtualHosts() throws ManagementException;

    void removeApplication(String virtualHost, String appUrl) throws ManagementException;

    void fullShutdown();

    int getHttpPort() throws ManagementException;

    public LocalContainer getLocalContainer();

    public void setDistributorIdToContexts(String id);

}
