package com.azoft.nusuth.security;

import java.security.acl.Permission;
import java.io.Serializable;

/**
 * This class implements Permission interface
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class NusuthPermission implements Permission, Serializable {

    private String permission = null;

    /**
     * Constructor for this class.
     * @param permission Permission ("read" or "write")
     */
    public NusuthPermission(String permission) {
        if (permission == null
                || (!permission.equals("read") && !permission.equals("write"))) {
            throw new IllegalArgumentException("Only \"read\" or \"write\" values "
                    + "supported");
        }
        this.permission = permission;
    }

    /**
     * Return true if given object equals to current.
     * @param obj Object with which to compare.
     * @return true if given object equals to current.
     */
    public boolean equals(Object obj) {
        if (obj instanceof NusuthPermission) {
            NusuthPermission per = (NusuthPermission) obj;
            return per.toString().equals(permission);
        }
        return false;
    }

    /**
     * Return string representation of this object.
     * @return String representation of this object.
     */
    public String toString() {
        return permission;
    }
}
