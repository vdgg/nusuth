/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.deployment;

import org.xml.sax.InputSource;

/**
 * Insert the type's description here. Creation date: (24.12.00 0:37:16)
 * @author: Administrator
 */
public class ClusterEntityResolver implements org.xml.sax.EntityResolver {


    /** ClusterEntityResolver constructor comment. */
    public ClusterEntityResolver() {
        super();
    }


    /** resolveEntity method comment. */
    public InputSource resolveEntity(String arg1, String arg2) throws java.io.IOException, org.xml.sax.SAXException {
        return new InputSource(getClass().getClassLoader().getResourceAsStream("com/azoft/nusuth/deployment/cluster.dtd"));
    }
}