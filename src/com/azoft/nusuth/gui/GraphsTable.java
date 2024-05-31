package com.azoft.nusuth.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.colorchooser.DefaultColorSelectionModel;
import javax.swing.table.*;

import com.azoft.nusuth.management.*;
import com.azoft.nusuth.management.rmi.*;

public class GraphsTable extends JTable {
    private Vector graphsRowData = new Vector();
    private String[] columnNames = {"graph name", "show", "component name", "hosts", "apps", "color"};
    private int showColumn = 1;
    private Vector data = new Vector();
    private int[] argscount = new int[0];
    public static final String ALLAPPS = "All";
    public static final String ALLHOSTS = "All";
    private static final int hostColumn = 3;
    private static final int appColumn = 4;
    private static final int colorColumn = 5;
    private String[] startItems = {"ALLAPPS"};
    public String currentType = "container";

    private BasicPanel basicPanel;
    private FixValuesElementRenderer hostsRenderer;
    private FixValuesElementRenderer functionsRenderer;
    private FixValuesElementRenderer componentNamesRenderer;

    public GraphsTable() {
        super();
        this.basicPanel = ManageTool.getBasicPanel();
        setModel(new GraphTableModel());
        setRowSelectionAllowed(true);
        hostsRenderer = new FixValuesElementRenderer(null);
        functionsRenderer = new FixValuesElementRenderer(null);
        componentNamesRenderer = new FixValuesElementRenderer(null);
        FixValuesElementRenderer appsRenderer = new FixValuesElementRenderer(startItems);
        BasicPanel.addChangingValuesElementRenderer("host", hostsRenderer);
        BasicPanel.addChangingValuesElementRenderer("context", appsRenderer);
        BasicPanel.addChangingValuesElementRenderer("function", functionsRenderer);
        BasicPanel.addChangingValuesElementRenderer("component", componentNamesRenderer);
        TableColumn colorTableColumn = this.getColumn("color");
        colorTableColumn.setCellEditor(new ColorCellEditor());
        colorTableColumn.setCellRenderer(new DefaultTableCellRenderer() {
            protected void setValue(Object value) {
                if (value instanceof Color) setBackground((Color) value);
            }
        });
        colorTableColumn.setMaxWidth(SwingUtilities.computeStringWidth(getFontMetrics(getFont()), " color "));
        this.getColumn("show").setCellEditor(new DefaultCellEditor((JComboBox) functionsRenderer.getComponent()));
        this.getColumn("component name").setCellEditor(new DefaultCellEditor((JComboBox) componentNamesRenderer.getComponent()));
        this.getColumn("hosts").setCellEditor(new DefaultCellEditor((JComboBox) hostsRenderer.getComponent()));
        this.getColumn("apps").setCellEditor(new DefaultCellEditor((JComboBox) appsRenderer.getComponent()));
    }

    public TableCellEditor getCellEditor(int row, int column) {
        if (column == appColumn) {
            // applications
            String host = (String) getValueAt(row, column - 1);
            BasicPanel.setContextByHost(host);
        }
        return super.getCellEditor(row, column);
    }

    public void addRow() {
        ((GraphTableModel) getModel()).addRow();
    }

    public void removeRowAt(int index) {
        ((GraphTableModel) getModel()).removeRow(index);
    }

    public void setGraphInfos(Vector graphInfos) {
        int rowcount = graphInfos.size();
        while (rowcount < ((GraphTableModel) getModel()).getRowCount()) {
            removeRowAt(((GraphTableModel) getModel()).getRowCount() - 1);
        }
        while (rowcount > ((GraphTableModel) getModel()).getRowCount()) {
            addRow();
        }
        argscount = new int[rowcount];
        for (int i = 0; i < rowcount; i++) {
            GraphInfo gi = (GraphInfo) graphInfos.elementAt(i);
            ((GraphTableModel) getModel()).setValueAt(gi.name, i, 0);
            ((GraphTableModel) getModel()).setValueAt(gi.functionName, i, 1);
            int count = basicPanel.getArgsCount(gi.functionName);
            setArgsCount(count, i);
//            ((GraphTableModel)getModel()).setValueAt(gi.systemId, i, 2);
            ((GraphTableModel) getModel()).setValueAt(BasicPanel.getComponentName(currentType, gi.systemId), i, 2);
            ((GraphTableModel) getModel()).setValueAt(Graph.getColor(gi.color), i, colorColumn);
            int ind = gi.args.indexOf("/");
            String host = (ind == -1) ? ALLHOSTS : gi.args.substring(0, ind);
            String app = (ind == -1 || gi.args.length() <= ind + 1) ? ALLAPPS : gi.args.substring(ind + 1);
            if (app.startsWith(ALLAPPS) && app.length() > ALLAPPS.length()) app = ALLAPPS;
            ((GraphTableModel) getModel()).setValueAt(host, i, hostColumn);
            ((GraphTableModel) getModel()).setValueAt(app, i, appColumn);
        }
    }

    private String getRealCellValue(int i, int j, ElementRenderer renderer) {
        Object o = (String) ((GraphTableModel) getModel()).getValueAt(i, j);
        if (o == null || o.equals("")) {
            o = ((JComboBox) renderer.getComponent()).getItemAt(0);
            if (o == null) o = "";
        }
        return o.toString();
    }

