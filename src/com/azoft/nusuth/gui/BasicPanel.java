/*
 * @(#)BasicPanel.java 1.0 12/03/2000
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.*;
import com.azoft.nusuth.management.rmi.*;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;

import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.lang.reflect.*;
import java.io.IOException;

/**
 * Class BasicPanel is the main class for editing, monitoring, claster viewing.
 *
 * @version 1.0 12/03/2000
 * @author  vdgg, tanya
 * @since Nusuth1.0
 */
public class BasicPanel extends JTabbedPane implements TreeSelectionListener,
        TreeWillExpandListener, NameChangedListener {

    /**
     * The distributor type.
     */
    public final static String SDISTRIBUTOR = ComponentType.SDISTRIBUTOR;

    /**
     * The container type.
     */
    public final static String SCONTAINER = ComponentType.SCONTAINER;

    /**
     * The web applicaiton type.
     */
    public final static String SWEB_APP = ComponentType.SAPPLICATION;

    /**
     * The type for node with context & web application.
     */
    public final static String SAPP = "application";

    /**
     * The application deployment type.
     */
    public final static String SAPP_DEP = "application-deployment";

    /**
     * The host type.
     */
    public final static String SHOST = "application-deployment.host";

    /**
     * The servlet type.
     */
    public final static String SSERVLET = "web-app.servlet";

    /**
     * The filter type.
     */
    public final static String SFILTER = "web-app.filter";

    /**
     * The type for security in web application.
     */
    public final static String SWEBSECURITY = "web-app.securitySettings";

    /**
     * The type for jndi in web application.
     */
    public final static String SWEBJNDI = "web-app.jndi";

    /**
     * The type for tagibs in web application.
     */
    public final static String SWEBTAGLIBS = "taglibs";

    /**
     * The type for tagib in web application.
     */
    public final static String SWEBTAGLIB = "taglib";

    /**
     * The type for tag in web application's taglib.
     */
    public final static String SWEBTAG = "taglib.tag";

    /**
     * The type for users in web application.
     */
    public final static String SWEBUSERS = "web-app-users";

    /**
     * The security config type.
     */
    public final static String SSECURITY = "securityConfig";

    /**
     * The security resources type.
     */
    public final static String SRESOURCES = "securityConfig.resources";

    /**
     * The security users type.
     */
    public final static String SUSERS = "securityConfig.users";

    /**
     * The security distributors type.
     */
    public final static String SDISTRIBUTORS =
            "securityConfig.resources.distributors";

    /**
     * The security containers type.
     */
    public final static String SCONTAINERS =
            "securityConfig.resources.containers";

    /**
     * The security deployers type.
     */
    public final static String SDEPLOYERS = "securityConfig.resources.deployers";

    /**
     * The security web applications type.
     */
    public final static String SWEB_APPS = "securityConfig.resources.web-apps";

    /**
     * The security distributor type.
     */
    public final static String SSECURITY_DISTRIBUTOR =
            "securityConfig.resources.distributors.distributor";

    /**
     * The security container type.
     */
    public final static String SSECURITY_CONTAINER =
            "securityConfig.resources.containers.container";

    /**
     * The security deployer type.
     */
    public final static String SSECURITY_DEPLOYER =
            "securityConfig.resources.deployers.deployer";

    /**
     * The security web application type.
     */
    public final static String SSECURITY_WEB_APP =
            "securityConfig.resources.web-apps.web-app";

    /**
     * The security group type.
     */
    public final static String SGROUP = "securityConfig.users.group";

    /**
     * The security user type.
     */
    public final static String SUSER = "securityConfig.users.user";

    /**
     * Types with own tree nodes.
     */
    public final static String[] nodesType = {SDISTRIBUTOR, SCONTAINER}; //, SDEPLOYER};

    /**
     * Functions properties.
     */
    public static Properties functionsProps;

    /**
     * The monitors container.
     */
    public static MonitorContainer monitorContainer;

    /**
     * Defines the current user is authorized or not.
     */
    public static boolean unauthorized = false;


    /**
     * The proxy.
     */
    static RmiClusterManagerProxyImpl proxy;

    /**
     * The type -> editor factory hash
     */
    private static Hashtable type2factory = new Hashtable();

    /**
     * Application expand properties.
     */
    private static Properties applicationProps;

    /**
     * hash of vectors of renderers
     */
    private static Hashtable changingValuesRenderers = new Hashtable();

    /**
     * hash of vectors of values
     */
    private static Hashtable changingValues = new Hashtable();

    /**
     * hash: host tree node -> vector of apps
     */
    private static Hashtable hostApplications = new Hashtable();

    /**
     * hash: type -> parent node (only for distributor, container, deployer)
     */
    private static Hashtable nodes = new Hashtable();

    /**
     * hash: type -> panel
     */
    private static Hashtable panels = new Hashtable();

    /**
     * The auxiliary element factory.
     */
    private static CompositeElementFactory
            elementFactory = new CompositeElementFactory();

    /**
     * Defines the current user can set config or not.
     */
    private static boolean canSetCongif = false;

    /**
     * Stores the admin password.
     */
    private static String adminPassword = null;


    /**
     * The main editor panel - split pane.
     */
    public JSplitPane splitPane;

    /**
     * The cluster view panel.
     */
    public ClusterViewPanel clusterViewPanel;

    /**
     * The main tree.
     */
    public JTree tree;

    /**
     * The last selected tree node.
     */
    public DefaultMutableTreeNode lastTreeNode = null;

    /**
     * The last selected tree path.
     */
    public TreePath lastTreePath = null;

    /**
     * The status writer.
     * Writes all status messages.
     */
    StatusLineWriter status;

    /**
     * The current application deployment.
     */
    CompositeNusuthWebAppElement application_deployment;

    /**
     * The current security config.
     */
    CompositeNusuthWebAppElement security_config;


    /**
     * Tmp component info array.
     */
    private ComponentInfo[] cinfo;

    /**
     * The boolean value.
     * If current panel is not saved & continue editing is choosed -
     * this variable will be <code>true</code>.
     */
    private boolean needToReturnLastPath = false;

    /**
     * Defines the last editor panel.
     */
    private EditorPanel lastEditorPanel;

    /**
     * Defines the last loaded application.
     */
    private TreeNode lastLoadedApplication = null;

    /**
     * The current tab's page.
     */
    private Component currentPage;

    /**
     * The current resources.
     * Used for a rights defining.
     */
    private CompositeNusuthWebAppElement resources;

    /**
     * The current users.
     * Used for a rights defining.
     */
    private CompositeNusuthWebAppElement users;

    /**
     * Defines the current user can add component or not.
     */
    private boolean canAdd = false;

    /**
     * Defines the current user can remove component or not.
     */
    private boolean canDelete = false;

    /**
     * The parent vector.
     * The auxiliary vector.
     */
    private Vector parentVector = new Vector();

    /**
     * The component changed listeners.
     */
    private Vector componentChangedListeners = new Vector();

    /**
     * tmp composite element.
     */
    private CompositeNusuthWebAppElement tmpCompositeElement = null;

    /**
     * the cell renderer for the tree.
     */
    private ConfigTreeCellRenderer configTreeCellRenderer;


    /**
     * application expand properties loading.
     */
    static {
        applicationProps = new Properties();
        try {
            applicationProps.load(ClassLoader.getSystemResourceAsStream(
                    "com/azoft/nusuth/gui/applicationExpand.properties"));
        } catch (IOException e) {
            System.out.println("can not load application properties");
        }
        type2factory.put(SCONTAINER, new ContainerEditorFactoryImpl());
        type2factory.put(SDISTRIBUTOR, new DistributorEditorFactoryImpl());
        type2factory.put(SAPP, new ApplicationEditorFactoryImpl());
        type2factory.put(SWEBSECURITY, new WebSecurityEditorFactoryImpl());
        type2factory.put(SWEBJNDI, new WebJndiEditorFactoryImpl());
        type2factory.put(SSERVLET, new ServletEditorFactoryImpl());
        type2factory.put(SFILTER, new FilterEditorFactoryImpl());
        type2factory.put(SWEBUSERS, new WebAppUsersEditorFactoryImpl());
        type2factory.put(SWEBTAGLIBS, new TaglibsEditorFactoryImpl());
        type2factory.put(SWEBTAGLIB, new TaglibEditorFactoryImpl());
        type2factory.put(SWEBTAG, new TagEditorFactoryImpl());
        type2factory.put(SAPP_DEP, new AppDepEditorFactoryImpl());
        type2factory.put(SHOST, new HostEditorFactoryImpl());
        type2factory.put(SSECURITY, new SecurityConfigEditorFactoryImpl());
    }


    /**
     * Constructs a new basic panel with the specified
     * split pane divider location, status writer.
     * Inits the split pane, monitoring container, cluster view panel.
     * Adds them as tabs.
     * Cteates the main tree, loads the necessary properties,
     * sets the necessary listeners.
     *
     * @param   divLoc            the split pane divider location.
     * @param   status            the status writer.
     * @param   monitorProps      the properties for monitoring.
     * @param   clusterViewProps  the properties for cluster view.
     */
    public BasicPanel(int divLoc, StatusLineWriter status,
                      Properties monitorProps, Properties clusterViewProps) {
        super(JTabbedPane.BOTTOM);
        this.proxy = ManageTool.getProxy();
        this.status = status;
        removeAllContextFromAllHost();
        initSplitPane(divLoc);
        addTab("Editing", splitPane);
        monitorContainer = new MonitorContainer(this, monitorProps);
        addTab("Monitoring", monitorContainer);
        clusterViewPanel = new ClusterViewPanel(this, clusterViewProps);
        addTab("Cluster View", clusterViewPanel);
        setSelectedIndex(0);
        createTabListener();
        loadProperties();
        ManageTool.getMonitorMenu().setEnabled(false);
        ManageTool.getClusterViewMenu().setEnabled(false);
        DefaultEditorPanel.setNameChangedListener(this);
        ConfigMutableTreeNode.setBasicPanel(this);
        DefaultEditorPanel.setBasicPanel(this);
        EmptyEditorPanel.setBasicPanel(this);
    }

    /**
     * Inits the split pane - the main pane for editor.
     * Creates the main tree.
     *
     * @param   divLoc    the split pane divider location.
     */
    private void initSplitPane(int divLoc) {
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDoubleBuffered(true);
        buildTree();
        splitPane.setLeftComponent(new JScrollPane(tree));
        splitPane.setRightComponent(new JLabel(""));
        splitPane.setDividerSize(2);
        if (divLoc >= 0) splitPane.setDividerLocation(divLoc);
    }

    /**
     * Gets the editor panel with the specified type.
     * If panel with necessary type is not exists it creates a new panel.
     *
     * @param   type    the specified type.
     * @return  the editor panel with the specified type.
     * @see #createEditorPanel(String)
     */
    EditorPanel getEditorPanel(String type) {
        EditorPanel panel = (EditorPanel) panels.get(type);
        if (panel == null) {
            panel = createEditorPanel(type);
            if (panel != null) {
                panels.put(type, panel);
            }
        }
        return panel;
    }

    /**
     * Creates a new editor panel by the specified type.
     *
     * @param   type    the specified type.
     * @return  a new editor panel by the specified type.
     * @see #getEditorPanel(String)
     */
    private EditorPanel createEditorPanel(String type) {
        EditorFactory factory = (EditorFactory) type2factory.get(type);
        return (factory != null)
                ? factory.createEditorPanel() : new DefaultEditorPanel(type);
    }

    /**
     * Creates the tree.
     * Adds all necessary listeners & default nodes for containers & distributors.
     */
    private void buildTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        tree = new JTree(root);
        tree.setRootVisible(false);
        configTreeCellRenderer = new ConfigTreeCellRenderer();
        tree.setCellRenderer(configTreeCellRenderer);
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(this);
        tree.addTreeWillExpandListener(this);
        tree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.getModifiers() == MouseEvent.BUTTON3_MASK) {
                    TreePath path = ((JTree) e.getComponent()).
                            getPathForLocation(e.getX(), e.getY());
                    if (needToReturnLastPath)
                        tree.setSelectionPath(lastTreePath);
                    else {
                        tree.setSelectionPath(path);
                        if (path != null) {
                            Object obj = path.getLastPathComponent();
                            if (obj != null && obj instanceof ConfigMutableTreeNode) {
                                ConfigMutableTreeNode node = (ConfigMutableTreeNode) obj;
                                JPopupMenu popup = null;
                                popup = getEditorPanel(node.getType()).getPopupMenu();
                                if (popup != null) {
                                    popup.show(e.getComponent(), e.getX(), e.getY());
                                }
                            }
                        }
                    }
                } else if (e.getModifiers() == MouseEvent.BUTTON1_MASK) {
                    TreePath path = ((JTree) e.getComponent()).
                            getPathForLocation(e.getX(), e.getY());
                    if (needToReturnLastPath)
                        tree.setSelectionPath(lastTreePath);
                    else
                        tree.setSelectionPath(path);
                }
            }
        });
        for (int i = 0; i < nodesType.length; i++) {
            String name = nodesType[i];
            String names = (name.endsWith("y"))
                    ? name.substring(0, name.length() - 1) + "es" : name + "s";
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(names);
            nodes.put(name, node);
            root.add(node);
        }
        reloadTree();
    }

    /**
     * Starts all threads for a component loading.
     *
     * @param   parentComponent   the parent component for dialogs.
     */
    public void startThreads(final Component parentComponent) {
        clearTree();
        clearAllChangingValues();
        MessageDialog.setMessage("Retrieving components...");
        setRegisteredComponent(parentComponent);
        if (unauthorized) return;
        MessageDialog.setMessage("Retrieving applications...");
        setApplicationsDeployment(parentComponent);
        if (unauthorized) return;
        MessageDialog.setMessage("Retrieving security settings...");
        setSecuritySettings(parentComponent);
        checkAddDeletePermission();
        setAdminPassword();
        reloadTree();
        status.clearStatusString();
    }

    /**
     * Clears the main tree.
     */
    public void clearTree() {
        DefaultMutableTreeNode root =
                (DefaultMutableTreeNode) (tree.getModel()).getRoot();
        root.removeAllChildren();
        for (Enumeration en = nodes.elements(); en.hasMoreElements();) {
            DefaultMutableTreeNode nextNode =
                    (DefaultMutableTreeNode) en.nextElement();
            nextNode.removeAllChildren();
            root.add(nextNode);
        }
        int cnt = tree.getRowCount();
        for (int i = 0; i < cnt; i++) {
            tree.collapseRow(i);
        }
        reloadTree();
        unauthorized = false;
        int pos = this.splitPane.getDividerLocation();
        this.splitPane.setRightComponent(new JLabel(""));
        this.splitPane.setDividerLocation(pos);
    }

    /**
     * Clears all changing values with the exception of functions.
     */
    private void clearAllChangingValues() {
        for (Enumeration e = changingValues.keys(); e.hasMoreElements();) {
            String type = (String) e.nextElement();
            if (type.equals("distributorFunction") || type.equals("containerFunction"))
                continue;
            fireAllValueRemoved(type);
        }
    }

    /**
     * Gets the registered components from the proxy, creates a new tree nodes
     * & adds them to the tree.
     *
     * @param   parentComponent   the parent component for dialogs.
     */
    private void setRegisteredComponent(final Component parentComponent) {
        RequestThread rt = new RequestThread() {
            protected void doWork() throws Exception {
                // can throw management || unath exc
                BasicPanel.this.cinfo = proxy.getRegisteredComponents();
            }
        };
        rt.setRequestThreadListener(new RequestThreadListener() {
            public void workFinished(RequestThreadEvent e) {
                if (!e.isSuccess()) {
                    if (e.isUnauthorized()) {
                        int res = showLoginDialog(parentComponent);
                        if (res == JOptionPane.NO_OPTION
                                || res == JOptionPane.CLOSED_OPTION) {
                            status.setStatusString("You are not authorized ...");
                            unauthorized = true;
                            return;
                        }
                        BasicPanel.this.setRegisteredComponent(parentComponent);
                    } else {
                        unauthorized = false;
                        System.out.println(e.getException());
                        if (new RetryDialog(parentComponent,
                                "Cannot retrieve components").isRetry())
                            BasicPanel.this.setRegisteredComponent(parentComponent);
                    }
                } else {
                    unauthorized = false;
                    if (BasicPanel.this.cinfo != null) {
                        for (int i = 0; i < BasicPanel.this.cinfo.length; i++) {
                            String typeName = BasicPanel.this.cinfo[i].getComponentTypeName();
                            DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) nodes.get(typeName);
                            if (node != null) {
                                String componentId = BasicPanel.this.cinfo[i].getComponentId();
                                node.add(new ConfigMutableTreeNode(componentId, typeName));
                                // now here will be only SCONTAINER & SDISTRIBUTOR !!!!
                                fireValueAdded(typeName, componentId, componentId);
                            }
                        }
                    }
                }
            }
        });
        rt.run();
    }

    /**
     * Gets the applicaion deployment from the proxy, creates a new necessary
     * tree nodes & adds them to the tree.
     *
     * @param   parentComponent   the parent component for dialogs.
     */
    private void setApplicationsDeployment(final Component parentComponent) {
        RequestThread rt = new RequestThread() {
            protected void doWork() throws Exception {
                BasicPanel.this.application_deployment =
                        proxy.getApplicationsDeployment();
            }
        };
        rt.setRequestThreadListener(new RequestThreadListener() {
            public void workFinished(RequestThreadEvent e) {
                if (!e.isSuccess()) {
                    if (e.isUnauthorized()) {
                        int res = showLoginDialog(parentComponent);
                        if (res == JOptionPane.NO_OPTION
                                || res == JOptionPane.CLOSED_OPTION) {
                            status.setStatusString("You are not authorized ...");
                            unauthorized = true;
                            return;
                        }
                        BasicPanel.this.setApplicationsDeployment(parentComponent);
                    } else {
                        unauthorized = false;
                        System.out.println(e.getException());
                        if (new RetryDialog(parentComponent,
                                "Cannot retrieve applications").isRetry())
                            BasicPanel.this.setApplicationsDeployment(parentComponent);
                    }
                } else {
                    unauthorized = false;
                    if (BasicPanel.this.application_deployment != null) {
                        ConfigMutableTreeNode allHostNode =
                                new ConfigMutableTreeNode("hosts", SAPP_DEP);
                        allHostNode.setUserObject(application_deployment);
                        ((DefaultMutableTreeNode) (BasicPanel.this.tree.getModel()).
                                getRoot()).add(allHostNode);
                        BasicPanel.this.addChildrenNodes(SAPP_DEP, SAPP_DEP,
                                BasicPanel.this.application_deployment, allHostNode);
                    }
                }
            }
        });
        rt.run();
    }

    /**
     * Gets the security settings from the proxy, creates a new necessary
     * tree nodes & adds them to the tree.
     *
     * @param   parentComponent   the parent component for dialogs.
     */
    private void setSecuritySettings(final Component parentComponent) {
        RequestThread rt = new RequestThread() {
            protected void doWork() throws Exception {
                BasicPanel.this.security_config = proxy.getSecuritySettings();
            }
        };
        rt.setRequestThreadListener(new RequestThreadListener() {
            public void workFinished(RequestThreadEvent e) {
                if (!e.isSuccess()) {
                    if (e.isUnauthorized()) {
                        int res = showLoginDialog(parentComponent);
                        if (res == JOptionPane.NO_OPTION
                                || res == JOptionPane.CLOSED_OPTION) {
                            status.setStatusString("You are not authorized ...");
                            unauthorized = true;
                            return;
                        }
                        BasicPanel.this.setSecuritySettings(parentComponent);
                    } else {
                        unauthorized = false;
                        System.out.println(e.getException());
                        if (new RetryDialog(parentComponent,
                                "Cannot retrieve security settings").isRetry()) {
                            BasicPanel.this.setSecuritySettings(parentComponent);
                        }
                    }
                } else {
                    unauthorized = false;
                    if (BasicPanel.this.security_config != null) {
                        ConfigMutableTreeNode node =
                                new ConfigMutableTreeNode("security_config", SSECURITY);
                        node.setUserObject(security_config);
                        ((DefaultMutableTreeNode) (BasicPanel.this.tree.getModel()).
                                getRoot()).add(node);
                        BasicPanel.this.addChildrenNodes(SSECURITY,
                                SSECURITY, BasicPanel.this.security_config, node);
                    }
                }
            }
        });
        rt.run();
    }

    /**
     * Adds a new component with the specified type.
     *
     * @param   type    the specified type.
     */
    public void addComponent(String type) {
        if (!canAdd || unauthorized) {
            ManageTool.showMessage("You can't add the component");
            return;
        }
//    addRegisteredComponent(type, type);
        addRegisteredComponent();
    }
