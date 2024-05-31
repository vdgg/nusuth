package com.azoft.nusuth.management;

import java.io.*;
import java.security.KeyStore;

import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;

import java.util.*;

import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

public final class ManagementUtil {
    public final static String EMPTY_APPLICATION_ERROR_XML =
            "<?xml version=\"1.0\"?>"
            + "<!DOCTYPE application-deployment-errors SYSTEM \"application_deployment_errors.dtd\">"
            + "<application-deployment-errors>"
            + "</application-deployment-errors>";
    public final static String APPLICATION_DEPLOYMENT_ERRORS_TYPE = "application-deployment-errors";
    public final static String APPLICATION_DEPLOYMENT_TYPE = "application-deployment";
    public final static String EMPTY_APPLICATION_DEPLOYMENT_XML =
            "<?xml version=\"1.0\"?>"
            + "<!DOCTYPE application-deployment SYSTEM \"application_deployment.dtd\">"
            + "<application-deployment>"
            + "</application-deployment>";
    public static final String DISTRIBUTED_JNDI_CONFIG_TYPE = "securityConfig";
    public static final String DISTRIBUTED_JNDI_HOST_CONFIG = "cluster-host";
    public static final String DISTRIBUTED_JNDI_APP_JBIRD_CONFIG = "nusuth-web-app-config";
    public static final String WEB_APP_USERS_CONFIG = "web-app-users";

    public static CompositeNusuthWebAppElement getCompositeElement(CompositeNusuthWebAppElement node, String name) throws DeploymentException {
        Enumeration result = node.getCompositeChild(name);
        if (result.hasMoreElements())
            return (CompositeNusuthWebAppElement) result.nextElement();
        else
            return null;
    }

    public static SimpleNusuthWebAppElement getSimpleElement(CompositeNusuthWebAppElement node, String name) throws DeploymentException {
        Enumeration result = node.getSimpleChild(name);
        if (result.hasMoreElements())
            return (SimpleNusuthWebAppElement) result.nextElement();
        else
            return null;
    }

    public static File getConfigFile(String config, String defaultConfig) throws ManagementException {
        File result = null;
        String jbirdHome = System.getProperty("nusuth.home");
        if (jbirdHome == null || jbirdHome.trim().length() == 0)
            jbirdHome = "..";

        if (config != null) {
            result = new File(new File(jbirdHome, "admin"), config);
        }
        if (config != null && (!result.exists() || !result.isFile() || !result.canRead()))
            result = new File(config);
        if (result == null || !result.exists() || !result.isFile() || !result.canRead()) {
            result = new File(new File(jbirdHome, "admin"), defaultConfig);
        }
        if (!result.exists() || !result.isFile() || !result.canRead()) {
            throw new ManagementException(
                    "couldn't find config file (\""
                    + (config == null ? "" : config)
                    + "\" or \""
                    + (new File(new File(jbirdHome, "admin"), config)).getAbsolutePath()
                    + "\" or \""
                    + (new File(new File(jbirdHome, "admin"), defaultConfig)).getAbsolutePath()
                    + "\" or )");
        }
        return result;
    }

    public static int getSimpleInt(CompositeNusuthWebAppElement settings, String elementName) throws DeploymentException {
        Enumeration simpleEnumeration = settings.getSimpleChild(elementName);
        if (!simpleEnumeration.hasMoreElements()) {
            throw new DeploymentException("Cannot find " + elementName + " element, wrong config");
        }
        try {
            return Integer.parseInt(((SimpleNusuthWebAppElement) simpleEnumeration.nextElement()).getContent());
        } catch (Exception ex) {
            throw new DeploymentException("Cannot convert element " + elementName + " to int");
        }
    }

    public static Vector getSimpleIpList(CompositeNusuthWebAppElement node, String name) throws DeploymentException {
        return parseIpList(getSimpleString(node, name));
    }

    public static String getSimpleString(CompositeNusuthWebAppElement node, String name) throws DeploymentException {
        Enumeration enum = node.getSimpleChild(name);
        if (enum.hasMoreElements())
            return ((SimpleNusuthWebAppElement) enum.nextElement()).getContent();
        else
            return "";
    }

    public static String[] getSimpleStrings(CompositeNusuthWebAppElement node, String name) throws DeploymentException {
        Enumeration enum = node.getSimpleChild(name);
        if (enum.hasMoreElements()) {
            Vector values = new Vector();
            while (enum.hasMoreElements()) {
                values.add(((SimpleNusuthWebAppElement) enum.nextElement()).getContent());
            }
            return (String[]) values.toArray(new String[0]);
        } else {
            return new String[0];
        }
    }

