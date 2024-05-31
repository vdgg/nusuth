/*
 * @(#)ConnectionDialog.java 1.0 12/03/2000
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Class ConnectionDialog.
 *
 * @version 1.0 12/03/2000
 * @author vdgg, tanya
 * @since Nusuth1.0
 */
public class ConnectionDialog extends JDialog {

    private String host;
    private int port;
    private JTextField hostText = null;
    private JTextField portText = null;
    private Component parentComponent;
    private String title;
    private String shostLabel;
    private String sportLabel;
    private Object[] messages = null;
    private static String[] options = new String[2];

    public ConnectionDialog(Component parentComponent, String host, int port) {
        this(parentComponent, host, port, "Connection options", "Host to connect:", "Port to connect:", "Connect", "Cancel");
    }

    public ConnectionDialog(Component parentComponent, String title, String shostLabel, String sportLabel, String ok_label) {
        this(parentComponent, "", 0, title, shostLabel, sportLabel, ok_label, "Cancel");
    }

    public ConnectionDialog(Component parentComponent, String host, int port, String title, String shostLabel, String sportLabel, String ok_label, String cancel_label) {
        this.parentComponent = parentComponent;
        this.host = host;
        this.port = port;
        this.title = title;
        this.shostLabel = shostLabel;
        this.sportLabel = sportLabel;
        options[0] = ok_label;
        options[1] = cancel_label;
        GridBagLayout gridbag = new GridBagLayout();
//    JPanel tmp = new JPanel(new GridLayout(2,2,10,5));
        JPanel tmp = new JPanel(gridbag);
        GridBagConstraints c1 = new GridBagConstraints();
        GridBagConstraints c2 = new GridBagConstraints();
        JLabel hostLabel = new JLabel(shostLabel);
        JLabel portLabel = new JLabel(sportLabel);
        hostText = new JTextField(host, 5);
        portText = new JTextField((new Integer(port)).toString(), 5);
        c1.fill = GridBagConstraints.NONE;
        c1.anchor = GridBagConstraints.EAST;
        c1.weightx = 0.0;
        c1.gridwidth = GridBagConstraints.RELATIVE;
        c1.insets = new Insets(2, 2, 5, 5);

        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.anchor = GridBagConstraints.WEST;
        c2.weightx = 1.0;
        c2.gridwidth = GridBagConstraints.REMAINDER;
        c2.insets = new Insets(2, 5, 5, 5);

        tmp.add(hostLabel, c1);
        tmp.add(hostText, c2);
        tmp.add(portLabel, c1);
        tmp.add(portText, c2);
        messages = new Object[1];
        messages[0] = tmp;
    }

    String getHost() {
        return host;
    }

    int getPort() {
        return port;
    }

    public void show() {
        showDialog();
    }

    public boolean showDialog() {
        int res = JOptionPane.showOptionDialog(parentComponent, messages, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (res == JOptionPane.OK_OPTION) {
            if (hostText.getText().trim().length() == 0) {
                dispose();
                JOptionPane.showMessageDialog(parentComponent, "Specify host to connect!", "Error!", JOptionPane.ERROR_MESSAGE);
                return showDialog();
            } else {
                int prt = -1;
                try {
                    prt = Integer.parseInt(portText.getText().trim());
                } catch (NumberFormatException nfe) {
                    dispose();
                    JOptionPane.showMessageDialog(parentComponent, "Port must be an integer!", "Error!", JOptionPane.ERROR_MESSAGE);
                    return showDialog();
                }
                if (prt < 0 || prt > 65535) {
                    dispose();
                    JOptionPane.showMessageDialog(parentComponent, "Port value must be 0..65535!", "Error!", JOptionPane.ERROR_MESSAGE);
                    return showDialog();
                }
                this.port = prt;
                this.host = hostText.getText().trim();
                dispose();
                return true;
            }
        } else
            dispose();
        return false;
//	return res;
    }
}

