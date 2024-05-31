package com.azoft.nusuth.core;

import javax.servlet.http.*;
import javax.servlet.*;
import java.net.URLDecoder;
import java.io.*;
import java.util.*;

import com.azoft.nusuth.container.*;
import com.azoft.nusuth.util.*;
import com.azoft.nusuth.container.http.HttpProtocolAdapter;

/**
 * This servlet serve requests for static resources.
 * @author vdgg, skilz
 * @version 1.44
 * @since Nusuth1.0
 */
public class FileServlet extends HttpServlet {

    public final static String FILE_SERVLET_HEADER_NAME_STRING
            = "Nusuth_File_Servlet_Produced";
    public final static char[] FILE_SERVLET_HEADER_NAME
            = FILE_SERVLET_HEADER_NAME_STRING.toCharArray();
    private final static String HTTP_LAST_MODIFIED
            = new String(HttpConstants.LAST_MODIFIED);

    private String docBase;
    private List welcomeFiles;
    private Hashtable mimeTypes;
    private int showCount = 0;
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.core");
    private static InvocationCache cache;
    private static boolean initialized;
    private static int checkInterval;
    private static int cacheSize;
//  private static boolean winOs = false;

    static {
        initialized = false;
//    if (System.getProperty("os.name").toLowerCase().indexOf("win") != -1) {
//      winOs = true;
//    }
    }

    public void init(ServletConfig conf) throws ServletException {
        if (!initialized) {
            String checkInterval = null;
            String cacheSize = null;
            int check = 60000;
            int size = 1048576;
            checkInterval
                    = conf.getInitParameter("check-interval").toLowerCase().trim();
            cacheSize = conf.getInitParameter("cache-size").toLowerCase().trim();
            synchronized (this) {
                if (checkInterval != null) {
                    try {
                        if (checkInterval.toLowerCase().trim().endsWith("h")) {
                            check = Integer.parseInt(
                                    checkInterval.substring(0, checkInterval.length() - 1))
                                    * 3600000;
                        } else {
                            if (checkInterval.toLowerCase().trim().endsWith("m")) {
                                check = Integer.parseInt(
                                        checkInterval.substring(0, checkInterval.length() - 1))
                                        * 60000;
                            } else {
                                if (checkInterval.toLowerCase().trim().endsWith("s")) {
                                    check = Integer.parseInt(
                                            checkInterval.substring(0, checkInterval.length() - 1))
                                            * 1000;
                                } else {
                                    check = Integer.parseInt(checkInterval) * 1000;
                                }
                            }
                        }
                    } catch (NumberFormatException ex) {
                        System.err.println("Wrong value in \'check-interval\' init "
                                + "parameter value. Default value will be "
                                + "used (1 minute)...");
                    }
                } else {
                    check = 60000;
                }
                if (cacheSize != null) {
                    try {
                        String tempcacheSize;
                        if (!(cacheSize.substring(cacheSize.length() - 1).equals("b") ||
                                cacheSize.substring(cacheSize.length() - 1).equals("k") ||
                                cacheSize.substring(cacheSize.length() - 1).equals("m"))) {
                            size = 1024 * Integer.parseInt(cacheSize);
                        } else {
                            if (cacheSize.substring(cacheSize.length() - 2).equals("kb")) {
                                tempcacheSize = cacheSize.substring(0, cacheSize.length() - 2);
                                size = 1024 * Integer.parseInt(
                                        cacheSize.substring(0, cacheSize.length() - 2));
                            } else {
                                if (cacheSize.substring(cacheSize.length() - 2).equals("mb")) {
                                    tempcacheSize = cacheSize.substring(0, cacheSize.length() - 2);
                                    size = 1048576 * Integer.parseInt(
                                            cacheSize.substring(0, cacheSize.length() - 2));
                                }
                            }
                            if (cacheSize.substring(cacheSize.length() - 1).equals("k")) {
                                tempcacheSize = cacheSize.substring(0, cacheSize.length() - 1);
                                size = 1024 * Integer.parseInt(
                                        cacheSize.substring(0, cacheSize.length() - 1));
                            } else {
                                if (cacheSize.substring(cacheSize.length() - 1).equals("m")) {
                                    tempcacheSize = cacheSize.substring(0, cacheSize.length() - 1);
                                    size = 1048576 * Integer.parseInt(
                                            cacheSize.substring(0, cacheSize.length() - 1));
                                } else {
                                    if (cacheSize.substring(cacheSize.length() - 1).equals("b")) {
                                        tempcacheSize = cacheSize.substring(
                                                cacheSize.length() - 2, cacheSize.length() - 1);
                                        if (!(tempcacheSize.equals("k")
                                                || tempcacheSize.equals("m"))) {
                                            size = Integer.parseInt(
                                                    cacheSize.substring(0, cacheSize.length() - 1));
                                        }
                                    }
                                }
                            }
                        }
                    } catch (NumberFormatException ex) {
                        System.err.println("Wrong value in \'cache-size\' init parameter"
                                + " value. Default value will be used (1Mb)...");
                    }
                } else {
                    size = 1048576;
                }
            }
            initialized = true;
            this.checkInterval = check;
            this.cacheSize = size;
            if (check != 0) {
                cache = new InvocationCache(check);
            } else {
                cache = new InvocationCache(60000);
            }
            if (size != 0) {
                cache.setSize(size);
                cache.setAvailableSize(size);
            } else {
                cache.setSize(1048576);
                cache.setAvailableSize(1048576);
            }
        }
        super.init(conf);
        NusuthContext context = (NusuthContext) conf.getServletContext();
        docBase = context.getDocBase();
        welcomeFiles = context.getWelcomeFiles();
        mimeTypes = context.getMimeTypes();
    }

