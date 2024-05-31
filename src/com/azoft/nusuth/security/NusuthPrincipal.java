package com.azoft.nusuth.security;

import java.security.Principal;
import java.io.Serializable;

/**
 * This class implements Principal interface.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class NusuthPrincipal implements Principal, Serializable {

    private String name = null;

    /**
     * Constructor for this class.
     * @param name Name of the Principal
     */
    public NusuthPrincipal(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this principal.
     * @return the name of this principal.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a hash code value for the object.
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this object is the same as the obj
     *          argument; <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (obj instanceof NusuthPrincipal) {
            NusuthPrincipal pr = (NusuthPrincipal) obj;
            return pr.getName().equals(name);
        }
        return false;
    }

    /**
     * Returns a string representation of the object.
     * @return  a string representation of the object.
     */
    public String toString() {
        return name;
    }
}
