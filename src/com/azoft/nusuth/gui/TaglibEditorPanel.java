/*
 * @(#)TaglibEditorPanel.java 1.0 09/19/2001
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
import java.awt.event.ActionEvent;

/**
 * Class TaglibEditorPanel.
 *
 * @version 1.0 09/19/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TaglibEditorPanel extends CustomEditorPanel {

    /**
     * The web users node name.
     */
//  public final static String WEB_TAGLIB_NODE_NAME = "taglib";

    /**
     * The validator element name.
     */
    private final static String VALIDATOR_NAME = "validator";

    /**
     * The init-param element name.
     */
    private final static String INIT_PARAM_NAME = "init-param";

    /**
     * The listener element name.
     */
    private final static String LISTENER_NAME = "listener";

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS =
            {LISTENER_NAME, VALIDATOR_NAME};


    static {
        USED_COMPOSITE_CHILDS = new Vector();
        int cnt = BASICS_FACS.length;
        for (int i = 0; i < cnt; i++) {
            USED_COMPOSITE_CHILDS.addElement(BASICS_FACS[i]);
        }
        ELEMENT_TYPE = BasicPanel.SWEBTAGLIB;
    }


    /**
     * Constructs a new taglib editor panel.
     * It's the custom editor with the taglib type.
     */
    public TaglibEditorPanel() {
        super(BasicPanel.SWEBTAGLIB);
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
            simplePanel.add(getValidatorPanel(), c);
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
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.insets = new Insets(15, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getMainElements(), c);
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        res.add(new JPanel(), c);
        return res;
    }

    /**
     * Main simple elements.
     *
     * @return  the main simple elements.
     */
    private JPanel getMainElements() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(this, "tlib-version", true, res, c);
        addElement(this, "jsp-version", true, res, c);
        addElement(this, "short-name", true, res, c);
        addElement(this, "uri", true, res, c);
        return res;
    }

    /**
     * Second large panel.
     * Contains the display name, icons, description, listeners.
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
     * Contains the description, listeners.
     *
     * @return  the description els panel
     */
    private JPanel getDescriptionEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 3, 5);
        addElement(this, "description", true, res, c, "up");
        addElement(getCompositePanelFactory(LISTENER_NAME),
                "listener-class", false, res, c, "up");
        return res;
    }

    /**
     * The validator panel.
     *
     * @return  the validator panel
     */
    private JPanel getValidatorPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Validator"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getValidatorClassPanel(), c);
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        res.add(getValidatorParamsPanel(), c);
        return res;
    }

    /**
     * The validator class panel.
     *
     * @return  the validator class panel
     */
    private JPanel getValidatorClassPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 7, 5, 7);
        c.fill = GridBagConstraints.HORIZONTAL;
        addElement(getCompositePanelFactory(VALIDATOR_NAME),
                "validator-class", false, res, c);
        return res;
    }

    /**
     * The validator init params panel.
     *
     * @return  the validator init params panel
     */
    private JPanel getValidatorParamsPanel() {
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
        CompositeElementPanelFactory validatorFac =
                getCompositePanelFactory(VALIDATOR_NAME);
        TablePanelFactory paramFac = (TablePanelFactory)
                validatorFac.getCompositePanelFactory(INIT_PARAM_NAME);
        res.add(paramFac.getMainPane(), c);
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
            JMenuItem item = menu.add("add tag");
            item.addActionListener(this);
            item = menu.add("remove taglib");
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
        if (e.getActionCommand().equals("remove taglib")) {
            if (!basicPanel.canDeleteItems() || basicPanel.unauthorized) {
                ManageTool.showMessage("You can't remove the component");
                return;
            }
            TaglibUserMutableTreeNode taglibNode = (TaglibUserMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            TaglibUserMutableTreeNode taglibsNode =
                    (TaglibUserMutableTreeNode) taglibNode.getParent();
            WebAppMutableTreeNode appNode = (WebAppMutableTreeNode)
                    taglibsNode.getParent();
            Object appObject = appNode.getUserObject();
            if (appObject instanceof Hashtable) {
                CompositeNusuthWebAppElement appElement = (CompositeNusuthWebAppElement)
                        ((Hashtable) appObject).get(BasicPanel.SWEB_APP);
                basicPanel.removeWebAppTaglib(appElement, webElement);
                taglibsNode.remove(taglibNode);
                basicPanel.splitPane.setRightComponent(new JLabel(""));
                basicPanel.reloadTree();
            }
        } else if (e.getActionCommand().toLowerCase().startsWith("add ")) {
            if (!basicPanel.canAddItems() || basicPanel.unauthorized) {
                ManageTool.showMessage("You can't add the component");
                return;
            }
            JMenuItem mit = (JMenuItem) e.getSource();
            String childName = mit.getActionCommand().substring(4); // tag
            String childType = getType() + "." + childName;
            TaglibUserMutableTreeNode taglibNode = (TaglibUserMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            if (webElement != null) {
                try {
                    CompositeNusuthWebAppElement childElement =
                            webElement.addCompositeChild(childName);
                    String nodeName = getDisplay(childType, childElement);
                    TaglibUserMutableTreeNode tagNode =
                            new TaglibUserMutableTreeNode(nodeName, childType);
                    tagNode.setUserObject(childElement);
                    tagNode.setElementNode(taglibNode.getElementNode());
                    basicPanel.getTreeNode(taglibNode, childName).add(tagNode);
                    basicPanel.reloadTree();
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
        }
    }

    /**
     * Saves this component to the server via the basic panel proxy.
     */
    public void saveComponent() throws UnauthorizedAccessException,
            ManagementException, AccessDeniedException {
        // save the web app taglib element
        WebAppMutableTreeNode appNode =
                (WebAppMutableTreeNode) getTreeNode().getParent().getParent();
        Object appObject = appNode.getUserObject();
        if (appObject instanceof Hashtable) {
            CompositeNusuthWebAppElement appElement = (CompositeNusuthWebAppElement)
                    ((Hashtable) appObject).get(BasicPanel.SWEB_APP);
            basicPanel.setWebAppTaglib(appElement,
                    getTreeNode().getComponentId(), webElement);
        }
    }
}
