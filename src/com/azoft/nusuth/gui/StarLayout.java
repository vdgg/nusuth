/*
 * @(#)StarLayout.java	1.0 07/27/2001
 */

package com.azoft.nusuth.gui;

import java.awt.*;
import java.util.*;

/**
 * A StarLayout object is a layout manager for a container.
 * It puts a center of star in the middle of star space and all spikes - around the center.
 * Then it lays out stars like FlowLayout (in a left-to-right flow).
 * <p>
 * A star layout uses FlowLayout constants - FlowLayout.LEFT, FlowLayout.RIGTH, FlowLayout.CENTER,
 * FlowLayout.LEADING, FlowLayout.TRAILING for align.
 * <p>
 * Each component managed by a star layout is associated with an instance of
 * StarConstraints that specifies a type of component (center or spike) & center component.
 * Here is an example of use star layout:
 * <pre>
 *    Panel p = new Panel();
 *    p.setLayout(new StarLayout());
 *    JLabel center = new JLabel("It's a center");
 *    p.add(center, new StarConstraints(StarConstraints.CENTER, null));
 *    for (int i = 0; i < 6; i++){
 *      p.add(new JLabel("It's a spike "+i), new StarConstraints(StarConstraints.SPIKE, center));
 *    }
 * </pre>
 * <p>
 * @version 	1.0 07/27/2001
 * @author 	    tanya
 * @since Nusuth1.0
 * @see         java.awt.Container#add(String, Component)
 * @see         java.awt.ComponentOrientation
 */
