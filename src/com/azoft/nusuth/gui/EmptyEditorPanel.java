/*
 * @(#)EmptyEditorPanel.java 1.0 09/20/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.management.ManagementException;

import javax.swing.*;
import java.util.Properties;

/**
 * Class EmptyEditorPanel.
 *
 * @version 1.0 09/20/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class EmptyEditorPanel implements EditorPanel {

    /**
     * The static basic panel.
     */
    protected static BasicPanel basicPanel;

    /**
     * Th empty label.
     */
    private JLabel label = new JLabel();


    /**
     * Constructs a new taglibs editor panel.
     * It's the custom editor with the taglibs type.
     */
    public EmptyEditorPanel() {
        super();
    }


    /**
     * Sets the static basic panel to this editor.
     *
     * @param   basicPanel    the specified basic panel.
     */
    static void setBasicPanel(BasicPanel bp) {
        basicPanel = bp;
    }

    // Methods from the EditorPanel interface.

    public String getDisplay() {
        return null;
    }

    public String[] getElementNames() {
        return new String[0];
    }

    public Object getEntry() {
        return null;
    }

    public JComponent getMainComponent() {
        return label;
    }

    public JPopupMenu getPopupMenu() {
        return null;
    }

    public String getTag() {
        return null;
    }

    public ConfigMutableTreeNode getTreeNode() {
        return null;
    }

    public String getType() {
        return null;
    }

    public CompositeNusuthWebAppElement getWebElement() {
        return null;
    }

    public boolean isSaving() {
        return true;
    }

    public void setEntry(Object entry) {
    }

    public void setTreeNode(ConfigMutableTreeNode node) {
    }

    public void updateControls() {
    }

    public void updateEntry() {
    }

    public void updateUI() {
    }

    public void save() {
    }

    public void saveComponent() throws UnauthorizedAccessException,
            ManagementException, AccessDeniedException {
    }
}
