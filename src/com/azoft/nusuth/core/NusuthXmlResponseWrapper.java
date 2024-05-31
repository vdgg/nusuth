package com.azoft.nusuth.core;

import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.io.*;

/**
 * This class is wrapper for HttpServletResponse. It used in XSLTFilter.
 * @author skilz.
 * @version 1.5
 * @since Nusuth1.0
 */
public class NusuthXmlResponseWrapper extends HttpServletResponseWrapper {

    /**Response output*/
    private OutputStream output;
    /**Content length*/
    private int contentLength;
    /**Conteny type*/
    private String contentType;

    /**
     * Constructor.
     * @param response HTTP Response to wrap.
     */
    public NusuthXmlResponseWrapper(HttpServletResponse response) {
        super(response);
//    output = new ByteArrayOutputStream();
    }

    /**
     * Returns content length
     *@return contet length
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * Returns content type.
     * @return content type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Return input stream (Stream of writed data).
     * @return Input stream.
     */
    public InputStream getInputStream() {
        return new ByteArrayInputStream(((ByteArrayOutputStream) output).
                toByteArray());
    }

    /**
     * Return response output.
     * @return response output.
     */
    public ServletOutputStream getOutputStream() {
        return new XmlFilterServletOutputStream(output);
    }

    /**
     * Return response writer.
     * @return response writer.
     */
    public PrintWriter getWriter() {
        return new PrintWriter(getOutputStream(), true);
    }

    /**
     * Sets content length.
     * @param i Content length
     */
    public void setContentLength(int i) {
        contentLength = i;
    }

    /**
     * Sets content type
     * @param s Content type
     */
    public void setContentType(String s) {
        super.setContentType(s);
        contentType = s;
    }

    public int getStatusCode() {
        HttpNusuthResponse res = getRealResponse((HttpServletResponse) getResponse());
        return res.getStatusCode();
    }

    /**
     * This method return "real" response from given. If given response is
     * wrapper, then this method get response from it.
     * @param response HTTP response or wrapper;
     * @return HttpNusuthResponse Real HTTP response.
     */
    private HttpNusuthResponse getRealResponse(HttpServletResponse response) {
        HttpServletResponse resp = response;
        while (resp instanceof HttpServletResponseWrapper) {
            resp = (HttpServletResponse)
                    ((HttpServletResponseWrapper) resp).getResponse();
        }
        return (HttpNusuthResponse) resp;
    }

    public void setOutput(ByteArrayOutputStream out) {
        output = out;
    }

    public ByteArrayOutputStream getOut() {
        return (ByteArrayOutputStream) output;
    }

    public void flushBuffer() throws IOException {
    }
}
