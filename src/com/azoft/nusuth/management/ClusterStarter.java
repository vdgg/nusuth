package com.azoft.nusuth.management;

import com.azoft.nusuth.management.security.*;

import java.io.*;

/**
 * Insert the type's description here.
 * Creation date: (08.12.00 20:57:26)
 * @author: Administrator
 */
public class ClusterStarter extends ComponentStarter {


    /**
     * Insert the method's description here.
     * Creation date: (24.01.01 4:00:16)
     * @param args java.lang.String[]
     */
    public ClusterStarter(String[] args) {
        super(args);
    }


    /**
     * Starts the application.
     * @param args an array of command-line arguments
     */
    public static void main(java.lang.String[] args) {
        ClusterStarter starter = new ClusterStarter(args);
    }


    /**
     * start method comment.
     */
    protected void start(java.util.Properties parameters) throws java.lang.Throwable {
        System.out.println("Starting Cluster Manager...");
        ClusterManager manager = new ClusterManager(parameters.getProperty("config"));
    }


    /**
     * usage method comment.
     */
    public void usage() {
        System.out.println("Usage: java com.azoft.nusuth.management.ClusterStarter [-config <config.file>]");
    }
}