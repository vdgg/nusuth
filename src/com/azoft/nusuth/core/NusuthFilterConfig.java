/*
 * Created by IntelliJ IDEA.
 * To change template for new class use
 * "Source Code" options (Tools | IDE Options), Templates tab.
 */
package com.azoft.nusuth.core;

import javax.servlet.*;
import java.util.*;

public class NusuthFilterConfig extends NusuthConfig implements FilterConfig {

    public NusuthFilterConfig(Hashtable initParameters, String filterName, ServletContext context) {
        super(initParameters, filterName, context);
    }

    public String getFilterName() {
        return name;
    }

}