package com.azoft.nusuth.jidep;

import java.util.Hashtable;

/**
 * This class represent session for JIDEP requests.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class JidepSession {

    private Hashtable attributes = new Hashtable();

    /**
     * This method set attribute to the session.
     * @param name Attribute name
     * @param value Attribute value
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * This method return attribute value if it has been set with the given name,
     * or null if it has not be set.
     * @param name Attribute name
     * @return Object Value of attribute associated with the given name.
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * This method removed attribute value if it has been set with the given name.
     * @param name Attribute name
     * @return Object Value of attribute associated with the given name.
     */
    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }

}