    public static int getSimpleTime(CompositeNusuthWebAppElement settings, String elementName) throws DeploymentException {
        Enumeration simpleEnumeration = settings.getSimpleChild(elementName);
        if (!simpleEnumeration.hasMoreElements()) {
            throw new DeploymentException("Cannot find " + elementName + " element, wrong config");
        }
        try {
            String timeout = ((SimpleNusuthWebAppElement) simpleEnumeration.nextElement()).getContent().toLowerCase().trim();
            if (timeout.endsWith("h"))
                return Integer.parseInt(timeout.substring(0, timeout.length() - 1)) * 3600000;
            else if (timeout.endsWith("m"))
                return Integer.parseInt(timeout.substring(0, timeout.length() - 1)) * 60000;
            else if (timeout.endsWith("s"))
                return Integer.parseInt(timeout.substring(0, timeout.length() - 1)) * 1000;
            else
                return Integer.parseInt(timeout) * 1000;
        } catch (Exception ex) {
            throw new DeploymentException("Cannot convert element " + elementName + " to time");
        }
    }

    public static KeyStore loadKeystore(CompositeNusuthWebAppElement keystoreNode) throws DeploymentException {
        try {
            File file = getConfigFile(getSimpleString(keystoreNode, "location"), "nusuth.keys");

            KeyStore result = KeyStore.getInstance("JKS");
            String password = getSimpleString(keystoreNode, "password");
            char[] passwordChars = new char[password.length()];
            password.getChars(0, password.length(), passwordChars, 0);
            result.load(new FileInputStream(file), passwordChars);

            return result;
        } catch (Exception ex) {
            throw new DeploymentException("Coudn't load keystore, nested:" + ex.getMessage());
        }
    }

    public static Vector parseIpList(String ipList) {
        Vector result = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(ipList, ",");

        while (tokenizer.hasMoreTokens()) {
            StringTokenizer st = new StringTokenizer(tokenizer.nextToken().trim(), ".");
            String[] ip = new String[4];
            for (int i = 0; i < 4; i++) {
                ip[i] = st.nextToken().trim();
            }

            result.add(ip);
        }

        return result;
    }

