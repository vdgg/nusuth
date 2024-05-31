package com.azoft.nusuth.gui;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.lang.reflect.Method;

import com.azoft.nusuth.management.ServerState;
import com.azoft.nusuth.management.ManagementException;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;

public class MonitorRequestThread extends RequestThread {
    Monitor monitor;
    boolean threadSuspended = false;
    long begin, end; // time of working

    public MonitorRequestThread(Monitor monitor) {
        super();
        this.monitor = monitor;
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
            begin = MonitorContainer.getMillis();
            Enumeration keys = monitor.graphsBySystemId.keys();
            while (keys.hasMoreElements()) {
                String systemId = (String) keys.nextElement();
                ServerState serverState = null;
                try {
                    serverState = (monitor.getType().equals("container")) ? (ServerState) BasicPanel.getContainerState(systemId) : (ServerState) BasicPanel.getDistributorState(systemId);
                    end = MonitorContainer.getNow();
                    Vector nextGraphs = (Vector) monitor.graphsBySystemId.get(systemId);
                    boolean hasEnabledGraph = false;
                    if (nextGraphs != null) {
                        for (Enumeration en = nextGraphs.elements(); en.hasMoreElements();) {
                            Graph gr = (Graph) en.nextElement();
                            int value = getFunctionValue(serverState, gr.getFunctionName(), gr.getArgs(), systemId);
                            gr.addRealValue(value, end);
                            if (gr.isEnabled()) hasEnabledGraph = true;
                        }
                    }
                    fireWorkFinished(true, null, hasEnabledGraph);
                } catch (Exception e) {
                    fireWorkFinished(false, e, false);
                }
            }
            end = MonitorContainer.getMillis();
            long trest = (long) (1000 * monitor.getStep() - (end - begin));
            while (trest < 0) {
                trest += 1000 * monitor.getStep();
            }
            try {
                Thread.sleep(trest);
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

    private int getFunctionValue(ServerState state, String grName, Object[] args, String systemId) {
        int value = -1;
        int indexOfAll = -1;
        if (args != null && args.length > 0) {
            // has arguments
            if (args[0] != null && args[0] instanceof String)
                indexOfAll = ((String) args[0]).indexOf("/" + GraphsTable.ALLAPPS);
        }
        try {
            Method method = null;
            String funName = (String) BasicPanel.functionsProps.get(grName);
            Class[] classes = (Class[]) BasicPanel.functionsProps.get(funName);
            if (state != null) {
                method = state.getClass().getMethod(funName, classes);
                if (method != null) {
                    if (indexOfAll == -1)
                        value = ((Integer) method.invoke(state, args)).intValue();
                    else {
                        value = 0;
                        Object[] argstmp = new Object[1];
                        String host = ((String) args[0]).substring(0, indexOfAll);
                        Vector apps = BasicPanel.getContextsByHostName(host);
                        if (apps != null) {
                            int cnt = apps.size();
                            for (int i = 0; i < cnt; i++) {
                                String nextApp = (String) apps.elementAt(i);
                                if (!nextApp.equals(GraphsTable.ALLAPPS)) {
                                    argstmp[0] = host + "/" + nextApp;
                                    value += ((Integer) method.invoke(state, argstmp)).intValue();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("getState " + e);
            e.printStackTrace();
        }
        return value;
    }

}
