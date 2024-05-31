package com.azoft.nusuth.jidep;

import java.io.Serializable;

/**
 * This class used for sending notifications of jndi tree changing.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class JidepNotification implements Serializable {

    public String name = null;
    public String contextName = null;
    public int notificationType = -1;

    /**
     * Constructor.
     * @param name Name of Context that was changed.
     * @param type Type of modification.
     */
    public JidepNotification(String name, String contextName, int type) {
        this.name = name;
        this.contextName = contextName;
        this.notificationType = type;
    }

}
