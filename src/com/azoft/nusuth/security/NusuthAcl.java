package com.azoft.nusuth.security;

import java.security.acl.*;
import java.security.Principal;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Hashtable;
import java.io.Serializable;

public class NusuthAcl extends NusuthOwner implements Acl, Serializable {

    private String aclName = null;
    private Hashtable principal2entry = new Hashtable();

    /**
     * Constructor for NusuthAcl.
     * @param owner Owner of this Acl.
     */
    public NusuthAcl(Principal owner) {
        owners.add(owner);
    }

    /**
     * Sets the name of this ACL.
     *
     * @param caller the principal invoking this method. It must be an
     * owner of this ACL.
     *
     * @param name the name to be given to this ACL.
     *
     * @exception NotOwnerException if the caller principal
     * is not an owner of this ACL.
     */
    public void setName(Principal caller, String name)
            throws NotOwnerException {
        checkOwner(caller);
        aclName = name;
    }

    /**
     * Returns the name of this ACL.
     *
     * @return the name of this ACL.
     */
    public String getName() {
        return aclName;
    }

    /**
     * Adds an ACL entry to this ACL. An entry associates a principal
     * (e.g., an individual or a group) with a set of
     * permissions. Each principal can have at most one positive ACL
     * entry (specifying permissions to be granted to the principal)
     * and one negative ACL entry (specifying permissions to be
     * denied). If there is already an ACL entry of the same type
     * (negative or positive) already in the ACL, false is returned.
     *
     * @param caller the principal invoking this method. It must be an
     * owner of this ACL.
     *
     * @param entry the ACL entry to be added to this ACL.
     *
     * @return true on success, false if an entry of the same type
     * (positive or negative) for the same principal is already
     * present in this ACL.
     *
     * @exception NotOwnerException if the caller principal
     *  is not an owner of this ACL.
     */
    public boolean addEntry(Principal caller, AclEntry entry)
            throws NotOwnerException {
        checkOwner(caller);
        Principal princ = entry.getPrincipal();
        if (principal2entry.containsKey(princ)) {
            return false;
        } else {
            principal2entry.put(princ, entry);
            return true;
        }
    }

    /**
     * Removes an ACL entry from this ACL.
     *
     * @param caller the principal invoking this method. It must be an
     * owner of this ACL.
     *
     * @param entry the ACL entry to be removed from this ACL.
     *
     * @return true on success, false if the entry is not part of this ACL.
     *
     * @exception NotOwnerException if the caller principal is not
     * an owner of this Acl.
     */
    public boolean removeEntry(Principal caller, AclEntry entry)
            throws NotOwnerException {
        checkOwner(caller);
        Principal princ = entry.getPrincipal();
        if (principal2entry.contains(princ)) {
            principal2entry.remove(princ);
            return true;
        }
        return false;
    }

    /**
     * Returns an enumeration for the set of allowed permissions for the
     * specified principal (representing an entity such as an individual or
     * a group). This set of allowed permissions is calculated as
     * follows:<p>
     *
     * <ul>
     *
     * <li>If there is no entry in this Access Control List for the
     * specified principal, an empty permission set is returned.<p>
     *
     * <li>Otherwise, the principal's group permission sets are determined.
     * (A principal can belong to one or more groups, where a group is a
     * group of principals, represented by the Group interface.)
     * The group positive permission set is the union of all
     * the positive permissions of each group that the principal belongs to.
     * The group negative permission set is the union of all
     * the negative permissions of each group that the principal belongs to.
     * If there is a specific permission that occurs in both
     * the positive permission set and the negative permission set,
     * it is removed from both.<p>
     *
     * The individual positive and negative permission sets are also
     * determined. The positive permission set contains the permissions
     * specified in the positive ACL entry (if any) for the principal.
     * Similarly, the negative permission set contains the permissions
     * specified in the negative ACL entry (if any) for the principal.
     * The individual positive (or negative) permission set is considered
     * to be null if there is not a positive (negative) ACL entry for the
     * principal in this ACL.<p>
     *
     * The set of permissions granted to the principal is then calculated
     * using the simple rule that individual permissions always override
     * the group permissions. That is, the principal's individual negative
     * permission set (specific denial of permissions) overrides the group
     * positive permission set, and the principal's individual positive
     * permission set overrides the group negative permission set.
     *
     * </ul>
     *
     * @param user the principal whose permission set is to be returned.
     *
     * @return the permission set specifying the permissions the principal
     * is allowed.
     */
    public Enumeration getPermissions(Principal user) {
        if (!principal2entry.containsKey(user)) {
            return (new Hashtable()).elements();
        } else {
            return ((AclEntry) principal2entry.get(user)).permissions();
        }
    }

    /**
     * Returns an enumeration of the entries in this ACL. Each element in
     * the enumeration is of type AclEntry.
     *
     * @return an enumeration of the entries in this ACL.
     */
    public Enumeration entries() {
        return principal2entry.elements();
    }

    /**
     * Checks whether or not the specified principal has the specified
     * permission. If it does, true is returned, otherwise false is returned.
     *
     * More specifically, this method checks whether the passed permission
     * is a member of the allowed permission set of the specified principal.
     * The allowed permission set is determined by the same algorithm as is
     * used by the <code>getPermissions</code> method.
     *
     * @param principal the principal, assumed to be a valid authenticated
     * Principal.
     *
     * @param permission the permission to be checked for.
     *
     * @return true if the principal has the specified permission, false
     * otherwise.
     *
     * @see #getPermissions
     */
    public boolean checkPermission(Principal principal, Permission permission) {
        if (isOwner(principal)) {
            return true;
        }
        Enumeration enum = getPermissions(principal);
        while (enum.hasMoreElements()) {
            if (((Permission) enum.nextElement()).equals(permission)) {
                return true;
            }
        }
        return false;
    }

    private void checkOwner(Principal owner) throws NotOwnerException {
        if (!owners.contains(owner)) {
            throw new NotOwnerException();
        }
    }

    /**
     * Returns a string representation of the object.
     * @return  a string representation of the object.
     */
    public String toString() {
        return super.toString();
    }

}
