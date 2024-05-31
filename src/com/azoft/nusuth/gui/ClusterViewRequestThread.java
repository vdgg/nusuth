/*
 * @(#)ClusterViewRequestThread.java 1.0 07/31/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.management.ServerState;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Class ClusterViewRequestThread.
 *
 * @version 1.0 07/31/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ClusterViewRequestThread extends RequestThread {
    ClusterViewPanel clusterViewPanel;
    boolean threadSuspended = false;

    public ClusterViewRequestThread(ClusterViewPanel clusterViewPanel) {
        super();
        this.clusterViewPanel = clusterViewPanel;
    }

    public void run() {
        while (true) {
            if (threadSuspended) {
                synchronized (this) {
                    while (threadSuspended) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            try {
                clusterViewPanel.dnd.checkIntensity();
                fireWorkFinished(true, null, true);
            } catch (Exception e) {
                e.printStackTrace();
                fireWorkFinished(false, e, false);
            }
            try {
                Thread.sleep(1000 * clusterViewPanel.refresh);
            } catch (InterruptedException e) {
            }
        }
    }

    public void suspendThread() {
        threadSuspended = true;
    }

    public synchronized void renew() {
        if (!this.isAlive()) {
            start();
            threadSuspended = false;
        } else {
            threadSuspended = false;
            notify();
        }
    }

    protected void fireWorkFinished(boolean b, Exception e, boolean repaint) {
        l.workFinished(new MonitorRequestThreadEvent(this, b, e != null && e instanceof UnauthorizedAccessException, e, repaint));
    }

}
