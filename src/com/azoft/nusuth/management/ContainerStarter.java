package com.azoft.nusuth.management;

import com.azoft.nusuth.container.NusuthContainerStarter;

/**
 * Insert the type's description here.
 * Creation date: (24.01.01 1:20:38)
 * @author: IgorK
 */
public class ContainerStarter extends ComponentStarter {


    /**
     * ContainerStarter constructor comment.
     * @param args java.lang.String[]
     */
    public ContainerStarter(String[] args) {
        super(args);
    }


    /**
     * Insert the method's description here.
     * Creation date: (24.01.01 1:28:56)
     * @param args java.lang.String[]
     */
    public static void main(String[] args) {
//    ContainerStarter containerStarter = new ContainerStarter(args);
        NusuthContainerStarter.main(args);
    }


    protected void start(java.util.Properties parameters) throws Throwable {
        //  JBirdContainer container = new JBirdContainer(parameters.getProperty("config"));
        //  String m = parameters.getProperty("manager");
        //  if (m != null && (m.equals("on") || m.equals("true") || m.equals("start")))

//    new ContainerManagerImpl(new JBirdContainer(parameters.getProperty("config")));
    }


    /**
     * usage method comment.
     */
    public void usage() {
        System.out.println("Usage: java com.azoft.nusuth.management.ContainerStarter [-config <config.file>] [-manager [true|on|start|<anything else>]]");
    }
}