//  private void addRegisteredComponent(final String name, final String type){
    private void addRegisteredComponent() {
        final JDialog dialog = MessageDialog.getDialog(ManageTool.getMainFrame(),
                " Message", "Adding new component...", true);
        final RequestThread rt = new RequestThread() {
            protected void doWork() throws Exception {
//              ComponentInfo cinfo = null;
//			  cinfo = new ComponentInfo(name, new ComponentType(type));
//              proxy.addRegisteredComponent(cinfo);
                ConnectionDialog conDialog = new ConnectionDialog(
                        dialog, "Component adding", "Host", "Port", "OK");
                if (conDialog.showDialog()) {
                    String host = conDialog.getHost();
                    int port = conDialog.getPort();
                    proxy.addRegisteredComponent(host, port);
                    BasicPanel.this.cinfo = proxy.getRegisteredComponents();  // can throw management || unath exc
                }
            }
        };
        rt.setRequestThreadListener(new RequestThreadListener() {
            public void workFinished(RequestThreadEvent e) {
                if (!e.isSuccess()) {
                    if (e.isUnauthorized()) {
                        int res = showLoginDialog(dialog);
                        if (res == JOptionPane.NO_OPTION
                                || res == JOptionPane.CLOSED_OPTION) {
                            status.setStatusString("You are not authorized ...");
                            unauthorized = true;
                            ManageTool.hideMessage();
                            return;
                        }
                        BasicPanel.this.addRegisteredComponent();
                    } else {
                        unauthorized = false;
                        System.out.println(e.getException());
                        if (new RetryDialog(dialog, "Cannot add new component").isRetry())
                            BasicPanel.this.addRegisteredComponent();
                    }
                } else {
                    unauthorized = false;
                    if (BasicPanel.this.cinfo != null) {
                        L:				  for (int i = 0; i < BasicPanel.this.cinfo.length; i++) {
                            String typeName = BasicPanel.this.cinfo[i].getComponentTypeName();
                            DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) nodes.get(typeName);
                            if (node != null) {
                                String componentId = BasicPanel.this.cinfo[i].getComponentId();
                                Enumeration chls = node.children();
                                while (chls != null && chls.hasMoreElements()) {
                                    ConfigMutableTreeNode childNode =
                                            (ConfigMutableTreeNode) chls.nextElement();
                                    if (childNode.getComponentId().equals(componentId)) {
                                        continue L;
                                    }
                                }
                                node.add(new ConfigMutableTreeNode(componentId, typeName));
                                BasicPanel.this.reloadTree();
                                if (typeName.equals(SCONTAINER)
                                        || typeName.equals(SDISTRIBUTOR)) {
                                    fireValueAdded(typeName, componentId, componentId);
                                }
                            }
                        }
                    }
//				  DefaultMutableTreeNode node = (DefaultMutableTreeNode)BasicPanel.this.nodes.get(type);
//                  if (node != null){
//                    node.add(new ConfigMutableTreeNode(name, type));
//                    BasicPanel.this.reloadTree();
//                    if (type.equals(ComponentType.SCONTAINER) || type.equals(ComponentType.SDISTRIBUTOR))
//                        fireValueAdded(type, name);
//                  }
                }
                dialog.dispose();
            }
        });
        rt.start();
        dialog.show();
    }

    /**
     * Gets the element type_names array for the specified type.
     * Some types can have more than one composite elements in user object.
     * For ex - app (context + web-app),
     * securty for web-app (login-config + roles + constraints)
     *
     * @param   type  the specified type.
     * @return  the element type names array.
     */
    String[] getElementNames(String type) {
        return getEditorPanel(type).getElementNames();
    }


    /**
     * If current node is application node it changes the servlet & filter vectors.
     * If current node is servlet & filter it check the current application.
     *
     * @param   node    the current node.
     */
    private void checkCurrentNode(ConfigMutableTreeNode node) {
        String type = node.getType();
        if (type.equals(SAPP)) {
            lastLoadedApplication = node;
            fireAllValueRemoved("servlet");
            fireAllValueRemoved("filter");
            int cnt = node.getChildCount();
            for (int i = 0; i < cnt; i++) {
                TreeNode childNode = node.getChildAt(i);
                if (childNode.toString().equals("servlets")) {
                    int scnt = childNode.getChildCount();
                    for (int j = 0; j < scnt; j++) {
                        TreeNode servletNode = childNode.getChildAt(j);
                        if (servletNode instanceof ConfigMutableTreeNode
                                && ((ConfigMutableTreeNode)
                                servletNode).getType().equals(SSERVLET)) {
                            fireValueAdded("servlet",
                                    ((ConfigMutableTreeNode) servletNode).toString());
                        }
                    }
                } else if (childNode.toString().equals("filters")) {
                    int scnt = childNode.getChildCount();
                    for (int j = 0; j < scnt; j++) {
                        TreeNode filterNode = childNode.getChildAt(j);
                        if (filterNode instanceof ConfigMutableTreeNode
                                && ((ConfigMutableTreeNode)
                                filterNode).getType().equals(SFILTER)) {
                            fireValueAdded("filter",
                                    ((ConfigMutableTreeNode) filterNode).toString());
                        }
                    }
                }
            }
        } else if (type.equals(SSERVLET) || type.equals(SFILTER)) {
            TreeNode appNode = node.getParent().getParent();
            if (appNode != lastLoadedApplication) {
                lastLoadedApplication = appNode;
                if (appNode instanceof WebAppMutableTreeNode) {
                    Object userObj = ((WebAppMutableTreeNode) appNode).getUserObject();
                    if (getEditorPanel(SAPP) instanceof ApplicationEditorPanel) {
                        ApplicationEditorPanel appPanel =
                                (ApplicationEditorPanel) getEditorPanel(SAPP);
                        appPanel.setEntry(userObj);
                        appPanel.updateControls();
                        appPanel.setTreeNode((WebAppMutableTreeNode) appNode);
                    }
                }
            }
        }
    }

    /**
     * Method from the TreeSelectionListener interface.
     * If selected tree node is config tree node -
     * it puts the necessary panel to the rigth split pane part.
     * Sets the web elements (from node user object) to this panel.
     * If user object isn't loaded yet, it loads the necessary elements.
     *
     * @param   e   the tree selection event.
     */
    public void valueChanged(TreeSelectionEvent e) {
        if (tree.getSelectionPath() != null
                && tree.getSelectionPath().equals(lastTreePath)) { // select after not save
            return;
        }
        if (this.splitPane.getRightComponent() instanceof JPanel
                && lastEditorPanel != null && !lastEditorPanel.isSaving()) {
            needToReturnLastPath = true;
            return;
        }
        needToReturnLastPath = false;
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                tree.getLastSelectedPathComponent();
        int dloc = this.splitPane.getDividerLocation();
        if (node != null && !node.isRoot()
                && node instanceof ConfigMutableTreeNode) {
            ConfigMutableTreeNode cnode = (ConfigMutableTreeNode) node;
            final String type = cnode.getType();
            if (!cnode.isUserObjectLoaded()) {
                cnode.loadUserObject();
            }
            checkCurrentNode(cnode);
            EditorPanel ep = getEditorPanel(type);
            if (ep != null) {
                ep.setTreeNode(cnode);
                ep.setEntry(cnode.getUserObject());
                ep.updateControls();
                this.splitPane.setRightComponent(ep.getMainComponent());
                lastEditorPanel = ep;
            } else {
                this.splitPane.setRightComponent(new JLabel(""));
                status.setStatusString("Can't find the panel with a current type!");
            }
            fireComponentChanged(
                    ComponentChangedListener.EDITING, type, node.toString());
        } else {
            // if folder (distributor, container, deployer, ...)
            this.splitPane.setRightComponent(new JLabel(""));
            String name = (node != null) ? node.toString() : "";
            fireComponentChanged(ComponentChangedListener.EDITING, "", name);
        }
        this.lastTreePath = this.tree.getSelectionPath();
        this.splitPane.setDividerLocation(dloc);
    }

