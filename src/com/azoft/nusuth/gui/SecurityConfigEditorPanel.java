/*
 * @(#)SecurityConfigEditorPanel.java 1.0 10/02/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.management.ManagementException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.*;
import java.awt.*;

/**
 * Class SecurityConfigEditorPanel.
 *
 * @version 1.0 10/02/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class SecurityConfigEditorPanel extends CustomEditorPanel {

    /**
     * The web jndi node name.
     */
    public final static String SECURITY_CONFIG_NODE_NAME = "securityConfig";

    /**
     * The resources element name.
     */
    public final static String RESOURCES_NAME = "resources";

    /**
     * The users element name.
     */
    public final static String USERS_NAME = "users";

    /**
     * The roles element name.
     */
    public final static String ROLES_NAME = "roles";

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS = {RESOURCES_NAME, USERS_NAME,
                                             ROLES_NAME};


    static {
        USED_COMPOSITE_CHILDS = new Vector();
        int cnt = BASICS_FACS.length;
        for (int i = 0; i < cnt; i++) {
            USED_COMPOSITE_CHILDS.addElement(BASICS_FACS[i]);
        }
        ELEMENT_TYPE = BasicPanel.SSECURITY;
    }


    /**
     * Constructs a new security config editor panel.
     * It's the custom editor with security config type.
     */
    public SecurityConfigEditorPanel() {
        super(BasicPanel.SSECURITY);
    }


    /**
     * Inits the renderers for simple elements.
     * Overrides the super method to not init
     * (there are no simple elements in this panel).
     */
    protected void initRenderers() {
        if (webElement == null) {
            webElement = createCompositeElement();
        }
    }

    /**
     * Gets the display string for this panel.
     *
     * @return  the display string for this panel.
     */
    public String getDisplay() {
        return SECURITY_CONFIG_NODE_NAME;
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
            c.insets = new Insets(7, 7, 4, 7);
            simplePanel.add(getResourcesPanel(), c);

            c.insets = new Insets(0, 7, 4, 7);
            simplePanel.add(getUsersPanel(), c);

            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getRolesPanel(), c);
        }
        return simplePanel;
    }

    /**
     * Resources panel.
     *
     * @return  the resources panel
     */
    private JPanel getResourcesPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Resources"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        CompositeElementPanelFactory fac = getCompositePanelFactory(RESOURCES_NAME);
        TablePanelFactory tfac =
                (TablePanelFactory) fac.getCompositePanelFactory("resource");
        tfac.setIndividualTab(false);
        addTableToFocused(tfac.getTable());
        res.add(tfac.getMainPane(), c);
        return res;
    }

    /**
     * Users panel.
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
        CompositeElementPanelFactory fac = getCompositePanelFactory(USERS_NAME);
        TablePanelFactory tfac =
                (TablePanelFactory) fac.getCompositePanelFactory("user");
        tfac.setIndividualTab(false);
        addTableToFocused(tfac.getTable());
        res.add(tfac.getMainPane(), c);
        return res;
    }

    /**
     * Roles panel.
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
        c.insets = new Insets(0, 0, 0, 0);
        CompositeElementPanelFactory fac = getCompositePanelFactory(ROLES_NAME);
        ElementRenderer renderer = (ElementRenderer) fac.renderers.get("role");
        if (renderer instanceof TableRenderer) {
            TableRenderer trenderer = (TableRenderer) renderer;
            addTableToFocused(trenderer.getTable());
            res.add(trenderer.getComponent(), c);
        } else {
            System.out.println("the renderer for role not instance of TableRenderer!");
        }
//    addTableFactoryToFocused(fac);
//    res.add(fac.getMainPane(), c);
        return res;
    }
}
