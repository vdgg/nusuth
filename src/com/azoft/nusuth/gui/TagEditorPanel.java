/*
 * @(#)TagEditorPanel.java 1.0 09/19/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.ManagementException;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.border.TitledBorder;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Class TagEditorPanel.
 *
 * @version 1.0 09/19/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TagEditorPanel extends CustomEditorPanel {

    /**
     * The variable element name.
     */
    private final static String VARIABLE_NAME = "variable";

    /**
     * The attribute element name.
     */
    private final static String ATTRIBUTE_NAME = "attribute";

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS = {VARIABLE_NAME, ATTRIBUTE_NAME};


    static {
        USED_COMPOSITE_CHILDS = new Vector();
        int cnt = BASICS_FACS.length;
        for (int i = 0; i < cnt; i++) {
            USED_COMPOSITE_CHILDS.addElement(BASICS_FACS[i]);
        }
        ELEMENT_TYPE = BasicPanel.SWEBTAG;
    }


    /**
     * Constructs a new servlet editor panel.
     * It's the custom editor with the servlet type.
     */
    public TagEditorPanel() {
        super(BasicPanel.SWEBTAG);
    }


    /**
     * Creates the composite element for this panel factory.
     *
     * @return  the created composite element
     */
    protected CompositeNusuthWebAppElement createCompositeElement() {
        return createCompositeElement(this.type);
    }

    /**
     * Gets the display string for this panel.
     *
     * @return  the display string for this panel.
     * @see #getDisplay(path, CompositeNusuthWebAppElement)
     */
    public String getDisplay() {
        return getDisplay(getType(), webElement);
    }

    /**
     * Gets the simple panel.
     * This panel is for BASICS tab.
     * Overrides the super methods to return special panel with simple
     * & composite elements.
     *
     * @return  the simple panel
     */
    public JPanel getSimplePanel() {
        if (simplePanel == null) {
            simplePanel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.gridheight = 1;
            c.weightx = 1.0;
            c.weighty = 0.1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(7, 7, 7, 7);
            simplePanel.add(getMainElementPanel(), c);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(7, 0, 7, 7);
            simplePanel.add(getDisplayDescriptionPanel(), c);

            c.weighty = 1;
            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getVariablesPanel(), c);
            simplePanel.add(getAttributesPanel(), c);
        }
        return simplePanel;
    }

    /**
     * Panel for the main simple elements.
     *
     * @return  the panel for main simple elements.
     */
    private JPanel getMainElementPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder(""));
        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);
        addElement(this, "name", true, res, c);
        addElement(this, "tag-class", true, res, c);
        addElement(this, "tei-class", true, res, c);
        addElement(this, "body-content", true, res, c);
        return res;
    }

    /**
     * Second large panel.
     * Contains the display name, icons, description, welcome-files, listeners.
     *
     * @return  the second large panel
     */
    private JPanel getDisplayDescriptionPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        res.add(getDisplayEls(), c);
        res.add(getDescriptionEls(), c);
        res.setBorder(new TitledBorder(""));
        return res;
    }

    /**
     * The display els panel.
     * Contains the display name el & icons.
     *
     * @return  the display els panel
     */
    private JPanel getDisplayEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(this, "display-name", true, res, c);
        addElement(this, "small-icon", true, res, c);
        addElement(this, "large-icon", true, res, c);
        return res;
    }

    /**
     * The description els panel.
     * Contains the description, welcome-files & listeners.
     *
     * @return  the description els panel
     */
    private JPanel getDescriptionEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 3, 5);
        addElement(this, "description", true, res, c, "up");
        return res;
    }

    /**
     * Variables panel.
     *
     * @return  the variables panel
     */
    private JPanel getVariablesPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Variables"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(VARIABLE_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }

    /**
     * Attributes panel.
     *
     * @return  the attributes panel
     */
    private JPanel getAttributesPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Attributes"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(ATTRIBUTE_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }

    /**
     * Gets the popup menu for this editor.
     *
     * @return  the popup menu for this editor.
     */
    public JPopupMenu getPopupMenu() {
        if (menu == null) {
            menu = new JPopupMenu();
            JMenuItem item = menu.add("remove tag");
            item.addActionListener(this);
        }
        return menu;
    }

    /**
     * Method from the ActionListener interface.
     * Processes the add/remove element commands.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("remove tag")) {
            if (!basicPanel.canDeleteItems() || basicPanel.unauthorized) {
                ManageTool.showMessage("You can't remove the component");
                return;
            }
            TaglibUserMutableTreeNode tagNode = (TaglibUserMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            TaglibUserMutableTreeNode taglibNode =
                    (TaglibUserMutableTreeNode) tagNode.getParent();
            CompositeNusuthWebAppElement taglibElement =
                    (CompositeNusuthWebAppElement) taglibNode.getUserObject();
            if (taglibElement != null && webElement != null) {
                try {
                    taglibElement.removeCompositeChild("tag", webElement);
                    ((DefaultMutableTreeNode) tagNode.getParent()).remove(tagNode);
                    basicPanel.splitPane.setRightComponent(new JLabel(""));
                    basicPanel.reloadTree();
                    // save component to server
                    EditorPanel taglibPanel =
                            basicPanel.getEditorPanel(taglibNode.getType());
                    taglibPanel.setTreeNode(taglibNode);
                    taglibPanel.setEntry(taglibElement);
                    taglibPanel.save();
                } catch (DeploymentException de) {
                    JOptionPane.showMessageDialog(getSimplePanel(), de.getLocalizedMessage());
                    System.out.println(de);
                }
            }
        }
    }
}
