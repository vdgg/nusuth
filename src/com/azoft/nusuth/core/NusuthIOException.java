package com.azoft.nusuth.core;

import java.io.IOException;

/**
 * This class represents IOException for broken pipe.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class NusuthIOException extends IOException {

    /**Message of exception*/
    private String message = null;

    /**
     * Default constructor.
     */
    public NusuthIOException() {
        super();
    }

    /**
     * Constructor.
     * @param message Message of Exception.
     */
    public NusuthIOException(String message) {
        this.message = message;
    }

}
