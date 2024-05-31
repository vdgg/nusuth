/*
 * @(#)EditorFactory.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Interface EditorFactory creates a new editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public interface EditorFactory {

    /**
     * Creates a new editor panel.
     * @return EditorPanel
     */
    public EditorPanel createEditorPanel();
}
