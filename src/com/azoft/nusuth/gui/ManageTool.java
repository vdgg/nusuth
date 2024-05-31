/*
 * @(#)ManageTool.java 1.0 12/03/2000
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.help.*;
import com.azoft.nusuth.management.*;
import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.rmi.*;
import com.azoft.nusuth.deployment.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import java.text.*;

/**
 * Class ManageTool is the main gui class.
 *
 * @version 1.0 12/03/2000
 * @author  vdgg, tanya
 * @since Nusuth1.0
 */
public class ManageTool extends JFrame implements ActionListener, ComponentChangedListener {
    public final static int M_WIDTH = 500;
    public final static int M_HEIGHT = 400;
    public final static int DIVLOC_WIDTH = 100;
    public final static int M_LISTWIDTH = 100;
    public final static String UNAUTORIZED_WARNING = "You are unauthorized to this system";

    static GraphSaver graphSaver;

    private static String mainTitle = " Nusuth Servlet Engine Manager";
    private static int MAX_LOGIN_COUNT = 3;
    private static int counter;
    private static Properties props = null;

    private static JCheckBoxMenuItem switchStopItem;
    private static JMenu editorMenu;
    private static JMenu monitorMenu;
    private static JMenu clusterViewMenu;
    private static JMenu showMonitors;
    private static JPanel main;
    private static BasicPanel basicPanel;
    private static String host = null;
    private static int port = -1;
    private static RmiClusterManagerProxyImpl proxy;

    private static Object[] messages;
    private static String[] options = {"OK", "Cancel"};
    private static String[] types = {"container", "distributor"};
    private static JDialog monitorDialog;
    private static JTextField loginField;
    private static JPasswordField passwordField;
    private static JLabel warningString;
    private static JTextField monitorNameField;

    private static TimeChanger monitorStepTimeChanger;
    private static TimeChanger monitorHistoryTimeChanger;
    private static JComboBox monitorTypeComboBox;
    private static JButton backgroundButton;
    private static JButton gridColorButton;
    private static JLabel refreshLabel;
    private static JLabel historyLabel;
    private static JLabel periodLabel;
    private static PeriodPanel periodPanel;

    private static GraphsTable graphsTable;
    private static JPanel monitorPanel;
    private static JPanel graphsPanel;
    private static JPanel monitorListButtons;
    private static JPanel monitorButtons;
    private static String selectedValue;
    private static JPanel graphsButtons;
    private static JList monitorList;

    private static DateFormat dateFormat;

    Object[] reportRowCountMessages = null;
    NumberChanger reportRowCountChanger = null;
    Object[] savingDelayMessages = null;
    TimeChanger savingDelayTimeChanger = null;

    private String workDir;
    private Properties monitorProps = null;
    private Properties clusterViewProps = null;
    private JMenuBar menuBar;
    private JLabel statusLabel;
    private int divLoc = -1;
    private JDialog monitorListDialog;
    private ComboStatusLine statusLine;
    private int maxStatusRowCount = 10;
    private IntervalSettingsDialog intervalSettingsDialog;

    private JHLauncher launcher;
    private boolean connectionCanceled = false;

    public ManageTool(String workDir) {
        super(mainTitle);
        this.workDir = workDir;
        graphSaver = new GraphSaver(workDir);
        NusuthAppConfigFactory.addEntityResolver("web-app", new WebEntityResolver());
        this.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent we) {
                graphSaver.closeStreams();
                System.exit(0);
            }

