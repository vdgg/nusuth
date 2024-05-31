/*
 * @(#)ApplicationEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class ApplicationEditorFactoryImpl creates a new application editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ApplicationEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new application editor panel.
     * @return a new application editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new ApplicationEditorPanel();
    }
}
