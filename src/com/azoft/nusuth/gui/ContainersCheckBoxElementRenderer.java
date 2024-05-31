/*
 * @(#)ContainersCheckBoxElementRenderer.java 1.0 03/28/2001
 */

package com.azoft.nusuth.gui;

import java.util.Vector;
import javax.swing.*;

/**
 * Class ContainersCheckBoxElementRenderer is a renderer for containers.
 * It implements not only ElementRenderer interface, but
 * ChangingValuesElementRenderer yet.
 *
 * @version 1.0 03/28/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ContainersCheckBoxElementRenderer extends CheckBoxElementRenderer
        implements ChangingValuesElementRenderer, Activating {


    /**
     * Constructs a new container checkbox element renderer.
     */
    public ContainersCheckBoxElementRenderer() {
        super();
    }


// Methods form Activating

    /**
     * Try to enable/disable this renderer's component.
     * If it is impossible, returns false; else enables/disables component &
     * returns true.
     *
     * @param b   enable or disable
     * @return  <code>true</code> if enable/disable is possible;
     * <code>false</code> otherwise.
     */
    public boolean tryToSetEnabled(boolean b) {
        if (!b && getSelectedCheckCount() > 1) {
            return false;
        }
        panel.setEnabled(b);
        return true;
    }

    /**
     * Shows the error message.
     */
    public void showErrorMessage() {
        JOptionPane.showMessageDialog(getComponent(),
                "Non - disrtibutable application can not be run in most than one container!",
                " Warning", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Enables or disables this renderer.
     *
     * @param   b   the boolean value.
     */
    public void setEnabled(boolean b) {
        getComponent().setEnabled(b);
    }

    /**
     * Gets the selected checkboxes count.
     *
     * @return  the selected checkboxes count.
     */
    private int getSelectedCheckCount() {
        int res = 0;
        for (int i = 0; i < checkboxes.size(); i++) {
            JCheckBox check = (JCheckBox) checkboxes.elementAt(i);
            if (check.isSelected()) {
                res++;
            }
        }
        return res;
    }

// Methods from ChangingValuesElementRenderer interface

    /**
     * Adds the specified item with the specified id.
     *
     * @param id    the id of the item
     * @param item  the specified item.
     * @see #removeItem(String, String)
     * @see #changeItem(String, String, String)
     */
    public void addItem(String id, String containerName) {
        JCheckBox check = addCheckBox(containerName);
        check.addActionListener(actionListener);
    }

    /**
     * Removes the specified item with the specified id.
     *
     * @param id    the id of the item
     * @param item  the specified item.
     * @see #addItem(String, String)
     * @see #changeItem(String, String, String)
     */
    public void removeItem(String id, String containerName) {
        removeCheckBox(containerName);
    }

    /**
     * Removes all items.
     *
     * @see #removeItem(String, String)
     */
    public void removeAllItems() {
        removeAllCheckBox();
    }

    /**
     * Changes the specified old item with the specified id
     * by the new item.
     *
     * @param id    the id of the item
     * @param oldItem  the specified old item.
     * @param newItem  the specified new item.
     * @see #addItem(String, String)
     * @see #removeItem(String, String)
     */
    public void changeItem(String id, String oldName, String newName) {
        JCheckBox check = getCheckBox(oldName);
        if (check != null) {
            check.setText(newName);
        }
    }
}