package com.azoft.nusuth.management;

import java.util.*;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import com.azoft.nusuth.util.StrBuffer;
import com.azoft.nusuth.deployment.*;

public class ApplicationInfoImpl implements ContainerApplicationInfo, DeployerApplicationInfo, DistributorApplicationInfo, java.io.Serializable {
    private final static char[] HTTP_PROTOCOL_CHARS = {'h', 't', 't', 'p'};
    private final static char[] HTTPS_PROTOCOL_CHARS = {'h', 't', 't', 'p', 's'};
    private boolean running;
    private HashSet containers;
    private HashSet protocols; //Strings
    private String location;


    public ApplicationInfoImpl(CompositeNusuthWebAppElement node) {
        try {
            location = ((SimpleNusuthWebAppElement) node.getSimpleChild("location").nextElement()).getContent();
        } catch (DeploymentException dex) {
            location = "";
        }
        try {
            running = ((SimpleNusuthWebAppElement) node.getSimpleChild("is-running").nextElement()).getContent().equalsIgnoreCase("true");
        } catch (DeploymentException dex) {
            running = false;
        }
        try {
            protocols = new HashSet();
            Enumeration protocolEnum = node.getSimpleChild("protocol");
            String protocolString = "http";
            if (protocolEnum.hasMoreElements())
                protocolString = ((SimpleNusuthWebAppElement) protocolEnum.nextElement()).getContent();
            StringTokenizer tokenizer = new StringTokenizer(protocolString, ",", false);
            while (tokenizer.hasMoreTokens())
                protocols.add(tokenizer.nextToken().toLowerCase());
            if (protocols.isEmpty())
                protocols.add("http");
        } catch (DeploymentException dex) {
            protocols.add("http");
        }
        containers = new HashSet();
        try {
            for (Enumeration containersEnum = node.getSimpleChild("container"); containersEnum.hasMoreElements();) {
                containers.add(((SimpleNusuthWebAppElement) containersEnum.nextElement()).getContent());
            }
        } catch (DeploymentException dex) {
        }
    }


    public ApplicationInfoImpl(HashSet containers, String location) {
        this.containers = containers;
        this.location = location;
        this.running = true;
        this.protocols = new HashSet(1);
        this.protocols.add(new String(HTTP_PROTOCOL_CHARS));
    }


    public ApplicationInfoImpl(HashSet containers, HashSet protocols) {
        this.containers = containers;
        this.protocols = protocols;
        this.location = "";
        this.running = true;
    }


    public ApplicationInfoImpl(boolean isRunning, String container) {
        this.protocols = new HashSet(1);
        this.running = isRunning;
        if (container != null)
            this.containers.add(container);
        this.location = "";
    }


    public ApplicationInfoImpl(boolean isRunning, HashSet containers, HashSet protocols, String location) {
        this.running = isRunning;
        this.containers = containers;
        this.protocols = protocols;
        this.location = location;
    }


    public boolean equals(Object o) {
        if (o instanceof ApplicationInfo) {
            ApplicationInfo app = (ApplicationInfo) o;
            return (containers.size() > 0 == app.getContainers().size() > 0);
        } else {
            return false;
        }
    }


    public HashSet getContainers() {
        return containers;
    }


    public String getLocation() {
        return location;
    }


    public HashSet getProtocols() {
        return protocols;
    }


    public boolean isRunning() {
        return running;
    }


    private static int protocol2int(StrBuffer protocolStr) {
        if (protocolStr.equals(HTTP_PROTOCOL_CHARS)) {
            return HTTP_PROTOCOL;
        } else if (protocolStr.equals(HTTPS_PROTOCOL_CHARS)) {
            return HTTPS_PROTOCOL;
        }
        return -1;
    }


    private static int protocol2int(String protocolString) {
        protocolString = protocolString.toLowerCase().trim();
        if (protocolString.equals("http")) {
            return HTTP_PROTOCOL;
        } else if (protocolString.equals("https")) {
            return HTTPS_PROTOCOL;
        }
        return -1;
    }


    public void setContainers(HashSet containers) {
        this.containers = containers;
    }


    public void setLocation(String location) {
        this.location = location;
    }


    public void setProtocols(HashSet protocols) {
        this.protocols = protocols;
    }


    public void setRunning(boolean running) {
        this.running = running;
    }


    public String toString() {
        return "{run=" + running + ", " + containers + ", " + protocols + ", " + location + "}";
    }
}