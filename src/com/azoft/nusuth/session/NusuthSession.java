package com.azoft.nusuth.session;

import javax.servlet.http.*;
import javax.servlet.ServletContext;
import java.util.*;
import java.io.*;
import java.security.Principal;

import com.azoft.nusuth.container.ContainerInvocationCacheElement;
import com.azoft.nusuth.core.HttpRequestLine;
import com.azoft.nusuth.core.NusuthContext;

/**
 * This class is implementation of HttpSession intarface.
 * @author vdgg, skilz
 * @version 1.12
 * @since Nusuth1.0
 */
public class NusuthSession implements HttpSession {
    protected Hashtable attributes = new Hashtable();
    protected long creationTime;
    protected String id;
    protected long lastAccessedTime;
    protected long newAccessedTime = -1;
    protected int inactiveInterval = -1;
    protected boolean valid;
    protected boolean isNew;
    protected LinkedList sessionListeners;
    protected LinkedList sessionAttrListeners;
    protected Principal user = null;
    protected int authType = -1;
    protected String savedUrl;
    protected NusuthContext context;
    protected Hashtable sessionActListeners = new Hashtable();
    protected boolean modified = true;
    private boolean backup = false;

    /**
     * Constructor for NusuthSession.
     * @param sessionListeners List of HttpSessionListener.
     * @param sessionAttrListeners List of HttpSessionAttributeListener.
     * @param context NusuthContext.
     */
    public NusuthSession(LinkedList sessionListeners,
                         LinkedList sessionAttrListeners, NusuthContext context) {
        creationTime = System.currentTimeMillis();
        id = Long.toHexString(creationTime)
                + Long.toHexString((long) (Math.random() * 1e16));
        lastAccessedTime = creationTime;
        newAccessedTime = creationTime;
        valid = true;
        isNew = true;
        this.sessionListeners = sessionListeners;
        this.sessionAttrListeners = sessionAttrListeners;
        this.context = context;
        String sessionBackup = context.getSessionBackup();
        if (sessionBackup.equals("always") || sessionBackup.equals("shutdown")) {
            backup = true;
        }
        for (int i = 0; i < sessionListeners.size(); i++) {
            ((HttpSessionListener) sessionListeners.get(i)).
                    sessionCreated(new HttpSessionEvent(this));
        }
    }

    /**
     * This method sets the session id.
     */
    public void setSessionId(String id) {
        this.id = id;
    }

    /**
     * Sets container id and distributor id for the session.
     * @param contId Container id
     * @param distribId Distributor id
     */
    public void setComponentsId(String contId, String distribId) {
        this.id = id + "$" + contId + "$_" + distribId;
    }

    /**
     * This method deserialize session from is.
     * @param is ObjectInputStream.
     * @exception Exception Throws if any errors occures during desirealization.
     */
    public void readObject(ObjectInputStream is) throws Exception {
        attributes = (Hashtable) is.readObject();
        sessionActListeners = (Hashtable) is.readObject();
        Enumeration enum = sessionActListeners.keys();
        while (enum.hasMoreElements()) {
            String key = (String) enum.nextElement();
            ((HttpSessionActivationListener) sessionActListeners.get(key)).sessionDidActivate(new HttpSessionEvent(this));
        }
    }

    /**
     * This method serialize session to the OutputStream.
     * @param os OutputStream.
     * @exception Exception Throws if any errors occures during serialization.
     */
    public void writeObject(OutputStream os) throws Exception {
        Thread.currentThread().setContextClassLoader(context.getServletLoader());
        Enumeration enum = sessionActListeners.keys();
        while (enum.hasMoreElements()) {
            String key = (String) enum.nextElement();
            ((HttpSessionActivationListener) sessionActListeners.get(key)).
                    sessionWillPassivate(new HttpSessionEvent(this));
        }
        ObjectOutputStream stream = new ObjectOutputStream(os);
        stream.writeObject(attributes);
        stream.writeObject(sessionActListeners);
    }

    public ServletContext getServletContext() {
        return context;

    }

    /**
     * This method reurns true if session was modified since last time
     * serialization.
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * This method sets <i>modified</i> flag on the session.
     * @param modified Modification flag.
     */
    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public Object getAttribute(String name) {
        if (!valid)
            throw new IllegalStateException("Session is not valid");
        modified = true;
        return attributes.get(name);
    }

