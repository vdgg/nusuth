/*
 * @(#)AppDepEditorFactoryImpl.java 1.0 09/26/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class AppDepEditorFactoryImpl creates a new hosts editor panel.
 *
 * @version 1.0 09/26/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class AppDepEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new hosts editor panel.
     * @return a new hosts editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new AppDepEditorPanel();
    }
}
