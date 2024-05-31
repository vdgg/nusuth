/*
 * @(#)AdvancedTable.java 1.0 08/31/01
 */

package com.azoft.nusuth.gui;

import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.util.Vector;
import java.util.Enumeration;
import java.awt.event.*;
import java.awt.*;

/**
 * Class AdvancedTable.
 *
 * @version 1.0 08/31/01
 * @author  tanya
 * @since Nusuth1.0
 */
public class AdvancedTable extends JTable {

    /**
     * defines the first column name
     */
    static String firstColumnName = "  ";

    /**
     * the renderer for the first column
     */
    FirstColumnTableCellRenderer firstColunmRenderer = null;

    /**
     * popup menu with the
     * remove selected rows, remove content & add row items
     */
    private JPopupMenu popupMenu = null;

    /**
     * removes selected rows
     */
    private ActionListener removeSelectedRowListener = null;

    /**
     * removes content
     */
    private ActionListener removeContentListener = null;

    /**
     * adds a row to main table
     */
    private ActionListener addRowListener = null;


    /**
     * Constructs a new advanced table.
     * Sets the column identifiers & the width of the first column.
     * Adds a list selection listener to column selection model
     * to forbid cursor being on the first column
     * and mouse listener to show popup menu on rigth click.
     */
    public AdvancedTable(Vector columnNames) {
        super();
        Vector realNames = new Vector();
        Enumeration en = columnNames.elements();
        while (en.hasMoreElements()) {
            realNames.addElement(DefaultEditorPanel.getDisplayName((String) en.nextElement()));
        }
        ((DefaultTableModel) getModel()).setColumnIdentifiers(realNames);
        getTableHeader().setReorderingAllowed(false);
        TableColumn tc = ((TableColumnModel) getColumnModel()).getColumn(0);
        tc.setPreferredWidth(20);
        tc.setMaxWidth(20);
        tc.setMinWidth(20);
        tc.setResizable(false);
        setRowSelectionAllowed(false);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getModifiers() == InputEvent.BUTTON3_MASK) {
                    getPopupMenu().show(me.getComponent(), me.getX(), me.getY());
                }
            }
        });
        getColumnModel().getSelectionModel().
                addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        int index = getColumnModel().
                                getSelectionModel().getAnchorSelectionIndex();
                        if (index == 0) {
                            if (!e.getValueIsAdjusting()
                                    && !AdvancedTable.this.getRowSelectionAllowed()) {
                                AdvancedTable.this.getColumnModel().
                                        getSelectionModel().setSelectionInterval(1, 1);
                            } else {
                                AdvancedTable.this.setRowSelectionAllowed(true);
                            }
                        } else {
                            AdvancedTable.this.setRowSelectionAllowed(false);
                            AdvancedTable.this.clearSelection();
                        }
                    }
                });
    }

    /**
     * Inits the popup menu & return it.
     *
     * @return  the popup menu for this table
     */
    JPopupMenu getPopupMenu() {
        if (popupMenu == null) {
            popupMenu = new JPopupMenu();
            JMenuItem mi = popupMenu.add(new JMenuItem("Remove selected rows"));
            mi.addActionListener(getRemoveSelectedRowListener());
            mi = popupMenu.add(new JMenuItem("Remove content"));
            mi.addActionListener(getRemoveContentListener());
            mi = popupMenu.add(new JMenuItem("Add new row"));
            mi.addActionListener(getAddRowListener());
        }
        return popupMenu;
    }

    /**
     * Gets the action listener, which deletes selected rows in main table.
     *
     * @return  the action listener
     */
    private ActionListener getRemoveSelectedRowListener() {
        if (removeSelectedRowListener == null) {
            removeSelectedRowListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getRowSelectionAllowed()) {
                        deleteSelectedRows();
                    }
                }
            };
        }
        return removeSelectedRowListener;
    }

    /**
     * Gets the action listener, which adds a new row in main table.
     *
     * @return  the action listener
     */
    public ActionListener getAddRowListener() {
        if (addRowListener == null) {
            addRowListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    addTableRow();
                }
            };
        }
        return addRowListener;
    }

    /**
     * Gets the action listener, which deletes selected a row content
     * in the specified table.
     *
     * @return  the action listener
     */
    private ActionListener getRemoveContentListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int cnt = getColumnCount();
                int[] rs = getSelectedRows();
                for (int i = 0; i < rs.length; i++) {
                    for (int colIndex = 0; colIndex < cnt; colIndex++) {
                        deleteCellContent(rs[i], colIndex);
                    }
                }
            }
        };
    }

    /**
     * Gets the table cell renderer for the specified row & column.
     *
     * @param   row     the table row.
     * @param   column  the table column.
     */
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 0) {
            return getFirstColumnRenderer();
        }
        return super.getCellRenderer(row, column);
    }

    /**
     * Gets the cell editor for the specified row & column.
     *
     * @param row     the specified row
     * @param column  the specified column
     * @return the cell editor
     */
    public TableCellEditor getCellEditor(int row, int column) {
        if (column == 0) {
            return null;
        }
        return super.getCellEditor(row, column);
    }

    /**
     * Gets the renderer for the first column.
     */
    private FirstColumnTableCellRenderer getFirstColumnRenderer() {
        if (firstColunmRenderer == null) {
            firstColunmRenderer = new FirstColumnTableCellRenderer();
        }
        return firstColunmRenderer;
    }

    /**
     * Sets the specified data vector to the table model.
     * The model for this table is the AdvancedTableModel.
     *
     * @param data    the specified data vector
     */
    public void setDataVector(Vector data) {
        ((AdvancedTableModel) getModel()).setDataVector(data);
    }

    /**
     * Returns the default table model object, which is
     * a <code>DefaultTableModel</code>.  A subclass can override this
     * method to return a different table model object.
     *
     * @return the default table model object
     * @see javax.swing.table.DefaultTableModel
     */
    protected TableModel createDefaultDataModel() {
        return new AdvancedTableModel();
    }

    /**
     * Processes the key binding.
     * If the key is 'Delete' it does deletion rows or content.
     *
     * @param   ks          the key stroke.
     * @param   ke          the key event.
     * @param   condition   the condition
     * @param   pressed     pressed or not (typed || released)
     * @return  the boolean value - value of super method
     */
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent ke,
                                        int condition, boolean pressed) {
        // if DELETE was pressed
        if (ks.getKeyCode() == KeyEvent.VK_DELETE) {
            if (getRowSelectionAllowed() && getSelectedRowCount() > 0) {
                deleteSelectedRows();
            } else {
                // delete cell content
                deleteCellContent(AdvancedTable.this.getEditingRow(),
                        AdvancedTable.this.getEditingColumn());
            }
        }
        return super.processKeyBinding(ks, ke, condition, pressed);
    }

