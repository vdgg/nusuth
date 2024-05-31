package com.azoft.nusuth.jidep;

import com.azoft.nusuth.jndi.ContextChangingListener;

import java.util.LinkedList;
import java.io.ObjectOutputStream;
import java.io.IOException;

/**
 * This class implements ContextChangingListener for sending notifications
 * about context changing via JIDEP protocol.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class JidepNotificationListener implements ContextChangingListener {

    private ServerJidepAdapter adapter = null;
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jidep");

    /**
     * Constructor.
     * @param adapter Server side of JidepProtocolAdapter.
     */
    public JidepNotificationListener(ServerJidepAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Adds context name. If later context with this name changed, notification
     * about this changing sends.
     * @param name Context name.
     */

    /**
     * Send Notification instance to the client via JIDEP protocol.
     * @param contextName Context name.
     * @param chngeName Name of changes.
     * @param changingType Type of change.
     */
    public void onContextChanged(String contextName, String changeName,
                                 int changingType) {
        JidepNotification notif
                = new JidepNotification(changeName, contextName, changingType);
        try {
            ObjectOutputStream oos
                    = new ObjectOutputStream(adapter.getOutputStream());
            oos.writeObject("notification");
            oos.writeObject(notif);
            adapter.endResponse();
            adapter.cleanup();
        } catch (IOException e) {
            cat.error("Can't notify about context chandes", e);
        }
    }

}