//  public CompositeNusuthWebAppElement getCompositeUserObject(ConfigMutableTreeNode cnode, String oldUserObject) {
    /**
     * Gets the composite element by the specified type & componentId.
     * Loads this component via proxy & writes a message.
     *
     * @param   type        the specified element type.
     * @param   componentId the specified element id.
     * @return  the composite element.
     * @see #loadComponent(String, String)
     */
    public CompositeNusuthWebAppElement getCompositeUserObject(String type,
                                                               String componentId) {
        if (unauthorized) return null;
        status.setStatusString("Retrieving component ....");
        loadComponent(type, componentId);
        status.clearStatusString();
        return tmpCompositeElement;
    }

    /**
     * Method from the TreeWillExpandListener interface.
     */
    public void treeWillCollapse(TreeExpansionEvent e) {
    }

    /**
     * Method from the TreeWillExpandListener interface.
     */
    public void treeWillExpand(TreeExpansionEvent e) {
    }

    /**
     * Updates the ui, when l&f is changed.
     */
    public void updateUI() {
        super.updateUI();
        updatePanels();
        if (tree != null && tree.getModel() != null) {
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) tree.getModel().getRoot();
            if (node != null) {
                updateTreeUI(node);
            }
        }
        if (configTreeCellRenderer != null) {
            configTreeCellRenderer.setUIBackgroundsIcons();
        }
    }

    /**
     * Updates the tree ui of nodes user objects.
     *
     * @param   node    the specified tree node.
     */
    private void updateTreeUI(DefaultMutableTreeNode node) {
        Object obj = node.getUserObject();
        if (obj instanceof JComponent) {
            JComponent comp = (JComponent) obj;
            comp.updateUI();
            SwingUtilities.updateComponentTreeUI(comp);
        }
        Enumeration enum = node.children();
        while (enum.hasMoreElements()) {
            updateTreeUI((DefaultMutableTreeNode) enum.nextElement());
        }
    }

    /**
     * Updates all panel.
     */
    private void updatePanels() {
        for (Enumeration en = panels.elements(); en.hasMoreElements();) {
            ((EditorPanel) en.nextElement()).updateUI();
        }
    }

    /**
     * Removes all not required elements in the specified composite element.
     *
     * @param   childElement  the specified composite element
     * @return  the composite element without not required elements
     */
    public static CompositeNusuthWebAppElement
            deactivateNotRequired(CompositeNusuthWebAppElement childElement) {
        if (childElement == null) return childElement;
        Enumeration en = childElement.getSimpleChildrenNames();
        while (en.hasMoreElements()) {
            String childName = (String) en.nextElement();
            try {
                if (!childElement.isChildRequired(childName)) {
                    Enumeration childEn = childElement.getSimpleChild(childName);
                    if (childEn.hasMoreElements()) {
                        SimpleNusuthWebAppElement el =
                                (SimpleNusuthWebAppElement) childEn.nextElement();
                        childElement.removeSimpleChild(childName, el);
                    }
                }
            } catch (DeploymentException e) {
            }
        }
        en = childElement.getCompositeChildrenNames();
        while (en.hasMoreElements()) {
            String childName = (String) en.nextElement();
            try {
                if (!childElement.isChildRequired(childName)) {
                    Enumeration childEn = childElement.getCompositeChild(childName);
                    if (childEn.hasMoreElements()) {
                        CompositeNusuthWebAppElement el =
                                (CompositeNusuthWebAppElement) childEn.nextElement();
                        childElement.removeCompositeChild(childName, el);
                    }
                }
            } catch (DeploymentException e) {
            }
        }
        return childElement;
    }

    /**
     * Method from the NameChangedListener interface.
     * Invoiked when name was changed.
     *
     * @param   source    the specified source editor panel.
     * @param   oldValue  the old name value.
     * @param   newValue  the new name value.
     */
    public void nameChanged(EditorPanel source,
                            String oldValue, String newValue) {
        DefaultMutableTreeNode dnode = source.getTreeNode();
        String comp = source.getTag();
        if (dnode != null && dnode instanceof ConfigMutableTreeNode) {
            ConfigMutableTreeNode node = (ConfigMutableTreeNode) dnode;
            node.setName(newValue);
            fireComponentChanged(
                    ComponentChangedListener.EDITING, node.getType(), newValue);
            reloadTree();
        }
        if (comp.equals("group") || comp.equals("user")
                || comp.equals("servlet") || comp.equals("host")) {
            fireValueChanged(comp, oldValue, newValue);
        } else if (comp.equals(SWEB_APP)
                && dnode instanceof WebAppMutableTreeNode) {
            changeContextInHost((ConfigMutableTreeNode)
                    source.getTreeNode().getParent(), oldValue, newValue);
        } else if ((comp.equals(SCONTAINER) || comp.equals(SDISTRIBUTOR))
                && (dnode instanceof ConfigMutableTreeNode)
                && !(source.getTreeNode().getParent()
                instanceof ConfigMutableTreeNode)) {
            fireValueChanged(comp, ((ConfigMutableTreeNode) dnode).getComponentId(),
                    oldValue, newValue);
        }
    }

    /**
     * Method from the NameChangedListener interface.
     * Invoiked when component id was changed.
     *
     * @param   source    the specified source editor panel.
     * @param   oldValue  the old component id value.
     * @param   newValue  the new component id value.
     */
    public void componentIdChanged(EditorPanel source,
                                   String oldValue, String newValue) {
        ConfigMutableTreeNode node = source.getTreeNode();
        String comp = source.getType();
        if (node != null) {
            if (comp.equals(SHOST) || comp.equals(SAPP)) {
                node.setComponentId(newValue);
            }
        }
    }

    /**
     * Loads a component with the specified type & name via the proxy
     * & puts it to the tmpCompositeElement.
     *
     * @param   type    the specified type.
     * @param   name    the specified name.
     */
    private void loadComponent(final String type, final String name) {
        RequestThread rt = new RequestThread() {
            protected void doWork() throws Exception {
                // can throw management || unath exc
                BasicPanel.this.tmpCompositeElement =
                        proxy.getComponentSettings(type, name);
            }
        };
        rt.setRequestThreadListener(new RequestThreadListener() {
            public void workFinished(RequestThreadEvent e) {
                if (!e.isSuccess()) {
                    ManageTool.hideMessage();
                    if (e.isUnauthorized()) {
                        int res = showLoginDialog();
                        if (res == JOptionPane.NO_OPTION || res == JOptionPane.CLOSED_OPTION) {
                            status.setStatusString("You are not authorized ...");
                            unauthorized = true;
                            return;
                        }
                        BasicPanel.this.loadComponent(type, name);
                    } else {
                        unauthorized = false;
                        System.out.println(e.getException());
                        if (new RetryDialog(
                                BasicPanel.this, "Cannot retrieve components").isRetry())
                            BasicPanel.this.loadComponent(type, name);
                    }
                } else {
                    unauthorized = false;
                }
            }
        });
        tmpCompositeElement = null;
        rt.run();
    }

    /**
     * Saves the changed component to the proxy.
     * Saving is defined by the specified editor panel.
     *
     * @param   wep   the specified editor panel.
     */
    public void saveComponent(final EditorPanel wep) {
        final JDialog dialog = MessageDialog.getDialog(ManageTool.getMainFrame(),
                " Saving", "Saving component...", true);
        RequestThread rt = new RequestThread() {
            protected void doWork() throws Exception {
                wep.saveComponent();
            }
        };
        rt.setRequestThreadListener(new RequestThreadListener() {
            public void workFinished(RequestThreadEvent e) {
                dialog.dispose();
                status.clearStatusString();
                if (!e.isSuccess()) {
                    if (e.isUnauthorized()) {
                        int res = showLoginDialog(dialog);
                        if (res == JOptionPane.NO_OPTION
                                || res == JOptionPane.CLOSED_OPTION) {
                            status.setStatusString("You are not authorized ...");
                            unauthorized = true;
                        } else {
                            BasicPanel.this.saveComponent(wep);
                        }
                    } else {
                        unauthorized = false;
                        System.out.println(e.getException());
                        if (new RetryDialog(dialog,
                                "Cann't save this component").isRetry()) {
                            BasicPanel.this.saveComponent(wep);
                        }
                    }
                } else
                    unauthorized = false;
            }
        });
        rt.start();
        dialog.show();
    }

    /**
     * Shows the login dialog.
     *
     * @return  the dialog result.
     * @see #showLoginDialog(Component)
     */
    int showLoginDialog() {
        return ManageTool.showLoginDialog();
    }

    /**
     * Shows the login dialog with the specified parent component.
     *
     * @param   the specified parent component.
     * @return  the dialog result.
     * @see #showLoginDialog()
     */
    int showLoginDialog(Component parentComponent) {
        return ManageTool.showLoginDialog(parentComponent);
    }

    /**
     * Creates a necessary tab listener.
     * Defines the necessary actions for tab changing.
     */
    void createTabListener() {
        // add listener to know when we've been shown
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JTabbedPane tab = (JTabbedPane) e.getSource();
                int index = tab.getSelectedIndex();
                currentPage = tab.getComponentAt(index);
                if (currentPage == BasicPanel.this.splitPane) {
                    if (ManageTool.getSwitchStop()) {
                        BasicPanel.monitorContainer.stopTimer();
                    }
                    BasicPanel.this.clusterViewPanel.stopThread();
                    DefaultMutableTreeNode dnode =
                            (tree != null && tree.getSelectionPath() != null)
                            ? (DefaultMutableTreeNode)
                            tree.getSelectionPath().getLastPathComponent()
                            : null;
                    String type =
                            (dnode != null && dnode instanceof ConfigMutableTreeNode)
                            ? ((ConfigMutableTreeNode) dnode).getType() : "";
                    String name =
                            (dnode != null && dnode instanceof ConfigMutableTreeNode)
                            ? ((ConfigMutableTreeNode) dnode).toString() : "";
                    BasicPanel.this.fireComponentChanged(
                            ComponentChangedListener.EDITING, type, name);
                } else if (currentPage == BasicPanel.monitorContainer) {
                    BasicPanel.monitorContainer.startTimer();
                    BasicPanel.this.clusterViewPanel.stopThread();
                    BasicPanel.this.fireComponentChanged(
                            ComponentChangedListener.MONITORING, "",
                            BasicPanel.monitorContainer.getActiveMonitorName());
                } else if (currentPage == BasicPanel.this.clusterViewPanel) {
                    if (ManageTool.getSwitchStop()) {
                        BasicPanel.monitorContainer.stopTimer();
                    }
                    BasicPanel.this.clusterViewPanel.startThread();
                    BasicPanel.this.fireComponentChanged(
                            ComponentChangedListener.CLUSTER_VIEW, "", "");
                }
            }
        };
        this.addChangeListener(changeListener);
    }

    /**
     * Gets the container state by the specified component id.
     *
     * @param   componentId   the specified component id.
     * @return  the container state
     * @throws  UnauthorizedAccessException
     * @throws  ManagementException
     * @see #getDistributorState(String)
     */
    public static ContainerState getContainerState(String componentId)
            throws UnauthorizedAccessException, ManagementException {
        return (componentId == null) ? null : proxy.getContainerState(componentId);
    }


    /**
     * Gets the distributor state by the specified component id.
     *
     * @param   componentId   the specified component id.
     * @return  the distributor state
     * @throws  UnauthorizedAccessException
     * @throws  ManagementException
     * @see #getContainerState(String)
     */
    public static DistributorState getDistributorState(String componentId)
            throws UnauthorizedAccessException, ManagementException {
        return (componentId == null) ? null : proxy.getDistributorState(componentId);
    }

    /**
     * Sets the function names for the specified type
     * by the specified string tokenizer.
     *
     * @param   type    the specified type.
     * @param   st      the specified string tokenizer.
     */
    private void setFunctionNames(StringTokenizer st, String type) {
        while (st.hasMoreTokens()) {
            String functionName = st.nextToken();
            fireValueAdded(type, functionName);
        }
    }

    /**
     * Loads the function properties.
     */
    private void loadProperties() {
        functionsProps = new Properties();
        try {
            functionsProps.load(ClassLoader.getSystemResourceAsStream(
                    "com/azoft/nusuth/gui/functions.properties"));
            Enumeration e = functionsProps.propertyNames();
            while (e.hasMoreElements()) {
                String key = e.nextElement().toString();
                String s = functionsProps.getProperty(key);
                if (s != null) {
                    StringTokenizer st = new StringTokenizer(s.trim(), ";");
                    if (key.equals("ContainerState.functions")
                            || key.equals("DistributorState.functions")) {
                        setFunctionNames(st,
                                (key.startsWith("Container")) ? "containerFunction"
                                : "distributorFunction");
                    } else {
                        String funName = "";
                        Class[] classes = new Class[st.countTokens() - 1];
                        if (st.hasMoreTokens()) funName = st.nextToken().trim();
                        int cnt = 0;
                        while (st.hasMoreTokens()) {
                            classes[cnt++] = Class.forName(st.nextToken().trim());
                        }
                        functionsProps.put(key, funName);
                        functionsProps.put(funName, classes);
                    }
                }
            }
        } catch (Throwable ex) {
            System.err.println("Cannot load function properties");
        }
    }

    /**
     * Gets the arguments count by the specified graph name.
     *
     * @param   grName    the specified graph name.
     * @return  the arguments count by the specified graph name
     */
    public static int getArgsCount(String grName) {
        String funName = (String) functionsProps.get(grName);
        Class[] classes = (funName == null) ? null
                : (Class[]) functionsProps.get(funName);
        return (classes == null) ? 0 : classes.length;
    }

    /**
     * Gets the empty composite element by the specified type.
     *
     * @param   type    the specified type.
     * @return  the empty composite element by the specified type
     * @see #getCompositeElement(String,String)
     */
    public static CompositeNusuthWebAppElement getCompositeElement(String type) {
//    System.out.println("getCompositeElement with type = "+type);
        int lastInd = type.lastIndexOf(".");
        if (lastInd != -1) {
            String parent = type.substring(0, lastInd);
            String child = type.substring(lastInd + 1);
            return getCompositeElement(parent, child);
        }
        return elementFactory.getWebElement(type);
    }

    /**
     * Gets the empty composite element by the specified parent type & child name.
     *
     * @param   parent    the specified parent type.
     * @param   childName the specified child name.
     * @return  the empty composite element
     * @see #getCompositeElement(String)
     */
    public static CompositeNusuthWebAppElement
            getCompositeElement(String parent, String childName) {
        CompositeNusuthWebAppElement el = getCompositeElement(parent);
        if (el != null) {
            return findChild(el, childName);
        }
        return null;
    }

    /**
     * Finds the child composite element by the
     * specified composite element & child name.
     *
     * @param   el        the specified composite element.
     * @param   childName the specified child name.
     * @return  the child composite element
     * @see #getCompositeElement(String,String)
     */
    static CompositeNusuthWebAppElement findChild(CompositeNusuthWebAppElement el,
                                                  String childName) {
        Enumeration e = el.getCompositeChildrenNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            if (childName.equals(name)) {
                Enumeration en = null;
                try {
                    en = el.getCompositeChild(name);
                } catch (DeploymentException de) {
                    System.out.println(de);
                    return null;
                }
                if (en != null && en.hasMoreElements()) {
                    return (CompositeNusuthWebAppElement) en.nextElement();
                }
            }
        }
        e = el.getCompositeChildrenNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            Enumeration en = null;
            try {
                en = el.getCompositeChild(name);
            } catch (DeploymentException de) {
                System.out.println(de);
                return null;
            }
            if (en != null && en.hasMoreElements()) {
                CompositeNusuthWebAppElement res = findChild(
                        (CompositeNusuthWebAppElement) en.nextElement(), childName);
                if (res != null) return res;
            }
        }
        return null;
    }

    /**
     * Parses properties, creates & adds a necessary child nodes by
     * the specified parent name, parent path, parent element & parent node.
     *
     * @param   parent_name   the specified parent name.
     * @param   path          the specified parent path.
     * @param   parent        the specified parent element.
     * @param   parentNode    the specified parent node.
     */
    private void addChildrenNodes(String parent_name, String path,
                                  CompositeNusuthWebAppElement parent,
                                  DefaultMutableTreeNode parentNode) {
        // for context nodes - servlets & filters
        String treeNodes = (String)
                applicationProps.get(parent_name + ".treeNodeFor");
        if (treeNodes != null) {
            StringTokenizer stNodes = new StringTokenizer(treeNodes, ";");
            while (stNodes.hasMoreTokens()) {
                parentNode.add(new DefaultMutableTreeNode(stNodes.nextToken() + "s"));
            }
        }
        // host for ex
        String childNames = (String) applicationProps.get(parent_name + ".child");
        if (childNames == null) return;
        StringTokenizer st = new StringTokenizer(childNames, ";");
        while (st.hasMoreElements()) {
            String childName = st.nextToken();
            Enumeration ench = null;
            try {
                ench = parent.getCompositeChild(childName);
            } catch (DeploymentException de) {
                System.out.println(de);
            }
            while (ench != null && ench.hasMoreElements()) {
                CompositeNusuthWebAppElement webElement =
                        (CompositeNusuthWebAppElement) ench.nextElement();
                String nodeName = DefaultEditorPanel.
                        getDisplay(path + "." + childName, webElement);
                DefaultMutableTreeNode node = null;
                if (childName.equals("context")) {
                    node = new WebAppMutableTreeNode(nodeName, SAPP);
                    ((WebAppMutableTreeNode) node).
                            setElementNode((WebAppMutableTreeNode) node);
                    // puts the context element
                    Hashtable hash = new Hashtable();
                    hash.put(ApplicationEditorPanel.contextName, webElement);
                    node.setUserObject(hash);
                } else {
                    node = new ConfigMutableTreeNode(nodeName, path + "." + childName);
                    node.setUserObject(webElement);
                }
                if (childName.equals("group") || childName.equals("user")
                        || childName.equals("servlet") || childName.equals("host")) {
                    fireValueAdded(childName, nodeName);
                } else if (childName.equals("context")) {
                    addContextToHost((ConfigMutableTreeNode) parentNode, nodeName);
                }
                // if context return servlets || filter nodes else return parent node
                parentNode.add(node);
//        getTreeNode(parentNode, childName).add(node);
                addChildrenNodes(childName, path + "." + childName, webElement, node);
            }
        }
    }

    /**
     * Gets the tree node for the specified parent node & child name.
     * It's the parent node by default. In case of servlet & filter -
     * the servlets/filters node.
     *
     * @param   parentNode  the specified parent node.
     * @param   childName   the specified child name.
     * @return  the tree node
     */
    DefaultMutableTreeNode getTreeNode(DefaultMutableTreeNode parentNode,
                                       String childName) {
        if (childName.equals("servlet")
                || childName.equals("filter")) {
            int cnt = parentNode.getChildCount();
            for (int i = 0; i < cnt; i++) {
                DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) parentNode.getChildAt(i);
                if (node.toString().equals(childName + "s"))
                    return node;
            }
        }
        return parentNode;
    }

    /**
     * Reloads the tree.
     */
    void reloadTree() {
        Enumeration enum = tree.getExpandedDescendants(new TreePath(
                ((DefaultMutableTreeNode) tree.getModel().getRoot()).getPath()));
        TreePath path = tree.getSelectionPath();
        ((DefaultTreeModel) tree.getModel()).reload();
        while (enum != null && enum.hasMoreElements()) {
            tree.expandPath((TreePath) enum.nextElement());
        }
        tree.setSelectionPath(path);
    }

    /**
     * Adds the specified changing values renderer to
     * the renderers vector of the specified type.
     *
     * @param   type      the specified type.
     * @param   renderer  the specified renderer.
     * @see #removeChangingValuesElementRenderer(String,ChangingValuesElementRenderer)
     */
    public static void addChangingValuesElementRenderer(String type,
                                                        ChangingValuesElementRenderer renderer) {
        getRenderersVectorByType(type).addElement(renderer);
        for (Enumeration en =
                getValuesVectorByType(type).elements(); en.hasMoreElements();) {
            renderer.addItem("", (String) en.nextElement());
        }
    }

    /**
     * Removes the specified changing values renderer from
     * the renderers vector of the specified type.
     *
     * @param   type      the specified type.
     * @param   renderer  the specified renderer.
     * @see #addChangingValuesElementRenderer(String,ChangingValuesElementRenderer)
     */
    public static void removeChangingValuesElementRenderer(String type,
                                                           ChangingValuesElementRenderer renderer) {
        getRenderersVectorByType(type).removeElement(renderer);
    }

    /**
     * Adds the specified value with the specified type &
     * notify all necessary renderers about adding.
     *
     * @param   type    the specified type.
     * @param   name    the specified name.
     * @see #fireValueAdded(String,String,String)
     */
    public static void fireValueAdded(String type, String name) {
//    System.out.println("fireValueAdded, type = " + type + ", name = " + name);
        fireValueAdded(type, "", name);
    }

    /**
     * Adds the specified value with the specified type & component id and
     * notify all necessary renderers about adding.
     *
     * @param   type    the specified type.
     * @param   id      the specified component id.
     * @param   name    the specified name.
     * @see #fireValueAdded(String,String)
     */
    public static void fireValueAdded(String type, String id, String name) {
//    System.out.println("fireValueAdded, type = " + type + ", name = " + name);
        getValuesVectorByType(type).addElement(name);
        for (Enumeration en =
                getRenderersVectorByType(type).elements(); en.hasMoreElements();) {
            ((ChangingValuesElementRenderer) en.nextElement()).addItem(id, name);
        }
    }

    /**
     * Removes the specified value with the specified type &
     * notify all necessary renderers about removing.
     *
     * @param   type    the specified type.
     * @param   name    the specified name.
     * @see #fireValueRemoved(String,String,String)
     */
    public static void fireValueRemoved(String type, String name) {
//    System.out.println("fireValueRemoved, type = " + type + ", name = " + name);
        fireValueRemoved(type, "", name);
    }

    /**
     * Removes the specified value with the specified type & component id and
     * notify all necessary renderers about removing.
     *
     * @param   type    the specified type.
     * @param   id      the specified component id.
     * @param   name    the specified name.
     * @see #fireValueRemoved(String,String)
     */
    public static void fireValueRemoved(String type, String id, String name) {
        getValuesVectorByType(type).removeElement(name);
        for (Enumeration en =
                getRenderersVectorByType(type).elements(); en.hasMoreElements();) {
            ((ChangingValuesElementRenderer) en.nextElement()).removeItem(id, name);
        }
    }

    /**
     * Removes all values with the specified type &
     * notify all necessary renderers about removing.
     *
     * @param   type    the specified type.
     * @see #fireValueRemoved(String,String)
     * @see #fireValueRemoved(String,String,String)
     */
    public static void fireAllValueRemoved(String type) {
//    System.out.println("fireAllValueRemoved, type = " + type);
        if (type.equals("host")) {
            removeAllContextFromAllHost();
        }
        getValuesVectorByType(type).removeAllElements();
        for (Enumeration en =
                getRenderersVectorByType(type).elements(); en.hasMoreElements();) {
            ((ChangingValuesElementRenderer) en.nextElement()).removeAllItems();
        }
    }

    /**
     * Changes the specified old value with the specified type to
     * the specified new value &
     * notify all necessary renderers about changing.
     *
     * @param   type    the specified type.
     * @param   oldName the specified old name.
     * @param   newName the specified new name.
     * @see #fireValueChanged(String,String,String,String)
     */
    public static void fireValueChanged(String type,
                                        String oldName, String newName) {
//    System.out.println("fireValueChanged, type = " + type + ", oldName = " + oldName + ", newName = " + newName);
        fireValueChanged(type, "", oldName, newName);
    }

    /**
     * Changes the specified old value with the specified type & component id to
     * the specified new value &
     * notify all necessary renderers about changing.
     *
     * @param   type        the specified type.
     * @param   componentId the specified component id.
     * @param   oldName     the specified old name.
     * @param   newName     the specified new name.
     * @see #fireValueChanged(String,String,String)
     */
    public static void fireValueChanged(String type, String componentId,
                                        String oldName, String newName) {
        int index = getValuesVectorByType(type).indexOf(oldName);
        if (index != -1) getValuesVectorByType(type).setElementAt(newName, index);
        for (Enumeration en =
                getRenderersVectorByType(type).elements(); en.hasMoreElements();) {
            ((ChangingValuesElementRenderer) en.nextElement()).
                    changeItem(componentId, oldName, newName);
        }
    }

    /**
     * Gets the renderers vector by the specified type.
     *
     * @param   type    the specified type.
     * @return  the renderers vector by the specified type
     * @see #getValuesVectorByType(String)
     */
    private static Vector getRenderersVectorByType(String type) {
        Vector vector = (Vector) changingValuesRenderers.get(type);
        if (vector == null) {
            vector = new Vector();
            changingValuesRenderers.put(type, vector);
        }
        return vector;
    }

    /**
     * Gets the values vector by the specified type.
     *
     * @param   type    the specified type.
     * @return  the values vector by the specified type
     * @see #setValuesVectorByType(String,Vector)
     */
    private static Vector getValuesVectorByType(String type) {
        Vector vector = (Vector) changingValues.get(type);
        if (vector == null) {
            vector = new Vector();
            changingValues.put(type, vector);
        }
        return vector;
    }

    /**
     * Sets the values vector by the specified type.
     *
     * @param   type    the specified type.
     * @see #getValuesVectorByType(String)
     */
    private static void setValuesVectorByType(String type, Vector newValues) {
        if (newValues != null && type != null) {
            changingValues.put(type, newValues);
        } else {
            changingValues.put(type, new Vector());
        }
    }

    /**
     * Gets the user names.
     *
     * @return  the user names.
     */
    public static Vector getUserNames() {
        return getValuesVectorByType("user");
    }

    /**
     * Gets the group names.
     *
     * @return  the group names.
     */
    public static Vector getGroupNames() {
        return getValuesVectorByType("group");
    }

    /**
     * Gets the hosts vector contains the default host or not.
     *
     * @return  <code>true</code> if hosts vector contains the default host;
     * <code>false</code> otherwise.
     */
    public static boolean hasDefaultHost() {
        return getValuesVectorByType("host").contains(GraphsTable.ALLHOSTS);
    }

    /**
     * Adds the specified context name to the contexts vector by the
     * specified host node.
     *
     * @param   hostNode    the specified host node.
     * @param   context     the specified context name.
     * @see #removeContextFromHost(hostNode,String)
     */
    private void addContextToHost(ConfigMutableTreeNode hostNode, String context) {
        Vector vector = (Vector) hostApplications.get(hostNode);
        if (vector == null) {
            vector = new Vector();
            vector.addElement(GraphsTable.ALLAPPS);
            hostApplications.put(hostNode, vector);
        }
        vector.addElement(context);
    }

    /**
     * Removes the specified context name from the contexts vector by the
     * specified host node.
     *
     * @param   hostNode    the specified host node.
     * @param   context     the specified context name.
     * @see #addContextToHost(ConfigMutableTreeNode,String)
     */
    void removeContextFromHost(ConfigMutableTreeNode hostNode, String context) {
        Vector vector = (Vector) hostApplications.get(hostNode);
        if (vector != null)
            vector.remove(context);
    }

    /**
     * Removes all context names from the contexts vector by the
     * specified host node.
     *
     * @param   hostNode    the specified host node.
     * @see #removeContextFromHost(ConfigMutableTreeNode,String)
     */
    static void removeAllContextFromHost(ConfigMutableTreeNode hostNode) {
        hostApplications.remove(hostNode);
    }

    /**
     * Removes all context names from the contexts vectors.
     *
     * @see #removeContextFromHost(ConfigMutableTreeNode,String)
     * @see #removeAllContextFromHost(ConfigMutableTreeNode)
     */
    private static void removeAllContextFromAllHost() {
        Enumeration keys = hostApplications.keys();
        while (keys.hasMoreElements()) {
            hostApplications.remove(keys.nextElement());
        }
    }

    /**
     * Changes the specified context name from the contexts vector by the
     * specified host node to the specified new context name.
     *
     * @param   hostNode    the specified host node.
     * @param   contextOld  the specified old context name.
     * @param   contextNew  the specified new context name.
     */
    private void changeContextInHost(ConfigMutableTreeNode hostNode,
                                     String contextOld, String contextNew) {
        Vector vector = (Vector) hostApplications.get(hostNode);
        if (vector != null) {
            int index = vector.indexOf(contextOld);
            if (index != -1) vector.setElementAt(contextNew, index);
        }
    }

    public static void setContextByHost(String hostName) {
        fireAllValueRemoved("context");
        Vector vector = getContextsByHostName(hostName);
        if (vector != null) {
            for (Enumeration en = vector.elements(); en.hasMoreElements();) {
                fireValueAdded("context", (String) en.nextElement());
            }
        }
    }

    public static Vector getContextsByHostName(String hostName) {
        Enumeration keys = hostApplications.keys();
        while (keys.hasMoreElements()) {
            Object o = keys.nextElement();
            if (o instanceof ConfigMutableTreeNode) {
                ConfigMutableTreeNode hostNode = (ConfigMutableTreeNode) o;
                if (hostNode.toString().equals(hostName))
                    return (Vector) hostApplications.get(hostNode);
            }
        }
        return null;
    }

    public static void setComponentsByType(String type) {
        // type = container || distibutor
        Vector values = getValuesVectorByType(type + "Function");
        setAllValues("function", values);
        values = getValuesVectorByType(type);
        setAllValues("component", values);
    }

    private static void setAllValues(String key, Vector newValues) {
        fireAllValueRemoved(key);
        for (Enumeration en = newValues.elements(); en.hasMoreElements();) {
            fireValueAdded(key, (String) en.nextElement());
        }
    }

    private void checkAddDeletePermission() {
        if (security_config == null) {
            System.out.println("security_config == null");
            return;
        }
        try {
            Enumeration enres = security_config.getCompositeChild("resources");
            Enumeration enusers = security_config.getCompositeChild("users");
            if (!enres.hasMoreElements() || !enusers.hasMoreElements()) return;
            resources = (CompositeNusuthWebAppElement) enres.nextElement();
            users = (CompositeNusuthWebAppElement) enusers.nextElement();
            String userName = ManageTool.getUserName();
            checkResources("user-name", userName);
            if (canAddDeleteAdmin()) return;
            fillParentVector("user", userName);
            while (parentVector.size() > 0) {
                Enumeration enparent = parentVector.elements();
                while (enparent.hasMoreElements()) {
                    String parentName = (String) enparent.nextElement();
                    checkResources("group-name", parentName);
                    if (canAddDeleteAdmin()) return;
                }
                Vector oldParent = (Vector) parentVector.clone();
                parentVector.clear();
                enparent = oldParent.elements();
                while (enparent.hasMoreElements()) {
                    String parentName = (String) enparent.nextElement();
                    fillParentVector("group", parentName);
                }
            }
        } catch (DeploymentException e) {
        }
    }

    private boolean canAddDeleteAdmin() {
        return canAdd && canDelete && canSetCongif;
    }

    private void checkResources(String usorgr, String ugname) {
        // usorgr = "user-name" || "group-name"
        if (resources == null) return;
        Enumeration enrights = null;
        try {
            enrights = resources.getCompositeChild("right");
            while (enrights.hasMoreElements()) {
                CompositeNusuthWebAppElement right = (CompositeNusuthWebAppElement) enrights.nextElement();
                try {
                    Enumeration enrus = right.getSimpleChild(usorgr); // users || groups
                    if (enrus.hasMoreElements()) {
                        String name = ((SimpleNusuthWebAppElement) enrus.nextElement()).getContent();
                        if (name.equals(ugname)) {
                            Enumeration enperm = right.getSimpleChild("permission");
                            while (enperm.hasMoreElements()) {
                                String permission = ((SimpleNusuthWebAppElement) enperm.nextElement()).getContent();
                                if (permission.equals("*")) {
                                    canAdd = true;
                                    canDelete = true;
                                    canSetCongif = true;
                                    return;
                                }
                                if (permission.equals("add"))
                                    canAdd = true;
                                if (permission.equals("delete"))
                                    canDelete = true;
                                if (permission.equals("set config"))
                                    canSetCongif = true;
                            }
                        }
                    }
                } catch (DeploymentException e) {
                }
            }
        } catch (DeploymentException e) {
        }
    }

    boolean canAddItems() {
        return canAdd;
    }

    boolean canDeleteItems() {
        return canDelete;
    }

    private void fillParentVector(String usorgr, String ugname) {
        // usorgr = "user" || "group"
        if (users == null) return;
        Enumeration enusers = null;
        try {
            enusers = users.getCompositeChild("user");
            while (enusers.hasMoreElements()) {
                CompositeNusuthWebAppElement user = (CompositeNusuthWebAppElement) enusers.nextElement();
                try {
                    Enumeration enname = user.getSimpleChild("name");
                    if (enname.hasMoreElements()) {
                        String name = ((SimpleNusuthWebAppElement) enname.nextElement()).getContent();
                        if (name.equals(ugname)) {
                            Enumeration enparents = user.getSimpleChild("parent-group");
                            while (enparents.hasMoreElements()) {
                                parentVector.addElement(((SimpleNusuthWebAppElement) enparents.nextElement()).getContent());
                            }
                            return;
                        }
                    }
                } catch (DeploymentException e) {
                }
            }
        } catch (DeploymentException e) {
        }
    }

    public static boolean userIsAdmin() {
        return canSetCongif;
    }

    private void setAdminPassword() {
        adminPassword = getPassword(ManageTool.getUserName());
    }

    public static String getAdminPassword() {
        if (!userIsAdmin()) return null;
        return adminPassword;
    }

    private String getPassword(String userName) {
        if (users == null) return null;
        Enumeration enusers = null;
        try {
            enusers = users.getCompositeChild("user");
            while (enusers.hasMoreElements()) {
                CompositeNusuthWebAppElement user = (CompositeNusuthWebAppElement) enusers.nextElement();
                try {
                    Enumeration enname = user.getSimpleChild("name");
                    if (enname.hasMoreElements()) {
                        String name = ((SimpleNusuthWebAppElement) enname.nextElement()).getContent();
                        if (name.equals(userName)) {
                            Enumeration enpasswd = user.getSimpleChild("password");
                            while (enpasswd.hasMoreElements()) {
                                return ((SimpleNusuthWebAppElement) enpasswd.nextElement()).getContent();
                            }
                        }
                    }
                } catch (DeploymentException e) {
                }
            }
        } catch (DeploymentException e) {
        }
        return null;
    }

    public void addComponentChangedListener(ComponentChangedListener l) {
        componentChangedListeners.addElement(l);
    }

    public void removeComponentChangedListener(ComponentChangedListener l) {
        componentChangedListeners.removeElement(l);
    }

    public void fireComponentChanged(int tab, String type, String id) {
        for (Enumeration en = componentChangedListeners.elements(); en.hasMoreElements();) {
            ((ComponentChangedListener) en.nextElement()).componentChanged(tab, type, id);
        }
    }

    public static String getComponentId(String type, String name) {
        // c - container or distributor
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.get(type);
        for (Enumeration en = node.children(); en.hasMoreElements();) {
            TreeNode childNode = (TreeNode) en.nextElement();
            if (childNode instanceof ConfigMutableTreeNode) {
                if (((ConfigMutableTreeNode) childNode).toString().equals(name)) {
                    return ((ConfigMutableTreeNode) childNode).getComponentId();
                }
            }
        }
        return "???";
    }

    public static String getComponentName(String type, String componentId) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.get(type);
        for (Enumeration en = node.children(); en.hasMoreElements();) {
            TreeNode childNode = (TreeNode) en.nextElement();
            if (childNode instanceof ConfigMutableTreeNode) {
                if (((ConfigMutableTreeNode) childNode).getComponentId().equals(componentId)) {
                    return ((ConfigMutableTreeNode) childNode).toString();
                }
            }
        }
        return "???";
    }

    /**
     * Gets the distributor node with the specified component id.
     *
     * @return  the distributors node with the specified component id.
     * @see #getDistributorsNode()
     */
    public static ConfigMutableTreeNode getDistributorNode(String componentId) {
        DefaultMutableTreeNode distrs = (DefaultMutableTreeNode) nodes.get(SDISTRIBUTOR);
        for (Enumeration en = distrs.children(); en.hasMoreElements();) {
            TreeNode childNode = (TreeNode) en.nextElement();
            if (childNode instanceof ConfigMutableTreeNode) {
                if (((ConfigMutableTreeNode) childNode).getComponentId().equals(componentId)) {
                    return (ConfigMutableTreeNode) childNode;
                }
            }
        }
        return null;
    }

    /**
     * Gets the node with all distributors.
     *
     * @return  the distributors node.
     * @see #getDistributorNode(String)
     */
    public DefaultMutableTreeNode getDistributorsNode() {
        return (DefaultMutableTreeNode) nodes.get(SDISTRIBUTOR);
    }

    /**
     * Sets the specified tabs type to this class.
     * It may be ALLTABS, USEDTABS, CUSTOMTABS.
     * Stores the specified type & call update method in panel in rigth part, if it exist.
     * If type is custom_tabs - it calls the custom dialog.
     *
     * @param   type   the specified tabs type.
     */
