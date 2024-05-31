/*
 * @(#)Activating.java 1.0 09/17/2001
 */

package com.azoft.nusuth.gui;

/**
 * Interface Activating.
 *
 * @version 1.0 09/17/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public interface Activating {

    /**
     * Try to enable/disable this renderer's component.
     * If it is impossible, returns false; else enables/disables component &
     * returns true.
     *
     * @param b   enable or disable
     * @return  <code>true</code> if enable/disable is possible;
     * <code>false</code> otherwise.
     */
    boolean tryToSetEnabled(boolean b);

    /**
     * Enables or disables this renderer.
     *
     * @param   b   the boolean value.
     */
    void setEnabled(boolean b);

    /**
     * Shows the error message.
     */
    void showErrorMessage();
}
