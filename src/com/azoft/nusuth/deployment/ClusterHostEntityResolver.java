package com.azoft.nusuth.deployment;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class ClusterHostEntityResolver implements EntityResolver {
    public InputSource resolveEntity(String publicId, String systemId) {
        return new InputSource(getClass().getClassLoader().getResourceAsStream(
                "com/azoft/nusuth/deployment/cluster-host.dtd"));
    }
}