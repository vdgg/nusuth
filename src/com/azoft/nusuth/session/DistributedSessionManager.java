package com.azoft.nusuth.session;

import com.azoft.nusuth.core.NusuthContext;
import com.azoft.nusuth.core.ComponentDistributionListener;
import com.azoft.nusuth.core.RemoteContainer;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.net.SocketException;

/**
 * This class represent SessionManager for cluster.
 * @author skilz
 * @version 1.3
 * @since Nusuth1.0
 */
public class DistributedSessionManager implements SessionManager,
        SessionWorkOverListener, ComponentDistributionListener {

    private Hashtable distribId2remoteHosts = new Hashtable();
    private NusuthContext context;
    private Hashtable sessions = new Hashtable();
    private Hashtable sessionId2Remote = new Hashtable();
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft."
            + "nusuth.session");

    public DistributedSessionManager(NusuthContext context) {
        (new Thread(new SessionCleaner(this))).start();
        this.context = context;
        context.registerSessionWorkOverListener(this);
    }

    public void addRemoteContainer(String distributorId, RemoteContainer cont) {
        synchronized (distribId2remoteHosts) {
            if (distribId2remoteHosts.get(distributorId) == null) {
                LinkedList list = new LinkedList();
                list.add(cont);
                distribId2remoteHosts.put(distributorId, list);
            } else {
                ((List) distribId2remoteHosts.get(distributorId)).add(cont);
            }
        }
    }

    public void removeRemoteContainer(String distributorId,
                                      String containerName) {
        synchronized (distribId2remoteHosts) {
            List list = (List) distribId2remoteHosts.get(distributorId);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (((RemoteContainer) list.get(i)).getName().equals(containerName)) {
                        list.remove(i);
                    }
                }
            }
        }
    }

    public void onComponentDistributionChanged(RemoteContainer cont) {
    }

    public HttpSession createSession() {
        DistributedNusuthSession session
                = new DistributedNusuthSession(context.getSessionListeners(),
                        context.getSessionAttrListeners(),
                        context);
        RemoteContainer remote = chooseRemote();
        if (remote != null) {
            session.setComponentsId(context.getDistributorId(),
                    context.getContainerId(),
                    remote.getName());
        } else {
            session.setComponentsId(context.getDistributorId(),
                    context.getContainerId(), "");
        }
        sessions.put(session.getId(), session);
        if (remote != null)
            sessionId2Remote.put(session.getId(), remote);
        return session;
    }

    /**
     * This method create session and set it id to <i>id</i>
     * @param id Session id.
     * @return HttpSession created session.
     */
    public HttpSession createSession(String id) {
        DistributedNusuthSession session
                = new DistributedNusuthSession(context.getSessionListeners(),
                        context.getSessionAttrListeners(),
                        context);
        session.setSessionId(id);
        session.setMaxInactiveInterval(context.getSessionTimeOut());
        sessions.put(session.getId(), session);
        return session;
    }

    private RemoteContainer chooseRemote() {
        if (context.getDistributorId() == null)
            return null;
        synchronized (distribId2remoteHosts) {
            if (distribId2remoteHosts.get(context.getDistributorId()) == null) {
                return null;
            } else {
                List list = (List) distribId2remoteHosts.get(context.getDistributorId());
                int size = list.size();
                if (size > 0) size--;
                int containerNumber = (int) Math.round(Math.random() * size);
                return (list.size() == 0
                        ? null
                        : (RemoteContainer) list.get(containerNumber));
            }
        }
    }

    public HttpSession getSession(String sessionId) {
        return (HttpSession) sessions.get(sessionId);
    }

    /**
     * This method remove session with given id and tell remove it to another
     * container that stores this session.
     * @param sessionId Session id
     * @return HttpSession Removed session or null if session doesn't exist.
     */
    public HttpSession removeSession(String sessionId) {
        DistributedNusuthSession session
                = (DistributedNusuthSession) sessions.remove(sessionId);
        if (session != null) {
            String distrId = session.getDistributorId();
            RemoteContainer container
                    = (RemoteContainer) sessionId2Remote.get(sessionId);
            if (container != null) {
                try {
                    container.removeBackupSession(session.getId());
                } catch (Exception e) {
                    cat.error("Cannot update session", e);
                }
            }
        }
        return session;
    }

    public Enumeration getSessionsKeys() {
        return sessions.keys();
    }

    public int getCurrentSessionSize() {
        return sessions.size();
    }

    public boolean changeId(String sessionId, String containerId) {
        DistributedNusuthSession session
                = (DistributedNusuthSession) sessions.get(sessionId);
        if (!session.isOnLocal(containerId)) {
            sessions.remove(sessionId);
            String localId = session.getLocalId();
            RemoteContainer con = null;
            synchronized (distribId2remoteHosts) {
                List list = (List) distribId2remoteHosts.get(session.getDistributorId());
                for (int i = 0; i < list.size(); i++) {
                    RemoteContainer cont = (RemoteContainer) list.get(i);
                    if (cont.getName().equals(session.getLocalId())) {
                        list.remove(cont);
                        sessionId2Remote.remove(sessionId);
                    }
                }
                con = chooseRemote();
            }
            if (con != null) {
                session.changeId(con.getName());
                sessionId2Remote.put(session.getId(), con);
            } else {
                session.changeId("");
            }
            sessions.put(session.getId(), session);
            return true;
        }
        if (!session.hasRemote()) {
            synchronized (distribId2remoteHosts) {
                if (distribId2remoteHosts.
                        get((session.getDistributorId() == null
                        ? ""
                        : session.getDistributorId())) != null
                        && ((List) distribId2remoteHosts.get(
                                (session.getDistributorId() == null
                        ? ""
                        : session.getDistributorId()))).size() != 0) {
                    sessions.remove(sessionId);
                    RemoteContainer con = chooseRemote();
                    if (con != null) {
                        session.setRemoteId(con.getName());
                    } else {
                        session.setRemoteId("");
                    }
                    sessionId2Remote.put(session.getId(), con);
                    sessions.put(session.getId(), session);
                    return true;
                }
            }
        }
        return false;
    }

    public void changeRemoteId(String sessionId) {
        DistributedNusuthSession session =
                (DistributedNusuthSession) sessions.remove(sessionId);
        removeRemoteContainer(session.getDistributorId(), session.getRemoteId());
        RemoteContainer cont = chooseRemote();
        if (cont != null) {
            session.setRemoteId(cont.getName());
        } else {
            session.setRemoteId("");
        }
        sessions.put(session.getId(), session);
    }

    public void onSessionWorkOver(String sessionId) throws SocketException {
        DistributedNusuthSession session
                = (DistributedNusuthSession) sessions.get(sessionId);
        if (session.isModified()) {
            String distrId = session.getDistributorId();
            RemoteContainer container
                    = (RemoteContainer) sessionId2Remote.get(sessionId);
            if (container != null) {
                try {
                    container.updateBackupSession(session);
                } catch (SocketException ex) {
                    cat.warn("Remote host is down...");
                    throw ex;
                } catch (Exception e) {
                    cat.error("Cannot update session", e);
                }
            } else {
                cat.warn("No remote for this container");
            }
            session.setModified(false);
        }
    }

    public void addNewSession(DistributedNusuthSession session) {
        sessions.put(session.getId(), session);
    }

}