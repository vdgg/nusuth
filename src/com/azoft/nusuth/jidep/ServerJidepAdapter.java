package com.azoft.nusuth.jidep;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This interface provide methods for server side of communicated parts which
 * communicate via JIDEP protocol.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.3
 */
public interface ServerJidepAdapter {

    /**
     * This method return requested command
     * @return Requested command
     */
    public String getCommand();

    /**
     * This method ends the response.
     * @exception IOException Throws if any errors occures during sending response
     * to the client
     */
    public void endResponse() throws IOException;

    /**
     * This method sets status code to response.
     * @param status Status code.
     */
    public void setStatus(int status);

    /**
     * Returns the requests content.
     * @return Requests content as arrar of bytes.
     */
    public InputStream getInputStream();

    /**
     * Parses the request.
     * @exception IOException Throws if any errors occures during parsing.
     */
    public void parseRequest() throws IOException;

    /**
     * Cleaning up the protocol adapter.
     */
    public void cleanup();

    /**
     * This method close connection with client.
     * @exception IOException Throws if any errors occures during closing
     * connection.
     */
    public void close() throws IOException;

    /**
     * Return output stream.
     * @return OutputStream.
     */
    public OutputStream getOutputStream();

    /**
     * This method set header to response.
     */
    public void setHeader(String name, String value);

    /**
     * This method return Jidep session.
     * @return Jidep session.
     */
    public JidepSession getSession();

}
