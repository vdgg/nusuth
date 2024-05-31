package com.azoft.nusuth.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.AWTEventMulticaster;

public class MultiListRenderer implements ChangingValuesElementRenderer {
    private JTextField textField = new JTextField();
    private JButton button = new JButton("...");
    private JList list;
    private JPanel panel = new JPanel();
    private Object[] options = {"OK"};
    private ActionListener actionListener;
    private JScrollPane pane;
    private DefaultListModel model;

    public MultiListRenderer() {
        model = new DefaultListModel();
        list = new JList(model); //{
        pane = new JScrollPane(list);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.setLayout(new BorderLayout(0, 0));
        panel.add("Center", textField);
        panel.add("East", button);
        button.setMargin(new Insets(0, 1, 0, 1));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object[] mes = new Object[1];
                mes[0] = pane;
                int res = JOptionPane.showOptionDialog(ManageTool.getMainFrame(), mes, "Choose some items, please", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
                if (res == JOptionPane.OK_OPTION) {
                    Object[] sels = list.getSelectedValues();
                    String s = "";
                    for (int i = 0; i < sels.length; i++) {
                        s += (i == 0) ? sels[i] : ";" + sels[i];
                    }
                    textField.setText(s);
                } else
                    selectListItems();
            }
        });
    }

    public void addItem(String id, String item) {
        model.addElement(item);
    }

    public void removeItem(String id, String item) {
        model.removeElement(item);
    }

    public void changeItem(String id, String oldItem, String newItem) {
        int index = model.lastIndexOf(oldItem);
        if (index != -1) model.set(index, newItem);
    }

    public void removeAllItems() {
        model.removeAllElements();
    }

    public void setValue(String value) {
        textField.setText(value);
        selectListItems();
    }

    private void selectListItems() {
        StringTokenizer st = new StringTokenizer(textField.getText(), ";");
        int[] indices = new int[0];
        while (st.hasMoreTokens()) {
            int index = model.indexOf(st.nextToken());
            if (index != -1) {
                int[] oldInd = indices;
                int len = oldInd.length;
                indices = new int[len + 1];
                System.arraycopy(oldInd, 0, indices, 0, len);
                indices[len] = index;
            }
        }
        list.setSelectedIndices(indices);
    }

    public String getValue() {
        return textField.getText();
    }

    public JComponent getComponent() {
        return panel;
    }

    /**
     * Gets the renderer content is empty or not.
     *
     * @return  <code>true</code> if the renderer content is empty;
     * <code>false</code> otherwise.
     * @see #getPar()
     */
    public boolean isContentEmpty() {
        return getValue().equals("");
    }

    public boolean takesAllPlace() {
        return true;
    }

    public void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                fireActionPerformed();
            }

            public void removeUpdate(DocumentEvent e) {
                fireActionPerformed();
            }

            public void changedUpdate(DocumentEvent e) {
                fireActionPerformed();
            }
        });
    }

    public void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    private void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }
}

