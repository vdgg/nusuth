/*
 * @(#)ClusterViewPanel.java 1.0 07/29/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.DistributorState;
import com.azoft.nusuth.management.ManagementException;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;

/**
 * Class ClusterViewPanel contains a DnD component with distributors & their containers,
 * panel for choosing connection colours and refresh controller.
 *
 * @version 1.0 07/29/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ClusterViewPanel extends JPanel implements PropertyChangeListener {
    private static final String SCONTAINER = "container";
    private static final String SDISTRIBUTOR = "distributor";

    BasicPanel basicPanel;                              // the basic panel
    ClusterViewRequestThread clusterViewRequestThread;  // the cluster thread - gets the states
    DnDComponent dnd;                                   // the dnd component
    ControlColoredLine controlColoredLine;              // the control colored line
    Properties clusterViewProps;                        // the link to cluster properties
    boolean paintText = true;                           // specifies paint names or not
    int refresh = 1;                                    // the refresh delay
    JPanel refreshController;                           // the refresh controller
    TimeChanger timeChanger;                            // the refresh time changer

    boolean componentsAdded = false;                    // fixes weither components were added or not


    /**
     * Constructs a new cluster view panel with the specified basic panel
     * and cluster properties.
     * Inits all components, parses properties, inits the cluster thread.
     *
     * @param   basicPanel          the basic panel.
     * @param   clusterViewProps    the cluster properties.
     */
    public ClusterViewPanel(BasicPanel basicPanel, Properties clusterViewProps) {
        super();
        this.basicPanel = basicPanel;
        this.clusterViewProps = clusterViewProps;
        setLayout(new BorderLayout());

        initPropertyValues();
        initDnDComponent();
        initControlColoredLine();
        initControlPanel();
        initClusterRequestThread();
        BasicPanel.addChangingValuesElementRenderer("distributor", new ComponentChangingValuesController(SDISTRIBUTOR));
        BasicPanel.addChangingValuesElementRenderer("container", new ComponentChangingValuesController(SCONTAINER));
    }

    // inits the dnd component
    private void initDnDComponent() {
        dnd = new DnDComponent();
        add(dnd, BorderLayout.CENTER);
    }

    // inits the control colored line
    // uses edges & colors properties.
    private void initControlColoredLine() {
        String edges = clusterViewProps.getProperty("edges.values", "");
        StringTokenizer st = new StringTokenizer(edges, ";");
        int[] eds = new int[st.countTokens()];
        int cnt = 0;
        while (st.hasMoreTokens()) {
            try {
                int nextInt = Integer.parseInt(st.nextToken());
                eds[cnt++] = nextInt;
            } catch (NumberFormatException e) {
                System.out.println("ERROR in cluster view property - edges.values has not int value");
                eds[cnt++] = 10;
            }
        }

        String colors = clusterViewProps.getProperty("colors.values", "");
        st = new StringTokenizer(colors, ";");
        Color[] cs = new Color[st.countTokens()];
        cnt = 0;
        while (st.hasMoreTokens()) {
            Color nextColor = Graph.getColor(st.nextToken());
            cs[cnt++] = nextColor;
        }

        controlColoredLine = (eds.length == 0 || cs.length == 0)
                ? new ControlColoredLine(this)
                : new ControlColoredLine(this, eds, cs);
        controlColoredLine.addPropertyChangeListener(this);
    }

    // inits the control panel - refresh controller & color controller
    private void initControlPanel() {
        JPanel cp = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 0);
        cp.add(new JPanel(), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.anchor = GridBagConstraints.EAST;
        cp.add(getRefreshController(), c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(5, 0, 0, 0);
        cp.add(controlColoredLine, c);
        add(cp, BorderLayout.SOUTH);
    }

    // inits the paintText & refresh delay property values
    private void initPropertyValues() {
        String paintT = clusterViewProps.getProperty("paintText", "");
        paintText = paintT.equals("true");

        String srefresh = clusterViewProps.getProperty("refresh", "");
        try {
            refresh = Integer.parseInt(srefresh);
        } catch (NumberFormatException e) {
            System.out.println("can not parse refresh property to int");
        }
    }

    // inits the cluster thread
    private void initClusterRequestThread() {
        clusterViewRequestThread = new ClusterViewRequestThread(this);
        clusterViewRequestThread.setRequestThreadListener(new RequestThreadListener() {
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
                        basicPanel.status.setStatusString("Cannot retrieve distributor states");
                        System.out.println(e.getException());
                    }
                } else {
                    MonitorRequestThreadEvent mrte = (MonitorRequestThreadEvent) e;
                    if (mrte.isRepaint()) {
                        dnd.repaint();
                    }
                }
            }
        });
    }

    // inits and gets the refresh controller
    private JPanel getRefreshController() {
        if (refreshController == null) {
            refreshController = new JPanel(new GridBagLayout()); //FlowLayout.LEFT, 5, 0));
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            c.insets = new Insets(0, 0, 0, 5);
            c.gridwidth = 1;

            timeChanger = new TimeChanger(false, true);
            refreshController.add(new JLabel("Refresh" + " :"), c);

            c.gridwidth = GridBagConstraints.RELATIVE;
            refreshController.add(timeChanger, c);
            JButton b = new JButton("Apply");
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int newRefresh = timeChanger.getSeconds();
                    if (newRefresh != -1) {
                        refresh = newRefresh;
                    }
                }
            });
            c.gridwidth = GridBagConstraints.REMAINDER;
            refreshController.add(b, c);
            timeChanger.setSeconds(refresh);
        }
        return refreshController;
    }

    /**
     * Starts the cluster thread.
     *
     * @see #stopThread()
     */
    public void startThread() {
        if (clusterViewRequestThread != null)
            clusterViewRequestThread.renew();
    }

    /**
     * Stops the cluster thread.
     *
     * @see #startThread()
     */
    public void stopThread() {
        if (clusterViewRequestThread != null)
            clusterViewRequestThread.suspendThread();
    }

    /**
     * Adds the components from specified distributos node.
     *
     * @param   distsnode   the distributos node.
     */
