package com.azoft.nusuth.gui;

import com.azoft.nusuth.management.ServerState;

import java.util.Vector;

public class MonitorRequestThreadEvent extends RequestThreadEvent {
    boolean repaint;

    public MonitorRequestThreadEvent(Object source, boolean success, boolean hasUnauthorizedAccessException, Exception exception, boolean repaint) {
        super(source, success, hasUnauthorizedAccessException, exception);
        this.repaint = repaint;
    }

    public boolean isRepaint() {
        return this.repaint;
    }
}