package com.azoft.nusuth.core;

import com.azoft.nusuth.util.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.net.*;

/**This class represents the http response. It contains all methods
 * and attributes that allow to work with response and to hold it's data.
 * @author vdgg, skilz
 * @version 1.43
 * @since Nusuth1.0
 */
public class HttpNusuthResponse extends NusuthResponse
        implements HttpServletResponse, FlushListener {

    private final static String DEFAULT_CHAR_ENCODING = "ISO-8859-1";
    private final static String DEFAULT_CONTENT_TYPE = "text/plain";
    private final static String SESSION_COOKIE_NAME = "JSESSIONID";
    private final static String CONTAINER_COOKIE_NAME = "JCONTAINERID";

    private NusuthServletOutputStream stream;
    private List cookies = null;
    private NusuthHeaders headers = new NusuthHeaders();
    private boolean commited = false;
    private String encoding = DEFAULT_CHAR_ENCODING;
    private Locale locale;
    private int statusCode = 200;
    private String errorMessage = "OK";
    private Cookie containerIDCookie;
    private Cookie sessionIDCookie;
    private PrintWriter pw = null;
    private static HttpErrors httpErrors;
    private boolean chunk = false;
    private HttpNusuthRequest request;
    private boolean usingWriter = false;
    private boolean usingStream = false;
    private static LocaleToCharset locale2charset;
    private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("com.azoft.nusuth.core");
    private String contentType = null;
    private String redirect = null;
    private boolean sendBody = true;
    private boolean emptyBody = false;
    private boolean isOneChunk = false;
//  private boolean idFromHeaders = false;

    private final static byte SPACE = (byte) ' ';
    private final static byte[] CRLF = "\r\n".getBytes();
    private final static byte[] OK_CODE = "200".getBytes();
    private final static byte[] OK_MESSAGE = "OK".getBytes();
    private final static byte[] protocol = "HTTP/1.1".getBytes();
    private final static byte[] standartResponse_NKA = "HTTP/1.1 200 OK\r\nServer: Nusuth/1.0b\r\nConnection: close\r\n".getBytes();
    private final static byte[] standartResponse_KA = "HTTP/1.1 200 OK\r\nServer: Nusuth/1.0b\r\nConnection: Keep-Alive\r\n".getBytes();
    private final static byte[] standartResponse_KA_CHUNKED = "HTTP/1.1 200 OK\r\nServer: Nusuth/1.0b\r\nConnection: Keep-Alive\r\nTransfer-Encoding: chunked\r\n".getBytes();
    private HttpDate httpDate = new HttpDate();

    static {
        httpErrors = new HttpErrors();
        locale2charset = new LocaleToCharset();
    }

    /**The constructor method of the HttpNusuthResponse class.
     * @param stream the output stream.
     * @param chunk True if the chunked transfer-coding is used, otherwise false.
     */
    public HttpNusuthResponse(NusuthServletOutputStream stream) {
        this.stream = stream;
    }

    public void init(NusuthServletOutputStream stream, boolean chunked) {
        this.stream = stream;
        chunk = chunked;
        stream.setFlushListener(this);
        usingStream = false;
        usingWriter = false;
        sendBody = true;
        isOneChunk = false;
        emptyBody = false;
    }

    public void resetBuffer() throws IllegalStateException {
        if (isCommitted())
            throw new IllegalStateException("Cannot reset buffer, response already commited");
        stream.clearBuffer();
    }

    /**This method is called on Flush.
     * It writes request headers to the output stream.
     *@exception IOException Throws if any errors occures during flushing.
     */
    public void onFlush() throws IOException {
        if (!isCommitted()) {
            if (statusCode == 200) {
                if (chunk) {
                    if (headers.containsHeader(HttpConstants.CONTENT_LENGTH)) {
                        stream.write(standartResponse_KA);
                    } else {
                        if (emptyBody || isOneChunk) {
                            stream.setChunked(false);
                            headers.putHeader("Content-Length",
                                    String.valueOf(stream.getBytesInBuffer()));
                            stream.write(standartResponse_KA);
                        } else {
                            stream.setChunked(true);
                            stream.write(standartResponse_KA_CHUNKED);
                        }
                    }
                } else {
                    stream.write(standartResponse_NKA);
                }
                stream.write(HttpDate.getCurrentDateHeader());
                if (cookies == null && headers.length() == 0) {
                    stream.write(CRLF);
                } else {
                    writeHeaders();
                }
            } else {
                headers.putHeader(HttpConstants.SERVER_NAME,
                        HttpConstants.SERVER_VALUE);
                headers.putDateHeader(HttpConstants.DATE, System.currentTimeMillis());
                if (chunk) {
                    headers.putHeader(HttpConstants.CONNECTION, HttpConstants.KEEP_ALIVE);
                    if (!headers.containsHeader(HttpConstants.CONTENT_LENGTH)) {
                        if (emptyBody || isOneChunk) {
                            stream.setChunked(false);
                            headers.putHeader("Content-Length",
                                    String.valueOf(stream.getBytesInBuffer()));
                        } else {
                            headers.putHeader(HttpConstants.TRANSFER_ENCODING,
                                    HttpConstants.CHUNKED);
                            stream.setChunked(true);
                        }
                    }
                } else {
                    headers.putHeader(HttpConstants.CONNECTION, HttpConstants.CLOSE);
                }
                stream.write(protocol);
                stream.write(SPACE);
                stream.write(String.valueOf(statusCode).getBytes());
                stream.write(SPACE);
                if (errorMessage.equals("OK")) {
                    stream.write(OK_MESSAGE);
                } else {
                    stream.write(errorMessage.getBytes());
                }
                stream.write(CRLF);
                writeHeaders();
            }
        }
    }

    public void setBodySend(boolean sendBody) {
        this.sendBody = sendBody;
        stream.setBodySend(sendBody);
    }

    /*This method gets headers and their values from the cookies(for version 0 and 1)
    * and then writes them to the output stream.
    */
    public void writeHeaders() throws IOException {
        commited = true;
        if (cookies != null) {
            Iterator iterator = cookies.iterator();
            while (iterator.hasNext()) {
                Cookie cookie = (Cookie) iterator.next();
                cookie.setVersion(0);
                headers.addHeader(getCookieHeaderName(cookie), getCookieHeaderValue(cookie));
                cookie.setVersion(1);
                headers.addHeader(getCookieHeaderName(cookie), getCookieHeaderValue(cookie));
            }
        }
        headers.write(stream);
    }

    /**If headers are not blocked for writing and this cookie doesn't have the name assigned by the
     *  SESSION_COOKIE_NAME (or the CONTAINER_COOKIE_NAME)constant of the @see Constants class, then
     * this method adds new coookie to the list of cookies associated with this response.
     * @param cookie cookie.
     */
    public void addCookie(Cookie cookie) {
        if (!isHeadersBlocked() && !cookie.getName().equals(SESSION_COOKIE_NAME) &&
                !cookie.getName().equals(CONTAINER_COOKIE_NAME)) {
//          cookie.setDomain(".novosoft.ru");
            if (cookies == null) {
                cookies = new ArrayList();
            }
            cookies.add(cookie);
        }
    }

    /**This method forces any content in the buffer to be written to the
     * client(output stream).
     * @exception IOException is thrown if the output stream is closed or any
     * error occures while writing to the output stream.
     */
    public void flushBuffer() throws IOException {
        if (pw != null) {
            pw.flush();
        }
        try {
            stream.flush();
        } catch (NusuthIOException e) {
            cat.debug("Cannot flush", e);
        }
    }

    /**This method returnes the output stream buffer size.
     * @return the output stream buffer size.
     */
    public int getBufferSize() {
        return stream.getBufferSize();
    }

    /**
     * @return character encoding name.
     */
    public String getCharacterEncoding() {
        return encoding;
    }

    /**
     * @return the locale object.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @return true if writer used, else false.
     */
    public boolean isWriterUsed() {
        return usingWriter;
    }

    /**
     * @return the servlet output stream.
     */
    public ServletOutputStream getOutputStream() {
        if (!usingWriter) {
            usingStream = true;
            return stream;
        } else {
            throw new IllegalStateException("Writer already used");
        }
    }

    /**XXX ???????????????????
     * @return .
     */
    public PrintWriter getWriter() {
        if (!usingStream) {
            if (pw == null) {
                pw = new PrintWriter(stream);
            }
            usingWriter = true;
            return pw;
        } else {
            throw new IllegalStateException("Stream already used");
        }
    }

    /**
     * @return returns a boolean value indicating whether or not any bytes from the response have yet been returned to the client.
     */
    public boolean isCommitted() {
        return (stream.isCommited() || commited);
    }

    /**If a response headers are not blocked for writing then this method
     * clears any data that exists in the buffer as long as the
     * response is not considered to be committed.
     * All headers and cookies are cleared as well.
     * @exception IllegalStateException is thrown if request is already commited.
     */
    public void reset() {
        if (isCommitted()) {
            throw new IllegalStateException("Already committed");
        }
        if (!isHeadersBlocked()) {
            headers.clear();
            if (cookies != null) {
                cookies.clear();
                if (containerIDCookie != null) {
                    cookies.add(containerIDCookie);
                }
                if (sessionIDCookie != null) {
                    cookies.add(sessionIDCookie);
                }
            }
            stream.clearBuffer();
            setContentType("text/html");
        }
        setStatus(200);
    }

    /**This method sets the buffer size of the servlet output stream.
     * @param size new size of the servlet output stream.
     */
    public void setBufferSize(int size) {
        if (stream.getBytesInBuffer() == 0) {
            stream.setBufferSize(size);
        } else {
            throw new IllegalStateException("Buffer already filled");
        }
    }

    /**This method sets the response content length.
     * @param len the response content length.
     */
    public void setContentLength(int len) {
        if (!isHeadersBlocked()) {
            setIntHeader("Content-Length", len);
            stream.setContentLength(len);
        }
    }

    /**This method sets the given parameter as the value of the "Content-Type" header.
     * @param type content type.
     */
    public void setContentType(String type) {
        if (!isHeadersBlocked()) {
            contentType = type;
            if (type.lastIndexOf('=') != -1) {
                encoding = type.substring(type.lastIndexOf('=') + 1, type.length());
            } else {
                encoding = DEFAULT_CHAR_ENCODING;
            }
            setHeader("Content-Type", type);
        }
    }

    /**This method sets locale.
     * @param locale the Locale class object.
     */
    public void setLocale(Locale locale) {
        if (!isHeadersBlocked()) {
            if (locale == null) {
                return;
            } else {
                this.locale = locale;
                String language = locale.getLanguage();
                String langs = headers.getHeader("Content-Language");
                if (langs == null) {
                    headers.putHeader("Content-Language", language);
                } else {
                    headers.putHeader("Content-Language", langs + "," + language);
                }
                String charset = locale2charset.getCharset(locale);
                if (charset != null) {
                    //String contentType = headers.getHeader("Content-Type");
                    if (contentType != null) {
                        int index = contentType.indexOf(";");
                        if (index != -1) {
                            contentType = contentType.substring(0, index);
                        } else {
                            contentType = contentType + "; charset=" + charset;
                        }
                        setContentType(contentType);
                    } else {
                        setContentType(DEFAULT_CONTENT_TYPE + "; charset=" + charset);
                    }
                }
            }
        }
    }


    /**
     * If headers are not blocked for writing then this method adds
     * a new header field whose value is the specified time.
     * The encoding uses RFC 822 date format, as updated by RFC 1123.
     * @param name the header name
     * @param date the time in number of milliseconds since the epoch
     */
    public void addDateHeader(String name, long date) {
        if (!isHeadersBlocked()) {
            headers.addDateHeader(name, date);
        }
    }

    /**
     * If this response headers are not blocked for writing then this method
     * adds a new header field whose value is the specified string.
     * @param name the header name
     * @param value the header field string value
     */
    public void addHeader(String name, String value) {
        if (!isHeadersBlocked()) {
            headers.addHeader(name, value);
        }
    }

    /**If this response headers are not blocked for
     * writing then this method adds a new header
     * field whose value is the specified integer.
     * @param name the header name.
     * @param value the header field integer value.
     */
    public void addIntHeader(String name, int value) {
        if (!isHeadersBlocked()) {
            headers.addIntHeader(name, value);
        }
    }

    /**
     * @param name header name.
     * @return True if this response contains header with the given name.
     */
    public boolean containsHeader(String name) {
//    if (name.equalsIgnoreCase("Content-Type")) {
//      return contentType != null;
//    } else {
        return headers.containsHeader(name);
//    }
    }

    /**XXX ??????????
     */
    public String encodeRedirectUrl(String url) {
        return url;
    }

    /**XXX ??????????
     */
    public String encodeRedirectURL(String url) {
        return url;
    }

    /**XXX ??????????
     */
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    /**XXX ??????????
     */
    public String encodeURL(String url) {
        if (url == null)
            return url;
        if (!request.isRequestedSessionIdFromCookie()) {
            int index = url.indexOf("?");
            if (index > -1) {
                String tmpurl = url.substring(0, index);
                String query = url.substring(index);
                return (tmpurl + ";jsessionid=" + request.getSession().getId() + query);
            } else {
                return (url + ";jsessionid=" + request.getSession().getId());
            }
        } else {
            return url;
        }
    }

    /** this method sends error to the client and closes the output stream.
     * It calles the sendEvent method of this class with the folowing signature
     * sendEvent(int sc, String message). where sc is the given code number and message is
     * the default arror description ssociated with this code number.
     * @param sc error code.
     * @exception IOException is thrown if the response is already commited.
     */
    public void sendError(int sc) throws IOException {
        sendError(sc, httpErrors.getErrorDescription(sc));
    }

    public String getErrorDescription(int code) {
        return httpErrors.getErrorDescription(code);
    }

    /**This method calles the flush method of the request output stream and then closes it.
     */
    private void finish() {
        try {
            if (pw != null) {
                stream.ignoreFlush();
                pw.flush();
            }
//      stream.flush();
            if (!stream.isCommited()) {
                isOneChunk = true;
            }
            if (!stream.hasBody()) {
                emptyBody = true;
            }
            stream.close();
            usingWriter = false;
        } catch (IOException ioex) {
            //Logger.log("Stream already closed", ioex, -1);
            cat.debug("Stream already closed", ioex);
        }
    }

    /**If headers are not blocked then this method sends error code and error message
     * to the client and closes the output stream.
     * @param sc error code.
     * @param message the content body of the error.
     * @exception IOException is thrown if the response is already commited.
     */
    public void sendError(int sc, String message) throws IOException {
        if (isCommitted()) {
            throw new IllegalStateException("Already committed");
        }
        resetBuffer();
        if (sendBody) {
            if (sc == 304 || sc == 204 || sc == 100 || sc == 101) {
                setBodySend(false);
            } else {
                setBodySend(true);
            }
        }
        if (!isHeadersBlocked()) {
            statusCode = sc;
            errorMessage = httpErrors.getErrorDescription(sc);
        }
        if (request == null || request.getContext() == null) {
            setContentType("text/html");
            statusCode = sc;
            errorMessage = httpErrors.getErrorDescription(sc);
            stream.println("<HTML><TITLE>" + httpErrors.getErrorDescription(sc) + "</TITLE><BODY><H1>" +
                    sc + " " + httpErrors.getErrorDescription(sc) + "</H1><br><pre>Context not found</pre></BODY></HTML>");
            finish();
            return;
        }
        request.getContext().processError(request, this, sc, null, message, null, request.getRequestURI(), request.getServletPath());
    }


    /**This method sets the appropriate headers and content body to redirect the
     * client to a different URL.Sends error with the error code defined by the SC_MOVED_PERMANENTLY constant.
     * @param location the new absolute location./XXX ????????
     * @exception IOException is thrown if new URL is not set or if
     */
    public void sendRedirect(String location) throws IOException {
        if (location == null) {
            throw new IllegalArgumentException("Location is null");
        }
        if (isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }
        String absLocation = location;
        URL url = null;
        try {
            url = new URL(location);
        } catch (MalformedURLException muex) {
            try {
                url = new URL(new URL(request.getRequestURL().toString()), location);
                absLocation = url.toString();
            } catch (MalformedURLException nuex1) {
                cat.info("Cannot redirect to this bad location " + location);
            }
        }
        setStatus(302);
        setHeader("Location", absLocation);
        redirect = absLocation;
        cat.info("Redirecting to " + absLocation);
        if (!isHeadersBlocked()) {
            resetBuffer();
            OutputStream out = getOutputStream();
            out.write("<H3>If you are not automatically redirected, please click here</H3><BR>".getBytes());
            out.write(("<a href=" + absLocation + ">" + absLocation + "</a>").getBytes());
            close();
        }
//    sendError(SC_MOVED_PERMANENTLY);
    }

    /**If the response headers are not blocked for writing then it
     * creates a new header field whose value is the specified time.
     * The encoding uses RFC 822 date format, as updated by RFC 1123.
     * @param name the header name
     * @param date the time in number of milliseconds since the epoch
     */
    public void setDateHeader(String name, long date) {
        if (!isHeadersBlocked()) {
            headers.putDateHeader(name, date);
        }
    }

    /**If the response headers are not blocked for writing then it
     * creates a new header field whose value is the specified string.
     * @param name the header name.
     * @param value the header field string value.
     */
    public void setHeader(String name, String value) {
        if (!isHeadersBlocked() && !(name.equalsIgnoreCase("Server") || name.equalsIgnoreCase("Connection"))) {
            headers.putHeader(name, value);
        }
    }

    /**If the response headers are not blocked for writing then it
     * creates a new header field whose value is the specified integer.
     * @param name the header name
     * @param value the header field integer value
     */
    public void setIntHeader(String name, int value) {
        if (!isHeadersBlocked()) {
            headers.putIntHeader(name, value);
        }
    }

    /**If the response headers are not blocked for writing then this method sets
     * the Status-code of the response.
     * @param sc the Status-code of the response.
     */
    public void setStatus(int sc) {
        setStatus(sc, httpErrors.getErrorDescription(sc));
    }

    /**If the response headers are not blocked for writing then this method sets
     * the Status-code of the response.
     * @deprecated Although it is implemented.
     * @param sc the Status-code of the response.(The Status-Code element is a 3-digit integer result code of the attempt to understand and satisfy the request.)
     * @param message the Reason-Phrase,it is intended to give a short textual description of the Status-Code.
     */
    public void setStatus(int sc, String message) {
        if (!isHeadersBlocked()) {
            if (sc == 304 || sc == 204 || sc == 100 || sc == 101) {
                setBodySend(false);
            } else if (!request.getMethod().equals("HEAD")) {
                setBodySend(true);
            }
            statusCode = sc;
            errorMessage = httpErrors.getErrorDescription(sc);
            resetBuffer();
        }
    }

    /**This method returnes the cookie header name depending on it's version.
     * @param cookie cookie.
     * @return the cookie header name.
     */
    public String getCookieHeaderName(Cookie cookie) {
        int version = cookie.getVersion();
        if (version == 1) {
            return "Set-Cookie2";
        } else {
            return "Set-Cookie";
        }
    }


    /**
     * This method returnes the cookie header value depending on it's version.
     * @param cookie cookie.
     * @return the cookie header value.
     */
    public String getCookieHeaderValue(Cookie cookie) {
        StringBuffer buf = new StringBuffer();
        int version = cookie.getVersion();
        buf.append(cookie.getName());
        buf.append("=");
        maybeQuote(version, buf, cookie.getValue());
        if (version == 1) {
            buf.append(";Version=1");
            if (cookie.getComment() != null) {
                buf.append(";Comment=");
                maybeQuote(version, buf, cookie.getComment());
            }
        }
        if (cookie.getDomain() != null) {
            buf.append(";Domain=");
            maybeQuote(version, buf, cookie.getDomain());
        }
        if (cookie.getMaxAge() >= 0) {
            if (version == 1) {
                buf.append(";MaxAge=");
                buf.append(cookie.getMaxAge());
            }
            buf.append(";expires=");
            buf.append(httpDate.convert((long) (cookie.getMaxAge() * 1000)
                    + System.currentTimeMillis()));
        } else if (version == 1) {
            buf.append(";Discard");
        }
        if (cookie.getPath() != null) {
            buf.append(";Path=");
            maybeQuote(version, buf, cookie.getPath());
        }
        if (cookie.getSecure()) {
            buf.append(";Secure");
        }
        return buf.toString();
    }

    private void maybeQuote(int version, StringBuffer buf, String value) {
        if (version == 1) {
            buf.append('"');
            buf.append(value);
            buf.append('"');
        } else {
            buf.append(value);
        }
    }

    /**This method creates cookie with the name @see Constants#SESSION_COOKIE_NAME that contains
     * the given sessionID and adds it to the cookies list of this request.
     * @param sessionID session ID.
     */
    public void setSessionID(String sessionID, NusuthRequest request) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionID);
