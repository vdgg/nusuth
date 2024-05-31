/*
 * @(#)StarConstraints.java 1.0 07/28/2001
 */

package com.azoft.nusuth.gui;

import java.awt.*;

/**
 * Class StarConstraints.
 *
 * @version 1.0 07/28/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class StarConstraints {
    public static final int CENTER = 0;
    public static final int SPIKE = 1;

    public int type;
    public Component center;

    public StarConstraints(int type, Component center) {
        this.type = type;
        this.center = center;
    }
}
