package com.azoft.nusuth.security;

import java.security.acl.AclEntry;
import java.security.acl.Permission;
import java.security.Principal;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Hashtable;
import java.io.Serializable;

public class NusuthAclEntry implements AclEntry, Serializable {

    private LinkedList permissions = new LinkedList();
    private Principal principal = null;

    /**
     * Specifies the principal for which permissions are granted or denied
     * by this ACL entry. If a principal was already set for this ACL entry,
     * false is returned, otherwise true is returned.
     *
     * @param user the principal to be set for this entry.
     *
     * @return true if the principal is set, false if there was
     * already a principal set for this entry.
     */
    public boolean setPrincipal(Principal user) {
        if (principal != null) {
            return false;
        } else {
            principal = user;
            return true;
        }
    }

    /**
     * Returns the principal for which permissions are granted or denied by
     * this ACL entry. Returns null if there is no principal set for this
     * entry yet.
     *
     * @return the principal associated with this entry.
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * Sets this ACL entry to be a negative one. That is, the associated
     * principal (e.g., a user or a group) will be denied the permission set
     * specified in the entry.
     *
     * Note: ACL entries are by default positive. An entry becomes a
     * negative entry only if this <code>setNegativePermissions</code>
     * method is called on it.
     */
    public void setNegativePermissions() {
    }

    /**
     * Returns true if this is a negative ACL entry (one denying the
     * associated principal the set of permissions in the entry), false
     * otherwise.
     *
     * @return true if this is a negative ACL entry, false if it's not.
     */
    public boolean isNegative() {
        return false;
    }

    /**
     * Adds the specified permission to this ACL entry. Note: An entry can
     * have multiple permissions.
     *
     * @param permission the permission to be associated with
     * the principal in this entry.
     *
     * @return true if the permission was added, false if the
     * permission was already part of this entry's permission set.
     */
    public boolean addPermission(Permission permission) {
        if (permissions.contains(permission)) {
            return false;
        } else {
            permissions.add(permission);
            return true;
        }
    }

    /**
     * Removes the specified permission from this ACL entry.
     *
     * @param permission the permission to be removed from this entry.
     *
     * @return true if the permission is removed, false if the
     * permission was not part of this entry's permission set.
     */
    public boolean removePermission(Permission permission) {
        if (!permissions.contains(permission)) {
            return false;
        } else {
            permissions.remove(permission);
            return true;
        }
    }

    /**
     * Checks if the specified permission is part of the
     * permission set in this entry.
     *
     * @param permission the permission to be checked for.
     *
     * @return true if the permission is part of the
     * permission set in this entry, false otherwise.
     */
    public boolean checkPermission(Permission permission) {
        return permissions.contains(permission);
    }

    /**
     * Returns an enumeration of the permissions in this ACL entry.
     *
     * @return an enumeration of the permissions in this ACL entry.
     */
    public Enumeration permissions() {
        Hashtable table = new Hashtable();
        for (int i = 0; i < permissions.size(); i++) {
            table.put(permissions.get(i), permissions.get(i));
        }
        return table.keys();
    }

    public Object clone() {
        NusuthAclEntry newEntry = new NusuthAclEntry();
        for (int i = 0; i < permissions.size(); i++) {
            newEntry.addPermission((Permission) permissions.get(i));
        }
        newEntry.setPrincipal(principal);
        return newEntry;
    }

    public String toString() {
        return super.toString();
    }
}
