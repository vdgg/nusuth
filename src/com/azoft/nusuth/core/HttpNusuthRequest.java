package com.azoft.nusuth.core;

import com.azoft.nusuth.container.http.HttpProtocolAdapter;
import com.azoft.nusuth.gui.Base64;
import com.azoft.nusuth.util.HttpConstants;
import com.azoft.nusuth.util.NusuthHeaders;
import com.azoft.nusuth.util.StrBuffer;
import com.azoft.nusuth.webappsecurity.AuthenticationData;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.*;

/**This class represents the http request. It contains all methods
 * and attributes that allow to work with request and to hold it's data.
 * @author vdgg, skilz
 * @version 1.18
 * @since Nusuth1.0
 */
public class HttpNusuthRequest extends NusuthRequest implements HttpServletRequest {
    private static final String JBIRD_BASIC_AUTH = "BASIC";
    private static final String JBIRD_FORM_AUTH = "FORM";
    private static final String JBIRD_CLIENT_CERT_AUTH = "CERT-CLIENT";
    private static final String JBIRD_DIGEST_AUTH = "DIGEST";
    private static char[] AUTHORIZATION_HEADER_NAME = "Authorization".toCharArray();
    private static String AUTHORIZATION_HEADER_NAME_String = "Authorization";
    private boolean dispatched = false;
    private ServletInputStream stream;
    private int error = -1;
    private Hashtable attributes = new Hashtable();
    private NusuthHeaders headers;
    private Hashtable parameters = null;
    private Locale[] locales;
    private String protocol;
    private String pathTranslated;
    private String scheme;
    private String remoteAddr;
    private String remoteHost;
    private int serverPort;
    private NusuthContext context;
    private Cookie[] cookies;
    private String method;
    private int contentLength = -1;
    private HttpProtocolAdapter adapter;
    private boolean usingReader = false;
    private boolean usingInput = false;
    private StrBuffer reqURIStrBuffer = null;
    private String sessionIdFromURL = null;
    private boolean paramProcessed = false;
    /** Holds user certificate. Inits and used in {@link #getAuthenticationData getAuthenticationData} */
    private X509Certificate peerCertificate;
    private String sessionId = null;

    /**Constructor.
     * @param headers mime headers.
     * @param stream servlet input stream.
     */
    public HttpNusuthRequest(NusuthHeaders headers, HttpProtocolAdapter adapter) {
        this.headers = headers;
        this.adapter = adapter;
    }

    public void init(ServletInputStream stream) {
        this.stream = stream;
        usingReader = false;
        usingInput = false;
        paramProcessed = false;
    }

    public StringBuffer getRequestURL() {
        StringBuffer result = new StringBuffer();
        result.append(getScheme());
        result.append("://");
        result.append(getServerName());
        result.append(":");
        result.append("" + getServerPort() + "");
        result.append(getRequestURI());
        return result;
    }

    /**
     * This method removes header with specified name.
     * @param name Name of header to remove.
     */
    public void removeHeader(String name) {
        headers.clearHeader(name);
    }

    /**
     * @param name request attribute name.
     * @return attribute object that is associated with the given name.
     */

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * @return Enumeration of String values - the request attribute names.
     */
    public Enumeration getAttributeNames() {
        return attributes.keys();
    }

    /**
     * @return the value of the general-header field of the request with the "Transfer-Encoding" name.
     */
    public String getCharacterEncoding() {
        if (encoding != null)
            return encoding;
        return getContentType();
    }

    /**
     * @return  the request content length.
     */
    public int getContentLength() {
        return adapter.getContentLength();
    }

    /**
     * @return the request content type. (The value of the "Content-Type" entity-header field), or -1 if this header is not present.
     */
    public String getContentType() {
        return headers.containsHeader("Content-Type") ? getHeader("Content-Type") : null;
    }

