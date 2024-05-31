/*
 * @(#)CompositeCellEditor.java 1.0 04/06/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.CellEditorListener;
import java.util.EventObject;
import java.awt.event.*;
import java.awt.*;

/**
 * Class CompositeCellEditor is the cell editor for table in table panel factory.
 * It can consist of check box / radio button, text field / combobox, button '...'.
 *
 * @version 1.0 04/06/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class CompositeCellEditor extends DefaultCellEditor {

    /**
     * the combobox; it is null, if cell editor don't contain it.
     */
    private JComboBox comboBox = null;

    /**
     * the text field; it is null, if cell editor don't contain it.
     * KeyTextField is the JTextField with the callProcessKeyBinding function
     */
    private KeyTextField textField = null;

    /**
     * the check box; it is null, if cell editor don't contain it.
     */
    private JCheckBox checkBox = null;

    /**
     * Defines weither this editor looks like one checkBox.
     * The composite object in case of true will have values:
     * selected, "true"/"false"
     */
    private boolean booleanValue = false;

    /**
     * the radio button; it is null, if cell editor don't contain it.
     */
    private JRadioButton radio = null;

    /**
     * the button '...'; it is null, if cell editor don't contain it.
     */
    private JButton button = null;

    /**
     * the label; it is null, if cell editor don't contain it.
     */
    private JLabel label = null;

    /**
     * If the label exists & visibleLabel is true -
     * the label will be added to the main panel.
     * Else overwise.
     */
    private boolean visibleLabel = true;

    /**
     * the table renderer; it may be an constructor argument.
     * Defines the button action.
     */
    private TableRenderer tableRenderer = null;

    /**
     * the child factory; it may be an constructor argument.
     * Defines the button action.
     */
    private CompositeElementPanelFactory childFactory = null;

    /**
     * The password element renderer.
     * Defines the nuton action & sets the necessary value to this editor.
     */
    private PasswordElementRenderer passwordRenderer = null;

    /**
     * column index; Used when the panel processes key binding.
     * If panel contains the radio button & radio isn't selected,
     * the processKeyBinding first clicks the radio
     * and then calls the processKeyBinding again. Before repeated call
     * it calls editCellAt(row, column) in table.
     * (Click on radio defines the row, column, table)
     */
    private int column = -1;

    /**
     * row index
     */
    private int row = -1;

    /**
     * table
     */
    private JTable table = null;


    /**
     * Constructs a new composite cell editor with the specified comboBox.
     *
     * @param   comboBox    the combobox.
     */
    public CompositeCellEditor(JComboBox comboBox) {
        super(comboBox);
        this.clickCountToStart = 2;
        this.comboBox = comboBox;
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified
     * radio button & comboBox.
     *
     * @param   comboBox    the combobox.
     * @param   radio       the radio button.
     */
    public CompositeCellEditor(JComboBox comboBox, JRadioButton radio) {
        super(comboBox);
        this.clickCountToStart = 2;
        this.comboBox = comboBox;
        this.radio = radio;
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified
     * checkBox & comboBox.
     *
     * @param   comboBox    the combobox.
     * @param   radio       the radio button.
     */
    public CompositeCellEditor(JComboBox comboBox, JCheckBox checkBox) {
        super(comboBox);
        this.clickCountToStart = 2;
        this.comboBox = comboBox;
        this.checkBox = checkBox;
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified textField.
     *
     * @param   textField    the textField.
     */
    public CompositeCellEditor(JTextField textField) {
        super(textField);
        this.clickCountToStart = 2;
        this.textField = new KeyTextField(textField);
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified
     * radio button & textField.
     *
     * @param   textField   the textField.
     * @param   radio       the radio button.
     */
    public CompositeCellEditor(JTextField textField, JRadioButton radio) {
        super(textField);
        this.clickCountToStart = 2;
        this.textField = new KeyTextField(textField);
        this.radio = radio;
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified
     * checkBox & textField.
     *
     * @param   textField   the textField.
     * @param   checkBox    the checkBox.
     */
    public CompositeCellEditor(JTextField textField, JCheckBox checkBox) {
        super(textField);
        this.clickCountToStart = 2;
        this.textField = new KeyTextField(textField);
        this.checkBox = checkBox;
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified checkBox.
     * For simple elements with check boolean element renderers.
     *
     * @param   checkBox    the checkBox.
     */
    public CompositeCellEditor(JCheckBox checkBox) {
        super(checkBox);
        this.clickCountToStart = 2;
        this.checkBox = checkBox;
        this.booleanValue = true;
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified
     * password element renderer.
     * Looks like button
     *
     * @param   renderer    the specified password element renderer.
     */
    public CompositeCellEditor(PasswordElementRenderer renderer) {
        super(new JCheckBox());
        this.clickCountToStart = 2;
        this.passwordRenderer = renderer;
        this.button = (JButton) renderer.getComponent();
        button.setText("...");
        button.setPreferredSize(TablePanelFactory.buttonSize);
        button.setMinimumSize(TablePanelFactory.buttonSize);
        button.setMaximumSize(TablePanelFactory.buttonSize);
        this.label = new JLabel("");
        this.visibleLabel = false;
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified
     * radio button & password element renderer.
     *
     * @param   renderer    the specified password element renderer.
     * @param   radio       the radio button.
     */
    public CompositeCellEditor(PasswordElementRenderer renderer,
                               JRadioButton radio) {
        super(new JCheckBox());
        this.clickCountToStart = 2;
        this.passwordRenderer = renderer;
        this.button = (JButton) renderer.getComponent();
        button.setText("...");
        button.setPreferredSize(TablePanelFactory.buttonSize);
        button.setMinimumSize(TablePanelFactory.buttonSize);
        button.setMaximumSize(TablePanelFactory.buttonSize);
        this.radio = radio;
        this.label = new JLabel("");
        this.visibleLabel = false;
        createPanel();
    }


    /**
     * Constructs a new composite cell editor with the specified tableRenderer.
     * It will look as  label + button "..."
     *
     * @param   tableRenderer   the tableRenderer.
     */
    public CompositeCellEditor(TableRenderer tableRenderer) {
        super(new JCheckBox());
        this.clickCountToStart = 2;
        this.tableRenderer = tableRenderer;
        this.button = getEditorButton(tableRenderer);
        this.label = new JLabel(tableRenderer.getValue());
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified childFactory.
     * ?hilFactory should be the TablePanelFactory.
     * It will look as  label + button "..."
     *
     * @param   childFactory    the table panel factory.
     */
    public CompositeCellEditor(CompositeElementPanelFactory childFactory) {
        super(new JCheckBox());
        this.clickCountToStart = 2;
        this.childFactory = childFactory;
        this.button = getEditorButton(childFactory);
        this.label = new JLabel(childFactory.getDisplay());
        createPanel();
    }

    /**
     * Constructs a new composite cell editor with the specified checkBox & childFactory.
     * It will look as  checkbox + label + button "..."
     *
     * @param   checkBox        the checkBox.
     * @param   childFactory    the table panel factory.
     */
    public CompositeCellEditor(JCheckBox checkBox, CompositeElementPanelFactory childFactory) {
        super(checkBox);
        this.clickCountToStart = 2;
        this.checkBox = checkBox;
        this.childFactory = childFactory;
        this.button = getEditorButton(childFactory);
        if (childFactory instanceof TablePanelFactory)
            this.label = new JLabel(((TablePanelFactory) childFactory).getDisplay());
        else
            this.label = new JLabel(DefaultEditorPanel.getDisplay(childFactory.getType(), childFactory.webElement));
        createPanel();
    }

    /**
     * Creates the panel. It paints its border after the panel painting and
     * it translates the key events to text field.
     * Adds all necessary components.
     * Creates a new editor delegate, working with a composite object.
     * Adds all listeners.
     */
    private void createPanel() {
        GridBagLayout gridbag = new GridBagLayout();
        JPanel panel = new JPanel(gridbag) {
            public void paint(Graphics g) {
                super.paint(g);
                paintBorder(g);
            }

            protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                                                int condition, boolean pressed) {
                if (checkBox != null) {
                    checkBox.doClick();
                } else if (radio != null && !radio.isSelected()) {
                    radio.doClick();
                    // when radio process click - it sets table, row, column
                    if (table != null && row != -1 && column != -1) {
                        table.editCellAt(row, column);
                    }
                    return processKeyBinding(ks, e, condition, pressed);
                }
                if (textField != null) {
                    return textField.callProcessKeyBinding(ks, e, condition, pressed);
                }
                CompositeCellEditor.this.stopCellEditing();
                return false;
            }
        };
        panel.setBorder(new LineBorder(Color.black, 2));
        GridBagConstraints c = new GridBagConstraints();

        c.gridwidth = (button != null) ? 1 :
                (booleanValue) ? GridBagConstraints.REMAINDER
                : GridBagConstraints.RELATIVE;
        c.weightx = 0.0;
        if (checkBox != null) {
            panel.add(checkBox, c);
        } else if (radio != null) {
            panel.add(radio, c);
        }

        c.gridwidth = (button != null)
                ? GridBagConstraints.RELATIVE : GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        if (comboBox != null) {
            panel.add(comboBox, c);
        } else if (textField != null) {
            panel.add(textField, c);
        } else if (label != null) {
            if (visibleLabel) {
                panel.add(label, c);
            } else {
                JPanel p = new JPanel();
                p.setBackground(Color.white);
                panel.add(p, c);
            }
        }

        if (button != null) {
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 0.0;
            c.fill = GridBagConstraints.NONE;
            panel.add(button, c);
        }

        editorComponent = panel;

        delegate = new EditorDelegate() {
            public void setValue(Object value) {
                boolean selected = false;
                Object boxValue = "";
                if (value instanceof CompositeObject) {
                    selected = ((CompositeObject) value).getSelected();
                    boxValue = ((CompositeObject) value).getValue();
                    boxValue = (boxValue == null) ? "" : boxValue;
                }
                if (checkBox != null) {
                    if (booleanValue) {
                        checkBox.setSelected(
                                boxValue.equals("true") || boxValue.equals("yes"));
                    } else {
                        checkBox.setSelected(selected);
                    }
                } else if (radio != null) {
                    radio.setSelected(selected);
                }
                if (checkBox != null || radio != null) {
                    setEnabled(selected);
                }
                setTextValue(boxValue.toString());
            }

            public Object getCellEditorValue() {
                if (tableRenderer != null) {
                    setTextValue(tableRenderer.getValue());
                }
                if (passwordRenderer != null) {
                    setTextValue(passwordRenderer.getValue());
                }
                if (childFactory != null) {
                    setTextValue(childFactory.getDisplay());
                }
                boolean selected = (checkBox != null)
                        ? checkBox.isSelected()
                        : (radio != null) ? radio.isSelected() : true;
                Object value = getTextValue();
                if (booleanValue) {
                    selected = true;
                    value = (checkBox.isSelected()) ? "true" : "false";
                }
                CompositeObject co = new CompositeObject(selected, value);
                return co;
            }

            public boolean shouldSelectCell(EventObject anEvent) {
                return true;
            }
        };
        if (checkBox != null) {
            checkBox.addActionListener(delegate);
        } else if (radio != null) {
            radio.addActionListener(delegate);
        }
        if (comboBox != null) {
            comboBox.addActionListener(delegate);
        } else if (textField != null) {
            textField.addActionListener(delegate);
        }
    }

    /**
     * Sets the all components enabled or disabled.
     *
     * @param   b   defines wheither enabled or disabled components will be
     */
    private void setEnabled(boolean b) {
        if (comboBox != null) {
            comboBox.setEnabled(b);
        }
        if (textField != null) {
            textField.setEnabled(b);
        }
        if (button != null) {
            button.setEnabled(b);
        }
        if (label != null) {
            label.setEnabled(b);
        }
    }

    /**
     * Gets the radio button.
     *
     * @return  the radio button.
     */
    public JRadioButton getRadio() {
        return radio;
    }

    /**
     * Gets the combobox.
     *
     * @return  the combobox.
     */
    public JComboBox getComboBox() {
        return comboBox;
    }

    /**
     * Gets the child factory or null if it not exist.
     *
     * @return  the child factory.
     */
    public CompositeElementPanelFactory getChildFactory() {
        return childFactory;
    }

    /**
     * Gets the button for the child factory showing.
     *
     * @return  the button for the child factory showing.
     */
    public JButton getButton() {
        return button;
    }

    /**
     * Sets the text value to one of existing text component.
     *
     * @param   text    the text value to be set.
     * @see #getTextValue()
     */
    public void setTextValue(String text) {
        if (comboBox != null) {
            comboBox.setSelectedItem(text);
        } else if (textField != null) {
            textField.setText(text);
        } else if (label != null) {
            label.setText(text);
        }
    }

    /**
     * Gets the text value from the one of existing text component.
     *
     * @return  the text value from the one of existing text component.
     * @see #setTextValue(String)
     */
    public Object getTextValue() {
        Object res = "";
        if (comboBox != null) {
            res = (comboBox.getSelectedItem() == null) ? ""
                    : comboBox.getSelectedItem();
        } else if (textField != null) {
            res = textField.getText();
        } else if (label != null) {
            res = label.getText();
        }
        return res;
    }

    /**
     * Gets the button '...' with a necessary action listener.
     * The button action is a dialog with renderer's component showing.
     *
     * @param   renderer    the table renderer, which define the action
     * @return  the button '...'
     */
    private JButton getEditorButton(final TableRenderer renderer) {
        final JButton button = TablePanelFactory.getRendererButton();
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = renderer.getColumnName();
                JComponent component = renderer.getComponent();
                Dimension d = TablePanelFactory.getPrefSize(name);
                if (d != null) {
                    component.setPreferredSize(d);
                }
                Object[] message = new Object[1];
                message[0] = component;
                TablePanelFactory.showDialog(message, name);
                TablePanelFactory.setPrefSize(name, component.getSize());
                CompositeCellEditor.this.stopCellEditing();
            }
        });
        return button;
    }

    /**
     * Gets the button '...' with a necessary action listener.
     * The button action is a dialog with child factory's panel showing.
     *
     * @param   childFactory    the child factory, which define the action
     * @return  the button '...'
     */
    private JButton getEditorButton(final CompositeElementPanelFactory childFactory) {
        final JButton button = TablePanelFactory.getRendererButton();
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String tag = childFactory.getTag();
                JComponent component = childFactory.createPanel();
                Dimension d = TablePanelFactory.getPrefSize(tag);
                if (d != null) {
                    component.setPreferredSize(d);
                }
                Object[] message = new Object[1];
                message[0] = component;
                childFactory.disableActive();
                TablePanelFactory.showDialog(message, tag);
                TablePanelFactory.setPrefSize(tag, component.getSize());
                CompositeCellEditor.this.stopCellEditing();
            }
        });
        return button;
    }

    /**
     * Sets the specified table renderer.
     *
     * @param   tableRenderer   the specified table renderer
     */
    public void setTableRenderer(TableRenderer tableRenderer) {
        this.tableRenderer = tableRenderer;
    }

    /**
     * Override the super function to save the table, row, column & set background.
     */
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected,
                                                 int row, int column) {
        this.table = table;
        this.column = column;
        this.row = row;
        setBackground(table.getBackground());
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    // sets the background to all components.
    private void setBackground(Color c) {
        editorComponent.setBackground(c);
        if (radio != null) {
            radio.setBackground(c);
        }
        if (checkBox != null) {
            checkBox.setBackground(c);
        }
        if (textField != null) {
            textField.setBackground(c);
        }
        if (label != null) {
            label.setBackground(c);
        }
    }

    /**
     * Gets the default value for this cell editor.
     * It text is stored by text field - it will be ""; if
     * combobox contains text - the first item; if
     * label - the label text.
     *
     * @return  the defalut value for this cell editor.
     */
    public String getDefaultValue() {
        if (comboBox != null && comboBox.getItemCount() > 0) {
            return (String) comboBox.getItemAt(0);
        }
        if (label != null) {
            return label.getText();
        }
        return "";
    }


    /**
     * KeyTextField class.
     * The method processKeyBinding is protected in JComponent,
     * because CompositeCellEditor can't call it on JTextField textField.
     * This method can call only subclasses.
     */
    private class KeyTextField extends JTextField {

        /**
         * Constructs a new KeyTextField with the specified textField.
         *
         * @param   tf      the parent textField.
         */
        public KeyTextField(JTextField tf) {
            super();
            setText(tf.getText());
        }

        /**
         * Calls the processKeyBinding method with the specified parameters.
         *
         * @param   ks          the keyStroke.
         * @param   e           the keyEvent.
         * @param   condition   the condition.
         * @param   pressed     pressed or not.
         * @return  the boolean value.
         * @see JComponent#callProcessKeyBinding(KeyStroke, KeyEvent, int, boolean)
         */
        public boolean callProcessKeyBinding(KeyStroke ks, KeyEvent e,
                                             int condition, boolean pressed) {
            return super.processKeyBinding(ks, e, condition, pressed);
        }

    } // end of KeyTextField

} // end of CompositeCellEditor