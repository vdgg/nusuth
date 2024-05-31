package com.azoft.nusuth.core;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.SAXException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Stack;
import java.util.HashSet;

import com.azoft.nusuth.util.StrBuffer;
import com.azoft.nusuth.container.InvocationCache;
import com.jclark.xsl.sax.*;

/**
 * This filter transform response content using xsl scheme.
 * @author skilz
 * @version 1.15
 * @since Nusuth1.0
 */
public class NusuthXSLTFilter implements Filter {

    /**uri ti xsl mapping*/
    private Hashtable uri2xsl = new Hashtable();
    /**uri to xsl transfromer mapping*/
    private Hashtable xsl2transformer = new Hashtable();
    private Hashtable xsl2realFile = new Hashtable();
    private Hashtable xsl2lastModified = new Hashtable();
    private Hashtable transformer2boolean = new Hashtable();
    /**servlet context*/
    private ServletContext context = null;
    /**uri to last modified mapping*/
    private Hashtable uri2lastModified = new Hashtable();
    /**logger*/
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.core");
    /**cache*/
    private static InvocationCache cache;
    /**cache initialization indicator*/
    private static boolean cacheInitialized;
    /**pool of response out*/
    private Stack responseOutPool = new Stack();

    static {
        cacheInitialized = false;
    }

