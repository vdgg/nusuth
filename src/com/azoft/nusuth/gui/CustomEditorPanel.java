/*
 * @(#)CustomEditorPanel.java 1.0 09/13/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.util.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Class CustomEditorPanel is the parent class for all special panel.
 * It defines the pattern for simple special panel with only one tab -
 * the main panel will not a tabbed pane - but a panel.
 * The special panel with some tabs extends this class too,
 * but they must override some methods.
 *
 * @version 1.0 09/13/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class CustomEditorPanel extends DefaultEditorPanel {

    /**
     * The hash: tab name -> array of childs.
     */
    protected static Vector USED_COMPOSITE_CHILDS;

    /**
     * The hash: tab name -> array of childs.
     */
    protected static String ELEMENT_TYPE;

    /**
     * The main component.
     */
    protected JComponent mainComponent;

    /**
     * Contains all table panel factories from this panel.
     * Used when someone table gains the focus to stop other tables editing.
     */
    protected Vector tables;


    static {
        USED_COMPOSITE_CHILDS = new Vector();
        ELEMENT_TYPE = BasicPanel.SWEB_APP;
    }


    /**
     * Constructs a new custom editor panel with the specified type.
     */
    public CustomEditorPanel(String type) {
        super(type);
    }


    /**
     * Creates the composite element for this panel factory.
     *
     * @return  the created composite element
     */
    protected CompositeNusuthWebAppElement createCompositeElement() {
        return createCompositeElement(ELEMENT_TYPE);
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
            Enumeration childs = USED_COMPOSITE_CHILDS.elements();
            while (childs.hasMoreElements()) {
                String childName = (String) childs.nextElement();
                CompositeElementPanelFactory factory = null;
                try {
                    if (webElement.isChildUnbounded(childName)) {
                        if (childName.equals("listener")) {
                            factory = new TextAreaPanelFactory(
                                    ELEMENT_TYPE + "." + childName, childName, webElement);
                        } else {
                            factory = new TablePanelFactory(
                                    ELEMENT_TYPE + "." + childName, childName, webElement);
                        }
                    } else {
                        Enumeration en = webElement.getCompositeChild(childName);
                        CompositeNusuthWebAppElement el = null;
                        if (en != null && en.hasMoreElements()) {
                            el = (CompositeNusuthWebAppElement) en.nextElement();
                        }
                        factory = new CompositeElementPanelFactory(
                                ELEMENT_TYPE + "." + el.getTag(), el);
                        factory.setRequired(webElement.isChildRequired(childName));
                    }
                } catch (DeploymentException de) {
                    System.out.println(de);
                } catch (NullPointerException npe) {
                    System.out.println(npe);
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
     * Used the USED_COMPOSITE_CHILDS vector.
     *
     * @param   childFacName  the specified child factory name.
     * @return  <code>true</code> if the basics tab contains the specified child
     * factory; <code>false</code> otherwise.
     */
    protected boolean basicsContains(String childFacName) {
        Enumeration en = USED_COMPOSITE_CHILDS.elements();
        while (en.hasMoreElements()) {
            if (childFacName.equals((String) en.nextElement())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new panel for this element panel.
     * It is tabbed pane with the tabs summary, basic
     * & all tabs from the TABS array.
     */
    public JComponent createPanel() {
        if (mainComponent == null) {
            mainComponent = new JScrollPane(getSimplePanel());
        }
        return mainComponent;
    }

    /**
     * Gets the componentId component content.
     * In this class component id not important.
     *
     * @return  the componentId component content.
     */
    protected String getComponentId() {
        return "";
    }

    /**
     * Gets the popup menu for this editor.
     *
     * @return  the popup menu for this editor.
     */
    public JPopupMenu getPopupMenu() {
        return null;
    }

    /**
     * Adds the element renderer to the specified panel.
     *
     * @param   fac       the specified parent factory.
     * @param   childName the specified child name.
     * @param   required  the parent factory is required or not in its parents.
     * @param   panel     the specified panel.
     * @param   c         the specified grid bag constraints.
     */
    protected void addElement(CompositeElementPanelFactory fac, String childName,
                              boolean required, JPanel panel, GridBagConstraints c) {
        addElement(fac, childName, required, panel, c, "left");
    }

    /**
     * Adds the element renderer to the specified panel.
     *
     * @param   fac       the specified parent factory.
     * @param   childName the specified child name.
     * @param   required  the parent factory is required or not in its parents.
     * @param   panel     the specified panel.
     * @param   c         the specified grid bag constraints.
     * @param   labelLoc  the specified label location.
     */
    protected void addElement(CompositeElementPanelFactory fac, String childName,
                              boolean required, JPanel panel, GridBagConstraints c,
                              String labelLoc) {
        if (fac == null) {
            return;
        }
        ElementRenderer renderer = (ElementRenderer) fac.renderers.get(childName);
        if (renderer != null) {
            try {
                required = required && fac.webElement.isChildRequired(childName);
            } catch (DeploymentException e) {
            }
            boolean unbounded = false;
            try {
                unbounded = fac.webElement.isChildUnbounded(childName);
            } catch (DeploymentException e) {
            }
            boolean choiced = false;
            try {
                choiced = fac.webElement.getChildChoices(childName).hasMoreElements();
            } catch (DeploymentException de) {
            }
            int ind = fac.getType().lastIndexOf(".");
            String parentPath = (ind == -1) ? fac.getType()
                    : fac.getType().substring(ind + 1);
            if (choiced) {
                createRow(renderer, panel, c, parentPath, childName,
                        required, unbounded, getButtonGroup(),
                        getRadioButtonActionListener(), getRadioHashtComps(),
                        getRadioHashNames(), labelLoc);
                boolean has = false;
                try {
                    Enumeration se = webElement.getSimpleChild(childName);
                    has = (se != null && se.hasMoreElements());
                } catch (DeploymentException e) {
                }
                JRadioButton radio = getRadioButton(childName);
                if (radio != null) {
                    radio.setSelected(has);
                    enableRadioComponents(radio, has);
                }
            } else {
                createRow(renderer, panel, c, parentPath, childName,
                        required, unbounded, labelLoc);
            }
//      createRow(renderer, panel, c, parentPath,
//              childName, required, unbounded, labelLoc);
        } else {
            if (fac instanceof TablePanelFactory) {
                JScrollPane pane =
                        ((TablePanelFactory) fac).getMainPane();
                panel.add(pane, c);
            } else {
                panel.add(fac.createPanel(), c);
            }
        }
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
        }
    }

    /**
     * Gets the element type_names array for this editor panel.
     * Overrides the super method.
     *
     * @return  the element type names array.
     */
    public String[] getElementNames() {
        return null;
    }

    /**
     * Adds the specified table to the tables vector.
     *
     * @param   table   the specified table.
     */
    protected void addTableToFocused(final AdvancedTable table) {
        table.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                stopAllEditing(table);
                super.focusGained(e);
            }
        });
        if (tables == null) {
            tables = new Vector();
        }
        tables.addElement(table);
    }

    /**
     * Stops editing in all tables &
     * put the focus to the specified source table.
     *
     * @param   source      the specified source table
     */
    private void stopAllEditing(AdvancedTable source) {
        if (tables != null) {
            Enumeration ts = tables.elements();
            while (ts.hasMoreElements()) {
                AdvancedTable table = (AdvancedTable) ts.nextElement();
                if (table.isEditing()) {
                    table.editingStopped(new ChangeEvent(this));
                }
            }
        }
        source.requestFocus();
    }
}