//    Logger.log("Setting session id to "+sessionID, 1);
        String contPath = request.getContextPath();
        if (contPath != null && contPath.length() > 0) {
            cookie.setPath(contPath);
        } else {
            cookie.setPath("/");
        }
//    System.out.println("!!!!!!!!!!!!!!!Context path: "+request.getContextPath());
/*    if (request.getServerName() != null) {
      System.out.println("!!!!!!!!!!!!!!!Server name: "+request.getServerName());
      cookie.setDomain(request.getServerName());
    }*/
        cookie.setVersion(1);
        cookie.setMaxAge(-1);
        if (cookies == null) {
            cookies = new ArrayList();
        }
        cookies.add(cookie);
        sessionIDCookie = cookie;
    }

    public void setContainerId(String contPath) {
        if (containerID != null) {
            containerIDCookie = new Cookie(CONTAINER_COOKIE_NAME, containerID);
            if (contPath != null && contPath.length() > 0) {
                containerIDCookie.setPath(contPath);
            } else {
                containerIDCookie.setPath("/");
            }
            containerIDCookie.setVersion(1);
            containerIDCookie.setMaxAge(-1);
            if (cookies == null) {
                cookies = new ArrayList();
            }
            cookies.add(containerIDCookie);
        }
    }

    protected void removeHeader(String name) {
        headers.clearHeader(name);
    }

    /**This method creates cookie with the name @see Constants#CONTAINER_COOKIE_NAME that contains
     * the given containerID and adds it to the cookies list of this request.
     * @param containerID container ID.
     */
