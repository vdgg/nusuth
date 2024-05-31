package com.azoft.nusuth.management.security;

/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/

import sun.security.acl.PrincipalImpl;

import java.security.Principal;
import java.security.SecureRandom;
import java.io.Serializable;

public class Session implements Serializable {

    private String user;
    static private SecureRandom random;

    private long lastAccess = System.currentTimeMillis();

    static {
        try {
            random = java.security.SecureRandom.getInstance("SHA1PRNG");
        } catch (java.security.NoSuchAlgorithmException nsaex) {
            throw new RuntimeException("No Such Random Algorithm Exception");
        }
    }

    protected String sessionID;


    /** Session constructor comment. */
    Session(String user) {

        super();

        this.user = user;
        sessionID = "sessionid: " + user + " " + random.nextInt();
    }


    /**
     * Insert the method's description here. Creation date: (02.12.00 21:11:38)
     * @return <{String}>
     */
    String getId() {
        return sessionID;
    }


    protected long getLastAccess() {
        return lastAccess;
    }


    /**
     * Insert the method's description here. Creation date: (02.12.00 21:06:56)
     * @return <{java.security.Principal}>
     */
    Principal getUser() {
        return new PrincipalImpl(user);
    }


    protected void touch() {
        lastAccess = System.currentTimeMillis();
    }
}