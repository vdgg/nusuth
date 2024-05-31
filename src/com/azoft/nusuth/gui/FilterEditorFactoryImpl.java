/*
 * @(#)FilterEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class FilterEditorFactoryImpl creates a new filter editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class FilterEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new filter editor panel.
     * @return a new filter editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new FilterEditorPanel();
    }
}
