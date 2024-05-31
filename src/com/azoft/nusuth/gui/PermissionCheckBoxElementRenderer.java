package com.azoft.nusuth.gui;

import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import java.util.Vector;

import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;

public class PermissionCheckBoxElementRenderer extends CheckBoxElementRenderer {
    private static String allItem = "*";
    private static String defaultItem = "see";
    private static String[] items = {allItem, defaultItem, "see config",
                                     "see security", "set config", "set security", "monitor", "add", "remove"};
    private JCheckBox allCheck;
    private JCheckBox defaultCheck;

    public PermissionCheckBoxElementRenderer() {
        super(items);
        JCheckBox check1 = getCheckBox(allItem);
        if (check1 != null)
            allCheck = check1;
        JCheckBox check2 = getCheckBox(defaultItem);
        if (check2 != null)
            defaultCheck = check2;
    }

    protected JCheckBox createNewCheckBox(String name) {
        JCheckBox check = new JCheckBox(name);
        if (name.equals(allItem))
            check.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    selectAll(((JCheckBox) e.getSource()).isSelected());
                }
            });
        else
            check.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    checkAll();
                }
            });
        return check;
    }

    private void checkAll() {
        boolean allSelected = true;
        boolean selFinished = false;
        boolean allDeselected = true;
        boolean deselFinished = false;
        for (int i = 0; i < checkboxes.size(); i++) {
            JCheckBox check = (JCheckBox) checkboxes.elementAt(i);
            if (!selFinished && !check.getText().equals(allItem) && !check.isSelected()) {
                allSelected = false;
                selFinished = true;
            }
            if (!deselFinished && !check.getText().equals(allItem) && check.isSelected()) {
                allDeselected = false;
                deselFinished = true;
            }
            if (selFinished && deselFinished) break;
        }
        allCheck.setSelected(allSelected);
        if (allDeselected) defaultCheck.setSelected(true);
    }

    private void selectAll(boolean b) {
        for (int i = 0; i < checkboxes.size(); i++) {
            ((JCheckBox) checkboxes.elementAt(i)).setSelected(b);
        }
        if (!b) defaultCheck.setSelected(true);
    }

    public void setValues(Enumeration e) {
        Vector values = new Vector();
        while (e != null && e.hasMoreElements()) {
            values.addElement(((SimpleNusuthWebAppElement) e.nextElement()).getContent());
        }
        if (values.contains(allItem)) {
            allCheck.setSelected(true);
            selectAll(true);
        } else {
            setValues(values);
            checkAll(); // may be it needed to set a defalut value
        }
    }

    public Enumeration getValues() {
        if (allCheck.isSelected()) {
            return getVectorWithString(allItem).elements();
        }
        Enumeration e = super.getValues();
        if (e.hasMoreElements()) return e;
        defaultCheck.setSelected(true);
        return getVectorWithString(defaultItem).elements();
    }

    private Vector getVectorWithString(String s) {
        Vector v = new Vector();
        v.addElement(s);
        return v;
    }
}