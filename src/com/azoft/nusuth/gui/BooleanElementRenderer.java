package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.AWTEventMulticaster;
import java.util.*;

public class BooleanElementRenderer implements ElementRenderer {
    private JComboBox comboBox;
    private ActionListener actionListener;
    private int lastIndex = 0;

    public BooleanElementRenderer() {
        comboBox = new JComboBox();
        comboBox.addItem("true");
        comboBox.addItem("false");
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = comboBox.getSelectedIndex();
                if (selectedIndex != lastIndex) {
                    fireActionPerformed();
                    lastIndex = selectedIndex;
                }
            }
        });
    }

    public void setValue(String value) {
        comboBox.setSelectedIndex(value.equals("true") ? 0 : 1);
    }

    public String getValue() {
        return (String) comboBox.getSelectedItem();
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

    public JComponent getComponent() {
        return comboBox;
    }

    public boolean takesAllPlace() {
        return false;
    }

    public void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
    }

    public void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    private void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }
}