            public void windowClosing(WindowEvent we) {
                try {
                    saveComponentBoundsToProperties(ManageTool.this, "", props);
                    if (launcher != null) saveComponentBoundsToProperties(ManageTool.this.getHelpLauncher().getFrame(), "help_", props);
                    if (monitorListDialog != null) saveComponentBoundsToProperties(ManageTool.this.getMonitorListDialog(), "monitorlist_", props);
                    if (monitorDialog != null) saveComponentBoundsToProperties(ManageTool.this.getAddMonitorDialog(), "monitoredit_", props);
                    if (intervalSettingsDialog != null) saveComponentBoundsToProperties(ManageTool.this.getIntervalSettingsDialog(), "intervalSettings_", props);

                    TablePanelFactory.savePrefSizes(props);

                    props.setProperty("stop_on_switch", "" + getSwitchStop());

                    if (UIManager.getLookAndFeel() != null) {
                        props.setProperty("look&feel", UIManager.getLookAndFeel().getName());
                    }
                    if (host != null) {
                        props.setProperty("host", host);
                    }
                    if (port > -1 && port < 65536) {
                        props.setProperty("port", "" + port);
                    }
                    if (basicPanel != null) {
                        props.setProperty("split-pos", "" + basicPanel.splitPane.getDividerLocation());
                    }
                    OutputStream os = new FileOutputStream(ManageTool.this.workDir + File.separator + "managetool.properties");
                    props.store(os, "ManageTool properties - do not modify");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                storeProperties(monitorProps, basicPanel, "monitors", new SaveAction() {
                    public void does() {
                        basicPanel.monitorContainer.saveMonitorProperties(monitorProps);
                    }
                });
                storeProperties(clusterViewProps, basicPanel, "clusterView", new SaveAction() {
                    public void does() {
                        basicPanel.clusterViewPanel.saveClusterViewProperties(clusterViewProps);
                    }
                });
            }
        });
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        createMainPanel();

        props = new Properties();
        monitorProps = new Properties();
        clusterViewProps = new Properties();

        loadMainProperty();
        loadProperty("monitors", monitorProps);
        loadProperty("clusterView", clusterViewProps);

        createMenus();

        show();
        proxy = new RmiClusterManagerProxyImpl();

        final JDialog dialog = MessageDialog.getDialog(getMainFrame(), " Loading", "Creating GUI components...", true);
        Thread t = new Thread(new Runnable() {
            public void run() {
                basicPanel = new BasicPanel(ManageTool.this.divLoc, statusLine,
                        ManageTool.this.monitorProps, ManageTool.this.clusterViewProps);

                startProxyThread(dialog);

                basicPanel.addComponentChangedListener(ManageTool.this);
                main.add(basicPanel, BorderLayout.CENTER);
                main.updateUI();
                ManageTool.this.addMonitorsItem();
            }
        });
        t.start();
        dialog.show();
    }

    private void loadMainProperty() {
        String lf = null;
        try {
            InputStream is = new FileInputStream(workDir + File.separator + "managetool.properties");
            props.load(is);

            host = props.getProperty("host");
            try {
                port = Integer.parseInt(props.getProperty("port"));
            } catch (Exception ex) {
            }
            try {
                divLoc = Integer.parseInt(props.getProperty("split-pos"));
            } catch (Exception ex) {
            }

            lf = props.getProperty("look&feel");
            String sdf = props.getProperty("dateFormat", "hh:mm:ss");
            dateFormat = new SimpleDateFormat(sdf);

            TablePanelFactory.setPrefSizes(props);

        } catch (IOException ioex) {
            dateFormat = new SimpleDateFormat("hh:mm:ss");
        }

        setBoundsFromProperties(this, "");

        if (lf != null) {
            UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
            if (lafs != null) {
                UIManager.LookAndFeelInfo jlf = null;
                for (int i = 0; i < lafs.length; i++) {
                    if (lafs[i].getName().equalsIgnoreCase(lf)) {
                        jlf = lafs[i];
                        break;
                    }
                }
                if (jlf != null) {
                    try {
                        UIManager.setLookAndFeel(jlf.getClassName());
                        SwingUtilities.updateComponentTreeUI(this);
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    private void createMenus() {
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        buildEditorMenu();
        menuBar.add(editorMenu);
        buildMonitorMenu();
        menuBar.add(monitorMenu);
        buildClusterViewMenu();
        menuBar.add(clusterViewMenu);
        JMenu optionsMenu = buildOptionsMenu();
        menuBar.add(optionsMenu);
        JMenu helpMenu = buildHelpMenu();
        menuBar.add(helpMenu);
    }

    private void loadProperty(String name, Properties pp) {
        try {
            InputStream is = new FileInputStream(workDir + File.separator + name + ".properties");
            pp.load(is);
        } catch (IOException ioex) {
        }
    }

    private interface SaveAction {
        void does();
    }

    private void storeProperties(Properties ps, Object checkNull, String name, SaveAction action) {
        try {
            if (ps != null && checkNull != null) {
                action.does();
            }
            OutputStream osm = new FileOutputStream(ManageTool.this.workDir + File.separator + name + ".properties");
            ps.store(osm, name + " properties - do not modify");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void buildEditorMenu() {
        editorMenu = new JMenu("Editing");
        JMenuItem addItem = editorMenu.add("Add component");
        addItem.setActionCommand("addContainer");
        addItem.addActionListener(this);
        editorMenu.setEnabled(true);
    }

    private void buildMonitorMenu() {
        monitorMenu = new JMenu("Monitoring");
        showMonitors = new JMenu("Show monitors");
        monitorMenu.add(showMonitors);
        JMenuItem item = monitorMenu.add("Edit monitors...");
        item.setActionCommand("editMonitors");
        item.addActionListener(this);
        monitorMenu.setEnabled(true);
        item = monitorMenu.add("Create history monitor...");
        item.setActionCommand("print_graphs");
        item.addActionListener(this);
    }

    private void buildClusterViewMenu() {
        clusterViewMenu = new JMenu("ClusterView");
        boolean state = !clusterViewProps.getProperty("paintText", "").equals("false");
        JCheckBoxMenuItem switchShowNamesItem = new JCheckBoxMenuItem("Show names", state);
        switchShowNamesItem.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                basicPanel.clusterViewPanel.setShowNames(((JCheckBoxMenuItem) e.getSource()).getState());
            }
        });
        clusterViewMenu.add(switchShowNamesItem);
        JMenuItem mi = clusterViewMenu.add("Layout with stars");
        mi.setActionCommand("layoutClustewView");
        mi.addActionListener(this);
        mi = clusterViewMenu.add("Interval settings ...");
        mi.setActionCommand("intervalSettings");
        mi.addActionListener(this);
    }

    private void addMonitorsItem() {
        Enumeration e = getMonitors().elements();
        JCheckBoxMonitorMenuItem item;
        while (e.hasMoreElements()) {
            addNewMonitorsItem((Monitor) e.nextElement());
        }
    }

    private void addNewMonitorsItem(final Monitor monitor) {
        JCheckBoxMonitorMenuItem item = monitor.getMenuItem();
        item.setState(monitor.getShowing());
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JCheckBoxMonitorMenuItem chb = (JCheckBoxMonitorMenuItem) ae.getSource();
                monitor.setShowing(chb.isSelected());
            }
        });
        showMonitors.add(item);
    }

    private void removeMonitorsItem(Monitor monitor) {
        showMonitors.remove(monitor.getMenuItem());
    }

    private JMenu buildOptionsMenu() {
        JMenu optionsMenu = new JMenu("Options");
        JMenuItem conOption = optionsMenu.add("Connection...");
        conOption.setActionCommand("connect");
        conOption.addActionListener(this);
        JMenuItem login = optionsMenu.add("Login as...");
        login.setActionCommand("login");
        login.addActionListener(this);
        UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
        if (lafs.length > 0) {
            final Hashtable laf2classes = new Hashtable(5);
            JMenu lafMenu = new JMenu("Look&Feel");
            optionsMenu.add(lafMenu);
            JMenuItem mi;
            for (int i = 0; i < lafs.length; i++) {
                mi = lafMenu.add(lafs[i].getName());
                laf2classes.put(lafs[i].getName(), lafs[i].getClassName());
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            UIManager.setLookAndFeel((String) laf2classes.get(e.getActionCommand()));
                            SwingUtilities.updateComponentTreeUI(ManageTool.this);
                            if (ManageTool.this.monitorListDialog != null) SwingUtilities.updateComponentTreeUI(ManageTool.this.monitorListDialog);
                            if (ManageTool.this.monitorDialog != null) SwingUtilities.updateComponentTreeUI(ManageTool.this.monitorDialog);
                            if (ManageTool.this.intervalSettingsDialog != null) SwingUtilities.updateComponentTreeUI(ManageTool.this.intervalSettingsDialog);
                            getHelpLauncher().updateUI();
                            MessageDialog.updateUI();
                            for (int m = 0; m < messages.length; m++) {
                                if (messages[m] instanceof Component)
                                    SwingUtilities.updateComponentTreeUI((Component) messages[m]);
                            }
                        } catch (Exception ex) {
                            System.err.println("Cannot set l&f, nested: " + ex);
                        }
                    }
                });
            }
        }
        boolean state = !props.getProperty("stop_on_switch", "true").equals("false");
        switchStopItem = new JCheckBoxMenuItem("Stop monitor on switch", state);
        optionsMenu.add(switchStopItem);
        JMenuItem rci = optionsMenu.add("Edit report row count...");
        rci.setActionCommand("editReportRowCount");
        rci.addActionListener(this);
        rci = optionsMenu.add("Edit monitor saving delay...");
        rci.setActionCommand("editSavingDelay");
        rci.addActionListener(this);

        return optionsMenu;
    }

    private JMenu buildHelpMenu() {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem topics = helpMenu.add("Help topics...");
        topics.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        topics.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getHelpLauncher().launch();
            }
        });
        return helpMenu;
    }

    private JHLauncher getHelpLauncher() {
        if (launcher == null) {
            launcher = new JHLauncher();
            setBoundsFromProperties(launcher.getFrame(), "help_");
        }
        return launcher;
    }

    private void createMainPanel() {
        main = new JPanel(new BorderLayout());
        setContentPane(main);
        statusLine = new ComboStatusLine(maxStatusRowCount);
        MessageDialog.setComboStatusLine(statusLine);
        main.add(statusLine, BorderLayout.SOUTH);
    }

    private void startProxyThread(final JDialog dialog) {
        Thread t = new Thread(new Runnable() {
            public void run() {
                MessageDialog.setMessage("Connecting to " + host + " : " + port + "...");
                doConnect(dialog);
                if (!connectionCanceled) basicPanel.startThreads(dialog);
                dialog.dispose();
            }
        });
        t.start();
    }

    private void initBasicPanelTree() {
        final JDialog dialog = MessageDialog.getDialog(getMainFrame(), " Loading", "Tree initialization...", true);
        Thread t = new Thread(new Runnable() {
            public void run() {
                basicPanel.startThreads(dialog);
                dialog.dispose();
            }
        });
        t.start();
        dialog.show();
    }

    private void newConnection() {
        final JDialog dialog = MessageDialog.getDialog(getMainFrame(), " Loading", "New host/port connecting...", true);
        startProxyThread(dialog);
        dialog.show();
    }

    private void doConnect(final JDialog dialog) {
        connectionCanceled = false;
        RequestThread rt = new RequestThread() {
            protected void doWork() throws Exception {
                if (host == null || port == -1) doConnectionOptions();
                if (host != null || port != -1) proxy.setServerLocation(host, port); // can throw management exc
            }
        };
        rt.setRequestThreadListener(new RequestThreadListener() {
            public void workFinished(RequestThreadEvent e) {
                if (!e.isSuccess()) {
                    System.out.println(e.getException());
//					if (new RetryDialog(ManageTool.this, "Cannot connect to "+host+" : "+port).isRetry())
                    if (new RetryDialog(dialog, "Cannot connect to " + host + " : " + port).isRetry())
                        doConnect(dialog);
                    else
                        ManageTool.this.connectionCanceled = true;
                }
            }
        });
        rt.run();
    }

    private void doConnectionOptions() {
        ConnectionDialog conDialog = new ConnectionDialog((Component) this,
//															  (int)getBounds().getCenterX(), (int)getBounds().getCenterY(),
                host == null ? "localhost" : host, port < 0 ? 1099 : port);
        conDialog.show();
        host = conDialog.getHost();
        port = conDialog.getPort();
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals("connect")) {
            doConnectionOptions();
            newConnection();
        } else if (command.equals("login")) {
            int res = showLoginDialog();
            if (res == JOptionPane.NO_OPTION || res == JOptionPane.CLOSED_OPTION) {
                statusLine.setStatusString("you are not authorized ....");
                basicPanel.clearTree();
            } else
                initBasicPanelTree();
        } else if (command.equals("addContainer")) {
            basicPanel.addComponent("container");
        } else if (command.equals("addDistributor")) {
            basicPanel.addComponent("distributor");
        } else if (command.equals("addDeployer")) {
            basicPanel.addComponent("deployer");
        } else if (command.equals("editReportRowCount")) {
            editReportRowCount();
        } else if (command.equals("editSavingDelay")) {
            editSavingDelay();
        } else if (command.equals("editMonitors")) {
            showMonitorList();
        } else if (command.equals("print_graphs")) {
            MonitorInfo mi = showAddMonitorDialog(null, true);
            if (mi != null) {
                DefaultListModel dlm = (DefaultListModel) getMonitorList().getModel();
                Monitor m = basicPanel.monitorContainer.addHistoryMonitor(mi);
                dlm.addElement(mi);
                addNewMonitorsItem(m);
            }
        } else if (command.equals("layoutClustewView")) {
            basicPanel.clusterViewPanel.doStarLayout();
        } else if (command.equals("intervalSettings")) {
            IntervalSettingsDialog dialog = getIntervalSettingsDialog();
            dialog.show();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            usage();
            System.exit(0);
        }
        new ManageTool(args[0]);
    }

    private static void usage() {
        System.out.println("Usage java ManageTool %workDir%");
    }

    public static int showLoginDialog() {
        return showLoginDialog(getMainFrame());
    }

    public static int showLoginDialog(Component parentComponent) {
        counter = MAX_LOGIN_COUNT;
        getAuthString().setText("");
        try {
            return login(parentComponent);
        } catch (UnauthorizedAccessException ex) {
            getAuthString().setText(UNAUTORIZED_WARNING);
            return showLoginDialog2(parentComponent);
        } catch (ManagementException mex) {
            showMessage(mex.toString());
            return JOptionPane.CLOSED_OPTION;
        }
    }

    private static int showLoginDialog2(Component parentComponent) {
        try {
            if (--counter > 0)
                return login(parentComponent);
            else {
                showMessage("you have taped wrong password for the " + MAX_LOGIN_COUNT + "th time");
                return JOptionPane.CLOSED_OPTION;
            }
        } catch (UnauthorizedAccessException ex) {
            return showLoginDialog2(parentComponent);
        } catch (ManagementException mex) {
            showMessage(mex.toString());
            return JOptionPane.CLOSED_OPTION;
        }
    }

    public static int login() throws UnauthorizedAccessException, ManagementException {
        return login(getMainFrame());
    }

    public static int login(Component parentComponent) throws UnauthorizedAccessException, ManagementException {
        if (messages == null) {
            GridBagLayout gridbag = new GridBagLayout();
            JPanel p = new JPanel(gridbag);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 10, 4, 10);

            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = 1;
            p.add(getAuthString(), c);

            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            c.weightx = 0.0;
            p.add(new JLabel("Login:"), c);

            c.insets = new Insets(4, 0, 4, 10);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1.0;
            p.add(getLoginField(), c);

            c.insets = new Insets(4, 10, 4, 10);
            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            c.weightx = 0.0;
            p.add(new JLabel("Password:"), c);

            c.insets = new Insets(4, 0, 4, 10);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1.0;
            p.add(getPasswordField(), c);

            messages = new Object[1];
            messages[0] = p;
        }
        getLoginField().setText("");
        getPasswordField().setText("");
        getPasswordField().setCaretPosition(0);
