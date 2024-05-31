/*
 * @(#)ElementRenderer.java 1.0 02/25/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.JComponent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;

/**
 * Interface ElementRenderer defines common renderers behavior.
 *
 * @version 1.0 02/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public interface ElementRenderer {

    /**
     * Sets a new value to this renderer.
     *
     * @param value   the specified value.
     * @see #getValue()
     */
    public void setValue(String value);

    /**
     * Gets the value of this renderer.
     *
     * @return the value of this renderer.
     * @see #setValue(String)
     */
    public String getValue();

    /**
     * Gets the renderer content is empty or not.
     *
     * @return  <code>true</code> if the renderer content is empty;
     * <code>false</code> otherwise.
     * @see #getPar()
     */
    public boolean isContentEmpty();

    /**
     * Gets the component for this renderer.
     *
     * @return the component for this renderer.
     */
    public JComponent getComponent();

    /**
     * Gets the component occupies all horizontal place or not.
     *
     * @return  <code>true</code> if the component occupies all horizontal place;
     * <code>false</code> otherwise.
     */
    public boolean takesAllPlace();

    /**
     * Adds the specified action listener.
     *
     * @param al    the specified action listener.
     * @see #removeActionListener(ActionListener)
     */
    public void addActionListener(ActionListener al);

    /**
     * Removes the specified action listener.
     *
     * @param al    the specified action listener.
     * @see #addActionListener(ActionListener)
     */
    public void removeActionListener(ActionListener al);
}