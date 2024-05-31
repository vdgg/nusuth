/*
 * @(#)ProtocolCheckBoxElementRenderer.java 1.0 08/23/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.*;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Class ProtocolCheckBoxElementRenderer.
 *
 * @version 1.0 08/23/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ProtocolCheckBoxElementRenderer extends CheckBoxElementRenderer {
    static String[] items = {"HTTP", "HTTPS"};

    private GridBagConstraints c;

    public ProtocolCheckBoxElementRenderer() {
        super(items);
    }

    protected void createPanel() {
        panel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
    }

    protected JCheckBox addCheckBox(String name) {
        JCheckBox check = createNewCheckBox(name);
        panel.add(check, c);
        checkboxes.addElement(check);
        return check;
    }

    public void setValue(String value) {
        Vector v = new Vector();
        StringTokenizer st = new StringTokenizer(value, ";");
        while (st.hasMoreTokens()) {
            v.addElement(st.nextToken().toUpperCase());
        }
        setValues(v);
    }

    public String getValue() {
        String res = "";
        for (int i = 0; i < checkboxes.size(); i++) {
            JCheckBox check = (JCheckBox) checkboxes.elementAt(i);
            if (check.isSelected()) {
                res += (res.equals("")) ? check.getText() : ";" + check.getText();
            }
        }
        return res;
    }

    public boolean takesAllPlace() {
        return false;
    }


}
