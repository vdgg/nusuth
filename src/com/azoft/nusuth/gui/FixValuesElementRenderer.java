package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.AWTEventMulticaster;
import java.util.*;

public class FixValuesElementRenderer implements ChangingValuesElementRenderer {
    protected JComboBox comboBox;
    private ActionListener actionListener;
    private int lastIndex = 0;
    private boolean adddel = false;
    private TabNameChangedListener tabNameChangedListener;

    public FixValuesElementRenderer() {
        this(null);
    }

    public FixValuesElementRenderer(String[] items) {
        comboBox = new JComboBox();
        for (int i = 0; items != null && i < items.length; i++) {
            comboBox.addItem(items[i]);
        }
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (adddel) return;
                fireActionNameChangedPerformed();
                int selectedIndex = comboBox.getSelectedIndex();
                if (selectedIndex != lastIndex) {
                    fireActionPerformed();
                    lastIndex = selectedIndex;
                }
            }
        });
    }

    public void addItem(String id, String item) {
        adddel = true;
        comboBox.addItem(item);
        adddel = false;
    }

    public void removeItem(String id, String item) {
        adddel = true;
        comboBox.removeItem(item);
        adddel = false;
    }

    public void removeAllItems() {
        adddel = true;
        comboBox.removeAllItems();
        adddel = false;
    }

    private int indexOf(String item) {
        int cnt = comboBox.getItemCount();
        for (int i = 0; i < cnt; i++) {
            if (comboBox.getItemAt(cnt - 1 - i).equals(item))
                return (cnt - 1 - i);
        }
        return -1;
    }

    public void changeItem(String id, String item, String newItem) {
//    System.out.println("change item " + item + " to " + newItem);
        adddel = true;
        int index = indexOf(item);
        if (index != -1) {
            comboBox.removeItemAt(index);
            comboBox.insertItemAt(newItem, index);
        }
        adddel = false;
    }

    public void setValue(String value) {
        comboBox.setSelectedItem(value);
    }

    public String getValue() {
        return (String) comboBox.getSelectedItem();
    }

    public JComponent getComponent() {
        return comboBox;
    }


    /**
     * Gets the renderer content is empty or not.
     *
     * @return  <code>true</code> if the renderer content is empty;
     * <code>false</code> otherwise.
     * @see #getPar()
     */
    public boolean isContentEmpty() {
        return getValue().equals("");
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

    private void fireActionNameChangedPerformed() {
        if (tabNameChangedListener != null)
            tabNameChangedListener.tabNameChanged((String) comboBox.getSelectedItem());
    }

    public void setTabNameChangedListener(TabNameChangedListener l) {
        this.tabNameChangedListener = l;
    }
}
