/*
 * Created by IntelliJ IDEA.
 * To change template for new class use
 * "Source Code" options (Tools | IDE Options), Templates tab.
 */
package com.azoft.nusuth.util;

public class AccessLogWrapper {

    public final static int TYPE_NONE = 0;
    public final static int TYPE_COMMON = 1;
    public final static int TYPE_EXTENDED = 2;

    private String logLocation = null;
    private int logType = AccessLogWrapper.TYPE_NONE;
    private boolean resolve = false;
    private AccessLogger category = null;

    public AccessLogWrapper(String logLocation, int logType, boolean resolve) {
        this.logLocation = logLocation;
        this.logType = logType;
        this.resolve = resolve;
    }

    public String getLogLocation() {
        return logLocation;
    }

    public int getLogType() {
        return logType;
    }

    public boolean isResolve() {
        return resolve;
    }

    public void setLogLocation(String logLocation) {
        this.logLocation = logLocation;
    }

    public void setLogType(int logType) {
        this.logType = logType;
    }

    public void setCategory(AccessLogger cat) {
        this.category = cat;
    }

    public AccessLogger getCategory() {
        return category;
    }

}
