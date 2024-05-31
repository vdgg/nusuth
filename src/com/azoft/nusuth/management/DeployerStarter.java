package com.azoft.nusuth.management;

import java.util.*;

public class DeployerStarter extends ComponentStarter {


    /**
     * Insert the method's description here.
     * Creation date: (21.01.01 19:33:34)
     * @param args java.lang.String[]
     */
    public DeployerStarter(String[] args) {
        super(args);
    }


    /**
     * Insert the method's description here.
     * Creation date: (21.01.01 19:32:24)
     * @param args java.lang.String[]
     */
    public static void main(String[] args) {
        DeployerStarter starter = new DeployerStarter(args);
    }


    /**
     * start method comment.
     */
    protected void start(Properties parameters) throws Throwable {
        Deployer deployer = new DeployerImpl(parameters.getProperty("config"));
    }


    /**
     * usage method comment.
     */
    public void usage() {
        System.out.println("Usage: java com.azoft.nusuth.management.DeployerStarter [-config <config.file>]");
    }
}