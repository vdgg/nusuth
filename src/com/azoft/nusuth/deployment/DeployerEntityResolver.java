/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.deployment;

import org.xml.sax.*;

public class DeployerEntityResolver implements EntityResolver {


    public InputSource resolveEntity(String publicId, String systemId) {
        return new InputSource(getClass().getClassLoader().getResourceAsStream("com/azoft/nusuth/deployment/deployer.dtd"));
    }
}