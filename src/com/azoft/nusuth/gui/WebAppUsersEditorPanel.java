/*
 * @(#)WebAppUsersEditorPanel.java 1.0 09/19/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.management.ManagementException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.border.TitledBorder;
import java.util.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Class WebAppUsersEditorPanel.
 *
 * @version 1.0 09/19/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class WebAppUsersEditorPanel extends CustomEditorPanel {

    /**
     * The web users node name.
     */
    public final static String WEB_USERS_NODE_NAME = "users";

    /**
     * The user element name.
     */
    private final static String USER_NAME = "user";

    /**
     * The role element name.
     */
    private final static String ROLE_NAME = "role";

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS =
            {USER_NAME, ROLE_NAME};


    static {
        USED_COMPOSITE_CHILDS = new Vector();
        int cnt = BASICS_FACS.length;
        for (int i = 0; i < cnt; i++) {
            USED_COMPOSITE_CHILDS.addElement(BASICS_FACS[i]);
        }
        ELEMENT_TYPE = BasicPanel.SWEBUSERS;
    }


    /**
     * Constructs a new web app users editor panel.
     * It's the custom editor with the web app users type.
     */
    public WebAppUsersEditorPanel() {
        super(BasicPanel.SWEBUSERS);
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
     */
    public String getDisplay() {
        return WEB_USERS_NODE_NAME;
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
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = 1;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(7, 7, 7, 7);
            simplePanel.add(getUsersPanel(), c);
            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getRolesPanel(), c);
        }
        return simplePanel;
    }

    /**
     * The users panel.
     *
     * @return  the users panel
     */
    private JPanel getUsersPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Users"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(USER_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }

    /**
     * The roles panel.
     *
     * @return  the roles panel
     */
    private JPanel getRolesPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Roles"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(ROLE_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }

    /**
     * Saves this component to the server via the basic panel proxy.
     */
    public void saveComponent() throws UnauthorizedAccessException,
            ManagementException, AccessDeniedException {
        // save the web app users element
        WebAppMutableTreeNode appNode =
                (WebAppMutableTreeNode) getTreeNode().getParent();
        Object appObject = appNode.getUserObject();
        if (appObject instanceof Hashtable) {
            CompositeNusuthWebAppElement appElement = (CompositeNusuthWebAppElement)
                    ((Hashtable) appObject).get(BasicPanel.SWEB_APP);
            basicPanel.setWebAppUsers(appElement, webElement);
        }
    }
}
