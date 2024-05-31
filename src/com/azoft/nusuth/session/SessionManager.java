package com.azoft.nusuth.session;

import javax.servlet.http.HttpSession;
import java.util.LinkedList;
import java.util.Enumeration;

public interface SessionManager {

    public HttpSession createSession();

    public HttpSession getSession(String sessionId);

    public HttpSession removeSession(String sessionId);

    public Enumeration getSessionsKeys();

    public int getCurrentSessionSize();

}