/*  public void setContainerID(String ID) {
    if (cookies == null) {
      cookies = new ArrayList();
    }
    containerIDCookie = new Cookie(Constants.CONTAINER_COOKIE_NAME, ID);
    cookies.add(containerIDCookie);
  }
*/
/*
  private static class HttpErrors {
    private String[] [] errorDescriptions = null;
    private final String NO_DESCRIPTION = "No Description";
    private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("core");

    HttpErrors() {
      ResourceBundle bundle = ResourceBundle.getBundle("com.azoft.nusuth.core.HttpErrors");
      Enumeration enum = bundle.getKeys();
      int[] indexes = new int[9];
      int code;
      int index;
      int sin;
      int max = 0;
      while (enum.hasMoreElements()) {
        try {
          code = Integer.parseInt((String)enum.nextElement());
          index = code / 100 - 1;
          max = index > max ? index : max;
          sin = code % 100 + 1;
          if (sin > indexes[index]) {
            indexes[index] = sin;
          }
        } catch (NumberFormatException nfe) {
          //Logger.log("Error in http errors file", 0);
          cat.warn("Error in http errors file");
          return;
        }
      }
      errorDescriptions = new String[max + 1] [1];
      index = 0;
      String desc;
      while (indexes[index] > 0) {
        errorDescriptions[index] = new String[indexes[index]];
        index++;
      }
      for (int i = 0; i < errorDescriptions.length; i++) {
        for (int j = 0; j < errorDescriptions[i].length; j++) {
          desc = bundle.getString((new Integer((i + 1) * 100 + j)).toString());
          errorDescriptions[i] [j] = desc == null ? NO_DESCRIPTION : desc;
          //System.out.println(((i + 1) * 100 + j) + " - " + desc);
        }
      }
    }

    String getErrorDescription(int errorCode) {
      if (errorDescriptions == null) {
        return NO_DESCRIPTION;
      }
      int findex = errorCode / 100 - 1;
      int sindex = errorCode % 100;
      return (findex < 0 || sindex < 0 || findex >= errorDescriptions.length || findex >= errorDescriptions[findex].length) ?
          NO_DESCRIPTION : errorDescriptions[findex] [sindex];
    }
  }

*/

    private static class LocaleToCharset {

        Properties map = new Properties();
        private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("core");

        LocaleToCharset() {
            File file = null;
            InputStream is = null;
            try {
                is = ClassLoader.getSystemClassLoader().getResourceAsStream("com/azoft/nusuth/core/LocaleToCharset.properties");
                map.load(is);
            } catch (IOException ex) {
                //Logger.log("IO error", 1);
                cat.error("IO error", ex);
            }
        }

        String getCharset(Locale loc) {
            String charset;
            charset = (String) map.get(loc.toString());
            if (charset != null) return charset;
            charset = (String) map.get(loc.getLanguage());
            return charset;
        }

    }


    /**This method calles the finish method of this class.
     */
    protected void close() {
/*    if (chunk) {
      try {
        stream.close();
      } catch (IOException ioex) {
        Logger.log("Exception while close", ioex, -1);
//        ioex.printStackTrace();
      }
    } else {
      finish();
    }*/
        finish();
    }

    /**This method sets the requested URL, gathered from the request.
     * @param requestedURL the URL where the corresponding request was send .
     */
    public void setRequest(HttpNusuthRequest request) {
        this.request = request;
    }

    /**This method sets flush listener to the servlet out stream.
     * @param listener flush listener.
     */
    public void setFlushListener(FlushListener listener) {
        stream.setFlushListener(listener);
    }

    /**This method gets the Status-Code of the response.
     * @return the Status-Code of the response.
     */
    public int getStatusCode() {
        return statusCode;
    }

    public void cleanup() {
        super.cleanup();
        stream = null;
        if (cookies != null) {
            cookies.clear();
        }
        headers.clear();
        commited = false;
        contentType = null;
//    encoding = null;
        encoding = DEFAULT_CHAR_ENCODING;
        locale = null;
        statusCode = 200;
        errorMessage = "OK";
        sessionIDCookie = null;
        pw = null;
        chunk = false;
        request = null;
        usingWriter = false;
        usingStream = false;
    }

    boolean[] getStreamWriterUsage() {
        boolean[] b = new boolean[2];
        b[0] = usingStream;
        b[1] = usingWriter;
        return b;
    }

    public void setStreamWriterUsage(boolean[] b) {
        usingStream = b[0];
        usingWriter = b[1];
    }

    public int getNumberOfBytesTransfered() {
        return (stream != null) ? stream.getNumberOfBytesTransfered() : 0;
    }


}

