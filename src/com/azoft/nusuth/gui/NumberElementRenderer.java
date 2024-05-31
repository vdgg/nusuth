package com.azoft.nusuth.gui;

import java.awt.event.ActionListener;
import javax.swing.JComponent;

public class NumberElementRenderer implements ElementRenderer {
    private NumberChanger numberChanger;

    public NumberElementRenderer(int maxValue) {
        numberChanger = new NumberChanger(1, 0, maxValue);
    }

    public NumberElementRenderer() {
        numberChanger = new NumberChanger();
    }

    public void setValue(String value) {
        numberChanger.setValue(value);
    }

    public String getValue() {
        return numberChanger.getValue();
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
        return numberChanger;
    }

    public boolean takesAllPlace() {
        return false;
    }

    public void addActionListener(ActionListener l) {
        numberChanger.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        numberChanger.removeActionListener(l);
    }
}
