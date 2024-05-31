package com.azoft.nusuth.core;

import java.util.*;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * This class represents chain of resources
 * @author skilz
 * @version 1.4
 * @since Nusuth1.0
 */
public class ResourceChain {

    private Stack filterWrappers = new Stack();
//  private Stack filterWrappersForServlet = new Stack();
    private String servletName = null;
    private String servletClass = null;
    private String servletPath = null;
    private boolean defaultServlet;
    private Servlet servlet = null;
    private ServletConfig conf = null;

    private LinkedList filters = null;

    public synchronized FilterChain getFilterChain() {
        if (filters == null) {
//      LinkedList result = new LinkedList();
            filters = new LinkedList();
            for (int i = 0; i < filterWrappers.size(); i++) {
                if (!((FilterMappingWrapper) filterWrappers.get(i)).isForServletName())
//          result.add(((FilterMappingWrapper)filterWrappers.get(i)).getFilter());
                    filters.add(((FilterMappingWrapper) filterWrappers.get(i)).getFilter());
            }
            for (int i = 0; i < filterWrappers.size(); i++) {
                if (((FilterMappingWrapper) filterWrappers.get(i)).isForServletName())
//          result.add(((FilterMappingWrapper)filterWrappers.get(i)).getFilter());
                    filters.add(((FilterMappingWrapper) filterWrappers.get(i)).getFilter());
            }
            if (servlet != null) {
//        result.add(servlet);
                filters.add(servlet);
            } else {
                try {
                    servlet = (Servlet) Class.forName(servletClass).newInstance();
                    servlet.init(conf);
//          result.add(servlet);
                    filters.add(servlet);
                } catch (Exception e) {
//          result.add(new ErrorServlet());
                    filters.add(new ErrorServlet());
                }
            }
//      return new NusuthFilterChain(result);
            return new NusuthFilterChain(filters);
        } else {
            return new NusuthFilterChain(filters);
        }
    }

    public void addWrapper(FilterMappingWrapper wrapper) {
        int len = filterWrappers.size();
        for (int i = 0; i < len; i++) {
            FilterMappingWrapper wr = (FilterMappingWrapper) filterWrappers.get(i);
            if (wrapper.getPlaceInDD() < wr.getPlaceInDD()) {
                filterWrappers.insertElementAt(wrapper, i);
                return;
            }
        }
        filterWrappers.add(wrapper);
    }

    public void addAll(ResourceChain chain) {
        Stack allWrappers = chain.getWrappers();
        for (int i = 0; i < allWrappers.size(); i++) {
            addWrapper((FilterMappingWrapper) allWrappers.get(i));
        }
    }

    public Stack getWrappers() {
        return filterWrappers;
    }

    public int getNumberOfFilters() {
        return filterWrappers.size();
    }

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }

    public void setServletConfig(ServletConfig conf) {
        this.conf = conf;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    public void setServlet(Servlet servlet) {
        this.servlet = servlet;
    }

    public String getServletName() {
        return servletName;
    }

    public String getServletPath() {
        return servletPath;
    }

    public boolean isDefaultServlet() {
        if (servletPath != null) {
            return servletPath.equals("/");
        } else {
            return true;
        }
    }

    public boolean isServletFound() {
        return (servletName != null);
    }

    public Servlet getServlet() {
        return servlet;
    }

}