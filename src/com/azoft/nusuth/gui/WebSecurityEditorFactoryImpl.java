/*
 * @(#)WebSecurityEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class WebSecurityEditorFactoryImpl creates a new web security editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class WebSecurityEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new web security editor panel.
     * @return a new web security editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new WebSecurityEditorPanel();
    }
}
