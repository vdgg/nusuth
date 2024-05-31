package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class RetryDialog {
    private Component owner;
    private String message;
    private Object[] options = {"Retry", "Cancel"};

    public RetryDialog(Component owner, String message) {
        super();
        this.owner = owner;
        this.message = message;
    }

    public boolean isRetry() {
        int res = JOptionPane.showOptionDialog(owner, message, "Error", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        return res == JOptionPane.OK_OPTION;
    }
}