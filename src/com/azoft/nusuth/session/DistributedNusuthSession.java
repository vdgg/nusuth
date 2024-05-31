package com.azoft.nusuth.session;

import java.util.LinkedList;
import java.util.Hashtable;
import java.util.List;
import java.util.Enumeration;
import java.io.*;
import javax.servlet.http.*;

import com.azoft.nusuth.core.NusuthContext;

/**
 * This class reprents NusuthSession in cluster.
 * @author skilz
 * @version 1.3
 * @since Nusuth1.0
 */
public class DistributedNusuthSession extends NusuthSession {

    private String distributorId;
    private String localId;
    private String remoteId;
    private boolean modified = false;

    public DistributedNusuthSession(LinkedList sessionListeners, LinkedList sessionAttrListeners, NusuthContext context) {
        super(sessionListeners, sessionAttrListeners, context);
    }

    public Object getAttribute(String name) {
        modified = true;
        return super.getAttribute(name);
    }

    public Object getValue(String name) {
        modified = true;
        return super.getValue(name);
    }

    public void putValue(String name, Object value) {
        if (!(value instanceof Serializable))
            throw new IllegalArgumentException("Only serializable values alowed as attribute of session");
        modified = true;
        super.putValue(name, value);
        if (value instanceof HttpSessionActivationListener) {
            sessionActListeners.put(name, value);
        }
    }

    public void removeAttribute(String name) {
        sessionActListeners.remove(name);
        modified = true;
        super.removeAttribute(name);
    }

    public void removeValue(String name) {
        sessionActListeners.remove(name);
        modified = true;
        super.removeValue(name);
    }

    public void setAttribute(String name, Object o) {
        if (!(o instanceof Serializable))
            throw new IllegalArgumentException("Only serializable values alowed as attribute of session");
        modified = true;
        super.setAttribute(name, o);
        if (o instanceof HttpSessionActivationListener) {
            sessionActListeners.put(name, o);
        }
    }

    public void setComponentsId(String distribId, String localContId, String remoteContId) {
        distributorId = distribId;
        localId = localContId;
        remoteId = remoteContId;
        this.id = this.id + "$" + localContId + "$" + remoteContId + "_" + distribId;
    }

    public boolean isOnLocal(String contId) {
        if (localId.equals(contId)) {
            return true;
        } else {
            Enumeration enum = sessionActListeners.keys();
            while (enum.hasMoreElements()) {
                String key = (String) enum.nextElement();
                ((HttpSessionActivationListener) sessionActListeners.get(key)).sessionDidActivate(new HttpSessionEvent(this));
            }
        }
        return false;
    }

    public void changeId(String remoteId) {
        this.id = this.id.substring(0, this.id.indexOf('$')) + "$" + this.remoteId + "$" + remoteId + "_" + this.distributorId;
        this.localId = this.remoteId;
        this.remoteId = remoteId;
    }

    public void setRemoteId(String remid) {
        this.id = this.id.substring(0, this.id.lastIndexOf('$')) + "$" + remid + "_" + this.distributorId;
        this.remoteId = remid;
    }

    public String getDistributorId() {
        return distributorId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setSessionId(String id) {
        this.id = id;
        this.distributorId = id.substring(id.indexOf('_') + 1);
        this.localId = id.substring(id.indexOf('$') + 1, id.lastIndexOf('$'));
        this.remoteId = id.substring(id.lastIndexOf('$') + 1, id.indexOf('_'));
    }

    public void readObject(ObjectInputStream is) throws Exception {
        attributes = (Hashtable) is.readObject();
        sessionActListeners = (Hashtable) is.readObject();
    }

    public void writeObject(OutputStream os) throws Exception {
        Thread.currentThread().setContextClassLoader(context.getServletLoader());
        Enumeration enum = sessionActListeners.keys();
        while (enum.hasMoreElements()) {
            String key = (String) enum.nextElement();
            ((HttpSessionActivationListener) sessionActListeners.get(key)).sessionWillPassivate(new HttpSessionEvent(this));
        }
        ObjectOutputStream stream = new ObjectOutputStream(os);
        stream.writeObject(((NusuthContext) getServletContext()).getContextName());
        stream.writeObject(getId());
        stream.writeObject(attributes);
        stream.writeObject(sessionActListeners);
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean hasRemote() {
        return (remoteId.length() != 0);
    }

}