package com.azoft.nusuth.gui;

import javax.swing.*;
import java.text.*;
import java.util.Date;
import java.awt.event.*;
import java.awt.*;
import javax.swing.event.*;
import javax.swing.border.*;

public class ComboStatusLine extends JPanel implements StatusLineWriter {
    private static int maxRow;
    private static JPopupMenu popup = new JPopupMenu();
    private JPanel doublePanel = new JPanel();
    private JLabel label = new JLabel(" ");
    private JLabel doubleLabel = new JLabel(" ");
    private java.text.SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm");
    private Date date = new Date();
    private static String empty = " ";
    private static Insets INSETS = new Insets(2, 20, 2, 2);

    public ComboStatusLine(int n) {
        super();
        maxRow = n + 1;
        java.net.URL url = getClass().getClassLoader().getResource("com/azoft/nusuth/gui/up.jpg");
        ImageIcon ii = new ImageIcon(url);
        createPanel(this, label, ii);
        createPanel(doublePanel, doubleLabel, ii);
        addMouseListenerToPanel(this, new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                showPopup();
            }
        });
        addMouseListenerToPanel(doublePanel, new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                popup.setVisible(false);
            }
        });
        EtchedBorder border = new EtchedBorder() {
            public Insets getBorderInsets(Component comp) {
                return ComboStatusLine.INSETS;
            }
        };
        setBorder(border);
        doublePanel.setBorder(border);
        popup.add(doublePanel);
        SwingUtilities.updateComponentTreeUI(doublePanel);
        popup.setBorder(new LineBorder(Color.darkGray, 1));
    }

    private void createPanel(JPanel panel, JComponent l, ImageIcon i) {
        panel.setLayout(new BorderLayout(0, 0));
        panel.add("West", l);
        JButton up = new JButton("", i);
        up.setBorderPainted(false);
        up.setMargin(new Insets(0, 0, 0, 0));
        panel.add("East", up);
    }

    public void setStatusString(String status) {
        clearStatusString();
        date.setTime(System.currentTimeMillis());
        setButtonsText(dateFormat.format(date) + " - " + status);
    }

    public void clearStatusString() {
        if (label.getText().equals(empty)) return;
        popup.insert(new JMenuItem(label.getText()), popup.getComponentCount() - 1);
        if (popup.getComponentCount() > maxRow) popup.remove(0);
        setButtonsText(empty);
    }

    private void setButtonsText(String text) {
        label.setText(text);
        doubleLabel.setText(text);
    }

    public void showPopup() {
        int width = getSize().width;
        Dimension dd = new Dimension(width, getPreferredSize().height);
        doublePanel.setSize(dd);
        doublePanel.setMinimumSize(dd);
        doublePanel.setMaximumSize(dd);
        doublePanel.setPreferredSize(dd);

        popup.setPreferredSize(null);
        int height = popup.getPreferredSize().height;
        Dimension d = new Dimension(width, height);
        popup.setPreferredSize(d);
        Insets ins = popup.getInsets();
        popup.show(this, -ins.left, -height + getSize().height + ins.bottom);
    }

    public void updateUI() {
        super.updateUI();
        if (popup != null)
            SwingUtilities.updateComponentTreeUI(popup);
    }

    private void addMouseListenerToPanel(JPanel p, MouseListener l) {
        p.addMouseListener(l);
        int cnt = p.getComponentCount();
        for (int i = 0; i < cnt; i++) {
            p.getComponent(i).addMouseListener(l);
        }
    }

    public static int getRowCount() {
        return maxRow;
    }

    public static void setMaxRowCount(int cnt) {
        maxRow = cnt + 1;
        recount();
    }

    private static void recount() {
        while (popup.getComponentCount() > maxRow) popup.remove(0);
    }

}

