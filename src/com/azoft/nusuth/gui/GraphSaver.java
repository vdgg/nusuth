package com.azoft.nusuth.gui;

import java.io.*;
import java.util.*;

public class GraphSaver {
    File file;
    FileOutputStream fos = null;
    DataOutputStream dos = null;
    boolean notEOF = true;

    public GraphSaver(String dir) {
        file = new File(dir, "graphs");
        try {
            fos = new FileOutputStream(file);
            dos = new DataOutputStream(fos);
        } catch (IOException eo) {
            System.out.println("cannot create FileOutputStream, e = " + eo);
        }
    }

    public long saveGraph(Graph g, long time) {
        writeInt(g.id);
        writeString(g.getFunctionName() + ";" + g.getSystemId() + ";" + g.getArgsString());
        Vector v = g.giveValues(time);
        int size = v.size();
        writeInt(size);
        for (int i = 0; i < size; i++) {
            int value = -1;
            try {
                value = ((Integer) v.elementAt(i)).intValue();
            } catch (Exception e) {
                value = -1;
            }
            writeInt(value);
        }
        flush();
        return g.shiftTime;
    }

    public void saveTime(long time) {
        writeLong(time);
    }

    public void saveCount(int cnt) {
        writeInt(cnt);
    }

    private void writeInt(int value) {
        try {
            dos.writeInt(value);
        } catch (IOException e) {
            System.out.println("cannot write int value, e = " + e);
        } catch (NullPointerException ne) {
            System.out.println("dos == null");
        }
    }

    private void writeLong(long value) {
        try {
            dos.writeLong(value);
        } catch (IOException e) {
            System.out.println("cannot write long value, e = " + e);
        } catch (NullPointerException ne) {
            System.out.println("dos == null");
        }
    }

    private void writeString(String s) {
        try {
            dos.writeUTF(s);
        } catch (IOException e) {
            System.out.println("cannot write string, e = " + e);
        } catch (NullPointerException ne) {
            System.out.println("dos == null");
        }
    }

    private void flush() {
        try {
            dos.flush();
        } catch (IOException e) {
            System.out.println("cannot flush, e = " + e);
        } catch (NullPointerException ne) {
            System.out.println("dos == null");
        }
    }

    private int readInt(DataInputStream dis) {
        try {
            return dis.readInt();
        } catch (IOException e) {
            if (e instanceof EOFException)
                notEOF = false;
            else
                System.out.println("cannot read int value, e = " + e);
        } catch (NullPointerException ne) {
            System.out.println("dis == null");
        }
        return -1;
    }

    private long readLong(DataInputStream dis) {
        try {
            return dis.readLong();
        } catch (IOException e) {
            if (e instanceof EOFException)
                notEOF = false;
            else
                System.out.println("cannot read int value, e = " + e);
        } catch (NullPointerException ne) {
            System.out.println("dis == null");
        }
        return -1;
    }

    private String readString(DataInputStream dis) {
        try {
            return dis.readUTF();
        } catch (IOException e) {
            if (e instanceof EOFException)
                notEOF = false;
            else
                System.out.println("cannot read int value, e = " + e);
        } catch (NullPointerException ne) {
            System.out.println("dis == null");
        }
        return "";
    }

    private void skipBytes(DataInputStream dis, int n) {
        try {
            dis.skipBytes(n);
        } catch (IOException e) {
            if (e instanceof EOFException)
                notEOF = false;
            else
                System.out.println("cannot read int value, e = " + e);
        } catch (NullPointerException ne) {
            System.out.println("dis == null");
        }
    }

    private String getTime(long time) {
        return PeriodPanel.formatLong(time);
    }

