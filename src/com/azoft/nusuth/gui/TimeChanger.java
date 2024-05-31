/*
 * @(#)TimeChanger.java 1.0 03/02/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.AWTEventMulticaster;
import java.awt.*;

/**
 * Class TimeChanger is used for a time editing.
 * It looks as: number changer + combobox with second, minute, hour, day items
 * Sometime one item is fixed (minute, for example) -
 * in case of withLabel = <code>true</code>
 *
 * @version 1.0 03/02/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TimeChanger extends JPanel {
    private JComboBox comboBox = null;
    private NumberChanger numberChanger;
    private JLabel minuteLabel;
    private String[] items = {"second(s)", "minute(s)", "hour(s)", dayItem};
    private static String dayItem = "day(s)";
    private ActionListener actionListener;
    private int lastIndex = 0;

    /**
     * Defines the next condition -
     * if <code>true</code> - this look like as: number + minute(s) label & value will be
     * without 'm' (for ex: '10');
     * if <code>false</code>: number + items combobox & value contain simbol
     * 's' or 'm' or 'h' or 'd' (for ex: '10s')
     */
    private boolean withLabel = false;


    public TimeChanger() {
        this(false);
    }

    public TimeChanger(boolean withLabel) {
        this(withLabel, false);
    }

    public TimeChanger(boolean withLabel, boolean withDay) {
        super();
        this.withLabel = withLabel;
        setLayout(new GridLayout(1, 2));
        numberChanger = new NumberChanger(1, 1, 59);
        add(numberChanger);
        if (withLabel) {
            minuteLabel = new JLabel(" minute(s)");
            minuteLabel.setFont(new Font("Helvetica", Font.PLAIN, 14));
            add(minuteLabel);
        } else {
            comboBox = new JComboBox();
            for (int i = 0; i < items.length; i++) {
                if (withDay || !items[i].equals(dayItem)) {
                    comboBox.addItem(items[i]);
                }
            }
            comboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    int selectedIndex = comboBox.getSelectedIndex();
                    if (selectedIndex != lastIndex) {
                        fireActionPerformed();
                        lastIndex = selectedIndex;
                    }
                }
            });
            add(comboBox);
        }
    }

    public void setValue(String value) {
        // type: 10s (or 10, if withLabel)
        if (withLabel) {
            numberChanger.setValue(value);
        } else {
            int indexOf = 0;
            int itemIndex = 0;
            for (int i = 0; i < items.length; i++) {
                indexOf = value.indexOf(items[i].substring(0, 1));
                if (indexOf != -1) {
                    itemIndex = i;
                    break;
                }
            }
            indexOf = (indexOf == -1) ? value.length() : indexOf;
            numberChanger.setValue(value.substring(0, indexOf));
            if (comboBox != null) {
                comboBox.setSelectedIndex(itemIndex);
            }
        }
    }

    public String getValue() {
        String text = numberChanger.getValue();
        return (text.equals("")) ? "" : (comboBox == null) ? text : text + ((String) comboBox.getSelectedItem()).substring(0, 1);
    }

    public void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
        numberChanger.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
        numberChanger.removeActionListener(l);
    }

    private void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (comboBox != null) {
            comboBox.setEnabled(enabled);
        }
        numberChanger.setEnabled(enabled);
    }

    /**
     * Overrides the super method to the numberChanger request the focus
     */
    public void requestFocus() {
        numberChanger.requestFocus();
    }

    public void setComboValue(int index) {
        if (comboBox != null) {
            comboBox.setSelectedIndex(index);
        }
    }

    public void setComboValue(String s) {
        if (comboBox != null) {
            for (int i = 0; i < items.length; i++) {
                if (s.equals(items[i].substring(0, 1))) {
                    comboBox.setSelectedItem(items[i]);
                    return;
                }
            }
        }
    }

    public void setNumberValue(String sv) {
        numberChanger.setValue(sv);
    }

    public int getComboIndex() {
        return (comboBox == null) ? 0 : comboBox.getSelectedIndex();
    }

    public String getComboValue() {
        return (comboBox == null) ? "s" : ((String) comboBox.getSelectedItem()).substring(0, 1);
    }

    public String getNumberValue() {
        return numberChanger.getValue();
    }

    public void setSeconds(int sec) {
        int si = 0;
        int rest = sec;
        while ((si < 2 && rest >= 60) || (si == 2 && rest >= 24)) {
            rest = (si < 2) ? (int) (rest / 60) : (int) (rest / 24);
            si++;
        }
        if (si >= items.length) si = 0;
        setComboValue(si);
        String sv = (rest <= 0) ? "1" : "" + rest;
        setNumberValue(sv);
    }

    public void setSeconds(String ssec) {
        try {
            int sec = Integer.parseInt(ssec);
            setSeconds(sec);
        } catch (Exception e) {
        }
    }

    public int getSeconds() {
        int index = getComboIndex();
        String sv = getNumberValue();
        int v = 1;
        try {
            v = Integer.parseInt(sv);
        } catch (Exception e) {
        }
        return (index == 1) ? v * 60 : (index == 2) ? v * 60 * 60 : (index == 3) ? v * 60 * 60 * 24 : v;
    }
}
