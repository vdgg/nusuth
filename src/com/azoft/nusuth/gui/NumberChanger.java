package com.azoft.nusuth.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.PanelUI;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class NumberChanger extends JPanel {
    private JTextField textField;
    private JScrollBar scrollbar;
    private boolean fromText = false;
    private ActionListener actionListener;
    private String winlf = "Windows";

    public NumberChanger() {
        this(1);
    }

    public NumberChanger(int unitIncrement) {
        this(unitIncrement, 0, 100);
    }

    String prevValue = "";
    boolean twice = false;

    public NumberChanger(int unitIncrement, int minimum, int maximum) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        textField = new JTextField(5) {
            protected Document createDefaultModel() {
                return new PlainDocument() {
                    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                        if (str == null || str.equals("")) return;
                        try {
                            int value = new Integer(str).intValue();
                            super.insertString(offs, str, a);
                        } catch (NumberFormatException ne) {
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                };
            }
        };
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                setValueToScrollbar(textField.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                setValueToScrollbar(textField.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                setValueToScrollbar(textField.getText());
            }
        });
        scrollbar = new JScrollBar(JScrollBar.VERTICAL);
        int th = textField.getPreferredSize().height;
        Dimension d = new Dimension(th, th);
        scrollbar.setPreferredSize(d);
        scrollbar.setMinimumSize(d);
        scrollbar.setMaximumSize(d);
        scrollbar.setUnitIncrement(unitIncrement);
        scrollbar.setMaximum(-minimum);
        scrollbar.setMinimum(-maximum);
        scrollbar.setBlockIncrement(1);
        scrollbar.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (!fromText) {
                    textField.setText("" + (-e.getValue()));
                }
                fromText = false;
            }
        });
        add(textField);
        add(scrollbar);
        if (UIManager.getLookAndFeel().getName().equals(winlf))
            scrollbar.setBorder(new NumberBorder(BevelBorder.LOWERED));
    }

    public void updateUI() {
        if (scrollbar != null) {
            if (UIManager.getLookAndFeel().getName().equals(winlf)) {
                scrollbar.setBorder(new NumberBorder(BevelBorder.LOWERED));
            } else {
                scrollbar.setBorder(new EmptyBorder(0, 0, 0, 0));
            }
        }
        setUI((PanelUI) UIManager.getUI(this));
    }

    public void setValue(String value) {
        textField.setText(value);
    }

    private void setValueToScrollbar(String value) {
        fromText = true;
        try {
            if (value.trim().length() > 9)
                value = value.trim().substring(0, 8);
            scrollbar.setValue(0 - new Integer(value.trim()).intValue());
        } catch (NumberFormatException e) {
            scrollbar.setValue(0);
        }
        fromText = false;
    }

    public String getValue() {
        return textField.getText();
    }

    public void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                fireActionPerformed();
            }

            public void removeUpdate(DocumentEvent e) {
                fireActionPerformed();
            }

            public void changedUpdate(DocumentEvent e) {
                fireActionPerformed();
            }
        });
    }

    public void removeActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.remove(actionListener, l);
    }

    private void fireActionPerformed() {
        if (actionListener != null) {
            actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        textField.setEnabled(enabled);
        scrollbar.setEnabled(enabled);
    }

    /**
     * Overrides the super method to the textField request the focus
     */
    public void requestFocus() {
        textField.requestFocus();
    }

    public class NumberBorder extends BevelBorder {

        public NumberBorder(int type) {
            super(type);
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = 1;
            return insets;
        }

        protected void paintLoweredBevel(Component c, Graphics g, int x, int y,
                                         int width, int height) {

            Color oldColor = g.getColor();
            int h = height;
            int w = width;

            g.translate(x, y);

            g.setColor(Color.black);
            g.drawLine(0, 0, 0, h - 1);
            g.drawLine(1, 0, w - 1, 0);

            g.setColor(getHighlightInnerColor(c));
            g.drawLine(0, h - 1, w - 1, h - 1);
            g.drawLine(w - 1, 0, w - 1, h - 2);

            g.translate(-x, -y);
            g.setColor(oldColor);
        }
    }
}
