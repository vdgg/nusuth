package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ColorCellEditor extends DefaultCellEditor {
    private JButton button = null;

    public ColorCellEditor() {
        super(new JCheckBox());
        this.clickCountToStart = 1;
        this.button = new JButton();
        editorComponent = this.button;
        delegate = new EditorDelegate() {
            public void setValue(Object value) {
                if (value instanceof Color) {
                    Color c = (Color) value;
                    button.setBackground(c);
                } else
                    button.setBackground(Color.white);
            }

            public Object getCellEditorValue() {
                return button.getBackground();
            }
        };
        button.addActionListener(delegate);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Color c = JColorChooser.showDialog(ColorCellEditor.this.button, "Select the color, please", ColorCellEditor.this.button.getBackground());
                if (c != null) button.setBackground(c);
                ColorCellEditor.this.stopCellEditing();
            }
        });
    }

}