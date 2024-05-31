package com.azoft.nusuth.deployment;

import org.xml.sax.InputSource;
import org.xml.sax.EntityResolver;

public class WebAppUsersEntityResolver implements EntityResolver {
    public InputSource resolveEntity(String arg1, String arg2) {
        return new InputSource(getClass().getClassLoader().getResourceAsStream("com/azoft/nusuth/deployment/web-app-users.dtd"));
    }
}