    public Vector getGraphInfos() {
        Vector res = new Vector();
        checkEditStopped();
        for (int i = 0; i < ((GraphTableModel) getModel()).getRowCount(); i++) {
            GraphInfo gi = new GraphInfo();
            gi.name = (String) ((GraphTableModel) getModel()).getValueAt(i, 0);
            gi.functionName = getRealCellValue(i, 1, functionsRenderer);
            gi.name = (gi.name == null || gi.name.equals("")) ? gi.functionName : gi.name;
//            gi.systemId = getRealCellValue(i, 2, componentNamesRenderer);
            gi.systemId = BasicPanel.getComponentId(currentType, getRealCellValue(i, 2, componentNamesRenderer));
            gi.color = Graph.getColorString((Color) ((GraphTableModel) getModel()).getValueAt(i, colorColumn));
            gi.args = (argscount[i] == 0) ? "" : (String) ((GraphTableModel) getModel()).getValueAt(i, hostColumn) + "/" + (String) ((GraphTableModel) getModel()).getValueAt(i, appColumn);
            res.addElement(gi);
        }
        return res;
    }

    public void setArgsCount(int count, int index) {
        this.argscount[index] = count;
        if (count == 0) {
            if (!BasicPanel.hasDefaultHost())
                BasicPanel.fireValueAdded("host", ALLHOSTS);
            ((GraphTableModel) getModel()).setAppsAll(index);
        } else {
            BasicPanel.fireValueRemoved("host", ALLHOSTS);
            ((GraphTableModel) getModel()).setDefaultHostValueAt(index);
        }
    }

    public void fireTypeChanged(String type) {
        BasicPanel.setComponentsByType(type);  // check on adding/deleting when type is the same
        if (!currentType.equals(type))
            ((GraphTableModel) getModel()).setDefaultValues();
        currentType = type;
    }

    private void incArgsCount() {
        int size = argscount.length;
        int[] old = argscount;
        argscount = new int[size + 1];
        System.arraycopy(old, 0, argscount, 0, size);
        argscount[size] = 0;
    }

    private void decArgsCount(int index) {
        int size = argscount.length;
        int[] old = argscount;
        argscount = new int[size - 1];
        int j = size - index - 1;
        System.arraycopy(old, 0, argscount, 0, index);
        if (j > 0) {
            System.arraycopy(old, index + 1, argscount, index, j);
        }
    }

    private void checkEditStopped() {
        if (isEditing()) {
            Component component = getEditorComponent();
            if (component instanceof JComboBox) {
                Object value = ((JComboBox) component).getSelectedItem();
                setValueAt(value, getEditingRow(), getEditingColumn());
            }
            removeEditor();
        }
    }

    private class GraphTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public Object getValueAt(int row, int col) {
            try {
                Object o = ((Vector) data.elementAt(row)).elementAt(col);
                return (o == null) ? "" : o;
            } catch (Exception e) {
            }
            return "";
        }

        public String getColumnName(int column) {
            return columnNames[column];
        }

        public Class getColumnClass(int c) {
            return (c == colorColumn) ? java.awt.Color.class : java.lang.String.class;
        }

        public boolean isCellEditable(int row, int col) {
            if (col == appColumn || col == hostColumn)
                return (argscount[row] > 0);
            return true;
        }

        public void setValueAt(Object aValue, int row, int column) {
            try {
                ((Vector) data.elementAt(row)).setElementAt(aValue, column);
                if (column == showColumn) {
                    int count = basicPanel.getArgsCount((String) aValue);
                    setArgsCount(count, row);
                }
                super.fireTableCellUpdated(row, column);
            } catch (Exception e) {
            }
        }

        public void addRow() {
            incArgsCount();
            Vector v = new Vector();
            for (int i = 0; i < getColumnCount(); i++) {
                Object o = (i == hostColumn) ? (Object) ALLHOSTS : (i == appColumn) ? (Object) ALLAPPS : (i == colorColumn) ? (Object) Color.green : (Object) (new String(""));
                v.addElement(o);
            }
            data.addElement(v);
            setEditingRow(data.size() - 1);
            super.fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public void removeRow(int index) {
            decArgsCount(index);
            data.removeElementAt(index);
            super.fireTableRowsDeleted(index, index);
        }

        public void setAppsAll(int index) {
            setValueAt(ALLHOSTS, index, hostColumn);
            setValueAt(ALLAPPS, index, appColumn);
        }

        public void setDefaultHostValueAt(int index) {
            Object value = ((DefaultCellEditor) getColumn("hosts").getCellEditor()).getCellEditorValue();
            setValueAt(value, index, hostColumn);
        }

        public void setDefaultValues() {
            setValuesToColumn(1); // show (functions)
            setValuesToColumn(2); // component names
        }

        private void setValuesToColumn(int column) {
            String columnName = getColumnName(column);
            int cnt = getRowCount();
            for (int i = 0; i < cnt; i++) {
                Object value = ((DefaultCellEditor) getColumn(columnName).getCellEditor()).getCellEditorValue();
                setValueAt(value, i, column);
            }
        }
    }

}
