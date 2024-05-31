package com.azoft.nusuth.gui;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EtchedBorder;

import com.azoft.nusuth.deployment.*;

public class UnboundedCompositeElementPanelFactory extends CompositeElementPanelFactory implements TabNameChangedListener {
    protected String childName = "";
    protected Vector compositeFactories = null;
    protected CompositeNusuthWebAppElement parent;
    protected JTabbedPane pane;
    protected JPanel buttons;
    protected JPanel panel;
    protected String path;
    private String[] tabNameComponents;
//  protected String pathOr;

    public UnboundedCompositeElementPanelFactory(String type, String childName, CompositeNusuthWebAppElement parent) {
        super(type, parent);
        this.childName = childName;
        this.tag = (childName.endsWith("y")) ? childName.substring(0, childName.length() - 1) + "ies" : childName + "s";
//    this.pathOr = type;
        this.path = (type.endsWith("y")) ? type.substring(0, type.length() - 1) + "ies" : type + "s";
        this.parent = parent;
        setTabNameComponents();
    }

    /**
     * Sets the component names array for a tab naming.
     */
    protected void setTabNameComponents() {
        String compNames = DefaultEditorPanel.getDisplayCompName(childName);
        StringTokenizer st = new StringTokenizer(compNames, ",");
        tabNameComponents = new String[st.countTokens()];
        int cnt = 0;
        while (st.hasMoreElements()) {
            tabNameComponents[cnt++] = st.nextToken();
        }
    }

    public Enumeration getCompositePanelFactories() {
        if (parent == null) return null;
        if (compositeFactories == null) {
            compositeFactories = new Vector();
            Enumeration e = null;
            try {
                e = parent.getCompositeChild(childName);
            } catch (DeploymentException de) {
            }
            if (e != null && e.hasMoreElements()) {
                CompositeNusuthWebAppElement el =
                        (CompositeNusuthWebAppElement) e.nextElement();
                compositeFactories.addElement(
                        new CompositeElementPanelFactory(getType(), el));
            }
        }
        return compositeFactories.elements();
    }

    public CompositeElementPanelFactory getCompositePanelFactory(int index) {
        if (compositeFactories == null) {
            getCompositePanelFactories();
        }
        return (CompositeElementPanelFactory) compositeFactories.get(index);
    }

    /**
     * Adds a new child factory.
     */
    protected void addCompositePanelFactory() {
        CompositeNusuthWebAppElement webElement
                = BasicPanel.getCompositeElement(getType());
//    CompositeNusuthWebAppElement webElement = BasicPanel.getCompositeElement(getType(), childName);
        if (webElement != null) {
            CompositeElementPanelFactory factory =
                    new CompositeElementPanelFactory(getType(), webElement);
            compositeFactories.addElement(factory);
            pane.addTab(DefaultEditorPanel.getDisplay(
                    factory.getType(), factory.webElement), factory.createPanel());
            factory.setTabNameChangers(tabNameComponents, this);
            pane.setSelectedComponent(factory.createPanel());  // createPanel gives ready panel
            factory.updateControls(BasicPanel.deactivateNotRequired(webElement));
        }
    }

    /**
     * Removes a child panel factory by the specified index.
     *
     * @param   index       the index of factory to be deleted.
     */
    protected void removeCompositePanelFactory(int index) {
//        if (compositeFactories.size() > 1){
        if (compositeFactories.size() > 0) {
            compositeFactories.removeElementAt(index);
            pane.remove(index);
        }
    }

    public JComponent createPanel() {
        if (panel == null) {
            panel = new JPanel(new BorderLayout());
            pane = new JTabbedPane(JTabbedPane.TOP);
            Enumeration e = getCompositePanelFactories();
            while (e.hasMoreElements()) {
                CompositeElementPanelFactory factory =
                        (CompositeElementPanelFactory) e.nextElement();
                pane.addTab(DefaultEditorPanel.getDisplay(
                        factory.getType(), factory.webElement), factory.createPanel());
                factory.setTabNameChangers(tabNameComponents, this);
            }
            panel.add("North", new JPanel() {
                public Dimension getPreferredSize() {
                    return new Dimension(1, 20);
                }

                public Dimension getMinimumSize() {
                    return new Dimension(1, 20);
                }

                public Dimension getMaximumSize() {
                    return new Dimension(1, 20);
                }
            });
            panel.add("Center", pane);
            panel.add("East", getButtons());
        }
        return panel;
    }

