/*
 * @(#)TablePanelFactory.java 1.0 04/06/2001
 */

package com.azoft.nusuth.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.BevelBorder;
import java.util.*;
import javax.accessibility.AccessibleContext;
import java.io.IOException;

import com.azoft.nusuth.deployment.*;

/**
 * Class TablePanelFactory used for unbounded elements.
 * It presenets its child elements as the table.
 *
 * @version 1.0 04/06/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TablePanelFactory extends UnboundedCompositeElementPanelFactory {

    /**
     * The minimum table scroll pane size.
     */
    static final Dimension minimumSize = new Dimension(100, 50);

    /**
     * The hash table with the panels pref sizes.
     * panel path -> preferred size
     */
    static Hashtable prefSizes = new Hashtable();

    /**
     * dimension for composite cell renderer's/editor's button ('...')
     */
    static Dimension buttonSize = new Dimension(20, 30);

    /**
     * selection properties
     */
    static Properties selectionProp;

    /**
     * defines the table dimension
     */
    private static Dimension tableDimension = new Dimension(600, 400);


    /**
     * the main panel
     */
    private JPanel panel;

    /**
     * scroll pane with main table
     */
    private JScrollPane mainPane = null;

    /**
     * the table
     */
    private AdvancedTable table = null;

    /**
     * vector of column names
     */
    private Vector columnNames = new Vector();

    /**
     * vector of column order - is parsed from display name properties
     */
    private Vector columnOrder = new Vector();

    /**
     * vector of arrays (by rows)
     */
    private Vector editors = new Vector();

    /**
     * vector of renderers (by column)
     */
    private Vector cellRenderers = new Vector();

    /**
     * stores radio column indices
     */
    private Vector group = new Vector();

    /**
     * factory for one row
     */
    private CompositeElementPanelFactory oneRowFactory;

    /**
     * array with requared or not values
     */
    private boolean[] requaredOrNot = null;

    /**
     * changing type
     */
    private String changingType = null;

    /**
     * changing column index
     */
    private int changingColumnIndex;

    /**
     * first required empty row
     */
    private int firstEmptyRow = -1;

    /**
     * first required empty column
     */
    private int firstEmptyColumn = -1;

    /**
     * The table insets.
     */
    private Insets tableInsets = new Insets(8, 8, 8, 8);


    // loads the selection properties
    static {
        selectionProp = new Properties();
        try {
            selectionProp.load(ClassLoader.getSystemResourceAsStream(
                    "com/azoft/nusuth/gui/selection.properties"));
        } catch (IOException e) {
            System.out.println("can not load select properties");
        }
    }


    /**
     * Constructs a new table panel factory with the specified
     * type, child name, parent & path.
     * Calls the super constructor (UnboundedCompositeElementPanelFactory)
     * and inits the popup menu.
     *
     * @param   type        the type of panel factory.
     * @param   childName   the childName.
     * @param   parent      the parent.
     * @param   path        the path.
     */
    public TablePanelFactory(String type, String childName,
                             CompositeNusuthWebAppElement parent) {
        super(type, childName, parent);
    }

    /**
     * Creates the panel for this panel factory.
     * Adds all necessary components - the main table,
     * the buttons add/delete rows, the one row table
     *
     * @return  the panel.
     */
    public JComponent createPanel() {
        if (panel == null) {
            panel = new ElementPanel(new GridBagLayout()) {
                public void checkEditing() {
                    TablePanelFactory.this.checkEditStopped();
                }
            };
            GridBagConstraints c = new GridBagConstraints();
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.insets = getTableInsets();
            mainPane = new JScrollPane(getTable());
            mainPane.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                        getTable().getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
            mainPane.setMinimumSize(minimumSize);
            mainPane.setPreferredSize(minimumSize);
            panel.add(mainPane, c);
        }
        return panel;
    }

    /**
     * Gets the table insets.
     *
     * @return  the table insets.
     */
    public Insets getTableInsets() {
        return tableInsets;
    }

    /**
     * Gets the scroll pane with main table.
     *
     * @return  the scroll pane with main table.
     */
    public JScrollPane getMainPane() {
        if (mainPane == null) {
            createPanel();
        }
        return mainPane;
    }

    /**
     * Returns <code>true</code> if column with the specified colIndex
     * is required. It means the cells in that column can't be empty.
     *
     * @param colIndex  the specified column index.
     * @return  <code>true</code> if column with the specified colIndex
     * is required;
     * <code>false</code> otherwise.
     */
    private boolean columnIsRequired(int colIndex) {
        if (colIndex < 0 || colIndex > requaredOrNot.length - 1) {
            return false;
        }
        return requaredOrNot[colIndex];
    }

    /**
     * Adds the specified boolean value to the end of required or not array.
     *
     * @param b   the specified boolean value
     */
    private void addReqToArray(boolean b) {
        int oldLen = requaredOrNot.length;
        boolean[] tmp = new boolean[oldLen + 1];
        System.arraycopy(requaredOrNot, 0, tmp, 0, oldLen);
        requaredOrNot = tmp;
        requaredOrNot[oldLen] = b;
    }

    /**
     * Inits the column order vector.
     * Gets the string from properties & parses it.
     */
    private void initColumnOrder() {
        String sorder = DefaultEditorPanel.getOrder(this.type);
        StringTokenizer st = new StringTokenizer(sorder, ",");
        while (st.hasMoreTokens()) {
            columnOrder.addElement(st.nextToken());
        }
    }

    /**
     * Adds a new column for a simple element with the specified name.
     * Adds necessary renderer.
     *
     * @param   name      the specified element name
     * @param   factory   the specified parent factory
     * @see #addCompositeElementColumn(String, CompositeElementPanelFactory)
     */
    private void addSimpleElementColumn(String name,
                                        CompositeElementPanelFactory factory) {
        columnNames.addElement(name);
        boolean req = false;
        try {
            req = factory.webElement.isChildRequired(name);
        } catch (DeploymentException e) {
            System.out.println("isRequired throws DeploymentException " + e);
        } finally {
            addReqToArray(req);
        }
        if ((ElementRenderer) factory.renderers.get(name)
                instanceof TableRenderer) {
            cellRenderers.addElement(new CompositeCellRenderer(getRendererButton()));
        } else if ((ElementRenderer) factory.renderers.get(name)
                instanceof CheckBooleanElementRenderer) {
            cellRenderers.addElement(new CompositeCellRenderer(new JCheckBox()));
        } else if ((ElementRenderer) factory.renderers.get(name)
                instanceof PasswordElementRenderer) {
            if (factory.getRadioButton(name) != null) {
                cellRenderers.addElement(new CompositeCellRenderer(
                        getRendererButton(), new JRadioButton(), false));
                group.addElement(new Integer(columnNames.indexOf(name)));
            } else {
                cellRenderers.addElement(new CompositeCellRenderer(
                        getRendererButton(), false));
            }
        } else {
            if (factory.getRadioButton(name) != null) {
                cellRenderers.addElement(new CompositeCellRenderer(new JRadioButton()));
                group.addElement(new Integer(columnNames.indexOf(name)));
            } else {
                cellRenderers.addElement(new CompositeCellRenderer());
            }
        }
    }

    /**
     * Adds a new column for a composite element with the specified name.
     * Adds necessary renderer.
     *
     * @param   name      the specified element name
     * @param   factory   the specified parent factory
     * @see #addSimpleElementColumn(String, CompositeElementPanelFactory)
     */
    private void addCompositeElementColumn(String childName,
                                           CompositeElementPanelFactory factory) {
        CompositeElementPanelFactory childFactory =
                factory.getCompositePanelFactory(childName);
        if (childFactory == null) {
            System.out.println("child factory for childName = " + childName + " is null!!!!");
            return;
        }
        columnNames.addElement(childName);
        boolean req = childFactory.isRequired();
        addReqToArray(req);
        if (childFactory.isRequired()) {
            cellRenderers.addElement(new CompositeCellRenderer(getRendererButton()));
        } else {
            cellRenderers.addElement(new CompositeCellRenderer(new JCheckBox(),
                    getRendererButton()));
        }
    }

    /**
     * Inits the column names vector, the renderers vector,
     * the one editors vector.
     * Columns follow in the next order -
     * first from the columnOrder vector, then other simple, then other composite.
     */
    private void initColumnsEditorsRenderers(
            CompositeElementPanelFactory factory) {

        // for the first column
        columnNames.addElement(AdvancedTable.firstColumnName);
        cellRenderers.addElement(null);
        requaredOrNot = new boolean[1];
        requaredOrNot[0] = false;

        // tmp clone renderers - to remove added renderers
        Hashtable clrenderers = (Hashtable) factory.renderers.clone();
        // tmp composite names vector - to remove added composite names
        Vector clcomp = new Vector();

        Enumeration compnames = factory.webElement.getCompositeChildrenNames();
        while (compnames.hasMoreElements()) {
            clcomp.addElement(compnames.nextElement());
        }

        // columns from the columnOrder vector
        for (int i = 0; i < columnOrder.size(); i++) {
            String childName = (String) columnOrder.elementAt(i);
            ElementRenderer renderer =
                    (ElementRenderer) factory.renderers.get(childName);
            if (renderer != null) {
                // simple element
                clrenderers.remove(childName);
                addSimpleElementColumn(childName, factory);
            } else {
                // composite element (if clcomp contains it)
                if (clcomp.remove(childName)) {
                    addCompositeElementColumn(childName, factory);
                }
            }
        }

        // for other elements
        Enumeration otherSimpleNames = clrenderers.keys();
        while (otherSimpleNames.hasMoreElements()) {
            String childName = (String) otherSimpleNames.nextElement();
            addSimpleElementColumn(childName, factory);
        }
        Enumeration otherCompositeNames = clcomp.elements();
        while (otherCompositeNames.hasMoreElements()) {
            String childName = (String) otherCompositeNames.nextElement();
            addCompositeElementColumn(childName, factory);
        }

        // add editor row
        addEditor(factory);
    }

    /**
     * Gets the main table.
     * Fills the column names vector, cell renderers vector,
     * adds editors by factory.
     * Creates the table.
     */
    AdvancedTable getTable() {
        if (table == null) {
            CompositeElementPanelFactory factory = null;
            Enumeration e = getCompositePanelFactories();
            if (e != null && e.hasMoreElements()) {
                factory = (CompositeElementPanelFactory) e.nextElement();
                factory.createPanel();
                initColumnOrder();
                initColumnsEditorsRenderers(factory);
            }
            table = new AdvancedTable(columnNames) {
                public TableCellEditor getCellEditor(int row, int column) {
                    CompositeCellEditor[] rowEditors =
                            (CompositeCellEditor[]) editors.elementAt(row);
                    return rowEditors[column];
                }

                public TableCellRenderer getCellRenderer(int row, int column) {
                    if (column == 0) {
                        return super.getCellRenderer(row, column);
                    }
                    return (TableCellRenderer) cellRenderers.elementAt(column);
                }

                public void setValueAt(Object aValue, int row, int column) {
                    if (!aValue.equals(getValueAt(row, column))) {
                        DefaultEditorPanel.needSave = true;
                    }
                    if (changingType != null && changingColumnIndex == column) {
                        String oldValue = getChangingValueAt(row);
                        super.setValueAt(aValue, row, column);
                        String newValue = getChangingValueAt(row);
                        if (oldValue == null && newValue != null) {
                            BasicPanel.fireValueAdded(changingType, newValue);
                        } else if (oldValue != null && newValue == null) {
                            BasicPanel.fireValueRemoved(changingType, oldValue);
                        } else if (oldValue != null && newValue != null) {
                            BasicPanel.fireValueChanged(changingType, oldValue, newValue);
                        }
                    } else {
                        super.setValueAt(aValue, row, column);
                    }
                }

                protected void addTableRow() {
                    DefaultEditorPanel.needSave = true;
                    addCompositePanelFactory();
                    if (changingType != null) {
                        String s = getChangingValueAt(getTable().getRowCount() - 1);
                        if (s != null) {
                            BasicPanel.fireValueAdded(changingType, s);
                        }
                    }
                }

                protected void deleteSelectedRows() {
                    DefaultEditorPanel.needSave = true;
                    int cnt = getTable().getSelectedRowCount();
                    while (cnt-- > 0) {
                        int sel = getTable().getSelectedRow();
                        if (sel > -1) {
                            if (changingType != null) {
                                String s = getChangingValueAt(sel);
                                if (s != null) {
                                    BasicPanel.fireValueRemoved(changingType, s);
                                }
                            }
                            removeCompositePanelFactory(sel);
                        }
                    }
                }
            };
            if (factory != null) {
                ((DefaultTableModel) table.getModel()).
                        addRow(getTableRowByFactory(factory));
            }
            checkSelectionProperties();
        }
        return table;
    }

    /**
     * checks if selection property contains key/value for that factory
     */
    private void checkSelectionProperties() {
        Enumeration keys = selectionProp.keys();
        while (keys.hasMoreElements()) {
            String type = (String) keys.nextElement();
            String val = (String) selectionProp.get(type);
            if (val.startsWith(this.childName)) {
                String columnName = val.substring(this.childName.length() + 1);
                if (columnNames.contains(columnName)) {
                    changingType = type;
                    changingColumnIndex = columnNames.indexOf(columnName);
                }
            }
        }
    }

    /**
     * Gets the changing value by the specified row index.
     *
     * @param index   the specified row index
     * @return  the changing value
     */
    private String getChangingValueAt(int index) {
        CompositeObject co =
                (CompositeObject) getTable().getValueAt(index, changingColumnIndex);
        return (co == null) ? null : (((String) co.getValue()).equals("")) ? null
                : (String) co.getValue();
    }

    /**
     * Gets a new factory instance.
     *
     * @return a new factory instance.
     */
    private CompositeElementPanelFactory getNewFactoryInstance() {
        CompositeNusuthWebAppElement webElement =
                BasicPanel.getCompositeElement(getType());
//            BasicPanel.getCompositeElement(getType(), childName);
        if (webElement != null) {
            CompositeElementPanelFactory factory = new CompositeElementPanelFactory(
                    getType(), webElement);
            factory.createPanel();
            factory.updateControls(BasicPanel.deactivateNotRequired(webElement));
            return factory;
        }
        return null;
    }

    /**
     * Gets the vector of table row for the specified factory.
     *
     * @param factory   the specified factory
     * @return  the vector of table row
     * @see #updateFactoryByTableRow(CompositeElementPanelFactory,Vector)
     */
    private Vector getTableRowByFactory(CompositeElementPanelFactory factory) {
        Vector row = new Vector();
        row.addElement("");
        for (int i = 1; i < columnNames.size(); i++) {
            // index 0 - not from factory
            String childName = (String) columnNames.elementAt(i);
            CompositeObject co;
            ElementRenderer renderer =
                    (ElementRenderer) factory.renderers.get(childName);
            if (renderer != null) {
                // simple element
                boolean selected = (factory.getRadioButton(childName) != null)
                        ? factory.getRadioButton(childName).isSelected() : true;
                co = new CompositeObject(selected, renderer.getValue());
            } else {
                // composite element
                CompositeElementPanelFactory childFactory =
                        factory.getCompositePanelFactory(childName);
                boolean selected = (childFactory.isRequired())
                        ? true : childFactory.getActive();
                co = new CompositeObject(selected, childFactory.getDisplay());
            }
            row.addElement(co);
        }
        return row;
    }

    /**
     * Updates the specified factory by the specified table row.
     *
     * @param factory   the specified factory
     * @param row       the specified table row
     * @see #getTableRowByFactory(CompositeElementPanelFactory)
     */
    private void updateFactoryByTableRow(CompositeElementPanelFactory factory,
                                         Vector row) {
        for (int i = 1; i < columnNames.size(); i++) {
            // index 0 - not from element
            String childName = (String) columnNames.elementAt(i);
            ElementRenderer renderer =
                    (ElementRenderer) factory.renderers.get(childName);
            if (renderer == null && renderer instanceof TableRenderer) continue;

            CompositeObject co = (CompositeObject) row.elementAt(i);
            boolean selected = co.getSelected();
            Object value = co.getValue();
            if (renderer != null) {
                // simple element
                if (factory.getRadioButton(childName) != null) {
                    factory.getRadioButton(childName).setSelected(selected);
                }
                renderer.setValue((String) value);
            } else {
                // composite element
                CompositeElementPanelFactory childFactory =
                        factory.getCompositePanelFactory(childName);
                if (!childFactory.isRequired()) {
                    childFactory.setActive(co.getSelected());
                }
            }
        }
    }

    /**
     * Gets the new ediotrs row by the specified factory.
     *
     * @param factory   the specified factory
     * @return  the new ediotrs row
     */
    private CompositeCellEditor[] getNewEditorsRow(
            CompositeElementPanelFactory factory) {
        CompositeCellEditor[] rowEditors =
                new CompositeCellEditor[columnNames.size()];
        rowEditors[0] = null; // for first column
        for (int i = 1; i < columnNames.size(); i++) {
            String colName = (String) columnNames.elementAt(i);
            ElementRenderer renderer =
                    (ElementRenderer) factory.renderers.get(colName);
            if (renderer != null) {
                // simple element
                JComponent comp = renderer.getComponent();
                JRadioButton radio = factory.getRadioButton(colName);
                if (comp instanceof JTextField) {
                    JTextField textField = (JTextField) comp;
                    rowEditors[i] = (radio == null) ? new CompositeCellEditor(textField)
                            : new CompositeCellEditor(textField, getEditorRadio());
                } else if (comp instanceof JComboBox) {
                    JComboBox comboBox = (JComboBox) comp;
                    rowEditors[i] = (radio == null) ? new CompositeCellEditor(comboBox)
                            : new CompositeCellEditor(comboBox, getEditorRadio());
                } else if (comp instanceof JCheckBox) {
                    JCheckBox check = (JCheckBox) comp;
                    check.setText("");
                    rowEditors[i] = new CompositeCellEditor(check);
                } else {
                    rowEditors[i] = null;
                }
                if (renderer instanceof TableRenderer) {
                    rowEditors[i] = new CompositeCellEditor((TableRenderer) renderer);
                } else if (renderer instanceof PasswordElementRenderer) {
                    rowEditors[i] = (radio == null)
                            ? new CompositeCellEditor((PasswordElementRenderer) renderer)
                            : new CompositeCellEditor((PasswordElementRenderer) renderer,
                                    getEditorRadio());
                }
            } else {
                // composite element
                CompositeElementPanelFactory childFactory =
                        factory.getCompositePanelFactory(colName);
                boolean vrequared = childFactory.isRequired();
                rowEditors[i] = (vrequared) ? new CompositeCellEditor(childFactory)
                        : new CompositeCellEditor(new JCheckBox(), childFactory);
            }
        }
        return rowEditors;
    }

    /**
     * Updates editors row number index by the specified factory.
     *
     * @param factory   the specified factory
     * @param index     the index of editors row.
     */
    private void updateEditors(CompositeElementPanelFactory factory, int index) {
        if (editors.size() <= index) {
            System.out.println("index >> size of vector !!!!!!!!!!!!!!!");
            return;
        }
        editors.setElementAt(getNewEditorsRow(factory), index);
    }

    /**
     * Gets the array of row & column in which the specified radio is placed.
     *
     * @param radio   the specified radio
     * @return  the array of row & column
     */
    private int[] getRowCol(JRadioButton radio) {
        for (int i = 0; i < editors.size(); i++) {
            CompositeCellEditor[] rowEditors =
                    (CompositeCellEditor[]) editors.elementAt(i);
            // index 0 - not from factory
            for (int j = 1; j < rowEditors.length; j++) {
                if (rowEditors[j] != null) {
                    JRadioButton nextRadio = rowEditors[j].getRadio();
                    if (nextRadio != null && nextRadio.equals(radio)) {
                        int[] res = new int[2];
                        res[0] = i;
                        res[1] = j;
                        return res;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Shows the dialog with the specified message object & title.
     *
     * @param   message     the message for JOptionPane dialog
     * @param   title       the dialog title.
     * @see     JOptionPane#showConfirmDialog(Component, Object[], String, int, int)
     */
    public static void showDialog(Object[] message, String title) {
        JOptionPane.showConfirmDialog(ManageTool.getMainFrame(), message, title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Gets the button for renderers with the necessary size.
     *
     * @return  the button for renderers
     */
    public static JButton getRendererButton() {
        JButton button = new JButton("...");
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);
        return button;
    }

    /**
     * Gets the editor radio button with the necessary action listener.
     *
     * @return  the editor radio button with the necessary action listener.
     */
    private JRadioButton getEditorRadio() {
        JRadioButton radio = new JRadioButton();
        radio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JRadioButton acRadio = (JRadioButton) e.getSource();
                int[] rowcol = getRowCol(acRadio);
                if (rowcol == null) return;
                int row = rowcol[0];
                int column = rowcol[1];
                JTable tableTmp = getTable();
                if (!acRadio.isSelected()) {
                    Object o = tableTmp.getValueAt(row, column);
                    if (o instanceof CompositeObject) {
                        CompositeObject co = (CompositeObject) o;
                        co.setSelected(true);
                        tableTmp.setValueAt(co, row, column);
                    }
                } else {
                    for (Enumeration en = group.elements(); en.hasMoreElements();) {
                        int nextIndex = ((Integer) en.nextElement()).intValue();
                        if (nextIndex != column) {
                            Object o = tableTmp.getValueAt(row, nextIndex);
                            if (o instanceof CompositeObject) {
                                CompositeObject co = (CompositeObject) o;
                                co.setSelected(false);
                                tableTmp.setValueAt(co, row, nextIndex);
                            }
                        }
                    }
                }
            }
        });
        return radio;
    }

    /**
     * Adds an editor to aditors vector by the specified factory.
     *
     * @param factory   the specified factory.
     */
    private void addEditor(CompositeElementPanelFactory factory) {
        CompositeCellEditor[] rowEditors =
                new CompositeCellEditor[columnNames.size()];
        editors.addElement(rowEditors);
        updateEditors(factory, editors.size() - 1);
    }

    /**
     * Adds a new child factory.
     * Override this method in unbounded factory.
     */
    protected void addCompositePanelFactory() {
        checkEditStopped();
        oneRowFactory = getNewFactoryInstance();
        if (oneRowFactory != null) {
            compositeFactories.addElement(oneRowFactory);
            addEditor(oneRowFactory);
            ((DefaultTableModel) getTable().getModel()).
                    addRow(getTableRowByFactory(oneRowFactory));
            getTable().setEditingRow(compositeFactories.size() - 1);
        }
    }

    /**
     * Removes a child panel factory by the specified index.
     * Overrides the super method.
     *
     * @param   index       the index of factory to be deleted.
     */
    protected void removeCompositePanelFactory(int index) {
        if (compositeFactories.size() > index) {
            checkEditStopped();
            compositeFactories.removeElementAt(index);
            editors.removeElementAt(index);
            ((DefaultTableModel) getTable().getModel()).removeRow(index);
        }
    }

    /**
     * Updates all panel components by the specified web element.
     * Overrides the composite panel factory method.
     *
     * @param   webElement  the specified web element.
     * @see #updateElement(CompositeNusuthWebAppElement)
     */
    public void updateControls(CompositeNusuthWebAppElement parent) {
        if (parent != null) {
            Enumeration e = null;
            try {
                e = parent.getCompositeChild(childName);
            } catch (DeploymentException de) {
                System.out.println(de);
            }
            int index = 0;
            Vector data = new Vector();
            while (e != null && e.hasMoreElements()) {
                CompositeNusuthWebAppElement webEl =
                        (CompositeNusuthWebAppElement) e.nextElement();
                try {
                    CompositeElementPanelFactory factory =
                            getCompositePanelFactory(index);
                    factory.updateControls(webEl);
                    updateEditors(factory, index);
                    data.addElement(getTableRowByFactory(factory));
                    index++;
                } catch (ArrayIndexOutOfBoundsException aie) {
                    addCompositePanelFactory();
                    CompositeElementPanelFactory factory =
                            getCompositePanelFactory(index);
                    factory.updateControls(webEl);
                    updateEditors(factory, index);
                    data.addElement(getTableRowByFactory(factory));
                    index++;
                }
            }
            int tabSize = compositeFactories.size();
            while (compositeFactories.size() > index) {
                removeCompositePanelFactory(index);
            }
            getTable().setDataVector(data);
            if (changingType != null) {
                BasicPanel.fireAllValueRemoved(changingType);
                int cnt = getTable().getRowCount();
                for (int i = 0; i < cnt; i++) {
                    String s = getChangingValueAt(i);
                    if (s != null) {
                        BasicPanel.fireValueAdded(changingType, s);
                    }
                }
            }
        }
    }

    /**
     * Updates the specified web element by all panel components.
     * Overrides the composite panel factory method.
     *
     * @param   webElement  the specified web element.
     * @see #updateControls(CompositeNusuthWebAppElement)
     */
    public void updateElement(CompositeNusuthWebAppElement parent) {
        checkEditStopped();
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
                    CompositeElementPanelFactory factory = getCompositePanelFactory(index);
                    Vector row = (Vector) ((DefaultTableModel) getTable().
                            getModel()).getDataVector().elementAt(index);
                    updateFactoryByTableRow(factory, row);
                    factory.updateElement(webEl);
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
                    CompositeElementPanelFactory factory =
                            getCompositePanelFactory(index);
                    Vector row = (Vector) ((DefaultTableModel) getTable().
                            getModel()).getDataVector().elementAt(index);
                    updateFactoryByTableRow(factory, row);
                    factory.updateElement(webElement);
                    index++;
                } catch (DeploymentException de) {
                    System.out.println(de);
                }
            }
        }
    }

    /**
     * Check the tables on editing. Stops it if necessary.
     */
    private void checkEditStopped() {
        if (getTable().isEditing()) {
            getTable().editingStopped(new ChangeEvent(this));
        }
    }

    /**
     * Gets the column name index.
     *
     * @param name    the specified column name
     * @return  the index of column or -1, if there isn't a colunm with the
     * specified name.
     */
    private int getIndexOf(String name) {
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnNames.elementAt(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the display for this panel factory.
     * Overrides the composite panel factory method.
     *
     * @return  the display for this panel factory.
     */
    public String getDisplay() {
        checkEditStopped();
        String displayComp = DefaultEditorPanel.getDisplayCompName(childName);
        int col = getIndexOf(displayComp);
        if (col == -1)
            return childName;
        String res = "";
        for (int i = 0; i < compositeFactories.size(); i++) {
            if (!res.equals("")) res += ";";
            res += ((CompositeObject) getTable().getValueAt(i, col)).getValue();
        }
        return res;
    }

    /**
     * Gets the all reqired elements are not empty or not.
     *
     * @return  <code>true</code> if all reqired elements are not empty;
     * <code>false</code> otherwise.
     */
    protected boolean requiredNotEmpty() {
        checkEditStopped();
        if (wrongCount()) {
            firstEmptyRow = -1;
            firstEmptyColumn = -1;
            emptyChild = childName;
            emptyChildFactory = null;
            return false;
        }
        int rowCnt = getTable().getRowCount();
        if (rowCnt > 0) {
            for (int i = 0; i < requaredOrNot.length; i++) {
                if (requaredOrNot[i]) {
                    for (int j = 0; j < rowCnt; j++) {
                        CompositeElementPanelFactory childFac = null;
                        TableCellEditor editor =
                                (TableCellEditor) getTable().getCellEditor(j, i);
                        if (editor instanceof CompositeCellEditor) {
                            childFac = ((CompositeCellEditor) editor).getChildFactory();
                        }
                        if (childFac != null) {
                            if (!childFac.requiredNotEmpty()) {
                                firstEmptyRow = j;
                                firstEmptyColumn = i;
                                emptyChildFactory = childFac;
                                emptyChild = (String) columnNames.elementAt(i);
                                return false;
                            }
                        } else {
                            Object ov = getTable().getValueAt(j, i);
                            if (ov instanceof CompositeObject
                                    && ((CompositeObject) ov).getSelected()
                                    && (((CompositeObject) ov).getValue() == null
                                    || ((CompositeObject) ov).getValue().equals(""))) {
                                firstEmptyRow = j;
                                firstEmptyColumn = i;
                                emptyChild = (String) columnNames.elementAt(i);
                                emptyChildFactory = null;
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Overrides the super method.
     * Sets the focus on the first empty element.
     */
    protected void gotoEmpty() {
        emptyRequestFocus();
        if (emptyChildFactory != null) {
            emptyChildFactory.gotoEmpty();
        }
    }

    /**
     * Overrides the super method -
     * empty element cell requests the focus.
     */
    protected void emptyRequestFocus() {
        if (firstEmptyRow != -1 && firstEmptyColumn != -1) {
            getTable().getSelectionModel().
                    setSelectionInterval(firstEmptyRow, firstEmptyRow);
            getTable().getColumnModel().getSelectionModel().
                    setSelectionInterval(firstEmptyColumn, firstEmptyColumn);
            getTable().requestFocus();
        }
    }

    /**
     * Gets the empty child name.
     * Overrides the super method.
     *
     * @return  the empty child name.
     */
    protected String getEmptyChild() {
        if (emptyChildFactory != null
                && !emptyChildFactory.getEmptyChild().equals("")) {
            return emptyChildFactory.getEmptyChild() + " in " + emptyChild;
        }
        return emptyChild;
    }

    /**
     * Gets the preferred size by the specified child path (name).
     * Values is taken from the static hashtable.
     *
     * @param childPath   the specified child path.
     * @return  the preferred size for this name || null.
     * @see #setPrefSize(String,Dimension)
     */
    static Dimension getPrefSize(String childPath) {
        return (Dimension) prefSizes.get(childPath);
    }

    /**
     * Sets the specified preferred size by the specified path (name).
     *
     * @param childPath   the specified child path.
     * @param d           the specified dimension.
     * @see #getPrefSize(String)
     */
    static void setPrefSize(String childPath, Dimension d) {
        prefSizes.put(childPath, d);
    }

    /**
     * Parses the specified properties and
     * adds the preferred sizes to hashtable.
     *
     * @param props   the specified properties
     * @see #savePrefSizes(Properties)
     */
    static void setPrefSizes(Properties props) {
        String prefSizesNames = props.getProperty("prefSizes.for", "");
        StringTokenizer st = new StringTokenizer(prefSizesNames, ";");
        while (st.hasMoreElements()) {
            String nextPath = (String) st.nextElement();
            String n_width = props.getProperty(nextPath + ".prefSize.width", "");
            String n_height = props.getProperty(nextPath + ".prefSize.height", "");
            try {
                int in_width = Integer.parseInt(n_width);
                int in_height = Integer.parseInt(n_height);
                setPrefSize(nextPath, new Dimension(in_width, in_height));
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Gets the preferred sizes from hashtable & saves them in the
     * specified properties.
     *
     * @param props   the specified properties
     * @see #setPrefSizes(Properties)
     */
    static void savePrefSizes(Properties props) {
        String names = "";
        Enumeration keys = prefSizes.keys();
        while (keys.hasMoreElements()) {
            String nextPath = (String) keys.nextElement();
            Dimension d = (Dimension) prefSizes.get(nextPath);
            names += (names.equals("")) ? nextPath : ";" + nextPath;
            props.setProperty(nextPath + ".prefSize.width", "" + d.width);
            props.setProperty(nextPath + ".prefSize.height", "" + d.height);
        }
        props.setProperty("prefSizes.for", names);
    }
}  // end of TablePanelFactory