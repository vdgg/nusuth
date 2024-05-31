package com.azoft.nusuth.management.security;

import java.security.acl.Permission;

public class NusuthPermission implements Permission {

    protected String componentId;
    protected String componentType;
    protected String action;
    public final static String ANY = "*";


    public NusuthPermission(String componentType, String componentId, String action) {
        this.componentType = componentType;
        this.componentId = componentId;
        this.action = action;
    }


    public boolean equals(Object another) {
        if (!(another instanceof NusuthPermission)) {
            return false;
        }

        NusuthPermission a = (NusuthPermission) another;

        return (action.equals(a.action) || action.equals(ANY))
                && (componentType.equals(a.componentType) || componentType.equals(ANY))
                && (componentId.equals(a.componentId) || componentId.equals(ANY));
    }


    public String toString() {
        return '"' + componentType + ' ' + componentId + ' ' + action + '"';
    }


    public int hashCode() {
        return (componentType + '/' + componentId + '/' + action).hashCode();
    }
}
