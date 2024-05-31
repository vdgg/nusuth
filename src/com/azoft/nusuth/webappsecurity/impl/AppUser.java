package com.azoft.nusuth.webappsecurity.impl;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import sun.security.acl.PrincipalImpl;

class AppUser {
    String name;
    String password;
    String[] certificateNames;
    Principal principal;

    /** allowed roles */
    Set roles;

    AppUser(String name, String password, String[] certificateNames) {
        this.name = name;
        this.password = password;
        this.certificateNames = certificateNames;
        roles = new HashSet();
        principal = new PrincipalImpl(name);
    }
}