    /**
     * @return the servlet input stream.
     */
    public ServletInputStream getInputStream() {
        if (!usingReader) {
            usingInput = true;
            return stream;
        } else {
            throw new IllegalStateException("Reader already used");
        }
    }

    /**
     * @return the first object from the list of the request locales(for the Locale class @see java.util.Locale ). Returns null if the is no any Locale object.
     */
    public Locale getLocale() {
        if (locales == null) {
            constructLocales();
        }
        return locales == null ? null : locales[0];
    }

    /**
     * @return the Enumeration of Locale objects of this request.
     */
    public Enumeration getLocales() {
        if (locales == null) {
            constructLocales();
        }
        return locales == null ? null : Collections.enumeration(Arrays.asList(locales));
    }

    /**
     * @param name request parameter name.
     * @return the first value from the list of values of the request parameter with the given name, null if there is an empty list of values.
     */
    public String getParameter(String name) {
        if (!paramProcessed) {
            processParameters();
        }
        //    if (parameters == null) {
        //      parameters = adapter.getParameters();
        //    }
        String[] s = (String[]) parameters.get(name);
        return s == null ? null : s[0];
    }


    /**
     * @return  Enumeration of String values - the request parameter names.
     */
    public Enumeration getParameterNames() {
        if (!paramProcessed) {
            processParameters();
        }
        //    if (parameters == null) {
        //      parameters = adapter.getParameters();
        //    }
        return parameters.keys();
    }

    /**
     * @param  name request parameter name.
     * @return the list of the request parameter values.
     */
    public String[] getParameterValues(String name) {
        if (!paramProcessed) {
            processParameters();
        }
        //    if (parameters == null) {
        //      parameters = adapter.getParameters();
        //    }
        return (String[]) parameters.get(name);
    }

    /**
     * @return request protocol name.
     */
    public String getProtocol() {
        if (protocol == null) {
            protocol = adapter.getProtocol();
        }
        return protocol;
    }

    /**
     * @return object of the BufferedReader class which encapsulates the object of the  InputStreamReader class. Which is connected with the request input stream.
     */
    public BufferedReader getReader() {
        if (!usingInput) {
            Reader reader = null;
            try {
                reader = getCharacterEncoding() == null ? new InputStreamReader(stream) :
                        new InputStreamReader(stream, getCharacterEncoding());
            } catch (UnsupportedEncodingException uee) {
                reader = new InputStreamReader(stream);
            }
            usingReader = true;
            return new BufferedReader(reader);
        } else {
            throw new IllegalStateException("InputStream already used");
        }
    }

    /**The getRealPath method takes a String argument and returns a String representation of a
     * file on the local file system to which that path corresponds.
     * @deprecated always return null
     * @param path context path.
     * @return String representation of a file on the local file system.
     */
    //XXX path - ???????
    public NusuthContext getContext() {
        if (context == null) {
            return adapter.getContext();
        } else {
            return context;
        }
    }

    /**
     * Returns a String containing the real path for a given virtual path.
     * if given path is null, then this method return null.
     * @param path Virtual path.
     * @return String String containing the real path for a given virtual path.
     */
    public String getRealPath(String path) {
        if (path != null) {
            if (context == null) {
                context = adapter.getContext();
            }
            if (path.startsWith("/")) {
                return context.getRealPath(path);
            } else {
                String realPath = path;
                StringBuffer sb = new StringBuffer();
                if (servletPath != null) {
                    sb.append(servletPath);
                }
                if (pathInfo != null) {
                    sb.append(pathInfo);
                }
                realPath = sb.toString();
                int ind = realPath.lastIndexOf("/");
                if (ind > -1) {
                    realPath = realPath.substring(0, ind);
                }
                realPath += ("/" + path);
                return context.getRealPath(realPath);
            }
        } else {
            return null;
        }
    }

    /**
     * @return remote address.
     */
    public String getRemoteAddr() {
        return adapter.getHostAddress();
    }

    /**
     * @param remote address.
     */
    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    /**
     * @return remote host.
     */
    public String getRemoteHost() {
        return adapter.getHostName();
    }

