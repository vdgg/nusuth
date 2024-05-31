package com.azoft.nusuth.jndi;

import java.io.IOException;

/**
 * This is an interface for listening Context changes
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public interface ContextChangingListener {

    /**
     * This method invoke if context with given name changed.
     * @param contextName Name of changed context.
     * @param changeName Name of changes.
     * @changingType Type of changing.
     */
    public void onContextChanged(String contextName, String changeName,
                                 int changingType);

}
