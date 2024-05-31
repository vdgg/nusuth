/*
 * Created by IntelliJ IDEA.
 * To change template for new class use
 * "Source Code" options (Tools | IDE Options), Templates tab.
 */
package com.azoft.nusuth.core;

import java.util.Hashtable;
import java.util.Enumeration;
import javax.servlet.ServletContext;

public abstract class NusuthConfig {

    protected String name;
    protected Hashtable parameters;
    protected ServletContext context;


    public NusuthConfig(Hashtable initParameters, String name, ServletContext context) {
        this.name = name;
        this.parameters = initParameters;
        this.context = context;
    }

    public String getInitParameter(String s) {
        return (String) parameters.get(s);
    }

    public Enumeration getInitParameterNames() {
        return parameters.keys();
    }

    public ServletContext getServletContext() {
        return context;
    }

}