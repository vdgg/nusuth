package com.azoft.nusuth.jndi;

import javax.naming.*;
import javax.naming.directory.*;
import java.io.Serializable;

/**This class represents substance that contains a pair of attribute name and the attribute value.
 * It contains methods that allow to set and get this data.
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
class NameAttributePair implements Serializable {
    private Name name;
    private Attribute attribute;

    /**Constructor.
     * @param name the attribute name.
     * @param attribute the attribute object.
     */
    NameAttributePair(Name name, Attribute attribute) {
        this.name = name;
        this.attribute = attribute;
    }

    /**
     * @return the attribute name.
     */
    Name getName() {
        return name;
    }

    /**
     * @return the attribute object.
     */
    Attribute getAttribute() {
        return attribute;
    }

    /**This method sets the new atribute value (object).
     * @param the new attribute object.
     */
    void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }
}

