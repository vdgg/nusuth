package com.azoft.nusuth.webappsecurity.impl;

import java.security.*;
import java.util.Set;

import com.azoft.nusuth.webappsecurity.WebAppSecurityManager;

class ResourceConstraint {
    boolean isSSLNeeded;
    Set users;

    ResourceConstraint(boolean isSSLNeeded, Set users) {
        this.isSSLNeeded = isSSLNeeded;
        this.users = users;
    }

    public int checkRequest(boolean isSecure, Principal user) {
        if (!isSSLNeeded || isSecure) {
            if (users != null) {
                if (user != null) {
                    if (users.contains(user))
                        return WebAppSecurityManager.ACCESS_GRANTED;
                    else
                        return WebAppSecurityManager.ACCESS_DENIED;
                } else
                    return WebAppSecurityManager.ACCESS_AUTENTICATION_NEEDED;
            } else
                return WebAppSecurityManager.ACCESS_GRANTED;
        } else
            return WebAppSecurityManager.ACCESS_DENIED;
    }
}

