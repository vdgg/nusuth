/*
 * @(#)AuthMethodElementRenderer.java 1.0 4/10/2001
 */
package com.azoft.nusuth.gui;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Vector;
import java.util.Enumeration;

/**
 * Class AuthMethodElementRenderer.
 *
 * @version 1.0 4/10/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class AuthMethodElementRenderer extends FixValuesElementRenderer
        implements Activator {

    /**
     * The item from the combobox which enables/disables activating.
     */
    static String enablesValue = "FORM";

    /**
     * The combobox items.
     */
    private static String[] items = {"", "BASIC", "DIGEST", "FORM", "CLIENT-CERT"};

    /**
     * The activating renderers vector.
     */
    private Vector activatingRenderers = new Vector();


    /**
     * Constructs a new AuthMethodElementRenderer.
     * It's the super constructor with the necessary items.
     * Adds the action listener to the combobox for the activating.
     */
    public AuthMethodElementRenderer() {
        super(items);
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkActivating();
            }
        });
    }


// Methods from Activator

    /**
     * Sets the specified activating renderer.
     *
     * @param renderer    the specified renderer.
     */
    public void addActivating(Activating renderer) {
        activatingRenderers.addElement(renderer);
        renderer.setEnabled(false);
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
        int cnt = activatingRenderers.size();
        boolean res = true;
        if (cnt > 0) {
            boolean b = ((String) comboBox.getSelectedItem()).equals(enablesValue);
            Enumeration en = activatingRenderers.elements();
            while (en.hasMoreElements()) {
                Activating activating = (Activating) en.nextElement();
                res = res && activating.tryToSetEnabled(b);
            }
        }
        return res;
    }

    /**
     * Shows the activating renderer error message.
     */
    public void showActivatingErrorMessage() {
        ((Activating) activatingRenderers.elementAt(0)).showErrorMessage();
    }
}