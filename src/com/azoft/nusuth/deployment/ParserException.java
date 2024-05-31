package com.azoft.nusuth.deployment;

/**
 * This Exception throws when error occured while parsing web application configuration file.
 * @version 1.0
 * @author VDGG (vdgg@azoft.com)
 * @since 1.0*/
public class ParserException extends DeploymentException {


    public ParserException() {
        super();
    }


    /**
     * @param message that describes the exception.
     */
    public ParserException(String message) {
        super(message);
    }
}