/*
  public void setShowTabs(int type) {
    this.tabsType = type;
    if (splitPane.getRightComponent() instanceof JPanel) {
      String nodeType = ((ConfigMutableTreeNode) tree.getLastSelectedPathComponent()).getType();
      EditorPanel panel = getEditorPanel(nodeType);
      if (type == CUSTOMTABS) {
        showCustomTabs(panel);
      }
      panel.updateControls();
    }
  }
*/

    /**
     * Shows the option dialog with custom tab panel of the necessary panel.
     *
     * @param   panel   the specified panel.
     */
/*
  private void showCustomTabs(EditorPanel panel) {
    if (panel instanceof DefaultEditorPanel) {
      DefaultEditorPanel defPanel = ((DefaultEditorPanel) panel);
      Object mes = defPanel.getCustomTabsPanel();
      if (mes == null) {
        JOptionPane.showMessageDialog(this, "Nothing to custom ... That's all tabs.");
      } else {
        int res = JOptionPane.showConfirmDialog(this, mes, "Choose the visible tabs, please", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        defPanel.updateCustomPanelDimension();
        if (res == JOptionPane.OK_OPTION) {
          defPanel.updateHavenTabNames();
        } else {
          defPanel.updateTabPanel();
        }
      }
    }
  }
*/

    /**
     * Saves all tabs properties - by each existing panel.
     *
     * @param   prop   the specified properties, to which data is saved
     */
