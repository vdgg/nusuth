/*
 * @(#)ConfigTreeCellRenderer.java 1.0 02/25/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.Hashtable;

/**
 * Class ConfigTreeCellRenderer is the tree cell renderer for config nodes.
 *
 * @version 1.0 02/25/2001
 * @author  vdgg, tanya
 * @since Nusuth1.0
 */
class ConfigTreeCellRenderer extends DefaultTreeCellRenderer {

    /**
     * All special types.
     */
    private String[] types = {"distributor", "container", //"deployer",
                              BasicPanel.SGROUP, BasicPanel.SUSER};

    /**
     * Contains the icons for all special types.
     */
    private Hashtable icons = new Hashtable();


    /**
     * Constructs a new config tree cell renderer.
     * Loads all icons.
     */
    public ConfigTreeCellRenderer() {
        super();
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            int pindex = type.lastIndexOf(".");
            type = (pindex == -1) ? type : type.substring(pindex + 1);
            java.net.URL url = getClass().getClassLoader().
                    getResource("com/azoft/nusuth/gui/" + type + ".gif");
            ImageIcon icon = new ImageIcon(url);
            icons.put(types[i], icon);
        }
    }

    /**
     * Configures the renderer based on the passed in components.
     * The value is set from messaging the tree with
     * <code>convertValueToText</code>, which ultimately invokes
     * <code>toString</code> on <code>value</code>.
     * The foreground color is set based on the selection and the icon
     * is set based on on leaf and expanded.
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        // There needs to be a way to specify disabled icons.
        if (leaf && value instanceof ConfigMutableTreeNode) {
            String type = ((ConfigMutableTreeNode) value).getType();
            if (!tree.isEnabled()) {
                setDisabledIcon(getLeafIcon(type));
            } else {
                setIcon(getLeafIcon(type));
            }
        }
        return this;
    }

    /**
     * Gets the leaf icon for the specified type.
     *
     * @param   type    the specified type.
     * @return  the leaf icon for the specified type.
     */
    private Icon getLeafIcon(String type) {
        return (Icon) icons.get(type);
    }

    /**
     * Updates all settings.
     */
    public void setUIBackgroundsIcons() {
        setHorizontalAlignment(JLabel.LEFT);

        setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
        setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
        setOpenIcon(UIManager.getIcon("Tree.openIcon"));

        setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
        setTextNonSelectionColor(UIManager.getColor("Tree.textForeground"));
        setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
        setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground"));
        setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));
    }
}

