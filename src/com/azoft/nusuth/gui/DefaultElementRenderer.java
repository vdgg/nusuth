package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.event.*;
import java.awt.AWTEventMulticaster;

public class DefaultElementRenderer implements ElementRenderer, Activating {

    /**
     * The main text field.
     */
    private JTextField textField;

    /**
     * The label for this renderer.
     */
    private JComponent label;

    /**
     * The action listener.
     */
    private ActionListener actionListener;

    /**
     * The password renderer.
     */
    private PasswordElementRenderer passwordRenderer;


    /**
     * Constructs a new default element renderer.
     * It looks like a text field.
     */
    public DefaultElementRenderer() {
        textField = new JTextField();
    }

    /**
     * Sets the specified value to the text field.
     *
     * @param   value   the specified value
     * @see #getValue()
     */
    public void setValue(String value) {
        textField.setText(value);
        if (this.passwordRenderer != null)
            firePasswordEnabled(value.equals(ManageTool.getUserName()) || BasicPanel.userIsAdmin());
    }

    /**
     * Gets the value from the text field.
     *
     * @return  the value from the text field.
     * @see #setValue(String)
     */
    public String getValue() {
        return textField.getText();
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

    /**
     * Gets the component for this renderer.
     *
     * @return the component for this renderer.
     */
    public JComponent getComponent() {
        return textField;
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
    public void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                fireActionPerformed();
            }

            public void removeUpdate(DocumentEvent e) {
                fireActionPerformed();
            }

            public void changedUpdate(DocumentEvent e) {
                fireActionPerformed();
            }
        });
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
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    /**
     * Sets the specified password element renderer.
     *
     * @param   renderer    the specified password element renderer.
     */
    public void setPasswordEnabledListener(PasswordElementRenderer renderer) {
        this.passwordRenderer = renderer;
    }

    /**
     * Calls the passwordEnabled method in all password enabled listeners.
     */
    private void firePasswordEnabled(boolean b) {
        passwordRenderer.passwordEnabled(b);
    }

// Methods from the Activating.

    /**
     * Try to enable/disable this renderer's component.
     * This realization returns the <code>true</code> value always.
     *
     * @param b   enable or disable
     * @return  <code>true</code> if enable/disable is possible;
     * <code>false</code> otherwise.
     */
    public boolean tryToSetEnabled(boolean b) {
        setEnabled(b);
        return true;
    }

    /**
     * Enables or disables this renderer.
     *
     * @param   b   the boolean value.
     */
    public void setEnabled(boolean b) {
        getComponent().setEnabled(b);
        if (getRendererLabel() != null) {
            getRendererLabel().setEnabled(b);
        }
    }

    /**
     * Shows the error message.
     */
    public void showErrorMessage() {
    }

    /**
     * Sets the label for this renderer.
     *
     * @param   label   the label for this renderer
     * @see #getRendererLabel()
     */
    public void setRendererLabel(JComponent label) {
        this.label = label;
    }

    /**
     * Gets this renderer's label.
     *
     * @return  this renderer's label
     * @see #setRendererLabel(JLabel)
     */
    public JComponent getRendererLabel() {
        return this.label;
    }
}