    /**
     * This method initialize filter. It construct all required mappings.
     * @param config Filter config.
     */
    public void init(FilterConfig config) throws ServletException {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
                this.getClass().getClassLoader());
        context = config.getServletContext();
        Enumeration enum = config.getInitParameterNames();
        while (enum.hasMoreElements()) {
            String paramName = (String) enum.nextElement();
            String paramValue = config.getInitParameter(paramName);
            if (paramName.toLowerCase().trim().equals("cache-size")) {
                if (!cacheInitialized) {
                    synchronized (this) {
                        if (!cacheInitialized) {
                            String cacheSize = null;
                            int size = 1048576;
                            cacheSize
                                    = config.getInitParameter(paramName).trim().toLowerCase();
                            if (cacheSize != null) {
                                try {
                                    String tempcacheSize;
                                    if (!(cacheSize.substring(cacheSize.length() - 1).equals("b")
                                            || cacheSize.substring(
                                                    cacheSize.length() - 1).equals("k")
                                            || cacheSize.substring(
                                                    cacheSize.length() - 1).equals("m"))) {
                                        size = 1024 * Integer.parseInt(cacheSize);
                                    } else {
                                        if (cacheSize.substring(
                                                cacheSize.length() - 2).equals("kb")) {
                                            tempcacheSize
                                                    = cacheSize.substring(0, cacheSize.length() - 2);
                                            size = 1024 * Integer.parseInt(
                                                    cacheSize.substring(0, cacheSize.length() - 2));
                                        } else {
                                            if (cacheSize.substring(
                                                    cacheSize.length() - 2).equals("mb")) {
                                                tempcacheSize
                                                        = cacheSize.substring(0, cacheSize.length() - 2);
                                                size = 1048576 * Integer.parseInt(
                                                        cacheSize.substring(0, cacheSize.length() - 2));
                                            }
                                        }
                                        if (cacheSize.substring(
                                                cacheSize.length() - 1).equals("k")) {
                                            tempcacheSize
                                                    = cacheSize.substring(0, cacheSize.length() - 1);
                                            size = 1024 * Integer.parseInt(
                                                    cacheSize.substring(0, cacheSize.length() - 1));
                                        } else {
                                            if (cacheSize.substring(
                                                    cacheSize.length() - 1).equals("m")) {
                                                tempcacheSize
                                                        = cacheSize.substring(0, cacheSize.length() - 1);
                                                size = 1048576 * Integer.parseInt(
                                                        cacheSize.substring(0, cacheSize.length() - 1));
                                            } else {
                                                if (cacheSize.substring(
                                                        cacheSize.length() - 1).equals("b")) {
                                                    tempcacheSize = cacheSize.substring(
                                                            cacheSize.length() - 2, cacheSize.length() - 1);
                                                    if (!(tempcacheSize.equals("k")
                                                            || tempcacheSize.equals("m"))) {
                                                        size = Integer.parseInt(
                                                                cacheSize.substring(0,
                                                                        cacheSize.length() - 1));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (NumberFormatException ex) {
                                    System.err.println("Wrong value in \'cache-size\' init "
                                            + "parameter value. Default value will "
                                            + "be used (1Mb)...");
                                }
                            } else {
                                size = 1048576;
                            }
                            cache = new InvocationCache(60000);
                            if (size != 0) {
                                cache.setSize(size);
                                cache.setAvailableSize(size);
                            } else {
                                cache.setSize(1048576);
                                cache.setAvailableSize(1048576);
                            }
                            cacheInitialized = true;
                        }
                    }
                }
            } else {
                String realPath2 = context.getRealPath(paramValue);
                File file2 = new File(realPath2);
                if (file2.exists()) {
                    try {
                        org.xml.sax.InputSource xsltInputSource =
                                new org.xml.sax.InputSource(new FileInputStream(file2));
                        XSLProcessor processor = new XSLProcessorImpl();
                        processor.setParser(new SAXParser());
                        processor.loadStylesheet(xsltInputSource);
                        xsl2transformer.put(paramValue, processor);
                        uri2xsl.put(paramName, paramValue);
                        xsl2lastModified.put(paramValue, new Long(file2.lastModified()));
                        xsl2realFile.put(paramValue, file2);
                        transformer2boolean.put(processor, new Boolean(false));
                    } catch (Exception e) {
                        cat.error("Cannot create transformer", e);
                        xsl2transformer.put(paramValue, e);
                        uri2xsl.put(paramName, paramValue);
                        xsl2lastModified.put(paramValue, new Long(file2.lastModified()));
                        xsl2realFile.put(paramValue, file2);
                    }
                } else {
                    cat.error("Incorrect mapping for param-name=\""
                            + paramName + "\" and param-value=\"" + paramValue + "\". "
                            + "Xsl file " + file2.getAbsolutePath() + " does not exist");
                }
            }
        }
        if (cache == null) {
            cache = new InvocationCache();
            cache.setSize(1048576);
            cache.setAvailableSize(1048576);
            cacheInitialized = true;
        }
        Thread.currentThread().setContextClassLoader(oldLoader);
        (new XslFileChecker(xsl2realFile, xsl2lastModified)).start();
    }

    /**
     * This method process filter.
     * @param request Request.
     * @param response Response.
     * @param FilterChain Filter chain.
     */
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {
        StrBuffer bufUri = new StrBuffer();
        HttpServletRequest req = (HttpServletRequest) request;
        XSLProcessor proc = null;
        try {
            proc = getProcessor(req, bufUri);
        } catch (ClassCastException e) {
            Exception ex = (Exception) xsl2transformer.get(bufUri.toString());
            ((NusuthContext) context).processError(request, response,
                    500, ex.getClass(),
                    "Cannot create "
                    + "transformer, nested:\r\n"
                    + ex.getMessage()
                    + "\r\n\r\nRequested uri is \""
                    + req.getRequestURI()
                    + "\"\r\nUsed xsl is \""
                    + getXsl(req) + "\"",
                    ex, req.getRequestURI(),
                    req.getServletPath());
            return;
        } catch (Exception e) {
            ((NusuthContext) context).processError(request, response,
                    500, e.getClass(),
                    "Cannot create "
                    + "transformer, nested:\r\n"
                    + e.getMessage()
                    + "\r\n\r\nRequested uri is \""
                    + req.getRequestURI()
                    + "\"\r\nUsed xsl is \""
                    + getXsl(req) + "\"",
                    e, req.getRequestURI(),
                    req.getServletPath());
            return;
        }
        if (proc != null) {
            String requetUri = req.getRequestURI();
            NusuthXmlResponseWrapper resp
                    = new NusuthXmlResponseWrapper((HttpServletResponse) response);
            synchronized (responseOutPool) {
                if (responseOutPool.size() > 0) {
                    resp.setOutput((ByteArrayOutputStream) responseOutPool.pop());
                } else {
                    resp.setOutput(new ByteArrayOutputStream());
                }
            }
            NusuthXmlRequestWrapper reqWrapper
                    = new NusuthXmlRequestWrapper((HttpServletRequest) request);
            boolean setLastMod = false;
            if (!((Boolean) transformer2boolean.get(proc)).booleanValue()) {
                if (req.getHeader("If-Modified-Since") == null
                        && uri2lastModified.containsKey(requetUri)) {
                    setLastMod = true;
                    reqWrapper.setModifiedSince(
                            ((Long) uri2lastModified.get(requetUri)).longValue());

                }
            } else {
                HttpNusuthRequest httpJBirdRequest = getRealRequest(req);
                httpJBirdRequest.removeHeader("If-Modified-Since");
            }
            chain.doFilter(reqWrapper, resp);
            FileInvocationCacheElement element = null;
            bufUri.append(requetUri);
            if ((element = (FileInvocationCacheElement) cache.find(bufUri)) != null
                    && setLastMod && resp.getStatusCode() == 304) {
                resp.setStatus(200);
                resp.setContentType(element.contentType);
                ServletOutputStream out = response.getOutputStream();
                out.write(element.fileContent);
            } else if (resp.getStatusCode() == 304
                    && !((Boolean) transformer2boolean.get(proc)).booleanValue()) {
                synchronized (responseOutPool) {
                    ByteArrayOutputStream os = resp.getOut();
                    os.reset();
                    responseOutPool.push(os);
                }
                (getRealResponse((HttpServletResponse) response)).
                        removeHeader("Last-Modified");
                return;
            } else {
                transformer2boolean.put(proc, new Boolean(false));
                if (resp.getContentType() != null
                        && resp.getContentType().indexOf("xml") != -1) {
                    ServletOutputStream out = response.getOutputStream();
                    try {
                        XSLProcessor processor = (XSLProcessor) proc.clone();
                        processor.setParser(new SAXParser());
                        OutputMethodHandlerImpl outHandler
                                = new OutputMethodHandlerImpl(processor);
                        NusuthXmlResponseWrapper wr
                                = new NusuthXmlResponseWrapper((HttpServletResponse)
                                response);
                        ByteArrayOutputStream os = null;
                        synchronized (responseOutPool) {
                            if (responseOutPool.size() > 0) {
                                os = (ByteArrayOutputStream) responseOutPool.pop();
                                wr.setOutput(os);
                            } else {
                                os = new ByteArrayOutputStream();
                                wr.setOutput(os);
                            }
                        }
                        Destination dest = new ServletDestination(wr);
                        outHandler.setDestination(dest);
                        processor.setOutputMethodHandler(outHandler);
                        InputStream is = resp.getInputStream();
                        org.xml.sax.InputSource xmlInputSource
                                = new org.xml.sax.InputSource(is);
                        processor.parse(xmlInputSource);
                        byte[] content = os.toByteArray();
                        if (cache.getAvailableSize() - content.length >= 0
                                && (cache.getSize() / 10) >= content.length) {
                            FileInvocationCacheElement element2
                                    = new FileInvocationCacheElement();
                            element2.fileContent = content;
                            element2.contentType = wr.getContentType();
                            cache.add(bufUri.cloneBuf(), element2);
                            cache.setAvailableSize(cache.getAvailableSize() - content.length);
                            uri2lastModified.put(reqWrapper.getRequestURI(),
                                    new Long(System.currentTimeMillis()));
                        }
                        out.write(content);
                        synchronized (responseOutPool) {
                            os.reset();
                            responseOutPool.push(os);
                        }
                    } catch (Exception e) {
                        cat.error("Cannot process transformation", e);
                        ((NusuthContext) context).processError(request, response,
                                500, e.getClass(),
                                "Cannot process "
                                + "transformation, nested:\r\n"
                                + e.getMessage()
                                + "\r\n\r\nRequested uri is \""
                                + req.getRequestURI()
                                + "\"\r\nUsed xsl is \""
                                + getXsl(req) + "\"",
                                e, req.getRequestURI(),
                                req.getServletPath());
                    }
                } else {
                    InputStream is = resp.getInputStream();
                    ServletOutputStream out = response.getOutputStream();
                    int read = 0;
                    byte[] cont = new byte[1024];
                    while ((read = is.read(cont)) != -1) {
                        out.write(cont, 0, read);
                    }
                }
            }
            synchronized (responseOutPool) {
                ByteArrayOutputStream out = resp.getOut();
                out.reset();
                responseOutPool.push(out);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * This method return Templates object using request.
     * @param request HTTP request.
     * @param buf Buffer for cache key.
     * @return Templates object.
     */
    private XSLProcessor getProcessor(HttpServletRequest request, StrBuffer buf) {
        String xsl = getXsl(request);
        if (xsl != null) {
            buf.append(xsl);
        }
        if (xsl == null) {
            return null;
        }
        if (xsl2transformer.containsKey(xsl)) {
            return (XSLProcessor) xsl2transformer.get(xsl);
        } else {
            String uri = request.getServletPath()
                    + (request.getPathInfo() == null ? "" : request.getPathInfo());
            String realPath2 = context.getRealPath(xsl);
            File file2 = new File(realPath2);
            if (file2.exists()) {
                try {
                    org.xml.sax.InputSource xsltInputSource =
                            new org.xml.sax.InputSource(new FileInputStream(file2));
                    XSLProcessor processor = new XSLProcessorImpl();
                    processor.setParser(new SAXParser());
                    processor.loadStylesheet(xsltInputSource);
                    xsl2transformer.put(xsl, processor);
                    uri2xsl.put(uri, xsl);
                    xsl2lastModified.put(xsl, new Long(file2.lastModified()));
                    xsl2realFile.put(xsl, file2);
                    transformer2boolean.put(processor, new Boolean(true));
                    return processor;
                } catch (Exception e) {
                    cat.error("Cannot create transformer", e);
                    xsl2transformer.put(xsl, e);
                    uri2xsl.put(uri, xsl);
                    xsl2lastModified.put(xsl, new Long(file2.lastModified()));
                    xsl2realFile.put(xsl, file2);
                    return (XSLProcessor) xsl2transformer.get(xsl);
                }
            } else {
                cat.error("Incorrect path to xsl file : " + xsl);
            }
        }
        return null;
    }


    /**
     * This method return xsl scheme using request.
     * @param request HTTP request.
     * @return XSL scheme.
     */
    public String getXsl(HttpServletRequest request) {
        String uri = null;
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            String spath = (String) request.
                    getAttribute("javax.servlet.include.servlet_path");
            String cpathinfo = (String) request.
                    getAttribute("javax.servlet.include.path_info");
            uri = spath + (cpathinfo == null ? "" : cpathinfo);
        }
        if (uri == null || uri.length() == 0) {
            uri = request.getServletPath()
                    + (request.getPathInfo() == null ? "" : request.getPathInfo());
        }
        String cloneUri = uri;
        String xsl = (String) uri2xsl.get(uri);
        if (xsl == null) {
            int f = uri.length();
            int k;
            while (f >= 0) {
                uri = uri.substring(0, f);
                xsl = (String) uri2xsl.get(uri.length() == 1
                        ? uri
                        : uri + "/*");
                if (xsl != null) {
                    break;
                }
                k = uri.lastIndexOf("/", f - 1);
                f = k;
            }
            if (xsl == null) {
                f = cloneUri.lastIndexOf(".", cloneUri.length());
                xsl = (String) uri2xsl.get("*." + cloneUri.substring(f + 1,
                        cloneUri.length()));
            }
        }
        if (xsl == null) {
            xsl = (String) uri2xsl.get("/");
        }
        return xsl == null ? null : xsl;
    }

    public void destroy() {
    }

    /**
     * This method return "real" response from given. If given response is
     * wrapper, then this method get response from it.
     * @param response HTTP response or wrapper;
     * @return HttpNusuthResponse Real HTTP response.
     */
    public HttpNusuthResponse getRealResponse(HttpServletResponse response) {
        HttpServletResponse resp = response;
        while (resp instanceof HttpServletResponseWrapper) {
            resp = (HttpServletResponse)
                    ((HttpServletResponseWrapper) resp).getResponse();
        }
        return (HttpNusuthResponse) resp;
    }

    public HttpNusuthRequest getRealRequest(HttpServletRequest request) {
        HttpServletRequest req = request;
        while (req instanceof HttpServletRequestWrapper) {
            req = (HttpServletRequest)
                    ((HttpServletRequestWrapper) req).getRequest();
        }
        return (HttpNusuthRequest) req;
    }


    class XslFileChecker extends Thread {

        private Hashtable xsl2realFile = new Hashtable();
        private Hashtable xsl2lastModified = new Hashtable();

        public XslFileChecker(Hashtable xsl2realFile, Hashtable xsl2lastModified) {
            this.xsl2lastModified = xsl2lastModified;
            this.xsl2realFile = xsl2realFile;
        }

        public void run() {
            while (true) {
                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                }
                Enumeration enum = xsl2realFile.keys();
                while (enum.hasMoreElements()) {
                    String xsl = (String) enum.nextElement();
                    File file = (File) xsl2realFile.get(xsl);
                    if (file.lastModified()
                            != ((Long) xsl2lastModified.get(xsl)).longValue()) {
                        xsl2transformer.remove(xsl);
                        xsl2realFile.remove(xsl);
                        xsl2lastModified.remove(xsl);
                        cache.clear();
                    }
                }
            }
        }

    }

}
