/*
 * @(#)ServletEditorPanel.java 1.0 09/13/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.management.ManagementException;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.border.TitledBorder;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Class ServletEditorPanel.
 *
 * @version 1.0 09/13/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ServletEditorPanel extends CustomEditorPanel {

    /**
     * The init param element name.
     */
    private final static String INIT_PARAM_NAME = "init-param";

    /**
     * The security role ref element name.
     */
    private final static String SEC_ROLE_REF_NAME = "security-role-ref";

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS =
            {INIT_PARAM_NAME, SEC_ROLE_REF_NAME, "icon"};


    static {
        USED_COMPOSITE_CHILDS = new Vector();
        int cnt = BASICS_FACS.length;
        for (int i = 0; i < cnt; i++) {
            USED_COMPOSITE_CHILDS.addElement(BASICS_FACS[i]);
        }
        ELEMENT_TYPE = BasicPanel.SSERVLET;
    }


    /**
     * Constructs a new servlet editor panel.
     * It's the custom editor with the servlet type.
     */
    public ServletEditorPanel() {
        super(BasicPanel.SSERVLET);
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
            simplePanel.add(getContextParamPanel(), c);
            simplePanel.add(getSecRoleRefPanel(), c);
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
        addElement(this, "servlet-name", true, res, c);
        addElement(this, "servlet-class", true, res, c);
        addElement(this, "jsp-file", true, res, c);
        addElement(this, "load-on-startup", true, res, c);
        c.weighty = 1.0;
        res.add(new JPanel());
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
        addElement(getCompositePanelFactory("icon"), "small-icon", false, res, c);
        addElement(getCompositePanelFactory("icon"), "large-icon", false, res, c);
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
     * Initial params panel.
     *
     * @return  the initial params panel
     */
    private JPanel getContextParamPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Initial parameters"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(INIT_PARAM_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }

    /**
     * Security role refs panel.
     *
     * @return  the sec role refs panel
     */
    private JPanel getSecRoleRefPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Security role references"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(SEC_ROLE_REF_NAME);
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
            JMenuItem item = menu.add("remove servlet");
            item.addActionListener(this);
        }
        return menu;
    }

    /**
     * Method from the ActionListener interface.
     * Processes the add/remove element commands.
     * Only for application menu elements!!!
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("remove servlet")) {
            if (!basicPanel.canDeleteItems() || basicPanel.unauthorized) {
                ManageTool.showMessage("You can't remove the component");
                return;
            }
            WebAppMutableTreeNode servletNode = (WebAppMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            WebAppMutableTreeNode appNode =
                    (WebAppMutableTreeNode) servletNode.getParent().getParent();
            Object appObject = appNode.getUserObject();
            if (appObject instanceof Hashtable) {
                CompositeNusuthWebAppElement appElement = (CompositeNusuthWebAppElement)
                        ((Hashtable) appObject).get(BasicPanel.SWEB_APP);
                if (appElement != null && webElement != null) {
                    try {
                        appElement.removeCompositeChild("servlet", webElement);
                        ((DefaultMutableTreeNode) servletNode.
                                getParent()).remove(servletNode);
                        basicPanel.splitPane.setRightComponent(new JLabel(""));
                        basicPanel.reloadTree();
                        // save component to server (set entry & tree node were made in
                        // checkCurrentNode when tree valueChanged)
                        basicPanel.getEditorPanel(appNode.getType()).save();
                    } catch (DeploymentException de) {
                        System.out.println(de);
                    }
                }
            }
        }
    }
}
