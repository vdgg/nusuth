/*
 * @(#)EditorPanel.java 1.0 09/10/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.management.ManagementException;

import javax.swing.*;
import java.util.Properties;
import java.util.Enumeration;

/**
 * Interface EditorPanel defines the methods for all editors.
 *
 * @version 1.0 09/10/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public interface EditorPanel {

    /**
     * The summary tab name.
     */
    static final String SUMMARY = "Summary";

    /**
     * The basics tab name.
     */
    static final String BASICS = "Basics";


    /**
     * Gets the type of this panel.
     *
     * @return  the type of this panel.
     */
    String getType();

    /**
     * Gets the tag of this panel.
     *
     * @return  the tag of this panel.
     */
    String getTag();

    /**
     * Gets the main component (panel or tabbed pane or something else).
     *
     * @return  the main component
     */
    JComponent getMainComponent();

    /**
     * Sets the specified entry to this editor.
     *
     * @param   entry   the specified entry
     * @see #getEntry()
     */
    void setEntry(Object entry);

    /**
     * Gets the entry of this editor.
     *
     * @return  the entry of this editor.
     * @see #setEntry(Object)
     */
    Object getEntry();

    /**
     * Gets the web element of this editor.
     *
     * @return  the web element of this editor.
     */
    CompositeNusuthWebAppElement getWebElement();

    /**
     * Updates the editor entry by the controls.
     */
    void updateEntry();

    /**
     * Updates the controls by the editor entry.
     */
    void updateControls();

    /**
     * Gets if this editor panel is saving or not.
     * If it were fields changing, it shows the dialog with 'applay', 'cancel'
     * & 'continue editing'. In case of 'continue editing' returns false.
     *
     * @return  <code>true</code> if the editing panel was saving;
     * <code>false</code> otherwise.
     */
    boolean isSaving();

    /**
     * Gets the popup menu for this editor.
     *
     * @return  the popup menu for this editor.
     */
    JPopupMenu getPopupMenu();

    /**
     * Sets the specified tree node to this editor.
     *
     * @param   node    the specified tree node.
     * @see #getTreeNode()
     */
    void setTreeNode(ConfigMutableTreeNode node);

    /**
     * Gets the current tree node.
     *
     * @return  the current tree node
     * @see #setTreeNode(ConfigMutableTreeNode)
     */
    ConfigMutableTreeNode getTreeNode();

    /**
     * Gets the display of this editor.
     *
     * @return  the display of this editor.
     */
    String getDisplay();

    /**
     * Updates component tree ui of this panel.
     */
    void updateUI();

    /**
     * Uses the specified properties.
     *
     * @param     type  the specified type of this panel.
     * @param     prop  the specified properties, from which data is involved.
     * @see #saveTabsProperties(String, Properties)
     */
//  void useProperties(String type, Properties prop);

    /**
     * Gets the element type_names array for this editor panel.
     *
     * @return  the element type names array.
     */
    String[] getElementNames();

    /**
     * Initializes the saving process.
     */
    void save();

    /**
     * Saves this component to the server via the basic panel proxy.
     */
    void saveComponent() throws UnauthorizedAccessException,
            ManagementException, AccessDeniedException;
}
