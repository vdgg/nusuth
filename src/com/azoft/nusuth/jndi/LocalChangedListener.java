package com.azoft.nusuth.jndi;

import com.azoft.nusuth.jidep.JidepNotification;

import java.util.List;
import java.util.LinkedList;

public class LocalChangedListener implements ContextChangingListener {

    private DistributedJNDIContextListener listener = null;

    public LocalChangedListener(DistributedJNDIContextListener listener) {
        this.listener = listener;
    }

    /**
     * This method invoke if context with given name changed.
     * @param contextName Name of changed context.
     * @param changeName Name of changes.
     * @changingType Type of changing.
     */
    public void onContextChanged(String contextName, String changeName,
                                 int changingType) {
        listener.notify(new JidepNotification(changeName, contextName, changingType));
    }

}
