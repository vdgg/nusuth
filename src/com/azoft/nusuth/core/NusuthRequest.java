package com.azoft.nusuth.core;

import javax.servlet.ServletRequest;
import java.util.Hashtable;
import java.util.Map;
import java.io.UnsupportedEncodingException;

import com.azoft.nusuth.webappsecurity.AuthenticationData;

/**The abstract class encapsulates all information from the client request.
 * It also represents methods that allow to work with the request data.
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
public abstract class NusuthRequest implements ServletRequest {

    protected String contextPath;
    protected String servletPath;
    protected String requestURI;
    protected String pathInfo;
    protected String queryString;
    private SessionCreationListener listener;
    private String currentSessionId;
    private String serverName;
    protected String encoding = null;

    public abstract Map getParameterMap();

    public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
        encoding = new String(enc.getBytes(enc));
    }


    /**This method gets the context path of the needed servlet.
     * @return the context path of the needed servlet.
     */
    public String getContextPath() {
        return contextPath;
    }

    /**This method sets the context path of the needed servlet.
     * @param contextPath the context path of the needed servlet.
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**This method gets the path that directly corresponds to the mapping which
     * activated this request. This path starts with a’/’ character.
     * @return the servlet path that directly corresponds to the mapping which activated this request.
     */
    public String getServletPath() {
        return servletPath;
    }

    /**This method sets the path that directly corresponds to the mapping which
     * activated this request.
     * @param contextPath the servlet path that directly corresponds to the mapping which activated this request.
     */
    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    /**This method gets request URI.
     * @return request URI.
     */
    public String getRequestURI() {
        return requestURI;
    }

    /**This method sets request URI.
     * @param request URI.
     */
    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    /**This method gets path Info.
     * @return path Info.
     */
    public String getPathInfo() {
        return pathInfo;
    }

    /**This method sets path Info.
     * @param path Info.
     */
    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    /**This method gets request query.
     * @return request query .
     */
    public String getQueryString() {
        return queryString;
    }

    /**This method sets request query.
     * @param request query .
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**This method gets the corresponding session ID.
     * @return requested session ID.
     */
    public abstract String getRequestedSessionId();

    /**This method gets the container ID of the corresponding servlet.
     * @return container ID.
     */
    public abstract String getRequestedContainerID();

    /**This method sets session creation listener object.
     * @return  session creation listener object.
     */
    public void setSessionCreationListener(SessionCreationListener listener) {
        this.listener = listener;
    }

    /**This method gets session creation listener object.
     * @return  session creation listener object.
     */
    public SessionCreationListener getSessionCreationListener() {
        return listener;
    }

    /**This method sets current session ID.
     * @param  session ID.
     */
    protected void setCurrentSessionId(String csid) {
        currentSessionId = csid;
    }

    /**This method gets current session ID. If there is no current session ID then it returnes
     * the requested session ID.
     * @return  session ID.
     */
    protected String getCurrentSessionId() {
        if (currentSessionId == null) {
            currentSessionId = getRequestedSessionId();
        }
        return currentSessionId;
    }

    /**
     * @return server name.
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * @param serverName server name.
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void cleanup() {
        contextPath = null;
        servletPath = null;
        requestURI = null;
        pathInfo = null;
        queryString = null;
        listener = null;
        currentSessionId = null;
        serverName = null;
        encoding = null;
    }

    protected abstract void addParameters(Hashtable parameters);

    protected abstract void removeParameters(Hashtable parameters);

    protected abstract void clearParameters();

    // added by igork

    protected abstract AuthenticationData getAuthenticationData();
}

