/*
 * @(#)CompositeCellRenderer.java 1.0 04/06/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Class CompositeCellRenderer is the cell renderer for table in table panel factory.
 * It can consist of check box / radio button, label & button '...'.
 *
 * @version 1.0 04/06/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class CompositeCellRenderer extends DefaultTableCellRenderer {

    /**
     * the check box
     */
    private JCheckBox checkBox = null;

    /**
     * Defines weither this editor looks like one checkBox.
     * The composite object in case of true will have values:
     * selected, "true"/"false"
     */
    private boolean booleanValue = false;

    /**
     * the radio button
     */
    private JRadioButton radio = null;

    /**
     * the button '...' or other
     */
    private JButton button = null;

    /**
     * the main panel
     */
    private JPanel panel;

    /**
     * the border for on_focus view
     */
    private LineBorder focusBorder = new LineBorder(Color.black, 2);

    /**
     * Defines this renderer contains a label or not.
     * The default value is true.
     */
    private boolean withLabel = true;


    /**
     * Constructs a new composite cell renderer.
     * It consists of label.
     */
    public CompositeCellRenderer() {
        // label
        super();
        createPanel();
    }

    /**
     * Constructs a new composite cell renderer with the specified radio button.
     * It will look as radio + label.
     *
     * @param   radio   the specified radio button.
     */
    public CompositeCellRenderer(JRadioButton radio) {
        super();
        this.radio = radio;
        createPanel();
    }

    /**
     * Constructs a new composite cell renderer with the specified
     * button & radio button. The withLabel value defines
     * this renderer contains a label or not.
     * It will look as radio + button.
     *
     * @param   button      the specified button.
     * @param   radio       the specified radio button.
     * @param   withLabel   defines this renderer contains a label or not.
     */
    public CompositeCellRenderer(JButton button, JRadioButton radio,
                                 boolean withLabel) {
        super();
        this.button = button;
        this.radio = radio;
        this.withLabel = withLabel;
        createPanel();
    }

    /**
     * Constructs a new composite cell renderer with the specified radio checkBox.
     * It will look as checkbox + label.
     *
     * @param   checkBox    the specified check box.
     */
    public CompositeCellRenderer(JCheckBox checkBox) {
        super();
        this.checkBox = checkBox;
        this.booleanValue = true;
        this.withLabel = false;
        createPanel();
    }

    /**
     * Constructs a new composite cell renderer with the specified button.
     * It will look as label + button "...".
     *
     * @param   button  the specified button.
     */
    public CompositeCellRenderer(JButton button) {
        this(button, true);
    }

    /**
     * Constructs a new composite cell renderer with the specified button.
     * It will look as label + button "...". The withLabel value defines
     * this renderer contains a label or not.
     *
     * @param   button  the specified button.
     * @param   withLabel   defines this renderer contains a label or not.
     */
    public CompositeCellRenderer(JButton button, boolean withLabel) {
        super();
        this.button = button;
        this.withLabel = withLabel;
        createPanel();
    }

    /**
     * Constructs a new composite cell renderer with the specified checkBox & button.
     * It will look as check + label + button "...".
     *
     * @param   checkBox    the specified checkBox.
     * @param   button      the specified button.
     */
    public CompositeCellRenderer(final JCheckBox checkBox, JButton button) {
        super();
        this.checkBox = checkBox;
        this.button = button;
        createPanel();
    }


    /**
     * Creates the panel. It paints its border after painting all.
     * Adds all necessary components.
     */
    private void createPanel() {
        GridBagLayout gridbag = new GridBagLayout();
        panel = new JPanel(gridbag) {
            public void paint(Graphics g) {
                super.paint(g);
                paintBorder(g);
            }
        };
        GridBagConstraints c = new GridBagConstraints();

        c.gridwidth = (button != null) ? 1 :
                (booleanValue) ? GridBagConstraints.REMAINDER
                : GridBagConstraints.RELATIVE;
//    c.gridwidth = (button != null) ? 1 : GridBagConstraints.RELATIVE;
        c.weightx = 0.0;
        if (checkBox != null) {
            panel.add(checkBox, c);
        } else if (radio != null) {
            panel.add(radio, c);
        }

        c.gridwidth = (button != null)
                ? GridBagConstraints.RELATIVE : GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        if (withLabel) {
            panel.add(this, c);
        } else if (!booleanValue) {
            JPanel p = new JPanel();
            p.setBackground(Color.white);
            panel.add(p, c);
        }

        if (button != null) {
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 0.0;
            c.fill = GridBagConstraints.NONE;
            panel.add(button, c);
        }
    }

    /**
     * Override this super method.
     * Sets the background in other way, a new borders.
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {

        if (isSelected) {
            super.setForeground(table.getSelectionForeground());
            setBackground(table.getSelectionBackground());
        } else {
            super.setForeground(table.getForeground());
            setBackground(table.getBackground());
        }
        setFont(table.getFont());
        if (hasFocus) {
            panel.setBorder(focusBorder);
            if (table.isCellEditable(row, column)) {
                super.setForeground(UIManager.getColor("Table.focusCellForeground"));
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        } else {
            panel.setBorder(noFocusBorder);
        }
        setValue(value);
        Color back = getBackground();
        boolean colorMatch = (back != null) && (back.equals(table.getBackground())) && table.isOpaque();
        setOpaque(!colorMatch);

        return panel;
    }

    /**
     * Sets the specified value.
     * Work with the composite object.
     *
     * @param   value   the specified value.
     */
    protected void setValue(Object value) {
        boolean selected = true;
        Object cvalue = "";
        if (value instanceof CompositeObject) {
            selected = ((CompositeObject) value).getSelected();
            cvalue = ((CompositeObject) value).getValue();
            cvalue = (cvalue == null) ? cvalue = "" : cvalue;
        }
        if (checkBox != null) {
            if (booleanValue) {
                checkBox.setSelected(cvalue.equals("true") || cvalue.equals("yes"));
            } else {
                checkBox.setSelected(selected);
            }
        } else if (radio != null) {
            radio.setSelected(selected);
        }
        if (checkBox != null || radio != null) {
            setComponentsEnabled(selected);
        }
        super.setValue(cvalue);
    }

    /**
     * Sets the specified background to all components exept button.
     *
     * @param   c   the specified background.
     */
    public void setBackground(Color c) {
        if (panel != null)
            panel.setBackground(c);
        super.setBackground(c);
        if (checkBox != null)
            checkBox.setBackground(c);
        if (radio != null)
            radio.setBackground(c);
    }

    /**
     * Enables or disables components.
     */
    private void setComponentsEnabled(boolean b) {
        super.setEnabled(b);
        if (button != null)
            button.setEnabled(b);
    }

    /**
     * Gets the radio button.
     *
     * @return  the radio button.
     */
    public JRadioButton getRadio() {
        return radio;
    }

    /**
     * Sets the specified text to this (i.e. to label).
     *
     * @param   the specified text.
     */
    public void setText(String text) {
        super.setText(text);
    }
}