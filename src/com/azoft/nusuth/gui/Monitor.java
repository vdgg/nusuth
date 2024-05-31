package com.azoft.nusuth.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.util.*;
import java.beans.PropertyVetoException;
import java.lang.reflect.Method;

import com.azoft.nusuth.management.ComponentType;
import com.azoft.nusuth.management.ServerState;
import com.azoft.nusuth.management.ContainerState;

public class Monitor extends JInternalFrame implements GraphListener {
    public static final Color DEFAULT_GRID = new Color(35, 122, 72);
    private static int STEP_COUNT = 10;
    private static int SPACE = 15;
    private static int grex = 5;
    private static int maxXgrex = 5;

    protected Container container;
    private MonitorContainer monitorContainer;
    MonitorInfo monitorInfo;
    boolean isHistoryMonitor = false;
    private MonitorRequestThread monitorRequestThread;
    Hashtable graphsBySystemId = new Hashtable();  // systemId -> vector of graphs

    private Controller refreshController;
    private Controller historyController;
    private JMenu graphsMenu;
    protected MonitorPanel monitorPanel;
    private GraphicsNamePanel graphicsNamePanel;
    private JCheckBoxMenuItem showGridItem;
    private JPanel control;
    private JCheckBoxMonitorMenuItem menuItem;

    private int step;
    private int history;
//    Vector graphs = new Vector();
    Hashtable graphs = new Hashtable();
    private Vector listeners = new Vector();

    public boolean stopped = false;
    public boolean showing = true;
    private long shiftTime;

    private Dimension BUTTON_SIZE = new Dimension(100, 21);
    private boolean wasMaximum = false;
    public int enabledCount = 0;
    private long prevTime = 0;

    public Monitor(final MonitorContainer monitorContainer, final MonitorInfo monitorInfo) {
        this(monitorContainer, monitorInfo, false);
    }

    public Monitor(final MonitorContainer monitorContainer, final MonitorInfo monitorInfo, boolean isHistoryMonitor) {
        super(monitorInfo.name,
                true, // resizable
                true, // closable
                true, // maximizable
                true);   // iconifiable

        this.monitorContainer = monitorContainer;
        this.monitorInfo = monitorInfo;
        this.isHistoryMonitor = isHistoryMonitor;

        this.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameActivated(InternalFrameEvent e) {
                monitorContainer.activeMonitor = Monitor.this;
                monitorContainer.basicPanel.fireComponentChanged(ComponentChangedListener.MONITORING, "", monitorInfo.name);
            }

            public void internalFrameDeactivated(InternalFrameEvent e) {
                monitorContainer.monitorDeactivate(Monitor.this);
            }
        });

        createComponents();
        initShiftTime();

        for (int i = 0; i < monitorInfo.graphs.size(); i++) {
            GraphInfo graphInfo = (GraphInfo) monitorInfo.graphs.elementAt(i);
            addGraph(graphInfo);
        }
        setStep(monitorInfo.step);
        setHistory(monitorInfo.history, false);
