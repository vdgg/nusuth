/*
 * @(#)IntervalSettingsDialog.java 1.0 08/01/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.StringTokenizer;

/**
 * Class IntervalSettingsDialog is a dialog for setting control colored line parameters.
 *
 * @version 1.0 08/01/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class IntervalSettingsDialog extends JDialog implements ActionListener {
    ControlColoredLine line;    // line of interaction
    JPanel mainPanel;           // central panel of dialog
    JPanel buttonsPanel;        // buttons panel of dialog

    // radio buttons:
    JRadioButton equalIntervals = new JRadioButton("Equal intervals", true);
    JRadioButton diffIntervals = new JRadioButton("Different intervals", false);

    JRadioButton prevColors = new JRadioButton("Preceding colors", true);
    JRadioButton newColors = new JRadioButton("New colors", false);

    // labels:
    JLabel intCntLabel = new JLabel("Interval count:");
    JLabel edgesLabel = new JLabel("Intervals edges:");
    JLabel exam = new JLabel("For example:  10, 25, 400, 567, ...");
    JLabel fromLabel = new JLabel("Conversion from");
    JLabel toLabel = new JLabel(" to ");

    // number changers:
    NumberChanger maxValueChanger = new NumberChanger(1, 0, 10000);
    NumberChanger intCntChanger = new NumberChanger(1, 1, 30);

    // text field for edges input:
    JTextField edges = new JTextField(20);

    // color buttons for new colors:
    ColorButton from, to;

    /**
     * Constructs a new interval settings dialog with the specified owner
     * and colored line.
     * Inits all components.
     *
     * @param   owner   the parent frame.
     * @param   line    the colored line for interactions.
     */
    public IntervalSettingsDialog(Frame owner, ControlColoredLine line) {
        super(owner, "Interval settings", true);    // true - modal
        this.line = line;
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(getMainPanel(), BorderLayout.CENTER);
        getContentPane().add(getButtons(), BorderLayout.SOUTH);
        pack();
    }

    // gets the main panel
    private JPanel getMainPanel() {
        if (mainPanel == null) {
            mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(Box.createRigidArea(new Dimension(1, 5)));
            mainPanel.add(getMaxValuePanel());
            mainPanel.add(Box.createRigidArea(new Dimension(1, 5)));
            mainPanel.add(getIntervalPanel());
            mainPanel.add(Box.createRigidArea(new Dimension(1, 5)));
            mainPanel.add(getColorPanel());
        }
        return mainPanel;
    }

    // inits max value panel
    private JPanel getMaxValuePanel() {
        JPanel maxValuePanel = new JPanel(new GridBagLayout());
        maxValuePanel.setBorder(new TitledBorder("Maximum value"));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(2, 10, 2, 0);
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.anchor = GridBagConstraints.CENTER;
        maxValuePanel.add(new JLabel("Input the max value, please"), c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        maxValuePanel.add(maxValueChanger, c);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        JLabel l = new JLabel("All values more than max will have equal colors");
        l.setForeground(Color.darkGray);
        Font f = l.getFont();
        l.setFont(f.deriveFont(f.getSize() - 2));
        maxValuePanel.add(l, c);

        return maxValuePanel;
    }

    // inits the interval panel
    private JPanel getIntervalPanel() {
        JPanel intervalPanel = new JPanel(new GridBagLayout());
        intervalPanel.setBorder(new TitledBorder("Intervals"));

        ButtonGroup group = new ButtonGroup();

        GridBagConstraints c = new GridBagConstraints();
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(2, 5, 2, 0);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        intervalPanel.add(equalIntervals, c);

        equalIntervals.addActionListener(this);
        group.add(equalIntervals);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(2, 20, 2, 0);
        c.gridwidth = GridBagConstraints.RELATIVE;
        intervalPanel.add(intCntLabel, c);

        c.insets = new Insets(2, 10, 2, 0);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        intervalPanel.add(intCntChanger, c);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(2, 5, 2, 0);
        c.anchor = GridBagConstraints.WEST;
        intervalPanel.add(diffIntervals, c);

        diffIntervals.addActionListener(this);
        group.add(diffIntervals);

        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(2, 20, 2, 0);
        c.gridwidth = GridBagConstraints.RELATIVE;
        intervalPanel.add(edgesLabel, c);

        c.insets = new Insets(2, 10, 2, 0);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        intervalPanel.add(edges, c);

        exam.setForeground(Color.darkGray);
        Font fe = exam.getFont();
        exam.setFont(fe.deriveFont(fe.getSize() - 2));
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        intervalPanel.add(exam, c);
        return intervalPanel;
    }

    // inits the color panel
    private JPanel getColorPanel() {
        JPanel colorPanel = new JPanel(new GridBagLayout());
        colorPanel.setBorder(new TitledBorder("Colors"));

        ButtonGroup group = new ButtonGroup();

        GridBagConstraints c = new GridBagConstraints();
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(2, 5, 2, 0);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        colorPanel.add(prevColors, c); // prevColors - radio button
        prevColors.addActionListener(this);
        group.add(prevColors);

        colorPanel.add(newColors, c); // newColors - radio button
        newColors.addActionListener(this);
        group.add(newColors);

        c.anchor = GridBagConstraints.CENTER;
        JPanel conversionPanel = new JPanel(new GridBagLayout());
        c.gridwidth = 1;
        c.insets = new Insets(0, 5, 0, 5);
        conversionPanel.add(fromLabel, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        from = new ColorButton(Color.white);
        conversionPanel.add(from, c);

        c.gridwidth = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        conversionPanel.add(toLabel, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        to = new ColorButton(Color.red);
        conversionPanel.add(to, c);

        c.insets = new Insets(0, 0, 0, 0);
        colorPanel.add(conversionPanel, c);

        return colorPanel;
    }

    // gets the buttons panel
    private JPanel getButtons() {
        if (buttonsPanel == null) {
            buttonsPanel = new JPanel(new FlowLayout());
            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        updateLine();
                        dispose();
                    } catch (ArrayIndexOutOfBoundsException exc) {
                        JOptionPane.showMessageDialog(IntervalSettingsDialog.this, exc.getMessage());
                    }
                }
            });
            buttonsPanel.add(okButton);
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
            buttonsPanel.add(cancelButton);
        }
        return buttonsPanel;
    }

    // enables or disables equal interval components
    private void setEqualsIntervalEnabled(boolean b) {
        intCntChanger.setEnabled(b);
        intCntLabel.setEnabled(b);
    }

    // enables or disables different interval components
    private void setDiffIntervalEnabled(boolean b) {
        edges.setEnabled(b);
        edgesLabel.setEnabled(b);
        exam.setEnabled(b);
    }

    // enables or disables new colors components
    private void setNewColorsEnabled(boolean b) {
        fromLabel.setEnabled(b);
        from.setEnabled(b);
        toLabel.setEnabled(b);
        to.setEnabled(b);
    }

    /**
     * Sets the fields values from line.
     */
    private void updateControl() {
        int intCnt = line.edges.length;  // intCnt != 0 !!!
        int maxValue = line.edges[intCnt - 1];
        intCntChanger.setValue("" + intCnt);
        maxValueChanger.setValue("" + maxValue);
        String intervals = "";
        boolean equals = true;
        int lastW = line.widths[intCnt - 1];
        for (int i = 0; i < intCnt - 1; i++) {
            intervals += (i == 0) ? "" + line.edges[i] : ", " + line.edges[i];
            if (equals && (line.widths[i] != lastW)) {
                equals = false;
            }
        }
        edges.setText(intervals);
        if (equals) {
            // equals intervals
            equalIntervals.setSelected(true);
            setEqualsIntervalEnabled(true);
            setDiffIntervalEnabled(false);
        } else {
            diffIntervals.setSelected(true);
            setEqualsIntervalEnabled(false);
            setDiffIntervalEnabled(true);
        }
        prevColors.setSelected(true);
        setNewColorsEnabled(false);
    }

    /**
     * Updates the line by all dialog fields.
     * Calls the line.setEdges method.
     */
    private void updateLine() throws ArrayIndexOutOfBoundsException {
        int[] edgesInt;
        int maxValue = 100;
        try {
            maxValue = Integer.parseInt(maxValueChanger.getValue());
        } catch (NumberFormatException e) {
        }
        if (equalIntervals.isSelected()) {
            int cnt = 1;
            try {
                cnt = Integer.parseInt(intCntChanger.getValue());
            } catch (NumberFormatException e) {
            }
            edgesInt = new int[cnt];
            for (int i = 0; i < cnt; i++) {
                edgesInt[i] = (i + 1) * maxValue / cnt;
            }
        } else {
            String intervals = edges.getText().trim();

            // cut 0 and maxValue if necessary
            if (intervals.startsWith("0")) {
                intervals = intervals.substring(1).trim();
                if (intervals.startsWith(","))
                    intervals = intervals.substring(1).trim();
            }
            String smax = "" + maxValue;
            if (intervals.endsWith(smax)) {
                int maxVLen = ("" + maxValue).length();
                intervals = intervals.substring(0, intervals.length() - maxVLen).trim();
                if (intervals.endsWith(","))
                    intervals = intervals.substring(0, intervals.length() - 1).trim();
            }

            // parsing
            StringTokenizer st = new StringTokenizer(intervals, ",");
            int cnt = st.countTokens();
            edgesInt = new int[cnt + 1];
            int prevInt = 0;
            int counter = 0;
            while (st.hasMoreTokens()) {
                int nextInt = prevInt + 1;
                String next = st.nextToken().trim();
                try {
                    nextInt = Integer.parseInt(next);
                    if (nextInt <= prevInt) nextInt = prevInt + 1;
                    if (nextInt > maxValue)
                        throw new ArrayIndexOutOfBoundsException("Check egdes and max value !!!");
                } catch (NumberFormatException e) {
                }
                edgesInt[counter++] = nextInt;
                prevInt = nextInt;
            }
            edgesInt[counter] = maxValue;
        }
        if (prevColors.isSelected()) {
            line.setEdges(edgesInt);
        } else {
            line.setEdges(edgesInt, from.getBackground(), to.getBackground());
        }
    }

    /**
     * Updates all fields before showing the dialog.
     */
    public void show() {
        updateControl();
        super.show();
    }

    /**
     * Method from interface ActionListener.
     * Invoked when radio button was pressed.
     *
     * @param   e   the action event.
     */
    public void actionPerformed(ActionEvent e) {
        JRadioButton b = (JRadioButton) e.getSource();
        if (b == equalIntervals) {
            setEqualsIntervalEnabled(true);
            setDiffIntervalEnabled(false);
        } else if (b == diffIntervals) {
            setEqualsIntervalEnabled(false);
            setDiffIntervalEnabled(true);
        } else if (b == prevColors) {
            setNewColorsEnabled(false);
        } else if (b == newColors) {
            setNewColorsEnabled(true);
        }
    }


    /**
     * ColorButton class.
     * It's the button with the specified action listener.
     * It show the color chooser when button is pressed.
     * Overrides th setEnabled method - sets the gray background when button is disabled.
     */
    private class ColorButton extends JButton {
        Color color;    // stores the color - used when button is enabled

        /**
         * Constructs a new color button with the specified color.
         *
         * @param   color   the color of this button
         */
        public ColorButton(Color color) {
            super("");
            addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JButton b = (JButton) e.getSource();
                    Color oldColor = b.getBackground();
                    Color c = JColorChooser.showDialog(b, "Select the color, please", oldColor);
                    if (c != null) {
                        b.setBackground(c);
                    }
                }
            });
            setBackground(color);
        }

        /**
         * Overrides this method.
         * Remembers the specified color after setting background.
         *
         * @param   bg   a new color
         */
        public void setBackground(Color bg) {
            super.setBackground(bg);
            this.color = bg;
        }

        /**
         * Overrides this method.
         * Sets the gray color when button is disabled & the saving color
         * when button is enabled.
         *
         * @param   b   enable or disable button
         */
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            if (!b)
                super.setBackground(Color.gray);
            else
                super.setBackground(color);
        }
    }
}
