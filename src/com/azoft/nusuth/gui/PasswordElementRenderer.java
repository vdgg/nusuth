package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Color;
import java.awt.AWTEventMulticaster;

import com.azoft.nusuth.management.security.UnauthorizedAccessException;
import com.azoft.nusuth.management.ManagementException;

public class PasswordElementRenderer implements ElementRenderer,
        PasswordEnabledListener {
    private JButton button;
    private String cryptPassword = "";
    private ActionListener actionListener;

    private String error = "";
    private int cnt;
    private static int MAX_COUNT = 3;
    private Object[] messages;
    private JPasswordField oldPassword;
    private JPasswordField newPassword;
    private JPasswordField confirmPassword;
    private JLabel warningString;
    private static String[] options = {"OK", "Cancel"};

    public PasswordElementRenderer() {
        button = new JButton("Set Password");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String newPassword = getNewPassword();
                if (newPassword != null) {
                    cryptPassword = MD5.cryptPassword(newPassword);
                    fireActionPerformed();
                }
            }
        });
    }

    public void setValue(String value) {
        cryptPassword = value;
    }

    public void setUserName(String name) {
        button.setEnabled(name.equals(ManageTool.getUserName()));
    }

    public String getValue() {
        return cryptPassword;
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
        return button;
    }

    public boolean takesAllPlace() {
        return false;
    }

    public void addActionListener(ActionListener al) {
        actionListener = AWTEventMulticaster.add(actionListener, al);
    }

    public void removeActionListener(ActionListener al) {
        actionListener = AWTEventMulticaster.remove(actionListener, al);
    }

    private void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    private boolean verifyOldPasswordField(String s) {
        return s.equals(MD5.cryptPassword(getPasswordString(getOldPasswordField())));
    }

    private boolean verifyCond() {
//        if (!ManageTool.verifyPassword(getOldPasswordField().getText())){
        if (!(verifyOldPasswordField(cryptPassword) ||
                (BasicPanel.userIsAdmin() && verifyOldPasswordField(BasicPanel.getAdminPassword()))
                )) {
            error = "ERROR! Your old password is not rigth!";
            return false;
        }
        if (getPasswordString(getNewPasswordField()).equals("")) {
            error = "ERROR! New password is empty!";
            return false;
        }
        if (!getPasswordString(getNewPasswordField()).equals(getPasswordString(getConfirmPasswordField()))) {
            error = "ERROR! Confirm password not equals to new password!";
            return false;
        }
        return true;
    }

    private String getError() {
        return error;
    }

    private String getNewPassword() {
        return getNewPassword(true);
    }

    private String getNewPassword(boolean firstCall) {
        if (firstCall) {
            cnt = MAX_COUNT;
            getAuthString().setText("");
        } else
            getAuthString().setText(getError());
        if (cnt-- > 0) {
            int res = getNewPasswordDialog();
            if (res != JOptionPane.OK_OPTION) return null;
            if (!verifyCond()) return getNewPassword(false);
            return getPasswordString(getNewPasswordField());
        }
        ManageTool.showMessage("you have taped wrong password for the " + MAX_COUNT + "th time");
        return null;
    }

    private int getNewPasswordDialog() {
        if (messages == null) {
            GridBagLayout gridbag = new GridBagLayout();
            JPanel p = new JPanel(gridbag);
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);

            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 0.0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = 1;
            p.add(getAuthString(), c);

            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            c.weightx = 0.0;
            p.add(new JLabel("Old Password:"), c);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1.0;
            p.add(getOldPasswordField(), c);

            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            c.weightx = 0.0;
            p.add(new JLabel("New Password:"), c);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1.0;
            p.add(getNewPasswordField(), c);

            c.gridwidth = GridBagConstraints.RELATIVE;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            c.weightx = 0.0;
            p.add(new JLabel("Confirm Password:"), c);

            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1.0;
            p.add(getConfirmPasswordField(), c);

            messages = new Object[1];
            messages[0] = p;
        }
        getOldPasswordField().setText("");
        getNewPasswordField().setText("");
        getConfirmPasswordField().setText("");
        return JOptionPane.showOptionDialog(ManageTool.getMainFrame(), messages,
                "Set new password", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
    }

    private JPasswordField getOldPasswordField() {
        if (oldPassword == null) {
            oldPassword = new JPasswordField(20);
        }
        return oldPassword;
    }

    private JPasswordField getNewPasswordField() {
        if (newPassword == null) {
            newPassword = new JPasswordField(20);
        }
        return newPassword;
    }

    private JPasswordField getConfirmPasswordField() {
        if (confirmPassword == null) {
            confirmPassword = new JPasswordField(20);
        }
        return confirmPassword;
    }

    /**
     * Gets the password string.
     * Instead of deprecated getText.
     */
    private String getPasswordString(JPasswordField pf) {
        return new String(pf.getPassword());
    }

    public JLabel getAuthString() {
        if (warningString == null) {
            warningString = new JLabel("");
            warningString.setForeground(Color.red);
        }
        return warningString;
    }

    public void passwordEnabled(boolean b) {
        button.setEnabled(b);
    }
}