    /**
     * @param remote host.
     */
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    /**This method takes a String argument described a path within the scope of the NusuthContext
     * This path must be relative to the root of the NusuthContext.This path is used to look up a servlet,
     * wrap it with the RequestDispatcher and return it. It may also take the servlet name .
     * @param path servlet path in the NusuthContext or servlet name.
     * @return servlet wrapped with the object of the RequestDispatcher class type, or null if no servlet is associated with the given name or if context is not set.
     */
    public RequestDispatcher getRequestDispatcher(String path) {
        if (context == null) {
            context = adapter.getContext();
        }
        String realPath = path;
        if (!path.startsWith("/")) {
            StringBuffer sb = new StringBuffer();
            if (servletPath != null) {
                sb.append(servletPath);
            }
            if (pathInfo != null) {
                sb.append(pathInfo);
            }
            realPath = sb.toString();
            int ind = realPath.lastIndexOf("/");
            if (ind > -1) {
                realPath = realPath.substring(0, ind);
            }
            realPath += ("/" + path);
        }
        return context == null ? null : context.getRequestDispatcher(realPath);
    }

    /**
     * @return protocol scheme.
     */
    public String getScheme() {
        if (scheme == null) {
            scheme = adapter.getScheme();
        }
        return scheme;
    }

    /**
     * @return server port.
     */
    public int getServerPort() {
        return adapter.getServerPort();
    }

    /**
     * @param serverPort server port.
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * @return true if this is secure connection request.
     */
    public boolean isSecure() {
        return getScheme().toUpperCase().equals("HTTPS");
    }

    /** This method removes attribute with the given name.
     * @param name attribute name.
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**This method sets attribute with the given name and value.
     * @param name attribute name.
     * @param o attribute object.
     */
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    /**XXX ?????????
     * @return
     */
    public String getAuthType() {
        if (context == null) {
            context = adapter.getContext();
        }
        return context == null ? null : context.getAuthType(this);
    }

    /**XXX ????????
     * @return
     */
    public Cookie[] getCookies() {
        if (cookies == null) {
            processCookies();
        }
        return cookies == null ? new Cookie[0] : cookies;
    }

    /**Returns the date value of a header with the specified name.
     * @param name the header field name.
     * @return the date value of the header field in number of milliseconds since the epoch, or -1 if the header was not found.
     * @exception IllegalArgumentException if the date format was invalid.
     */
    public long getDateHeader(String name) {
        return headers.getDateHeader(name);
    }

    /**
     * Returns the string value of one of the headers with the
     * specified name.
     * @param name the header field name.
     * @return the string value of the field, or null if none found.
     */
    public String getHeader(String name) {
        return headers.getHeader(name);
    }

    /**
     * @return an enumeration of strings representing the header field names.
     * Field names may appear multiple times in this enumeration, indicating
     * that multiple fields with that name exist in this header.
     */
    public Enumeration getHeaderNames() {
        return headers.getHeaderNames();
    }


    /**
     * Returns the string value of all of the headers with the
     * specified name.
     * @param name the header field name.
     * @return Enumeration of Strings - values of the fields, or null if none found.
     */
    public Enumeration getHeaders(String name) {
        return headers.getHeaders(name);
    }

    /**
     * Returns the integer value of a header with the specified name.
     * @param name the header field name.
     * @return the integer value of the header field, or -1 if the header was not found.
     * @exception NumberFormatException if the integer format was invalid.
     */
    public int getIntHeader(String name) {
        return headers.getIntHeader(name);
    }

    /**
     * @return the request method.
     */
    public String getMethod() {
        if (method == null) {
            method = adapter.getMethod();
        }
        return method;
    }

    /*
     * @return the real path of the pathInfo of this request.
     */
    public String getPathTranslated() {
        if (context == null) {
            context = adapter.getContext();
        }
        String path = getPathInfo();
        pathTranslated = null;
        if (path == null || "".equals(path)) {
            return null;
        }
        pathTranslated = context.getRealPath(path);
        return pathTranslated;
    }

