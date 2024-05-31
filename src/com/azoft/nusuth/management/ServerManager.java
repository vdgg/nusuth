package com.azoft.nusuth.management;

import java.io.*;
import java.security.KeyStore;

import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;

import java.util.*;

import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.server.*;
import com.azoft.nusuth.distributor.DistributorRequestHandler;
import org.apache.log4j.Category;

public class ServerManager
        implements Manageable {
    private CompositeNusuthWebAppElement settings;
    private NusuthTcpServer tcpServer;
    private NusuthSslServer sslServer;
    private Category logger = Category.getInstance(this.getClass());


    public ServerManager(CompositeNusuthWebAppElement settings)
            throws ManagementException, DeploymentException {
        logger.info("Server manager creating");
        this.settings = settings;
        applySettings(settings);
    }


    public ServerState getState() {
        logger.debug("Get settings");
        ServerState state = new ServerState();
        state.setActiveHandlers((tcpServer != null ? tcpServer.getNowActive() : 0) + (sslServer != null ? sslServer.getNowActive() : 0));
        state.setTotalHandlers((tcpServer != null ? tcpServer.getNowHandlers() : 0) + (sslServer != null ? sslServer.getNowHandlers() : 0));
        state.setActiveKeepAlives((tcpServer != null ? tcpServer.getNowKeepalive() : 0) + (sslServer != null ? sslServer.getNowKeepalive() : 0));

        return state;
    }


    public void startServer()
            throws DeploymentException {
        logger.info("Server manager starting");
        if (settings != null)
            applySettings(settings);
    }


    public void stopServer() {
        logger.info("Server manager stopping");
        if (tcpServer != null) {
            tcpServer.stopServer();
            tcpServer = null;
        }
        if (sslServer != null) {
            sslServer.stopServer();
            sslServer = null;
        }
    }


    public void applySettings(CompositeNusuthWebAppElement newSettings)
            throws DeploymentException {
        logger.info("Apply new settings");
        try {
            String protocolAdapterName = ManagementUtil.getSimpleString(newSettings, "protocol-adapter");
            if (protocolAdapterName.length() == 0)
                protocolAdapterName = "com.azoft.nusuth.distributor.HttpDistributorRequestAdapter";
            DistributorRequestHandler.setAdapterClass(Class.forName(protocolAdapterName));
        } catch (ClassNotFoundException cnfex) {
            logger.error("Coudn't create TCP Server: adapter class \"" + ManagementUtil.getSimpleString(newSettings, "protoco-adapter") + "\" not found", cnfex);
            throw new DeploymentException("Coudn't create TCP Server: adapter class \"" + ManagementUtil.getSimpleString(newSettings, "protoco-adapter") + "\" not found");
        } catch (ClassCastException ccex) {
            logger.error("Coudn't create TCP Server: adapter class \""
                    + ManagementUtil.getSimpleString(newSettings, "protocol-adapter")
                    + "\" must be inherited from \"com.truport.nusuth.distributor.DistributorRequestAdapter\"", ccex);
            throw new DeploymentException(
                    "Coudn't create TCP Server: adapter class \""
                    + ManagementUtil.getSimpleString(newSettings, "protocol-adapter")
                    + "\" must be inherited from \"com.truport.nusuth.distributor.DistributorRequestAdapter\"");
        }

        CompositeNusuthWebAppElement newTcpNode = ManagementUtil.getCompositeElement(newSettings, "tcp-server");
        if (tcpServer == null || tcpServer.isRestartNeeded(newTcpNode)) {
            //      DistributorRequestHandler.setAllowHosts(ManagementUtil.getSimpleIpList(newTcpNode, "allow-hosts"));
            //      DistributorRequestHandler.setDenyHosts(ManagementUtil.getSimpleIpList(newTcpNode, "deny-hosts"));

            if (tcpServer != null)
                tcpServer.stopServer();
            tcpServer = new NusuthTcpServer(newTcpNode, DistributorRequestHandler.class);
            /*        new NusuthTcpServer(
             ManagementUtil.getSimpleInt(newTcpNode, "port"),
             ManagementUtil.getSimpleTime(newTcpNode, "so-timeout"),
             ManagementUtil.getSimpleInt(newTcpNode, "queue"),
             ManagementUtil.getSimpleInt(newTcpNode, "min-handlers"),
             ManagementUtil.getSimpleInt(newTcpNode, "max-handlers"),
             ManagementUtil.getSimpleTime(newTcpNode, "handler-timeout"),
             ManagementUtil.getSimpleInt(newTcpNode, "max-keepalives"),
             DistributorRequestHandler.class);
             */
            tcpServer.startServer();
        } else {
            tcpServer.applySettings(newTcpNode);
        }

        CompositeNusuthWebAppElement newSslNode = ManagementUtil.getCompositeElement(newSettings, "ssl-server");
        if (newSslNode != null) {
            //      DistributorRequestHandler.setSSLAllowHosts(ManagementUtil.getSimpleIpList(newSslNode, "allow-hosts"));
            //      DistributorRequestHandler.setSSLDenyHosts(ManagementUtil.getSimpleIpList(newSslNode, "deny-hosts"));

            CompositeNusuthWebAppElement sslKeystoreNode = ManagementUtil.getCompositeElement(newSslNode, "keystore");
            if (sslServer == null || sslServer.isRestartNeeded(newSslNode)) {
                if (sslServer != null)
                    sslServer.stopServer();
                sslServer = new NusuthSslServer(newSslNode, DistributorRequestHandler.class);
                /*          new NusuthSslServer(
                 ManagementUtil.getSimpleInt(newSslNode, "port"),
                 ManagementUtil.getSimpleTime(newSslNode, "so-timeout"),
                 ManagementUtil.getSimpleInt(newSslNode, "queue"),
                 ManagementUtil.getSimpleInt(newSslNode, "min-handlers"),
                 ManagementUtil.getSimpleInt(newSslNode, "max-handlers"),
                 ManagementUtil.getSimpleTime(newSslNode, "handler-timeout"),
                 ManagementUtil.getSimpleInt(newSslNode, "max-keepalives"),
                 DistributorRequestHandler.class,
                 ManagementUtil.getSimpleString(sslKeystoreNode, "location"),
                 ManagementUtil.getSimpleString(sslKeystoreNode, "password"));
                 */
                sslServer.startServer();
            }
        } else {
            if (sslServer != null)
                sslServer.stopServer();
            sslServer = null;
        }

        //DistributorRequestHandler.resetContainerCount();

        // all ok
        this.settings = settings;
    }


    public boolean isRestartNeeded(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        return false;
    }
}
