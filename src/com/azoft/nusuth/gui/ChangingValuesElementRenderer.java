/*
 * @(#)ChangingValuesElementRenderer.java 1.0 4/12/2001
 */

package com.azoft.nusuth.gui;

import java.util.Vector;

/**
 * Interface ChangingValuesElementRenderer defines behavior for renderers
 * with changing values. Defines the methods to add/delete/change items.
 *
 * @version 1.0 4/12/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public interface ChangingValuesElementRenderer extends ElementRenderer {

    /**
     * Adds the specified item with the specified id.
     *
     * @param id    the id of the item
     * @param item  the specified item.
     * @see #removeItem(String, String)
     * @see #changeItem(String, String, String)
     */
    public void addItem(String id, String item);

    /**
     * Removes the specified item with the specified id.
     *
     * @param id    the id of the item
     * @param item  the specified item.
     * @see #addItem(String, String)
     * @see #changeItem(String, String, String)
     */
    public void removeItem(String id, String item);


    /**
     * Changes the specified old item with the specified id
     * by the new item.
     *
     * @param id    the id of the item
     * @param oldItem  the specified old item.
     * @param newItem  the specified new item.
     * @see #addItem(String, String)
     * @see #removeItem(String, String)
     */
    public void changeItem(String id, String oldItem, String newItem);

    /**
     * Removes all items.
     *
     * @see #removeItem(String, String)
     */
    public void removeAllItems();
}