package com.azoft.nusuth.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class TimeElementRenderer implements ElementRenderer {
    protected TimeChanger timeChanger;

    public TimeElementRenderer() {
        timeChanger = new TimeChanger();
    }

    public void setValue(String value) {
        timeChanger.setValue(value);
    }

    public String getValue() {
        return timeChanger.getValue();
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

    public JComponent getComponent() {
        return timeChanger;
    }

    public boolean takesAllPlace() {
        return false;
    }

    public void addActionListener(ActionListener l) {
        timeChanger.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        timeChanger.removeActionListener(l);
    }
}