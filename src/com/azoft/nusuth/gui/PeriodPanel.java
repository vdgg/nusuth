/*
 * @(#)PeriodPanel.java 1.0 5/29/2001
 */

package com.azoft.nusuth.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.text.*;

/**
 * Class PeriodPanel
 *
 * @version 1.0 5/29/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class PeriodPanel extends JPanel {
    JTextField from;
    JTextField to;
    static SimpleDateFormat dateFormat = new SimpleDateFormat("d/M/yy H:m:s");

    public PeriodPanel() {
        super();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.gridwidth = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0;
        add(new JLabel("  From  "), c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        add(getFromTextField(), c);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        add(new JLabel("  to  "), c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.weightx = 1.0;
        add(getToTextField(), c);
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 0.0;
        add(new JLabel("  (d/M/yy H:m:s) "), c);
    }

    private JTextField getFromTextField() {
        if (from == null) {
            from = new JTextField();
        }
        return from;
    }

    private JTextField getToTextField() {
        if (to == null) {
            to = new JTextField();
        }
        return to;
    }

    public long getFromMillis() {
        return getMillis(getFromTextField());
    }

    public long getToMillis() {
        return getMillis(getToTextField());
    }

    private long getMillis(JTextField t) {
        ParsePosition pos = new ParsePosition(0);
        return dateFormat.parse(t.getText().trim(), pos).getTime();
    }

    public void setFromMillis(long l) {
        getFromTextField().setText(formatLong(l));
    }

    public void setToMillis(long l) {
        getToTextField().setText(formatLong(l));
    }

    public void setEnabled(boolean b) {
        super.setEnabled(b);
        int cnt = getComponentCount();
        for (int i = 0; i < cnt; i++) {
            getComponent(i).setEnabled(b);
        }
    }

    public static String formatLong(long time) {
        return dateFormat.format(new Date(time));
    }

    public static void printNow() {
        System.out.println("dateFormat.format(new Date()) = " + dateFormat.format(new Date()));
    }
}

