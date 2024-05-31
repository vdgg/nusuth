package com.azoft.nusuth.core;

import javax.servlet.*;
import java.util.*;

/**This class is used to hold servlet parameters such as it's name,
 * init parameters, servlet class and servlet context.
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
public class NusuthServletConfig extends NusuthConfig implements ServletConfig {
    private ClassOrJsp servletClass;

    /**This is constructor of the NusuthServletConfig class.
     * @param initParameters hashtable that consist of the servlet init parameters, their names(String)and associated values(String).
     * @param context web application context where this servlet belongs.
     * @param name servlet name.
     * @param servletClass servlet class name.
     */
    public NusuthServletConfig(Hashtable initParameters, ServletContext context, String name, ClassOrJsp servletClass) {
        super(initParameters, name, context);
        this.servletClass = servletClass;
    }

    /**
     * @return servlet name.
     */
    public String getServletName() {
        return name;
    }

    /**
     * @return servlet class name.
     */
    public String getServletClass() {
        return servletClass.getName();
    }

    public boolean isForJsp() {
        return servletClass.isJsp();
    }

}

