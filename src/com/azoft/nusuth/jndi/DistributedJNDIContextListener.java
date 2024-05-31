package com.azoft.nusuth.jndi;

import com.azoft.nusuth.jidep.JidepNotification;

/*
 *
 * @author igork
 * @version $VERSION$
 * @since JBird 1.0
 */

public interface DistributedJNDIContextListener {

    void notify(JidepNotification notification);

}
