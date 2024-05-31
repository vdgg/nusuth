/*
 * @(#)ElementPanel.java 1.0 09/04/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Class ElementPanel is the panel with a new method checkEditing.
 * It's used in time of tab changing in the web element panel.
 *
 * @version 1.0 09/04/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ElementPanel extends JPanel {

    /**
     * Constructs a new element panel without params.
     */
    public ElementPanel() {
        super();
    }

    /**
     * Constructs a new element panel with the specified layout manager.
     *
     * @param layout    the specified layout manager.
     */
    public ElementPanel(LayoutManager layout) {
        super(layout);
    }

    /**
     * Checks if this panel is editing.
     */
    public void checkEditing() {
    }
}
