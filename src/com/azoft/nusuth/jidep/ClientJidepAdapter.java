package com.azoft.nusuth.jidep;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This interface provide methods for client side of communicated parts which
 * communicate via JIDEP protocol.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.4
 */
public interface ClientJidepAdapter {

    public OutputStream getOutputStream();

    public void setCommand(String command);

    public void endRequest() throws IOException;

    public void parseResponse() throws IOException;

    public int getResponseCode();

    public void close() throws IOException;

    /**
     * Process authentication.
     * @param key Authentication key.
     */
    public void processAuthenticate() throws IOException;

    /**
     * This method return content of the response.
     * @return content of thre response.
     */
    public InputStream getInputStream();

    /**
     * This method set header to request.
     */
    public void setHeader(String name, String value);

    /**
     * This method return session associated with the current request.
     * @return JidepSession.
     */
    public JidepSession getSession();

}
