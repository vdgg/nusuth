/*
 * @(#)ColoredLine.java 1.0 07/25/2001
 */

package com.azoft.nusuth.gui;

import java.awt.geom.Line2D;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Class ColoredLine represents a line segment in (x,&nbsp;y)
 * coordinate space. It connects the centers of two components.
 *
 * @version 1.0 07/25/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class ColoredLine extends Line2D.Double {
    int intensity;      // the intensity of this line
    Color color;        // the color of this line
    Component c1, c2;   // the components of this line

    /**
     * Constructs a colored line with the specified
     * components. // Adds the component adapters to catch component moving.
     *
     * @param   c1   the first component.
     * @param   c2   the second component.
     */
    public ColoredLine(Component c1, Component c2) {
        this(c1, c2, Color.white);
    }

    /**
     * Constructs a colored line with the specified
     * components and color.
     *
     * @param   c1      the first component.
     * @param   c2      the second component.
     * @param   color   the color of this line.
     */
    public ColoredLine(Component c1, Component c2, Color color) {
        super(c1.getLocation().x + (int) c1.getSize().width / 2,
                c1.getLocation().y + (int) c1.getSize().height / 2,
                c2.getLocation().x + (int) c2.getSize().width / 2,
                c2.getLocation().y + (int) c2.getSize().height / 2);
        this.c1 = c1;
        this.c2 = c2;
        setColor(color);
/*
        c1.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
//                synchronized (lock) {
                    checkP1();
//                }
            }
        });
        c2.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
//                synchronized (lock) {
                    checkP2();
//                }
            }
        });
*/
    }

    /**
     * Gets the intensity of this line.
     *
     * @return  the intensity of this line.
     * @see #setIntensity(int)
     */
    public int getIntensity() {
        return intensity;
    }

    /**
     * Sets the specified intensity to this line.
     *
     * @param   intensity   the intensity of this line.
     * @see #getIntensity()
     */
    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    /**
     * Gets the color of this line.
     *
     * @return  the color of this line.
     * @see #setColor(Color)
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the specified color to this line.
     *
     * @param   color   the color of this line.
     * @see #getColor()
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Gets the first component of this line.
     *
     * @return  the first component of this line.
     * @see #setC1(Compponent)
     * @see #getC2()
     */
    public Component getC1() {
        return c1;
    }

    /**
     * Gets the second component of this line.
     *
     * @return  the second component of this line.
     * @see #setC2(Compponent)
     * @see #getC1()
     */
    public Component getC2() {
        return c2;
    }

    /**
     * Sets the specified component as the first component of this line.
     * Checks the first point locations.
     *
     * @param   c1   the first component of this line.
     * @see #getC1()
     */
    public void setC1(Component c1) {
        this.c1 = c1;
        checkP1();
    }

    /**
     * Sets the specified component as the second component of this line.
     * Checks the second point locations.
     *
     * @param   c2   the second component of this line.
     * @see #getC1()
     */
    public void setC2(Component c2) {
        this.c2 = c2;
        checkP2();
    }

    /**
     * Checks the first point locations.
     */
    public void checkP1() {
        setP1To(new Point(c1.getLocation().x + (int) c1.getWidth() / 2, c1.getLocation().y + (int) c1.getHeight() / 2));
    }

    /**
     * Checks the second point locations.
     */
    public void checkP2() {
        setP2To(new Point(c2.getLocation().x + (int) c2.getWidth() / 2, c2.getLocation().y + (int) c2.getHeight() / 2));
    }

    private void setP1To(Point p) {
        setLine(p.x, p.y, x2, y2);
    }

    private void setP2To(Point p) {
        setLine(x1, y1, p.x, p.y);
    }
}
