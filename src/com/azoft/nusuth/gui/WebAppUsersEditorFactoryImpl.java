/*
 * @(#)WebAppUsersEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class WebAppUsersEditorFactoryImpl creates a new web app users editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class WebAppUsersEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new web app users editor panel.
     * @return a new web app users editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new WebAppUsersEditorPanel();
    }
}