    /**
     * @param the real path of the pathInfo of this request.
     */
    public void setPathTranslated(String pathTranslated) {
        this.pathTranslated = pathTranslated;
    }

    /**
     * @return the user name that the client authenticated with.
     */
    public String getRemoteUser() {
        if (context == null) {
            context = adapter.getContext();
        }
        return context == null ? null : context.getRemoteUser(this);
    }

    /**This method returns the  request session Id.
     * @return the request session Id or null if there is no cookie wuth the name "JSESSIONID" which contains this session Id.
     */
    public String getRequestedSessionId() {
        if (cookies == null) {
            processCookies();
        }
        if (sessionId != null) {
            return sessionId;
        }
        /*
         if (cookies == null)
         return null;
         for (int i = 0; i < cookies.length; i++) {
         Cookie cookie = cookies[i];
         if (cookie.getName().equals("JSESSIONID"))
         return cookie.getValue();
         }
         */
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (cookie.getName().equals("JSESSIONID")) {
                    sessionId = cookie.getValue();
                    return sessionId;
                }
            }
        }
        if (sessionIdFromURL != null) {
            return sessionIdFromURL;
        }
        return null;
    }

    /**This method returnes the http session object associated with this request
     * or creates the new one if there was no session associated with it.
     * @return the http session object for this request.
     */
    public HttpSession getSession() {
        return getSession(true);
    }

    /**This method returnes the http session object associated with this request.
     * @param create True if the new session will be created if there is no session associated with this request, otherwise Flse .
     * @return the http session object for this request.
     */
    public HttpSession getSession(boolean create) {
        if (context == null) {
            context = adapter.getContext();
        }
        return context == null ? null : context.getSession(this, create);
    }

    /**
     * @return the user name that the client authenticated with.
     */
    public java.security.Principal getUserPrincipal() {
        if (context == null) {
            context = adapter.getContext();
        }
        return context == null ? null : context.getUserPrincipal(this);
    }

    /**This method indicates if the session Id is requested from cookie.
     * @return always True .
     */
    public boolean isRequestedSessionIdFromCookie() {
        Cookie[] cookies = getCookies();
        String id = getRequestedSessionId();
        for (int i = 0; i < cookies.length; i++) {
            if (cookies[i].getName().equals("JSESSIONID") && cookies[i].getValue().equals(id))
                return true;
        }
        return false;
    }

    /**This method indicates if the session Id is requested from URl.
     * @return always False .
     */
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    /**This method indicates if the session Id is requested from URL.
     * @return always False .
     */
    public boolean isRequestedSessionIdFromURL() {
        return (!isRequestedSessionIdFromCookie() && sessionIdFromURL.endsWith(getRequestedSessionId()));
    }

    /**This method indicates if the session with the given Id is valid or not.
     * @return True if the session is valid, otherwise false.
     */
    public boolean isRequestedSessionIdValid() {
        if (context == null) {
            context = adapter.getContext();
        }
        return context == null ? false : context.isSessionValid(this);
    }

    /**
     * This method queries the underlying security mechanism of the container to
     * determine if a particular user is in a given security role.
     * @param role name.
     * @return True if the user is in the given role, otherwise false.
     */
    public boolean isUserInRole(String role) {
        if (context == null) {
            context = adapter.getContext();
        }
        return context == null ? false : context.isUserInRole(this, role);
    }

    /**
     * @return
     */
    //XXX what's this ??
    public int getError() {
        return error;
    }

    private void processCookies() {
        /*    // XXX bug in original RequestImpl - might not work if multiple
         // cookie headers.
         //
         // XXX need to use the cookies hint in RequestAdapter
         String cookieString = getHeader("Cookie");
         List lst = null;
         //System.out.println("Cookie header: " + cookieString);
         // Gets cookie information from the requets header and creates the list of Cookie class objects.
         if (cookieString != null) {
         lst = new ArrayList();
         StringTokenizer tok = new StringTokenizer(cookieString,";", false);
         while (tok.hasMoreTokens()) {
         String token = tok.nextToken();
         int i = token.indexOf("=");
         if (i > -1) {

         // XXX
         // the trims here are a *hack* -- this should
         // be more properly fixed to be spec compliant

         String name = token.substring(0, i).trim();
         String value = token.substring(i+1, token.length()).trim();
         if (value.startsWith("\""))
         value = value.substring(1, value.length()-1);
         Cookie cookie = new Cookie(name, value);
         //System.out.println("Cookie received, name: "+name+", value: "+value);
         lst.add(cookie);
         } else {
         // we have a bad cookie.... just let it go
         }
         }
         }
         if (lst != null && lst.size() > 0) {
         Iterator iterator = lst.iterator();
         cookies = new Cookie[lst.size()];
         int i = 0;
         while(iterator.hasNext()) {
         cookies[i++] = (Cookie)iterator.next();
         }
         }*/
        cookies = headers.processCookies();
    }

    /**This method returns the container ID of the corresponding servlet.
     * @return container ID, null if the cookie list is empty or there is no cookie with the "JCONTAINERID" name.
     */
    public String getRequestedContainerID() {
        if (cookies == null)
            return null;
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookie.getName().equals("JCONTAINERID"))
                return cookie.getValue();
        }
        return null;
    }

    /**This method sets the content length.
     * @param contentLength the content length.
     */
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * This method creates the array of Locale objects @see java.util.Locale depending on the request information.
     * Depending on the value of the Accept-Language request-header field.
     */
    private void constructLocales() {
        String alangs = getHeader("Accept-Language");
        if (alangs == null || alangs.length() == 0) {
            locales = new Locale[1];
            locales[0] = Locale.getDefault();
        } else {
            StringTokenizer st = new StringTokenizer(alangs, ",");
            int sz = st.countTokens();
            locales = new Locale[sz];
            for (int i = 0; i < sz; i++) {
                //Added by skilz..
                String str = st.nextToken();
                if (str.length() > 2) {
                    if (str.charAt(2) == '-') {
                        locales[i] = new Locale(str.trim().substring(0, 2).toLowerCase(), str.trim().substring(3, str.length()).toUpperCase());
                    } else {
                        locales[i] = new Locale(str.trim().substring(0, 2), "");
                    }
                } else {
                    locales[i] = new Locale(str, "");
                }
//
                //Commented by skilz...
                //        locales[i] = new Locale(st.nextToken().toLowerCase().trim(), "");
            }
        }
    }

    public void cleanup() {
        super.cleanup();
        dispatched = false;
        stream = null;
        error = -1;
        attributes.clear();
        parameters = null;
        locales = null;
        protocol = null;
        contextPath = null;
        servletPath = null;
        pathInfo = null;
        pathTranslated = null;
        scheme = null;
        remoteAddr = null;
        remoteHost = null;
        serverPort = 0;
        context = null;
        cookies = null;
        method = null;
        queryString = null;
        requestURI = null;
        contentLength = -1;
        usingReader = false;
        usingInput = false;
        reqURIStrBuffer = null;
        sessionIdFromURL = null;
        sessionId = null;
        paramProcessed = false;
        peerCertificate = null;
    }

    public String getServerName() {
        String s = headers.getHeader("Host");
        if (s == null) {
            return adapter.getServerHost();
        } else {
            int j = s.indexOf(":");
            return j > -1 ? s.substring(0, j) : s;
        }
    }

    public String getQueryString() {
        if (queryString == null) {
            if (encoding == null)
                encoding = parseCharacterEncoding(getCharacterEncoding());
            if (encoding == null)
                encoding = "ISO8859_1";
            queryString = adapter.getQueryString(encoding);
        }
        return queryString;
    }

    public String getContextPath() {
        if (contextPath == null) {
            contextPath = adapter.getContextPath();
        }
        return contextPath;
    }

    public String getServletPath() {
        if (servletPath == null) {
            servletPath = adapter.getServletPath();
        }
        return servletPath;
    }

    public String getPathInfo() {
        if (pathInfo == null) {
            pathInfo = adapter.getPathInfo();
        }
        return pathInfo;
    }

    public String getRequestURI() {
        if (requestURI == null) {
            requestURI = adapter.getRequestURI().toString();
        }
        return requestURI;
    }

    public StrBuffer getRequestURIAsStrBuffer() {
        if (reqURIStrBuffer == null) {
            reqURIStrBuffer = adapter.getRequestURI();
        }
        return reqURIStrBuffer;
    }

    protected void addParameters(Hashtable newParams) {
        if (!paramProcessed) {
            processParameters();
        }
        //    if (parameters == null) {
        //      parameters = adapter.getParameters();
        //    }
        Enumeration enum = newParams.keys();
        String[] oldParamSet;
        String[] newParamSet;
        String[] concatParamSet;
        String key;
        while (enum.hasMoreElements()) {
            key = (String) enum.nextElement();
            oldParamSet = (String[]) parameters.get(key);
            if (oldParamSet == null) {
                concatParamSet = (String[]) newParams.get(key);
            } else {
                newParamSet = (String[]) newParams.get(key);
                concatParamSet = new String[oldParamSet.length + newParamSet.length];
                System.arraycopy(newParamSet, 0, concatParamSet, 0, newParamSet.length);
                System.arraycopy(oldParamSet, 0, concatParamSet, newParamSet.length, oldParamSet.length);
            }
            parameters.put(key, concatParamSet);
        }
    }

    protected void removeParameters(Hashtable newParams) {
        if (parameters == null) {
            return;
        }
        Enumeration enum = newParams.keys();
        String[] oldParamSet;
        String[] newParamSet;
        String[] concatParamSet;
        String key;
        while (enum.hasMoreElements()) {
            key = (String) enum.nextElement();
            oldParamSet = (String[]) parameters.get(key);
            if (oldParamSet != null) {
                newParamSet = (String[]) newParams.get(key);
                if (newParamSet.length == oldParamSet.length) {
                    parameters.remove(key);
                } else if (newParamSet.length < oldParamSet.length) {
                    concatParamSet = new String[oldParamSet.length - newParamSet.length];
                    System.arraycopy(oldParamSet, newParamSet.length, concatParamSet, 0, oldParamSet.length - newParamSet.length);
                    parameters.put(key, concatParamSet);
                }
            }
        }
    }

    protected void clearParameters() {
        parameters = new Hashtable();
    }

    public void setRequestURI(String reqUri) {
        super.setRequestURI(reqUri);
        reqURIStrBuffer = new StrBuffer();
        reqURIStrBuffer.append(reqUri);
    }

    public void setSessionIdFromURL(String id) {
        this.sessionIdFromURL = id;
    }

    public String getSessionIdFromURL() {
        return sessionIdFromURL;
    }

    public String parseCharacterEncoding(String contentType) {
        if (contentType == null)
            return (null);
        int start = contentType.indexOf("charset=");
        if (start < 0)
            return (null);
        String enc = contentType.substring(start + 8);
        int end = enc.indexOf(";");
        if (end >= 0)
            enc = enc.substring(0, end);
        enc = enc.trim();
        if ((enc.length() > 2) && (enc.startsWith("\""))
                && (enc.endsWith("\"")))
            enc = enc.substring(1, enc.length() - 1);
        return (enc.trim());
    }


    private void processParameters() {
        if (encoding == null)
            encoding = parseCharacterEncoding(getCharacterEncoding());
        if (encoding == null)
            encoding = "ISO8859_1";
        if (parameters == null) {
            parameters = adapter.getParameters(encoding);
        }
        paramProcessed = true;
    }

    public Map getParameterMap() {
        if (!paramProcessed) {
            processParameters();
        }
        return parameters;
    }

    /**
     * @return {@link AuthenticationData AuthenticationData} if this data
     * represented in request, or <code>null</code>, if not.
     */
    protected AuthenticationData getAuthenticationData() {
        switch (adapter.getContext().getDefaultAuthType()) {
            case AuthenticationData.AUTH_METHOD_BASIC:
                if (headers != null && headers.containsHeader(AUTHORIZATION_HEADER_NAME)) {
                    String headerString = headers.getHeader(AUTHORIZATION_HEADER_NAME_String).trim();
                    int index = headerString.indexOf(' ');
                    if (index < 0)
                        return null;
                    String authType = headerString.substring(0, index);
                    if (authType.equalsIgnoreCase("basic")) {
                        String encodedAuthString = headerString.substring(index + 1).trim();
                        String authString;
                        try {
                            authString = new String(Base64.decode(encodedAuthString.toCharArray()));
                        } catch (Error err) {
                            return null;
                        }
                        index = authString.indexOf(':');
                        if (index > 0) {
                            String userName = authString.substring(0, index);
                            String password = authString.substring(index + 1);
                            return new AuthenticationData(AuthenticationData.AUTH_METHOD_BASIC, userName, password);
                        }
                    }
                } else if (false) {
                    // process form based login
                }
                return null;
            case AuthenticationData.AUTH_METHOD_CERT:
                // for SSL authentication
                if (adapter.isStandAlone()) {
                    if (peerCertificate == null) {
                        Socket socket = adapter.getSocket();
                        if (socket instanceof SSLSocket) {
                            SSLSocket sslSocket = (SSLSocket) socket;
                            if (sslSocket.getNeedClientAuth()) {
                                try {
                                    X509Certificate[] peerCertificateChain =
                                            sslSocket.getSession().getPeerCertificateChain();
                                    if (peerCertificateChain != null &&
                                            peerCertificateChain.length > 0) {
                                        peerCertificate = peerCertificateChain[0];
                                    }
                                } catch (SSLPeerUnverifiedException sslpuex) {
                                    /*if (catProxy.isDebugEnabled()) {
                                    cat.debug("SSL client not verified", sslpuex);
                                    }*/
                                }
                            }
                        }
                    }
                } else {
                    if (peerCertificate == null) {
                        String certificateString = headers.getHeader(HttpConstants.CLIENT_CERTIFICATE_HEADER_NAME_STRING);
                        if (certificateString != null && certificateString.length() > 0) {
                            try {
                                byte[] certificate = Base64.decode(certificateString.toCharArray());
                                peerCertificate = X509Certificate.getInstance(certificate);
                            } catch (CertificateException cex) {
                                //cat.warn("Couldn't decode client certificate from http header", cex);
                            } catch (Error err) {
                                //cat.warn("Couldn't decode client certificate from http header", err);
                            }
                        }
                    }
                }
                return peerCertificate == null
                        ? null
                        : new AuthenticationData(peerCertificate);
            case AuthenticationData.AUTH_METHOD_DIGEST:
                // not implemented
                return null;
            case AuthenticationData.AUTH_METHOD_FORM:
                String pathInfo = adapter.getPathInfo();
                if (pathInfo != null && pathInfo.endsWith("/j_security_check")) {
                    String userName = getParameter("j_username");
                    String password = getParameter("j_password");
                    if (userName != null && password != null)
                        return new AuthenticationData(AuthenticationData.AUTH_METHOD_FORM, userName, password);
                } else
                    return null;
            default:
                return null;
        }
    }

    public HttpNusuthRequest cloneRequest() {
        try {
            HttpNusuthRequest result = (HttpNusuthRequest) this.clone();
            result.attributes = (Hashtable) this.attributes.clone();
            return result;
        } catch (CloneNotSupportedException cnsex) {
            return null;
        }
    }
}

