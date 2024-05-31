package com.azoft.nusuth.core;

import com.azoft.nusuth.util.Utils;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

/**
 * This Class provides forward and include methods.
 * forward method is used to forward request to the servlet bound up with this
 * class object. include method is used to include the response information
 * from the servlet bound up with this class object to the responce from the
 * sourse servlet.
 * @author vdgg, skilz
 * @version 1.15
 * @since Nusuth1.0
 */
public class NusuthRequestDispatcher implements RequestDispatcher {

    private FilterChain chain;
    private boolean named;
    private String path;
    private String requestURI;
    private String contextPath;
    private String servletPath;
    private String pathInfo;
    private String queryString;

    /**This is the constructor method.
     * @param servlet servlet object.
     * @param servletPath not null when parameter "named" = false, otherwise null. This parameter contains mapped script path.
     * @param path contains servlet name if "named" = true and path in the JBirdContex if "named" = false.
     * @param named True if this is named servlet, false otherwise.
     */
//  protected NusuthRequestDispatcher(Servlet servlet, String servletPath, String path, boolean named) {
    protected NusuthRequestDispatcher(FilterChain chain, String servletPath, String path, boolean named) {
        super();
        this.chain = chain;
        this.servletPath = servletPath;
        this.path = path;
        this.named = named;
    }

    /**This method is used to forward request to the servlet that is bound up
     * with this NusuthRequestDispatcher class object. See Java Servlet Specification Version 2.2.
     * @param request request object.
     * @param response response object.
     * @exception ServletException is thrown if response is already commited or there is unknown type of ServletResponse or ServletRequest.
     * @exception IOException is thrown if any error occures while running service method of servlet.
     */
    public void forward(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        if (response.isCommitted())
            throw new IllegalStateException("Response already commited");
        response.reset();
        NusuthRequest jreq = null;
        NusuthResponse jresp = null;
        try {
//      jreq = (NusuthRequest)request;
//      jresp = (NusuthResponse)response;
            jreq = getRealRequest(request);
            jresp = getRealResponse(response);
        } catch (ClassCastException cce) {
            throw new IllegalStateException("Unknown type of ServletResponse or ServletRequest");
        }
        if (!named) {
            processPath(jreq);
            jreq.setRequestURI(requestURI);
            jreq.setContextPath(contextPath);
            jreq.setServletPath(servletPath);
            jreq.setPathInfo(pathInfo);
            jreq.setQueryString(queryString);
//      jreq.clearParameters();
            if (queryString != null && queryString.length() > 0) {
                try {
                    jreq.addParameters(Utils.parseQueryString(queryString));
                } catch (Exception ex) {
                }
            }
        }
        if (path.equals("_nusuth_error_servlet")) {
            boolean[] b = {false, false};
            jresp.setStreamWriterUsage(b);
        }
        chain.doFilter(request, response);
        if (!(response instanceof ServletResponseWrapper)) {
            jresp.close();
        } else {
            response.flushBuffer();
        }
    }

