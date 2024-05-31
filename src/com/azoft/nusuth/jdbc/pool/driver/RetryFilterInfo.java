package com.azoft.nusuth.jdbc.pool.driver;

/**
 * Retry state information. It is is used to configure a database adapter.
 *
 * @author Constantine Plotnikov
 *
 * @version 0.5.5
 * @since 0.5.0
 */
public class RetryFilterInfo {
    /**
     * sql state
     */
    private String state;
    /**
     * sql codes
     */
    private int[] codes;


    /**
     * Constructor
     */
    public RetryFilterInfo() {
    }


    /**
     * get sql state
     *
     *@return The State value
     */
    public String getState() {
        return state;
    }


    /**
     * set sql state
     *
     *@param newValue The new State value
     */
    public void setState(String newValue) {
        state = newValue;
    }


    /**
     * get sql state
     *
     *@return The Codes value
     */
    public int[] getCodes() {
        return codes;
    }


    /**
     * set sql state
     *
     *@param newValue The new Codes value
     */
    public void setCodes(int[] newValue) {
        codes = newValue;
    }
}

