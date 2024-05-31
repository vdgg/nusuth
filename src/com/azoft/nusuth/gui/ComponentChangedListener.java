package com.azoft.nusuth.gui;

public interface ComponentChangedListener {
    public static final int EDITING = 0;
    public static final int MONITORING = 1;
    public static final int CLUSTER_VIEW = 2;

    void componentChanged(int tab, String type, String id);
}

