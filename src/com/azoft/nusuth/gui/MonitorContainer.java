package com.azoft.nusuth.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.beans.PropertyVetoException;
import javax.swing.*;

public class MonitorContainer extends JDesktopPane {
    public BasicPanel basicPanel;
    Vector monitors = new Vector();
    private GridBagConstraints c;
    public javax.swing.Timer timer;
    private int savingDelay = 60000; // 10 sec
    public static long time = 0;
    public static long beginTime = 0;
    Date tmpDate = new Date();
    java.text.DateFormat dateFormat;
    Monitor activeMonitor = null;
    static int lastId = 0;

    public MonitorContainer(BasicPanel basicPanel, Properties monitorProps) {
        super();
        this.basicPanel = basicPanel;
        setOpaque(false);

        c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;

        StringTokenizer st = new StringTokenizer(monitorProps.getProperty("monitor.names", ""), ";", false);
        StringTokenizer stg;
        MonitorInfo monitorInfo;
        Vector v;

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            monitorInfo = new MonitorInfo();
            monitorInfo.name = monitorProps.getProperty(token + ".name", "");
            monitorInfo.type = monitorProps.getProperty(token + ".type", "");
            monitorInfo.step = monitorProps.getProperty(token + ".step", "");
            monitorInfo.history = monitorProps.getProperty(token + ".history", "");
            monitorInfo.width = monitorProps.getProperty(token + ".width", "");
            monitorInfo.height = monitorProps.getProperty(token + ".height", "");
            monitorInfo.xloc = monitorProps.getProperty(token + ".xloc", "");
            monitorInfo.yloc = monitorProps.getProperty(token + ".yloc", "");
            monitorInfo.iconized = monitorProps.getProperty(token + ".iconized", "");
            monitorInfo.maximized = monitorProps.getProperty(token + ".maximized", "");
            monitorInfo.background = monitorProps.getProperty(token + ".background", "");
            monitorInfo.showGrid = monitorProps.getProperty(token + ".showGrid", "");
            monitorInfo.gridColor = monitorProps.getProperty(token + ".gridColor", "");

            stg = new StringTokenizer(monitorProps.getProperty(token + ".graphs", ""), ";", false);
            v = new Vector();
            while (stg.hasMoreTokens()) {
                String grtoken = stg.nextToken();
                GraphInfo graphInfo = new GraphInfo();
                Vector vgr = new Vector();
                graphInfo.name = monitorProps.getProperty(grtoken + ".name", "");
                graphInfo.functionName = monitorProps.getProperty(grtoken + ".functionName", "");
                graphInfo.systemId = monitorProps.getProperty(grtoken + ".systemId", "");
                graphInfo.color = monitorProps.getProperty(grtoken + ".color", "");
                graphInfo.args = monitorProps.getProperty(grtoken + ".args", "");
                graphInfo.state = monitorProps.getProperty(grtoken + ".state", "");
                v.addElement(graphInfo);
            }
            monitorInfo.graphs = v;
            Monitor monitor = addMonitorPrivate(monitorInfo, monitorProps.getProperty(token + ".showing", "").equals("true"));
        }
        if (monitors.size() == 0)
            ManageTool.getShowMonitors().setEnabled(false);
        String activeMonitorName = monitorProps.getProperty("active_monitor", "");
        if (!activeMonitorName.equals("")) setMonitorActive(activeMonitorName);
        dateFormat = ManageTool.getDateFormat();
    }

    public static long getNow() {
        return (long) (getMillis() / 1000);
    }

    public static long getMillis() {
        return (beginTime == 0) ? 0 : (System.currentTimeMillis() - beginTime);
    }

    public static long getMillis(long time) {
        return (beginTime == 0) ? 0 : (time - beginTime);
    }

    public static long getNowInSec(long time) {
        return (beginTime == 0) ? 0 : (long) (time - (long) beginTime / 1000);
    }

    public javax.swing.Timer getTimer() {
        if (timer == null) {
            timer = new javax.swing.Timer(savingDelay, new ActionListener() {
                public void actionPerformed(ActionEvent ace) {
                    saveGraphs();
                }
            });
            ManageTool.graphSaver.saveTime((long) beginTime / 1000);
        }
        return timer;
    }

    public void startTimer() {
        if (this.beginTime == 0) this.beginTime = System.currentTimeMillis();
        getTimer().start();
        for (Enumeration en = monitors.elements(); en.hasMoreElements();) {
            ((Monitor) en.nextElement()).startThread();
        }
    }

    public void stopTimer() {
        getTimer().stop();
        for (Enumeration en = monitors.elements(); en.hasMoreElements();) {
            ((Monitor) en.nextElement()).stopThread();
        }
    }

    public void sizeChanged() {
        getParent().repaint();
    }

    public Vector getMonitors() {
        return monitors;
    }

    public Monitor addMonitor(MonitorInfo monitorInfo) {
        Monitor monitor = addMonitorPrivate(monitorInfo);
        monitor.startThread();
        return monitor;
    }

    public void addMonitor() {
        MonitorInfo monitorInfo = ManageTool.showAddMonitorDialog(null);
        if (monitorInfo == null) return;
        addMonitor(monitorInfo);
    }

    public synchronized Monitor addHistoryMonitor(MonitorInfo monitorInfo) {
        Monitor m = addMonitorPrivate(monitorInfo, true, true);
        ManageTool.graphSaver.restoreGraphs(m);
        return m;
    }

    public Monitor removeMonitor(Monitor monitor) {
        monitor.stopThread();
        monitors.removeElement(monitor);
        if (monitor.isIcon()) {
            try {
                monitor.setIcon(false);
            } catch (PropertyVetoException exc) {
            }
        }
        remove(monitor);
        repaint();
        if (monitors.size() == 0)
            ManageTool.getShowMonitors().setEnabled(false);
        return monitor;
    }

    public synchronized void editMonitor(Monitor monitor) {
        MonitorInfo monitorInfo = ManageTool.showAddMonitorDialog(monitor.getMonitorInfo(), monitor.isHistoryMonitor);
        if (monitorInfo == null) return;
        monitor.setMonitorInfo(monitorInfo);
        if (monitor.isHistoryMonitor) {
            ManageTool.graphSaver.restoreGraphs(monitor);
        }
    }

    private Monitor addMonitorPrivate(MonitorInfo monitorInfo) {
        return addMonitorPrivate(monitorInfo, true);
    }

    private Monitor addMonitorPrivate(MonitorInfo monitorInfo, boolean showing) {
        return addMonitorPrivate(monitorInfo, showing, false);
    }

    private Monitor addMonitorPrivate(MonitorInfo monitorInfo, boolean showing, boolean isMonitorHistory) {
        Monitor monitor = new Monitor(this, monitorInfo, isMonitorHistory);
        monitor.showing = showing;
        monitors.addElement(monitor);
        add(monitor);
        monitor.setVisible(showing);
        try {
            monitor.setSelected(false);
        } catch (PropertyVetoException e) {
        }
        monitor.setWidth(monitorInfo.width);
        monitor.setHeight(monitorInfo.height);
        try {
            monitor.setMaximum(monitorInfo.maximized.equals("true"));
        } catch (PropertyVetoException e) {
        } catch (NullPointerException e) {
        }
        try {
            monitor.setIcon(!monitorInfo.iconized.equals("false"));
        } catch (PropertyVetoException e) {
        } catch (NullPointerException e) {
        }
        try {
            int xl = (new Integer(monitorInfo.xloc)).intValue();
            int yl = (new Integer(monitorInfo.yloc)).intValue();
            monitor.setLocation(xl, yl);
        } catch (Exception e) {
        }
        ManageTool.getShowMonitors().setEnabled(true);
        return monitor;
    }

    public void saveMonitorProperties(Properties monitorProps) {
        monitorProps.clear();
        String names = "";
        for (int i = 0; i < monitors.size(); i++) {
            Monitor monitor = (Monitor) monitors.elementAt(i);
            if (monitor.isHistoryMonitor)
                continue;
            MonitorInfo mi = monitor.getMonitorInfo();
            if (i > 0) names += ";";
            names += mi.saveProperties(monitorProps, i);
            monitorProps.setProperty("monitor" + i + ".showing", "" + monitor.getShowing());
        }
        monitorProps.setProperty("monitor.names", names);
        if (activeMonitor != null && !activeMonitor.isIcon())
            monitorProps.setProperty("active_monitor", activeMonitor.monitorInfo.name);
    }

    public void showMonitor(Monitor monitor) {
        monitor.setVisible(true);
    }

    public void hideMonitor(Monitor monitor) {
        monitor.setVisible(false);
    }

    public void setMonitorActive(String activeMonitorName) {
        int cnt = monitors.size();
        for (int i = 0; i < cnt; i++) {
            Monitor m = (Monitor) monitors.elementAt(i);
            if (m.monitorInfo.name.equals(activeMonitorName)) {
                activeMonitor = m;
                try {
                    m.setSelected(true);
                } catch (PropertyVetoException e) {
                    System.out.println("e = " + e);
                }
            }
        }
    }

    public void monitorDeactivate(Monitor m) {
        if (activeMonitor == m) {
            // there is not other - not icon
            activeMonitor = null;
            basicPanel.fireComponentChanged(ComponentChangedListener.MONITORING, "", "");
        }
    }

    public String getActiveMonitorName() {
        if (activeMonitor == null) return "";
        return (activeMonitor.isIcon()) ? "" : activeMonitor.monitorInfo.name;
    }

    public int getAllGraphCount() {
        int res = 0;
        for (Enumeration en = monitors.elements(); en.hasMoreElements();) {
            Monitor m = (Monitor) en.nextElement();
            if (m.isHistoryMonitor)
                continue;
            res += m.graphs.size();
        }
        return res;
    }

    public synchronized void saveGraphs() {
        long time = (long) System.currentTimeMillis() / 1000 - 1;  // prev second
        ManageTool.graphSaver.saveTime(time);
        ManageTool.graphSaver.saveCount(basicPanel.monitorContainer.getAllGraphCount());
        for (Enumeration en = monitors.elements(); en.hasMoreElements();) {
            Monitor nextm = (Monitor) en.nextElement();
            if (nextm.isHistoryMonitor)
                continue;
            long lastShiftTime = -1;
            for (Enumeration eng = nextm.graphs.elements(); eng.hasMoreElements();) {
                lastShiftTime = ManageTool.graphSaver.saveGraph((Graph) eng.nextElement(), time);
            }
            if (lastShiftTime != -1) nextm.setShiftTime(lastShiftTime);
        }
    }

    public static synchronized int getNewGraphId() {
        return lastId++;
    }

    public void setSavingDelay(int delay) {
        this.savingDelay = delay;
        getTimer().setDelay(delay);
    }

    public int getSavingDelay() {
        return this.savingDelay;
    }
}
