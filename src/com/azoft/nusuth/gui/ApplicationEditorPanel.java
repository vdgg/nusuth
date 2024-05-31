/*
 * @(#)ApplicationEditorPanel.java 1.0 09/11/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.security.AccessDeniedException;
import com.azoft.nusuth.management.ManagementException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * Class ApplicationEditorPanel.
 *
 * @version 1.0 09/11/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ApplicationEditorPanel extends CustomEditorPanel {

    /**
     * The context element name.
     */
    public final static String contextName = BasicPanel.SHOST + ".context";

    /**
     * The element names array.
     */
    public final static String[] APP_EL_NAMES =
            {BasicPanel.SWEB_APP, contextName};

    /**
     * Child names in basics tab.
     */
    protected static String[] BASICS_FACS = {contextName, "session-config",
                                             "icon", "welcome-file-list", "context-param", "listener"};

    /**
     * The hash: tab name -> array of childs.
     */
    protected static Hashtable TAB_CONTENTS;

    /**
     * fs mapping tab name
     */
    private final static String FS_MAPPING = "fs-mappings";

    /**
     * error-page tab name
     */
    private final static String ERROR_PAGE = "error-pages";

    /**
     * mime-mapping tab name
     */
    private final static String MIME_MAPPING = "mime-mappings";

    /**
     * taglib tab name
     */
    private final static String TAGLIB = "taglibs";

    /**
     * The tabs in this editor panel (after the summary & basics).
     */
    protected static String[] TABS =
            {FS_MAPPING, ERROR_PAGE, MIME_MAPPING, TAGLIB};

    /**
     * Child names in fs-mapping tab.
     */
    private final static String[] FS_MAPPING_FACS =
            {"servlet-mapping", "filter-mapping"};

    /**
     * Child names in error-page tab.
     */
    private final static String[] ERROR_PAGE_FACS = {"error-page"};

    /**
     * Child names in mime-mapping tab.
     */
    private final static String[] MIME_MAPPING_FACS = {"mime-mapping"};

    /**
     * Child names in taglib tab.
     */
    private final static String[] TAGLIB_FACS = {"taglib"};

    /**
     * The entry of this editor panel.
     */
    protected Hashtable entry;

    /**
     * The context element.
     */
    CompositeNusuthWebAppElement contextElement;


    static {
        TAB_CONTENTS = new Hashtable();
        TAB_CONTENTS.put(BASICS, BASICS_FACS);
        TAB_CONTENTS.put(FS_MAPPING, FS_MAPPING_FACS);
        TAB_CONTENTS.put(ERROR_PAGE, ERROR_PAGE_FACS);
        TAB_CONTENTS.put(MIME_MAPPING, MIME_MAPPING_FACS);
        TAB_CONTENTS.put(TAGLIB, TAGLIB_FACS);
        USED_COMPOSITE_CHILDS = new Vector();
        Enumeration en = TAB_CONTENTS.elements();
        while (en.hasMoreElements()) {
            String[] childs = (String[]) en.nextElement();
            for (int i = 0; i < childs.length; i++) {
                USED_COMPOSITE_CHILDS.addElement(childs[i]);
            }
        }
        ELEMENT_TYPE = BasicPanel.SWEB_APP;
    }


    /**
     * Constructs a new application editor panel.
     * It's the default editor with app type.
     */
    public ApplicationEditorPanel() {
        this(BasicPanel.SAPP);
    }

    /**
     * Constructs a new default editor panel with the specified type.
     */
    public ApplicationEditorPanel(String type) {
        super(type);
    }

    /**
     * Gets the display string for this panel.
     *
     * @return  the display string for this panel.
     * @see #getDisplay(path, CompositeNusuthWebAppElement)
     */
    public String getDisplay() {
        String display_name =
                getWebElementDisplay(BasicPanel.SWEB_APP, this.webElement);
        if (display_name != null) {
            return display_name;
        }
        return getDisplay(getCompositePanelFactory(contextName).getType(),
                this.contextElement);
    }

    /**
     * Gets the display string for the web element.
     * Gets null in all cases, except display comp content is not empty.
     *
     * @param   path          the specified path
     * @param   webElement    the specified web element
     * @return  the display string
     */
    public static String getWebElementDisplay(String path,
                                              CompositeNusuthWebAppElement webElement) {
        String s = namesProps.getProperty(path + ".displayComp");
        if (s != null) {
            StringTokenizer st = new StringTokenizer(s.trim(), ",");
            Enumeration enum;
            while (st.hasMoreTokens()) {
                String childName = st.nextToken().trim();
                try {
                    enum = webElement.getSimpleChild(childName);
                    if (enum.hasMoreElements()) {
                        String content = ((SimpleNusuthWebAppElement) enum.nextElement()).
                                getContent();
                        if (!content.equals("")) return content;
                    }
                } catch (DeploymentException dex) {
                } catch (NullPointerException npex) {
                }
            }
        }
        return null;
    }

    /**
     * Gets the popup menu for this editor.
     *
     * @return  the popup menu for this editor.
     */
    public JPopupMenu getPopupMenu() {
        if (menu == null) {
            menu = new JPopupMenu();
            JMenuItem item = menu.add("add servlet");
            item.addActionListener(this);
            item = menu.add("add filter");
            item.addActionListener(this);
            item = menu.add("remove application");
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
        if (e.getActionCommand().equals("remove application")) {
            if (!basicPanel.canDeleteItems() || basicPanel.unauthorized) {
                ManageTool.showMessage("You can't remove the component");
                return;
            }
            WebAppMutableTreeNode appNode = (WebAppMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            ConfigMutableTreeNode hostNode =
                    (ConfigMutableTreeNode) appNode.getParent();
            CompositeNusuthWebAppElement hostElement =
                    (CompositeNusuthWebAppElement) hostNode.getUserObject();
            if (hostElement != null && contextElement != null) {
                try {
                    hostElement.removeCompositeChild("context", contextElement);
                    basicPanel.removeContextFromHost(hostNode, appNode.toString());
                    hostNode.remove(appNode);
                    basicPanel.splitPane.setRightComponent(new JLabel(""));
                    basicPanel.reloadTree();
                    // save component to server
                    EditorPanel parentPanel = basicPanel.getEditorPanel(hostNode.getType());
                    parentPanel.setTreeNode(hostNode);
                    parentPanel.setEntry(hostElement);
                    parentPanel.save();
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
        } else if (e.getActionCommand().toLowerCase().startsWith("add ")) {
            if (!basicPanel.canAddItems() || basicPanel.unauthorized) {
                ManageTool.showMessage("You can't add the component");
                return;
            }
            JMenuItem mit = (JMenuItem) e.getSource();
            String childName = mit.getActionCommand().substring(4);
            String childType = BasicPanel.SWEB_APP + "." + childName;
            WebAppMutableTreeNode appNode = (WebAppMutableTreeNode)
                    basicPanel.tree.getLastSelectedPathComponent();
            if (webElement != null) {
                try {
                    CompositeNusuthWebAppElement childElement =
                            webElement.addCompositeChild(childName);
                    System.out.println("servlet element = ");
                    System.out.println(childElement.toString());
                    String nodeName = getDisplay(childType, childElement);
                    WebAppMutableTreeNode sn =
                            new WebAppMutableTreeNode(nodeName, childType);
                    sn.setUserObject(childElement);
                    sn.setElementNode(appNode.getElementNode());
                    basicPanel.getTreeNode(appNode, childName).add(sn);
                    basicPanel.reloadTree();
                    String newName = basicPanel.getEditorPanel(childType).getDisplay();
                    basicPanel.fireValueAdded(childName, newName);
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
        }
    }

    /**
     * Sets the specified entry to this editor.
     * The entry is hash with web-app & context elements.
     *
     * @param   entry   the specified entry
     */
    public void setEntry(Object entry) {
        this.entry = (Hashtable) entry;
        this.webElement = (CompositeNusuthWebAppElement)
                this.entry.get(BasicPanel.SWEB_APP);
        this.contextElement = (CompositeNusuthWebAppElement)
                this.entry.get(contextName);
    }

    /**
     * Gets the entry of this editor.
     *
     * @return  the entry of this editor.
     * @see #setEntry(Object)
     */
    public Object getEntry() {
        return this.entry;
    }

    /**
     * Overrides the super method.
     * Updates the web element fields by web element &
     * context fields by context element.
     */
    protected void updateAllControlls() {
        // updates web element fields
        super.updateAllControlls();
        // updates context element fields
        if (contextElement != null) {
            getCompositePanelFactory(contextName).updateControls(contextElement);
        }
    }

    /**
     * Overrides super method to add the context element summary
     */
    protected void addComponentLabels() {
        addComponentLabels(viewPanel, contextElement, true);
        addComponentLabels(viewPanel, webElement);
    }

    /**
     * Overrides the super method.
     * Updates all web element (web element & context element) by the panel fields
     */
    protected void updateAllElements() {
        // updates web element
        super.updateAllElements();
        // updates context element
        updateContextElement(contextElement);
    }

    /**
     * Updates the specified context element by fields.
     *
     * @param   contextElement  the specified context element
     */
    void updateContextElement(CompositeNusuthWebAppElement contextElement) {
        if (contextElement != null) {
            getCompositePanelFactory(contextName).updateElement(contextElement);
        }
    }

    /**
     * Overides the super method.
     * Not checks the unique.
     *
     * @param lastD   the last display content
     * @param newD    the new display content
     * @return  <code>true</code> if name is unique;
     * <code>false</code> otherwise.
     */
    protected boolean checkUnique(String lastD, String newD) {
        return true;
    }

    /**
     * Gets the componentId component content.
     * In this class component must be in context.
     *
     * @return  the componentId component content.
     * @see #getComponentId(String, CompositeNusuthWebAppElement)
     */
    protected String getComponentId() {
        return getComponentId(contextName, this.contextElement);
    }

    /**
     * Gets the enumeration of the child factories.
     * Overrides this method in CompositeElementPanelFactory -
     * adds only necessary factories.
     *
     * @return  the enumeration of the child factories.
     */
    public Enumeration getCompositePanelFactories() {
        if (webElement == null) return null;
        if (compositeFactories == null) {
            compositeFactories = new Hashtable();
            Enumeration e = USED_COMPOSITE_CHILDS.elements();
            while (e.hasMoreElements()) {
                String childName = (String) e.nextElement();
                CompositeElementPanelFactory factory = null;
                if (childName.equals(contextName)) {
                    if (contextElement == null) {
                        contextElement = BasicPanel.getCompositeElement(contextName);
                    }
                    factory =
                            new CompositeElementPanelFactory(contextName, contextElement);
                } else {
                    try {
                        if (webElement.isChildUnbounded(childName)) {
                            if (childName.equals("listener")) {
                                factory = new TextAreaPanelFactory(
                                        ELEMENT_TYPE + "." + childName, childName, webElement);
                            } else {
                                factory = new TablePanelFactory(
                                        BasicPanel.SWEB_APP + "." + childName, childName, webElement);
                            }
                        } else {
                            Enumeration en = webElement.getCompositeChild(childName);
                            CompositeNusuthWebAppElement el = null;
                            if (en != null && en.hasMoreElements()) {
                                el = (CompositeNusuthWebAppElement) en.nextElement();
                            }
                            factory = new CompositeElementPanelFactory(
                                    BasicPanel.SWEB_APP + "." + el.getTag(), el);
                            factory.setRequired(webElement.isChildRequired(childName));
                        }
                    } catch (DeploymentException de) {
                        System.out.println(de);
                    } catch (NullPointerException npe) {
                        System.out.println(npe);
                    }
                }
                if (factory != null) {
                    factory.setIndividualTab(!basicsContains(childName));
                    compositeFactories.put(childName, factory);
                }
            }
        }
        return compositeFactories.elements();
    }

    /**
     * Gets the basics tab contains the specified child factory or not.
     * Used the FACS_IN_BASICS array.
     *
     * @param   childFacName  the specified child factory name.
     * @return  <code>true</code> if the basics tab contains the specified child
     * factory; <code>false</code> otherwise.
     */
    protected boolean basicsContains(String childFacName) {
        int cnt = BASICS_FACS.length;
        for (int i = 0; i < cnt; i++) {
            if (BASICS_FACS[i].equals(childFacName)) {
                return true;
            }
        }
        return false;
    }

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
        if (emptyChildFactory == null || !emptyChildFactory.isIndividualTab()) {
            if (createPanel() instanceof JTabbedPane) {
                int basicsIndex =
                        ((JTabbedPane) createPanel()).indexOfTab(EditorPanel.BASICS);
                ((JTabbedPane) createPanel()).setSelectedIndex(basicsIndex);
            }
            emptyRequestFocus();
        } else {
            try {
                Component comp = getFactoryTabComponent(((JTabbedPane) createPanel()),
                        emptyChildFactory);
                ((JTabbedPane) createPanel()).setSelectedComponent(comp);
                emptyChildFactory.gotoEmpty();
            } catch (Exception e) {
                System.out.println("cast exception when goto " + e);
            }
        }
    }

    /**
     * Gets the tab component for the specified factory
     * in the specified tabbed pane.
     *
     * @param   pane    the specified tabbed pane.
     * @param   fac     the specified factory.
     * @return  the tab component for the factory in the tabbed pane.
     */
    protected Component getFactoryTabComponent(JTabbedPane pane,
                                               CompositeElementPanelFactory fac) {

        String tag = fac.getType();
        int pindex = tag.lastIndexOf(".");
        tag = (pindex == -1) ? tag : tag.substring(pindex + 1);
        Enumeration en = TAB_CONTENTS.keys();
        while (en.hasMoreElements()) {
            String tabTitle = (String) en.nextElement();
            String[] tags = (String[]) TAB_CONTENTS.get(tabTitle);
            int cnt = tags.length;
            for (int i = 0; i < cnt; i++) {
                if (tag.equals(tags[i])) {
                    int index = pane.indexOfTab(tabTitle);
                    return pane.getComponentAt(index);
                }
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

            int cnt = TABS.length;
            for (int i = 0; i < cnt; i++) {
                tabbedPane.addTab(TABS[i], getTabPanel(TABS[i]));
            }
        }
        return tabbedPane;
    }

    /**
     * Gets the panel for the specified tab name.
     *
     * @param   tabName   the specified tab name.
     * @return  the panel for the specified tab name.
     */
    protected JComponent getTabPanel(String tabName) {
        String[] childs = (String[]) TAB_CONTENTS.get(tabName);
        int childsCnt = childs.length;
        if (childsCnt == 1) {
            CompositeElementPanelFactory fac = getCompositePanelFactory(childs[0]);
            if (fac != null) {
                return fac.createPanel();
            }
        } else if (childsCnt > 1) {
            JPanel res = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(10, 0, 0, 0);
            for (int j = 0; j < childsCnt; j++) {
                String nextChild = childs[j];
                CompositeElementPanelFactory fac = getCompositePanelFactory(nextChild);
                if (fac != null) {
                    if (fac instanceof TablePanelFactory) {
                        addTableToFocused(((TablePanelFactory) fac).getTable());
                    }
                    String label = DefaultEditorPanel.getDisplayName(nextChild);
                    JComponent comp = fac.createPanel();
                    comp.setBorder(new TitledBorder(label));
                    res.add(comp, c);
                }
            }
            return res;
        }
        return new JPanel();
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
            c.weightx = 0.0;
            c.weighty = 0.1;
            c.fill = GridBagConstraints.VERTICAL;
            c.insets = new Insets(7, 7, 7, 7);
            simplePanel.add(getContainerPanel(), c);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(15, 0, 9, 9);
            simplePanel.add(getDisplayDescriptionPanel(), c);

            c.weighty = 1;
            c.insets = new Insets(0, 7, 7, 7);
            simplePanel.add(getContextParamPanel(), c);
        }
        return simplePanel;
    }

    /**
     * Panel for containers.
     * Contains the context elements, simple elements,
     * session-timeout from session-config, icons, welcome-files, listeners.
     *
     * @return  the first large panel
     */
    private JPanel getContainerPanel() {
        JPanel res = new JPanel(new GridBagLayout()) {
            public Dimension getPreferredSize() {
                int wid = (int) (0.5 * (simplePanel.getSize().width));
                int hei = super.getPreferredSize().height;
                return new Dimension(wid, hei);
            }

            public Dimension getMaximumSize() {
                int wid = (int) (0.5 * (simplePanel.getSize().width));
                int hei = super.getMaximumSize().height;
                return new Dimension(wid, hei);
            }

            public Dimension getMinimumSize() {
                int wid = (int) (0.5 * (simplePanel.getSize().width));
                int hei = super.getMinimumSize().height;
                return new Dimension(wid, hei);
            }
        };
        res.setBorder(new TitledBorder("Container"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        res.add(getStartedEls(), c);
        res.add(getDialogEls(), c);
        res.add(getDistributableEls(), c);
        res.add(getContextContainersEls(), c);
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        res.add(new JPanel(), c);
        return res;
    }

    /**
     * The started els panel.
     * Contains the started el & the protocol.
     *
     * @return  the started els panel
     */
    private JPanel getStartedEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.fill = GridBagConstraints.NONE;
        addElement(getCompositePanelFactory(contextName),
                "is-running", true, res, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        addElement(getCompositePanelFactory(contextName),
                "protocol", true, res, c, "none");
        return res;
    }

    /**
     * The dialog els panel.
     * Contains the location, url & session-timeout els.
     *
     * @return  the dialog els panel
     */
    private JPanel getDialogEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(getCompositePanelFactory(contextName),
                "location", true, res, c);
        addElement(getCompositePanelFactory(contextName),
                "url", true, res, c);
        addElement(getCompositePanelFactory("session-config"),
                "session-timeout", false, res, c);
        return res;
    }

    /**
     * The distributable els panel.
     * Contains the distributable el.
     *
     * @return  the distributable els panel
     */
    private JPanel getDistributableEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        addElement(this, "distributable", true, res, c);
        return res;
    }

    /**
     * The context containers els panel.
     * Contains the context containers.
     *
     * @return  the context containers els panel
     */
    private JPanel getContextContainersEls() {
        JPanel res = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 15, 5, 0);
        addElement(getCompositePanelFactory(contextName),
                "container", true, res, c);
        ElementRenderer renderer = (ElementRenderer) renderers.get("distributable");
        if (renderer instanceof Activator) {
            ElementRenderer activatingRenderer = (ElementRenderer)
                    getCompositePanelFactory(contextName).renderers.get("container");
            if (activatingRenderer instanceof Activating) {
                ((Activator) renderer).addActivating((Activating) activatingRenderer);
            }
        }
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
        addElement(getCompositePanelFactory("welcome-file-list"),
                "welcome-file", false, res, c, "up");
        addElement(getCompositePanelFactory("listener"),
                "listener-class", false, res, c, "up");
        return res;
    }

    /**
     * Context params panel.
     * Contains context params.
     *
     * @return  the context params panel
     */
    private JPanel getContextParamPanel() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Context initial parameters"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        TablePanelFactory contextParamFac =
                (TablePanelFactory) getCompositePanelFactory("context-param");
        res.add(contextParamFac.getMainPane(), c);
        return res;
    }

    /**
     * Gets the context element of this editor panel.
     *
     * @return  the context element of this editor panel.
     */
    public CompositeNusuthWebAppElement getContextElement() {
        return this.contextElement;
    }

    /**
     * Gets the element type_names array for this editor panel.
     * Overrides the super method.
     *
     * @return  the element type names array.
     */
    public String[] getElementNames() {
        return APP_EL_NAMES;
    }

    /**
     * Saves this component to the server via the basic panel proxy.
     */
    public void saveComponent() throws UnauthorizedAccessException,
            ManagementException, AccessDeniedException {
        System.out.println("****** save component in application panel");
        // save the context elemenent
        basicPanel.proxy.
                setApplicationsDeployment(basicPanel.application_deployment);

        // save the web-app element
        String componentId = getTreeNode().getComponentId();
        basicPanel.proxy.setComponentSettings(
                BasicPanel.SWEB_APP, componentId, getWebElement());
    }
}
