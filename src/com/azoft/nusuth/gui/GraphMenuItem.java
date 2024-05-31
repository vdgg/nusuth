package com.azoft.nusuth.gui;

import javax.swing.*;

public class GraphMenuItem extends JCheckBoxMenuItem {
    Graph g;

    public GraphMenuItem(Graph g) {
        super("Show " + g.getName());
        this.g = g;
    }

    public Graph getGraph() {
        return this.g;
    }

    public void nameChanged() {
        this.setText("Show " + g.getName());
    }
}
