/*
 * @(#)AppDepEditorPanel.java 1.0 09/26/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

/**
 * Class AppDepEditorPanel.
 *
 * @version 1.0 09/26/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class AppDepEditorPanel extends DefaultEditorPanel {

    /**
     * There is not panel in rigth split pane part for this editor panel.
     * It used label instead.
     */
    private JLabel label = new JLabel();


    /**
     * Constructs a new hosts editor panel.
     */
    public AppDepEditorPanel() {
        super(BasicPanel.SAPP_DEP);
    }

    /**
     * Gets the display string of this editor.
     *
     * @return  the display string of this editor.
     * @see #getDisplay(path, CompositeNusuthWebAppElement)
     */
    public String getDisplay() {
        return "";
    }

    /**
     * Gets the main component (panel or tabbed pane or something else).
     * In this panel it is a label.
     *
     * @return  the main component
     */
    public JComponent getMainComponent() {
        return label;
    }
}
