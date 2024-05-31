/*
 * @(#)TableRenderer.java 1.0 4/10/2001
 */


package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;

/**
 * Class TableRenderer is the element renderer for unbounded simple eleemnts.
 * It looks like the advanced table.
 *
 * @version 1.0 4/10/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TableRenderer implements ElementRenderer {

    /**
     * The minimum table scroll pane size.
     */
    static final Dimension minimumSize = new Dimension(350, 50);

    /**
     * the column name (unbounded child name)
     */
    private String columnName;

    /**
     * The parent type.
     */
    private String parentType;

    /**
     * the main table
     */
    private AdvancedTable table;

    /**
     * changing type
     */
    private String changingType = null;

    /**
     * changing column index
     */
    private int changingColumnIndex;

    /**
     * the main panel
     */
    private JPanel panel;

    /**
     * the action listener
     */
    private ActionListener actionListener;

    /**
     * the editor for the table cells
     */
    private CompositeCellEditor cellEditor = new CompositeCellEditor(new JTextField());

    /**
     * the renderer for the table cells
     */
    private CompositeCellRenderer cellRenderer = new CompositeCellRenderer();


    /**
     * Constructs a new table renderer with the specified column name.
     * Creates the main panel with the advanced table.
     *
     * @param columnName    the specified column name.
     */
    public TableRenderer(String columnName, String parentType) {
        this.columnName = columnName;
        this.parentType = parentType;
        panel = new JPanel(new GridBagLayout());
//    {
//      public void setEnabled(boolean enabled) {
//        setEnabledContainer(panel, enabled);
//        if (!enabled)
//          removeAllRows();
//      }
//    };
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.insets = new Insets(5, 5, 5, 5);
        JScrollPane mainPane = new JScrollPane(getTable());
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
        checkSelectionProperties();
    }

    /**
     * Gets the main advanced table.
     *
     * @return  the main advanced table.
     */
    AdvancedTable getTable() {
        if (table == null) {
            Vector columns = new Vector();
            columns.addElement(AdvancedTable.firstColumnName);
            columns.addElement(columnName);
            table = new AdvancedTable(columns) {
                public boolean editCellAt(int row, int column) {
                    fireActionPerformed();
                    return super.editCellAt(row, column);
                }

                public TableCellEditor getCellEditor(int row, int column) {
                    if (column == 0 || TableRenderer.this.cellEditor == null) {
                        return super.getCellEditor(row, column);
                    }
                    return TableRenderer.this.cellEditor;
                }

                public TableCellRenderer getCellRenderer(int row, int column) {
                    if (column == 0 || TableRenderer.this.cellRenderer == null) {
                        return super.getCellRenderer(row, column);
                    }
                    return TableRenderer.this.cellRenderer;
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
                        } else if (oldValue != null && newValue != null
                                && !oldValue.equals(newValue)) {
                            BasicPanel.fireValueChanged(changingType, oldValue, newValue);
                        }
                    } else {
                        super.setValueAt(aValue, row, column);
                    }
                }

                protected void addTableRow() {
                    DefaultEditorPanel.needSave = true;
                    addRow();
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
                            removeRow(sel);
                        }
                    }
                }
            };
            addRow();
        }
        return table;
    }

    /**
     * Gets the objects array for the table row with the specified content.
     *
     * @param s   the specified content.
     * @return  the objects array for the table row.
     */
    private Object[] getObjectArray(String s) {
        Object[] array = new Object[2];
        array[0] = "";
        array[1] = (cellEditor == null) ? (Object) s
                : (Object) (new CompositeObject(true, s));
        return array;
    }

    /**
     * Adds the empty table row.
     */
    private void addRow() {
        addRow(getObjectArray(""));
    }

    /**
     * Adds the specified table row.
     *
     * @param row   the specified table row.
     */
    private void addRow(Object[] row) {
        checkEditStopped();
        ((DefaultTableModel) getTable().getModel()).addRow(row);
    }

    /**
     * Removes a table row with the specified index.
     *
     * @param index   the specified row index.
     */
    private void removeRow(int index) {
        checkEditStopped();
        if (index < 0 || index >= getTable().getRowCount()) {
            return;
        }
        ((DefaultTableModel) getTable().getModel()).removeRow(index);
    }

    /**
     * Removes all table rows.
     */
    private void removeAllRows() {
        int cnt = getTable().getRowCount();
        for (int i = 0; i < cnt; i++) {
            removeRow(cnt - 1 - i);
        }
    }


    /**
     * Method from the ElementRenderer interface.
     * Do nothing, it requires the enumeration.
     *
     * @param value   the specified value.
     * @see #setValues(Enumeration)
     * @see #getValue()
     */
    public void setValue(String value) {
    }

    /**
     * Sets the specified values enumeration to the table.
     *
     * @param en    the specified values enumeration.
     * @see #setValue(String)
     * @see #getValues()
     */
    public void setValues(Enumeration en) {
        removeAllRows();
        while (en.hasMoreElements()) {
            String content = ((SimpleNusuthWebAppElement) en.nextElement()).getContent();
            addRow(getObjectArray(content));
        }
    }

    /**
     * Method from the ElementRenderer interface.
     * Gets all table contents divided by ';'.
     *
     * @return  the all values
     * @see #setValue(String)
     * @see #getValues()
     */
    public String getValue() {
        // return values delimited by ;
        checkEditStopped();
        String res = "";
        int cnt = getTable().getRowCount();
        for (int i = 0; i < cnt; i++) {
            String value = getStringValue(getTable().getValueAt(i, 1));
            if (value != null && !value.equals("")) {
                if (i != 0) res += ";";
                res += value;
            }
        }
        return res;
    }

    /**
     * Gets the enumeration of the table contents.
     *
     * @return  the enumeration of the table contents
     * @see #getValue()
     * @see #setValues(Enumeration)
     */
    public Enumeration getValues() {
        checkEditStopped();
        Vector res = new Vector();
        int cnt = getTable().getRowCount();
        for (int i = 0; i < cnt; i++) {
            String value = getStringValue(getTable().getValueAt(i, 1));
            if (value != null && !value.equals(""))
                res.addElement(value);
        }
        return res.elements();
    }

    /**
     * Method from the ElementRenderer interface.
     * Gets the renderer content is empty or not.
     *
     * @return  <code>true</code> if the renderer content is empty;
     * <code>false</code> otherwise.
     * @see #getPar()
     */
    public boolean isContentEmpty() {
        return getValue().equals("");
    }

    /**
     * Gets the string value of the specified object.
     *
     * @param o   the specified object.
     * @return  the string value
     */
    private String getStringValue(Object o) {
        if (o == null) return null;
        return (o instanceof CompositeObject)
                ? ((((CompositeObject) o).getValue() == null)
                ? null : (String) ((CompositeObject) o).getValue())
                : (String) o;
    }

    /**
     * Method from the ElementRenderer interface.
     * Gets the component for this renderer.
     *
     * @return the component for this renderer.
     */
    public JComponent getComponent() {
        return panel;
    }

    /**
     * Method from the ElementRenderer interface.
     * Gets the component occupies all horizontal place or not.
     *
     * @return  <code>true</code> if the component occupies all horizontal place;
     * <code>false</code> otherwise.
     */
    public boolean takesAllPlace() {
        return true;
    }

    /**
     * Method from the ElementRenderer interface.
     * Adds the specified action listener.
     *
     * @param al    the specified action listener.
     * @see #removeActionListener(ActionListener)
     */
    public void addActionListener(ActionListener al) {
        actionListener = AWTEventMulticaster.add(actionListener, al);
    }

    /**
     * Method from the ElementRenderer interface.
     * Removes the specified action listener.
     *
     * @param al    the specified action listener.
     * @see #addActionListener(ActionListener)
     */
    public void removeActionListener(ActionListener al) {
        actionListener = AWTEventMulticaster.remove(actionListener, al);
    }

    /**
     * Calls the action performed method in the action listener.
     */
    private void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    /**
     * Checks the table on editing.
     */
    private void checkEditStopped() {
        if (getTable().isEditing()) {
            getTable().editingStopped(new ChangeEvent(this));
/*
      Component component = getTable().getEditorComponent();
      if (component instanceof JTextField) {
        Object value = ((JTextField) component).getText();
        getTable().setValueAt(value, getTable().getEditingRow(), getTable().getEditingColumn());
      } else if (component instanceof JPanel && ((JPanel) component).getComponent(0) instanceof JComboBox) {
        Object value = ((JComboBox) ((JPanel) component).getComponent(0)).getSelectedItem();
        getTable().setValueAt(value, getTable().getEditingRow(), getTable().getEditingColumn());
      }
      getTable().removeEditor();
*/
        }
    }

    /**
     * Gets the main column name.
     *
     * @return  the main column name.
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * Sets the specified cell editor to the table.
     *
     * @param cellEditor    the specified cell editor.
     * @see #getCellEditor()
     * @see #setCellRenderer(CompositeCellRenderer)
     */
    public void setCellEditor(CompositeCellEditor cellEditor) {
        this.cellEditor = cellEditor;
    }

    /**
     * Sets the specified cell renderer to the table.
     *
     * @param cellRenderer    the specified cell renderer.
     * @see #getCellRenderer()
     * @see #setCellEditor(CompositeCellEditor)
     */
    public void setCellRenderer(CompositeCellRenderer cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    /**
     * Gets the cell editor.
     *
     * @return  the cell editor.
     * @see #setCellEditor(CompositeCellEditor)
     */
    public CompositeCellEditor getCellEditor() {
        return this.cellEditor;
    }

    /**
     * Gets the cell renderer.
     *
     * @return  the cell renderer.
     * @see #setCellRenderer(CompositeCellRenderer)
     */
    public CompositeCellRenderer getCellRenderer() {
        return this.cellRenderer;
    }

    /**
     * checks if selection property contains key/value for that factory
     */
    private void checkSelectionProperties() {
        Enumeration keys = TablePanelFactory.selectionProp.keys();
        while (keys.hasMoreElements()) {
            String type = (String) keys.nextElement();
            String val = (String) TablePanelFactory.selectionProp.get(type);
            if (val.equals(this.parentType + "." + columnName)) {
                changingType = type;
                changingColumnIndex = 1;
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
}