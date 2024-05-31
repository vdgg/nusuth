/*
 * @(#)ControlColoredLine.java 1.0 07/30/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.Properties;

/**
 * Class ControlColoredLine controls the cluster view colours.
 * It's presented by a line with moving edges between colored intervals.
 * You can change interval widths & colors by mouse moving/clicking
 * and through menu - in setting dialog.
 *
 * @version 1.0 07/30/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ControlColoredLine extends JPanel {
    /**
     * Used when cluster view properties don't contain edges.
     */
    static final int[] defaultEdges = {200, 400, 600, 800, 1000};
    /**
     * Used when cluster view properties don't contain colors.
     */
    static final Color[] defaultColors = {Color.red, Color.green, Color.blue, Color.yellow, Color.gray, Color.black};

    Component comp;             // the component for color chooser
    int[] edges;                // the edges array
    int[] widths;               // the widths array
    Color[] colors;             // the colors array
    int maxIndex;               // the maximum index in header
    JTableHeader tableHeader;   // the table header - main component
    NumberPanel numberPanel;    // the number panel - shows the numbers
    boolean fromResize = false; // boolean to not repeat resizing consequence
    float mash;                 // the scale


    /**
     * Constructs a new control colored line with the specified component.
     *
     * @param   comp   the parent component for color chooser
     */
    public ControlColoredLine(Component comp) {
        this(comp, defaultEdges, defaultColors);
    }

    /**
     * Constructs a new control colored line with the specified component,
     * edges array and colors array.
     *
     * @param   comp   the parent component for color chooser
     * @param   edges   the edges of intervals
     * @param   colors   the colors of intervals
     */
    public ControlColoredLine(Component comp, int[] edges, Color[] colors) {
        super();
        this.edges = edges;
        this.maxIndex = edges.length - 1;
        this.widths = new int[edges.length];
        this.colors = colors;
        numberPanel = new NumberPanel();
        initTableHeader();
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(0, 5, 0, 5);
        add(tableHeader, c);
        c.insets = new Insets(5, 5, 0, 5);
        add(numberPanel, c);
    }

    // constructs the table header, adds all listeners
    private void initTableHeader() {
        tableHeader = new JTableHeader() {
            public Dimension getPreferredSize() {
                Dimension ss = super.getPreferredSize();
                return new Dimension(ss.width, (int) (ss.height / 2));
            }

            public Dimension getMinimumSize() {
                Dimension ss = super.getMinimumSize();
                return new Dimension(ss.width, (int) (ss.height / 2));
            }

            public Dimension getMaximumSize() {
                Dimension ss = super.getMaximumSize();
                return new Dimension(ss.width, (int) (ss.height / 2));
            }
        };
        tableHeader.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                rearrangeColumns();
            }
        });
        tableHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                setText(" ");
                if (value instanceof Color) setBackground((Color) value);
                setBorder(UIManager.getBorder("TableHeader.cellBorder"));
                return this;
            }
        });
        tableHeader.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int index = tableHeader.getColumnModel().getColumnIndexAtX(e.getX());
                if (index != -1) {
                    TableColumn tc = tableHeader.getColumnModel().getColumn(index);
                    Object o = tc.getHeaderValue();
                    Color oldColor = (o instanceof Color) ? (Color) o : Color.white;
                    Color c = JColorChooser.showDialog(comp, "Select the color, please", oldColor);
                    if (c != null) {
                        tc.setHeaderValue(c);
                        tableHeader.repaint(0, 0, tableHeader.getWidth(), tableHeader.getHeight());
                        firePropertyColorChange(oldColor, c);
                    }
                } else {
                    Color oldColor = tableHeader.getBackground();
                    Color c = JColorChooser.showDialog(comp, "Select the color, please", oldColor);
                    if (c != null) {
                        tableHeader.setBackground(c);
                        firePropertyColorChange(oldColor, c);
                    }
                }
            }
        });
        int prevV = 0;
        for (int i = 0; i < edges.length; i++) {
            widths[i] = edges[i] - prevV;
            prevV = edges[i];
            TableColumn tc = newColumn(i, widths[i], colors[i]);
            ((DefaultTableColumnModel) tableHeader.getColumnModel()).addColumn(tc);
        }
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);
        tableHeader.setBackground(colors[maxIndex + 1]);
        tableHeader.setSize(new Dimension(400, 40));
    }

    // constructs a new column, adds a component listener for column resizing -
    // when a width of column is changing, the neibor width needed to be changed too.
    private TableColumn newColumn(int ind, int width, Color c) {
        TableColumn tc = new TableColumn(ind, width);
        tc.setHeaderValue(c);
        tc.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("width")) {
                    if (fromResize) {
                        fromResize = false;
                        return;
                    }
                    int oldInt = ((Integer) evt.getOldValue()).intValue();
                    int newInt = ((Integer) evt.getNewValue()).intValue();
                    int index = ((TableColumn) evt.getSource()).getModelIndex();
                    if (index == maxIndex) {
                        // && newInt > oldInt){
                        int oldWidth = tableHeader.getColumnModel().getColumn(index).getWidth();
                        fromResize = true;
                        tableHeader.getColumnModel().getColumn(index).setWidth(oldWidth - (newInt - oldInt));
                        return;
                    }
                    int neborInd = index + 1; //(newInt > oldInt) ? index + 1 : index - 1;
                    int oldWidth = tableHeader.getColumnModel().getColumn(neborInd).getWidth();
                    int newWidth = oldWidth - (newInt - oldInt);  //(newInt > oldInt) ? oldWidth - (newInt - oldInt) : oldWidth + (newInt - oldInt);
                    int minWidth = tableHeader.getColumnModel().getColumn(neborInd).getMinWidth();
                    fromResize = true;
                    if (minWidth >= newWidth) {
                        tableHeader.getColumnModel().getColumn(index).setWidth(tableHeader.getColumnModel().getColumn(index).getWidth() - (newInt - oldInt));
                    } else {
                        tableHeader.getColumnModel().getColumn(neborInd).setWidth(newWidth);
                        Rectangle rect = tableHeader.getHeaderRect(index);
                        int realX = rect.x + rect.width;
                        int newValue = (int) (realX / mash);
                        edges[index] = newValue;
                        widths[index] = newValue - ((index == 0) ? 0 : edges[index - 1]);
                        widths[index + 1] = edges[index + 1] - newValue;
                    }
                    numberPanel.repaint(0, 0, numberPanel.getWidth(), numberPanel.getHeight());
                }
            }
        });
        return tc;
    }

    // calculates a new scale and real column widths. Used when size of component is changed &
    // when point column widths was changed.
    private void rearrangeColumns() {
        int wid = tableHeader.getSize().width;
        int newWidth = wid - (int) (wid / (edges.length + 1));
        mash = ((float) newWidth) / edges[maxIndex];
        int minWidth = tableHeader.getColumnModel().getColumn(0).getMinWidth();
        int minDiff = (int) (minWidth / mash); // cnt ed in min column

        int cnt = tableHeader.getColumnModel().getColumnCount();
        int surplus = 0;
        for (int i = 0; i < cnt; i++) {
            TableColumn tc = tableHeader.getColumnModel().getColumn(i);
            float realW = widths[i] * mash;
            if (widths[i] <= minDiff) {
                surplus += minWidth - realW;
            } else if (surplus > 0) {
                int dW = Math.min((int) ((widths[i] - minDiff) * mash), surplus);
                surplus -= dW;
                realW -= dW;
            }
            fromResize = true;
            tc.setWidth((int) realW);
        }
    }

    /**
     * Saves edges & colors to the specified cluster property.
     *
     * @param   clusterViewProps   the cluter view property.
     */
    void saveProperties(Properties clusterViewProps) {
        String wids = "";
        for (int i = 0; i < edges.length; i++) {
            wids += (i == 0) ? "" + edges[i] : ";" + edges[i];
        }
        clusterViewProps.setProperty("edges.values", wids);
        String colors = "";
        int cnt = tableHeader.getColumnModel().getColumnCount();
        for (int i = 0; i < cnt; i++) {
            String sc = Graph.getColorString(
                    (Color) tableHeader.getColumnModel().getColumn(i).getHeaderValue());
            colors += (i == 0) ? "" + sc : ";" + sc;
        }
        colors += ";" + Graph.getColorString(tableHeader.getBackground());
        clusterViewProps.setProperty("colors.values", colors);
    }

    /**
     * Gets the color by the specified number.
     *
     * @param   intens   the specified number.
     * @return  the color by this number.
     */
    public Color getIntensityColor(int intens) {
        int cnt = tableHeader.getColumnModel().getColumnCount();
        for (int i = 0; i < cnt; i++) {
            if (intens <= edges[i]) {
                return (Color) tableHeader.getColumnModel().getColumn(i).getHeaderValue();
            }
        }
        return tableHeader.getBackground();
    }

    // calls the protected method firePropertyColorChange
    private void firePropertyColorChange(Color oldColor, Color newColor) {
        firePropertyChange("color", oldColor, newColor);
    }

    /**
     * Sets new edges of intervals to this line.
     * It remains a previous colors.
     *
     * @param   newEdges   new edges array
     * @see #setEdges(int[], Color, Color)
     */
    void setEdges(int[] newEdges) {
        setEdges(newEdges, null, null);
    }

    /**
     * Sets new edges & colors to this lines intervals.
     * All colors - it's conversion between from & to colors.
     *
     * @param   newEdges    new edges array
     * @param   cfom        the from color
     * @param   cto         the to color
     * @see #setEdges(int[])
     */
    void setEdges(int[] newEdges, Color cfrom, Color cto) {
        boolean conversion = (cfrom != null && cto != null);
        float[] resfrom = new float[3];
        float[] resto = new float[3];
        if (conversion) {
            Color.RGBtoHSB(cfrom.getRed(), cfrom.getGreen(), cfrom.getBlue(), resfrom);
            Color.RGBtoHSB(cto.getRed(), cto.getGreen(), cto.getBlue(), resto);
        }
        int cnt = tableHeader.getColumnModel().getColumnCount();
        this.edges = newEdges;
        this.maxIndex = edges.length - 1;
        this.widths = new int[edges.length];
        int prevV = 0;
        for (int i = 0; i < edges.length; i++) {
            widths[i] = edges[i] - prevV;
            prevV = edges[i];
            TableColumn tc = null;
            Color c = Color.white;
            if (conversion) {
                float s = i * (resto[0] - resfrom[0]) / edges.length;
                float h = i * (resto[1] - resfrom[1]) / edges.length;
                float b = i * (resto[2] - resfrom[2]) / edges.length;
                c = Color.getHSBColor(resfrom[0] + s, resfrom[1] + h, resfrom[2] + b);
            } else {
                c = (Color) ((DefaultTableColumnModel) tableHeader.getColumnModel()).getColumn(i % cnt).getHeaderValue();
            }
            try {
                tc = ((DefaultTableColumnModel) tableHeader.getColumnModel()).getColumn(i);
                tc.setHeaderValue(c);
            } catch (Exception e) {
            }
            if (tc == null) {
                // old model has less cnt
                tc = newColumn(i, widths[i], c);
                ((DefaultTableColumnModel) tableHeader.getColumnModel()).addColumn(tc);
            }

        }
        Color c = (conversion) ? Color.getHSBColor(resto[0], resto[1], resto[2]) : tableHeader.getBackground();
        tableHeader.setBackground(c);
        int newCnt = ((DefaultTableColumnModel) tableHeader.getColumnModel()).getColumnCount();
        while (newCnt > edges.length) {
            TableColumn tcol = ((DefaultTableColumnModel) tableHeader.getColumnModel()).getColumn(newCnt - 1);
            ((DefaultTableColumnModel) tableHeader.getColumnModel()).removeColumn(tcol);
            newCnt = ((DefaultTableColumnModel) tableHeader.getColumnModel()).getColumnCount();
        }
        rearrangeColumns();
        repaint();
        numberPanel.repaint(0, 0, numberPanel.getWidth(), numberPanel.getHeight());
    }


    /**
     * NumberPanel class paints the interval edges.
     */
    private class NumberPanel extends JComponent {
        int height = 30;
        int shift = 5;

        /**
         * Constructs a new number panel.
         */
        public NumberPanel() {
            super();
        }

        /**
         * Paints the interval edges.
         *
         * @param   g   the graphics context
         */
        public void paint(Graphics g) {
            g.setColor(Color.black);
            FontMetrics fm = g.getFontMetrics();
            int height = fm.getHeight();
            g.drawString("0", 0, height);
            int sum = 0;
            int lastX = fm.stringWidth("0") + shift;
            for (int i = 0; i < edges.length; i++) {
                TableColumn tc = tableHeader.getColumnModel().getColumn(i);
                sum += tc.getWidth();
                int sw = fm.stringWidth("" + edges[i]);
                int x = sum - (sw / 2);
                if (x < lastX) x = lastX;
                g.drawString("" + edges[i], x, height);
                lastX = x + sw + shift;
            }
        }

        /**
         * Gets the preferred size of this component.
         */
        public Dimension getPreferredSize() {
            return new Dimension(getWidth(), getHeight());
        }

        /**
         * Gets the minimum size of this component.
         */
        public Dimension getMinimumSize() {
            return new Dimension(getWidth(), getHeight());
        }

        /**
         * Gets the width of this component.
         */
        public int getWidth() {
            return tableHeader.getSize().width + 20;
        }

        /**
         * Gets the height of this component.
         */
        public int getHeight() {
            return height;
        }
    }

}
