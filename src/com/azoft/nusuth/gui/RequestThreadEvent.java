package com.azoft.nusuth.gui;

import java.util.EventObject;

import com.azoft.nusuth.management.security.UnauthorizedAccessException;

public class RequestThreadEvent extends EventObject {
    private boolean success;
    private boolean hasUnauthorizedAccessException;
    private Exception exception;

    public RequestThreadEvent(Object source, boolean success, boolean hasUnauthorizedAccessException, Exception exception) {
        super(source);
        this.success = success;
        this.hasUnauthorizedAccessException = hasUnauthorizedAccessException;
        this.exception = exception;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isUnauthorized() {
        return hasUnauthorizedAccessException;
    }

    public Exception getException() {
        return exception;
    }
}