package com.azoft.nusuth.management;

/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/

/**
 * Insert the type's description here.
 * Creation date: (12.12.00 23:27:31)
 * @author: Administrator
 */
public class DistributorStarter extends ComponentStarter {


    /**
     * Insert the method's description here.
     * Creation date: (24.01.01 4:05:17)
     * @param args java.lang.String[]
     */
    public DistributorStarter(String[] args) {
        super(args);
    }


    /**
     * Starts the application.
     * @param args an array of command-line arguments
     */
    public static void main(String[] args) {
        DistributorStarter starter = new DistributorStarter(args);
    }


    /**
     * start method comment.
     */
    protected void start(java.util.Properties parameters) throws java.lang.Throwable {
        DistributorManager dmanager = new DistributorManagerImpl(parameters.getProperty("config"));
    }


    /**
     * usage method comment.
     */
    public void usage() {
        System.out.println("Usage: java com.azoft.nusuth.management.DistributorStarter [-config <config.file>]");
    }
}