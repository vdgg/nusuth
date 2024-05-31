/*
 * Created by IntelliJ IDEA.
 * To change template for new class use
 * "Source Code" options (Tools | IDE Options), Templates tab.
 */
package com.azoft.nusuth.core;

import javax.servlet.Filter;

public class FilterMappingWrapper {

    private Filter filter;
    private int placeInDD;
    private boolean isForServletName;
    private String mapping;

    public FilterMappingWrapper(Filter filter, int placeInDD, boolean isForServletName, String mapping) {
        this.filter = filter;
        this.placeInDD = placeInDD;
        this.isForServletName = isForServletName;
        this.mapping = mapping;
    }

    public Filter getFilter() {
        return filter;
    }

    public int getPlaceInDD() {
        return placeInDD;
    }

    public boolean isForServletName() {
        return isForServletName;
    }

    public String getMapping() {
        return mapping;
    }

}