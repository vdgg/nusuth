package com.azoft.nusuth.gui;

public class GraphInfo {
    public String name;
    public String functionName;
    public String systemId;
    public String args;
    public String color;
    public String state = "true";

    public GraphInfo() {
    }

    public GraphInfo(String[] info) {
        if (info != null && info.length == 3) {
            this.functionName = info[0];
            this.name = info[0];
            this.systemId = info[1];
            this.args = (info[2] == null) ? "" : info[2];
            this.color = ((int) (Math.random() * 255)) + ", " + ((int) (Math.random() * 255)) + ", " + ((int) (Math.random() * 255));
        }
    }

    public Object[] toArray() {
        Object[] res = new Object[5];
        res[0] = name;
        res[1] = functionName;
        res[2] = systemId;
        res[3] = color;
        res[4] = args;
        return res;
    }

    public boolean equals(GraphInfo another) {
        if (this.functionName.equals(another.functionName) && this.systemId.equals(another.systemId) && this.args.equals(another.args)) return true;
        return false;
    }

}
