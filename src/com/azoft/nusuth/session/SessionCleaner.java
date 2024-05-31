package com.azoft.nusuth.session;

import com.azoft.nusuth.container.NusuthRequestHandler;

import java.util.*;
import javax.servlet.http.HttpSession;

/**
 * @author vdgg, skilz
 * @version 1.6
 * @since Nusuth1.0
 */
public class SessionCleaner implements Runnable {
    SessionManager sm;

    public SessionCleaner(SessionManager sm) {
        this.sm = sm;
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException iexc) {
            }
            long currentTime = System.currentTimeMillis();
            Enumeration keys = sm.getSessionsKeys();
            while (keys.hasMoreElements()) {
                String sessId = (String) keys.nextElement();
                NusuthSession sess = (NusuthSession) sm.getSession(sessId);
                if (!sess.isValid()) {
                    sm.removeSession(sessId);
                    NusuthRequestHandler.removeEpiredSession(sess);
                    continue;
                }
                if ((currentTime - sess.getRealLastAccessedTime()
                        > sess.getMaxInactiveInterval() * 1000 || !sess.isValid())
                        && (sess.getMaxInactiveInterval() != -1)) {
                    try {
                        sess.invalidate();
                    } catch (Exception ex) {
                    }
                    sm.removeSession(sessId);
                    NusuthRequestHandler.removeEpiredSession(sess);
                }
            }
        }
    }
}

