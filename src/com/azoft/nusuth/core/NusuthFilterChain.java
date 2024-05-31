package com.azoft.nusuth.core;

import javax.servlet.*;
import java.util.*;

/**
 * This class implements FilterChain interface.
 * @author skilz
 * @version 1.2
 * @since Nusuth1.0
 */
public class NusuthFilterChain implements FilterChain {

    private LinkedList filters;
    private int number = -1;

    private NusuthFilterChain(LinkedList filters, int number) {
        this.filters = filters;
        this.number = number;
    }

    public NusuthFilterChain(LinkedList filters) {
        this.filters = filters;
        this.number = 0;
    }

    public void doFilter(ServletRequest request, ServletResponse response)
            throws java.io.IOException, ServletException {
        if ((number + 1) < filters.size()) {
//    if (filters.size() >1) {
//      Filter filter = (Filter)filters.removeFirst();
            Filter filter = (Filter) filters.get(number);
//      filter.doFilter(request, response, new NusuthFilterChain(filters,
//                                                              number+1));
            number++;
            filter.doFilter(request, response, this);
        } else {
//      Servlet servlet = (Servlet)filters.removeFirst();
            Servlet servlet = (Servlet) filters.get(number);
            servlet.service(request, response);
        }
    }

}