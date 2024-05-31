package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class MessageDialog {
    private static JDialog dialog;
    private static JLabel l;
    private static ComboStatusLine comboStatusLine = null;
    private final static String TITLE = " Warning";

    static {
        dialog = null;
        l = new JLabel();
    }

    public static void createDialog(Frame parent) {
        dialog = new JDialog(parent, " message", true);
        JPanel panel = new JPanel();
        JPanel horpanel = new JPanel();
        dialog.getContentPane().add(panel);
        panel.setBorder(new EtchedBorder());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createRigidArea(new Dimension(1, 10)));
        panel.add(horpanel);
        panel.add(Box.createRigidArea(new Dimension(1, 10)));

        horpanel.setLayout(new BoxLayout(horpanel, BoxLayout.X_AXIS));
        horpanel.add(Box.createRigidArea(new Dimension(20, 1)));
        horpanel.add(l);
        horpanel.add(new Panel());
        horpanel.add(Box.createRigidArea(new Dimension(20, 1)));
        SwingUtilities.updateComponentTreeUI(dialog);
    }

    public static void setMessage(String message) {
        if (dialog != null) {
            l.setText(message);
        }
        if (comboStatusLine != null)
            comboStatusLine.setStatusString(message);
    }

    public static void showMessage(Frame parent, String message, boolean doPack) {
        showMessage(parent, TITLE, message, doPack);
    }

    public static void showMessage(Frame parent, String title, String message, boolean doPack) {
        getDialog(parent, title, message, doPack).show();
    }

    public static void hideMessage() {
        if (dialog != null) dialog.hide();
    }

    public static JDialog getDialog(Frame parent, String title, String message, boolean doPack) {
        if (dialog == null) createDialog(parent);
        dialog.setTitle(title);
        setMessage(message);
        if (doPack) {
            try {
                dialog.pack();
            } catch (NullPointerException e) {
            }
            if (!dialog.getTitle().equals(TITLE)) dialog.setSize(new Dimension((int) (dialog.getWidth() * 1.2), dialog.getHeight()));
            dialog.setLocationRelativeTo(parent);
        }
        return dialog;
    }

    public static void updateUI() {
        SwingUtilities.updateComponentTreeUI(dialog);
    }

    public static void setComboStatusLine(ComboStatusLine csl) {
        comboStatusLine = csl;
    }
}
