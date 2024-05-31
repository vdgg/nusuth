package com.azoft.nusuth.jndi;

import javax.naming.*;
import java.util.*;
import java.io.Serializable;

/**This class is used to hold differents elements of @see Binding or @see SearchResult class type.
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
public class NusuthNamingEnumeration implements NamingEnumeration, Serializable {
    private boolean valid;
    private List elements;
    private int current = 0;

    /**Constructor.
     */
    protected NusuthNamingEnumeration() {
        elements = new ArrayList();
        valid = true;
    }

    /**Adds element to the enumeration.
     * @param obj new element.
     */
    protected void addElement(Object obj) {
        elements.add(obj);
    }

    /**Determines whether there are any more elements in the enumeration.
     * @return True if there are any more elements and enumeration is valid, otherwise False.
     * @exception NamingException never.
     */
    public boolean hasMore() throws NamingException {
        return hasMoreElements();
    }

    /**Determines whether there are any more elements in the enumeration.
     * @return True if there are any more elements and enumeration is valid, otherwise False.
     */
    public boolean hasMoreElements() {
        if (valid) {
            return current < elements.size();
        } else
            return false;
    }

    /**Retrieves the next element in the enumeration.
     * This method allows naming exceptions encountered while retrieving the next element to be caught and handled by the application.
     * @return The possibly null element in the enumeration. null is only valid for enumerations that can return null (e.g. Attribute.getAll() returns an enumeration of attribute values, and an attribute value can be null).
     * @exception NamingException never.
     * @exception NoSuchElementException  if attempting to get the next element when none is available.
     */
    public Object next() throws NamingException {
        return nextElement();
    }

    /**Retrieves the next element in the enumeration
     * Note that this method can also throw the runtime exception NoSuchElementException
     * to indicate that the caller is attempting to enumerate beyond the end of the enumeration
     * @return The possibly null element in the enumeration. null is only valid for enumerations that can return null (e.g. Attribute.getAll() returns an enumeration of attribute values, and an attribute value can be null).
     * @exception NoSuchElementException  if attempting to get the next element when none is available or enumeration is not valid.
     */
    public Object nextElement() {
        if (valid) {
            return elements.get(current++);
        } else
            throw new NoSuchElementException();
    }

    /**Closes this enumeration.
     * Makes it not valid.
     * @exception NamingException never.
     */
    public void close() throws NamingException {
        valid = false;
    }
}

