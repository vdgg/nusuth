/*
 * @(#)TextAreaElementRenderer.java 1.0 08/22/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

/**
 * Class TextAreaElementRenderer.
 *
 * @version 1.0 08/22/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TextAreaElementRenderer implements ElementRenderer {
    private AdvTextArea textArea;
    private JScrollPane pane;
    private ActionListener actionListener;


    public TextAreaElementRenderer() {
        this(5);
    }

    public TextAreaElementRenderer(int rows) {
        //        textArea = new JTextArea(5, 20);
        textArea = new AdvTextArea();
        textArea.setRows(rows);
        pane = new JScrollPane(textArea);
        Dimension d = new Dimension(10,
                textArea.getRows() * textArea.getAdvRowHeight() + 5);
        pane.setPreferredSize(d);
        pane.setMinimumSize(d);
        pane.setMaximumSize(d);
    }

    public void setValue(String value) {
        textArea.setText(value);
    }

    public String getValue() {
        return textArea.getText();
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
        return pane;
    }

    public boolean takesAllPlace() {
        return true;
    }

    public void addActionListener(ActionListener l) {
        actionListener = AWTEventMulticaster.add(actionListener, l);
        textArea.getDocument().addDocumentListener(new DocumentListener() {
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
            actionListener.actionPerformed(
                    new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
        }
    }


    private class AdvTextArea extends JTextArea {

        public AdvTextArea() {
            super();
            setLineWrap(true);
            setWrapStyleWord(false);
        }

        int getAdvRowHeight() {
            return super.getRowHeight();
        }

        public Dimension getPreferredSize() {
            Dimension superPref = super.getPreferredSize();
            superPref.width = (superPref.width > 100) ? 100 : superPref.width;
            return superPref;
        }
    }

}
