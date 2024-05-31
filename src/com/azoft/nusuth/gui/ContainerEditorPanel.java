/*
 * @(#)ContainerEditorPanel.java 1.0 09/24/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.*;
import java.awt.*;

/**
 * Class ContainerEditorPanel.
 *
 * @version 1.0 09/24/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ContainerEditorPanel extends CustomEditorPanel {

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS = {"jsp", "logger", "manager"};

    /**
     * The servers tab name
     */
    private final static String SERVERS = "Servers";

    /**
     * Child names in servers tab.
     */
    private final static String[] SERVERS_FACS = {"tcp-server", "ssl-server"};


    static {
        USED_COMPOSITE_CHILDS = new Vector();
        for (int i = 0; i < BASICS_FACS.length; i++) {
            USED_COMPOSITE_CHILDS.addElement(BASICS_FACS[i]);
        }
        for (int i = 0; i < SERVERS_FACS.length; i++) {
            USED_COMPOSITE_CHILDS.addElement(SERVERS_FACS[i]);
        }
        ELEMENT_TYPE = BasicPanel.SCONTAINER;
    }


    /**
     * Constructs a new container editor panel.
     * It's the default editor with container type.
     */
    public ContainerEditorPanel() {
        super(BasicPanel.SCONTAINER);
    }


    /**
     * Creates the composite element for this panel factory.
     *
     * @return  the created composite element
     */
/*
  protected CompositeNusuthWebAppElement createCompositeElement() {
    return createCompositeElement(this.type);
  }
*/

    /**
     * Gets the display string for this panel.
     *
     * @return  the display string for this panel.
     * @see #getDisplay(path, CompositeNusuthWebAppElement)
     */
/*
  public String getDisplay() {
    return getDisplay(getType(), webElement);
  }
*/

    /**
     * Gets the basics tab contains the specified child factory or not.
     * Used the BASICS_FACS array.
     *
     * @param   childFacName  the specified child factory name.
     * @return  <code>true</code> if the basics tab contains the specified child
     * factory; <code>false</code> otherwise.
     */
