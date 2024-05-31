package com.azoft.nusuth.core;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.azoft.nusuth.session.DistributedSessionManager;
import com.azoft.nusuth.session.DistributedNusuthSession;
import com.azoft.nusuth.distributor.connectionfactory.ContainerAddress;
import com.azoft.nusuth.management.ContainerManager;
import com.azoft.nusuth.management.ContainerManagerImpl;

import javax.servlet.http.HttpSession;

public class LocalContainer {

    private Hashtable contextPath2sessionManager = new Hashtable();
    private ContainerManager manager;
    private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jidep");
    private String authKey = null;

    public LocalContainer(ContainerManager manager, String authKey) {
        this.manager = manager;
        this.authKey = authKey;
    }

    public void registerManager(String contextPath, DistributedSessionManager manager) {
        contextPath2sessionManager.put(contextPath, manager);
    }

    public void updateSession(InputStream ist) throws Exception {
        ObjectInputStream is = new ObjectInputStream(ist);
        String contextName = (String) is.readObject();
        String id = (String) is.readObject();
        DistributedSessionManager sm = (DistributedSessionManager) contextPath2sessionManager.get(contextName);
        DistributedNusuthSession sess = (DistributedNusuthSession) sm.getSession(id);
        if (sess == null) {
            sess = (DistributedNusuthSession) sm.createSession(id);
        }
        sess.readObject(is);
    }

    public void removeSession(InputStream ist) throws Exception {
        ObjectInputStream is = new ObjectInputStream(ist);
        String id = (String) is.readObject();
        Enumeration enum = contextPath2sessionManager.keys();
        while (enum.hasMoreElements()) {
            String path = (String) enum.nextElement();
            DistributedSessionManager sm = (DistributedSessionManager) contextPath2sessionManager.get(path);
            if (sm.getSession(id) != null) {
                HttpSession session = sm.removeSession(id);
                if (session != null) {
                    session.invalidate();
                }
            }
        }
    }

    public void addContainers(InputStream ist) throws Exception {
        ObjectInputStream is = new ObjectInputStream(ist);
        String distributorId = (String) is.readObject();
        manager.setDistributorIdToContexts(distributorId);
        String path = convertName((String) is.readObject());
        List list = (List) is.readObject();
        for (int i = 0; i < list.size(); i = i + 2) {
            String contName = (String) list.get(i);
            ContainerAddress addr = (ContainerAddress) list.get(i + 1);
            RemoteContainer rem = new RemoteContainer(addr.host, addr.adminPort, addr.port, contName);
            rem.setLocalAuthKey(authKey);
            DistributedSessionManager sm = (DistributedSessionManager) contextPath2sessionManager.get(path);
            if (sm != null && !rem.getName().equals(manager.getComponentId()))
                sm.addRemoteContainer(distributorId, rem);
        }
    }

    public void removeContainer(InputStream ist) throws Exception {
        ObjectInputStream stream = new ObjectInputStream(ist);
        String distributorId = (String) stream.readObject();
        String contId = (String) stream.readObject();
        Enumeration enum = contextPath2sessionManager.keys();
        while (enum.hasMoreElements()) {
            String key = (String) enum.nextElement();
            DistributedSessionManager sm = (DistributedSessionManager) contextPath2sessionManager.get(key);
            sm.removeRemoteContainer(distributorId, contId);
        }
    }

    private String convertName(String name) {
        StringBuffer sb = new StringBuffer();
        char tmp;
        for (int i = 0; i < name.length(); i++) {
            tmp = name.charAt(i);
            if ((tmp >= 'a' && tmp <= 'z') || (tmp >= 'A' && tmp <= 'Z') ||
                    (tmp >= '0' && tmp <= '9') || tmp == '_') {
                sb.append(tmp);
            } else {
                sb.append("d" + (int) tmp);
            }
        }
        return sb.toString();
    }


}