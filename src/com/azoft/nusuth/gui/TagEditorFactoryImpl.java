/*
 * @(#)TagEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class TagEditorFactoryImpl creates a new tag editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TagEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new tag editor panel.
     * @return a new tag editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new TagEditorPanel();
    }
}
