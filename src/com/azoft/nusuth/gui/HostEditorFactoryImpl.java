/*
 * @(#)HostEditorFactoryImpl.java 1.0 09/26/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class HostEditorFactoryImpl creates a new host editor panel.
 *
 * @version 1.0 09/26/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class HostEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new host editor panel.
     * @return a new host editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new HostEditorPanel();
    }
}
