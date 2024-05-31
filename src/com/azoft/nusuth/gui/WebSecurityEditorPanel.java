/*
 * @(#)WebSecurityEditorPanel.java 1.0 09/13/2001
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
 * Class WebSecurityEditorPanel.
 *
 * @version 1.0 09/13/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class WebSecurityEditorPanel extends CustomEditorPanel {

    /**
     * The web security node name.
     */
    public final static String WEB_SECURITY_NODE_NAME = "securitySettings";

    /**
     * The login config element name.
     */
    private final static String LOGIN_CONFIG_NAME = "login-config";

    /**
     * The form login config element name.
     */
    private final static String FORM_LOGIN_CONFIG_NAME = "form-login-config";

    /**
     * The role element name.
     */
    private final static String ROLE_NAME = "security-role";

    /**
     * The constraint element name.
     */
    private final static String CONSTRAINT_NAME = "security-constraint";

    /**
     * Child names in basics tab.
     */
    private static String[] BASICS_FACS = {LOGIN_CONFIG_NAME, ROLE_NAME,
                                           CONSTRAINT_NAME};


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
    public WebSecurityEditorPanel() {
        super(BasicPanel.SWEBSECURITY);
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
        return WEB_SECURITY_NODE_NAME;
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
            simplePanel.add(getLoginConfigPanel(), c);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets(7, 0, 4, 7);
            simplePanel.add(getRolesPanel(), c);

            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getConstraintsPanel(), c);
        }
        return simplePanel;
    }

    /**
     * Panel for login-config elements.
     *
     * @return  the panel for login-config elements.
     */
    private JPanel getLoginConfigPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Login configuration"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        res.add(getAuthEl(), c);
        c.insets = new Insets(0, 15, 0, 0);
        res.add(getFormEls(), c);
        c.insets = new Insets(0, 0, 0, 0);
        res.add(getRealmEl(), c);
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        res.add(new JPanel(), c);
        return res;
    }

    /**
     * The login config auth el panel.
     *
     * @return  the login config auth el panel
     */
    private JPanel getAuthEl() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(getCompositePanelFactory(LOGIN_CONFIG_NAME),
                "auth-method", false, res, c);
        return res;
    }

    /**
     * The login config realm el panel.
     *
     * @return  the login config realm el panel
     */
    private JPanel getRealmEl() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(getCompositePanelFactory(LOGIN_CONFIG_NAME),
                "realm-name", false, res, c);
        return res;
    }

    /**
     * The login config form els panel.
     *
     * @return  the login config form els panel
     */
    private JPanel getFormEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        CompositeElementPanelFactory loginFac =
                getCompositePanelFactory(LOGIN_CONFIG_NAME);
        if (loginFac != null) {
            CompositeElementPanelFactory formloginFac =
                    loginFac.getCompositePanelFactory(FORM_LOGIN_CONFIG_NAME);
            if (formloginFac != null) {
                formloginFac.setIndividualTab(false);
                addElement(formloginFac, "form-login-page", true, res, c);
                addElement(formloginFac, "form-error-page", true, res, c);
            }
            try {
                Activator activator = (Activator) loginFac.renderers.get("auth-method");
                if (activator != null) {
                    Activating loginActivating = (Activating)
                            formloginFac.renderers.get("form-login-page");
                    Activating errorActivating = (Activating)
                            formloginFac.renderers.get("form-error-page");
                    if (loginActivating != null) {
                        activator.addActivating(loginActivating);
                    }
                    if (errorActivating != null) {
                        activator.addActivating(errorActivating);
                    }
                }
            } catch (Exception e) {
            }
        }
/*
    ElementRenderer renderer = (ElementRenderer) renderers.get("distributable");
    if (renderer instanceof CheckBooleanElementRenderer) {
      ElementRenderer activatingRenderer = (ElementRenderer)
              getCompositePanelFactory(contextName).renderers.get("container");
      ((CheckBooleanElementRenderer) renderer).
                  setActivating(activatingRenderer);
    }
*/
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
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory rolesFac =
                (TablePanelFactory) getCompositePanelFactory(ROLE_NAME);
        addTableToFocused(rolesFac.getTable());
        res.add(rolesFac.getMainPane(), c);
        return res;
    }

    /**
     * Constraints panel.
     *
     * @return  the constraints panel
     */
    private JPanel getConstraintsPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Constraints"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory constFac =
                (TablePanelFactory) getCompositePanelFactory(CONSTRAINT_NAME);
        addTableToFocused(constFac.getTable());
        res.add(constFac.getMainPane(), c);
        return res;
    }

    /**
     * Gets the all reqired elements are not empty or not.
     *
     * @return  <code>true</code> if all reqired elements are not empty;
     * <code>false</code> otherwise.
     */
    protected boolean requiredNotEmpty() {
        if (super.requiredNotEmpty()) {
            CompositeElementPanelFactory loginFac =
                    getCompositePanelFactory(LOGIN_CONFIG_NAME);
            if (loginFac != null) {
                ElementRenderer renderer =
                        (ElementRenderer) loginFac.renderers.get("auth-method");
                if (renderer instanceof AuthMethodElementRenderer) {
                    AuthMethodElementRenderer authRenderer =
                            (AuthMethodElementRenderer) renderer;
                    if (authRenderer.getValue().
                            equals(AuthMethodElementRenderer.enablesValue)) {
                        CompositeElementPanelFactory formloginFac =
                                loginFac.getCompositePanelFactory(FORM_LOGIN_CONFIG_NAME);
                        if (formloginFac != null) {
                            Enumeration en = formloginFac.renderers.keys();
                            while (en.hasMoreElements()) {
                                String childName = (String) en.nextElement();
                                ElementRenderer rend = (ElementRenderer)
                                        formloginFac.renderers.get(childName);
                                if (rend.isContentEmpty()) {
                                    emptyChildFactory = loginFac;
                                    loginFac.emptyChildFactory = formloginFac;
                                    formloginFac.emptyChildFactory = null;
                                    formloginFac.emptyChild = childName;
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }
}
