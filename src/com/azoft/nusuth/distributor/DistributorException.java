package com.azoft.nusuth.distributor;

import java.io.IOException;

class DistributorException extends IOException {
    private Throwable nested = null;
    private int errorCode = DistributorRequestAdapter.OK;


    /**
     * DistributorException constructor comment.
     * @param s java.lang.String
     */
    public DistributorException(String s, Throwable nested, int errorCode) {
        super(s);
        this.nested = nested;
        this.errorCode = errorCode;
    }


    /**
     * Insert the method's description here.
     * Creation date: (02.02.2001 0:12:50)
     * @return int
     */
    public int getErrorCode() {
        return errorCode;
    }


    /**
     * Insert the method's description here.
     * Creation date: (01.02.2001 23:47:04)
     * @return java.lang.Throwable
     */
    public java.lang.Throwable getNested() {
        return nested;
    }


    public String toString() {
        return super.toString() + (nested == null ? "" : ", nested: " + nested.toString());
    }
}