/*
    public void addComponents(DefaultMutableTreeNode distsnode) {
        dnd.addComponents(distsnode);
    }
*/

    /**
     * Gets the color by intensity.
     *
     * @param   intens  the intensity.
     * @return  the corresponding color
     */
    public Color getIntensityColor(int intens) {
        return controlColoredLine.getIntensityColor(intens);
    }

    /**
     * Method from interface PropertyChangeListener.
     * Invoked when property was changed.
     *
     * @param   evt   an event.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("color")) {
            dnd.checkLineColors();
            repaint();
        }
    }

    /**
     * Saves all necessary properties to specified cluster properties.
     *
     * @param   clusterViewProps    the specified cluster properties.
     */
    public void saveClusterViewProperties(Properties clusterViewProps) {
        controlColoredLine.saveProperties(clusterViewProps);
        dnd.saveProperties(clusterViewProps);
        clusterViewProps.setProperty("refresh", "" + refresh);
    }

    /**
     * If b is true all component in DnD will have its names,
     * false - overwise.
     *
     * @param   b   paint or not component names.
     */
    public void setShowNames(boolean b) {
        this.paintText = b;
        dnd.setShowNames(b);
    }

    /**
     * Layout the DnD panel with star layout.
     */
    public void doStarLayout() {
        dnd.setDoLayout(true);
        dnd.doLayout();
    }

    /**
     * Gets weither components were added or not
     *
     * @return  the <code>true</code> if  components were added and <code>false</code> overwise.
     * @see #setComponentsAdded(boolean)
     */
    public boolean isComponentsAdded() {
        return componentsAdded;
    }

    /**
     * Sets the boolean weither components were added or not
     *
     * @param  the boolean weither components were added or not
     * @see #isComponentsAdded()
     */
    public void setComponentsAdded(boolean componentsAdded) {
        this.componentsAdded = componentsAdded;
    }

    /**
     * Removes all component from the dnd panel.
     */
    public void removeAllComponents() {
        dnd.removeAllComponents();
        setComponentsAdded(false);
    }

    /**
     * The DnDComponent class. It controls the component moving.
     * Allows the drag and drop.
     * It stores & paints all connections between distributors & containers.
     */
    class DnDComponent extends JComponent implements DropTargetListener, DragGestureListener, DragSourceListener {
        NamedImageComponent draggedComponent;           // stores the dragged component
//        Vector lines = new Vector();                    // stores all lines
        Hashtable hashLines = new Hashtable();              // distributor --> vector of lines
        // for its containers
        private Hashtable containers = new Hashtable(); // container name --> container
        private Hashtable cache = new Hashtable();      // cache for images
        private boolean doLayout = true;                // do layout when component resized or not


        /**
         * Constructs a new DnD component. Executes all operations for drag and drop.
         * Sets the star layout.
         */
        public DnDComponent() {
            super();
            DragSource dragSource = DragSource.getDefaultDragSource();
            dragSource.createDefaultDragGestureRecognizer(
                    this, // component where drag originates
                    DnDConstants.ACTION_COPY_OR_MOVE, // actions
                    this); // drag gesture recognizer
            new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
            setLayout(new StarLayout(FlowLayout.CENTER, 10, 10) {
                public void layoutContainer(Container target) {
                    if (doLayout) {
                        super.layoutContainer(target);
                        DnDComponent.this.checkLines();
                    }
                }
            });
        }

        /**
         * Adds a distributor & all its containers by the specified distributor node.
         * All containers are taken from dist node user object.
         * If user object is String (component isn't loaded yet), we will load it.
         *
         * @param   distnode    the specified distributor node
         */
        public void addDistributor(ConfigMutableTreeNode distnode) {
            if (distnode == null) return;
            String systemId = distnode.getComponentId();
            String name = distnode.toString();

            NamedImageComponent dist = newDistributor(systemId, name);
            add(dist, new StarConstraints(StarConstraints.CENTER, null));
            checkLocation(SDISTRIBUTOR, dist);

            Object userObject = distnode.getUserObject();
            if (userObject instanceof String) {
                // this method load component and set loaded composite element as user object
                CompositeNusuthWebAppElement newUserObject =
                        basicPanel.getCompositeUserObject(
                                distnode.getType(), distnode.getComponentId());
                if (newUserObject != null) {
                    userObject = newUserObject;
                }
            }
            if (userObject instanceof CompositeNusuthWebAppElement) {
                addStarSpikes(dist, (CompositeNusuthWebAppElement) userObject);
            }
        }

        /**
         * Adds a new container.
         */
        public void addContainer(String id, String item) {
            System.out.println("add a new container with name " + item + " and id = " + id);
        }

        /**
         * Removes a component and all its lines by specified type & id.
         *
         * @param   type    the component type.
         * @param   id      the component id.
         * @see #addDistributor(ConfigMutableTreeNode)
         * @see #addContainer(String, String)
         * @see #changeComponentName(String, String, String)
         */
        public void removeComponent(String type, String id) {
            int cnt = getComponentCount();
            for (int i = 0; i < cnt; i++) {
                Component next = getComponent(i);
                if (next instanceof NamedImageComponent
                        && ((NamedImageComponent) next).getType().equals(type)
                        && ((NamedImageComponent) next).getSystemId().equals(id)) {
                    remove(next);
                    removeLines(type, next);
                    repaint();
                    return;
                }
            }
        }

        // remove all lines connecting with comp component
        private void removeLines(String type, Component comp) {
            if (type.equals(SDISTRIBUTOR)) {
                hashLines.remove(comp);
            } else {
                // when comp type is container
                Enumeration en = hashLines.elements();
                while (en.hasMoreElements()) {
                    Vector v = (Vector) en.nextElement();
                    if (v != null) {
                        for (int i = 0; i < v.size(); i++) {
                            ColoredLine line = (ColoredLine) v.elementAt(i);
                            if (line.getC2() == comp) {
                                v.remove(line);
                                i--;
                            }
                        }
                    }
                }
            }
        }

        /**
         * Changes the name of component with the specified type & id.
         *
         * @param   type    the component type.
         * @param   id      the component id.
         * @param   newName the new component name.
         */
        public void changeComponentName(String type, String id, String newName) {
            int cnt = getComponentCount();
            for (int i = 0; i < cnt; i++) {
                Component next = getComponent(i);
                if (next instanceof NamedImageComponent
                        && ((NamedImageComponent) next).getType().equals(type)
                        && ((NamedImageComponent) next).getSystemId().equals(id)) {
                    ((NamedImageComponent) next).setName(newName);
                    checkLines();
                    return;
                }
            }
        }

        /**
         * Removes all components from this component.
         */
        public void removeAllComponents() {
            super.removeAll();
            for (Enumeration e = hashLines.keys(); e.hasMoreElements();) {
                hashLines.remove(e.nextElement());
            }
            for (Enumeration e = containers.keys(); e.hasMoreElements();) {
                containers.remove(e.nextElement());
            }
            repaint();
        }

        // creates a new distributor
        private NamedImageComponent newDistributor(String systemId, String name) {
            return new NamedImageComponent(SDISTRIBUTOR, systemId, name, getImage(SDISTRIBUTOR + ".gif"), paintText);
        }

        // creates a new container
        private NamedImageComponent newContainer(String name) {
            // without systemId because container-info param has only name
            NamedImageComponent nic = new NamedImageComponent(SCONTAINER, name, name, getImage(SCONTAINER + ".gif"), paintText);
            containers.put(name, nic);
            return nic;
        }

        // gets a container from hashtable by name.
        private NamedImageComponent getContainerFromCache(String name) {
            return (NamedImageComponent) containers.get(name);
        }

        // checks the component location by properties.
        private void checkLocation(String type, NamedImageComponent c) {
            String xloc = clusterViewProps.getProperty(type + "." + c.getSystemId() + ".x");
            String yloc = clusterViewProps.getProperty(type + "." + c.getSystemId() + ".y");
            if (xloc != null && yloc != null) {
                try {
                    int px = Integer.parseInt(xloc);
                    int py = Integer.parseInt(yloc);
                    c.setLocation(px, py);
                    doLayout = false;
                } catch (NumberFormatException e) {
                    System.out.println("ERROR in cluster view properties - x or y location is not int");
                }
            }
        }

        // adds line in hashtable by distributor key
        private synchronized void addLineByDistributor(NamedImageComponent distributor, ColoredLine line) {
            Vector v = (Vector) hashLines.get(distributor); // vector of lines
            if (v == null) {
                v = new Vector();
                hashLines.put(distributor, v);
            }
            if (!v.contains(line)) v.addElement(line);
        }

        // adds the star spikes by the distributor (center) & distributor node user object.
        // user object - it's a composite element, contains all simple & composite childs.
        // it have to contain "container-info" element with "name" component - container!
        private void addStarSpikes(NamedImageComponent center, CompositeNusuthWebAppElement userObject) {
            if (userObject != null) {
                try {
                    CompositeNusuthWebAppElement co = (CompositeNusuthWebAppElement) userObject;
                    Enumeration en = co.getCompositeChild("container-info");
                    while (en.hasMoreElements()) {
                        CompositeNusuthWebAppElement child = (CompositeNusuthWebAppElement) en.nextElement();
                        try {
                            Enumeration ename = child.getSimpleChild("name");
                            if (ename.hasMoreElements()) {
                                String contName = ((SimpleNusuthWebAppElement) ename.nextElement()).getContent();
                                NamedImageComponent ic = getContainerFromCache(contName);
                                if (ic == null) {
                                    ic = newContainer(contName);
                                    add(ic, new StarConstraints(StarConstraints.SPIKE, center));
                                    checkLocation(SCONTAINER, ic);
                                }
                                addLineByDistributor(center, new ColoredLine(center, ic));
//                                lines.addElement(new ColoredLine(center, ic));
                            }
                        } catch (DeploymentException e) {
                            System.out.println("DeploymentException e = " + e);
                        }
                    }
                } catch (DeploymentException e) {
                    System.out.println("DeploymentException e = " + e);
                }
            }
        }

        // checks all lines - if components were moved
        private synchronized void checkLines() {
            for (Enumeration en = hashLines.elements(); en.hasMoreElements();) {
                Vector v = (Vector) en.nextElement();
                for (Enumeration e = v.elements(); e.hasMoreElements();) {
                    ColoredLine line = (ColoredLine) e.nextElement();
                    line.checkP1();
                    line.checkP2();
                }
            }
            repaint();
        }

        /**
         * Checks the connection colors.
         * Gets the distributors states & corresponding function value and then
         * gets the color by value.
         */
        synchronized void checkIntensity() throws UnauthorizedAccessException, ManagementException {
            for (Enumeration en = hashLines.keys(); en.hasMoreElements();) {
                NamedImageComponent distributor = (NamedImageComponent) en.nextElement();
                DistributorState state = distributorState(distributor);
                Vector v = (Vector) hashLines.get(distributor);
                for (Enumeration e = v.elements(); e.hasMoreElements();) {
                    ColoredLine line = (ColoredLine) e.nextElement();
                    int intens = getIntensity(state, (NamedImageComponent) line.getC2());
                    Color c = getIntensityColor(intens);
                    line.setIntensity(intens);
                    line.setColor(c);
                }
            }
        }

        /**
         * Gets the distributors state by distributor component.
         */
        private DistributorState distributorState(NamedImageComponent dist) throws UnauthorizedAccessException, ManagementException {
            return BasicPanel.getDistributorState(dist.getSystemId());
        }

        /**
         * Gets the function value by state & container.
         */
        public int getIntensity(DistributorState state, NamedImageComponent c2) throws UnauthorizedAccessException, ManagementException {
            return state.getContainerRequestCount(c2.getSystemId());
        }

        /**
         * Calls when color controller changes its colors.
         * Gets the intensity from line and requries a new color for it.
         */
        synchronized void checkLineColors() {
            for (Enumeration en = hashLines.elements(); en.hasMoreElements();) {
                Vector v = (Vector) en.nextElement();
                for (Enumeration e = v.elements(); e.hasMoreElements();) {
                    ColoredLine line = (ColoredLine) e.nextElement();
                    line.setColor(getIntensityColor(line.getIntensity()));
                }
            }
        }

        /**
         * Sets show names of component or not.
         *
         * @param   b   if b is true all components will show its names,
         * false - overwise.
         */
        void setShowNames(boolean b) {
            Component[] comps = getComponents();
            for (int i = 0; i < comps.length; i++) {
                if (comps[i] instanceof NamedImageComponent) {
                    ((NamedImageComponent) comps[i]).setPaintText(b);
                }
            }
            checkLines();
        }

        /**
         * Sets layout this component with star layout or not.
         *
         * @param   b   is b is true component will layout with star layout.
         */
        public void setDoLayout(boolean b) {
            doLayout = b;
        }

        /**
         * Method from drag & drop listener.
         * Invoked when component was catched.
         */
        public void dragGestureRecognized(DragGestureEvent e) {
            Component c = getComponentAt(e.getDragOrigin());
            if (c != null && c instanceof NamedImageComponent) {
                draggedComponent = (NamedImageComponent) c;
                e.startDrag(DragSource.DefaultCopyDrop, // cursor
                        new StringSelection(""), // transferable
                        this);  // drag source listener
                doLayout = false;
            }
        }

        /**
         * Method from drag & drop listener.
         */
        public void dragDropEnd(DragSourceDropEvent e) {
        }

        /**
         * Method from drag & drop listener.
         */
        public void dragEnter(DragSourceDragEvent e) {
        }

        /**
         * Method from drag & drop listener.
         */
        public void dragExit(DragSourceEvent e) {
        }

        /**
         * Method from drag & drop listener.
         */
        public void dragOver(DragSourceDragEvent e) {
        }

        /**
         * Method from drag & drop listener.
         */
        public void dropActionChanged(DragSourceDragEvent e) {
        }

        // draws the dragged image & checks lines.
        private void drawImage(Point p) {
            NamedImageComponent b = draggedComponent;
            if (b != null) {
                b.setLocation(p.x, p.y);
                for (Enumeration en = hashLines.elements(); en.hasMoreElements();) {
                    Vector v = (Vector) en.nextElement();
                    for (Enumeration e = v.elements(); e.hasMoreElements();) {
                        ColoredLine line = (ColoredLine) e.nextElement();
                        if (line.getC1() == b)
                            line.checkP1();
                        else if (line.getC2() == b) line.checkP2();
                    }
                }
            }
            repaint();
        }

        /**
         * Method from drag & drop listener.
         * Invoked when drop happens.
         */
        public void drop(DropTargetDropEvent e) {
            e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            drawImage(e.getLocation());
            e.dropComplete(true);
        }

        /**
         * Method from drag & drop listener.
         * Invoked when drop over.
         */
        public void dragOver(DropTargetDragEvent e) {
            e.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
            drawImage(e.getLocation());
        }

        /**
         * Method from drag & drop listener.
         */
        public void dragEnter(DropTargetDragEvent e) {
        }

        /**
         * Method from drag & drop listener.
         */
        public void dragExit(DropTargetEvent e) {
        }

        /**
         * Method from drag & drop listener.
         */
        public void dropActionChanged(DropTargetDragEvent e) {
        }

        /**
         * Overrides the super paint method to paint all connections.
         *
         * @param   g   the graphics context
         */
        public void paint(Graphics g) {
            for (Enumeration en = hashLines.elements(); en.hasMoreElements();) {
                Vector v = (Vector) en.nextElement();
                for (Enumeration e = v.elements(); e.hasMoreElements();) {
                    ColoredLine line = (ColoredLine) e.nextElement();
                    g.setColor(line.getColor());
                    g.drawLine((int) line.x1, (int) line.y1, (int) line.x2, (int) line.y2);
                }
            }
            super.paint(g);
        }

        /**
         * Gets the image by name.
         * First checks if cache contains this image.
         *
         * @param   name   an image name.
         * @return  the image
         */
        public Image getImage(String name) {
            Image img = null;
            if (cache != null) {
                if ((img = (Image) cache.get(name)) != null) {
                    return img;
                }
            }
            java.net.URL url = getClass().getClassLoader().getResource("com/azoft/nusuth/gui/" + name);
            img = Toolkit.getDefaultToolkit().createImage(url);

            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(img, 0);
            try {
                tracker.waitForID(0);
                if (tracker.isErrorAny()) {
                    System.out.println("Error loading image " + name);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (cache != null) {
                cache.put(name, img);
            }
            return img;
        }

        /**
         * Saves the component locations to cluster property.
         *
         * @param   clusterViewProps   the cluster property.
         */
        void saveProperties(Properties clusterViewProps) {
            Component[] comps = getComponents();
            for (int i = 0; i < comps.length; i++) {
                Component nextComp = comps[i];
                if (nextComp instanceof NamedImageComponent) {
                    String type = ((NamedImageComponent) nextComp).getType();
                    String systemId = ((NamedImageComponent) nextComp).getSystemId();
                    Point p = nextComp.getLocation();
                    clusterViewProps.setProperty(type + "." + systemId + ".x", "" + p.x);
                    clusterViewProps.setProperty(type + "." + systemId + ".y", "" + p.y);
                }
            }
            clusterViewProps.setProperty("paintText", "" + paintText);
        }
    }


    private class ComponentChangingValuesController implements ChangingValuesElementRenderer {
        private boolean distributor;
        private String type;

        public ComponentChangingValuesController(String type) {
            super();
            this.type = type;
            this.distributor = (type.equals(SDISTRIBUTOR));
        }

        public void addItem(String id, String item) {
            if (distributor) {
                dnd.addDistributor(BasicPanel.getDistributorNode(id));
            } else {
                dnd.addContainer(id, item);
            }
        }

        public void changeItem(String id, String oldItem, String newItem) {
            dnd.changeComponentName(this.type, id, newItem);
        }

        public void removeAllItems() {
            dnd.removeAllComponents();
        }

        public void removeItem(String id, String item) {
            dnd.removeComponent(type, id);
        }

        public void addActionListener(ActionListener al) {
        }

        public JComponent getComponent() {
            return null;
        }

        public String getValue() {
            return null;
        }

        /**
         * Gets the renderer content is empty or not.
         *
         * @return  <code>true</code> if the renderer content is empty;
         * <code>false</code> otherwise.
         * @see #getPar()
         */
        public boolean isContentEmpty() {
            return false;
        }

        public void removeActionListener(ActionListener al) {
        }

        public void setValue(String value) {
        }

        public boolean takesAllPlace() {
            return false;
        }
    }

}
