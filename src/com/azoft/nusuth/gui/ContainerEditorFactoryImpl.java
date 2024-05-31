/*
 * @(#)ContainerEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class ContainerEditorFactoryImpl creates a new container editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ContainerEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new container editor panel.
     * @return a new container editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new ContainerEditorPanel();
    }
}
