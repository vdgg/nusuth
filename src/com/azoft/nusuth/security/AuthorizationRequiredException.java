package com.azoft.nusuth.security;

public class AuthorizationRequiredException extends Exception {

    public AuthorizationRequiredException() {
        super();
    }

    public AuthorizationRequiredException(String message) {
        super(message);
    }

}
