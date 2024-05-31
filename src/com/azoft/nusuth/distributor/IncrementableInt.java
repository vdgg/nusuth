package com.azoft.nusuth.distributor;

/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/

/**
 * Insert the type's description here.
 * Creation date: (11.01.01 1:14:04)
 * @author:
 */
public class IncrementableInt {

    private int value = 0;


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 1:21:57)
     * @param value long
     */
    public IncrementableInt(int value) {
        this.value = value;
    }


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 1:20:24)
     * @return long
     */
    public void dec() {
        --value;
    }


    /**
     * Compares two objects for equality. Returns a boolean that indicates
     * whether this object is equivalent to the specified object. This method
     * is used when an object is stored in a hashtable.
     * @param obj the Object to compare with
     * @return true if these Objects are equal; false otherwise.
     * @see java.util.Hashtable
     */
    public boolean equals(Object obj) {

        if (obj instanceof IncrementableInt) {
            return ((IncrementableInt) obj).value == this.value;
        } else {
            return super.equals(obj);
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 1:17:21)
     * @return long
     */
    public int getValue() {
        return value;
    }


    /**
     * Generates a hash code for the receiver.
     * This method is supported primarily for
     * hash tables, such as those provided in java.util.
     * @return an integer hash code for the receiver
     * @see java.util.Hashtable
     */
    public int hashCode() {
        return (int) (value ^ (value >> 32));
    }


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 1:20:24)
     * @return long
     */
    public void inc() {
        ++value;
    }


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 1:17:21)
     * @param newValue long
     */
    public void setValue(int newValue) {
        value = newValue;
    }
}