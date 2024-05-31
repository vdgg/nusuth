package com.azoft.nusuth.gui;

public class HistoryComboBoxModel extends TimeComboBoxModel {
    public HistoryComboBoxModel() {
        super();
        super.addElement("10 seconds");
        super.addElement("30 seconds");
        super.addElement("1 minute");
        super.addElement("5 minutes");
        super.addElement("10 minutes");
        super.addElement("15 minutes");
        super.addElement("30 minutes");
        super.addElement("1 hour");
    }
}