    /**
     * This method process requests for static resource.
     * @param request HttpRequest.
     * @param response HttpResponse.
     */
    protected void service(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {
        long ifModSince = -1;
        if ((ifModSince = request.getDateHeader("If-Modified-Since")) != -1) {
            if (request.getMethod().equals("GET")
                    || request.getMethod().equals("HEAD")) {
                long lastMod = getLastModified(request);
                if (lastMod != -1) {
                    lastMod = (lastMod / 1000) * 1000;
                    if (ifModSince < lastMod) {
                        response.setDateHeader("Last-Modified", lastMod);
                    } else {
                        if (response instanceof NusuthXmlResponseWrapper) {
                            response.setDateHeader("Last-Modified", lastMod);
                        }
                        response.setStatus(304);
                        return;
                    }
                }
            }
        }
        if (request.getMethod().equals("HEAD")) {
            HttpNusuthResponse resp = getRealResponse(response);
            ((HttpNusuthResponse) resp).setBodySend(false);
        }
        boolean[] swu = {true, false};
        boolean[] swuold = {false, true};
        boolean writerUsed = false;
        HttpNusuthResponse resp = getRealResponse(response);
        if (resp.isWriterUsed()) {
            writerUsed = true;
            resp.getWriter().flush();
            resp.setStreamWriterUsage(swu);
        }
        StrBuffer uri = new StrBuffer();
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            uri.append((String) request.getAttribute("javax.servlet.include."
                    + "request_uri"));
        } else {
            uri.append(request.getRequestURI());
        }

        // added by IgorK
        // for distributor caching
        if (request.getHeader(HttpProtocolAdapter.REMOTE_ADDR_HEADER_NAME) != null)
            response.setHeader(FILE_SERVLET_HEADER_NAME_STRING, "");
        //

        FileInvocationCacheElement element =
                (FileInvocationCacheElement) cache.find(uri);
        if (element != null && request.getHeader("Range") == null &&
                element.fileLastCheck > (System.currentTimeMillis() - checkInterval)) {
            if (element.contentType != null) {
                response.setContentType(element.contentType);
            }
            response.setDateHeader("Last-Modified", element.fileLastModified);
            OutputStream os = response.getOutputStream();
            os.write(element.fileContent, 0, element.fileContent.length);
        } else {
            if (request.getContextPath() != null
                    && !request.getContextPath().endsWith("/")
                    && (request.getServletPath() == null
                    || request.getServletPath().length() == 0)
                    && (request.getPathInfo() == null
                    || request.getPathInfo().length() == 0)) {
//                request.getRequestDispatcher("/").forward(request, response);
                response.sendRedirect(request.getContextPath() + "/");
                return;
            }
            boolean includePathComplete = false;
            String path = (String) request.getAttribute("javax.servlet.include."
                    + "servlet_path");
            if (path == null || path.trim().length() == 0) {
                path = (String) request.getAttribute("javax.servlet.include.path_info");
            }
            if (path == null) {
                path = request.getServletPath();
            } else {
                includePathComplete = true;
            }
            if (path == null) {
                path = request.getPathInfo();
            } else if (!includePathComplete) {
                path = path + ((request.getPathInfo() == null)
                        ? "" : request.getPathInfo());
            }
            if (path != null) {
                try {
                    path = URLConverter.decodeURL(path);
                } catch (Exception illEx) {
                    cat.error("Bad request", illEx);
                    ((NusuthContext) getServletContext()).processError(request, response,
                            400,
                            null,
                            "Can't decode url \""
                            + path + "\"", null,
                            request.
                            getRequestURI(),
                            request.
                            getServletPath());
                    if (writerUsed) {
                        resp.setStreamWriterUsage(swuold);
                    }
                    return;
                }
            }
            if ((path != null && path.indexOf("..") > -1)
                    || (path != null && path.endsWith(".jsp."))) {
                ((NusuthContext) getServletContext()).
                        processError(request, response,
                                HttpServletResponse.SC_NOT_FOUND, null,
                                "Resource " + path + " not found", null,
                                request.getRequestURI(),
                                request.getServletPath());
                if (writerUsed) {
                    resp.setStreamWriterUsage(swuold);
                }
                return;
            }
            if (path != null && (path.toUpperCase().startsWith("/WEB-INF/")
                    || path.toUpperCase().startsWith("/META-INF/"))) {
                ((NusuthContext) getServletContext()).
                        processError(request, response,
                                HttpServletResponse.SC_FORBIDDEN, null,
                                "Forbidden", null, request.getRequestURI(),
                                request.getServletPath());
                if (writerUsed) {
                    resp.setStreamWriterUsage(swuold);
                }
                return;
            }
            File file = null;
            if (path == null) {
                if (welcomeFiles.size() > 0) {
                    int i = 0;
                    do {
                        file = new File(docBase + "/" + (String) welcomeFiles.get(i++));
                    } while (i < welcomeFiles.size() && !file.exists());
                    if (file != null && file.exists()) {
                        request.getRequestDispatcher((String) welcomeFiles.get(i - 1)).forward(request, response);
//                        response.sendRedirect((String) welcomeFiles.get(i - 1));
                        return;
                    }
                }
            } else {
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                file = new File(docBase + path);
            }
            if (file == null || !file.exists() ||
                    !((NusuthContext) getServletContext()).containsResource(path)) {
                ((NusuthContext) getServletContext()).
                        processError(request, response,
                                HttpServletResponse.SC_NOT_FOUND, null,
                                "Resource " + path + " not found", null,
                                request.getRequestURI(),
                                request.getServletPath());
                if (writerUsed) {
                    resp.setStreamWriterUsage(swuold);
                }
                return;
            } else {
                if (!file.isDirectory()) {
                    String range = null;
                    int startPos = 0;
                    int endPos = (int) file.length() - 1;
                    int realEndPos = endPos;
                    range = request.getHeader("Range");
                    if (range != null) {
                        int[] array = findPosition(range, startPos, endPos);
                        startPos = array[0];
                        endPos = array[1];
                        response.setHeader("Accept-Ranges", "bytes");
                        if ((startPos != 0) || (endPos != (file.length() - 1))) {
                            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                            response.setHeader("Content-Range", "bytes " + startPos + "-"
                                    + endPos + "/" + (int) file.length());
                        }
                    }
                    FileInputStream is = null;
                    try {
                        is = new FileInputStream(file);
                        OutputStream os = response.getOutputStream();
                        os = response.getOutputStream();
                        int ind = path.lastIndexOf(".");
                        String conType = null;
                        if (ind > -1) {
//              if (!winOs) {
                            conType = (String) mimeTypes.get(path.substring(ind + 1));
//              } else {
//                conType = (String)mimeTypes.get(path.substring(ind+1).
//                                                toLowerCase());
//              }
                            if (conType != null) {
                                response.setContentType(conType);
                            }
                        }
                        response.setContentLength(endPos - startPos + 1);
                        response.setDateHeader(HTTP_LAST_MODIFIED, file.lastModified());
                        if (startPos > 0) {
                            long totalSkipped = 0;
                            long skipped = 0;
                            while (totalSkipped < startPos
                                    && (skipped = is.skip(startPos - totalSkipped)) > -1) {
                                totalSkipped += skipped;
                            }
                            if (totalSkipped != startPos) {
                                throw new IOException("Cannot skip " + startPos + " bytes");
                            }
                        }
                        byte[] buf = new byte[1024];
                        int read;
                        long availSize = cache.getAvailableSize();
                        long cacheSize = cache.getSize();
                        if (endPos == -1) {
                            if (writerUsed) {
                                resp.setStreamWriterUsage(swuold);
                            }
                            return;
                        }
                        if (startPos == 0 && endPos == realEndPos
                                && (availSize - endPos - 1 >= 0)
                                && (cacheSize / (endPos + 1) >= 10)) {
                            if (element == null) {
                                element = new FileInvocationCacheElement();
                                element.fileContent = new byte[endPos + 1];
                                element.fileLastModified = file.lastModified();
                                element.fileLastCheck = System.currentTimeMillis();
                                if (conType != null) {
                                    element.contentType = conType;
                                }
                                response.setDateHeader("Last-Modified",
                                        element.fileLastModified);
                                while ((read = is.read(element.fileContent)) > -1) {
                                    os.write(element.fileContent, 0, read);
                                }
                                cache.setAvailableSize(availSize - endPos - 1);
                                cache.add(uri, element);
                            } else {
                                if (element.fileLastCheck
                                        < (System.currentTimeMillis() - checkInterval)) {
                                    if (file.lastModified() != element.fileLastModified) {
                                        cache.remove(uri);
                                        element = new FileInvocationCacheElement();
                                        element.fileContent = new byte[endPos + 1];
                                        element.fileLastModified = file.lastModified();
                                        element.fileLastCheck = System.currentTimeMillis();
                                        if (conType != null) {
                                            element.contentType = conType;
                                        }
                                        response.setDateHeader("Last-Modified",
                                                element.fileLastModified);
                                        while ((read = is.read(element.fileContent)) > -1) {
                                            os.write(element.fileContent, 0, read);
                                        }
                                        cache.setAvailableSize(availSize - endPos - 1);
                                        cache.add(uri, element);
                                    } else {
                                        element.fileLastCheck = System.currentTimeMillis();
                                        response.setDateHeader("Last-Modified",
                                                element.fileLastModified);
                                        while ((read = is.read(element.fileContent)) > -1) {
                                            os.write(element.fileContent, 0, read);
                                        }
                                    }
                                } else {
                                    while ((read = is.read(element.fileContent)) > -1) {
                                        os.write(element.fileContent, 0, read);
                                    }
                                }
                            }
                        } else {
                            while ((read = is.read(buf)) > -1) {
                                os.write(buf, 0, read);
                            }
                        }
                    } catch (IOException ioex) {
                        cat.info("Exception occured", ioex);
                        ((NusuthContext) getServletContext()).
                                processError(request, response,
                                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                        null, "Internal server error", null,
                                        request.getRequestURI(),
                                        request.getServletPath());
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException ex) {
                                cat.error("Unable to close inputstream", ex);
                            }
                        }
                    }
                } else {
                    if (!path.endsWith("/")) {
                        response.sendRedirect(request.getContextPath()
                                + (request.getContextPath().endsWith("/")
                                ? path.substring(1, path.length())
                                : path) + "/");

                        return;
                    }
                    File[] files = file.listFiles();
                    int index = -1;
                    for (int i = 0; i < files.length; i++) {
                        if (welcomeFiles.contains(files[i].getName())) {
                            index = i;
                        }
                    }
                    if (index == -1) {
                        String query = "?uri=" + request.getRequestURI() + "&docBase="
                                + docBase + "&file=" + file.getAbsolutePath();
                        request.setAttribute("uri", request.getRequestURI());
                        request.setAttribute("docBase", docBase);
                        request.setAttribute("file", file.getAbsolutePath());
                        ServletContext context = getServletConfig().getServletContext();
                        RequestDispatcher disp
                                = context.getNamedDispatcher("_nusuth_dir_servlet");
                        response.reset();
                        disp.include(request, response);
                    } else {
                        request.getRequestDispatcher(files[index].getName()).forward(request, response);
//                        response.sendRedirect(files[index].getName());
                    }
                }
            }
        }
        if (writerUsed) {
            resp.setStreamWriterUsage(swuold);
        }
    }

    protected long getLastModified(HttpServletRequest request) {
        String path = request.getServletPath() + request.getPathInfo();
        if (path == null || path.length() < 1 || path.indexOf("..") > -1) {
            return -1;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        File file = new File(docBase + path);
        if (!file.exists() || file.isDirectory()) {
            return -1;
        } else {
            return file.lastModified();
        }
    }

    private String cutString(String s) {
        if (s.endsWith("/")) {
            return cutString(s.substring(0, s.length() - 1));
        } else {
            return s.substring(0, s.lastIndexOf("/"));
        }
    }

    private int[] findPosition(String str, int start, int stop) {
        String strstart;
        String strstop;
        int start1;
        int stop1;
        str = str.substring(str.indexOf('=') + 1);
        int index = str.indexOf('-');
        strstart = str.substring(0, index);
        strstop = str.substring(index + 1);
        if (strstart.length() != 0 && strstop.length() != 0) {
            start1 = Integer.parseInt(strstart);
            stop1 = Integer.parseInt(strstop);
        } else {
            if (strstart.length() == 0 && strstop.length() != 0) {
                start1 = stop + 1 - Integer.parseInt(strstop);
                stop1 = stop;
            } else {
                start1 = Integer.parseInt(strstart);
                stop1 = stop;
            }
        }
        int[] array = new int[2];
        array[0] = start1;
        array[1] = stop1;
        return array;
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

}

