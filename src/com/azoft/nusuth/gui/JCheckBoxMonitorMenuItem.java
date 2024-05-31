package com.azoft.nusuth.gui;

import javax.swing.*;

public class JCheckBoxMonitorMenuItem extends JCheckBoxMenuItem implements MonitorNameChangedListener {

    public JCheckBoxMonitorMenuItem(Monitor monitor) {
        super(monitor.getMonitorInfo().name);
        monitor.addMonitorNameChangedListener(this);
    }

    public void monitorNameChanged(MonitorInfo mi) {
        this.setText(mi.name);
    }
}