package com.azoft.nusuth.management.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;

import com.azoft.nusuth.management.*;
import com.azoft.nusuth.management.security.*;
import com.azoft.nusuth.deployment.*;

import java.io.*;
import java.util.*;

public class RmiClusterManagerProxyImpl implements RmiClusterManagerProxy {

    private String host = "localhost";
    private int port = 1099;
    private RmiClusterManager manager = null;
    private String sessionId = "";

    private GuiCallbackImpl callback = new GuiCallbackImpl();

    public RmiClusterManagerProxyImpl() {
        NusuthAppConfigFactory.addEntityResolver("container", new ContainerEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("distributor", new DistributorEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("deployer", new DeployerEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("web-app", new WebEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("taglib", new JspEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("application-deployment", new ApplicationDeploymentEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("application-deployment-errors", new ApplicationDeploymentErrorsEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("securityConfig", new SecurityConfigEntityResolver());
    }

    public void addRegisteredComponent(ComponentInfo cinfo)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        addRegisteredComponent(cinfo.getHost(), cinfo.getAdminPort());
    }

    public void addRegisteredComponent(String host, int port)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        try {
            manager.addComponent(sessionId, host, port);
        } catch (RemoteException rex) {
            processException(rex);
        }
    }

    public CompositeNusuthWebAppElement getApplicationsDeployment()
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        try {
            CompositeNusuthWebAppElement result = NusuthAppConfigFactory.createConfig("application-deployment", new ApplicationInputStreamWrapper(manager.getApplicationsDeployment(sessionId)));
            //      CompositeNusuthWebAppElement errors = NusuthAppConfigFactory.createConfig("application-deployment-errors", new ApplicationInputStreamWrapper(manager.getApplicationsDeploymentErrors(sessionId)));
            //      System.out.println("gep applications deployment: OK\nErrors:\n"+errors.compose("application-deployment-errors", "application-deployment-errors.dtd")+'\n');
            return result;
        } catch (ParserException pex) {
            throw new ManagementException("bad config");
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    CompositeNusuthWebAppElement result = NusuthAppConfigFactory.createConfig("application-deployment", new ApplicationInputStreamWrapper(manager.getApplicationsDeployment(sessionId)));
                } catch (ParserException pex) {
                    throw new ManagementException("bad config");
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
            return null;
        }
    }


    public CompositeNusuthWebAppElement getComponentSettings(String componentType, String componentId)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        if (manager == null)
            throw new ManagementException("Not connected to Cluster Manager");
        if (sessionId == null)
            throw new UnauthorizedAccessException();
        RmiApplicationInputStream rmiistream = null;
        try {
            rmiistream = manager.getComponentSettings(sessionId, componentType, componentId);
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    rmiistream = manager.getComponentSettings(sessionId, componentType, componentId);
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
        }
        if (rmiistream == null)
            throw new ManagementException("bad component");
        try {
            return NusuthAppConfigFactory.createConfig(componentType, new ApplicationInputStreamWrapper(rmiistream));
        } catch (ParserException pex) {
            throw new ManagementException("Bad config, nested:" + pex.getMessage());
        }
    }


    public ContainerState getContainerState(String containerId)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        try {
            return manager.getContainerState(sessionId, containerId);
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    return manager.getContainerState(sessionId, containerId);
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
            return null;
        }
    }


    public DistributorState getDistributorState(String distributorId)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        try {
            return manager.getDistributorState(sessionId, distributorId);
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    return manager.getDistributorState(sessionId, distributorId);
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
            return null;
        }
    }


    public ComponentInfo[] getRegisteredComponents()
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        try {
            return (ComponentInfo[]) manager.getRegisteredComponents(sessionId).toArray(new ComponentInfo[0]);
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    return (ComponentInfo[]) manager.getRegisteredComponents(sessionId).toArray(new ComponentInfo[0]);
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
            return null;
        }
    }


    public void login(String user, String encodedPassword) throws UnauthorizedAccessException, ManagementException {
        try {
            sessionId = manager.login(user, encodedPassword);
        } catch (java.rmi.RemoteException rex) {
            if (rex.detail != null && rex.detail instanceof java.rmi.RemoteException)
                rex = (java.rmi.RemoteException) rex.detail;
            if (rex.detail instanceof ManagementException)
                throw (ManagementException) rex.detail;
            if (rex.detail instanceof UnauthorizedAccessException)
                throw (UnauthorizedAccessException) rex.detail;
            throw new ManagementException(rex.detail.getMessage());
        }
        if (sessionId == null)
            throw new UnauthorizedAccessException();
    }