    public void restoreGraphs(Monitor m) {
        for (Enumeration en = m.graphs.elements(); en.hasMoreElements();) {
            ((Graph) en.nextElement()).clearValues();
        }
        m.clearMaxMin();
        long from = (long) m.monitorInfo.beginTime / 1000;
        long to = (long) m.monitorInfo.endTime / 1000;
        FileInputStream fis = null;
        DataInputStream dis = null;
        try {
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
        } catch (IOException ei) {
            ManageTool.showMessage("cannot create File(Data)InputStream");
            return;
        }
        notEOF = true;
        boolean beforeTo = true;
        Hashtable graphs = new Hashtable();
        long beginTime = readLong(dis);
        if (from > to || to < beginTime) {
            ManageTool.showMessage("   Wrong interval !!!    ");
            m.setHistory(1);
            m.setShiftTime(0);
            closeIS(fis, dis);
            return;
        }
        if (from < beginTime) from = beginTime;
        long prevTime = beginTime - 1;
        Vector vGraphs = new Vector();
        for (Enumeration en = m.graphs.elements(); en.hasMoreElements();) {
            vGraphs.addElement((Graph) en.nextElement());
        }
//			(Vector)m.graphs.clone();
        while (notEOF && beforeTo) {
            long time = readLong(dis);
            int count = readInt(dis);
            int skipCnt = (int) Math.max(0, (from - prevTime));
            int npcnt = (int) (time - prevTime - skipCnt);  // normal point cnt
//			npcnt += (prevTime == beginTime) ? 1 : 0;
            for (int i = 0; i < count; i++) {
                int gid = readInt(dis);
                String info = readString(dis);
                int size = readInt(dis);
                if (from <= time) {
                    Graph g = (Graph) graphs.get("" + gid);
                    if (g == null && vGraphs.size() > 0) {
                        String[] sta = new String[3];
                        int cnt = 0;
                        StringTokenizer st = new StringTokenizer(info, ";");
                        while (st.hasMoreTokens()) {
                            sta[cnt++] = st.nextToken();
                        }
                        GraphInfo gi = new GraphInfo(sta);
                        for (Enumeration en = vGraphs.elements(); en.hasMoreElements();) {
                            Graph gnext = (Graph) en.nextElement();
                            if (gnext.getGraphInfo().equals(gi)) {
                                g = gnext;
                                graphs.put("" + gid, g);
                                vGraphs.remove(gnext);
                                break;
                            }
                        }
                    }
                    if (g != null) {
                        int tcnt = npcnt - size;
                        long curTime = prevTime + 1;
                        while (tcnt > 0) {
                            g.values.addElement(new Integer(-1));
                            curTime += 1;
                            tcnt--;
                        }
                        for (int j = 0; j < size; j++, curTime += 1) {
                            int nextValue = readInt(dis);
                            if (tcnt++ >= 0) {
                                if (curTime <= to) {
                                    g.values.addElement(new Integer(nextValue));
                                    if (nextValue != -1) g.fireValueAdded(nextValue);
                                } else
                                    beforeTo = false;
                            }
                        }
                    } else {
                        skipBytes(dis, size * 4);
                        if (prevTime + size > to)
                            beforeTo = false;
                    }
                } else
                    skipBytes(dis, size * 4);
            }
            if (time != -1) prevTime = time;
        }
        m.setHistory(Math.max(1, (int) (Math.min(to, prevTime) - from)));
        m.setShiftTime(from - beginTime);
        closeIS(fis, dis);
    }

    public void restoreRealGraphs(Monitor m, long from, long to) {
        // from & to - seconds, beginning with begintime !!!
        FileInputStream fis = null;
        DataInputStream dis = null;
        try {
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
        } catch (IOException ei) {
            ManageTool.showMessage("cannot create File(Data)InputStream");
            return;
        }
        for (Enumeration en = m.graphs.elements(); en.hasMoreElements();) {
            ((Graph) en.nextElement()).prepareInsert();
        }
        notEOF = true;
        boolean beforeTo = true;
        long beginTime = readLong(dis);  // sec
        from += beginTime;
        to += beginTime;
        if (from > to || to < beginTime) {
            closeIS(fis, dis);
            return;
        }
        if (from < beginTime) from = beginTime;
        long prevTime = beginTime - 1;
        while (notEOF && beforeTo) {
            long time = readLong(dis); // sec
            int count = readInt(dis);
            int skipCnt = (int) Math.max(0, (from - prevTime - 1));
            int npcnt = (int) (time - prevTime - skipCnt);  // normal point cnt
            Hashtable idshashcl = (Hashtable) m.graphs.clone();
            // restore saved graphs
            for (int i = 0; i < count; i++) {
                int gid = readInt(dis);
                String info = readString(dis);
                int size = readInt(dis);
                if (from <= time) {
                    Graph g = m.getGraph("" + gid);
                    if (g != null) {
                        idshashcl.remove("" + g.id);
                        int tcnt = npcnt - size;
                        long curTime = prevTime + 1;
                        while (tcnt > 0) {
                            g.insertValue(-1);
                            curTime += 1;
                            tcnt--;
                        }
                        for (int j = 0; j < size; j++, curTime += 1) {
                            int nextValue = readInt(dis);
                            if (tcnt++ >= 0) {
                                if (curTime < to) {
                                    g.insertValue(nextValue);
                                } else
                                    beforeTo = false;
                            }
                        }
                    } else {
                        skipBytes(dis, size * 4);
                        if (prevTime + size >= to)
                            beforeTo = false;
                    }
                } else
                    skipBytes(dis, size * 4);
            }
            // work with not existing in that time graphs in current monitor
            for (Enumeration neids = idshashcl.keys(); neids.hasMoreElements();) {
                String nextId = (String) neids.nextElement();
                Graph g = (Graph) m.graphs.get(nextId);
                if (from <= time) {
                    long curTime = prevTime + 1;
                    int newcnt = npcnt;
                    while (newcnt-- > 0) {
                        if (curTime++ < to) {
                            g.insertValue(-1);
                        } else
                            beforeTo = false;
                    }
                }
            }
            if (time != -1) prevTime = time;
        }
        m.setShiftTime(from - beginTime);
        closeIS(fis, dis);
    }
//	private void printHash(Hashtable hash){
//		for (Enumeration en = hash.keys(); en.hasMoreElements(); ){
//			System.out.print(en.nextElement()+", ");
//		}
//		System.out.println();
//	}
    private void closeIS(FileInputStream fis, DataInputStream dis) {
        try {
            dis.close();
            fis.close();
        } catch (IOException e) {
            System.out.println("cannot close dis & fis");
        }
    }

    public void closeStreams() {
        try {
            fos.close();
            dos.close();
        } catch (IOException e) {
            System.out.println("cannot close dos & fos");
        } catch (NullPointerException ne) {
            System.out.println("dos & fos = null");
        }
    }
}