/*
  protected boolean basicsContains(String childFacName) {
    int cnt = BASICS_FACS.length;
    for (int i = 0; i < cnt; i++) {
      if (BASICS_FACS[i].equals(childFacName)) {
        return true;
      }
    }
    return false;
  }
*/

    /**
     * Overrides the super method.
     * Goes to necessary tab (tab can contain some factories).
     * In case of simple child empty:
     * if factory has only simple elements - we do nothing.
     * if factory has some tabs we have to goto basics tab.
     * In case of composite child has empty simple child:
     * we goes to composite child tab.
     */
    protected void gotoEmpty() {
        // goto basics or servers
        if (emptyChildFactory == null || !emptyChildFactory.isIndividualTab()) {
            if (createPanel() instanceof JTabbedPane) {
                String facTag = emptyChildFactory.getTag();
                String tab = getTabByFactoryTag(facTag);
                if (tab != null) {
                    int tabIndex =
                            ((JTabbedPane) createPanel()).indexOfTab(tab);
                    ((JTabbedPane) createPanel()).setSelectedIndex(tabIndex);
                }
            }
            emptyRequestFocus();
        }
    }

    /**
     * Gets the tab title for the specified factory tag.
     * The factory have to lie in this tab.
     *
     * @param   tag   the specified factory tag.
     * @return  the tab title for the specified factory tag.
     */
    private String getTabByFactoryTag(String tag) {
        for (int i = 0; i < BASICS_FACS.length; i++) {
            if (tag.equals(BASICS_FACS[i])) {
                return EditorPanel.BASICS;
            }
        }
        for (int i = 0; i < SERVERS_FACS.length; i++) {
            if (tag.equals(SERVERS_FACS[i])) {
                return SERVERS;
            }
        }
        return null;
    }

    /**
     * Creates a new panel for this element panel.
     * It is tabbed pane with the tabs summary, basic
     * & all tabs from the TABS array.
     */
    public JComponent createPanel() {
        if (tabbedPane == null) {
            initTabbedPane();

            tabbedPane.addTab(SUMMARY, new JScrollPane(getViewTab()));
            tabbedPane.addTab(BASICS, new JScrollPane(getSimplePanel()));
            tabbedPane.addTab(SERVERS, new JScrollPane(getServersPanel()));
        }
        return tabbedPane;
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
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weighty = 0.1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(7, 7, 7, 7);
            simplePanel.add(getMainSettingsPanel(), c);

            c.gridx = 1;
            c.gridy = 0;
            c.insets = new Insets(7, 0, 7, 7);
            simplePanel.add(getManagerSettingsPanel(), c);

            c.gridx = 0;
            c.gridy = 1;
            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getLoggingSettingsPanel(), c);

            c.gridx = 1;
            c.gridy = 1;
            c.insets = new Insets(0, 0, 7, 7);
            simplePanel.add(getJspSettingsPanel(), c);

            c.gridx = 0;
            c.gridy = 2;
            c.weighty = 1;
            simplePanel.add(new JPanel(), c);
        }
        return simplePanel;
    }

    /**
     * Panel for main settings - the simple elements.
     *
     * @return  the panel for main settings
     */
    private JPanel getMainSettingsPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Main Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getFirstEls(), c);
        res.add(getOtherEls(), c);
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Gets the name & stand alone element panel.
     *
     * @return  the name & stand alone element panel
     */
    private JPanel getFirstEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        String childName = "name";
        ElementRenderer renderer = (ElementRenderer) renderers.get(childName);
        if (renderer != null) {
            JLabel nameLabel = new JLabel(
                    getDisplayName(getType() + "." + childName) + ":");
            if (renderer instanceof DefaultElementRenderer) {
                ((DefaultElementRenderer) renderer).setRendererLabel(nameLabel);
            }
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            res.add(nameLabel, c);
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
            res.add(renderer.getComponent(), c);
        }
        childName = "standalone";
        renderer = (ElementRenderer) renderers.get(childName);
        if (renderer != null) {
            String label = getDisplayName(getType() + "." + childName);
            if (renderer instanceof CheckBooleanElementRenderer) {
                JCheckBox check = (JCheckBox)
                        ((CheckBooleanElementRenderer) renderer).getComponent();
                check.setText(label);
                check.setHorizontalTextPosition(SwingConstants.LEFT);
                check.setForeground(Color.red);
            }
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 0.0;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(5, 15, 5, 0);
            c.anchor = GridBagConstraints.EAST;
            res.add(renderer.getComponent(), c);
        }
        return res;
    }

    /**
     * Gets other simple elements panel.
     *
     * @return  the other simple elements panel
     */
    private JPanel getOtherEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(this, "admin-ip", true, res, c);
        addElement(this, "default-web-config", true, res, c);
        addElement(this, "work-dir", true, res, c);
        addElement(this, "protocol-adapter", true, res, c);
        return res;
    }

    /**
     * Panel for management settings.
     *
     * @return  the panel for management settings.
     */
    private JPanel getManagerSettingsPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Management Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getManagerEls(), c);
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Gets the manager elements panel.
     *
     * @return  the manager elements panel
     */
    private JPanel getManagerEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(getCompositePanelFactory("manager"), "port", false, res, c);
        addElement(getCompositePanelFactory("manager"), "auth-key", false, res, c);
        addElement(getCompositePanelFactory("manager"), "user-name", false, res, c);
        addElement(getCompositePanelFactory("manager"), "password", false, res, c);
        return res;
    }

    /**
     * Panel for logging settings.
     *
     * @return  the panel for logging settings.
     */
    private JPanel getLoggingSettingsPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Logging Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getLoggingEls(), c);
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Gets the logging elements panel.
     *
     * @return  the logging elements panel
     */
    private JPanel getLoggingEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(getCompositePanelFactory("logger"), "location", true, res, c);
        addElement(getCompositePanelFactory("logger"), "level", true, res, c);
        addElement(getCompositePanelFactory("logger"), "config", true, res, c);
        return res;
    }

    /**
     * Panel for jsp settings.
     *
     * @return  the panel for jsp settings.
     */
    private JPanel getJspSettingsPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("JSP Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getJspEls(), c);
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Gets the jsp elements panel.
     *
     * @return  the jsp elements panel
     */
    private JPanel getJspEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(getCompositePanelFactory("jsp"), "refresh", false, res, c);
        addElement(getCompositePanelFactory("jsp"), "compiler", false, res, c);
        return res;
    }

    /**
     * Gets the panel for tcp- & ssl-servers.
     *
     * @return  the panel for tcp- & ssl-servers.
     */
    private JPanel getServersPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(7, 7, 7, 7);
        res.add(getTcpServerPanel(), c);

        c.insets = new Insets(0, 7, 7, 7);
        res.add(getSslServerPanel(), c);

        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Panel for tcp server settings.
     *
     * @return  the panel for tcp server settings
     */
    private JPanel getTcpServerPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("TCP Server"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 1;
        c.weightx = 0.1;
        c.weighty = 0.1;
        c.fill = GridBagConstraints.BOTH;
        res.add(getLeftTcpEls(), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        res.add(getRigthTcpEls(), c);
        c.weighty = 1.0;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Gets the left tcp elements panel.
     *
     * @return  the left tcp elements panel
     */
    private JPanel getLeftTcpEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 5, 4, 5);
        addElement(getCompositePanelFactory("tcp-server"),
                "port", true, res, c);
        addElement(getCompositePanelFactory("tcp-server"),
                "queue", true, res, c);
        addElement(getCompositePanelFactory("tcp-server"),
                "min-handlers", true, res, c);
        addElement(getCompositePanelFactory("tcp-server"),
                "max-handlers", true, res, c);
        addElement(getCompositePanelFactory("tcp-server"),
                "max-keepalives", true, res, c);
        return res;
    }

    /**
     * Gets the rigth tcp elements panel.
     *
     * @return  the rigth tcp elements panel
     */
    private JPanel getRigthTcpEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getRigthTimeTcpEls(), c);
        res.add(getRigthTextAreaTcpEls(), c);
        return res;
    }

    /**
     * Gets the rigth time tcp elements panel.
     *
     * @return  the rigth time tcp elements panel
     */
    private JPanel getRigthTimeTcpEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 5, 4, 5);
        addElement(getCompositePanelFactory("tcp-server"),
                "so-timeout", true, res, c);
        addElement(getCompositePanelFactory("tcp-server"),
                "handler-timeout", true, res, c);
        return res;
    }

    /**
     * Gets the rigth text area tcp elements panel.
     *
     * @return  the rigth text area tcp elements panel
     */
    private JPanel getRigthTextAreaTcpEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 3, 5);
        addElement(getCompositePanelFactory("tcp-server"),
                "allow-hosts", true, res, c);//, "up");
        addElement(getCompositePanelFactory("tcp-server"),
                "deny-hosts", true, res, c); // , "up");
        return res;
    }

    /**
     * Panel for ssl server settings.
     *
     * @return  the panel for ssl server settings
     */
    private JPanel getSslServerPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("SSL Server"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.1;
        c.fill = GridBagConstraints.BOTH;
        res.add(getSimpleSslEls(), c);
        res.add(getCompositeSslEls(), c);
        c.weighty = 1.0;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Panel for simple ssl server elements.
     *
     * @return  the panel for simple ssl server elements.
     */
    private JPanel getSimpleSslEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 1;
        c.weightx = 0.1;
        c.weighty = 0.1;
        c.fill = GridBagConstraints.BOTH;
        res.add(getLeftSslEls(), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        res.add(getRigthSslEls(), c);
        return res;
    }

    /**
     * Gets the left ssl elements panel.
     *
     * @return  the left ssl elements panel
     */
    private JPanel getLeftSslEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 5, 4, 5);
        addElement(getCompositePanelFactory("ssl-server"),
                "port", false, res, c);
        addElement(getCompositePanelFactory("ssl-server"),
                "queue", false, res, c);
        addElement(getCompositePanelFactory("ssl-server"),
                "min-handlers", false, res, c);
        addElement(getCompositePanelFactory("ssl-server"),
                "max-handlers", false, res, c);
        addElement(getCompositePanelFactory("ssl-server"),
                "max-keepalives", false, res, c);
        return res;
    }

    /**
     * Gets the rigth ssl elements panel.
     *
     * @return  the rigth ssl elements panel
     */
    private JPanel getRigthSslEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getRigthTimeSslEls(), c);
        res.add(getRigthTextAreaSslEls(), c);
        res.add(getAuthNeededEl(), c);
        c.weighty = 1.0;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Gets the rigth time ssl elements panel.
     *
     * @return  the rigth time ssl elements panel
     */
    private JPanel getRigthTimeSslEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 5, 4, 5);
        addElement(getCompositePanelFactory("ssl-server"),
                "so-timeout", false, res, c);
        addElement(getCompositePanelFactory("ssl-server"),
                "handler-timeout", false, res, c);
        return res;
    }

    /**
     * Gets the rigth text area ssl elements panel.
     *
     * @return  the rigth text area ssl elements panel
     */
    private JPanel getRigthTextAreaSslEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 3, 5);
        addElement(getCompositePanelFactory("ssl-server"),
                "allow-hosts", false, res, c); // , "up");
        addElement(getCompositePanelFactory("ssl-server"),
                "deny-hosts", false, res, c); // , "up");
        return res;
    }

    /**
     * Gets the auth needed element panel.
     *
     * @return  the auth needed element panel
     */
    private JPanel getAuthNeededEl() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 2, 5);
        addElement(getCompositePanelFactory("ssl-server"),
                "is-client-authentication-needed", false, res, c, "right");
        return res;
    }

    /**
     * Panel for composite ssl server elements.
     *
     * @return  the panel for composite ssl server elements.
     */
    private JPanel getCompositeSslEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.1;
        c.insets = new Insets(4, 5, 4, 5);
        c.fill = GridBagConstraints.BOTH;
        res.add(getKeystoreEls(), c);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(5, 0, 5, 5);
        res.add(getTrustedstoreEls(), c);
        return res;
    }

    /**
     * Gets the ssl keystore elements panel.
     *
     * @return  the ssl keystore elements panel
     */
    private JPanel getKeystoreEls() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Key store"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 5, 4, 5);
        CompositeElementPanelFactory sslFac = getCompositePanelFactory("ssl-server");
        if (sslFac != null) {
            CompositeElementPanelFactory keystFac =
                    sslFac.getCompositePanelFactory("keystore");
            keystFac.setIndividualTab(false);
            addElement(keystFac, "location", false, res, c);
            addElement(keystFac, "password", false, res, c);
        }
        return res;
    }

    /**
     * Gets the ssl trustedstore elements panel.
     *
     * @return  the ssl trustedstore elements panel
     */
    private JPanel getTrustedstoreEls() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Trusted store"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 5, 4, 5);
        CompositeElementPanelFactory sslFac = getCompositePanelFactory("ssl-server");
        if (sslFac != null) {
            CompositeElementPanelFactory trustFac =
                    sslFac.getCompositePanelFactory("trustedstore");
            trustFac.setIndividualTab(false);
            addElement(trustFac, "location", false, res, c);
            addElement(trustFac, "password", false, res, c);
        }
        return res;
    }

    private class EmptyPanel extends JPanel {


        public EmptyPanel() {
            super();
        }

        public Dimension getPreferredSize() {
            return new Dimension(10, 1);
        }

        public Dimension getMaximumSize() {
            return new Dimension(10, 1);
        }

        public Dimension getMinimumSize() {
            return new Dimension(10, 1);
        }
    }
}
