/*
 * @(#)SecurityConfigEditorFactoryImpl.java 1.0 10/02/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class SecurityConfigEditorFactoryImpl creates
 * a new security config editor panel.
 *
 * @version 1.0 10/02/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class SecurityConfigEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new security config editor panel.
     * @return a new security config editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new SecurityConfigEditorPanel();
    }
}
