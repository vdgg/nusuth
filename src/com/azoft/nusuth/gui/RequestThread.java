package com.azoft.nusuth.gui;

import com.azoft.nusuth.management.security.UnauthorizedAccessException;

public class RequestThread extends Thread {
    protected RequestThreadListener l;

    public RequestThread() {
        super();
    }

    public void run() {
        try {
            doWork();
            fireWorkFinished();
        } catch (Exception e) {
            fireWorkFinished(false, e);
        }
    }

    protected void doWork() throws Exception {
    }

    public void setRequestThreadListener(RequestThreadListener l) {
        this.l = l;
    }

    protected void fireWorkFinished() {
        fireWorkFinished(true, null);
    }

    protected void fireWorkFinished(boolean b, Exception e) {
        l.workFinished(new RequestThreadEvent(this, b, e != null && e instanceof UnauthorizedAccessException, e));
    }
}