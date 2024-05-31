/*
 * @(#)TaglibEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class TaglibEditorFactoryImpl creates a new taglib editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TaglibEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new taglib editor panel.
     * @return a new taglib editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new TaglibEditorPanel();
    }
}
