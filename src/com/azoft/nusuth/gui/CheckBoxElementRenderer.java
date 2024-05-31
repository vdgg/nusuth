/*
 * @(#)CheckBoxElementRenderer.java 1.0 03/02/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
import java.awt.*;
import java.util.*;

import com.azoft.nusuth.deployment.*;

/**
 * Class CheckBoxElementRenderer presents a panel with some checkboxes.
 * As all component renderers it implements ElementRenderer interface.
 *
 * @version 1.0 03/02/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class CheckBoxElementRenderer implements ElementRenderer {
    protected JPanel panel;
    protected Vector checkboxes = new Vector();
    protected ActionListener actionListener;


    /**
     * Constructs a new checkbox element renderer.
     */
    public CheckBoxElementRenderer() {
        createPanel();
    }

    /**
     * Constructs a new checkbox element renderer with the
     * specified check names.
     *
     * @param items   the specified array of check names
     */
    public CheckBoxElementRenderer(String[] items) {
        this();
        createCheckBoxes(items);
    }

    /**
     * Constructs a new checkbox element renderer with the
     * specified check names.
     *
     * @param items   the specified vector of check names
     */
    public CheckBoxElementRenderer(Vector items) {
        this();
        createCheckBoxes(items);
    }


    /**
     * Creates a panel with necessary properties.
     */
    protected void createPanel() {
        panel = new JPanel(new FlowLayout(FlowLayout.LEFT)) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                int cnt = getComponentCount();
                for (int i = 0; i < cnt; i++) {
                    this.getComponent(i).setEnabled(enabled);
                }
            }
        };
        panel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                checkSize();
            }
        });
    }

    /**
     * Checks the selected checkboxes count.
     * It used in subclasses.
     */
    protected void checkSelectedCount(boolean enabled) {
    }

    /**
     * Creates checkboxes with the specified names and
     * adds them to the panel.
     *
     * @param items   the specified array of check names
     */
    private void createCheckBoxes(String[] items) {
        for (int i = 0; items != null && i < items.length; i++) {
            addCheckBox(items[i]);
        }
    }

    /**
     * Creates checkboxes with the specified names and
     * adds them to the panel.
     *
     * @param items   the specified vector of check names
     */
    private void createCheckBoxes(Vector items) {
        if (items != null) {
            for (Enumeration e = items.elements(); e.hasMoreElements();) {
                addCheckBox((String) e.nextElement());
            }
        }
    }

    /**
     * Creates a new checkbox with the specified name & puts it in the panel.
     *
     * @param name  the specified name
     */
    protected JCheckBox addCheckBox(String name) {
        JCheckBox check = createNewCheckBox(name);
        check.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                checkSize();
            }
        });
        panel.add(check);
        if (!panel.isEnabled()) {
            check.setEnabled(false);
        }
        checkboxes.addElement(check);
        return check;
    }

    /**
     * Removes checkbox with the specified name.
     *
     * @param name  the specified name.
     */
    protected void removeCheckBox(String name) {
        JCheckBox check = getCheckBox(name);
        if (check != null) {
            panel.remove(check);
            checkboxes.removeElement(check);
        }
    }

    /**
     * Removes all checkboxes.
     */
    protected void removeAllCheckBox() {
        panel.removeAll();
        checkboxes.removeAllElements();
    }

    /**
     * Creates a new checkbox with the specified name.
     *
     * @param name  the specified name
     */
    protected JCheckBox createNewCheckBox(String name) {
        return new JCheckBox(name);
    }

    /**
     * Getss a checkbox with the specified name.
     *
     * @param name  the specified name
     */
    protected JCheckBox getCheckBox(String name) {
        for (int i = 0; i < checkboxes.size(); i++) {
            JCheckBox check = (JCheckBox) checkboxes.elementAt(i);
            if (check.getText().equals(name))
                return check;
        }
        return null;
    }

    /**
     * Sets the necessary size to the panel.
     */
    private void checkSize() {
        if (panel.getParent() != null) {
            setNewMinimum(panel);
            panel.invalidate();
            panel.getParent().doLayout();
        }
    }

    /**
     * Sets the necessary minimum size to the specified panel.
     * Size is calculated from the components size & location.
     *
     * @param p   the specified panel.
     */
    private void setNewMinimum(JPanel p) {
        int count = p.getComponentCount();
        int wid = 0;
        int hei = 0;
        for (int i = 0; i < count; i++) {
            Rectangle rect = p.getComponent(i).getBounds();
            wid = Math.max(rect.x + rect.width, wid);
            hei = Math.max(rect.y + rect.height, hei);
        }
        p.setMinimumSize(new Dimension(wid, hei));
//    p.setPreferredSize(new Dimension(wid, hei));
    }

