package com.azoft.nusuth.gui;

import java.awt.*;
import java.util.*;

public class Graph {
    int id;
    private boolean state = true;
    private Color color = Color.black;
    private String name = "undefined";
    private String systemId;
    private Object[] args;
    private GraphInfo graphInfo;
    Vector values = new Vector();
    private int lastValue = -1;
    long shiftTime;
    int history = 10;
    int sinvcnt = 0; // count of the points int values, which are saving already (in file)
    private int insertIndex = 0;
    Polygon poly = new Polygon();

    private Vector listeners = new Vector();

    private GraphMenuItem menuItem;

    public Graph(String name, String functionName, String systemId, Object[] args, Color color, long shiftTime) {
        this.id = MonitorContainer.getNewGraphId();
        this.name = name;
        this.systemId = systemId;
        this.args = args;
        this.color = color;
        this.state = true;
        this.shiftTime = shiftTime;
    }

    public Graph(GraphInfo graphInfo, long shiftTime) {
        this.id = MonitorContainer.getNewGraphId();
        this.shiftTime = shiftTime;
        setGraphInfo(graphInfo);
    }

    public GraphInfo getGraphInfo() {
        return graphInfo;
    }

    public void setGraphInfo(GraphInfo graphInfo) {
        this.graphInfo = graphInfo;
        this.name = graphInfo.name;
        getGraphMenuItem().nameChanged();
        this.systemId = graphInfo.systemId;
        this.color = getColor(graphInfo.color);
        this.state = !graphInfo.state.equals("false");
        String forToken = graphInfo.args;
        if (forToken.trim().equals(GraphsTable.ALLAPPS)) forToken = "";
        StringTokenizer stArgs = new StringTokenizer(forToken, ";", false);
        Object[] newargs = new Object[stArgs.countTokens()];
        int cnt = 0;
        while (stArgs.hasMoreTokens()) {
            newargs[cnt++] = stArgs.nextToken();
        }
        this.args = newargs;
    }

    public String getFunctionName() {
        return this.graphInfo.functionName;
    }

    public void setFunctionName(String functionName) {
        this.graphInfo.functionName = functionName;
    }

    public void setColor(Color color) {
        this.color = color;
        this.graphInfo.color = getColorString(color);
    }

    public Color getColor() {
        return this.color;
    }

    public void setName(String name) {
        this.name = name;
        this.graphInfo.name = name;
        getGraphMenuItem().nameChanged();
    }

    public String getName() {
        return this.name;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
        this.graphInfo.systemId = systemId;
    }

    public String getSystemId() {
        return this.systemId;
    }

    public void setArgs(Object[] args) {
        this.args = args;
        this.graphInfo.args = "";
        for (int i = 0; i < args.length; i++) {
            this.graphInfo.args = this.graphInfo.args + args[i];
        }
    }

    public String getArgsString() {
        return this.graphInfo.args;
    }

    public Object[] getArgs() {
        return this.args;
    }

    public void setHistory(int history) {
        this.history = history;
    }

    public GraphMenuItem getGraphMenuItem() {
        if (menuItem == null) menuItem = new GraphMenuItem(this);
        return menuItem;
    }

    public synchronized void addRealValue(int value, long endTime) {
        int index = (int) (endTime - shiftTime);
        while (index > values.size()) {
            values.addElement(new Integer(-1));
        }
        if (index < values.size())
            values.setElementAt(new Integer(value), index);
        else
            values.addElement(new Integer(value));
        if (value != -1) {
            lastValue = value;
            fireValueAdded(value);
        }
    }

    public void setEnabled(boolean b) {
        this.state = b;
        this.graphInfo.state = "" + b;
    }

    public boolean isEnabled() {
        return this.state;
    }

    public int getValueAt(int index) {
        try {
            return ((Integer) this.values.elementAt(index)).intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    public void clearValues() {
        values.removeAllElements();
    }

    public synchronized Vector giveValues(long time) {
        Vector res = (Vector) values.clone();
        long nowInT = MonitorContainer.getNowInSec(time);
        int sindex = (int) (nowInT - shiftTime);   // last saving index
        int rindex = values.size() - 1;  // real last index
        while (rindex < sindex) {
            // if point not arrived yet
            values.addElement(new Integer(-1));
            rindex = values.size() - 1;
        }
        int realSize = sindex + 1 - sinvcnt;
        for (int i = 0; i < sinvcnt; i++) {
            res.remove(0);
        }
        res.setSize(realSize);
        sinvcnt = (history >= rindex + 1) ? sindex + 1 : history - (rindex - sindex);
        int delSize = Math.max(0, rindex + 1 - history);
        shiftTime += delSize;
        for (int i = 0; i < delSize; i++) {
            values.remove(0);
        }
        return res;
    }

    public String getCaption() {
        String lastv = (lastValue == -1) ? "" : " ( " + lastValue + " )";
        return getName() + lastv;
    }

    public void prepareInsert() {
        insertIndex = 0;
    }

    public void insertValue(int value) {
        values.insertElementAt(new Integer(value), insertIndex);
        if (value != -1) fireValueAdded(value);
        insertIndex++;
        sinvcnt++;
    }

    public Polygon getPolygon() {
//		System.out.println("return prev polygon from graph = ");
//		System.out.println("xs = "); printArray(poly.xpoints);
//		System.out.println("ys = "); printArray(poly.ypoints);
//		System.out.println("np = "+poly.npoints);
        return poly;
    }

    public void setPolygon(int[] xs, int[] ys, int ct) {
//		poly.xpoints = new int[ct];
//		poly.ypoints = new int[ct];
//		System.arraycopy(xs, 0, poly.xpoints, 0, ct);
//		System.arraycopy(ys, 0, poly.ypoints, 0, ct);
        poly.xpoints = xs;
        poly.ypoints = ys;
        poly.npoints = ct;
//		System.out.println("new polygon in graph = ");
//		System.out.println("xs = "); printArray(poly.xpoints);
//		System.out.println("ys = "); printArray(poly.ypoints);
//		System.out.println("np = "+poly.npoints);
    }

    private void printArray(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + ", ");
        }
        System.out.println();
    }

    public void addListener(GraphListener l) {
        listeners.addElement(l);
    }

    public void removeListener(GraphListener l) {
        listeners.removeElement(l);
    }

    public void fireValueAdded(int value) {
        for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
            ((GraphListener) e.nextElement()).valueAdded(value, this.isEnabled());
        }
    }

    public static Color getColor(String s) {
        if (s == null) return Color.white;
        StringTokenizer st = new StringTokenizer(s, ",");
        int[] rgb = new int[3];
        int cnt = 0;
        while (st.hasMoreElements()) {
            String token = st.nextToken().trim();
            try {
                rgb[cnt] = (new Integer(token)).intValue();
            } catch (Exception e) {
                return Color.white;
            }
            if (rgb[cnt] < 0 || rgb[cnt++] > 255) return Color.white;
        }
        if (cnt != 3) return Color.white;
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    public static String getColorString(Color c) {
        return c.getRed() + ", " + c.getGreen() + ", " + c.getBlue();
    }
}