//        setHistory(monitorInfo.history);
        setMonitorPanelBackground(Graph.getColor(monitorInfo.background));
        showGrid(monitorInfo.showGrid);
        setGridColor(Graph.getColor(monitorInfo.gridColor));
        setClosable(false);
        addMonitorNameChangedListener(new MonitorNameChangedListener() {
            public void monitorNameChanged(MonitorInfo mi) {
                setTitle(mi.name);
            }
        });
        initMonitorRequestThread();
    }

    protected void createComponents() {
        container = getContentPane();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 1.0;
        c.weighty = 1.0;
        monitorPanel = new MonitorPanel();
        container.add(monitorPanel, c);

        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = GridBagConstraints.RELATIVE;
        graphicsNamePanel = new GraphicsNamePanel();
        container.add(graphicsNamePanel, c);

        c.insets = new Insets(5, 0, 5, 5);
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        control = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        control.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                setNewMinimum((JPanel) e.getComponent());
                ((JPanel) e.getComponent()).getParent().doLayout();
            }
        });
        if (!isHistoryMonitor) {
            container.add(control, c);
            control.add(getRefreshController());
            control.add(getHistoryController());
        }

        setBackground(control.getBackground());
        setJMenuBar(createMenuBar());
    }

    protected void initShiftTime() {
        shiftTime = (isHistoryMonitor) ? 0 : MonitorContainer.getNow();
    }

    protected void initMonitorRequestThread() {
        if (!isHistoryMonitor) {
            monitorRequestThread = new MonitorRequestThread(this);
            monitorRequestThread.setRequestThreadListener(new RequestThreadListener() {
                public void workFinished(RequestThreadEvent e) {
                    if (!e.isSuccess()) {
                        if (e.isUnauthorized()) {
                            int res = ManageTool.showLoginDialog();
                            if (res == JOptionPane.NO_OPTION || res == JOptionPane.CLOSED_OPTION) {
                                BasicPanel.unauthorized = true;
                                return;
                            }
                        } else {
//							ManageTool.showMessage("Cannot retrieve state");
                            Monitor.this.monitorContainer.basicPanel.status.setStatusString("Cannot retrieve states");
                            System.out.println(e.getException());
                        }
                    } else {
                        MonitorRequestThreadEvent mrte = (MonitorRequestThreadEvent) e;
                        if (mrte.isRepaint()) {
                            Monitor.this.monitorPanel.paintMonitor();
                            Monitor.this.graphicsNamePanel.repaint();
                        }
                    }
                }
            });
        }
    }


    public void setMaximum(boolean b) throws PropertyVetoException {
        this.setSelected(true);
        super.setMaximum(b);
    }

    private JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.add(getGraphsMenu());
        JMenu optionsMenu = bar.add(new JMenu("Options"));
        JMenuItem editItem = optionsMenu.add(new JMenuItem("Edit monitor"));
        editItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Monitor.this.monitorContainer.editMonitor(Monitor.this);
            }
        });
        JCheckBoxMenuItem startWithItem = new JCheckBoxMenuItem("Start with minimum value", false);
        optionsMenu.add(startWithItem);
        startWithItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Monitor.this.monitorPanel.setStartingWithNull(!((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });
        optionsMenu.add(getShowGridItem());
        return bar;
    }

    private JCheckBoxMenuItem getShowGridItem() {
        if (showGridItem == null) {
            showGridItem = new JCheckBoxMenuItem("Show grid lines", true);
            showGridItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Monitor.this.monitorPanel.showGrid(((JCheckBoxMenuItem) e.getSource()).isSelected());
                }
            });
        }
        return showGridItem;
    }

    private void showGrid(String showGrid) {
        boolean show = showGrid.equals("true");
        getShowGridItem().setSelected(show);
        this.monitorPanel.showGrid(show);
    }

    private void setGridColor(Color c) {
        this.monitorPanel.setGridColor(c);
    }

    protected Controller getRefreshController() {
        if (refreshController == null) {
            refreshController = new Controller("Refresh", new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int newstep = Monitor.this.refreshController.getSeconds();
                    if (newstep != -1) {
                        Monitor.this.step = newstep;
                        Monitor.this.monitorInfo.step = "" + newstep;
                        Monitor.this.monitorPanel.setStepX(newstep);
                        Monitor.this.repaint();
                    }
                }
            });
            refreshController.setSeconds(1);
            monitorPanel.setStepX(1);
        }
        return refreshController;
    }

    protected Controller getHistoryController() {
        if (historyController == null) {
            historyController = new Controller(" History ", new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int length = Monitor.this.historyController.getSeconds();
                    if (length != -1) {
                        Monitor.this.monitorPanel.setLenX(length);
                        Monitor.this.repaint();
                        Monitor.this.history = length;
                        Monitor.this.monitorInfo.history = "" + length;
                        for (Enumeration en = graphs.elements(); en.hasMoreElements();) {
                            ((Graph) en.nextElement()).setHistory(length);
                        }
                        Monitor.this.checkHistory(length);
                    }
                }
            });
            historyController.setSeconds(50);
            monitorPanel.setLenX(50);
        }
        return historyController;
    }

    private void checkHistory(int history) {
        if (history > MonitorContainer.getNow() - shiftTime) {
            stopThread();
            ManageTool.graphSaver.restoreRealGraphs(this, MonitorContainer.getNow() - history, shiftTime);
            startThread();
        }
    }

    public void setIcon(boolean b) throws PropertyVetoException {
        if (b)   // iconyfied
            wasMaximum = isMaximum();  // super.setIcon set maximum to false
        else {
            if (wasMaximum) setMaximum(true);
        }
        super.setIcon(b);
    }

    private void checkGraphs() {
        Vector v = (Vector) monitorInfo.graphs.clone();
        GraphInfo oldgraphInfo, newgraphInfo = new GraphInfo();
//        for (int i = 0; i < graphs.size(); i++){
        for (Enumeration en = graphs.elements(); en.hasMoreElements();) {
//            Graph graph = (Graph)graphs.elementAt(i);
            Graph graph = (Graph) en.nextElement();
            oldgraphInfo = graph.getGraphInfo();
            System.out.println("next graph = " + graph.toString());
            boolean exist = false;
            for (int j = 0; j < v.size(); j++) {
                newgraphInfo = (GraphInfo) v.elementAt(j);
                if (oldgraphInfo.equals(newgraphInfo)) {
                    exist = true;
                    v.removeElement(newgraphInfo);
                    break;
                }
            }
            if (!exist) {
                removeGraphPrivate(graph);
//                i --;
            } else {
                graph.setGraphInfo(newgraphInfo);
                graph.setEnabled(!oldgraphInfo.state.equals("false"));
            }
        }
        for (int i = 0; i < v.size(); i++) {
            newgraphInfo = (GraphInfo) v.elementAt(i);
            Graph gr = new Graph(newgraphInfo, shiftTime);
            addGraphPrivate(gr);
        }
    }

    public MonitorInfo getMonitorInfo() {
        this.monitorInfo.width = "" + getSize().width;
        this.monitorInfo.height = "" + getSize().height;
        this.monitorInfo.xloc = "" + getLocation().x;
        this.monitorInfo.yloc = "" + getLocation().y;
        this.monitorInfo.iconized = "" + isIcon();
        this.monitorInfo.maximized = (isIcon()) ? "" + wasMaximum : "" + isMaximum();
        this.monitorInfo.showGrid = "" + getShowGridItem().isSelected();
        return this.monitorInfo;
    }

    public void setMonitorInfo(MonitorInfo monitorInfo) {
        this.monitorInfo = monitorInfo;
        setName(monitorInfo.name);
        setStep(monitorInfo.step);
        setHistory(monitorInfo.history);
        setMonitorPanelBackground(Graph.getColor(monitorInfo.background));
        setGridColor(Graph.getColor(monitorInfo.gridColor));
        setWidth(monitorInfo.width);
        setHeight(monitorInfo.height);
        if (!isHistoryMonitor) monitorContainer.saveGraphs();
        checkGraphs();
        graphicsNamePanel.checkGraphicsHeight();
    }


    public void startThread() {
        if (monitorRequestThread != null)
            monitorRequestThread.renew();
    }

    public void stopThread() {
        if (monitorRequestThread != null)
            monitorRequestThread.suspendThread();
    }


    public String getName() {
        return this.monitorInfo.name;
    }

    public void setName(String name) {
        this.monitorInfo.name = name;
        fireMonitorNameChanged();
    }

    public String getType() {
        return this.monitorInfo.type;
    }

    public void setType(String type) {
        this.monitorInfo.type = type;
    }

    public int getStep() {
        return this.step;
    }

    public void setStep(int step) {
        getRefreshController().setSeconds(step);
        this.step = getRefreshController().getSeconds();
        monitorPanel.setStepX(this.step);
        this.monitorInfo.step = "" + this.step;
    }

    private void setStep(String sstep) {
        try {
            int step = Integer.parseInt(sstep);
            if (step != 0) setStep(step);
        } catch (Exception e) {
        }
    }

    public int getHistory() {
        return this.history;
    }

    public void setHistory(int history) {
        setHistory(history, true);
    }

    public void setHistory(int history, boolean withCheck) {
        if (!isHistoryMonitor) {
            getHistoryController().setSeconds(history);
            this.history = getHistoryController().getSeconds(); // round value
        } else
            this.history = history;
        monitorPanel.setLenX(this.history);
        this.monitorInfo.history = "" + this.history;
        for (Enumeration en = graphs.elements(); en.hasMoreElements();) {
            ((Graph) en.nextElement()).setHistory(this.history);
        }
        if (withCheck) checkHistory(this.history);
    }

    private void setHistory(String shistory) {
        setHistory(shistory, true);
    }

    private void setHistory(String shistory, boolean withCheck) {
        try {
            int history = Integer.parseInt(shistory);
            if (history != 0) setHistory(history, withCheck);
        } catch (Exception e) {
        }
    }

    public void setWidth(int width) {
        if (width == 0) width = 100;
        Dimension d = getSize();
        setSize(width, d.height);
    }

    public void setWidth(String swidth) {
        if (swidth != null) {
            try {
                setWidth(Integer.parseInt(swidth));
            } catch (Exception e) {
            }
        }
    }

    public void setHeight(int height) {
        if (height == 0) height = 100;
        Dimension d = getSize();
        setSize(d.width, height);
    }

    public void setHeight(String sheight) {
        if (sheight != null) {
            try {
                setHeight(Integer.parseInt(sheight));
            } catch (Exception e) {
            }
        }
    }

    public void setMonitorPanelBackground(Color c) {
        try {
            monitorPanel.setBackColor(c);
        } catch (Exception e) {
        }
    }

    private JMenu getGraphsMenu() {
        if (graphsMenu == null) {
            graphsMenu = new JMenu("Show graphics");
            addItemsToMenu();
        }
        return graphsMenu;
    }


    public void addGraph(Graph gr) {
        addGraphPrivate(gr);
        monitorInfo.graphs.addElement(gr.getGraphInfo());
    }

    private void addGraph(GraphInfo graphInfo) {
        addGraphPrivate(new Graph(graphInfo, shiftTime));
    }

    private void addGraphPrivate(Graph gr) {
//        graphs.addElement(gr);
        graphs.put("" + gr.id, gr);
        addGraphBySystemId(gr);
        gr.addListener(this);
        monitorPanel.checkNullY();
        graphicsNamePanel.checkGraphicsHeight();
        addGraphItemToMenu(gr);
        gr.setHistory(this.history);
    }

    private void addGraphBySystemId(Graph gr) {
        Vector v = (Vector) graphsBySystemId.get(gr.getSystemId());
        if (v == null) {
            v = new Vector();
            graphsBySystemId.put(gr.getSystemId(), v);
        }
        v.addElement(gr);
    }

    private void removeGraphBySystemId(Graph gr) {
        Vector v = (Vector) graphsBySystemId.get(gr.getSystemId());
        if (v != null) {
            v.removeElement(gr);
            if (v.size() == 0) graphsBySystemId.remove(gr.getSystemId());
        }
    }

    public void removeGraph(Graph gr) {
        removeGraphPrivate(gr);
        repaint();
        monitorInfo.graphs.removeElement(gr.getGraphInfo());
    }

    private void removeGraphPrivate(Graph gr) {
        gr.removeListener(this);
//        graphs.removeElement(gr);
        graphs.remove("" + gr.id);
        removeGraphBySystemId(gr);
        monitorPanel.checkNullY();
        graphicsNamePanel.checkGraphicsHeight();
        GraphMenuItem gmi = gr.getGraphMenuItem();
        getGraphsMenu().remove(gmi);
        if (gr.isEnabled()) enabledCount--;
    }

    public void valueAdded(int value, boolean isEnabled) {
        monitorPanel.valueAdded(value, isEnabled);
    }

    public void addItemsToMenu() {
        for (Enumeration e = graphs.elements(); e.hasMoreElements();) {
            final Graph gr = (Graph) e.nextElement();
            addGraphItemToMenu(gr);
        }
    }

    private void addGraphItemToMenu(final Graph gr) {
        GraphMenuItem gmi = (GraphMenuItem) getGraphsMenu().add(gr.getGraphMenuItem());
        if (gr.isEnabled()) {
            gmi.setSelected(true);
            enabledCount++;
            graphicsNamePanel.checkGraphicsHeight();
        } else
            gmi.setSelected(false);
        gmi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMenuItem chb = (JCheckBoxMenuItem) ae.getSource();
                boolean selected = chb.isSelected();
                gr.setEnabled(selected);
                if (selected)
                    enabledCount++;
                else
                    enabledCount--;
                Monitor.this.monitorPanel.checkBeginIndex(true);
                Monitor.this.graphicsNamePanel.checkGraphicsHeight();
            }
        });
    }

    public Enumeration getGraphs() {
        return graphs.elements();
    }

    public boolean hasEnabledGraph() {
        boolean res = false;
        for (Enumeration en = graphs.elements(); en.hasMoreElements();) {
            if (((Graph) en.nextElement()).isEnabled()) {
                res = true;
                break;
            }
        }
        return res;
    }

    public void setShiftTime(long shiftTime) {
        this.shiftTime = shiftTime;
        for (Enumeration en = graphs.elements(); en.hasMoreElements();) {
            ((Graph) en.nextElement()).shiftTime = shiftTime;
        }
        this.monitorPanel.checkShiftTime();
    }

    public void clearMaxMin() {
        this.monitorPanel.clearMaxMin();
    }

    public Graph getGraph(String sid) {
        return (Graph) graphs.get(sid);
    }

    private class MonitorPanel extends JPanel {
//        private Vector graphs = new Vector();

//        private int maxValue, realValue, baseValue, minValue;
        private int maxValue, baseValue, minValue;
        private boolean startingWithNull = true;
        private boolean showMaxValue = false;
        private int beginIndex = 0;
        private boolean scaleYDefined = false;
        private boolean minValueDefined = false;
        private int count;
        private int step = 1;
        private int length = 1;
        public int width = 400, graphicsWidth = -1;
        public int height = 200, graphicsHeight = -1;
        private int min_graph_width = 50;
        private int min_graph_height = 50;
        private int nullY = 0;
        private int valuesStringsLen = 20;
        private int horBorder = 5;
        private int verBorder = 5;
        private int border = 5;
        private int pointerSpace = 9;
        private int pointerSize = 7;
        private int stepLineSize = 3;

        private Color backColor;
        private Color gridColor;
        private boolean backWithValues = true;
        private boolean showGrid = true;
        private Image offscreen;
        private Dimension offscreensize;
        private Graphics offgraphics;
        private Image offEmptyScreen;
        private Graphics offEmptyGraphics;
        private Image offEmptyScreenValues;
        private Graphics offEmptyGraphicsValues;
        private Image offEmptyScreenTimesValues;
        private Graphics offEmptyGraphicsTimesValues;

        private FontMetrics fm;
        private int string_height = 20;
        private int descent = 0;

        private int firstX, firstY;
        private boolean drag = false;

        private int timeRefreshTimeout = 0;    // changing value

        Font font = new Font("Helvetica", Font.PLAIN, 12);

        public MonitorPanel() {
            super();
            clearMaxMin();
            this.baseValue = 0;
//            this.graphs = Monitor.this.graphs;
            addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    Dimension d = getSize();
                    setWidth(d.width);
                    setHeight(d.height);
//					System.out.println("!!!!!!!!!!!!!! paint in monitor "+getTitle());
                    repaint();
                }
            });
            setToolTipText("");
        }

        public void clearMaxMin() {
            this.maxValue = height;
            scaleYDefined = false;
            minValueDefined = false;
        }

        public void setStepX(int step) {
            this.step = step;
            this.timeRefreshTimeout = 0;
            this.count = (step == 0) ? 0 : length / step + 1;
            checkBeginIndex(true);
        }

        public void setLenX(int length) {
            if (length == 0) return;
            this.length = length;
            this.count = (step == 0) ? 0 : length / step + 1;
            checkBeginIndex(true);
        }

        public void setFont(Font font) {
            this.font = font;
            if (offgraphics != null && fm != null) {
                offgraphics.setFont(font);
                fm = offgraphics.getFontMetrics();
                string_height = fm.getHeight();
                descent = fm.getDescent();
            }
            checkNullY();
            checkGraphicsHeight();
            checkValuesStringsLen();
            repaint();
        }

        public Font getFont() {
            return this.font;
        }

        public void setWidth(int width) {
            if (width < 2 * horBorder + min_graph_width + valuesStringsLen)
                width = 2 * horBorder + min_graph_width + valuesStringsLen;
            this.width = width;
            checkGraphicsWidth();
            invalidate();
        }

        public void setHeight(int height) {
            if (height < 2 * verBorder + min_graph_height + stepLineSize + string_height + descent) {
                height = 2 * verBorder + min_graph_height + stepLineSize + string_height + descent;
            }
            this.height = height;
            checkNullY();
            checkGraphicsHeight();
            invalidate();
        }

        public void paint(Graphics g) {
            if (isHistoryMonitor) {
                checkValuesStringsLen();
                checkNullY();
                checkGraphicsHeight();
                checkGraphicsWidth();
                this.beginIndex = 0;
                repaintEmptySystem = true;
            }
            createEmptySystem();
            // draw graphics
            if (graphs.size() == 0) return;
            long nowTime = MonitorContainer.getNow();
            int realCount = (!isHistoryMonitor) ? Math.min(length + 1, (int) (nowTime - shiftTime + 1)) : length + 1;

            for (Enumeration e = graphs.elements(); e.hasMoreElements();) {
                Graph gr = (Graph) e.nextElement();
                if (!gr.isEnabled()) continue;
                // draw points & lines
                int ct = 0;
                int v;
                if (repaintEmptySystem || repaintEmptySystemValues) {
                    gr.poly.xpoints = new int[realCount];
                    gr.poly.ypoints = new int[realCount];
                    for (int i = 0; i < realCount; i++) {
                        v = gr.getValueAt(beginIndex + i);
                        if (v != -1) {
                            gr.poly.xpoints[ct] = (int) (i * graphicsWidth / length) + valuesStringsLen + horBorder;
                            gr.poly.ypoints[ct] = nullY - (int) ((v - baseValue) * graphicsHeight / (maxValue - baseValue));
                            ct++;
                        }
                    }
                    gr.poly.npoints = ct;
                } else {
                    if (nowTime > shiftTime + length) {
                        gr.poly.translate((int) ((prevTime - nowTime) * graphicsWidth / length), 0);
                        int minX = valuesStringsLen + horBorder;
                        int curI = 0;
                        try {
                            while (gr.poly.xpoints[curI++] < minX) {
                            }
                        } catch (ArrayIndexOutOfBoundsException aiex) {
                        }
                        curI--;
                        if (curI > 0) {
                            System.arraycopy(gr.poly.xpoints, curI, gr.poly.xpoints, 0, gr.poly.npoints - curI);
                            System.arraycopy(gr.poly.ypoints, curI, gr.poly.ypoints, 0, gr.poly.npoints - curI);
                            gr.poly.npoints = gr.poly.npoints - curI;
                        }
                    }
                    for (int j = (int) (prevTime - shiftTime); j <= nowTime - shiftTime; j++) {
                        if (gr.poly.npoints > 0 && gr.poly.xpoints[gr.poly.npoints - 1] == (int) ((j - beginIndex) * graphicsWidth / length) + valuesStringsLen + horBorder) continue;
                        v = gr.getValueAt(j);
                        if (v != -1)
                            gr.poly.addPoint((int) ((j - beginIndex) * graphicsWidth / length) + valuesStringsLen + horBorder, nullY - (int) ((v - baseValue) * graphicsHeight / (maxValue - baseValue)));
                    }
                }
                offgraphics.setColor(gr.getColor());
                offgraphics.drawPolyline(gr.poly.xpoints, gr.poly.ypoints, gr.poly.npoints);
            }
            g.drawImage(offscreen, 0, 0, null);
            prevTime = nowTime;
            repaintEmptySystem = false;
            repaintEmptySystemValues = false;
            repaintEmptySystemTimesValues = false;
        }

        private void createNewEmptySystem(int curX) {
            if (nullY == 0) nullY = height - stepLineSize - string_height - descent - verBorder;
            if (graphicsWidth == -1) graphicsWidth = width - (2 * horBorder + valuesStringsLen + pointerSpace);
            if (graphicsHeight == -1) graphicsHeight = nullY - (verBorder + pointerSpace);
            // clear all
            offEmptyGraphics.setColor(getBackground());
            offEmptyGraphics.fillRect(0, 0, width, height);
            offEmptyGraphics.setColor(backColor);
            if (backWithValues)
                offEmptyGraphics.fillRect(0, 0, width, height);
            else
                offEmptyGraphics.fillRect(curX, verBorder, width - curX - horBorder, nullY - verBorder);
            // draw the coordinate system
            offEmptyGraphics.setColor(getForeground());
            offEmptyGraphics.drawLine(curX, verBorder, curX, nullY);
            offEmptyGraphics.drawLine(curX, nullY, width - horBorder, nullY);
            offEmptyGraphics.drawLine(width - horBorder - pointerSize, (int) (nullY - pointerSize / 2), width - horBorder, nullY);
            offEmptyGraphics.drawLine(width - horBorder - pointerSize, (int) (nullY + pointerSize / 2), width - horBorder, nullY);
            offEmptyGraphics.drawLine(curX, verBorder, (int) (curX - pointerSize / 2), verBorder + pointerSize);
            offEmptyGraphics.drawLine(curX, verBorder, (int) (curX + pointerSize / 2), verBorder + pointerSize);
            // draw vertical lines
            int topY = (showGrid) ? verBorder : nullY - stepLineSize;
            if (showGrid) offEmptyGraphics.setColor(gridColor);
            for (int i = 1; i <= STEP_COUNT; i++) {
                offEmptyGraphics.drawLine((int) (curX + i * graphicsWidth / 10), topY, (int) (curX + i * graphicsWidth / 10), nullY + stepLineSize);
            }
        }

        private void createNewEmptySystemValues() {
            offEmptyGraphicsValues.drawImage(offEmptyScreen, 0, 0, null);
            // draw y steps & values
            offEmptyGraphicsValues.setColor(getForeground());
            offEmptyGraphicsValues.drawString("" + baseValue, horBorder + valuesStringsLen - fm.stringWidth("" + baseValue) - 5, nullY + stepLineSize);

            int maxVY = verBorder + pointerSpace;
            int rigthX = 0;
            if (showMaxValue) {
                offEmptyGraphicsValues.drawString("" + maxValue, horBorder + valuesStringsLen - fm.stringWidth("" + maxValue) - 5, maxVY + stepLineSize);
                rigthX = (showGrid) ? width - horBorder : valuesStringsLen + horBorder + (stepLineSize - 1) + 2;
                if (showGrid) offEmptyGraphicsValues.setColor(gridColor);
                offEmptyGraphicsValues.drawLine(valuesStringsLen + horBorder - (stepLineSize - 1) - 2, maxVY, rigthX, maxVY);
            }
            int curY = nullY + stepLineSize - string_height;
            int dV = (int) ((maxValue - baseValue) / STEP_COUNT);
            if (dV == 0) dV = 1;
            int nextV = baseValue + dV;
            int nextY = (int) (nullY - (nextV - baseValue) * graphicsHeight / (maxValue - baseValue));
            int dx = 0; // if there is value in cur point - dx will be 4. (the line is longer)
            int spacecnt = 0, curscnt = 1;
            while (nextY >= verBorder + pointerSpace) {
                curscnt -= 1;
                if (curscnt == 0 && nextY + stepLineSize <= curY && (!showMaxValue || nextY - string_height >= maxVY)) {
                    if (showGrid) offEmptyGraphicsValues.setColor(getForeground());
                    offEmptyGraphicsValues.drawString("" + nextV, horBorder + valuesStringsLen - fm.stringWidth("" + nextV) - 5, nextY + stepLineSize);
                    curY = nextY + stepLineSize - string_height;
                    dx = 2;
                    curscnt = spacecnt + 1;
                    spacecnt = 0;
                } else {
                    spacecnt++;
                    curscnt = Math.max(1, curscnt);
                }
                rigthX = (showGrid) ? width - horBorder : valuesStringsLen + horBorder + (stepLineSize - 1) + dx;
                if (showGrid) offEmptyGraphicsValues.setColor(gridColor);
                offEmptyGraphicsValues.drawLine(valuesStringsLen + horBorder - (stepLineSize - 1) - dx, nextY, rigthX, nextY);
                nextV += dV;
                nextY = (int) (nullY - (nextV - baseValue) * graphicsHeight / (maxValue - baseValue));
                dx = 0;
            }
        }

        private void createNewEmptySystemTimesValues(int curX) {
            offEmptyGraphicsTimesValues.drawImage(offEmptyScreenValues, 0, 0, null);
            // draw x steps & values
            offEmptyGraphicsTimesValues.setColor(getForeground());
            monitorContainer.tmpDate.setTime(MonitorContainer.beginTime + (int) (beginIndex + shiftTime) * 1000);
            String firstDateString = monitorContainer.dateFormat.format(monitorContainer.tmpDate);
            int maxStringWidth = fm.stringWidth("" + firstDateString) + 3;
            int firstX = Math.max(0, (int) (horBorder + valuesStringsLen - maxStringWidth / 2));
            int lastVX = firstX + maxStringWidth;
            offEmptyGraphicsTimesValues.drawString("" + firstDateString, firstX, nullY + stepLineSize + string_height);

            int ch = 0, min = 0, sec = 0;
            try {
                int ind = firstDateString.indexOf(":");
                ch = Integer.parseInt(firstDateString.substring(0, ind));
                int ind2 = firstDateString.indexOf(":", ind + 1);
                min = Integer.parseInt(firstDateString.substring(ind + 1, ind2));
                sec = Integer.parseInt(firstDateString.substring(ind2 + 1));
            } catch (NumberFormatException exc) {
            }

//			monitorContainer.tmpDate.setTime(MonitorContainer.beginTime+(int)(beginIndex+shiftTime+length*STEP_COUNT/10)*1000);
//			String lastDateString = monitorContainer.dateFormat.format(monitorContainer.tmpDate);
            String lastDateString = timePlusSeconds(ch, min, sec, length);
            int lastRealX = width - horBorder - maxStringWidth;
//			if (showGrid) offEmptyGraphicsTimesValues.setColor(gridC);
//			offEmptyGraphicsTimesValues.drawLine((int)(curX+STEP_COUNT*graphicsWidth/10), topY, (int)(curX+STEP_COUNT*graphicsWidth/10), nullY+stepLineSize);
//			if (showGrid) offEmptyGraphicsTimesValues.setColor(getForeground());
            offEmptyGraphicsTimesValues.drawString("" + lastDateString, lastRealX, nullY + stepLineSize + string_height);

            for (int i = 1; i < STEP_COUNT; i++) {
//				if (showGrid) offEmptyGraphicsTimesValues.setColor(gridC);
//                offEmptyGraphicsTimesValues.drawLine((int)(curX+i*graphicsWidth/10), topY, (int)(curX+i*graphicsWidth/10), nullY+stepLineSize);
                if (MonitorContainer.beginTime != 0) {
//                    monitorContainer.tmpDate.setTime(MonitorContainer.beginTime+(int)(beginIndex+shiftTime+length*i/10)*1000);
//                    String nextDateString = monitorContainer.dateFormat.format(monitorContainer.tmpDate);
                    String nextDateString = timePlusSeconds(ch, min, sec, (int) length * i / 10);
                    int nextX = (int) (curX + i * graphicsWidth / 10 - maxStringWidth / 2);
                    if (nextX > lastVX + SPACE && nextX + maxStringWidth < lastRealX) {
                        lastVX = nextX + maxStringWidth;
//						if (showGrid) offEmptyGraphicsTimesValues.setColor(getForeground());
                        offEmptyGraphicsTimesValues.drawString("" + nextDateString, nextX, nullY + stepLineSize + string_height);
                    }
                }
            }
        }

        private void createNewScreens() {
            offscreen = createImage(width, height);
            offgraphics = offscreen.getGraphics();
            offgraphics.setFont(font);
            offEmptyScreen = createImage(width, height);
            offEmptyGraphics = offEmptyScreen.getGraphics();
            offEmptyGraphics.setFont(font);
            offEmptyScreenValues = createImage(width, height);
            offEmptyGraphicsValues = offEmptyScreenValues.getGraphics();
            offEmptyGraphicsValues.setFont(font);
            offEmptyScreenTimesValues = createImage(width, height);
            offEmptyGraphicsTimesValues = offEmptyScreenTimesValues.getGraphics();
            offEmptyGraphicsTimesValues.setFont(font);
        }

        private void createEmptySystem() {
            if ((offscreen == null) || (this.width != offscreensize.width) || (this.height != offscreensize.height)) {
                createNewScreens();
                offscreensize = new Dimension(width, height);
                fm = offgraphics.getFontMetrics();
                string_height = fm.getHeight();
                descent = fm.getDescent();
                repaintEmptySystem = true;
            }
            int curX = horBorder + valuesStringsLen;
            if (repaintEmptySystem) {
                createNewEmptySystem(curX);
                repaintEmptySystemValues = true;
            }
            if (repaintEmptySystemValues) {
                createNewEmptySystemValues();
                repaintEmptySystemTimesValues = true;
            }
            if (repaintEmptySystemTimesValues) {
                createNewEmptySystemTimesValues(curX);
            }
            offgraphics.drawImage(offEmptyScreenTimesValues, 0, 0, null);
        }

        private String timePlusSeconds(int ch1, int m1, int s1, int sec) {
            int sr = s1 + sec;
            int rch = (int) sr / 60;
            if (rch > 0) {
                m1 += rch;
                sr -= rch * 60;
            }
            rch = (int) m1 / 60;
            if (rch > 0) {
                ch1 += rch;
                m1 -= rch * 60;
            }
            rch = (int) ch1 / 24;
            if (rch > 0) {
                ch1 -= rch * 24;
            }
            return ch1 + ":" + m1 + ":" + sr;
        }

        private String doubleChar(int i) {
            String res = "" + i;
            return (res.length() == 1) ? "0" + res : res;
        }

        public void valueAdded(int value, boolean isEnabled) {
            if (!isEnabled) return;
            if (value == -1) return;
            if (!minValueDefined || value < minValue) {
//				repaintEmptySystemValues = true;  // it will be done in checkBaseValue()
                minValue = value;
                checkBaseValue();
                minValueDefined = true;
            }
            if (value != 0 && (!scaleYDefined || value >= maxValue)) {
                repaintEmptySystemValues = true;
                maxValue = value + 1;
                checkValuesStringsLen();  // there is checking fm in this method
                scaleYDefined = true;
            }
        }

        private boolean isCriticalMax(int index) {
            for (Enumeration e = graphs.elements(); e.hasMoreElements();) {
                Graph gr = (Graph) e.nextElement();
                if (!gr.isEnabled()) continue;
                if (gr.getValueAt(index) == maxValue - 1)
                    return true;
            }
            return false;
        }

        private boolean isCriticalMin(int index) {
            for (Enumeration e = graphs.elements(); e.hasMoreElements();) {
                Graph gr = (Graph) e.nextElement();
                if (!gr.isEnabled()) continue;
                if (gr.getValueAt(index) == minValue)
                    return true;
            }
            return false;
        }

        private void checkMaxMin(boolean globalCheck) {
            int realCount = (!isHistoryMonitor) ? Math.min(length + 1, (int) (MonitorContainer.getNow() - shiftTime + 1)) : length + 1;
//			int oldMax = maxValue;
            clearMaxMin();
            for (Enumeration e = graphs.elements(); e.hasMoreElements();) {
                Graph gr = (Graph) e.nextElement();
                if (!gr.isEnabled()) continue;
                // draw points & lines
                int v;
                for (int i = 0; i < realCount; i++) {
                    valueAdded(gr.getValueAt(beginIndex + i), true);
                }
            }
//			if (!globalCheck && maxValue > oldMax) maxValue = oldMax;
        }

        private boolean repaintEmptySystem = false;
        private boolean repaintEmptySystemValues = false;
        private boolean repaintEmptySystemTimesValues = false;

        private void checkBeginIndex(boolean globalCheck) {
            // globalCheck if step || history changed || new graph added
            // and not if it's new second with drag
            if (isHistoryMonitor || graphs.size() == 0) {
                beginIndex = 0;
                if (globalCheck && isHistoryMonitor) checkMaxMin(globalCheck);
                return;
            }
            int newBeginIndex = Math.max(0, (int) (MonitorContainer.getNow() - length - shiftTime));
            if (globalCheck || beginIndex != newBeginIndex) {
                int di = newBeginIndex - beginIndex;
                beginIndex = newBeginIndex;
                if (!globalCheck) {
                    // next second
                    repaintEmptySystemTimesValues = true;
                    for (int i = 1; i <= di; i++) {
                        // think about inc index
                        if (isCriticalMax(beginIndex - i) || isCriticalMin(beginIndex - i)) {
                            checkMaxMin(globalCheck);
                            break;
                        }
                    }
                } else
                    checkMaxMin(globalCheck);
            }
        }

        public void paintMonitor() {
//			System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! paintMonitor in monitor "+getTitle());
            checkBeginIndex(false);
            repaint();
        }

        public void setStartingWithNull(boolean b) {
            this.startingWithNull = b;
            checkBaseValue();
            repaint();
        }