//	int res = JOptionPane.showOptionDialog(basicPanel, messages, "Login dialog", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        int res = JOptionPane.showOptionDialog(parentComponent, messages, "Login dialog", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (res == JOptionPane.OK_OPTION) {
            String name = getLoginField().getText();
            String password = MD5.cryptPassword(new String(getPasswordField().getPassword()));
            proxy.login(name, password);
        }
        return res;
    }

    public static JLabel getAuthString() {
        if (warningString == null) {
            warningString = new JLabel("");
            warningString.setForeground(Color.red);
        }
        return warningString;
    }

    public JDialog getMonitorListDialog() {
        if (monitorListDialog == null) {
            monitorListDialog = new JDialog(ManageTool.this, "Edit monitors", true);
            setBoundsFromProperties(monitorListDialog, "monitorlist_");

            JPanel p = new JPanel(new BorderLayout(0, 0));
            p.add("Center", new JScrollPane(getMonitorList()));
            p.add("East", getMonitorListButtons());
            monitorListDialog.getContentPane().add(p);
        }
        return monitorListDialog;
    }

    public void showMonitorList() {
        getMonitorListDialog().show();
    }

    private JPanel getMonitorListButtons() {
        if (monitorListButtons == null) {
            monitorListButtons = new JPanel();
            GridBagLayout gridbag = new GridBagLayout();
            JPanel p = new JPanel(gridbag);
            monitorListButtons.add("North", p);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(1, 1, 1, 1);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = 1;

            JButton add = new JButton("Add ...");
            add.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    MonitorInfo mi = showAddMonitorDialog(null);
                    if (mi != null) {
                        Monitor m = basicPanel.monitorContainer.addMonitor(mi);
                        ((DefaultListModel) getMonitorList().getModel()).addElement(mi);
                        addNewMonitorsItem(m);
                    }
                }
            });
            p.add(add, c);
            JButton edit = new JButton("Edit ...");
            edit.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    editSelectedMonitor();
                }
            });
            p.add(edit, c);
            JButton remove = new JButton("Remove");
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int[] indices = getMonitorList().getSelectedIndices();
                    for (int i = indices.length - 1; i >= 0; i--) {
                        Monitor m = basicPanel.monitorContainer.removeMonitor((Monitor) getMonitors().elementAt(indices[i]));
                        ((DefaultListModel) getMonitorList().getModel()).remove(indices[i]);
                        removeMonitorsItem(m);
                    }
                }
            });
            p.add(remove, c);
            JButton close = new JButton("Close");
            close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getMonitorListDialog().setVisible(false);
                }
            });
            p.add(close, c);
        }
        return monitorListButtons;
    }

    private static JPanel getMonitorButtons() {
        if (monitorButtons == null) {
            monitorButtons = new JPanel();
            JButton ok = new JButton("OK");
            ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectedValue = "OK";
                    getAddMonitorDialog().setVisible(false);
                }
            });
            monitorButtons.add(ok);
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectedValue = "Cancel";
                    getAddMonitorDialog().setVisible(false);
                }
            });
            monitorButtons.add(cancel);
        }
        return monitorButtons;
    }

    public static JDialog getAddMonitorDialog() {
        return getAddMonitorDialog(false);
    }

    public static JDialog getAddMonitorDialog(boolean beHistoryMonitor) {
        if (monitorDialog == null) {
            monitorDialog = new JDialog(getMainFrame(), "Edit monitor", true);
            setBoundsFromProperties(monitorDialog, "monitoredit_");

            JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
            tabbedPane.addTab("General", getMonitorPanel(beHistoryMonitor));
            tabbedPane.addTab("Graphs", getGraphsPanel());

            JPanel p = new JPanel(new BorderLayout());
            p.add("Center", tabbedPane);
            p.add("South", getMonitorButtons());
            monitorDialog.getContentPane().add(p);
        }
        getRefreshLabel().setEnabled(!beHistoryMonitor);
        getMonitorStepTimeChanger().setEnabled(!beHistoryMonitor);
        getMonitorHistoryTimeChanger().setEnabled(!beHistoryMonitor);
        getHistoryLabel().setEnabled(!beHistoryMonitor);
        getPeriodLabel().setEnabled(beHistoryMonitor);
        getPeriodPanel().setEnabled(beHistoryMonitor);
        return monitorDialog;
    }

    public static MonitorInfo showAddMonitorDialog(MonitorInfo monitorInfo) {
        return showAddMonitorDialog(monitorInfo, false);
    }

    public static MonitorInfo showAddMonitorDialog(MonitorInfo monitorInfo, boolean beHistoryMonitor) {
        String type = (monitorInfo == null) ? "container" : monitorInfo.type;
        getGraphsTable().fireTypeChanged(type);
        String title = (monitorInfo == null) ? "Add " : "Edit ";
        title += (beHistoryMonitor) ? "history monitor, available time is " + PeriodPanel.formatLong(MonitorContainer.beginTime) + " - now" : "monitor";
        JDialog dialog = getAddMonitorDialog(beHistoryMonitor);
        setInfo(monitorInfo);
        dialog.setTitle(title);
        dialog.show();
        if (selectedValue != null && selectedValue.equals("OK"))
            return getInfo(monitorInfo);
        return null;
    }

    private static JPanel getMonitorPanel(boolean beHistoryMonitor) {
        if (monitorPanel == null) {
            GridBagLayout gridbag = new GridBagLayout();
            monitorPanel = new JPanel(gridbag);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.gridy = -1;
            createDialogRow(monitorPanel, c, new JLabel("Monitor name:"), getMonitorNameField());
            createDialogRow(monitorPanel, c, new JLabel("Component type:"), getMonitorTypeComboBox());
            createDialogRow(monitorPanel, c, getRefreshLabel(), getMonitorStepTimeChanger());
            createDialogRow(monitorPanel, c, getHistoryLabel(), getMonitorHistoryTimeChanger());
            createDialogRow(monitorPanel, c, new JLabel("Background:"), getBackgroundButton());
            createDialogRow(monitorPanel, c, new JLabel("Grid color:"), getGridColorButton());
            createDialogRow(monitorPanel, c, getPeriodLabel(), getPeriodPanel());
        }
        return monitorPanel;
    }

    private static JLabel getRefreshLabel() {
        if (refreshLabel == null) refreshLabel = new JLabel("Refresh:");
        return refreshLabel;
    }

    private static JLabel getHistoryLabel() {
        if (historyLabel == null) historyLabel = new JLabel("History:");
        return historyLabel;
    }

    private static JLabel getPeriodLabel() {
        if (periodLabel == null) periodLabel = new JLabel("Period:");
        return periodLabel;
    }

    private static PeriodPanel getPeriodPanel() {
        if (periodPanel == null) {
            periodPanel = new PeriodPanel();
        }
        return periodPanel;
    }

    private static JPanel getGraphsPanel() {
        if (graphsPanel == null) {
            graphsPanel = new JPanel(new BorderLayout());
            graphsPanel.add("Center", new JScrollPane(getGraphsTable()));
            graphsPanel.add("East", getGraphsButtons());
        }
        return graphsPanel;
    }

    public static void showMessage(String mes) {
        showMessage(mes, true);
    }

    public static void showMessage(String mes, String title) {
        showTitleMessage(mes, title, true);
    }

    public static void showMessage(String mes, boolean doPack) {
        MessageDialog.showMessage(getMainFrame(), mes, doPack);
    }

    public static void showLoadMessage(String mes, boolean doPack) {
        MessageDialog.showMessage(getMainFrame(), " Loading", mes, doPack);
    }

    public static void showTitleMessage(String mes, String title, boolean doPack) {
        MessageDialog.showMessage(getMainFrame(), " " + title, mes, doPack);
    }

    public static void hideMessage() {
        MessageDialog.hideMessage();
    }

    private static JPasswordField getPasswordField() {
        if (passwordField == null) passwordField = new JPasswordField();
        return passwordField;
    }

    private static JTextField getLoginField() {
        if (loginField == null) loginField = new JTextField();
        return loginField;
    }

    private static Vector getMonitors() {
        return basicPanel.monitorContainer.getMonitors();
    }

    private static JList getMonitorList() {
        if (monitorList == null) {
            DefaultListModel model = new DefaultListModel();
            Enumeration e = getMonitors().elements();
            while (e.hasMoreElements()) {
                model.addElement(((Monitor) e.nextElement()).getMonitorInfo());
            }
            monitorList = new JList(model);
            monitorList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent ev) {
                    if (ev.getClickCount() == 2) {
                        editSelectedMonitor();
                    }
                }
            });
        }
        return monitorList;
    }

    private static JTextField getMonitorNameField() {
        if (monitorNameField == null) monitorNameField = new JTextField();
        return monitorNameField;
    }

    private static JComboBox getMonitorTypeComboBox() {
        if (monitorTypeComboBox == null) {
            monitorTypeComboBox = new JComboBox();
            for (int i = 0; i < types.length; i++) {
                monitorTypeComboBox.addItem(types[i]);
            }
            monitorTypeComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String type = (String) monitorTypeComboBox.getSelectedItem();
                    getGraphsTable().fireTypeChanged(type);
                }
            });
        }
        return monitorTypeComboBox;
    }

    private static TimeChanger getMonitorStepTimeChanger() {
        if (monitorStepTimeChanger == null) monitorStepTimeChanger = new TimeChanger(false, true);
        return monitorStepTimeChanger;
    }

    private static TimeChanger getMonitorHistoryTimeChanger() {
        if (monitorHistoryTimeChanger == null) monitorHistoryTimeChanger = new TimeChanger(false, true);
        return monitorHistoryTimeChanger;
    }

    private static JButton getBackgroundButton() {
        if (backgroundButton == null) {
            backgroundButton = new JButton("") {
                public Dimension getPreferredSize() {
                    return getMonitorHistoryTimeChanger().getPreferredSize();
                }

                public Dimension getMaximumSize() {
                    return getMonitorHistoryTimeChanger().getMaximumSize();
                }

                public Dimension getMinimumSize() {
                    return getMonitorHistoryTimeChanger().getMinimumSize();
                }
            };
            backgroundButton.setBackground(Color.white);
            backgroundButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Color c = JColorChooser.showDialog(backgroundButton, "Select the background, please", backgroundButton.getBackground());
                    if (c != null) backgroundButton.setBackground(c);
                }
            });
        }
        return backgroundButton;
    }

    private static JButton getGridColorButton() {
        if (gridColorButton == null) {
            gridColorButton = new JButton("") {
                public Dimension getPreferredSize() {
                    return getMonitorHistoryTimeChanger().getPreferredSize();
                }

                public Dimension getMaximumSize() {
                    return getMonitorHistoryTimeChanger().getMaximumSize();
                }

                public Dimension getMinimumSize() {
                    return getMonitorHistoryTimeChanger().getMinimumSize();
                }
            };
            gridColorButton.setBackground(Color.white);
            gridColorButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Color c = JColorChooser.showDialog(gridColorButton, "Select the background, please", gridColorButton.getBackground());
                    if (c != null) gridColorButton.setBackground(c);
                }
            });
        }
        return gridColorButton;
    }

    private static GraphsTable getGraphsTable() {
        if (graphsTable == null) {
            graphsTable = new GraphsTable();
        }
        return graphsTable;
    }

    private static JPanel getGraphsButtons() {
        if (graphsButtons == null) {
            graphsButtons = new JPanel();
            GridBagLayout gridbag = new GridBagLayout();
            JPanel p = new JPanel(gridbag);
            graphsButtons.add("North", p);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(1, 1, 1, 1);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = 1;

            JButton addGraph = new JButton("Add");
            addGraph.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    getGraphsTable().addRow();
                }
            });
            p.add(addGraph, c);
            JButton removeGraph = new JButton("Remove");
            removeGraph.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int cnt = getGraphsTable().getSelectedRowCount();
                    while (cnt-- > 0) {
                        int sel = getGraphsTable().getSelectedRow();
                        if (sel > -1) getGraphsTable().removeRowAt(sel);
                    }
                }
            });
            p.add(removeGraph, c);
        }
        return graphsButtons;
    }

    private static void createDialogRow(JPanel p, GridBagConstraints c, JLabel label, JComponent comp) {
        c.gridx = 0;
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 0.0;
        p.add(label, c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        p.add(comp, c);
    }

    private static void setInfo(MonitorInfo monitorInfo) {
        getMonitorNameField().setText((monitorInfo == null) ? "" : monitorInfo.name);
        getMonitorTypeComboBox().setSelectedItem((monitorInfo == null) ? "container" : monitorInfo.type);
        getMonitorStepTimeChanger().setSeconds((monitorInfo == null) ? "1" : monitorInfo.step);
        getMonitorHistoryTimeChanger().setSeconds((monitorInfo == null) ? "50" : monitorInfo.history);
        getBackgroundButton().setBackground(Graph.getColor((monitorInfo == null) ? "0, 0, 0" : monitorInfo.background));
        getGridColorButton().setBackground(Graph.getColor((monitorInfo == null) ? Graph.getColorString(Monitor.DEFAULT_GRID) : monitorInfo.gridColor));
        Vector v = (monitorInfo == null) ? new Vector() : monitorInfo.graphs;
        getPeriodPanel().setFromMillis((monitorInfo == null || monitorInfo.beginTime == 0) ? MonitorContainer.beginTime : monitorInfo.beginTime);
        getPeriodPanel().setToMillis((monitorInfo == null || monitorInfo.endTime == 0) ? MonitorContainer.beginTime : monitorInfo.endTime);
        getGraphsTable().setGraphInfos(v);
    }

    private static MonitorInfo getInfo(MonitorInfo monitorInfo) {
        if (monitorInfo == null) monitorInfo = new MonitorInfo();
        monitorInfo.name = getMonitorNameField().getText();
        monitorInfo.type = getMonitorTypeComboBox().getSelectedItem().toString();
        monitorInfo.step = getMonitorStepTimeChanger().getSeconds() + "";
        monitorInfo.history = getMonitorHistoryTimeChanger().getSeconds() + "";
        monitorInfo.background = Graph.getColorString(getBackgroundButton().getBackground());
        monitorInfo.gridColor = Graph.getColorString(getGridColorButton().getBackground());
        monitorInfo.graphs = getGraphsTable().getGraphInfos();
        monitorInfo.beginTime = getPeriodPanel().getFromMillis();
        monitorInfo.endTime = getPeriodPanel().getToMillis();
        return monitorInfo;
    }

    private void saveComponentBoundsToProperties(Component c, String s, Properties pp) {
        pp.setProperty(s + "x-pos", "" + c.getX());
        pp.setProperty(s + "y-pos", "" + c.getY());
        pp.setProperty(s + "width", "" + c.getWidth());
        pp.setProperty(s + "height", "" + c.getHeight());
    }

    private static void setBoundsFromProperties(Component component, String s) {
        int xLoc = Integer.MIN_VALUE;
        int yLoc = Integer.MIN_VALUE;
        int fWidth = -1;
        int fHeight = -1;
        try {
            xLoc = Integer.parseInt(props.getProperty(s + "x-pos"));
        } catch (Exception ex) {
        }
        try {
            yLoc = Integer.parseInt(props.getProperty(s + "y-pos"));
        } catch (Exception ex) {
        }
        try {
            fWidth = Integer.parseInt(props.getProperty(s + "width"));
        } catch (Exception ex) {
        }
        try {
            fHeight = Integer.parseInt(props.getProperty(s + "height"));
        } catch (Exception ex) {
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (xLoc == Integer.MIN_VALUE) {
            int width = (fWidth > 0) ? (int) Math.min(fWidth, screenSize.getWidth()) : M_WIDTH;
            xLoc = (int) (screenSize.getWidth() - width) / 2;
        }
        if (yLoc == Integer.MIN_VALUE) {
            int height = (fHeight > 0) ? (int) Math.min(fHeight, screenSize.getHeight()) : M_HEIGHT;
            yLoc = (int) (screenSize.getHeight() - height) / 2;
        }
        if (fWidth <= 0) {
            fWidth = M_WIDTH + xLoc > screenSize.getWidth() ? (int) screenSize.getWidth() - xLoc : M_WIDTH;
        }
        if (fHeight <= 0) {
            fHeight = M_HEIGHT + yLoc > screenSize.getHeight() ? (int) screenSize.getHeight() - yLoc : M_HEIGHT;
        }
        component.setBounds(xLoc, yLoc, fWidth, fHeight);
    }

    private static void editSelectedMonitor() {
        int index = getMonitorList().getSelectedIndex();
        MonitorInfo mi = (MonitorInfo) getMonitorList().getSelectedValue();
        if (mi != null) {
            MonitorInfo newmi = showAddMonitorDialog(mi);
            if (newmi != null) {
                ((Monitor) getMonitors().elementAt(index)).setMonitorInfo(newmi);
                ((DefaultListModel) getMonitorList().getModel()).setElementAt(mi, index);
            }
        }
    }

    public static JFrame getMainFrame() {
        for (Container p = main.getParent(); p != null; p = p.getParent()) {
            if (p instanceof JFrame) {
                return (JFrame) p;
            }
        }
        return null;
//		return (JFrame)SwingUtilities.getWindowAncestor(main);
    }

    public static RmiClusterManagerProxyImpl getProxy() {
        return proxy;
    }

    public static BasicPanel getBasicPanel() {
        return basicPanel;
    }

    public static JMenu getMonitorMenu() {
        return monitorMenu;
    }

    public static JMenu getEditorMenu() {
        return editorMenu;
    }

    public static JMenu getClusterViewMenu() {
        return clusterViewMenu;
    }

    public void setSwitchStop(boolean b) {
        switchStopItem.setState(b);
    }

    public static boolean getSwitchStop() {
        return switchStopItem.isSelected();
    }

    public static DateFormat getDateFormat() {
        return dateFormat;
    }

    public static JMenu getShowMonitors() {
        return showMonitors;
    }

    public static String getUserName() {
        return getLoginField().getText();
//		return "igork";
    }

    public void componentChanged(int tab, String type, String id) {
        if (tab == ComponentChangedListener.EDITING) {
            setTitle(" Edit " + type + " " + id + " on " + host + " -" + mainTitle);
        } else if (tab == ComponentChangedListener.MONITORING) {
            if (!id.equals(""))
                setTitle(" " + id + " monitor on " + host + " -" + mainTitle);
            else
                setTitle(" Monitors on " + host + " -" + mainTitle);
        } else {
            // if cluster view
            setTitle(" Cluster view on " + host + " -" + mainTitle);
        }
        getEditorMenu().setEnabled(tab == ComponentChangedListener.EDITING);
        getMonitorMenu().setEnabled(tab == ComponentChangedListener.MONITORING);
        getClusterViewMenu().setEnabled(tab == ComponentChangedListener.CLUSTER_VIEW);
    }

    private void editReportRowCount() {
        if (reportRowCountMessages == null) {
            JPanel p = new JPanel();
            reportRowCountChanger = new NumberChanger(1, 1, 30);
            reportRowCountChanger.setValue("" + (ComboStatusLine.getRowCount() - 1));
            p.add(new JLabel("Set report row count, please"));
            p.add(reportRowCountChanger);
            reportRowCountMessages = new Object[1];
            reportRowCountMessages[0] = p;
        }
        int res = JOptionPane.showOptionDialog(getMainFrame(), reportRowCountMessages, "Report row count editing", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (res == JOptionPane.OK_OPTION) {
            String num = reportRowCountChanger.getValue();
            try {
                int v = new Integer(num).intValue();
                ComboStatusLine.setMaxRowCount(v);
            } catch (Exception e) {
            }
        } else
            reportRowCountChanger.setValue("" + (ComboStatusLine.getRowCount() - 1));
    }

    private void editSavingDelay() {
        if (savingDelayMessages == null) {
            JPanel p = new JPanel();
            savingDelayTimeChanger = new TimeChanger();
            savingDelayTimeChanger.setSeconds((int) basicPanel.monitorContainer.getSavingDelay() / 1000);
            p.add(new JLabel("Set saving delay, please"));
            p.add(savingDelayTimeChanger);
            savingDelayMessages = new Object[1];
            savingDelayMessages[0] = p;
        }
        int res = JOptionPane.showOptionDialog(getMainFrame(), savingDelayMessages, "Saving delay editing", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (res == JOptionPane.OK_OPTION) {
            int sec = savingDelayTimeChanger.getSeconds();
            basicPanel.monitorContainer.setSavingDelay(1000 * sec);
        } else
            savingDelayTimeChanger.setSeconds((int) basicPanel.monitorContainer.getSavingDelay() / 1000);
    }

    public static String getHost() {
        return host;
    }

    public static int getPort() {
        return port;
    }

    private IntervalSettingsDialog getIntervalSettingsDialog() {
        if (intervalSettingsDialog == null) {
            intervalSettingsDialog = new IntervalSettingsDialog(this, basicPanel.clusterViewPanel.controlColoredLine);
            setBoundsFromProperties(intervalSettingsDialog, "intervalSettings_");
        }
        return intervalSettingsDialog;
    }
}