public class StarLayout implements LayoutManager2,
        java.io.Serializable {

    /**
     * <code>align</code> is the property that determines
     * how each row distributes empty space.
     * It can be one of the following three values :
     * <code>LEFT</code>
     * <code>RIGHT</code>
     * <code>CENTER</code>
     *
     * @serial
     * @see #getAlignment
     * @see #setAlignment
     */
    int align;

    /**
     * <code>newAlign</code> is the property that determines
     * how each row distributes empty space for the Java 2 platform, v1.2 and greater.
     * It can be one of the following three values :
     * <code>LEFT</code>
     * <code>RIGHT</code>
     * <code>CENTER</code>
     *
     * @serial
     * @since 1.2
     * @see #getAlignment
     * @see #setAlignment
     */
    int newAlign;       // This is the one we actually use

    /**
     * Constructs a star layout with the horizontal gaps
     * between stars.
     * The horizontal gap is specified by <code>hgap</code>.
     *
     * @see getHgap()
     * @see setHgap()
     *
     * @serial
     */
    int hgap;

    /**
     * Constructs a star layout with the vertical gaps
     * between stars.
     * The vertical gap is specified by <code>vgap</code>.
     *
     * @see getVgap()
     * @see setVgap()
     * @serial
     */
    int vgap;

    /**
     * Hashtable to specify stars. Keys - centers of stars, values - stars.
     * @serial
     * @see #addLayoutComponent
     * @see #removeLayoutComponent
     */
    Hashtable stars = new Hashtable();

    /**
     * Constructs a new star layout with the center alignment and a
     * default 5-unit horizontal and vertical gap.
     */
    public StarLayout() {
        this(FlowLayout.CENTER, 5, 5);
    }

    /**
     * Constructs a new Star Layout with the specified alignment and a
     * default 5-unit horizontal and vertical gap.
     * The value of the alignment argument must be one of
     * <code>FlowLayout.LEFT</code>, <code>FlowLayout.RIGHT</code>,
     * or <code>FlowLayout.CENTER</code>.
     * @param align the alignment value
     */
    public StarLayout(int align) {
        this(align, 5, 5);
    }

    /**
     * Constructs a star layout with the specified gaps
     * between components.
     * The horizontal gap is specified by <code>hgap</code>
     * and the vertical gap is specified by <code>vgap</code>.
     * @param   hgap   the horizontal gap.
     * @param   vgap   the vertical gap.
     */
    public StarLayout(int align, int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
        setAlignment(align);
    }

    /**
     * Gets the alignment for this layout.
     * Possible values are <code>FlowLayout.LEFT</code>,
     * <code>FlowLayout.RIGHT</code>, or <code>FlowLayout.CENTER</code>.
     * @return     the alignment value for this layout.
     * @see        java.awt.FlowLayout#setAlignment
     */
    public int getAlignment() {
        return newAlign;
    }

    /**
     * Sets the alignment for this layout.
     * Possible values are <code>FlowLayout.LEFT</code>,
     * <code>FlowLayout.RIGHT</code>, and <code>FlowLayout.CENTER</code>.
     * @param      align the alignment value.
     * @see        #getAlignment()
     */
    public void setAlignment(int align) {
        this.newAlign = align;
        switch (align) {
            case FlowLayout.LEADING:
                this.align = FlowLayout.LEFT;
                break;
            case FlowLayout.TRAILING:
                this.align = FlowLayout.RIGHT;
                break;
            default:
                this.align = align;
                break;
        }
    }

    /**
     * Returns the horizontal gap between components.
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components.
     * @param hgap the horizontal gap between components
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Returns the vertical gap between components.
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components.
     * @param vgap the vertical gap between components
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.  For star layouts, the constraint must be
     * null or StarConstraints instance.
     * <p>
     * @param   comp         the component to be added.
     * @param   constraints  an object that specifies how
     *                       the component is added to the layout.
     * @see     java.awt.Container#add(java.awt.Component, java.lang.Object)
     * @exception   IllegalArgumentException  if the constraint object is not
     *                 a StarConstraints.
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        synchronized (comp.getTreeLock()) {
            comp.setSize(comp.getPreferredSize());
            if (comp == null) return;
            if (constraints instanceof StarConstraints) {
                if (((StarConstraints) constraints).type == StarConstraints.CENTER) {
                    if (stars.get(comp) == null)
                        stars.put(comp, new Star(comp));
                    //                stars.addElement(new Star(comp));
                } else if (((StarConstraints) constraints).type == StarConstraints.SPIKE) {
                    Component center = ((StarConstraints) constraints).center;
                    if (center != null) {
                        Star star = (Star) stars.get(center);
                        if (star == null) {
                            star = new Star(center);
                            stars.put(center, star);
                        }
                        star.addSpike(comp);
/*
                    Enumeration e = stars.elements();
                    while(e.hasMoreElements()){
                        Star star = (Star)e.nextElement();
                        if (star.getCenter() == center){
                            star.addSpike(comp);
                            return;
                        }
                    } */
                    }
                }
            } else if (constraints != null) {
                throw new IllegalArgumentException("cannot add to layout: constraint must be a StarConstraints");
            }
        }
    }

    /**
     * @deprecated  replaced by <code>addLayoutComponent(Component, Object)</code>.
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from this star layout. This
     * method is called when a container calls its <code>remove</code> or
     * <code>removeAll</code> methods. Most applications do not call this
     * method directly.
     * @param   comp   the component to be removed.
     * @see     java.awt.Container#remove(java.awt.Component)
     * @see     java.awt.Container#removeAll()
     */
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            if (stars.containsKey(comp)) {
                stars.remove(comp);
            } else {
                Enumeration e = stars.elements();
                while (e.hasMoreElements()) {
                    Star star = (Star) e.nextElement();
                    if (star.containsSpike(comp)) {
                        star.removeSpike(comp);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Determines the minimum size of the <code>target</code> container
     * using this layout manager.
     * <p>
     * This method is called when a container calls its
     * <code>getMinimumSize</code> method. Most applications do not call
     * this method directly.
     * @param   target   the container in which to do the layout.
     * @return  the minimum dimensions needed to lay out the subcomponents
     *          of the specified container.
     * @see     java.awt.Container
     * @see     java.awt.BorderLayout#preferredLayoutSize
     * @see     java.awt.Container#getMinimumSize()
     */
    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);

            Enumeration e = stars.elements();
            boolean firstStar = true;
            while (e.hasMoreElements()) {
                Star star = (Star) e.nextElement();
                Dimension d = star.getMinimumSize();
                dim.height = Math.max(dim.height, d.height);
                if (firstStar) {
                    dim.width += hgap;
                    firstStar = false;
                }
                dim.width += d.width;
            }
            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    /**
     * Determines the preferred size of the <code>target</code>
     * container using this layout manager, based on the components
     * in the container.
     * <p>
     * Most applications do not call this method directly. This method
     * is called when a container calls its <code>getPreferredSize</code>
     * method.
     * @param   target   the container in which to do the layout.
     * @return  the preferred dimensions to lay out the subcomponents
     *          of the specified container.
     * @see     java.awt.Container
     * @see     java.awt.BorderLayout#minimumLayoutSize
     * @see     java.awt.Container#getPreferredSize()
     */
    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);

            Enumeration e = stars.elements();
            boolean firstStar = true;
            while (e.hasMoreElements()) {
                Star star = (Star) e.nextElement();
                Dimension d = star.getPreferredSize();
                dim.height = Math.max(dim.height, d.height);
                if (firstStar)
                    firstStar = false;
                else
                    dim.width += hgap;
                dim.width += d.width;
            }
            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    /**
     * Returns the maximum dimensions for this layout given the components
     * in the specified target container.
     * @param target the component which needs to be laid out
     * @see Container
     * @see #minimumLayoutSize
     * @see #preferredLayoutSize
     */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns the alignment along the x axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /**
     * Returns the alignment along the y axis.  This specifies how
     * the component would like to be aligned relative to other
     * components.  The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     */
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     */
    public void invalidateLayout(Container target) {
    }

    /**
     * Centers the elements in the specified row, if there is any slack.
     * @param target the component which needs to be moved
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width dimensions
     * @param height the height dimensions
     * @param rowStart the beginning of the row
     * @param rowEnd the the ending of the row
     * @param array the stars array
     */
    private void moveComponents(Container target, int x, int y, int width, int height,
                                int rowStart, int rowEnd, boolean ltr, Object[] array) {
        synchronized (target.getTreeLock()) {
            switch (newAlign) {
                case FlowLayout.LEFT:
                    x += ltr ? 0 : width;
                    break;
                case FlowLayout.CENTER:
                    x += width / 2;
                    break;
                case FlowLayout.RIGHT:
                    x += ltr ? width : 0;
                    break;
                case FlowLayout.LEADING:
                    break;
                case FlowLayout.TRAILING:
                    x += width;
                    break;
            }
            for (int i = rowStart; i < rowEnd; i++) {
                Star s = (Star) array[i];
//            Component m = target.getComponent(i);
                if (ltr) {
                    s.layoutStar(x, y + (height - s.height) / 2);
                } else {
                    s.layoutStar(target.getWidth() - x - s.width, y + (height - s.height) / 2);
                }
                x += s.width + hgap;
            }
        }
    }

    /**
     * Lays out the container argument using this star layout.
     * <p>
     * Lays out the container. This method lets each star take
     * its preferred size by reshaping the stars in the
     * target container in order to satisfy the constraints of
     * this <code>StarLayout</code> object.
     * <p>
     * Most applications do not call this method directly. This method
     * is called when a container calls its <code>doLayout</code> method.
     * @param   target   the container in which to do the layout.
     * @see     java.awt.Container
     * @see     java.awt.Container#doLayout()
     */
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Object[] starArray = stars.values().toArray();

            Insets insets = target.getInsets();
            int maxwidth = target.getWidth() - (insets.left + insets.right + hgap * 2);
            int nmembers = starArray.length;
            int x = 0, y = insets.top + vgap;
            int rowh = 0, start = 0;

            boolean ltr = target.getComponentOrientation().isLeftToRight();

            for (int i = 0; i < nmembers; i++) {
                Star s = (Star) starArray[i];
                Dimension d = s.getPreferredSize();

                if ((x == 0) || ((x + d.width) <= maxwidth)) {
                    if (x > 0) x += hgap;
                    x += d.width;
                    rowh = Math.max(rowh, d.height);
                } else {
                    moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh, start, i, ltr, starArray);
                    x = d.width;
                    y += vgap + rowh;
                    rowh = d.height;
                    start = i;
                }
            }
            moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh, start, nmembers, ltr, starArray);
        }
    }

    /**
     * Returns a string representation of this <code>StarLayout</code>
     * object and its values.
     * @return     a string representation of this layout.
     */
    public String toString() {
        String str = "";
        switch (align) {
            case FlowLayout.LEFT:
                str = ",align=left";
                break;
            case FlowLayout.CENTER:
                str = ",align=center";
                break;
            case FlowLayout.RIGHT:
                str = ",align=right";
                break;
            case FlowLayout.LEADING:
                str = ",align=leading";
                break;
            case FlowLayout.TRAILING:
                str = ",align=trailing";
                break;
        }
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + str + "]";
    }

    /**
     * A Star object is a star model.
     * It stores a star center, spikes, width, height.
     * It calculates preferred & minimum sizes and can lay out components relatively
     * its own location.
     */

    private class Star {
        /**
         * Center of this star.
         */
        public Component center;
        /**
         * Vector to specify spikes.
         */
        public Vector spikes = new Vector();
        /**
         * <code>offset</code> is the property that determines the offset beetwen components.
         */
        private int offset = 0;
        /**
         * <code>maxMeas</code> is the property that determines the max components meashure.
         * <code>R</code> is the property that determines the star radius.
         */
        private int maxMeas, R = 0;
        /**
         * <code>width, height</code> is the property that determines the width & height
         * of this star.
         */
        public int width = 0, height = 0;

        /**
         * Constructs a new star.
         */
        public Star() {
        }

        /**
         * Constructs a new star with the specified center.
         * @param c the center of this star
         */
        public Star(Component c) {
            setCenter(c);
        }

        /**
         * Sets the center for this star.
         * @param      c the center of this star.
         * @see        #getCenter()
         */
        public void setCenter(Component c) {
            this.center = c;
        }

        /**
         * Gets the center of this star.
         * @return     the center of this star.
         * @see        #setCenter(Component)
         */
        public Component getCenter() {
            return this.center;
        }

        /**
         * Adds the specified component to this star. <p>
         * @param   c   the component to be added.
         * @see	   #setCenter(Component)
         */
        public void addSpike(Component c) {
            this.spikes.addElement(c);
            R = 0;
        }

        /**
         * Tests if the specified component is a spike in this star.
         *
         * @param   c   a component.
         * @return  <code>true</code> if vector of spikes contains the specified component;
         * <code>false</code> otherwise.
         */
        public boolean containsSpike(Component c) {
            return this.spikes.contains(c);
        }

        /**
         * Removes the occurrence of the argument from this star
         * if vector of spikes contains that.
         *
         * @param   c   the component to be removed.
         * @see	#addSpike(Component)
         */
        public void removeSpike(Component c) {
            this.spikes.removeElement(c);
            R = 0;
        }

        /**
         * Returns the number of spikes in this star.
         *
         * @return  the number of spikes in this star.
         */
        public int getSpikesCount() {
            return this.spikes.size();
        }

        /**
         * Gets the preferred size of this star.
         * @return A dimension object indicating this star's preferred size.
         * @see #getMinimumSize
         * @see java.awt.LayoutManager
         */
        public Dimension getPreferredSize() {
            maxMeas = 0;
            for (int i = 0; i < spikes.size(); i++) {
                maxMeas = Math.max(maxMeas, Math.max(((Component) spikes.elementAt(i)).getPreferredSize().width, ((Component) spikes.elementAt(i)).getPreferredSize().height));
            }
            return size();
        }

        /**
         * Gets the minimum size of this star.
         * @return A dimension object indicating this star's minimum size.
         * @see #getPreferredSize
         * @see java.awt.LayoutManager
         */
        public Dimension getMinimumSize() {
            maxMeas = 0;
            for (int i = 0; i < spikes.size(); i++) {
                maxMeas = Math.max(maxMeas, Math.max(((Component) spikes.elementAt(i)).getPreferredSize().width, ((Component) spikes.elementAt(i)).getPreferredSize().height));
            }
            return size();
        }

        /**
         * Gets the size of this star by maxMeas, center meas & spikes cnt.
         * @return A dimension object indicating this star's size.
         * @see #getPreferredSize
         * @see #getMinimumSize
         */
        private Dimension size() {
            int rc = (int) (Math.sqrt(center.getWidth() * center.getWidth() + center.getHeight() * center.getHeight()) / 2);
            R = rc + (int) (spikes.size() * (maxMeas * Math.sqrt(2) + offset) / (2 * Math.PI));
            int wh = 2 * R + maxMeas;
            width = wh;
            height = wh;
            return new Dimension(width, height);
        }

        /**
         * Lays out the star.
         * Gets private coordinates for each components & adds them to
         * this star coordinate.
         * @param   xs   the x coordinate of star.
         * @param   ys   the y coordinate of star.
         * @see     StarLayout#layoutContainer(Container)
         */
        public void layoutStar(int xs, int ys) {
            // the star coordinate
            if (R == 0) getPreferredSize();
            double a = Math.PI / spikes.size();
            double da = 2 * a;
            for (int i = 0; i < spikes.size(); i++) {
                int x = (int) (R * Math.cos(a) + R + maxMeas / 2 - ((Component) spikes.elementAt(i)).getWidth() / 2);
                int y = (int) (R * Math.sin(a) + R + maxMeas / 2 - ((Component) spikes.elementAt(i)).getHeight() / 2);
                ((Component) spikes.elementAt(i)).setLocation(x + xs, y + ys);
                a += da;
            }
            center.setLocation(xs + R + maxMeas / 2 - center.getWidth() / 2, ys + R + maxMeas / 2 - center.getHeight() / 2);
        }
    }
}