    /**This method is used to add response for the given request from the servlet,
     * that is bound up with this class, to the given response.
     * @param request request object.
     * @param response response object.
     * @exception ServletException is thrown if there is unknown type of ServletResponse or ServletRequest.
     * @exception IOException is thrown if any error occures while running service method of servlet.
     */
    public void include(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        NusuthResponse jresp = null;
        NusuthRequest jreq = null;
        try {
//      jresp = (NusuthResponse)response;
//      jreq = (NusuthRequest)request;
            jresp = getRealResponse(response);
            jreq = getRealRequest(request);
        } catch (ClassCastException cce) {
            throw new IllegalStateException("Unknown type of ServletResponse or ServletRequest");
        }
//response.flushBuffer();
        jresp.blockHeaders();
        String old_uri = null;
        String old_contextPath = null;
        String old_servletPath = null;
        String old_pathInfo = null;
        String old_query = null;
        if (!named) {
            old_uri = (String) request.getAttribute("javax.servlet.include.request_uri");
            old_contextPath = (String) request.getAttribute("javax.servlet.include.context_path");
            old_servletPath = (String) request.getAttribute("javax.servlet.include.servlet_path");
            old_pathInfo = (String) request.getAttribute("javax.servlet.include.path_info");
            old_query = (String) request.getAttribute("javax.servlet.include.query_string");
            processPath(jreq);
            request.setAttribute("javax.servlet.include.request_uri", requestURI);
            request.setAttribute("javax.servlet.include.context_path", contextPath);
            request.setAttribute("javax.servlet.include.servlet_path", servletPath);
            if (pathInfo.length() != 0)
                request.setAttribute("javax.servlet.include.path_info", pathInfo);
            if (queryString != null) {
                request.setAttribute("javax.servlet.include.query_string", queryString);
            }
            if (queryString != null && queryString.length() > 0) {
                try {
                    jreq.addParameters(Utils.parseQueryString(queryString));
                } catch (Exception ex) {
                }
            }
        }
        boolean[] swUsage = jresp.getStreamWriterUsage();
        if (path.equals("_nusuth_error_servlet")) {
            boolean[] b = {false, false};
            jresp.setStreamWriterUsage(b);
        }
        try {
            chain.doFilter(request, response);
/*    } catch(IllegalStateException ise) {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        ise.printStackTrace(ps);
        ps.flush();
        ps.close();
        OutputStream os = response.getOutputStream();
        os.write("<pre>".getBytes());
        os.write(baos.toByteArray());
        os.write("</pre>".getBytes());
        os.write("<BR>This error occured while including<BR>".getBytes());
      } catch(IllegalStateException ex) {
      }
*/
        } catch (IOException ex) {
            throw ex;
        } catch (ServletException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServletException(ex);
        } finally {
            jresp.unblockHeaders();
            if (path.equals("_nusuth_error_servlet")) {
                jresp.setStreamWriterUsage(swUsage);
            }
        }
        if (!named && queryString != null && queryString.length() > 0) {
            try {
                jreq.removeParameters(Utils.parseQueryString(queryString));
            } catch (Exception ex) {
            }
        }
        if (!named) {
            request.removeAttribute("javax.servlet.include.request_uri");
            request.removeAttribute("javax.servlet.include.context_path");
            request.removeAttribute("javax.servlet.include.servlet_path");
            request.removeAttribute("javax.servlet.include.path_info");
            request.removeAttribute("javax.servlet.include.query_string");
            if (old_uri != null)
                request.setAttribute("javax.servlet.include.request_uri", old_uri);
            if (old_contextPath != null)
                request.setAttribute("javax.servlet.include.context_path", old_contextPath);
            if (old_servletPath != null)
                request.setAttribute("javax.servlet.include.servlet_path", old_servletPath);
            if (old_pathInfo != null)
                request.setAttribute("javax.servlet.include.path_info", old_pathInfo);
            if (old_query != null)
                request.setAttribute("javax.servlet.include.query_string", old_query);
        }
    }

    //This method creates full servlet URI from the request information.
    private void processPath(NusuthRequest req) {
        contextPath = req.getContextPath();
        String ost = path.substring(servletPath.length(), path.length());
        int k = ost.indexOf('?');
        if (k == -1) {
            pathInfo = ost;
            queryString = null;
        } else {
            pathInfo = ost.substring(0, k);
            if (k < ost.length() - 1)
                queryString = ost.substring(k + 1, ost.length());
            else
                queryString = null;
        }
        requestURI = contextPath + servletPath + pathInfo;
        String query = req.getQueryString();
    }

    /**
     * This method return "real" response from given. If given response is
     * wrapper, then this method get response from it.
     * @param response response or wrapper;
     * @return HttpNusuthResponse Real response.
     */
    public NusuthResponse getRealResponse(ServletResponse response) {
        ServletResponse resp = response;
        while (resp instanceof ServletResponseWrapper) {
            resp = (ServletResponse)
                    ((ServletResponseWrapper) resp).getResponse();
        }
        return (NusuthResponse) resp;
    }

    /**
     * This method return "real" request from given. If given request is
     * wrapper, then this method get request from it.
     * @param request request or wrapper;
     * @return HttpNusuthRequest Real request.
     */
    public NusuthRequest getRealRequest(ServletRequest request) {
        ServletRequest req = request;
        while (req instanceof ServletRequestWrapper) {
            req = (ServletRequest)
                    ((ServletRequestWrapper) req).getRequest();
        }
        return (NusuthRequest) req;
    }

}
