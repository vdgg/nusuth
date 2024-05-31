package com.azoft.nusuth.core;

import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;

/**
 * This wrapper used by XSLT Filter.
 * @author skilz
 * @version 1.1
 * @since Nusuth1.0
 */
public class NusuthXmlRequestWrapper extends HttpServletRequestWrapper {

    private long ifModSince = -1;

    /**
     * Constructor.
     * @param request Request to wrap.
     */
    public NusuthXmlRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * Sets "If-Modified-Since" value.
     * @param value Value.
     */
    public void setModifiedSince(long value) {
        ifModSince = value;
    }

    /**
     * Return ifModSince field if name equals "If-Modified-Since" or return
     * header of wrapped request
     * @param name Header name.
     */
    public long getDateHeader(String name) {
        if (name.equals("If-Modified-Since") && ifModSince != -1) {
            return ifModSince;
        } else {
            return super.getDateHeader(name);
        }
    }

}
