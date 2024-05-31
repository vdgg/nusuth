package com.azoft.nusuth.management.security;

import java.security.Principal;
import java.security.acl.*;
import java.util.*;

import sun.security.acl.AclImpl;

/** Implementation of Access Control List
 *  Only positive permissions allowed. Negative permissions ignored.
 */
public class NusuthAclImpl extends AclImpl implements Acl {
    public NusuthAclImpl(Principal principal, String string) {
        super(principal, string);
    }

    public boolean checkPermission(Principal principal, Permission permission) {
        for (Enumeration i = getPermissions(principal); i.hasMoreElements();) {
            Permission p = (Permission) i.nextElement();
            if (p.equals(permission))
                return true;
        }
        return false;
    }

    private void getPerms(Principal principal, Vector result) {
        for (Enumeration i = entries(); i.hasMoreElements();) {
            AclEntry entry = (AclEntry) i.nextElement();
            if (!entry.isNegative() && entry.getPrincipal().equals(principal)) {
                for (Enumeration j = entry.permissions(); j.hasMoreElements();) {
                    Permission perm = (Permission) j.nextElement();
                    result.add(perm);
                }
            }
        }
    }

    private void getGroupPerms(Principal principal, Vector result) {
        for (Enumeration i = entries(); i.hasMoreElements();) {
            AclEntry entry = (AclEntry) i.nextElement();
            if (!entry.isNegative() && entry.getPrincipal() instanceof Group && ((Group) entry.getPrincipal()).isMember(principal)) {
                for (Enumeration j = entry.permissions(); j.hasMoreElements();) {
                    Permission perm = (Permission) j.nextElement();
                    result.add(perm);
                }
                getGroupPerms(entry.getPrincipal(), result);
            }
        }
    }

    /** return set of allowed permissions.
     *  Denied permissions simply ignore.
     */
    public Enumeration getPermissions(Principal principal) {
        Vector result = new Vector();
        Principal curr = principal;
        //
        getPerms(curr, result);
        getGroupPerms(curr, result);
        return result.elements();
    }
}
