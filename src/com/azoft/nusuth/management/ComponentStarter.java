package com.azoft.nusuth.management;

import java.util.*;

public abstract class ComponentStarter {


    /**
     * Insert the method's description here.
     * Creation date: (21.01.01 19:29:15)
     * @param args java.lang.String[]
     */
    public ComponentStarter(String[] args) {
        try {
            start(parseArgs(args));
        } catch (Throwable t) {
            System.out.println("Can't start:" + t.getMessage());
            System.out.println("Stack:");
            t.printStackTrace();
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (21.01.01 14:38:26)
     * @param args java.lang.String[]
     */
    private Properties parseArgs(String[] args) {
        Properties result = new Properties();
        for (int i = 0; i < args.length;) {
            String name = args[i++];
            String value = "true";
            if (!name.startsWith("-")) {
                usage();
                return null;
            }

            if (i < args.length && !args[i].startsWith("-")) {
                value = args[i++];
            }

            name = name.substring(1).toLowerCase();

            result.setProperty(name, value);
        }
        return result;
    }


    /**
     * Insert the method's description here.
     * Creation date: (21.01.01 19:19:58)
     * @param parameters java.util.Properties
     */
    protected abstract void start(Properties parameters) throws Throwable;


    /**
     * Insert the method's description here.
     * Creation date: (21.01.01 19:00:27)
     */
    public abstract void usage();
}