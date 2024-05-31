/*
 * @(#)DistributorEditorPanel.java 1.0 09/24/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.util.*;
import java.awt.*;

/**
 * Class DistributorEditorPanel.
 *
 * @version 1.0 09/24/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class DistributorEditorPanel extends CustomEditorPanel {

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS = {"connection-factory", "logger",
                                             "manager", "cache"};

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
        ELEMENT_TYPE = BasicPanel.SDISTRIBUTOR;
    }


    /**
     * Constructs a new container editor panel.
     * It's the default editor with container type.
     */
    public DistributorEditorPanel() {
        super(BasicPanel.SDISTRIBUTOR);
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
                String tab = EditorPanel.BASICS;
                if (emptyChildFactory != null) {
                    String facTag = emptyChildFactory.getTag();
                    tab = getTabByFactoryTag(facTag);
                }
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
            simplePanel.add(getLoggingSettingsPanel(), c);

            c.gridx = 0;
            c.gridy = 1;
            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getManagerSettingsPanel(), c);

            c.gridx = 1;
            c.gridy = 1;
            c.insets = new Insets(0, 0, 7, 7);
            simplePanel.add(getCacheSettingsPanel(), c);

            c.gridx = 0;
            c.gridy = 2;
            c.weighty = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getConnectionFactoryPanel(), c);
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
        res.add(getMainEls(), c);
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Gets main simple elements panel.
     *
     * @return  the main simple elements panel
     */
    private JPanel getMainEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(this, "name", true, res, c);
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
        addElement(getCompositePanelFactory("manager"), "port", true, res, c);
        addElement(getCompositePanelFactory("manager"), "auth-key", true, res, c);
        addElement(getCompositePanelFactory("manager"), "user-name", true, res, c);
        addElement(getCompositePanelFactory("manager"), "password", true, res, c);
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
     * Panel for cache settings.
     *
     * @return  the panel for cache settings.
     */
    private JPanel getCacheSettingsPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Cache Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getCacheEls(), c);
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        res.add(new EmptyPanel(), c);
        return res;
    }

    /**
     * Gets the cache elements panel.
     *
     * @return  the cache elements panel
     */
    private JPanel getCacheEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(getCompositePanelFactory("cache"), "location", false, res, c);
        addElement(getCompositePanelFactory("cache"), "max-used-memory-size", false, res, c);
        addElement(getCompositePanelFactory("cache"), "max-used-disk-size", false, res, c);
        addElement(getCompositePanelFactory("cache"), "max-page-size", false, res, c);
        addElement(getCompositePanelFactory("cache"), "min-refresh-time", false, res, c);
        return res;
    }

    /**
     * Gets the connection factory panel.
     *
     * @return  the connection factory panel
     */
    private JPanel getConnectionFactoryPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Connection Factory Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getConnectionFactoryClassPanel(), c);
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        res.add(getConnectionFactoryParamsPanel(), c);
        return res;
    }

    /**
     * The connection factory class panel.
     *
     * @return  the connection factory class panel
     */
    private JPanel getConnectionFactoryClassPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 7, 5, 7);
        c.fill = GridBagConstraints.HORIZONTAL;
        addElement(getCompositePanelFactory("connection-factory"),
                "class-name", false, res, c);
        return res;
    }

    /**
     * The connection factory init params panel.
     *
     * @return  the connection factory init params panel
     */
    private JPanel getConnectionFactoryParamsPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 7, 5, 7);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        res.add(new JLabel("Initial parameters:"), c);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 7, 5, 7);
        CompositeElementPanelFactory connFac =
                getCompositePanelFactory("connection-factory");
        TablePanelFactory paramFac = (TablePanelFactory)
                connFac.getCompositePanelFactory("init-parameter");
        res.add(paramFac.getMainPane(), c);
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