    protected JPanel getButtons() {
        if (buttons == null) {
            buttons = new JPanel();
            GridBagLayout gridbag = new GridBagLayout();
            JPanel p = new JPanel(gridbag);
            buttons.add("North", p);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(1, 1, 1, 1);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTH;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = 1;

            JButton add = new JButton("Add");
            add.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DefaultEditorPanel.needSave = true;
                    addCompositePanelFactory();
                }
            });
            p.add(add, c);
            JButton remove = new JButton("Remove");
            remove.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    DefaultEditorPanel.needSave = true;
                    int index = pane.getSelectedIndex();
                    removeCompositePanelFactory(index);
                }
            });
            p.add(remove, c);
        }
        return buttons;
    }

    public void clear() {
    }

    public void updateControls(CompositeNusuthWebAppElement parent) {
        if (parent != null) {
            Enumeration e = null;
            try {
                e = parent.getCompositeChild(childName);
            } catch (DeploymentException de) {
                System.out.println(de);
            }
            int index = 0;
            while (e != null && e.hasMoreElements()) {
                CompositeNusuthWebAppElement webEl =
                        (CompositeNusuthWebAppElement) e.nextElement();
                try {
                    CompositeElementPanelFactory factory = getCompositePanelFactory(index);
                    factory.updateControls(webEl);
                    pane.setTitleAt(index,
                            DefaultEditorPanel.getDisplay(factory.getType(), webEl));
                    index++;
                } catch (ArrayIndexOutOfBoundsException aie) {
                    addCompositePanelFactory();
                    CompositeElementPanelFactory factory = getCompositePanelFactory(index);
                    factory.updateControls(webEl);
                    pane.setTitleAt(index++,
                            DefaultEditorPanel.getDisplay(factory.getType(), webEl));
                }
            }
            int tabSize = compositeFactories.size();
            while (compositeFactories.size() > index) {
                removeCompositePanelFactory(index);
            }
        }
    }

    public void updateElement(CompositeNusuthWebAppElement parent) {
//        if (childName.equals("host")) return;
        if (parent != null) {
            Enumeration e = null;
            try {
                e = parent.getCompositeChild(childName);
            } catch (DeploymentException de) {
                System.out.println(de);
            }
            int index = 0;
            Vector toDel = new Vector();
            while (e != null && e.hasMoreElements()) {
                CompositeNusuthWebAppElement webEl =
                        (CompositeNusuthWebAppElement) e.nextElement();
                try {
                    getCompositePanelFactory(index).updateElement(webEl);
                    index++;
                } catch (ArrayIndexOutOfBoundsException aie) {
                    toDel.addElement(webEl);
                }
            }
            for (int i = 0; i < toDel.size(); i++) {
                try {
                    parent.removeCompositeChild(childName,
                            (CompositeNusuthWebAppElement) toDel.elementAt(i));
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
            int tabSize = compositeFactories.size();
            while (tabSize > index) {
                try {
                    CompositeNusuthWebAppElement webElement =
                            parent.addCompositeChild(childName);
                    getCompositePanelFactory(index++).updateElement(webElement);
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
        }
    }

    public void tabNameChanged(String newName) {
        if (pane != null) {
            int index = pane.getSelectedIndex();
            pane.setTitleAt(index, newName);
        }
    }

/*
  public int getChildrensSize() {
    return compositeFactories.size();
  }
*/

    /**
     * If child is required in the parent element
     * this table have to containt one row at least.
     *
     * @return  <code>true</code> if all reqired elements are not empty;
     * <code>false</code> otherwise.
     */
    protected boolean requiredNotEmpty() {
        if (wrongCount()) {
            emptyChildFactory = null;
            emptyChild = childName;
            return false;
        }
        return super.requiredNotEmpty();
    }

    /**
     * Checks the child factories count,
     * if child is required in the parent element.
     *
     * @return  <code>true</code> if child is required & factories size = 0;
     * <code>false</code> otherwise.
     */
    protected boolean wrongCount() {
        try {
            if (parent.isChildRequired(childName) && compositeFactories.size() == 0) {
                return true;
            }
        } catch (DeploymentException de) {
            System.out.println(de);
        }
        return false;
    }

    /**
     * Overrides the super method.
     * Goes to necessary tab.
     */
    protected void gotoEmpty() {
        if (emptyChildFactory != null && pane != null) {
            try {
                pane.setSelectedComponent(emptyChildFactory.createPanel());
                emptyChildFactory.gotoEmpty();
            } catch (Exception e) {
                System.out.println("exception when goto " + e);
            }
        }
    }

    /**
     * Overrides the super method to do nothing.
     * There aren't any simple elements in this factory!!
     */
    protected void initRenderers() {
    }

    /**
     * Overrides the super method.
     */
    public boolean getActive() {
        try {
            if (parent.isChildRequired(childName) || compositeFactories.size() != 0) {
                return true;
            }
        } catch (DeploymentException de) {
            System.out.println(de);
        }
        return false;
    }
}