    private void processException(RemoteException rex) throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        while (rex.detail instanceof RemoteException)
            rex = (RemoteException) rex.detail;
        if (rex.detail instanceof UnauthorizedAccessException)
            throw (UnauthorizedAccessException) rex.detail;
        if (rex.detail instanceof AccessDeniedException)
            throw (AccessDeniedException) rex.detail;
        if (rex.detail instanceof ManagementException)
            throw (ManagementException) rex.detail;
        throw new ManagementException(rex.getMessage());
    }


    public void removeRegisteredComponent(ComponentInfo cinfo)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        try {
            manager.removeComponent(sessionId, cinfo);
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    manager.removeComponent(sessionId, cinfo);
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
        }
    }


    public void setApplicationsDeployment(CompositeNusuthWebAppElement element)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        try {
            manager.setApplicationsDeployment(
                    sessionId,
                    new RmiApplicationInputStreamImpl(new ByteArrayInputStream(element.compose("application-deployment", "application_deployment.dtd").getBytes())));
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    manager.setApplicationsDeployment(
                            sessionId,
                            new RmiApplicationInputStreamImpl(new ByteArrayInputStream(element.compose("application-deployment", "application_deployment.dtd").getBytes())));
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
        }
    }


    public void setComponentSettings(String componentType, String componentId, CompositeNusuthWebAppElement componentSettings)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        String docType = "unknown";
        String systemId = "unknown.dtd";
        if (componentType.equalsIgnoreCase("container")) {
            docType = "container";
            systemId = "container.dtd";
        } else if (componentType.equalsIgnoreCase("distributor")) {
            docType = "distributor";
            systemId = "distributor.dtd";
        } else if (componentType.equalsIgnoreCase("web-app")) {
            docType = "web-app";
            systemId = "web.dtd";
        } else if (componentType.equalsIgnoreCase("deployer")) {
            docType = "deployer";
            systemId = "deployer.dtd";
        }
        try {
            RmiApplicationInputStream settings = new RmiApplicationInputStreamImpl(new ByteArrayInputStream(componentSettings.compose(docType, systemId).getBytes()));
            manager.setComponentSettings(sessionId, componentType, componentId, settings);
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    RmiApplicationInputStream settings = new RmiApplicationInputStreamImpl(new ByteArrayInputStream(componentSettings.compose(docType, systemId).getBytes()));
                    manager.setComponentSettings(sessionId, componentType, componentId, settings);
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
        }
    }


    public void setServerLocation(String host, int port) throws ManagementException {
        if (this.host != host || this.port != port || manager == null) {
            this.host = host;
            this.port = port;
            this.sessionId = "";
            String rmi_name = "rmi://" + host + ':' + port + "/Cluster Manager";
            try {
                manager = null;
                manager = (RmiClusterManager) java.rmi.Naming.lookup(rmi_name);
            } catch (java.rmi.RemoteException rex) {
                throw new ManagementException("Couldn't lookup cluster manager on \"" + rmi_name + '"');
            } catch (java.rmi.NotBoundException nbex) {
                throw new ManagementException("Couldn't lookup cluster manager on \"" + rmi_name + '"');
            } catch (java.net.MalformedURLException mfuex) {
                throw new ManagementException("Couldn't lookup cluster manager on \"" + rmi_name + '"');
            }
            if (manager == null)
                throw new ManagementException("Couldn't lookup cluster manager on \"" + rmi_name + '"');
            /*
            try {
              manager.registerCallback(new RmiGuiCallbackImpl(callback));
            } catch (RemoteException rex) {
              if (rex.detail instanceof RemoteException)
                rex = (RemoteException) rex.detail;
              if (rex.detail instanceof ManagementException) {
                throw (ManagementException) rex.detail;
              } else
                throw new ManagementException("Couldn't register callback, nested:" + rex.getMessage());
              System.err.println("Cannot register callback: "+rex);
            }
           */
        }
    }


    public void addServerEventListener(ServerEventListener eventListener) {
        callback.addEventListener(eventListener);
    }


    public void removeServerEventListener(ServerEventListener eventListener) {
        callback.removeEventListener(eventListener);
    }


    public CompositeNusuthWebAppElement getSecuritySettings()
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        if (manager == null)
            throw new ManagementException("Not connected to Cluster Manager");
        if (sessionId == null)
            throw new UnauthorizedAccessException();
        RmiApplicationInputStream rmiistream = null;
        try {
            rmiistream = manager.getSecuritySettings(sessionId);
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    rmiistream = manager.getSecuritySettings(sessionId);
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
        }
        if (rmiistream == null)
            throw new ManagementException("bad component");
        try {
            return NusuthAppConfigFactory.createConfig("securityConfig", new ApplicationInputStreamWrapper(rmiistream));
        } catch (ParserException pex) {
            throw new ManagementException("Bad config, nested:" + pex.getMessage());
        }
    }


    public void setSecuritySettings(CompositeNusuthWebAppElement newSettings)
            throws UnauthorizedAccessException, ManagementException, AccessDeniedException {
        try {
            RmiApplicationInputStream settings = new RmiApplicationInputStreamImpl(new ByteArrayInputStream(newSettings.compose("securityConfig", "security.dtd").getBytes()));
            manager.setSecuritySettings(sessionId, settings);
        } catch (RemoteException rex) {
            try {
                processException(rex);
            } catch (ComponentUnavailableException cuex) {
                try {
                    manager.reconnect(sessionId, cuex.getComponentType(), cuex.getComponentId());
                    RmiApplicationInputStream settings = new RmiApplicationInputStreamImpl(new ByteArrayInputStream(newSettings.compose("securityConfig", "security.dtd").getBytes()));
                    manager.setSecuritySettings(sessionId, settings);
                } catch (RemoteException rex2) {
                    processException(rex2);
                }
            }
        }
    }
}