// Methods from the element renderer interface

    /**
     * Sets a new value to this renderer.
     *
     * @param value   the specified value.
     * @see #getValue()
     */
    public void setValue(String value) {
        System.out.println(
                "this is CheckBoxElementRebderer!!! it requares the enumeration");
    }

    /**
     * Sets a new values to this renderer.
     *
     * @param e   the specified enumeration.
     */
    public void setValues(Enumeration e) {
        // enumeration of the SimpleJBWAEs
        Vector values = new Vector();
        while (e != null && e.hasMoreElements()) {
            values.addElement(
                    ((SimpleNusuthWebAppElement) e.nextElement()).getContent());
        }
        setValues(values);
    }

    /**
     * Sets a new values to this renderer.
     *
     * @param values  the specified vector.
     */
    protected void setValues(Vector values) {
        for (int i = 0; i < checkboxes.size(); i++) {
            JCheckBox check = (JCheckBox) checkboxes.elementAt(i);
            check.setSelected(values.contains(check.getText()));
        }
    }

    /**
     * Gets the value of this renderer.
     *
     * @return the value of this renderer.
     * @see #setValue(String)
     */
    public String getValue() {
        System.out.println(
                "this is CheckBoxElementRebderer! it gives an enumeration");
        return null;
    }

    /**
     * Gets the values from this renderer.
     *
     * @return  the values enumeration.
     */
    public Enumeration getValues() {
        Vector v = new Vector();
        for (int i = 0; i < checkboxes.size(); i++) {
            JCheckBox check = (JCheckBox) checkboxes.elementAt(i);
            if (check.isSelected()) {
                v.addElement(check.getText());
            }
        }
        return v.elements();
    }

    /**
     * Gets the renderer content is empty or not.
     *
     * @return  <code>true</code> if the renderer content is empty;
     * <code>false</code> otherwise.
     * @see #getPar()
     */
    public boolean isContentEmpty() {
        return !getValues().hasMoreElements();
    }

    /**
     * Gets the component for this renderer.
     *
     * @return the component for this renderer.
     */
    public JComponent getComponent() {
        return panel;
    }

    /**
     * Gets the component occupies all horizontal place or not.
     *
     * @return  <code>true</code> if the component occupies all horizontal place;
     * <code>false</code> otherwise.
     */
    public boolean takesAllPlace() {
        return true;
    }

    /**
     * Adds the specified action listener.
     *
     * @param al    the specified action listener.
     * @see #removeActionListener(ActionListener)
     */
    public void addActionListener(ActionListener al) {
        actionListener = AWTEventMulticaster.add(actionListener, al);
        for (int i = 0; i < checkboxes.size(); i++) {
            ((JCheckBox) checkboxes.elementAt(i)).addActionListener(al);
        }
    }

    /**
     * Method from element renderer interface.
     * Removes the specified action listener.
     *
     * @param al    the specified action listener.
     * @see #addActionListener(ActionListener)
     */
    public void removeActionListener(ActionListener al) {
        actionListener = AWTEventMulticaster.remove(actionListener, al);
        for (int i = 0; i < checkboxes.size(); i++) {
            ((JCheckBox) checkboxes.elementAt(i)).removeActionListener(al);
        }
    }

}