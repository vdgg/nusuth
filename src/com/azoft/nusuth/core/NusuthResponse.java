package com.azoft.nusuth.core;

import javax.servlet.*;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;

/**The abstract class encapsulates all information from the client response.
 * It also represents methods that allow to work with the response data.
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
public abstract class NusuthResponse implements ServletResponse, SessionCreationListener {

    private boolean headersBlocked = false;

    protected static String containerID;

    public abstract void resetBuffer() throws IllegalStateException;

    /**Realization of this abstract method will set the session ID for this response.
     * @param sessionID session ID.
     */
    protected abstract void setSessionID(String sessionID, NusuthRequest request);

    /**This method is used to forbid servlet, which response should be included in this responce,
     * to change the response headers.
     */
    protected final void blockHeaders() {
        headersBlocked = true;
    }

    /** This method unblocks the response headers.
     */
    protected final void unblockHeaders() {
        headersBlocked = false;
    }

    /**
     * @return True if response headers are blocked for writing , otherwise False.
     */
    protected final boolean isHeadersBlocked() {
        return headersBlocked;
    }

    /**Realization of this abstract method will close response object for writing.
     * Is called before sending response for client.
     */
    protected abstract void close();

    /**This method saves session ID (if the new session was created for this request) in the response object.
     * @param session created session.
     */
    public void sessionCreated(HttpSession session, NusuthRequest request) {
        setSessionID(session.getId(), request);
    }

    public void cleanup() {
        headersBlocked = false;
    }

    public static void setContainerID(String id) {
        containerID = id;
    }

    abstract boolean[] getStreamWriterUsage();

    abstract void setStreamWriterUsage(boolean[] b);

}

