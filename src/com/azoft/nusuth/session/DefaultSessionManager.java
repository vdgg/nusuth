package com.azoft.nusuth.session;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Enumeration;
import java.net.SocketException;
import java.io.*;
import javax.servlet.http.HttpSession;

import com.azoft.nusuth.core.NusuthContext;

/**
 * This implementats SessionManager interface and its destination is to manage
 * sessions work on stand-alone container.
 * @author skilz
 * @version 1.9
 * @since Nusuth1.0
 */
public class DefaultSessionManager
        implements SessionManager, SessionWorkOverListener {

    private Hashtable sessions = new Hashtable();
    private NusuthContext context;
    private String serDir = null;
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth."
            + "session");

    /**
     * Constructor for this class.
     * @param context NusuthContext
     */
    public DefaultSessionManager(NusuthContext context, boolean loadSessions) {
        (new Thread(new SessionCleaner(this))).start();
        this.context = context;
        if (context.getSessionBackup().equals("always")) {
            context.registerSessionWorkOverListener(this);
        }
        File sessionDir = new File(context.getWorkDir()
                + File.separator + context.getContextName());
        if (context.getSessionBackup().equals("always")
                || context.getSessionBackup().equals("shutdown")) {
            if (!sessionDir.exists()) {
                sessionDir.mkdir();
            }
        }
        serDir = sessionDir.getAbsolutePath();
        if (loadSessions) {
            if (sessionDir.exists()) {
                File[] files = sessionDir.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        String name = files[i].getName();
                        if (name.endsWith(".ser")) {
                            name = name.substring(0, name.indexOf(".ser"));
                            NusuthSession ses = (NusuthSession) createSession(name);
                            ObjectInputStream is = null;
                            try {
                                is = new ObjectInputStream(
                                        new FileInputStream(files[i]));
                                ses.readObject(is);
                                is.close();
                                if (!files[i].delete()) {
                                    cat.warn("Cannot remove file " + files[i].getAbsolutePath()
                                            + " with session");
                                }
                                sessions.put(name, ses);
                            } catch (Exception e) {
                                cat.error("Cannot read session", e);
                            } finally {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method create session and set it id to <i>id</i>
     * @param id Session id.
     * @return HttpSession created session.
     */
    public HttpSession createSession(String id) {
        NusuthSession session = new NusuthSession(context.getSessionListeners(),
                context.getSessionAttrListeners(),
                context);
        session.setSessionId(id);
        sessions.put(session.getId(), session);
        session.setMaxInactiveInterval(context.getSessionTimeOut());
        return session;
    }

    /**
     * This method serialize session to hard drive into the working directory.
     * It invoked then request processing finished.
     * @param sessionId Session id that ends working.
     */
    public void onSessionWorkOver(String sessionId) throws SocketException {

        NusuthSession session = (NusuthSession) getSession(sessionId);
        if (session.isModified()) {
            String serFileName = serDir + File.separator + sessionId + ".ser";
            File file = new File(serFileName);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream os = new FileOutputStream(file);
                session.writeObject(os);
                os.close();
            } catch (Exception e) {
                cat.error("Cannot serrialize session", e);
            }
            session.setModified(false);
        }
    }

    /**
     * This method invoke when session-backup element changed.
     */
    public void sessionBackupChanged() {
        context.deleteSessionWorkOverListener(this);
        if (context.getSessionBackup().equals("always")) {
            context.registerSessionWorkOverListener(this);
        }
        File sessionDir = new File(context.getWorkDir()
                + File.separator + context.getContextName());
        if (context.getSessionBackup().equals("always")
                || context.getSessionBackup().equals("shutdown")) {
            if (!sessionDir.exists()) {
                sessionDir.mkdir();
            }
        }
    }

    /**
     * This method do nothing.
     * @return false.
     */
    public boolean changeId(String sessionId, String containerId) {
        return false;
    }

    /**
     * This method do nothing.
     * @param sessionId SessionId.
     */
    public void changeRemoteId(String sessionId) {
    }

    /**
     * This method create instance of NusuthSession
     * @return created session
     */
    public HttpSession createSession() {
        NusuthSession session = new NusuthSession(context.getSessionListeners(),
                context.getSessionAttrListeners(),
                context);
        if (context.getDistributorId() != null) {
            session.setComponentsId(context.getContainerId(),
                    context.getDistributorId());
        } else {
            session.setComponentsId(context.getContainerId(), "");
        }
        sessions.put(session.getId(), session);
        return session;
    }

    /**
     * This method return session that corresponds to the given id on null if
     * there is no session with given id.
     * @param sessionId Session id.
     * @return HttpSession Requested session.
     */
    public HttpSession getSession(String sessionId) {
        return (HttpSession) sessions.get(sessionId);
    }

    /**
     * This method remove session that corresponds to the given id or do nothing
     * if there is no session with this id.
     * @param sessionId Session id.
     * @return HttpSession removed session.
     */
    public HttpSession removeSession(String sessionId) {
        if (!context.isShuttingDown()) {
            if (serDir != null) {
                String fileName = serDir + File.separator + sessionId + ".ser";
                File file = new File(fileName);
                if (file.exists()) {
                    if (!file.delete()) {
                        cat.warn("Cannot remove file " + file.getAbsolutePath()
                                + " with session");
                    }
                }
            }
        } else if (context.getSessionBackup().equals("shutdown")) {
            try {
                onSessionWorkOver(sessionId);
            } catch (SocketException e) {
            }
        }
        return (HttpSession) sessions.remove(sessionId);
    }

    public Enumeration getSessionsKeys() {
        return sessions.keys();
    }

    public int getCurrentSessionSize() {
        return sessions.size();
    }

}
