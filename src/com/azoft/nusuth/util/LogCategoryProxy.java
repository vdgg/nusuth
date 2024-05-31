package com.azoft.nusuth.util;

import org.apache.log4j.Category;

import java.util.Hashtable;

public class LogCategoryProxy {

    private Category category;

    private long lastInfoCheck;
    private boolean lastInfoValue;
    private Object infoLock = new Object();

    private long lastDebugCheck;
    private boolean lastDebugValue;
    private Object debugLock = new Object();

    private static Hashtable registeredProxies = new Hashtable();

    public LogCategoryProxy(Category category) {
        this.category = category;
        lastInfoValue = category.isInfoEnabled();
        lastInfoCheck = System.currentTimeMillis();
        lastDebugValue = category.isDebugEnabled();
        lastDebugCheck = System.currentTimeMillis();
    }

    public boolean isDebugEnabled() {
        if (System.currentTimeMillis() - lastDebugCheck > 5000) {
            synchronized (debugLock) {
                if (System.currentTimeMillis() - lastDebugCheck > 5000) {
                    lastDebugValue = category.isDebugEnabled();
                    lastDebugCheck = System.currentTimeMillis();
                }
            }
        }
        return lastDebugValue;
    }

    public boolean isInfoEnabled() {
        if (System.currentTimeMillis() - lastInfoCheck > 5000) {
            synchronized (infoLock) {
                if (System.currentTimeMillis() - lastInfoCheck > 5000) {
                    lastInfoValue = category.isInfoEnabled();
                    lastInfoCheck = System.currentTimeMillis();
                }
            }
        }
        return lastInfoValue;
    }

    public static LogCategoryProxy getInstance(String name) {
        synchronized (registeredProxies) {
            LogCategoryProxy newInstance = (LogCategoryProxy) registeredProxies.get(name);
            if (newInstance == null) {
                Category cat = Category.getInstance(name);
                if (cat == null) {
                    return null;
                }
                newInstance = new LogCategoryProxy(cat);
                registeredProxies.put(name, newInstance);
            }
            return newInstance;
        }
    }
}