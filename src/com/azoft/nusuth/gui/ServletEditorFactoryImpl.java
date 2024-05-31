/*
 * @(#)ServletEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class ServletEditorFactoryImpl creates a new servlet editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ServletEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new servlet editor panel.
     * @return a new servlet editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new ServletEditorPanel();
    }
}
