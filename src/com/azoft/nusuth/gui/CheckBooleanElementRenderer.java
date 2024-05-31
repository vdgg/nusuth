/*
 * @(#)CheckBooleanElementRenderer.java 1.0 08/21/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.AWTEventMulticaster;
import java.util.*;

/**
 * Class CheckBooleanElementRenderer is the renderer for boolean values.
 * It looks like checkbox.
 *
 * @version 1.0 08/21/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class CheckBooleanElementRenderer implements ElementRenderer, Activator {

    /**
     * the checkbox
     */
    private JCheckBox check;

    /**
     * the action listener
     */
    private ActionListener actionListener;

    /**
     * Activating element renderer, on which affects this renderer's checkbox
     */
    private Activating activatingRenderer = null;


    /**
     * Constructs a new check boolean element renderer
     * Creates the checkbox & adds necessary listener.
     */
    public CheckBooleanElementRenderer() {
        check = new JCheckBox();
        check.addActionListener(new ActionListener() {

            // before firing it checks the activating state
            // if activating element is bad - it changes check state
            public void actionPerformed(ActionEvent e) {
                if (!checkActivating()) {
                    showActivatingErrorMessage();
                    if (!((JCheckBox) e.getSource()).isSelected()) {
                        ((JCheckBox) e.getSource()).setSelected(true);
                    }
                } else {
                    fireActionPerformed();
                }
            }
        });
    }

// Methods from ElementRenderer interface

    /**
     * Sets a new value to this renderer.
     *
     * @param value   the specified value.
     * @see #getValue()
     */
    public void setValue(String value) {
        check.setSelected(value.equals("true"));
        if (!checkActivating()) {
            activatingRenderer.setEnabled(check.isSelected());
//      activatingRenderer.getComponent().setEnabled(check.isSelected());
        }
    }

    /**
     * Gets the value of this renderer.
     *
     * @return the value of this renderer.
     * @see #setValue(String)
     */
    public String getValue() {
        return (check.isSelected()) ? "true" : "false";
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

    /**
     * Gets the component for this renderer.
     *
     * @return the component for this renderer.
     */
    public JComponent getComponent() {
        return check;
    }

    /**
     * Gets the component occupies all horizontal place or not.
     *
     * @return  <code>true</code> if the component occupies all horizontal place;
     * <code>false</code> otherwise.
     */
    public boolean takesAllPlace() {
        return false;
    }

    /**
     * Adds the specified action listener.
     *
     * @param al    the specified action listener.
     * @see #removeActionListener(ActionListener)
     */
    public void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
    }

    /**
     * Removes the specified action listener.
     *
     * @param al    the specified action listener.
     * @see #addActionListener(ActionListener)
     */
    public void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }


    /**
     * Calls the actionPerformed method in all action listeners.
     */
    private void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

// Methods from Activator

    /**
     * Sets the specified activating renderer.
     *
     * @param renderer    the specified renderer.
     */
    public void addActivating(Activating renderer) {
        activatingRenderer = renderer;
    }

    /**
     * Gets the activating renderer (if exist) can be enabled/disabled or not.
     * If it can - it is enabled/disabled.
     *
     * @return  <code>true</code> if activating renderer is null or
     * activating renderer can be enabled/disabled;
     * <code>false</code> otherwise.
     */
    public boolean checkActivating() {
        if (activatingRenderer != null) {
            return activatingRenderer.tryToSetEnabled(check.isSelected());
        }
        return true;
    }

    /**
     * Shows the activating renderer error message.
     */
    public void showActivatingErrorMessage() {
        if (activatingRenderer != null) {
            activatingRenderer.showErrorMessage();
        }
    }
}
