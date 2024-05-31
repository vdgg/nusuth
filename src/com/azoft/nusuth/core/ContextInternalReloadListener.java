package com.azoft.nusuth.core;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.session.DefaultSessionManager;
import com.azoft.nusuth.session.DistributedSessionManager;
import com.azoft.nusuth.container.NusuthRequestHandler;

/**
 * This class extends Thread. It listen for context changes and cause context
 * to reload all classes.
 * @author skilz.
 * @version 1.2
 * @since Nusuth1.0
 */
public class ContextInternalReloadListener extends Thread {

    /**Context to listen*/
    private NusuthContext context = null;
    /**Checking period in milliseconds*/
    private long checkTime = -1;
    /**life indicator*/
    private boolean alive = true;

    private String workDir = null;
    private String logLocation = null;
    private String contextName = null;
    private CompositeNusuthWebAppElement appSettings = null;
    private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("com.azoft.nusuth.core");


    /**
     * Constructor.
     * @param context Context to listen.
     * @checkTime Time in milliseconds.
     */
    public ContextInternalReloadListener(NusuthContext context, long checkTime,
                                         String workDir, String logLocation,
                                         CompositeNusuthWebAppElement appSettings,
                                         String contextName)
                throws DeploymentException {
        this.context = context;
        this.checkTime = checkTime;
        this.workDir = workDir;
        this.logLocation = logLocation;
        this.appSettings = appSettings;
        this.contextName = contextName;
    }

    /**
     * This method listen for context changes.
     */
    public void run() {
        while (!context.isShuttingDown() && alive) {
            if (context.isReloadNeeded()) {
                try {
                    context.shutdownContext(true);
                    NusuthContext newContext = new NusuthContext(context.getDocBase(), contextName,
                            workDir, logLocation,
                            appSettings,
                            context.getSessionBackup());
                    if (context.getSessionManager() instanceof DefaultSessionManager) {
                        newContext.setSessionManager(new DefaultSessionManager(newContext,
                                false));
                    } else {
                        newContext.setSessionManager(
                                new DistributedSessionManager(newContext));
                    }
                    NusuthRequestHandler.startNewContextInternal(contextName, newContext);
                    this.context = newContext;
                } catch (DeploymentException e) {
                    cat.error("Cannot create context", e);
                }
            }
            try {
                sleep(checkTime);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * This method shutdown current thread.
     */
    public void shutDown() {
        alive = false;
    }

}
