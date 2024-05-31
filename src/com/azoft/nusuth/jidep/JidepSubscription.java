package com.azoft.nusuth.jidep;

import java.io.Serializable;

/**
 * This class is wrapper for subsription.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class JidepSubscription implements Serializable {

    /** Subscribed contexts */
    public String[] contexts = null;

    /**
     * Constructor.
     * @param role Subscriber role.
     * @param contexts Contexts to subscribe
     */
    public JidepSubscription(String[] contexts) {
        this.contexts = contexts;
    }

}
