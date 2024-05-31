/*
 * @(#)DistributorEditorFactoryImpl.java 1.0 09/25/2001
 */

package com.azoft.nusuth.gui;

/**
 * Class DistributorEditorFactoryImpl creates a new distributor editor panel.
 *
 * @version 1.0 09/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class DistributorEditorFactoryImpl implements EditorFactory {

    /**
     * Creates a new distributor editor panel.
     * @return a new distributor editor panel.
     */
    public EditorPanel createEditorPanel() {
        return new DistributorEditorPanel();
    }
}
