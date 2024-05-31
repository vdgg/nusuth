/*
 * @(#)Activator.java 1.0 09/17/2001
 */

package com.azoft.nusuth.gui;

/**
 * Interface Activator.
 *
 * @version 1.0 09/17/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public interface Activator {

    /**
     * Adds the specified activating renderer.
     *
     * @param renderer    the specified renderer.
     */
    void addActivating(Activating renderer);

    /**
     * Gets the activating renderer (if exist) can be enabled/disabled or not.
     * If it can - it is enabled/disabled.
     *
     * @return  <code>true</code> if activating renderer is null or
     * activating renderer can be enabled/disabled;
     * <code>false</code> otherwise.
     */
    boolean checkActivating();

    /**
     * Shows the activating renderer error message.
     */
    void showActivatingErrorMessage();
}