/*
  public void saveTabsProperties(Properties prop) {
    for (Enumeration en = panels.keys(); en.hasMoreElements();) {
      String type = (String) en.nextElement();
      EditorPanel panel = (EditorPanel) panels.get(type);
      if (panel instanceof DefaultEditorPanel) {
        ((DefaultEditorPanel) panel).saveTabsProperties(type, prop);
      }
    }
  }
*/

    /**
     * Gets the taglibs array for the specified
     * web-app composite element via proxy.
     *
     * @param   web_app   the specified web-app composite element.
     * @return  the taglibs array
     * @see #setWebAppTaglib(CompositeNusuthWebAppElement, String,
            * CompositeNusuthWebAppElement)
     */
    public CompositeNusuthWebAppElement[] getWebAppTaglibs(
            CompositeNusuthWebAppElement web_app) {
        CompositeNusuthWebAppElement element = getCompositeElement(SWEBTAGLIB);
        CompositeNusuthWebAppElement[] res = new CompositeNusuthWebAppElement[1];
        res[0] = element;
        return res;
    }

    /**
     * Commits the taglib with the specified id, element & web application
     * to the server.
     *
     * @param   web_app   the specified web-app composite element.
     * @param   taglibId  the specified taglib id.
     * @param   taglib    the specified taglib composite element.
     * @see #getWebAppTaglibs(CompositeNusuthWebAppElement)
     */
    public void setWebAppTaglib(CompositeNusuthWebAppElement web_app,
                                String taglibId,
                                CompositeNusuthWebAppElement taglib) {
        System.out.println("setWebAppTaglib for the taglibId = " + taglibId + ", web_app = ");
        System.out.println(web_app.toString());
        System.out.println("taglib = " + taglib);
    }

    /**
     * Adds the taglib with the specified element & web application
     * to the server.
     *
     * @param   web_app   the specified web-app composite element.
     * @param   taglib    the specified taglib composite element.
     * @see #removeWebAppTaglib(CompositeNusuthWebAppElement)
     */
    public String addWebAppTaglib(CompositeNusuthWebAppElement web_app,
                                  CompositeNusuthWebAppElement taglib) {
        System.out.println("addWebAppTaglib");
        return DefaultEditorPanel.getDisplay(BasicPanel.SWEBTAGLIB, taglib);
    }

    /**
     * Removes the taglib with the specified element & web application
     * to the server.
     *
     * @param   web_app   the specified web-app composite element.
     * @param   taglib    the specified taglib composite element.
     * @see #addWebAppTaglib(CompositeNusuthWebAppElement)
     */
    public void removeWebAppTaglib(CompositeNusuthWebAppElement web_app,
                                   CompositeNusuthWebAppElement taglib) {
        System.out.println("removeWebAppTaglib");
    }

    /**
     * Gets the usres for the specified web-app composite element via proxy.
     *
     * @param   web_app   the specified web-app composite element.
     * @return  the users
     * @see #setWebAppUsers(CompositeNusuthWebAppElement,
            * CompositeNusuthWebAppElement)
     */
    public CompositeNusuthWebAppElement getWebAppUsers(
            CompositeNusuthWebAppElement web_app) {
        CompositeNusuthWebAppElement element = getCompositeElement(SWEBUSERS);
        return element;
    }

    /**
     * Commits the web app users to the server.
     *
     * @param   web_app   the specified web-app composite element.
     * @param   users     the specified users composite element.
     * @see #getWebAppUsers(CompositeNusuthWebAppElement)
     */
    public void setWebAppUsers(CompositeNusuthWebAppElement web_app,
                               CompositeNusuthWebAppElement users) {
        System.out.println("setWebAppUsers for the web_app = ");
        System.out.println(web_app.toString());
        System.out.println("users = ");
        System.out.println(users.toString());
    }
}
