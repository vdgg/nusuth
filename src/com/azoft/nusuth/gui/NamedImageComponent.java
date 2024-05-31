/*
 * @(#)NamedImageComponent.java 1.0 07/28/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Class NamedImageComponent include an image component and label with name.
 *
 * @version 1.0 07/28/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class NamedImageComponent extends JPanel {
    String systemId;    // systemId of the component
    String type;        // type of the component
    JLabel nameLabel;   // stores the name of the component

    /**
     * Constructs a new named image component with the specified type,
     * systemId, name and image. If paintText is true this component has text label.
     *
     * @param   type        the type of image component.
     * @param   systemId    the systemId of image component.
     * @param   name        the name of image component.
     * @param   image       the image of image component.
     * @param   paintText   the parameter to specify weither paint the text or not.
     */
    public NamedImageComponent(String type, String systemId, String name, Image image, boolean paintText) {
        super();
        this.type = type;
        this.systemId = systemId;
        this.nameLabel = new JLabel(name, JLabel.CENTER);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.insets = new Insets(0, 0, 0, 0);
        add(new ImageComponent(image), c);
        if (paintText) {
            add(nameLabel, c);
        }
    }

    /**
     * Constructs a new named image component with the specified type,
     * systemId, name and image.
     *
     * @param   type        the type of image component.
     * @param   systemId    the systemId of image component.
     * @param   name        the name of image component.
     * @param   image       the image of image component.
     */
    public NamedImageComponent(String type, String systemId, String name, Image image) {
        this(type, systemId, name, image, true);
    }

    /**
     * Constructs a new named image component with the specified type,
     * systemId and image.
     *
     * @param   type        the type of image component.
     * @param   systemId    the systemId of image component.
     * @param   image       the image of image component.
     */
    public NamedImageComponent(String type, String systemId, Image image) {
        this(type, systemId, systemId, image);
    }

    /**
     * Gets the systemId of this component.
     *
     * @return  systemId    the systemId of this component.
     * @see #setSystemId(String)
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Sets the specified systemId to this component.
     *
     * @param   systemId    the systemId of this component.
     * @see #getSystemId()
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Gets the name of this component.
     *
     * @return  the name of this component.
     * @see #setName(String)
     */
    public String getName() {
        return nameLabel.getText();
    }

    /**
     * Sets the specified name to this component.
     *
     * @param name    the name of this component.
     * @see #getName()
     */
    public void setName(String name) {
        this.nameLabel.setText(name);
        invalidate();
        setSize(getPreferredSize());
    }

    /**
     * Gets the type of this component.
     *
     * @return  the type of this component.
     * @see #setType(String)
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the specified type to this component.
     *
     * @param type    the type of this component.
     * @see #getType()
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Adds the text component or remove it if necessary.
     *
     * @param   b   true to paint the text, false to not paint.
     */
    public void setPaintText(boolean b) {
        if (b && getComponentCount() == 1) {
            // has only image component
            GridBagConstraints c = new GridBagConstraints();
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            add(nameLabel, c);
            invalidate();
            setSize(getPreferredSize());
        } else if (!b && getComponentCount() == 2) {
            // has text component too
            Component c = getComponent(1);
            if (c instanceof JLabel) {
                remove(c);
                invalidate();
                setSize(getPreferredSize());
            }
        }
    }
}
