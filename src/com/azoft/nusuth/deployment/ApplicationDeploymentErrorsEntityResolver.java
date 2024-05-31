package com.azoft.nusuth.deployment;

import org.xml.sax.*;

public class ApplicationDeploymentErrorsEntityResolver implements EntityResolver {


    public InputSource resolveEntity(String arg1, String arg2) {
        return new InputSource(getClass().getClassLoader().getResourceAsStream("com/azoft/nusuth/deployment/application_deployment_errors.dtd"));
    }
}