    public static Vector parseCommaList(String list) {
        Vector result = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(list, ",");

        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken().trim());
        }

        return result;
    }

    public static boolean getSimpleBoolean(CompositeNusuthWebAppElement node,
                                           String name,
                                           boolean defaultValue)
            throws DeploymentException {
        Enumeration enum = node.getSimpleChild(name);
        if (enum.hasMoreElements()) {
            String va = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent();

            if (va.equalsIgnoreCase("true")
                    || va.equalsIgnoreCase("on")
                    || va.equalsIgnoreCase("yes")) {
                return true;
            } else if (va.equalsIgnoreCase("false")
                    || va.equalsIgnoreCase("off")
                    || va.equalsIgnoreCase("no")) {
                return false;
            }
        }
        return defaultValue;
    }

    public static boolean getSimpleBoolean(CompositeNusuthWebAppElement node,
                                           String name)
            throws DeploymentException {
        Enumeration enum = node.getSimpleChild(name);
        if (enum.hasMoreElements()) {
            String va = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent();

            if (va.equalsIgnoreCase("true")
                    || va.equalsIgnoreCase("on")
                    || va.equalsIgnoreCase("yes")) {
                return true;
            } else if (va.equalsIgnoreCase("false")
                    || va.equalsIgnoreCase("off")
                    || va.equalsIgnoreCase("no")) {
                return false;
            } else {
                throw new DeploymentException("Cannot recognize value "
                        + va + " of " + name + " element"
                        + "; use true or false");
            }
        } else {
            throw new DeploymentException("Element " + name + " not specified");
        }
    }

    public static void removeSimpleChild(CompositeNusuthWebAppElement parent,
                                         String childName)
            throws DeploymentException {
        if (parent.isChildRequired(childName)) {
            if (parent.isChildUnbounded(childName)) {
                boolean cycle = true;
                do {
                    Enumeration i = parent.getSimpleChild(childName);
                    if (i.hasMoreElements()) {
                        i.nextElement();
                        if (cycle = i.hasMoreElements()) {
                            parent.removeSimpleChild(childName,
                                    (SimpleNusuthWebAppElement)
                                    i.nextElement());
                        }
                    } else {
                        cycle = false;
                    }
                } while (cycle);
            } else {
                // nothing to do
            }
        } else {
            while (parent.getSimpleChild(childName).hasMoreElements()) {
                parent.removeSimpleChild(childName,
                        (SimpleNusuthWebAppElement)
                        parent.getSimpleChild(childName)
                        .nextElement());
            }
        }
    }

    public static void removeCompositeChild(CompositeNusuthWebAppElement parent,
                                            String childName)
            throws DeploymentException {
        if (parent.isChildRequired(childName)) {
            if (parent.isChildUnbounded(childName)) {
                boolean cycle = true;
                do {
                    Enumeration i = parent.getCompositeChild(childName);
                    if (i.hasMoreElements()) {
                        i.nextElement();
                        if (cycle = i.hasMoreElements()) {
                            parent.removeCompositeChild(childName,
                                    (CompositeNusuthWebAppElement)
                                    i.nextElement());
                        }
                    } else {
                        cycle = false;
                    }
                } while (cycle);
            } else {
                // nothing to do
            }
        } else {
            while (parent.getCompositeChild(childName).hasMoreElements()) {
                parent.removeCompositeChild(childName,
                        (CompositeNusuthWebAppElement)
                        parent.getCompositeChild(childName)
                        .nextElement());
            }
        }
    }

    public static void copyCompositeChild(CompositeNusuthWebAppElement srcParent,
                                          CompositeNusuthWebAppElement dstParent,
                                          String childName)
            throws DeploymentException {
        if (dstParent.isChildUnbounded(childName)) {
            for (Enumeration i = srcParent.getCompositeChild(childName); i.hasMoreElements();) {
                CompositeNusuthWebAppElement child = (CompositeNusuthWebAppElement) i.nextElement();
                dstParent.addCompositeChild(childName, child);
            }
        } else {
            CompositeNusuthWebAppElement srcChild = getCompositeElement(srcParent, childName);
            if (srcChild != null) {
                CompositeNusuthWebAppElement dstChild = dstParent.setCompositeChild(childName);
                for (Enumeration i = srcChild.getCompositeChildrenNames(); i.hasMoreElements();) {
                    copyCompositeChild(srcChild, dstChild, (String) i.nextElement());
                }
                for (Enumeration i = srcChild.getSimpleChildrenNames(); i.hasMoreElements();) {
                    copySimpleChild(srcChild, dstChild, (String) i.nextElement());
                }
            }
        }
    }

    public static void copySimpleChild(CompositeNusuthWebAppElement srcParent,
                                       CompositeNusuthWebAppElement dstParent,
                                       String childName)
            throws DeploymentException {
        if (dstParent.isChildUnbounded(childName)) {
            for (Enumeration i = srcParent.getSimpleChild(childName); i.hasMoreElements();) {
                SimpleNusuthWebAppElement child = (SimpleNusuthWebAppElement) i.nextElement();
                dstParent.addSimpleChild(childName).setContent(child.getContent());
            }
        } else {
            String value = getSimpleString(srcParent, childName);
            if (value != null && value.length() > 0) {
                dstParent.setSimpleChild(childName).setContent(value);
            }
        }
    }

    public static void copyCompositeElement(CompositeNusuthWebAppElement srcParent,
                                            CompositeNusuthWebAppElement dstParent)
            throws DeploymentException {
        // copy simple childs
        for (Enumeration i = srcParent.getSimpleChildrenNames();
             i.hasMoreElements();) {
            String elemName = (String) i.nextElement();
            removeSimpleChild(dstParent, elemName);
            ManagementUtil.copySimpleChild(srcParent, dstParent, elemName);
        }

        // copy composite childs
        for (Enumeration i = srcParent.getCompositeChildrenNames();
             i.hasMoreElements();) {
            String elemName = (String) i.nextElement();
            removeCompositeChild(dstParent, elemName);
            copyCompositeChild(srcParent, dstParent, elemName);
        }
    }

    public static CompositeNusuthWebAppElement
            getCompositeElement(CompositeNusuthWebAppElement node,
                                String childName,
                                String subChildName,
                                String value)
            throws DeploymentException {
        for (Enumeration i = node.getCompositeChild(childName); i.hasMoreElements();) {
            CompositeNusuthWebAppElement element =
                    (CompositeNusuthWebAppElement) i.nextElement();
            if (getSimpleString(element, subChildName).equals(value)) {
                return element;
            }
        }
        return null;
    }
}
