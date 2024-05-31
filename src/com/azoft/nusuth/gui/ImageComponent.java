/*
 * @(#)ImageComponent.java 1.0 07/26/2001
 */

package com.azoft.nusuth.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;

/**
 * Class ImageComponent.
 *
 * @version 1.0 07/26/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ImageComponent extends JComponent {
    Image image;
    int width, height;

    public ImageComponent(Image image) {
        super();
        this.image = image;
        if (image != null) {
            width = image.getWidth(null);
            height = image.getHeight(null);
        }
    }

    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(image, 0, 0, null);
    }

    public Dimension getSize() {
        return (image != null) ? new Dimension(width, height) : new Dimension(20, 20);
    }

    public Dimension getPreferredSize() {
        return getSize();
    }

    public Dimension getMaximumSize() {
        return getSize();
    }

    public Dimension getMinimumSize() {
        return getSize();
    }
}
