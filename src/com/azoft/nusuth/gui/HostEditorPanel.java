/*
 * @(#)HostEditorPanel.java 1.0 09/03/2001 Sep 26, 2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Enumeration;

/**
 * Class HostEditorPanel.
 *
 * @version 1.0 09/03/2001 Sep 26, 2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class HostEditorPanel extends DefaultEditorPanel {

    /**
     * The main component.
     */
    protected JComponent mainComponent;


    /**
     * Constructs a new host editor panel.
     */
    public HostEditorPanel() {
        super(BasicPanel.SHOST);
    }


    /**
     * Creates a new panel for this element panel.
     * It is the simple panel only in this class.
     */
    public JComponent createPanel() {
        if (mainComponent == null) {
            mainComponent = new JScrollPane(getSimplePanel());
        }
        return mainComponent;
    }

    /**
     * Gets the simple panel - the panel with simple elements.
     *
     * @return  the simple panel
     */
    public JPanel getSimplePanel() {
        if (simplePanel == null) {
            simplePanel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = 1;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.insets = new Insets(5, 5, 5, 5);
            c.fill = GridBagConstraints.HORIZONTAL;
            simplePanel.add(getSimpleEls(), c);
            c.weighty = 1.0;
            c.insets = new Insets(0, 0, 0, 0);
            c.fill = GridBagConstraints.BOTH;
            simplePanel.add(new JPanel(), c);
        }
        return simplePanel;
    }

    /**
     * Gets the simple els panel.
     *
     * @return  the simple panel
     */
    public JPanel getSimpleEls() {
        JPanel res = new JPanel(new GridBagLayout());
        res.setBorder(new TitledBorder("Main Settings"));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = -1;
        if (webElement != null) {
            Enumeration e = webElement.getSimpleChildrenNames();
            c.insets = new Insets(10, 5, 10, 5);
            if (e.hasMoreElements()) {
                addChildComponents(res, (String) e.nextElement(), c);
            }
            c.insets = new Insets(0, 5, 10, 5);
            while (e.hasMoreElements()) {
                addChildComponents(res, (String) e.nextElement(), c);
            }
        }
        return res;
    }
}
