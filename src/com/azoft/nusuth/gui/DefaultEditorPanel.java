/*
 * @(#)DefaultEditorPanel.java 1.0 09/10/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.management.ManagementException;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.*;
import java.util.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Class DefaultEditorPanel.
 *
 * @version 1.0 09/10/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class DefaultEditorPanel
        extends CompositeElementPanelFactory
        implements EditorPanel, ActionListener {

    /**
     * The package prefix for renderers.
     * The renderer names are without this path in properties.
     */
    public static String prefix = "com.azoft.nusuth.gui.";

    /**
     * The default element renderer.
     * Used if there isn't any renderers in properties.
     */
    public static String defaultRenderer = "DefaultElementRenderer";

    /**
     * The names properties.
     */
    protected static Properties namesProps;

    /**
     * Defines if it's needed to save this editor panel or not.
     */
    protected static boolean needSave = false;

    /**
     * The renderers properties.
     */
    private static Properties renderersProps;

    /**
     * The popup properties.
     */
    private static Properties popupProps;

    /**
     * The name changed listener.
     */
    private static NameChangedListener listener;

    /**
     * The static basic panel.
     */
    protected static BasicPanel basicPanel;


    /**
     * The main tabbed pane.
     */
    protected JTabbedPane tabbedPane = null;

    /**
     * The popup menu
     */
    protected JPopupMenu menu;

    /**
     * The view panel.
     */
    protected JPanel viewPanel;

    /**
     * add/delete string for this editor.
     */
    private String addComponents;

    /**
     * Options for the option pane.
     * We show this option pane if panel is not saving, when is leaving.
     */
    private String[] options = {"Apply", "Cancel", "Continue editing"};

    /**
     * The panel with the composite element panel & apply/cancel buttons.
     */
    private JPanel commonPanel;

    /**
     * The current config tree node for this editor.
     */
    private ConfigMutableTreeNode treeNode;

    /**
     * The custom panel dimension.
     */
    private Dimension customPanelDimension = new Dimension(100, 100);

    /**
     * The current tab index in the tabbed pane.
     */
    private int currentTabIndex = -1;


    static {
        namesProps = new Properties();
        renderersProps = new Properties();
        popupProps = new Properties();
        try {
            namesProps.load(ClassLoader.getSystemResourceAsStream(
                    "com/azoft/nusuth/gui/DisplayNames.properties"));
        } catch (Throwable ex) {
            System.err.println("Cannot load names properties - using names");
        }
        try {
            renderersProps.load(ClassLoader.getSystemResourceAsStream(
                    "com/azoft/nusuth/gui/renderers.properties"));
        } catch (Throwable ex) {
            System.err.println(
                    "Cannot load renderers properties - using defaults instead");
        }
        try {
            popupProps.load(ClassLoader.getSystemResourceAsStream(
                    "com/azoft/nusuth/gui/popup.properties"));
        } catch (Throwable ex) {
            System.err.println(
                    "Cannot load popup properties - all popup will be empty");
        }
    }


    /**
     * Constructs a new default editor panel with the specified type.
     *
     * @param   type    the specified type.
     */
    public DefaultEditorPanel(String type) {
        super(type);
        commonPanel = new JPanel(new BorderLayout());
        commonPanel.add("Center", createPanel());
        JPanel options = new JPanel();
        JButton ok = new JButton("Apply");
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (aplly()) {
                    save();        // save changes
                }
            }
        });
        options.add(ok);
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                checkAllElementsEditing();
                updateControls();
            }
        });
        options.add(cancel);
        commonPanel.add("South", options);
    }

    /**
     * Apply button action.
     * First checks all required element on empty.
     * Then checks all this element tree components.
     * And then performs update if it's ok.
     */
    private boolean aplly() {
        return aplly(true);
    }

    /**
     * Checks all required element on empty.
     * And then performs update if it's ok.
     */
    private boolean aplly(boolean needCheckTree) {
        if (requiredNotEmpty()) {
            updateEntry();
            if (needCheckTree) {
                ConfigMutableTreeNode node = getTreeNode().getElementNode();
                if (node != null) {
                    if (!checkNodes(node)) {
                        return false;
                    }
                }
            }
            return true;
        }
        gotoEmpty();
        JOptionPane.showMessageDialog(ManageTool.getMainFrame(),
                "The required element " + getEmptyChild() + " is empty!!!",
                " Warning", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
     * Recursive function for checking the specified node & all its childs on
     * the possibility to be apllied.
     *
     * @param   node    the specified node.
     */
    private boolean checkNodes(DefaultMutableTreeNode node) {
        if (!checkNodePanel(node)) {
            gotoNodePanel(node);
            return false;
        }
        Enumeration childs = (node instanceof ConfigMutableTreeNode)
                ? ((ConfigMutableTreeNode) node).getCheckedChilds()
                : node.children();
        while (childs.hasMoreElements()) {
            DefaultMutableTreeNode dchild =
                    (DefaultMutableTreeNode) childs.nextElement();
            if (!checkNodes(dchild)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks the node panel on required not empty.
     *
     * @param   node    the specified node.
     */
    private boolean checkNodePanel(DefaultMutableTreeNode node) {
        if (getTreeNode() == node || !(node instanceof ConfigMutableTreeNode)) {
            return true;
        }
        ConfigMutableTreeNode cnode = (ConfigMutableTreeNode) node;
        String type = cnode.getType();
        Object userObject = cnode.getUserObject();
        EditorPanel ep = basicPanel.getEditorPanel(type);
        ep.setEntry(userObject);
        ep.updateControls();
        if (ep instanceof DefaultEditorPanel) {
            return ((DefaultEditorPanel) ep).requiredNotEmpty();
        }
        return true;
    }

    /**
     * Selects the specified node in the main tree,
     * sets the node panel in the right part of split pane & apllies the panel.
     *
     * @param   node    the specified node.
     */
    private void gotoNodePanel(DefaultMutableTreeNode node) {
        if (node instanceof ConfigMutableTreeNode) {
            TreeNode[] path = node.getPath();
            TreePath tp = new TreePath(path);
            // sets the entry to the necessary panel &
            // the panel in right part of the split pane
            basicPanel.tree.setSelectionPath(tp);
            String type = ((ConfigMutableTreeNode) node).getType();
            ((DefaultEditorPanel) basicPanel.getEditorPanel(type)).aplly(false);
        }
    }

    /**
     * Creates a new panel for this element panel.
     * It is tabbed pane with the tabs summary, basic & all composite panel tabs.
     */
    public JComponent createPanel() {
        if (tabbedPane == null) {
            initTabbedPane();

            tabbedPane.addTab(SUMMARY, new JScrollPane(getViewTab()));
            if (hasSimplePanel()) {
                tabbedPane.addTab(BASICS, new JScrollPane(getSimplePanel()));
            } else {
                getSimplePanel();
            }

            Enumeration e = getCompositePanelFactories();
            if (e != null) {
                while (e.hasMoreElements()) {
                    CompositeElementPanelFactory factory =
                            (CompositeElementPanelFactory) e.nextElement();
                    if (factory.isIndividualTab()) {
                        String title = getDisplayName(factory.getType());
                        tabbedPane.addTab(title, factory.createPanel());
//            allTitles.addElement(title);
                    }
                }
            }
        }
        return tabbedPane;
    }

    /**
     * Inits the tabbed pane.
     * Adds the necessary change listener.
     */
    protected void initTabbedPane() {
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JTabbedPane tab = (JTabbedPane) e.getSource();
                if (currentTabIndex != -1) {
                    Component prev = (JComponent) tab.getComponentAt(currentTabIndex);
                    if (prev instanceof ElementPanel) {
                        ((ElementPanel) prev).checkEditing();
                    }
                }
                currentTabIndex = tab.getSelectedIndex();
            }
        });
    }

    /**
     * Checka all elements on editing.
     * Used before need save checking.
     */
    private void checkAllElementsEditing() {
        Enumeration compositeFacs = getCompositePanelFactories();
        while (compositeFacs != null && compositeFacs.hasMoreElements()) {
            CompositeElementPanelFactory factory =
                    (CompositeElementPanelFactory) compositeFacs.nextElement();
            if (factory instanceof TablePanelFactory) {
                JComponent panel = ((TablePanelFactory) factory).createPanel();
                if (panel instanceof ElementPanel) {
                    ((ElementPanel) panel).checkEditing();
                }
            }
        }
    }

// Methods from the editor panel interface

    /**
     * Gets the display string of this editor.
     *
     * @return  the display string of this editor.
     * @see #getDisplay(path, CompositeNusuthWebAppElement)
     */
    public String getDisplay() {
        return getDisplay(this.type, this.webElement);
    }

    /**
     * Gets the display string for the specified path & web element.
     *
     * @param   path        the specified path
     * @param   webElement  the specified web element
     * @return  the display string
     */
    public static String getDisplay(String path,
                                    CompositeNusuthWebAppElement webElement) {
        if (path == null) return "display name";
        if (webElement == null) return getDisplayName(path);
        String s = namesProps.getProperty(path + ".displayComp");
        if (s == null) {
            int ind = path.lastIndexOf(".");
            if (ind == -1) return getDisplayName(path);
            String tag = path.substring(ind + 1);
            return getDisplay(tag, webElement);
        }
        StringTokenizer st = new StringTokenizer(s.trim(), ",");
        Enumeration enum;
        while (st.hasMoreTokens()) {
            try {
                String childName = st.nextToken().trim();
                enum = webElement.getSimpleChild(childName);
                if (enum.hasMoreElements()) {
                    String content = ((SimpleNusuthWebAppElement) enum.nextElement()).
                            getContent();
                    if (!content.equals("")) return content;
                }
            } catch (DeploymentException dex) {
            }
        }
        return webElement.getTag();
    }

    /**
     * Gets the name from display name properties.
     * If path is composite, for ex. 'host.context.location',
     * it find the name first for 'host.context.location',
     * then (if prop null) for 'context.location', and then for 'location'.
     *
     * @param   path    element path
     * @return  the name from display name properties.
     */
    public static String getDisplayName(String path) {
        String prop = namesProps.getProperty(path + ".display");
        if (prop != null) return prop;
        int ind = path.indexOf(".");
        if (ind == -1) return path; // tag
        String tag = path.substring(ind + 1);
        return getDisplayName(tag);
    }

    /**
     * Gets the main component (panel or tabbed pane or something else).
     *
     * @return  the main component
     */
    public JComponent getMainComponent() {
        return commonPanel;
    }

    /**
     * Gets the popup menu for this editor.
     *
     * @return  the popup menu for this editor.
     */
    public JPopupMenu getPopupMenu() {
        boolean hasadd = hasAddMenu();
        boolean hasdel = hasDeleteMenu();
        if (menu == null && (hasadd || hasdel)) {
            menu = new JPopupMenu();
            if (hasadd) {
                if (menu == null) menu = new JPopupMenu();
                StringTokenizer st = new StringTokenizer(addComponents, ";");
                while (st.hasMoreElements()) {
                    JMenuItem item = menu.add("add " + getPopupType(st.nextToken()));
                    item.addActionListener(this);
                }
            }
            if (hasdel) {
                JMenuItem item = menu.add("remove " + getPopupType(type));
                item.addActionListener(this);
            }
        }
        return menu;
    }

    /**
     * Gets if this editor has add menu or not.
     *
     * @return  <code>true</code> if this editor has add menu;
     * <code>false</code> overwise.
     */
    private boolean hasAddMenu() {
        addComponents = popupProps.getProperty(getType() + ".add");
        return (addComponents != null);
    }

    /**
     * Gets if this editor has delete menu or not.
     *
     * @return  <code>true</code> if this editor has delete menu;
     * <code>false</code> overwise.
     */
    private boolean hasDeleteMenu() {
        Enumeration e = popupProps.elements();
        while (e.hasMoreElements()) {
            String next = (String) e.nextElement();
            StringTokenizer st = new StringTokenizer(next, ";");
            while (st.hasMoreElements()) {
                if (type.equals(st.nextToken())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the type for popup menu.
     * Cuts the specified full path, leaves only last type (after the last '.')
     *
     * @param   fullType  the specified full type
     * @return  the type for popup menu
     */
    private String getPopupType(String fullType) {
        int lastPointInd = fullType.lastIndexOf(".");
        return (lastPointInd == -1) ? fullType :
                fullType.substring(lastPointInd + 1);
    }

    /**
     * Method from the ActionListener interface.
     * Processes the add/remove element commands.
     * Only for default elements!!!
     * The subclasses have to overrides this method.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().toLowerCase().startsWith("remove ")) {
            if (!basicPanel.canDeleteItems() || BasicPanel.unauthorized) {
                ManageTool.showMessage("You can't remove the component");
                return;
            }
            DefaultMutableTreeNode dnode = (DefaultMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            if (dnode instanceof ConfigMutableTreeNode) {
                ConfigMutableTreeNode cnode = (ConfigMutableTreeNode) dnode;
                try {
                    ConfigMutableTreeNode configParentNode =
                            (ConfigMutableTreeNode) cnode.getParent();
                    CompositeNusuthWebAppElement webElement =
                            (CompositeNusuthWebAppElement) configParentNode.getUserObject();
                    CompositeNusuthWebAppElement childElement =
                            (CompositeNusuthWebAppElement) cnode.getUserObject();
                    webElement.removeCompositeChild(getPopupType(type), childElement);
                    if (type.equals(BasicPanel.SGROUP) || type.equals(BasicPanel.SUSER)) {
                        basicPanel.fireValueRemoved(getPopupType(type),
                                basicPanel.getEditorPanel(type).getDisplay());
                    }
                    configParentNode.remove(cnode);
                    basicPanel.splitPane.setRightComponent(new JLabel(""));
                    basicPanel.reloadTree();
                    // save component to server
                    EditorPanel parentPanel =
                            basicPanel.getEditorPanel(configParentNode.getType());
                    parentPanel.setTreeNode(configParentNode);
                    parentPanel.setEntry(webElement);
                    parentPanel.save();
                } catch (ClassCastException e1) {
                    System.out.println("ClassCastException in time of deleting !!! " + e);
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
        } else if (e.getActionCommand().toLowerCase().startsWith("add ")) {
            if (!basicPanel.canAddItems() || BasicPanel.unauthorized) {
                ManageTool.showMessage("You can't add the component");
                return;
            }
            DefaultMutableTreeNode dnode = (DefaultMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            if (dnode instanceof ConfigMutableTreeNode) {
                ConfigMutableTreeNode cnode = (ConfigMutableTreeNode) dnode;
                JMenuItem mit = (JMenuItem) e.getSource();
                String childName = mit.getActionCommand().substring(4);
                String realChildName = (childName.equals("application"))
                        ? "context" : childName;
                String childType = type + "." + realChildName;
                CompositeNusuthWebAppElement webElement =
                        (CompositeNusuthWebAppElement) cnode.getUserObject();
                if (webElement != null) {
                    try {
                        CompositeNusuthWebAppElement childElement =
                                webElement.addCompositeChild(realChildName);
                        String nodeName = getDisplay(childType, childElement);
                        ConfigMutableTreeNode childNode = null;
                        if (childName.equals("application")) {
                            childNode = new WebAppMutableTreeNode(nodeName, BasicPanel.SAPP);
                            childNode.setElementNode(childNode);
                            // puts the context element
                            Hashtable hash = new Hashtable();
                            hash.put(ApplicationEditorPanel.contextName, childElement);
                            childNode.setUserObject(hash);
                        } else {
                            childNode = new ConfigMutableTreeNode(nodeName, childType);
                            childNode.setUserObject(childElement);
                        }
                        cnode.add(childNode);
                        TreePath selPath = basicPanel.tree.getSelectionPath();
                        basicPanel.reloadTree();
                        basicPanel.tree.setSelectionPath(selPath);
                        if (childType.equals(BasicPanel.SGROUP)
                                || childType.equals(BasicPanel.SUSER)) {
                            basicPanel.fireValueAdded(childName,
                                    basicPanel.getEditorPanel(childType).getDisplay());
                        }
                    } catch (DeploymentException de) {
                        System.out.println(de);
                    }
                } else {
                    System.out.println("ERROR");
                }
            }
        }
    }

    /**
     * Gets if this editor panel is saving or not.
     * If it were fields changing, it shows the dialog with 'applay', 'cancel'
     * & 'continue editing'. In case of 'continue editing' returns false.
     *
     * @return  <code>true</code> if the editing panel was saving;
     * <code>false</code> otherwise.
     */
    public boolean isSaving() {
        checkAllElementsEditing();
        if (needSave) {
            int res = JOptionPane.showOptionDialog(commonPanel,
                    "The last component has been modified!", "",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            if (res == JOptionPane.OK_OPTION) {
                boolean success = aplly();
                if (success) {
                    save();
                }
                return success;
//        updateEntry();
//        return true;
            } else if (res == JOptionPane.NO_OPTION) {
                updateControls();
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the specified entry to this editor.
     *
     * @param   entry   the specified entry
     */
    public void setEntry(Object entry) {
        if (entry instanceof CompositeNusuthWebAppElement) {
            this.webElement = (CompositeNusuthWebAppElement) entry;
        }
//    updateControls();
    }

    /**
     * Gets the entry of this editor.
     *
     * @return  the entry of this editor.
     * @see #setEntry(Object)
     */
    public Object getEntry() {
        return this.webElement;
    }

    /**
     * Gets the web element of this editor panel.
     *
     * @return  the web element of this editor panel.
     */
    public CompositeNusuthWebAppElement getWebElement() {
        return this.webElement;
    }


    /**
     * Sets the specified tree node to this editor.
     *
     * @param   node    the specified tree node.
     * @see #getTreeNode()
     */
    public void setTreeNode(ConfigMutableTreeNode node) {
        this.treeNode = node;
    }

    /**
     * Gets the current tree node.
     *
     * @return  the current tree node
     * @see #setTreeNode(ConfigMutableTreeNode)
     */
    public ConfigMutableTreeNode getTreeNode() {
        return this.treeNode;
    }

    /**
     * Updates all fields by the web element.
     * Checks show tabs value, controlls summary tab content.
     */
    public void updateControls() {
//    checkShowTabs();
        updateAllControlls();
        setViewTab();
        basicPanel.repaint();
        needSave = false;
    }

    /**
     * Gets the specified title is required or not.
     *
     * @param   title   a component.
     * @return  <code>true</code> if the specified title is required;
     * <code>false</code> otherwise.
     */
    private boolean titleIsRequired(String title) {
        return (title.equals(SUMMARY) || title.equals(BASICS));
    }

    /**
     * Sets a new content of summary tab by the web element.
     */
    protected void setViewTab() {
        getViewTab().removeAll();
        viewPanel.setLayout(new BoxLayout(viewPanel, BoxLayout.Y_AXIS));
        viewPanel.add(Box.createRigidArea(new Dimension(1, 10)));
        addComponentLabels();
    }

    /**
     * Add web element content
     */
    protected void addComponentLabels() {
        addComponentLabels(viewPanel, webElement);
    }

    /**
     * Add content of the specified element to the specified panel.
     *
     * @param   panel   the specified panel
     * @param   element the specified element
     */
    protected void addComponentLabels(JPanel panel,
                                      CompositeNusuthWebAppElement element) {
        addComponentLabels(panel, element, false);
    }

    /**
     * Add content of the specified element to the specified panel.
     * Boolean value context defines if it element is context element.
     *
     * @param   panel   the specified panel
     * @param   element the specified element
     * @param   context defines if it element is context element
     */
    protected void addComponentLabels(JPanel panel,
                                      CompositeNusuthWebAppElement element,
                                      boolean context) {
        if (element != null) {
            String wes = element.toString();
            StringTokenizer st = new StringTokenizer(wes, "\r\n");
            if (!context && st.hasMoreTokens()) st.nextToken(); // skip main tag
            String tmp;
            int prevShift = 0;
            while (st.hasMoreTokens()) {
                String ors = st.nextToken();
                if (ors.trim().startsWith("</")) {
                    continue;
                } else {
                    tmp = "";
                    if (ors.trim().startsWith("<")) {
                        int iopen = ors.indexOf("<");
                        int iclose = ors.indexOf(">");
                        int iopen2 = ors.indexOf("</", iclose);
                        int cnt = iopen * 2;
                        while (cnt-- > 0) {
                            tmp += " ";
                        }
                        tmp += ors.substring(iopen + 1, iclose) + ": ";
                        prevShift = (int) (tmp.length() * 1.5);
                        tmp += (iopen2 > 0) ? ors.substring(iclose + 1, iopen2)
                                : ors.substring(iclose + 1, ors.length());
                        if (!tmp.trim().startsWith("password")) {
                            tmp = changeTabs(tmp);
                            JLabel l = new JLabel(tmp);
                            panel.add(l);
                        }
                    } else {
                        int iopen = ors.indexOf("</");
                        int cnt = prevShift;
                        while (cnt-- > 0) {
                            tmp += " ";
                        }
                        tmp += (iopen == -1) ? ors : ors.substring(0, iopen);
                        tmp = changeTabs(tmp);
                        JLabel l = new JLabel(tmp);
                        panel.add(l);
                    }
                }
            }
        }
    }

    /**
     * Changes all tabs (i.e \t) to some spaces in the specified string.
     *
     * @param s   the specified string
     */
    private String changeTabs(String s) {
        String res = "";
        StringTokenizer st = new StringTokenizer(s, "\t");
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            res += (res.length() == 0) ? next : "    " + next;
        }
        return res;
    }

    /**
     * Gets the view tab.
     *
     * @return  the view tab
     */
    protected JPanel getViewTab() {
        if (viewPanel == null) {
            viewPanel = new JPanel();
        }
        return viewPanel;
    }

    /**
     * Updates all fields by the web element.
     */
    protected void updateAllControlls() {
        updateControls(webElement);
    }

    /**
     * Updates the editor entry by the controls.
     * Controlls a display name changing & a componentId changing.
     * Saves this component.
     */
    public void updateEntry() {
        String lastDisplay = getDisplay();
        String lastComponentId = getComponentId();
        updateAllElements();
        needSave = false;
        String newDisplay = getDisplay();
        String newComponentId = getComponentId();
        if (!checkUnique(lastDisplay, newDisplay)) {
            return;
        }
        if (!newDisplay.equals(lastDisplay)) {
            fireNameChanged(this, lastDisplay, newDisplay);
        }
        if (newComponentId != null && lastComponentId != null
                && !newComponentId.equals(lastComponentId)) {
            fireComponentIdChanged(this, lastComponentId, newComponentId);
        }
        updateControls(); // setViewTab() & basicPanel.repaint() inside
    }

    /**
     * Initializes the saving process.
     */
    public void save() {
        basicPanel.saveComponent(this);
    }

    /**
     * Gets the componentId component content for this panel.
     *
     * @return  the componentId component content for this panel.
     * @see #getComponentId(String, CompositeNusuthWebAppElement)
     */
    protected String getComponentId() {
        return getComponentId(this.type, this.webElement);
    }

    /**
     * Gets the componentId child content.
     * The children is defined by the specified path (from properties)
     * and the content is taken from the specified element.
     *
     * @param   path    the specified path.
     * @param   element the specified web element.
     * @return  the componentId children content.
     * @see #getComponentId()
     */
    protected String getComponentId(String path,
                                    CompositeNusuthWebAppElement element) {
        String childName = namesProps.getProperty(path + ".componentIdComp");
        if (childName != null) {
            try {
                Enumeration enum = element.getSimpleChild(childName);
                if (enum.hasMoreElements()) {
                    return ((SimpleNusuthWebAppElement) enum.nextElement()).getContent();
                }
            } catch (DeploymentException dex) {
            }
        }
        return null;
    }

    /**
     * Updates all web element by the panel fields.
     */
    protected void updateAllElements() {
        updateElement(webElement);
    }

    /**
     * Checks the element name unique.
     * If the element name isn't unique, it shows the warning,
     * updates controls & returns <code>false</code>;
     * else returns <code>true</code>
     *
     * @param lastD   the last display content
     * @param newD    the new display content
     * @return  <code>true</code> if name is unique;
     * <code>false</code> otherwise.
     */
    protected boolean checkUnique(String lastD, String newD) {
        if ((!newD.equals(lastD) && notUnique(newD))
                || (newD.equals(lastD) && notUnique2(newD))) {
            ManageTool.showMessage("The name of " + getType() + " must be unique");
            returnValue(webElement, lastD);
            updateControls(webElement);
            return false;
        }
        return true;
    }

    /**
     * Gets the specified name is unique or not.
     *
     * @param name  the specified name.
     * @return  <code>true</code> if the specified name is not unique;
     * <code>false</code> otherwise.
     */
    private boolean notUnique(String name) {
        if (getType().equals("user")) {
            return BasicPanel.getUserNames().contains(name);
        }
        if (getType().equals("group")) {
            return BasicPanel.getGroupNames().contains(name);
        }
        return false;
    }

    /**
     * Gets the specified name is unique or not without one equals.
     *
     * @param name  the specified name.
     * @return  <code>true</code> if the specified name is not unique;
     * <code>false</code> otherwise.
     */
    private boolean notUnique2(String name) {
        if (getType().equals("user")) {
            int index = BasicPanel.getUserNames().indexOf(name);
            if (index != -1) {
                return BasicPanel.getUserNames().indexOf(name, index + 1) != -1;
            }
            return false;
        }
        if (getType().equals("group")) {
            int index = BasicPanel.getGroupNames().indexOf(name);
            if (index != -1) {
                return BasicPanel.getGroupNames().indexOf(name, index + 1) != -1;
            }
            return false;
        }
        return false;
    }

    /**
     * Sets the last display value to element 'name' in web element.
     *
     * @param   webElement  the specified web element
     * @param   lastDisplay the last display string
     */
    private void returnValue(CompositeNusuthWebAppElement webElement,
                             String lastDisplay) {
        try {
            SimpleNusuthWebAppElement name = webElement.setSimpleChild("name");
            name.setContent(lastDisplay);
        } catch (DeploymentException e) {
        }
    }

    /**
     * Calls the nameChanged method in NameChangedListener.
     *
     * @param   wep       the web element panel, firing a changing.
     * @param   oldValue  the old name value.
     * @param   newValue  the new name value.
     */
    protected void fireNameChanged(EditorPanel ep, String oldValue,
                                   String newValue) {
        listener.nameChanged(ep, oldValue, newValue);
    }

    /**
     * Calls the componentIdChanged method in NameChangedListener.
     *
     * @param   wep       the web element panel, firing a changing.
     * @param   oldValue  the old name value.
     * @param   newValue  the new name value.
     */
    protected void fireComponentIdChanged(EditorPanel ep, String oldValue,
                                          String newValue) {
        listener.componentIdChanged(ep, oldValue, newValue);
    }

    /**
     * Gets the type of this panel.
     *
     * @return  the type of this panel.
     */
    public String getType() {
        return super.getType();
    }

    /**
     * Updates component tree ui of this panel.
     */
    public void updateUI() {
        SwingUtilities.updateComponentTreeUI(commonPanel);
    }

    /**
     * Sets the static basic panel to this editor.
     *
     * @param   basicPanel    the specified basic panel.
     */
    static void setBasicPanel(BasicPanel bp) {
        basicPanel = bp;
    }

    /**
     * Sets the NameChangedListener to this panel.
     *
     * @param l   the specified NameChangedListener.
     */
    public static void setNameChangedListener(NameChangedListener l) {
        listener = l;
    }

    /**
     * Creates an row with child name & renderer component.
     * It may contains a radio button, if necessary.
     * If renderer is Table Renderer - it will a table.
     *
     * @param   renderer    an element renderer.
     * @param   p           a panel, in which row will be put.
     * @param   c           a grid bag constraints.
     * @param   parentPath  a parent path.
     * @param   elementName a child element name.
     * @param   required    <code>true</code> if element is required
     *                      in parent element.
     * @param   unbounded   <code>true</code> if element is unbounded
     *                      in parent element.
     */
    public static void createRow(ElementRenderer renderer, JPanel p,
                                 GridBagConstraints c, String parentPath,
                                 String elementName, boolean required,
                                 boolean unbounded) {
        createRow(renderer, p, c, parentPath, elementName, required,
                unbounded, "left");
    }

    /**
     * Creates an row with child name & renderer component.
     * It may contains a radio button, if necessary.
     * If renderer is Table Renderer - it will a table.
     *
     * @param   renderer    an element renderer.
     * @param   p           a panel, in which row will be put.
     * @param   c           a grid bag constraints.
     * @param   parentPath  a parent path.
     * @param   elementName a child element name.
     * @param   required    <code>true</code> if element is required in parent element.
     * @param   unbounded   <code>true</code> if element is unbounded in parent element.
     * @param   labelLoc    defines a label location
     */
    public static void createRow(ElementRenderer renderer, JPanel p,
                                 GridBagConstraints c, String parentPath,
                                 String elementName, boolean required,
                                 boolean unbounded, String labelLoc) {
        createRow(renderer, p, c, parentPath, elementName, required,
                unbounded, null, null, null, null, labelLoc);
    }

    /**
     * Creates an row with child name & renderer component.
     * It may contains a radio button, if necessary.
     * If renderer is Table Renderer - it will a table.
     *
     * @param   renderer    an element renderer.
     * @param   p           a panel, in which row will be put.
     * @param   c           a grid bag constraints.
     * @param   parentPath  a parent path.
     * @param   elementName a child element name.
     * @param   required    <code>true</code> if element is required in parent element.
     * @param   unbounded   <code>true</code> if element is unbounded in parent element.
     * @param   group       a group for radio button if it exist & null if it doesn't.
     * @param   actionListener  an action listener for radio button.
     * @param   radioHashComps  a hashtable for radio with components.
     * @param   radioHashNames  a hashtable for radio with radio names.
     */
    public static void createRow(ElementRenderer renderer, JPanel p,
                                 GridBagConstraints c, String parentPath,
                                 String elementName, boolean required,
                                 boolean unbounded, ButtonGroup group,
                                 ActionListener actionListener,
                                 Hashtable radioHashComps,
                                 Hashtable radioHashNames) {
        createRow(renderer, p, c, parentPath, elementName, required, unbounded,
                group, actionListener, radioHashComps, radioHashNames, "left");
    }

    /**
     * Creates an row with child name & renderer component.
     * It may contains a radio button, if necessary.
     * If renderer is Table Renderer - it will a table.
     *
     * @param   renderer    an element renderer.
     * @param   p           a panel, in which row will be put.
     * @param   c           a grid bag constraints.
     * @param   parentPath  a parent path.
     * @param   elementName a child element name.
     * @param   required    <code>true</code> if element is required in parent element.
     * @param   unbounded   <code>true</code> if element is unbounded in parent element.
     * @param   group       a group for radio button if it exist & null if it doesn't.
     * @param   actionListener  an action listener for radio button.
     * @param   radioHashComps  a hashtable for radio with components.
     * @param   radioHashNames  a hashtable for radio with radio names.
     * @param   labelLoc        defines a label location (left or up)
     */
    public static void createRow(ElementRenderer renderer, JPanel p,
                                 GridBagConstraints c, String parentPath,
                                 String elementName, boolean required,
                                 boolean unbounded, ButtonGroup group,
                                 ActionListener actionListener,
                                 Hashtable radioHashComps,
                                 Hashtable radioHashNames, String labelLoc) {
        if (renderer != null) {
            String suf = (unbounded) ? "s" : "";
            String displayName = getDisplayName(parentPath + "." + elementName);
            int ind = displayName.indexOf("\n");
            String firstString = (ind == -1)
                    ? displayName + suf + ":" : displayName.substring(0, ind);
            JLabel nameLabel = new JLabel(firstString);
            JLabel doubleLabel = null;
            if (ind != -1) {
                String doubleSt = displayName.substring(ind + 1);
                doubleLabel = new JLabel(doubleSt + suf + ":");
            }
            if (required) {
                nameLabel.setForeground(Color.red);
                if (doubleLabel != null) {
                    doubleLabel.setForeground(Color.red);
                }
            }
            JComponent labels = getLabels(nameLabel, doubleLabel);
            if (renderer instanceof DefaultElementRenderer) {
                ((DefaultElementRenderer) renderer).setRendererLabel(labels);
            }
            if (group == null) {
                if (renderer instanceof TableRenderer) {
                    createTable(p, c, renderer.getComponent(), renderer.takesAllPlace());
                } else if (renderer instanceof CheckBooleanElementRenderer) {
                    String labelText = nameLabel.getText();
                    ((JCheckBox) renderer.getComponent()).setText(
                            labelText.substring(0, labelText.length() - 1));
                    createDialogRow(p, c, null, renderer.getComponent(),
                            renderer.takesAllPlace(), "none");
                } else {
                    createDialogRow(p, c, labels, renderer.getComponent(),
                            renderer.takesAllPlace(), labelLoc);
                }
            } else {
                JPanel labelPanel = new JPanel(new BorderLayout());
                labelPanel.add("East", labels);
                JRadioButton radio = new JRadioButton();
                group.add(radio);
                radio.addActionListener(actionListener);
                JComponent[] comps = {labels, renderer.getComponent()};
                radioHashComps.put(radio, comps);
                radioHashNames.put(elementName, radio);
                labelPanel.add("West", radio);
                createDialogRow(p, c, labelPanel, renderer.getComponent(),
                        renderer.takesAllPlace(), labelLoc);
            }
        }
    }

    /**
     * Gets the labels component.
     * If there are two labels - it puts them in grid layout panel.
     *
     * @param   jc1   the first label
     * @param   jc2   the second label
     * @return  the labels component
     */
    private static JComponent getLabels(JComponent jc1, JComponent jc2) {
        if (jc2 == null) {
            return jc1;
        }
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.EAST;
        res.add(jc1, c);
        res.add(jc2, c);
        return res;
    }

    /**
     * Creates a dialog row & adds it to the specified panel
     * with the specified constraints.
     * Dialog row consisits of the specified label & component.
     * If the field is true the component will take all horizontal space.
     * Label location defines the location of the label.
     *
     * @param   p         the specified panel.
     * @param   c         the specified grid bag constraints.
     * @param   label     the specified label.
     * @param   comp      the specified component.
     * @param   field     the specified field.
     * @param   labelLoc  the specified label location.
     * @see #getPar()
     */
    public static void createDialogRow(JPanel p, GridBagConstraints c,
                                       JComponent label, JComponent comp,
                                       boolean field, String labelLoc) {
        // if label is placed on left side
        if (labelLoc.equals("left")) {
            // default
            c.gridx = 0;
            c.gridy++;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            c.weightx = 0.0;
            c.weighty = 0.0;
            p.add(label, c);

            c.gridx = 1;
            if (field) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else {
                c.fill = GridBagConstraints.NONE;
            }
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1.0;
            c.weighty = 0.0;
            p.add(comp, c);
        } else if (labelLoc.equals("none")) {
            if (field) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else {
                c.fill = GridBagConstraints.NONE;
            }
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1.0;
            c.weighty = 0.0;
            p.add(comp, c);
        }
        // if label is placed upper then component
        else if (labelLoc.equals("up")) {
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 0.0;
            c.weighty = 0.0;
            p.add(label, c);

            if (field) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else {
                c.fill = GridBagConstraints.NONE;
            }
            c.weightx = 1.0;
            p.add(comp, c);
        } else {
            System.out.println("label " + labelLoc + " is not known ");
        }
    }

    /**
     * Adds the specified table to the specified panel
     * with the spec grid bag constraints.
     * If the field is true the table will take all horizontal space.
     *
     * @param   p         the specified panel.
     * @param   c         the specified grid bag constraints.
     * @param   table     the specified table.
     * @param   field     the specified field.
     */
    public static void createTable(JPanel p, GridBagConstraints c,
                                   JComponent table, boolean field) {
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        if (field) c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
        p.add(table, c);
    }

    /**
     * Gets the element renderer by parent tag & child name.
     *
     * @param parent      the specified parent tag.
     * @param elementName the specified element name.
     * @return  the element renderer.
     */
    public static ElementRenderer getRenderer(String parent,
                                              String elementName) {
        return getRendererByName(parent,
                getRendererName(parent + "." + elementName), elementName);
    }

    /**
     * Gets the element renderer by renderer name & child name.
     *
     * @param rendererName  the specified renderer name.
     * @param elementName   the specified element name.
     * @return  the element renderer.
     */
    public static ElementRenderer getRendererByName(String parent,
                                                    String rendererName,
                                                    String elementName) {
        ElementRenderer res = null;

        if (rendererName.startsWith("com.azoft.nusuth.gui.TableRenderer")) {
            res = new TableRenderer(elementName, parent);
            int indRaz = rendererName.indexOf(";");
            if (indRaz != -1) {
                String cellRendererName = prefix + rendererName.substring(indRaz + 1);
                ElementRenderer cellRenderer =
                        getRendererByName(parent, cellRendererName, elementName);
                if (cellRenderer != null) {
                    CompositeCellEditor cellEditor = getCellEditor(cellRenderer);
                    if (cellEditor != null) {
                        ((TableRenderer) res).setCellEditor(cellEditor);
                    }
                }
            }
        } else if (rendererName.startsWith(
                "com.azoft.nusuth.gui.FixArrayElementRenderer")) {
            String[] items = getStringsParam(rendererName);
            res = new FixArrayElementRenderer(items);
        } else if (rendererName.startsWith(
                "com.azoft.nusuth.gui.NumberElementRenderer")) {
            int maxValue = getIntParam(rendererName);
            if (maxValue != -1) {
                res = new NumberElementRenderer(maxValue);
            } else {
                res = new NumberElementRenderer();
            }
        } else if (rendererName.startsWith(
                "com.azoft.nusuth.gui.TextAreaElementRenderer")) {
            int rows = getIntParam(rendererName);
            if (rows != -1) {
                res = new TextAreaElementRenderer(rows);
            } else {
                res = new TextAreaElementRenderer();
            }
        } else {
            int indRaz = rendererName.indexOf(";");
            String type = "";
            if (indRaz != -1) {
                type = rendererName.substring(indRaz + 1);
                rendererName = rendererName.substring(0, indRaz);
            }
            res = getRendererInstance(rendererName);
            if (res instanceof ChangingValuesElementRenderer) {
                if (!type.equals("")) {
                    BasicPanel.addChangingValuesElementRenderer(type,
                            (ChangingValuesElementRenderer) res);
                }
            }
        }
        res.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                needSave = true;
            }
        });
        return res;
    }

    /**
     * Gets the String[] parameter after the divider
     * in the specified renderer name.
     *
     * @param   rendererName    the specified renderer name.
     * @return  the String[] parameter after the divider in the renderer name.
     */
    private static String[] getStringsParam(String rendererName) {
        String[] res = null;
        int indRaz = rendererName.indexOf(";");
        if (indRaz != -1) {
            String sParam = rendererName.substring(indRaz + 1).trim();
            StringTokenizer st = new StringTokenizer(sParam, ";");
            res = new String[st.countTokens()];
            int cnt = 0;
            while (st.hasMoreElements()) {
                String s = st.nextToken();
                res[cnt++] = (s.equals("_")) ? "" : s;
            }
        }
        return res;
    }

    /**
     * Gets the int parameter after the divider in the specified renderer name.
     *
     * @param   rendererName    the specified renderer name.
     * @return  the int parameter after the divider in the renderer name.
     */
    private static int getIntParam(String rendererName) {
        int res = -1;
        int indRaz = rendererName.indexOf(";");
        if (indRaz != -1) {
            String sParam = rendererName.substring(indRaz + 1).trim();
            try {
                res = Integer.parseInt(sParam);
            } catch (NumberFormatException ne) {
            }
        }
        return res;
    }

    /**
     * Gets the renderer instance by the specified renderer name.
     *
     * @param   rendererName  the specified renderer name.
     * @return  the renderer instance
     */
    private static ElementRenderer getRendererInstance(String rendererName) {
        ElementRenderer renderer = null;
        try {
            renderer = (ElementRenderer) Class.forName(rendererName).
                    getConstructor(null).newInstance(null);
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        } catch (ClassNotFoundException e) {
        }
        return renderer;
    }

    /**
     * Gets the cell editor by the specified element renderer.
     *
     * @param   elementRenderer   the specified element renderer.
     * @return  the cell editor by the renderer component.
     */
    private static CompositeCellEditor getCellEditor(
            ElementRenderer elementRenderer) {
        JComponent component = elementRenderer.getComponent();
        if (component instanceof JTextField) {
            return new CompositeCellEditor((JTextField) component);
        } else if (component instanceof JComboBox) {
            return new CompositeCellEditor((JComboBox) component);
        }
        return null;
    }

    /**
     * Gets the name of display component from the display properties.
     *
     * @param tag   the specified tag.
     * @return  the name of display component
     */
    public static String getDisplayCompName(String tag) {
        String prop = namesProps.getProperty(tag + ".displayComp");
        return (prop == null) ? tag : prop;
    }

    /**
     * Gets the column order string from the display properties.
     * It used in table panel factory for the table.
     *
     * @param path  the specified path.
     * @return  the column order string
     */
    public static String getOrder(String path) {
        return namesProps.getProperty(path + ".columnOrder", "");
    }

    /**
     * Gets the renderer name from the renderers properties.
     *
     * @param   tag   the specified tag.
     * @return  the renderer name
     */
    public static String getRendererName(String tag) {
        String prop = renderersProps.getProperty(tag);
        if (prop != null) {
            return prefix + prop;
        }
        int pindex = tag.indexOf(".");
        if (pindex != -1) {
            return getRendererName(tag.substring(pindex + 1));
        }
        return prefix + defaultRenderer;
    }

    /**
     * Gets the element type_names array for this editor panel.
     * By default - null.
     * Some subclasses can override this method.
     *
     * @return  the element type names array.
     */
    public String[] getElementNames() {
        return null;
    }

    /**
     * Saves this component to the server via the basic panel proxy.
     */
    public void saveComponent() throws UnauthorizedAccessException,
            ManagementException, AccessDeniedException {
        // save the elements
        ConfigMutableTreeNode elNode =
                (ConfigMutableTreeNode) getTreeNode().getElementNode();
        if (elNode == null || getTreeNode() == elNode) {
            if (getType().startsWith(BasicPanel.SSECURITY)) {
                basicPanel.proxy.setSecuritySettings(basicPanel.security_config);
            } else if (getType().equals("hosts")
                    || getType().equals(BasicPanel.SHOST)) {
                basicPanel.proxy.
                        setApplicationsDeployment(basicPanel.application_deployment);
            } else {
                String componentId = getTreeNode().getComponentId();
                basicPanel.proxy.setComponentSettings(
                        getType(), componentId, getWebElement());
            }
        } else {
            String type = elNode.getType();
            EditorPanel ep = basicPanel.getEditorPanel(type);
            ep.setTreeNode(elNode);
            ep.setEntry(elNode.getUserObject());
            ep.saveComponent();
        }
    }
}
