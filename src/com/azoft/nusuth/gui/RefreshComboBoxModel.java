package com.azoft.nusuth.gui;

public class RefreshComboBoxModel extends TimeComboBoxModel {
    public RefreshComboBoxModel() {
        super();
        super.addElement("1 second");
        super.addElement("2 seconds");
        super.addElement("3 seconds");
        super.addElement("5 seconds");
        super.addElement("10 seconds");
        super.addElement("15 seconds");
        super.addElement("30 seconds");
        super.addElement("1 minute");
    }
}