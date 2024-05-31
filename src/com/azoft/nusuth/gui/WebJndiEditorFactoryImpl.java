/*
 * @(#)WebJndiEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class WebJndiEditorFactoryImpl creates a new web jndi editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class WebJndiEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new web jndi editor panel.
     * @return a new web jndi editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new WebJndiEditorPanel();
    }
}
