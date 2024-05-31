package com.azoft.nusuth.core;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.container.NusuthRequestHandler;
import com.azoft.nusuth.session.DefaultSessionManager;
import com.azoft.nusuth.session.DistributedSessionManager;

/**
 * This class extends Thread. It listen for context changes.
 * If context was changed it creates new context and put it in
 * NusuthRequestHandler.
 * @author skilz.
 * @version 1.2
 * @since Nusuth1.0
 */
public class ContextLazyReloadListener extends Thread {

    /**context to listen*/
    private NusuthContext context = null;
    /**time to check*/
    private long checkTime = -1;
    /**context location*/
    private String location = null;
    /**context name (hostId+contextPath)*/
    private String contextName = null;
    /**working directory of context*/
    private String workDir = null;
    /**location of logging file*/
    private String logLocation = null;
    /**Context settings*/
    private CompositeNusuthWebAppElement appSettings = null;
    /**sessionBackup parameter for context*/
    private String sessionBackup = null;
    /**logger*/
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.core");
    private boolean alive = true;

    /**
     * Constructor.
     * @param context Context to listen
     * @param checkTime Checking interval
     * @param workDir Context's working directory
     * @param logLocation Context's log location
     * @param appSettings Context's config
     * @param contextName Context name (hostId+contextPath)
     */
    public ContextLazyReloadListener(NusuthContext context, long checkTime,
                                     String workDir, String logLocation,
                                     CompositeNusuthWebAppElement appSettings,
                                     String contextName)
            throws DeploymentException {
        this.context = context;
        this.checkTime = checkTime;
        this.location = context.getDocBase();
        this.contextName = contextName;
        this.logLocation = logLocation;
        this.appSettings = (CompositeNusuthWebAppElement) appSettings.clone();
        ManagementUtil.removeCompositeChild(this.appSettings, "auto-reload");
        this.sessionBackup = context.getSessionBackup();
        this.workDir = context.getWorkDir();
    }

    /**
     * This method listen for context changes.
     * If it was changed then it create new Context and put it in
     * NusuthRequestHandler. It also tell to JBirdContextHandler to remove all
     * old contexts which not contains sessions.
     */
    public void run() {
        while (!context.isShuttingDown() && alive) {
            if (context.isReloadNeeded()) {
                try {
                    NusuthContext newContext = new NusuthContext(location, contextName,
                            workDir, logLocation,
                            appSettings,
                            sessionBackup);
                    if (context.getSessionManager() instanceof DefaultSessionManager) {
                        newContext.setSessionManager(new DefaultSessionManager(newContext,
                                false));
                    } else {
                        newContext.setSessionManager(
                                new DistributedSessionManager(newContext));
                    }
                    NusuthRequestHandler.startNewContext(contextName, newContext);
                    this.context = newContext;
                } catch (DeploymentException e) {
                    cat.error("Cannot create context", e);
                }
            }
            NusuthRequestHandler.shutDownOldContexts(contextName);
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