// these two methods are defined in subclasses

    /**
     * Removes the selected table rows.
     */
    protected void deleteSelectedRows() {
    }

    /**
     * Adds one table row.
     */
    protected void addTableRow() {
    }

    /**
     * Removes the table cell content.
     */
    protected void deleteCellContent(int row, int column) {
        if (row < 0 || row > getRowCount()
                || column < 0 || column > getColumnCount()) {
            return;
        }
        if (isEditing()) {
            editingStopped(new ChangeEvent(this));
        }
        Object o = getValueAt(row, column);
        Object newo = null;
        if (o instanceof CompositeObject) {
            String newTextValue = "";
            TableCellEditor editor = getCellEditor(row, column);
            if (editor instanceof CompositeCellEditor) {
                newTextValue = ((CompositeCellEditor) editor).getDefaultValue();
            }
            newo = new CompositeObject(
                    ((CompositeObject) o).getSelected(), newTextValue);
        }
        newo = (newo == null) ? "" : newo;
        setValueAt(newo, row, column);
    }


    /**
     * Class AdvancedTableModel overrides one method from defalut table model -
     * setDataVector. It needed to do not change column structure.
     */
    private class AdvancedTableModel extends DefaultTableModel {

        /**
         * Overrides the super method to not change column structure.
         *
         * @see  DefaultTableModel#setDataVector(Vector, Vector)
         */
        public void setDataVector(Vector newData) {
            if (newData == null)
                throw new IllegalArgumentException("setDataVector() - Null parameter");

            dataVector = new Vector(0);
            dataVector = newData;
            newRowsAdded(new TableModelEvent(this, 0, getRowCount() - 1,
                    TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
        }
    }   // end of AdvancedTableModel


    private class FirstColumnTableCellRenderer extends DefaultTableCellRenderer {

        /**
         * Rized bevel border with 1-line.
         * Used for not selected components.
         */
        BevelBorder rizedBorder = new BevelBorder(BevelBorder.RAISED) {
            public Insets getBorderInsets(Component c) {
                return new Insets(1, 1, 1, 1);
            }

            public Insets getBorderInsets(Component c, Insets insets) {
                insets.left = insets.top = insets.right = insets.bottom = 1;
                return insets;
            }

            protected void paintRaisedBevel(Component c, Graphics g, int x, int y,
                                            int width, int height) {
                Color oldColor = g.getColor();
                int h = height;
                int w = width;

                g.translate(x, y);

                g.setColor(getHighlightOuterColor(c));
                g.drawLine(0, 0, 0, h - 1);
                g.drawLine(1, 0, w - 1, 0);

                g.setColor(getShadowOuterColor(c));
                g.drawLine(1, h - 1, w - 1, h - 1);
                g.drawLine(w - 1, 1, w - 1, h - 2);

                g.translate(-x, -y);
                g.setColor(oldColor);

            }
        };

        /**
         * Lowered bevel border with 1-line.
         * Used for selected components.
         */
        BevelBorder loweredBorder = new BevelBorder(BevelBorder.LOWERED) {
            public Insets getBorderInsets(Component c) {
                return new Insets(1, 1, 1, 1);
            }

            public Insets getBorderInsets(Component c, Insets insets) {
                insets.left = insets.top = insets.right = insets.bottom = 1;
                return insets;
            }

            protected void paintLoweredBevel(Component c, Graphics g, int x, int y,
                                             int width, int height) {
                Color oldColor = g.getColor();
                int h = height;
                int w = width;

                g.translate(x, y);

                g.setColor(getShadowInnerColor(c));
                g.drawLine(0, 0, 0, h - 1);
                g.drawLine(1, 0, w - 1, 0);

                g.setColor(getHighlightOuterColor(c));
                g.drawLine(1, h - 1, w - 1, h - 1);
                g.drawLine(w - 1, 1, w - 1, h - 2);

                g.translate(-x, -y);
                g.setColor(oldColor);
            }
        };

        /**
         * Constructs a new table cell renderer for the first column.
         */
        public FirstColumnTableCellRenderer() {
            super();
        }

        // implements javax.swing.table.TableCellRenderer
        // override the super method
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int column) {

            setBackground(table.getTableHeader().getBackground());
            setBorder((isSelected) ? loweredBorder : rizedBorder);
            return this;
        }

    }   // end of FirstColumnTableCellRenderer

}   // end of AdvancedTable