//		public void showMaxValue(boolean b){
//            this.showMaxValue = b;
//            repaint();
//		}
        public void checkBaseValue() {
            baseValue = (startingWithNull) ? 0 : minValue;
            repaintEmptySystemValues = true;
        }

        private void checkValuesStringsLen() {
            if (fm != null) {
                if (fm.stringWidth("" + maxValue) > valuesStringsLen) {
                    repaintEmptySystem = true;
                    valuesStringsLen = fm.stringWidth("" + maxValue) + 5;
                    checkGraphicsWidth();
                }
//                if (fm.stringWidth(""+realValue) > valuesStringsLen){
//                    valuesStringsLen = fm.stringWidth(""+realValue) + 5;
//                    checkGraphicsWidth();
//                }
            }
        }

        private void checkGraphicsHeight() {
            if (graphicsHeight != -1 && nullY != 0) {
                graphicsHeight = nullY - (verBorder + pointerSpace);
            }
        }

        private void checkGraphicsWidth() {
            if (graphicsWidth != -1)
                graphicsWidth = width - (2 * horBorder + valuesStringsLen + pointerSpace);
        }

        private void checkNullY() {
            if (nullY != 0)
                nullY = height - stepLineSize - string_height - descent - verBorder;
        }

        public void checkShiftTime() {
            checkBeginIndex(true);
            repaint();
        }

        public Dimension getPreferredSize() {
            return new Dimension(width, height);
        }


        public void setToolTipText(String text) {
            putClientProperty(TOOL_TIP_TEXT_KEY, text);
            ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
            if (text != null) {
                toolTipManager.registerComponent(this);
            } else {
                toolTipManager.unregisterComponent(this);
            }
            toolTipManager.setInitialDelay(10);
            toolTipManager.setReshowDelay(10);
        }

        public String getToolTipText(MouseEvent e) {
            String res = null;
            int realX = e.getX() - valuesStringsLen - horBorder;
            int index = realX * length / graphicsWidth;
            int xIn = (int) (index * graphicsWidth / length);
            int xNx = (int) ((index + 1) * graphicsWidth / length);
            if (realX - xIn > xNx - realX) index++;
            res = checkValue(beginIndex + index, e.getY());
            if (res != null) return res;
            int count = 4;
            int din = 1;
            boolean rigth = true;
            while (count-- > 0) {
                if (rigth)
                    index += din;
                else
                    index -= din;
                din++;
                rigth = !rigth;
                res = checkValue(beginIndex + index, e.getY());
                if (res != null) return res;
            }
            return res;
        }

        private String checkValue(int index, int py) {
            int xIn = (int) (index * graphicsWidth / length);
            if (index >= 0 && index < MonitorContainer.getNow() - shiftTime) {
//                for (int i = 0; i < graphs.size(); i++){
                for (Enumeration en = graphs.elements(); en.hasMoreElements();) {
                    Graph g = (Graph) en.nextElement();
                    float v = g.getValueAt(index);
                    if (v != -1) {
                        int y = nullY - (int) ((v - baseValue) * graphicsHeight / (maxValue - baseValue));
                        int x = xIn + valuesStringsLen + horBorder;
                        int dif = (py > y) ? py - y : y - py;
                        if (dif < grex) {
                            return "" + v;
                        }
                    }
                }
            }
            return null;
        }

        public void setBackColor(Color c) {
            this.backColor = c;
            if (c.equals(Color.black)) {
                setForeground(DEFAULT_GRID);
            } else {
                setForeground(getParent().getForeground());
            }
            repaintEmptySystem = true;
        }

        public void setGridColor(Color c) {
            this.gridColor = c;
            repaintEmptySystem = true;
        }

        public void setWithHorBorder(boolean b) {
            horBorder = (b) ? border : 0;
            checkGraphicsWidth();
            repaint();
        }

        public void setWithVerBorder(boolean b) {
            verBorder = (b) ? border : 0;
            checkNullY();
            checkGraphicsHeight();
            repaint();
        }

        public void setBackWithValues(boolean b) {
            backWithValues = b;
            repaint();
        }

        public void showGrid(boolean b) {
            repaintEmptySystem = true;
            showGrid = b;
            repaint();
        }

    }


    private class GraphicsNamePanel extends JComponent {
        public int width = 10;
        public int height = 10;
        private int minValue = 10;
        Font font = new Font("Helvetica", Font.PLAIN, 12);
        private FontMetrics fm = null;
        private int string_height = 0;
        private int descent = 0;
        private Dimension rectDim = new Dimension(40, 3);
        private int shift = 10;

        private Image offscreen;
        private Dimension offscreensize;
        private Graphics offgraphics;

        public GraphicsNamePanel() {
            super();
        }

        public void paint(Graphics g) {
//			if (!isHistoryMonitor){
//			System.out.println("paint names, check width in monitor "+getTitle());
            checkWidth();
//			System.out.println("check height in monitor "+getTitle());
            checkHeight();
//			}
            createOffGraphics();
            if (enabledCount == 0) return;
            int cnt = 1;
            for (Enumeration e = graphs.elements(); e.hasMoreElements();) {
                Graph gr = (Graph) e.nextElement();
                if (!gr.isEnabled()) continue;
                // draw graphs name
                int strLevel = cnt * string_height;
                offgraphics.setColor(gr.getColor());
                offgraphics.fillRect(0, strLevel - (int) (string_height / 2), rectDim.width, rectDim.height);
                offgraphics.setColor(getForeground());
                offgraphics.drawString(gr.getCaption(), rectDim.width + shift, strLevel);
                cnt++;
            }
            g.drawImage(offscreen, 0, 0, null);
        }

        private void createOffGraphics() {
            if ((offscreen == null) || (this.width != offscreensize.width) || (this.height != offscreensize.height)) {
                offscreen = createImage(width, height);
                offscreensize = new Dimension(width, height);
                offgraphics = offscreen.getGraphics();
                offgraphics.setFont(font);
                if (fm == null) {
                    fm = offgraphics.getFontMetrics();
                    string_height = fm.getHeight();
                    descent = fm.getDescent();
                }
            }
            offgraphics.setColor(getBackground());
            offgraphics.fillRect(0, 0, width, height);
        }

        private int getGraphCaptionLenght(Graph g) {
            return (fm == null) ? 0 : fm.stringWidth(g.getCaption()) + rectDim.width + shift;
        }

        public void checkWidth() {
            width = minValue;
            for (Enumeration e = graphs.elements(); e.hasMoreElements();) {
                Graph gr = (Graph) e.nextElement();
                if (!gr.isEnabled()) continue;
                width = Math.max(width, getGraphCaptionLenght(gr));
            }
            invalidate();
//			System.out.println("getParent().doLayout() in monitor "+getTitle());
//			System.out.println("getParent() = "+getParent());
            getParent().doLayout();
        }

        private void checkHeight() {
            height = enabledCount * string_height;
            height = Math.max(minValue, height);
            invalidate();
//			System.out.println("getParent().doLayout() in monitor "+getTitle());
            getParent().doLayout();
        }

        public void checkGraphicsHeight() {
            if (height != enabledCount * string_height) {
                height = enabledCount * string_height;
                height = Math.max(minValue, height);
                invalidate();
                getParent().doLayout();
                repaint();
            }
        }

        public Dimension getPreferredSize() {
            return new Dimension(width, height);
        }

        public Dimension getMinimumSize() {
            return new Dimension(width, height);
        }

        public Dimension getMaximumSize() {
            return new Dimension(width, height);
        }
    }

    private class Controller extends JPanel {
        TimeChanger timeChanger;
        int width = 0;

        public Controller(String labelString, ActionListener l) {
            super();
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
            add(new JLabel(labelString + " :") {
                public Dimension getPreferredSize() {
                    if (super.getPreferredSize().width > width) width = super.getPreferredSize().width;
                    return new Dimension(width, super.getPreferredSize().height);
                }

                public Dimension getMinimumSize() {
                    if (super.getMinimumSize().width > width) width = super.getMinimumSize().width;
                    return new Dimension(width, super.getMinimumSize().height);
                }

                public Dimension getMaximumSize() {
                    if (super.getMaximumSize().width > width) width = super.getMaximumSize().width;
                    return new Dimension(width, super.getMaximumSize().height);
                }
            });
            timeChanger = new TimeChanger(false, true);
            add(timeChanger);
            JButton b = new JButton("Apply");
            b.addActionListener(l);
            add(b);
        }

        public void setSeconds(int sec) {
            timeChanger.setSeconds(sec);
        }

        public void setSeconds(String ssec) {
            timeChanger.setSeconds(ssec);
        }

        public int getSeconds() {
            return timeChanger.getSeconds();
        }
    }


    public void addMonitorNameChangedListener(MonitorNameChangedListener l) {
        listeners.addElement(l);
    }

    public void removeMonitorNameChangedListener(MonitorNameChangedListener l) {
        listeners.removeElement(l);
    }

    public void fireMonitorNameChanged() {
        for (Enumeration e = listeners.elements(); e.hasMoreElements();) {
            ((MonitorNameChangedListener) e.nextElement()).monitorNameChanged(this.monitorInfo);
        }
    }

    public void setShowing(boolean b) {
        this.showing = b;
        if (!isIcon())
            setVisible(b);
        else {
            getDesktopIcon().setVisible(b);
            setVisible(b);
            if (b) {
                try {
                    setIcon(false);
                } catch (PropertyVetoException e) {
                }
            }
        }
    }

    public boolean getShowing() {
        return this.showing;
    }

    public JCheckBoxMonitorMenuItem getMenuItem() {
        if (menuItem == null) {
            menuItem = new JCheckBoxMonitorMenuItem(this);
        }
        return menuItem;
    }

    private void setNewMinimum(JPanel p) {
        p.doLayout();
        int count = p.getComponentCount();
        int wid = 0;
        int hei = 0;
        for (int i = 0; i < count; i++) {
            Rectangle rect = p.getComponent(i).getBounds();
            wid = Math.max(rect.x + rect.width, wid);
            hei = Math.max(rect.y + rect.height, hei);
        }
        p.setMinimumSize(new Dimension(wid, hei));
    }
}
