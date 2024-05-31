package com.azoft.nusuth.gui;

import java.util.*;
import java.awt.*;

public class MonitorInfo {
    public String name;
    public String type;
    public String step;
    public String history;
    public String width;
    public String height;
    public String xloc;
    public String yloc;
    public String iconized;
    public String maximized;
    public Vector graphs;
    public String background;
    public String showGrid;
    public String gridColor;
    public long beginTime; // begin millis in period
    public long endTime; // end millis in period

    public MonitorInfo() {
        this.width = "550";
        this.height = "250";
        this.xloc = "0";
        this.yloc = "0";
        this.iconized = "false";
        this.maximized = "false";
        this.background = "";
        this.showGrid = "true";
        this.gridColor = "";
    }

    public MonitorInfo(String name, String type, String step, String history, String width, String height, String xloc, String yloc, String iconized, String maximized, Vector graphs, String background, String showGrid, String gridColor) {
        this.name = name;
        this.type = type;
        this.step = step;
        this.history = history;
        this.width = width;
        this.height = height;
        this.xloc = xloc;
        this.yloc = yloc;
        this.iconized = iconized;
        this.maximized = maximized;
        this.graphs = graphs;
        this.background = background;
        this.showGrid = showGrid;
        this.gridColor = gridColor;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public void setXloc(String xloc) {
        this.xloc = xloc;
    }

    public void setYloc(String yloc) {
        this.yloc = yloc;
    }

    public void setIconized(String iconized) {
        this.iconized = iconized;
    }

    public void setMaximized(String maximized) {
        this.maximized = maximized;
    }

    public void setGraphs(Vector graphs) {
        this.graphs = graphs;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public void setShowGrid(String showGrid) {
        this.showGrid = showGrid;
    }

    public void setGridColor(String gridColor) {
        this.gridColor = gridColor;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String saveProperties(Properties monitorProps, int index) {
        String token = "monitor" + index;
        monitorProps.setProperty(token + ".name", this.name);
        monitorProps.setProperty(token + ".type", this.type);
        monitorProps.setProperty(token + ".step", this.step);
        monitorProps.setProperty(token + ".history", this.history);
        monitorProps.setProperty(token + ".width", this.width);
        monitorProps.setProperty(token + ".height", this.height);
        monitorProps.setProperty(token + ".xloc", this.xloc);
        monitorProps.setProperty(token + ".yloc", this.yloc);
        monitorProps.setProperty(token + ".iconized", this.iconized);
        monitorProps.setProperty(token + ".maximized", this.maximized);
        monitorProps.setProperty(token + ".background", this.background);
        monitorProps.setProperty(token + ".showGrid", this.showGrid);
        monitorProps.setProperty(token + ".gridColor", this.gridColor);
        String graphsName = "";
        String grtoken;
        for (int i = 0; i < this.graphs.size(); i++) {
            GraphInfo graphInfo = (GraphInfo) this.graphs.elementAt(i);
            grtoken = token + "_graph" + i;
            monitorProps.setProperty(grtoken + ".name", graphInfo.name);
            monitorProps.setProperty(grtoken + ".functionName", graphInfo.functionName);
            monitorProps.setProperty(grtoken + ".systemId", graphInfo.systemId);
            monitorProps.setProperty(grtoken + ".color", graphInfo.color);
            monitorProps.setProperty(grtoken + ".args", graphInfo.args);
            monitorProps.setProperty(grtoken + ".state", graphInfo.state);
            if (i > 0) graphsName += ";";
            graphsName += grtoken;
        }
        monitorProps.setProperty(token + ".graphs", graphsName);
        return token;
    }

    public String toString() {
        return this.name;
    }
}