    public Enumeration getAttributeNames() {
        if (!valid)
            throw new IllegalStateException("Session is not valid");
        return attributes.keys();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getId() {
        return id;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    protected long getRealLastAccessedTime() {
        return newAccessedTime;
    }

    public void touch() {
        isNew = false;
        lastAccessedTime = newAccessedTime;
        newAccessedTime = System.currentTimeMillis();
    }

    public int getMaxInactiveInterval() {
        if (!valid)
            throw new IllegalStateException("Session is not valid");
        return inactiveInterval;
    }

    public HttpSessionContext getSessionContext() {
        return null;
    }

    public Object getValue(String name) {
        return getAttribute(name);
    }

    public String[] getValueNames() {
        String[] result = new String[attributes.size()];
        Enumeration enum = getAttributeNames();
        for (int i = 0; i < result.length; i++) {
            result[i] = (String) enum.nextElement();
        }
        return result;
    }

    /**
     * This method invalidate session.
     * @exception IllegalArgumentException Throws if session is not valid.
     */
    public void invalidate() {
        if (!valid)
            throw new IllegalStateException("Session is not valid");
        Enumeration enum = getAttributeNames();
        while (enum.hasMoreElements()) {
            removeAttribute((String) enum.nextElement());
        }
        valid = false;
        for (int i = sessionListeners.size() - 1; i >= 0; i--) {
            ((HttpSessionListener) sessionListeners.get(i)).
                    sessionDestroyed(new HttpSessionEvent(this));
        }
    }

    public boolean isNew() {
        if (!valid)
            throw new IllegalStateException("Session is not valid");
        return isNew;
    }

    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    public void removeAttribute(String name) {
        sessionActListeners.remove(name);
        Object o = getAttribute(name);
        attributes.remove(name);
        if (o != null) {
            modified = true;
            if (o instanceof HttpSessionBindingListener) {
                ((HttpSessionBindingListener) o).valueUnbound(new HttpSessionBindingEvent(this, name, o));
            }
            for (int i = 0; i < sessionAttrListeners.size(); i++) {
                ((HttpSessionAttributesListener) sessionAttrListeners.get(i)).attributeRemoved(new HttpSessionBindingEvent(this, name, o));
            }
        }
    }

    public void removeValue(String name) {
        removeAttribute(name);
    }

    public void setAttribute(String name, Object o) {
        if (!valid)
            throw new IllegalStateException("Session is not valid");
        Object old = attributes.get(name);
        modified = true;
        if (old != null) {
            if (old instanceof HttpSessionBindingListener) {
                ((HttpSessionBindingListener) old).valueUnbound(new HttpSessionBindingEvent(this, name, old));
            }
            for (int i = 0; i < sessionAttrListeners.size(); i++) {
                ((HttpSessionAttributesListener) sessionAttrListeners.get(i)).attributeReplaced(new HttpSessionBindingEvent(this, name, o));
            }
        } else {
            for (int i = 0; i < sessionAttrListeners.size(); i++) {
                ((HttpSessionAttributesListener) sessionAttrListeners.get(i)).attributeAdded(new HttpSessionBindingEvent(this, name, o));
            }
        }
        if (backup) {
            if (!(o instanceof Serializable)) {
                throw new IllegalArgumentException("Cannot bind object that doesn't implement Serializable to session");
            }
        }
        attributes.put(name, o);
        if (o != null && o instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener) o).valueBound(new HttpSessionBindingEvent(this, name, o));
        }
        if (o instanceof HttpSessionActivationListener) {
            sessionActListeners.put(name, o);
        }
    }

    public void setMaxInactiveInterval(int interval) {
        inactiveInterval = interval;
    }

    public boolean isValid() {
        return valid;
    }

    public void setUser(Principal newUser) {
        this.user = newUser;
    }

    public Principal getUser() {
        return user;
    }

    public void setAuthType(int newAuthType) {
        this.authType = newAuthType;
    }

    public int getAuthType() {
        return authType;
    }

    public void saveUrl(String url) {
        savedUrl = url;
    }

    public void clearSavedUrl() {
        savedUrl = null;
    }

    public String getSavedUrl() {
        return savedUrl;
    }
}

