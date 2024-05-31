package com.azoft.nusuth.help;

import java.awt.*;
import java.awt.event.*;
import javax.help.*;
import javax.swing.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class JHLauncher {

    private static JFrame frame;
    private static JHelp jh = null;
    HelpSet hs = null;

    private static String hsName = "Help.hs";


    public JHLauncher() {
        ClassLoader loader = this.getClass().getClassLoader();
        initialize(hsName, loader);
        if (hs == null || jh == null)
            return;
        createFrame();
    }


    protected void initialize(String name, ClassLoader loader) {
        URL url = HelpSet.findHelpSet(loader, name, "", Locale.getDefault());
        if (url == null) {
            url = HelpSet.findHelpSet(loader, name, ".hs", Locale.getDefault());
            if (url == null) {
                // could not find it!
                JOptionPane.showMessageDialog(null, "HelpSet not found", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        initialize(url, loader);
    }


    protected void initialize(URL url, ClassLoader loader) {
        try {
            hs = new HelpSet(loader, url);
        } catch (Exception ee) {
            JOptionPane.showMessageDialog(null, "Help not found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        jh = new JHelp(hs);
    }


    protected JFrame createFrame() {
        WindowListener closer = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                getFrame().setVisible(false);
            }

            public void windowClosed(WindowEvent e) {
                getFrame().setVisible(false);
            }
        };
        frame = new JFrame();
        frame.setForeground(Color.black);
        frame.setBackground(Color.lightGray);
        frame.addWindowListener(closer);
        frame.getContentPane().add(jh); // the JH panel

        TextHelpModel m = jh.getModel();
        HelpSet hs = m.getHelpSet();
        String hsTitle = hs.getTitle();
        if (hsTitle == null || hsTitle.equals("")) {
            frame.setTitle("Unnamed HelpSet"); // maybe based on HS?
        } else {
            frame.setTitle(hsTitle);
        }
        return frame;
    }


    public void setMenuBar(JMenuBar bar) {
        frame.setJMenuBar(bar);
    }


    public Frame getFrame() {
        return frame;
    }


    public void launch() {
        if (frame == null)
            return;
        frame.setVisible(true);
    }


    public void updateUI() {
        if (jh != null) {
            //            jh.updateUI();
            jh.getContentViewer().updateUI();
            Enumeration e = jh.getHelpNavigators();
            while (e.hasMoreElements()) {
                ((JHelpNavigator) e.nextElement()).updateUI();
            }
        }
    }
}