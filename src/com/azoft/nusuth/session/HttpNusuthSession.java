package com.azoft.nusuth.session;

import java.util.LinkedList;

import com.azoft.nusuth.core.NusuthContext;

/**
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
public class HttpNusuthSession extends NusuthSession {

    public HttpNusuthSession(LinkedList sessionListeners, LinkedList sessionAttrListeners, NusuthContext context) {
        super(sessionListeners, sessionAttrListeners, context);
    }

}

