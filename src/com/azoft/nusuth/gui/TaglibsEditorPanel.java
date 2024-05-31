/*
 * @(#)TaglibsEditorPanel.java 1.0 09/20/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

/**
 * Class TaglibsEditorPanel.
 *
 * @version 1.0 09/20/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TaglibsEditorPanel extends EmptyEditorPanel
        implements ActionListener {

    /**
     * The popup menu for adding the childs.
     */
    private JPopupMenu menu;

    /**
     * The taglib child name.
     */
    private String childName = "taglib";

    /**
     * The taglib child type.
     */
    private String childType = BasicPanel.SWEBTAGLIB;


    /**
     * Gets the popup menu for this editor.
     *
     * @return  the popup menu for this editor.
     */
    public JPopupMenu getPopupMenu() {
        if (menu == null) {
            menu = new JPopupMenu();
            JMenuItem item = menu.add("add " + childName);
            item.addActionListener(this);
        }
        return menu;
    }

    /**
     * Method from the ActionListener interface.
     * Processes the add/remove element commands.
     */
    public void actionPerformed(ActionEvent e) {
        // add taglib
        if (e.getActionCommand().toLowerCase().startsWith("add ")) {
            if (!basicPanel.canAddItems() || basicPanel.unauthorized) {
                ManageTool.showMessage("You can't add the component");
                return;
            }
            TaglibUserMutableTreeNode taglibsNode = (TaglibUserMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            WebAppMutableTreeNode appNode = (WebAppMutableTreeNode)
                    taglibsNode.getParent();
            Object appObject = appNode.getUserObject();
            if (appObject instanceof Hashtable) {
                CompositeNusuthWebAppElement appElement = (CompositeNusuthWebAppElement)
                        ((Hashtable) appObject).get(BasicPanel.SWEB_APP);
                CompositeNusuthWebAppElement newTaglib =
                        BasicPanel.getCompositeElement(childType);
                if (appElement != null && newTaglib != null) {
                    String componentId = basicPanel.addWebAppTaglib(appElement, newTaglib);
                    TaglibUserMutableTreeNode node =
                            new TaglibUserMutableTreeNode(
                                    componentId, BasicPanel.SWEBTAGLIB);
                    node.setUserObject(newTaglib);
                    node.setElementNode(node);
                    taglibsNode.addTagNodes(node, newTaglib);
                    taglibsNode.add(node);
                    basicPanel.reloadTree();
                }
            }
        }
    }
}
