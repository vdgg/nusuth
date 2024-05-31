/*
 * @(#)WebJndiEditorPanel.java 1.0 09/13/2001
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
 * Class WebJndiEditorPanel.
 *
 * @version 1.0 09/13/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class WebJndiEditorPanel extends CustomEditorPanel {

    /**
     * The web jndi node name.
     */
    public final static String WEB_JNDI_NODE_NAME = "jndi";

    /**
     * The env entry element name.
     */
    public final static String ENV_ENTRY_NAME = "env-entry";

    /**
     * The ejb ref element name.
     */
    public final static String EJB_REF_NAME = "ejb-ref";

    /**
     * The resource ref element name.
     */
    public final static String RES_REF_NAME = "resource-ref";

    /**
     * The resource env ref element name.
     */
    public final static String RES_ENV_REF_NAME = "resource-env-ref";

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS = {ENV_ENTRY_NAME, EJB_REF_NAME,
                                             RES_REF_NAME, RES_ENV_REF_NAME};


    static {
        USED_COMPOSITE_CHILDS = new Vector();
        int cnt = BASICS_FACS.length;
        for (int i = 0; i < cnt; i++) {
            USED_COMPOSITE_CHILDS.addElement(BASICS_FACS[i]);
        }
        ELEMENT_TYPE = BasicPanel.SWEB_APP;
    }


    /**
     * Constructs a new web security editor panel.
     * It's the application editor with web security type.
     */
    public WebJndiEditorPanel() {
        super(BasicPanel.SWEBJNDI);
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
        return WEB_JNDI_NODE_NAME;
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
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(7, 7, 4, 7);
            simplePanel.add(getEnvEntryPanel(), c);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets(7, 0, 4, 7);
            simplePanel.add(getResEnvRefPanel(), c);

            c.insets = new Insets(0, 7, 4, 7);
            simplePanel.add(getEjbRefPanel(), c);

            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getResRefPanel(), c);
        }
        return simplePanel;
    }

    /**
     * Env entry panel.
     *
     * @return  the env entry panel
     */
    private JPanel getEnvEntryPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Environment entries"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(ENV_ENTRY_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }

    /**
     * Res env ref panel.
     *
     * @return  the res env ref panel
     */
    private JPanel getResEnvRefPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Resource environment references"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(RES_ENV_REF_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }

    /**
     * Ejb ref panel.
     *
     * @return  the ejb ref panel
     */
    private JPanel getEjbRefPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Ejb references"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(EJB_REF_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }

    /**
     * Res ref panel.
     *
     * @return  the res ref panel
     */
    private JPanel getResRefPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Resource references"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory fac =
                (TablePanelFactory) getCompositePanelFactory(RES_REF_NAME);
        addTableToFocused(fac.getTable());
        res.add(fac.getMainPane(), c);
        return res;
    }
}
