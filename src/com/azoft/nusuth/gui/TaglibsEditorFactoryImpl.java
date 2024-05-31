/*
 * @(#)TaglibsEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class TaglibsEditorFactoryImpl creates a new taglibs editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TaglibsEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new taglibs editor panel.
     * @return a new taglibs editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new TaglibsEditorPanel();
    }
}
