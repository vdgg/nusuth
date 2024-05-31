package com.azoft.nusuth.management.security;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.ComponentType;
import com.azoft.nusuth.management.ManagementException;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.management.Manageable;
import com.azoft.nusuth.jidep.ClientJidepAdapter;
import com.azoft.nusuth.jidep.JidepProtocolAdapter;
import com.azoft.nusuth.jidep.JidepConnectionFactory;

import java.security.acl.*;
import java.util.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;

import org.w3c.dom.*;
import org.apache.log4j.Category;
import sun.security.acl.*;

/**
 * Security manager class
 * @author vdgg, igork
 * @since Nusuth1.0
 * @version 1.13
 */
public class SecurityManager
        implements Manageable {
    protected Acl acl = null;
    protected String clusterKeyAlias;
    protected String authKey;
    protected int clusterRmiPort = 1080;
    protected Hashtable invoked = new Hashtable();
    private Hashtable passwords = new Hashtable();
    private Hashtable sessions = new Hashtable();
    protected SecuritySettings settings = new SecuritySettings();
    private CompositeNusuthWebAppElement settingsNode = null;
    private File configFile = null;
    private Category logger = Category.getInstance(this.getClass());

    private class SessionCleaner
            extends Thread {
        private long expirationTime;
        private long checkTime;

        SessionCleaner(long expirationTime, long checkTime) {
            super("Session Cleaner");
            this.expirationTime = expirationTime;
            this.checkTime = checkTime;
        }

        public void run() {
            while (true) {
                synchronized (sessions) {
                    Enumeration enum = sessions.keys();
                    while (enum.hasMoreElements()) {
                        String sessionId = (String) enum.nextElement();
                        Session session = (Session) sessions.get(sessionId);
                        if (System.currentTimeMillis() - session.getLastAccess()
                                > expirationTime) {
                            sessions.remove(sessionId);
                        }
                    }
                }
                try {
                    sleep(checkTime);
                } catch (InterruptedException ie) {
                }
            }
        }
    }


    public SecurityManager(CompositeNusuthWebAppElement newSettings)
            throws DeploymentException {
        logger.info("Starting security manager");
        NusuthAppConfigFactory.addEntityResolver("securityConfig",
                new SecurityConfigEntityResolver());

        applySettings(newSettings);
        (new SessionCleaner(1800000, 60000)).start();
        logger.info("Security manager started");
    }

    public void applySettings(CompositeNusuthWebAppElement newSettings)
            throws DeploymentException {
        logger.info("Applying new settings");
        CompositeNusuthWebAppElement managerNode
                = ManagementUtil.getCompositeElement(newSettings, "manager");
        clusterRmiPort = ManagementUtil.getSimpleInt(managerNode, "port");
        authKey = ManagementUtil.getSimpleString(managerNode, "auth-key");
        JidepConnectionFactory.setKey(authKey);

        try {
            File newConfigFile = ManagementUtil.getConfigFile(
                    ManagementUtil.getSimpleString(newSettings, "access-config"),
                    "security.xml");
            if (!newConfigFile.equals(configFile)) {
                this.settings.read(settingsNode =
                        NusuthAppConfigFactory.createConfig(
                                "securityConfig",
                                new FileInputStream(newConfigFile)));
                this.acl = settings.getAcl();
                this.passwords = settings.getPasswords();
                configFile = newConfigFile;
            }
        } catch (NotOwnerException noex) {
            logger.error("NotOwnerException in SecuritySettings", noex);
            throw new DeploymentException("NotOwnerException in SecuritySettings: "
                    + noex.getMessage());
        } catch (Exception ex) {
            logger.error("Couldn't get security settings", ex);
            throw new DeploymentException("Couldn't get security settings, nested:"
                    + ex.getMessage());
        }
    }

    public boolean isRestartNeeded(CompositeNusuthWebAppElement newSettings)
            throws DeploymentException {
        return false;
    }

    public boolean checkComponent(String address, int adminPort)
            throws ManagementException {
        logger.debug("Check component on " + address + ':' + adminPort);
        return invoked.containsKey(address + ':' + adminPort);
    }

    public boolean checkPermission(String sessionId, NusuthPermission permission)
            throws UnauthorizedAccessException, ManagementException {
        logger.debug("Check permission " + permission);
        Session session = (Session) sessions.get(sessionId);
        if (session == null) {
            throw new UnauthorizedAccessException();
        }

        session.touch();

        if (acl != null) {
            return acl.checkPermission(session.getUser(), permission);
        } else {
            logger.error("Internal error: acl is null");
            throw new ManagementException("Internal error: acl is null");
        }
    }


    protected void clear() {
        acl = null;
    }

    public void invokeComponent(String host, int adminPort)
            throws UnknownHostException, IOException, ManagementException {
        logger.info("Invoke component on " + host + ':' + adminPort);
        Socket component = new Socket(host, adminPort);
        ClientJidepAdapter adapter = JidepProtocolAdapter.getClientSide(component,
                authKey);
        adapter.processAuthenticate();
        adapter.setCommand("accept");
        ObjectOutputStream ostream =
                new ObjectOutputStream(adapter.getOutputStream());

        String address = component.getInetAddress().getHostAddress();
        if (address.equals("127.0.0.1"))
            address = InetAddress.getLocalHost().getHostAddress();

        byte[] message = ("//" + InetAddress.getLocalHost().getHostName() + ":"
                + clusterRmiPort + "/Cluster Manager Controller").getBytes();

        ostream.writeInt(message.length);
        ostream.write(message);
        ostream.flush();
        adapter.endRequest();
        adapter.parseResponse();
        adapter.close();
        invoked.put(address + ':' + adminPort, address + ':' + adminPort);
    }


    public String login(String user, String encodedPassword) {
        if ((user != null) && (encodedPassword != null) && encodedPassword.equals(passwords.get(user.trim()))) {
            Session session = new Session(user.trim());
            String sessionId = session.getId();
            sessions.put(sessionId, session);
            logger.info("User \"" + user + "\" logged in");
            return sessionId;
        } else {
            logger.info("User \"" + user + "\" not logged in");
            return null;
        }
    }


    public CompositeNusuthWebAppElement getSettings() {
        return settingsNode;
    }

    private void saveSettings() throws ManagementException {
        logger.debug("Save settings");
        try {
            String src = settingsNode.compose("securityConfig", "security.dtd");
            OutputStream ostream = new FileOutputStream(configFile);
            ostream.write(src.getBytes());
            ostream.flush();
            ostream.close();
        } catch (IOException ioex) {
            throw new ManagementException("Couldn't save security settings, nested: " + ioex.getMessage());
        }
    }

    public void setSettings(CompositeNusuthWebAppElement newSettingsNode)
            throws ManagementException {
        logger.debug("Set new security settings");
        try {
            this.settings.read(settingsNode = newSettingsNode);
        } catch (NotOwnerException noex) {
            throw new ManagementException("NotOwnerException in SecuritySettings: " + noex.getMessage());
        }

        this.acl = settings.getAcl();
        this.passwords = settings.getPasswords();

        saveSettings();
    }

    public void setUserPassword(String sessionId, CompositeNusuthWebAppElement securitySettingsNode)
            throws UnauthorizedAccessException, ManagementException {
        Session session = (Session) sessions.get(sessionId);
        if (session == null) {
            throw new UnauthorizedAccessException();
        }
        session.touch();

        String userName = session.getUser().getName();
        logger.info("Set user password for user \"" + userName + '"');
        String oldPassword = (String) passwords.get(userName);
        if (oldPassword == null) {
            logger.warn("Couldn't set new password for user \"" + userName + "\" - unknown user");
            throw new ManagementException("Couldn't set new password for user \"" + userName + "\", nested: unknown user");
        }

        String newPassword = null;
        try {
            for (Enumeration i = ManagementUtil.getCompositeElement(securitySettingsNode, "users").getCompositeChild("user"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement userNode = (CompositeNusuthWebAppElement) i.nextElement();
                if (ManagementUtil.getSimpleString(userNode, "name").equals(userName)) {
                    newPassword = ManagementUtil.getSimpleString(userNode, "password");
                    break;
                }
            }

            if (newPassword == null) {
                logger.warn("Couldn't set new password for user \"" + userName + "\" - empty password");
                throw new ManagementException("Couldn't set new password for user \"" + userName + "\", nested: empty password");
            }

            if (!newPassword.equals(oldPassword)) {
                passwords.put(userName, newPassword);

                boolean passwordSettedSucessfully = false;
                for (Enumeration i = ManagementUtil.getCompositeElement(settingsNode, "users").getCompositeChild("user"); i.hasMoreElements();) {
                    CompositeNusuthWebAppElement userNode = (CompositeNusuthWebAppElement) i.nextElement();
                    if (ManagementUtil.getSimpleString(userNode, "name").equals(userName)) {
                        SimpleNusuthWebAppElement passwordNode = ManagementUtil.getSimpleElement(userNode, "password");
                        passwordNode.setContent(newPassword);
                        passwordSettedSucessfully = true;
                        break;
                    }
                }

                if (passwordSettedSucessfully)
                    saveSettings();
                else {
                    logger.warn("Couldn't set password for user \"" + userName + "\"");
                    throw new ManagementException("Couldn't set password for user \"" + userName + "\"");
                }
            }
        } catch (DeploymentException dex) {
            logger.error("Couldn't set new user password", dex);
            throw new ManagementException("Couldn't set new user password, nested: " + dex.getMessage());
        }
    }
}
