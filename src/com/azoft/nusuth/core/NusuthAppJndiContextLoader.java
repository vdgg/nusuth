package com.azoft.nusuth.core;


import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.jndi.NusuthJNDIContext;
import com.azoft.nusuth.jndi.java.JavaContext;
import com.azoft.nusuth.management.ManagementUtil;

import org.apache.log4j.Category;
import com.azoft.nusuth.jdbc.pool.ConnectionPoolDataSourceImpl;
import com.azoft.nusuth.jdbc.pool.ConnectionPoolWithQueue;
import com.azoft.nusuth.jdbc.pool.driver.DataSourceAdapter;
import com.azoft.nusuth.jdbc.pool.driver.DriverDataSource;
import com.azoft.nusuth.jdbc.xa.FLXADataSource;
import com.azoft.nusuth.jdbc.xa.XADataSourceImpl;
import tyrex.tm.Tyrex;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Driver;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.rmi.RMISecurityManager;
import java.io.*;


class ExternalContext {
    protected String root = "";

    protected Context context = null;

    protected ExternalContext(String initRoot, Context initContext) {
        root = initRoot;
        context = initContext;
    }
}

/**
 * @author igork.
 * @version 1.10
 * @since Nusuth1.0
 */
public class NusuthAppJndiContextLoader {
    /** UserTransaction JNDI name. */
    protected final static String USER_TRANSACTION_NAME = "UserTransaction";

    private Category logger = Category.getInstance(NusuthAppJndiContextLoader.class.getName());


    public static Context load(CompositeNusuthWebAppElement config, CompositeNusuthWebAppElement appSettings, Category logger, boolean useJTA) {
        if (appSettings == null)
            return null;

        logger.debug("Start naming context loading");
        String prevPacks = System.getProperty("java.naming.factory.url.pkgs");
        String newPacks = "com.azoft.nusuth.jndi" + (prevPacks == null || prevPacks.length() == 0 ? "" : ":" + prevPacks);
        System.setProperty("java.naming.factory.url.pkgs", newPacks);

        NusuthAppJndiContextLoader contextLoader = new NusuthAppJndiContextLoader();

        try {
            Context rootContext = contextLoader.createContext();

            contextLoader.loadRmiSecurityManager(appSettings);

            ExternalContext[] externalContexts = contextLoader.loadExternalJndiContexts(appSettings, logger);
            HashMap externalJdbcConnects = contextLoader.loadExternalJdbcConnects(appSettings, logger, useJTA);
            contextLoader.loadResourceEntries(config, appSettings, externalContexts, externalJdbcConnects, rootContext, logger, useJTA);
            contextLoader.loadEnvEntries(config, appSettings, rootContext, logger);
            contextLoader.loadEjbEntries(config, externalContexts, appSettings, rootContext, logger);

            /*System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "tyrex.naming.MemoryContextFactory");
             System.setProperty(Context.URL_PKG_PREFIXES, "tyrex.naming");
             */
            //System.setProperty(Context.INITIAL_CONTEXT_FACTORY, JBirdWebAppInitialContextFactory.class.getName());
            //Class waicf = Class.forName(JBirdWebAppInitialContextFactory.class.getName(), true, Thread.currentThread().getContextClassLoader());
            //waicf.getMethod("initialize", new Class[] {Hashtable.class, Context.class}).invoke(null, new Object[] {new Hashtable(), rootContext});

            Class waicf = Class.forName(JavaContext.class.getName(), true, Thread.currentThread().getContextClassLoader());
            waicf.getMethod("initialize", new Class[]{Context.class}).invoke(null, new Object[]{rootContext});

            logger.debug("Naming context loaded sucessfully");

            return rootContext;
        } catch (Exception ex) {
            logger.error("Couldn't create web application environment jndi context", ex);
            //return new NusuthJNDIContext();
            return null;
        }
    }

    public static void reload(CompositeNusuthWebAppElement config,
                              CompositeNusuthWebAppElement appSettings,
                              Category logger, boolean useJTA,
                              Context rootContext) {

        logger.debug("Start naming context reloading");
        String prevPacks = System.getProperty("java.naming.factory.url.pkgs");
        String newPacks = "com.azoft.nusuth.jndi"
                + (prevPacks == null || prevPacks.length() == 0
                ? "" : ":" + prevPacks);
        System.setProperty("java.naming.factory.url.pkgs", newPacks);

        NusuthAppJndiContextLoader contextLoader = new NusuthAppJndiContextLoader();

        try {
            rootContext.unbind("comp");

            Context compContext = rootContext.createSubcontext("comp");
            Context envContext = compContext.createSubcontext("env");
            envContext.createSubcontext("ejb");
            envContext.createSubcontext("jdbc");
            envContext.createSubcontext("jms");   // not implemented
            envContext.createSubcontext("mail");  // not implemented
            envContext.createSubcontext("url");   // not implemented

            contextLoader.loadRmiSecurityManager(appSettings);

            ExternalContext[] externalContexts
                    = contextLoader.loadExternalJndiContexts(appSettings, logger);
            HashMap externalJdbcConnects
                    = contextLoader.loadExternalJdbcConnects(appSettings,
                            logger, useJTA);
            contextLoader.loadResourceEntries(config, appSettings,
                    externalContexts,
                    externalJdbcConnects, rootContext,
                    logger, useJTA);
            contextLoader.loadEnvEntries(config, appSettings,
                    rootContext, logger);
            contextLoader.loadEjbEntries(config, externalContexts, appSettings,
                    rootContext, logger);

            Class waicf
                    = Class.forName(JavaContext.class.getName(), true,
                            Thread.currentThread().
                    getContextClassLoader());
            waicf.getMethod("initialize",
                    new Class[]{Context.class}).
                    invoke(null, new Object[]{rootContext});

            logger.debug("Naming context reloaded sucessfully");

        } catch (Exception ex) {
            logger.error("Couldn't create web application environment "
                    + "jndi context", ex);
        }
    }

    private Context createContext() throws NamingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        //Context root = new MemoryContext(null);
        Context root = new NusuthJNDIContext();

        Context compContext = root.createSubcontext("comp");
        Context envContext = compContext.createSubcontext("env");
        envContext.createSubcontext("ejb");
        envContext.createSubcontext("jdbc");
        envContext.createSubcontext("jms");   // not implemented
        envContext.createSubcontext("mail");  // not implemented
        envContext.createSubcontext("url");   // not implemented

        return root;
    }

    // Static helpers

    private static String createJndiName(String root, String name) {
        String b = name.startsWith("/") && root.length() > 0 ? name.substring(1) : name;
        String r = root.endsWith("/") ? root : (root.length() > 0 ? root + "/" : root);
        return r + b;
    }

    private static String cutHeader(String str, String header) {
        String result = str;
        if (result.startsWith(header)) result = result.substring(header.length());
        if (result.startsWith("/")) result = result.substring(1);
        return result;
    }

    private static String createInternalJndiName(String beanName) {
        String result = beanName;
        result = cutHeader(result, "java:comp");
        result = cutHeader(result, "comp");
        result = cutHeader(result, "env");
        result = cutHeader(result, "ejb");
        result = cutHeader(result, "jdbc");
        result = cutHeader(result, "jms");
        result = cutHeader(result, "mail");
        result = cutHeader(result, "url");
        return result;
    }

    //

    private void loadEnvEntries(CompositeNusuthWebAppElement config, CompositeNusuthWebAppElement appSettings, Context rootContext, Category logger) throws DeploymentException, NamingException {
        Context envContext = (Context) rootContext.lookup("comp/env");
        for (Enumeration entries = config.getCompositeChild("env-entry"); entries.hasMoreElements();) {
            CompositeNusuthWebAppElement entry = (CompositeNusuthWebAppElement) entries.nextElement();
            String name = ManagementUtil.getSimpleString(entry, "env-entry-name");
            String valueString = ManagementUtil.getSimpleString(entry, "env-entry-value");
            String typeName = ManagementUtil.getSimpleString(entry, "env-entry-type");
            Object value = null;
            if (typeName.equals("java.lang.Boolean")) {
                value = new Boolean(valueString);
            } else if (typeName.equals("java.lang.String")) {
                value = new String(valueString);
            } else if (typeName.equals("java.lang.Integer")) {
                value = new Integer(valueString);
            } else if (typeName.equals("java.lang.Double")) {
                value = new Double(valueString);
            } else if (typeName.equals("java.lang.Float")) {
                value = new Float(valueString);
            } else {
                logger.error("Configuration error: incorrect type \"" + typeName + "\"for environment entry \"" + name + "\". Legal values of env-entry-type: java.lang.Boolean, java.lang.String, java.lang.Integer, java.lang.Double, java.lang.Float");
            }
            if (value != null) {
                envContext.bind(name, value);
            }
        }
    }

    private ExternalContext[] loadExternalJndiContexts(CompositeNusuthWebAppElement appSettings, Category logger) throws DeploymentException, NamingException {
        Vector externalJndiContexts = new Vector();
        for (Enumeration links = appSettings.getCompositeChild("jndi-link"); links.hasMoreElements();) {
            CompositeNusuthWebAppElement linkNode = (CompositeNusuthWebAppElement) links.nextElement();
            Properties parameters = new Properties();
            for (Enumeration parametersEnum = linkNode.getCompositeChild("parameter"); parametersEnum.hasMoreElements();) {
                CompositeNusuthWebAppElement parameterNode = (CompositeNusuthWebAppElement) parametersEnum.nextElement();
                parameters.put(ManagementUtil.getSimpleString(parameterNode, "name"), ManagementUtil.getSimpleString(parameterNode, "value"));
            }
            String extICFN = ManagementUtil.getSimpleString(linkNode, "factory");
            parameters.put(Context.INITIAL_CONTEXT_FACTORY, extICFN);

            try {
                externalJndiContexts.add(new ExternalContext(ManagementUtil.getSimpleString(linkNode, "lookup"), new InitialContext(parameters)));
            } catch (Throwable t) {
                logger.error("Couldn't connect to external EJB server", t);
            }
        }
        return (ExternalContext[]) externalJndiContexts.toArray(new ExternalContext[0]);
    }

    private HashMap loadExternalJdbcConnects(CompositeNusuthWebAppElement appSettings, Category logger, boolean useJTA) throws DeploymentException, NamingException {
        HashMap result = new HashMap();
        for (Enumeration links = appSettings.getCompositeChild("jdbc-link"); links.hasMoreElements();) {
            CompositeNusuthWebAppElement linkNode = (CompositeNusuthWebAppElement) links.nextElement();
            String name = ManagementUtil.getSimpleString(linkNode, "name");
            if (result.containsKey(name)) {
                logger.error("Misconfiguration: duplicate definition for \"" + name + "\" jdbc link.");
                continue;
            }
            String url = ManagementUtil.getSimpleString(linkNode, "url");

            String driverClassName = ManagementUtil.getSimpleString(linkNode, "driver");
            String user = ManagementUtil.getSimpleString(linkNode, "user");
            String password = ManagementUtil.getSimpleString(linkNode, "password");
            // properties with default value
            String loginTimeoutStr = ManagementUtil.getSimpleString(linkNode, "login-timeout");
            String poolSizeLimitStr = ManagementUtil.getSimpleString(linkNode, "pool-size-limit");
            // optional properties
            String fillPoolStr = ManagementUtil.getSimpleString(linkNode, "fill-pool");
            //
            String initSql = ManagementUtil.getSimpleString(linkNode, "init-sql");
            //
            String description = ManagementUtil.getSimpleString(linkNode, "description");
            String transactionTimeout = ManagementUtil.getSimpleString(linkNode, "transaction-timeout");
            String isolationLevel = ManagementUtil.getSimpleString(linkNode, "isolation-level");

            try {
                DataSource ds = initDataSource(name, url, driverClassName, user, password, loginTimeoutStr, poolSizeLimitStr, fillPoolStr, initSql, description, transactionTimeout, isolationLevel, useJTA);
                result.put(name, ds);
            } catch (Throwable t) {
                logger.error("Couldn't create DataSource", t);
            }
        }

        return result;
    }

    private DataSource initDataSource(String dsName, // required properties
                                      String url, String driverClassName, String user, String password, // properties with default value
                                      String loginTimeoutStr, String poolSizeLimitStr, // optional properties
                                      String fillPoolStr, //
                                      String initSql, //
                                      String description, String transactionTimeout, String isolationLevel, //
                                      boolean useJTA) throws DeploymentException, SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating datasources for db : " + dsName);
        }
        // check
        if (url == null || url.length() == 0 || driverClassName == null || driverClassName.length() == 0) {
            throw new RuntimeException("Illegal \"jdbc-link\" node: not all required properties are set");
        }
        // properties with default value
        int loginTimeout = Integer.parseInt((loginTimeoutStr == null || loginTimeoutStr.length() == 0 ? "30" : loginTimeoutStr));
        int poolSizeLimit = Integer.parseInt((poolSizeLimitStr == null || poolSizeLimitStr.length() == 0 ? "20" : poolSizeLimitStr));
        // optional properties
        boolean fillPool = new Boolean((fillPoolStr == null || fillPoolStr.length() == 0 ? "false" : fillPoolStr)).booleanValue();

        // driver class name
        try {
            Class.forName(driverClassName, true, Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (Exception ex) {
            logger.error("Cannot load driver class, class name = " + driverClassName, ex);
            throw new DeploymentException("Cannot load driver class, class name = " + driverClassName + ", nested exception \"" + ex.getMessage() + '"');
        }
        // DriverDataSource
        DataSource driverDS = initDriverDataSource(dsName, url, user, password, initSql);
        driverDS.setLoginTimeout(loginTimeout);
        // ConnectionPoolDataSource
        ConnectionPoolDataSourceImpl poolDS = new ConnectionPoolDataSourceImpl(driverDS);
        poolDS.setLoginTimeout(loginTimeout);
        // ConnectionPoolWithQueue
        ConnectionPoolWithQueue pooledDS = new ConnectionPoolWithQueue(poolDS, poolSizeLimit);
        pooledDS.setLoginTimeout(loginTimeout);
        if (fillPool) {
            fillConnectionPool(pooledDS, poolSizeLimit);
        }
        return useJTA ?
                initXADataSource(dsName, description, transactionTimeout, isolationLevel, pooledDS) :
                pooledDS;
    }

    /**
     * Initializes driver data source.
     *
     * @param dsName DataSource property prefix
     * @param url database URL
     * @param user userid
     * @param password user's password
     */
    private DataSource initDriverDataSource(String dsName, String url, String user, String password, String initSql
                                            //String eRetries,
                                            //String euRetries,
                                            //String eqRetries
                                            ) {
        // added by bbb
        // modified by Igork 1 Jan 2001
        try {
            //DriverDataSource dds = new DriverDataSource();
            Class ddsClass = Class.forName("com.azoft.nusuth.jdbc.pool.driver.DriverDataSource", true, Thread.currentThread().getContextClassLoader());
            DataSource dds = (DataSource) ddsClass.newInstance();
            /*
            java.util.Hashtable s = new java.util.Hashtable();
            s.put("appendeers", logger.getAllAppenders());
            s.put("priority", logger.getChainedPriority());
            ids.setLog4jSettings(s);*/

            //dds.setUrl(url);
            ddsClass.getMethod("setUrl", new Class[]{String.class}).invoke(dds, new Object[]{url});

            if (user != null && user.length() > 0) {
                //dds.setUser(user);
                ddsClass.getMethod("setUser", new Class[]{String.class}).invoke(dds, new Object[]{user});
                if (password != null && password.length() > 0) {
                    //dds.setPassword(password);
                    ddsClass.getMethod("setPassword", new Class[]{String.class}).invoke(dds, new Object[]{password});
                }
            }

/*
        ids = (user != null && password != null && user.length() > 0)
            ? new DriverDataSource(url, user, password)
            : new DriverDataSource(url);
*/
            if ((initSql != null && initSql.length() > 0) /*|| eRetries != null || euRetries != null || eqRetries != null*/) {
                DataSourceAdapter dsa = new DataSourceAdapter(dds);

                if (initSql != null) {
                    dsa.setConnectionInitSql(initSql);
                }
                /*
                 // eRetries
                 if(eRetries != null)
                 {
                 String[] states = getComplexProperty(prp, PROPERTY_PREFIX + dsName + ".statement.execute.states");
                 for(int i = 0 ; i < states.length ; i++)
                 {
                 String[] ecs = getComplexProperty(prp, PROPERTY_PREFIX + dsName + ".statement.execute.errorCodes." + states[i]);
                 int[] errorCodes = new int[ecs.length];
                 for(int j = 0 ; j < errorCodes.length ; j++)
                 {
                 errorCodes[j] = Integer.parseInt(ecs[j]);
                 }
                 dsa.addExecuteState(states[i], errorCodes);
                 }
                 dsa.setExecuteRetries(Integer.parseInt(eRetries));
                 }
                 // euRetries
                 if(euRetries != null)
                 {
                 String[] states = getComplexProperty(prp, PROPERTY_PREFIX + dsName + ".statement.executeUpdate.states");
                 for(int i = 0 ; i < states.length ; i++)
                 {
                 String[] ecs = getComplexProperty(prp, PROPERTY_PREFIX + dsName + ".statement.executeUpdate.errorCodes." + states[i]);
                 int[] errorCodes = new int[ecs.length];
                 for(int j = 0 ; j < errorCodes.length ; j++)
                 {
                 errorCodes[j] = Integer.parseInt(ecs[j]);
                 }
                 dsa.addExecuteUpdateState(states[i], errorCodes);
                 }
                 dsa.setExecuteUpdateRetries(Integer.parseInt(euRetries));
                 }
                 // eqRetries
                 if(eqRetries != null)
                 {
                 String[] states = getComplexProperty(prp, PROPERTY_PREFIX + dsName + ".statement.executeQuery.states");
                 for(int i = 0 ; i < states.length ; i++)
                 {
                 String[] ecs = getComplexProperty(prp, PROPERTY_PREFIX + dsName + ".statement.executeQuery.errorCodes." + states[i]);
                 int[] errorCodes = new int[ecs.length];
                 for(int j = 0 ; j < errorCodes.length ; j++)
                 {
                 errorCodes[j] = Integer.parseInt(ecs[j]);
                 }
                 dsa.addExecuteQueryState(states[i], errorCodes);
                 }
                 dsa.setExecuteQueryRetries(Integer.parseInt(eqRetries));
                 }
                 */
                return dsa;
            } else {
                return dds;
            }
        } catch (Exception e) {
            logger.error("Cannot instantiate DataSource ", e);
            return null;
        }
    }

    /**
     * Init XA datasource.
     *
     * @param dsName DataSource property prefix
     * @param prp properties
     * @param ds non-transactional dataSource
     *
     * @throws Exception  if initialization erro occurs
     */
    private DataSource initXADataSource(String dsName, String description, String transactionTimeout, String isolationLevel, DataSource ds) throws SQLException {
        XADataSourceImpl xaDS = new XADataSourceImpl(ds);
        if (transactionTimeout != null && transactionTimeout.length() > 0) {
            xaDS.setTransactionTimeout(Integer.parseInt(transactionTimeout));
        }
        if (isolationLevel != null && isolationLevel.length() > 0) {
            xaDS.setIsolationLevel(Integer.parseInt(isolationLevel));
        }

        FLXADataSource flxads = new FLXADataSource(xaDS, ds);
        if (description != null && description.length() > 0) {
            flxads.setDescription(description);
        }
        flxads.setTransactionManager(Tyrex.getTransactionManager());

        if (logger.isDebugEnabled()) {
            logger.debug(dsName + " transctional datasource : " + flxads);
        }

        return flxads;
    }

    private void fillConnectionPool(DataSource ds, int poolSizeLimit) throws SQLException {
        logger.info("started filling pool");
        Connection cons[] = new Connection[poolSizeLimit];
        try {
            for (int i = 0; i < poolSizeLimit; i++) {
                logger.info("allocating connection: " + i);
                cons[i] = ds.getConnection();
            }
        } catch (SQLException ex) {
            logger.error("cannot preallocate connections", ex);
            for (int i = 0; i < poolSizeLimit; i++) {
                if (cons[i] != null) {
                    logger.info("freeing connection after error: " + i);
                    try {
                        cons[i].close();
                    } catch (Exception exx) {
                        logger.error("cannot free preallocated connection", exx);
                    }
                }
            }
            throw ex;
        }
        SQLException ex = null;
        logger.info("started to freeing connections");
        for (int i = 0; i < poolSizeLimit; i++) {
            if (cons[i] != null) {
                logger.info("freeing connection: " + i);
                try {
                    cons[i].close();
                } catch (SQLException exx) {
                    logger.error("cannot free preallocated connection", exx);
                    if (ex == null)
                        ex = exx;
                }
            }
        }
        if (ex != null)
            throw ex;
    }

    /**
     * This method loads ejb entries.
     * @param config web application config.
     * @param externalJndiContexts External contexts.
     * @param appSettings Context config from container.xml
     * @param rootContext root context.
     * @param logger Logger for error lgging.
     * @throws DeploymentException.
     * @throws NamingException.
     */
    private void loadEjbEntries(CompositeNusuthWebAppElement config,
                                ExternalContext[] externalJndiContexts,
                                CompositeNusuthWebAppElement appSettings,
                                Context rootContext, Category logger)
            throws DeploymentException, NamingException {
        Context ejbContext = (Context) rootContext.lookup("comp/env/ejb");
        try {
            for (Enumeration refEnum = config.getCompositeChild("ejb-ref");
                 refEnum.hasMoreElements();) {
                CompositeNusuthWebAppElement beanNode
                        = (CompositeNusuthWebAppElement) refEnum.nextElement();
                String name = ManagementUtil.getSimpleString(beanNode,
                        "ejb-ref-name");
                String homeClassName = ManagementUtil.getSimpleString(beanNode,
                        "home");
                loadBean(name, homeClassName, externalJndiContexts, ejbContext,
                        logger);
            }
            for (Enumeration refEnum = config.getCompositeChild("ejb-local-ref");
                 refEnum.hasMoreElements();) {
                CompositeNusuthWebAppElement beanNode
                        = (CompositeNusuthWebAppElement) refEnum.nextElement();
                String name = ManagementUtil.getSimpleString(beanNode,
                        "ejb-ref-name");
                String homeClassName
                        = ManagementUtil.getSimpleString(beanNode, "local-home");
                loadBean(name, homeClassName, externalJndiContexts,
                        ejbContext, logger);
            }
        } catch (DeploymentException dex) {
        }
    }

    private void loadBean(String beanName, String homeClassName, ExternalContext[] externalContexts, Context ejbContext, Category logger) {
        try {
            //Class homeClass = Class.forName(homeClassName, true, Thread.currentThread().getContextClassLoader());
            for (int i = 0; i < externalContexts.length; i++) {
                String lookupName = createJndiName(externalContexts[i].root, beanName);
                try {
                    Object homeObj = externalContexts[i].context.lookup(lookupName);
                    //Object beanHome = PortableRemoteObject.narrow(homeObj, homeClass); //ClassCastException
                    ejbContext.bind(createInternalJndiName(beanName), homeObj);
                    logger.debug("Bean \"" + beanName + "\"(" + homeClassName + ") binded");// into context \""+ejbContext.getNameInNamespace()+'"');
                    return;
                } catch (NamingException nex) {
                    logger.error("Couldn't find EJBean \"" + beanName + "\"", nex);
                } catch (ClassCastException ccex) {
                    logger.error("EJBean \"" + beanName + "\" is incorrect", ccex);
                }
            }
            /*} catch (ClassNotFoundException cnfex) {
             cat.error("Couldn't instantiate bean \""+beanName+"\"", cnfex);*/
        } catch (Throwable t) {
            logger.error("Couldn't instantiate bean \"" + beanName + "\"", t);
        }
    }

    private void loadResourceEntries(CompositeNusuthWebAppElement config, CompositeNusuthWebAppElement appSettings, ExternalContext[] externalContexts, HashMap externalJdbcConnects, Context rootContext, Category logger, boolean useJTA) throws DeploymentException, NamingException {
        Context compContext = (Context) rootContext.lookup("comp");
        if (useJTA)
            bind(compContext, USER_TRANSACTION_NAME, Tyrex.getUserTransaction());
        Context envContext = (Context) compContext.lookup("env");

        for (Enumeration links = config.getCompositeChild("resource-ref"); links.hasMoreElements();) {
            CompositeNusuthWebAppElement link = (CompositeNusuthWebAppElement) links.nextElement();
            String name = ManagementUtil.getSimpleString(link, "res-ref-name");
            String type = ManagementUtil.getSimpleString(link, "res-type");
            boolean authIsContainer = ManagementUtil.getSimpleString(link, "res-auth").equalsIgnoreCase("Container");
            try {
                Class resClass = Class.forName(type, true, Thread.currentThread().getContextClassLoader());
                if (type.equals("javax.sql.DataSource"))
                    loadJdbcResource(name, type, authIsContainer, externalContexts, externalJdbcConnects, envContext, logger);
                else if (type.equals("javax.jms.QueueConnectionFactory"))
                    loadJmsResource(name, type, authIsContainer, externalContexts, envContext);
                else if (type.equals("javax.mail.Session"))
                    loadMailResource(name, type, authIsContainer, externalContexts, envContext);
                else if (type.equals("java.net.URL"))
                    loadUrlResource(name, type, authIsContainer, externalContexts, envContext);
                else
                    logger.error("Unknow resurce reference type \"" + type + "\" for resorce reference named \"" + name + "\".");
            } catch (ClassNotFoundException cnfex) {
                logger.error("Couldn't load resource factory \"" + name + "\"", cnfex);
            }
        }
    }

    private void bind(Context context, String bindName, Object bindObj) throws NamingException {
        CompositeName name = new CompositeName(bindName);
        if (name.size() > 0) {
            Context ctx = context;
            for (int i = 0; i < name.size() - 1; i++) {
                ctx = ctx.createSubcontext(name.get(i));
            }
            ctx.bind(name.get(name.size() - 1), bindObj);
            logger.debug(bindName + " binded into naming context");
        }
    }

    private void loadJdbcResource(String name, String type, boolean authIsContainer, ExternalContext[] externalContexts, HashMap externalJdbcConnects, Context envContext, Category logger) throws NamingException {
        String bindName = createInternalJndiName(name);
        Context jdbcContext = (Context) envContext.lookup("jdbc");

        Object bindObj = externalJdbcConnects.get(name);
        if (bindObj == null) {
            for (int i = 0; i < externalContexts.length; i++) {
                String lookupName = createJndiName(externalContexts[i].root, bindName);
                try {
                    bindObj = (DataSource) externalContexts[i].context.lookup(lookupName);
                    if (bindObj == null) {
                        lookupName = createJndiName(externalContexts[i].root, bindName);
                        logger.error("Couldn't connect to \"" + lookupName + "\"");
                        continue;
                    }
                    break;
                } catch (Throwable t) {
                    if (!(t instanceof NamingException))
                        logger.error("Couldn't connect to \"" + name + "\" jdbc datasource", t);
                }
            }
        }
        if (bindObj != null)
            bind(jdbcContext, bindName, bindObj);
        else
            logger.error("Couldn't connect to \"" + name + "\" jdbc datasource");
    }

    private void loadJmsResource(String name, String type, boolean authIsContainer, ExternalContext[] externalContexts, Context envContext) {
    }

    private void loadMailResource(String name, String type, boolean authIsContainer, ExternalContext[] externalContexts, Context envContext) {
    }

    private void loadUrlResource(String name, String type, boolean authIsContainer, ExternalContext[] externalContexts, Context envContext) {
    }

    private void loadRmiSecurityManager(CompositeNusuthWebAppElement appSettings) {
        Enumeration enum = null;
        try {
            enum = appSettings.getSimpleChild("external-rmi-codebase-url");
        } catch (DeploymentException e) {
            logger.error("context.dtd is broken", e);
        }
        try {
            if (enum.hasMoreElements()) {
                String external_rmi_codebase_url = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent().trim();
                File temporaryPolicyFile = createPolicyFile(external_rmi_codebase_url);
                System.setProperty("java.security.policy", temporaryPolicyFile.getAbsolutePath());
                System.setSecurityManager(new RMISecurityManager());
                logger.info("RmiSecurityManager started");
            } else {
                // do nothing
            }
        } catch (Exception e) {
            logger.warn("Couldn't load RmiSecurityManager", e);
        }
    }

    private File createPolicyFile(String external_rmi_codebase_url)
            throws Exception {
        final String policy = "grant {\n"
                + "  permission java.security.AllPermission;\n"
                + "  permission tyrex.tm.TyrexPermission \"server.start\","
                + "                                      \"server.start\";\n"
                + "  permission tyrex.naming.NamingPermission \"shared\";\n"
                + "  permission tyrex.naming.NamingPermission \"enc\";\n"
                + "  permission tyrex.tm.TyrexPermission \"manager\";\n"
                + "};\n"
                + "\n"
                + "grant "
                + (external_rmi_codebase_url.length() > 0
                ? "codeBase \"" + external_rmi_codebase_url + "\" "
                : "")
                + "{\n"
                + "  permission java.security.AllPermission;\n"
                + "};\n";
        File temporaryPolicyFile = File.createTempFile("NusuthTempFile", "policy");
        OutputStream ostream = new FileOutputStream(temporaryPolicyFile);
        ostream.write(policy.getBytes());
        ostream.close();
        temporaryPolicyFile.deleteOnExit();
        logger.debug("Temporary policy file created on \""
                + temporaryPolicyFile.getAbsolutePath() + '"');
        return temporaryPolicyFile;
    }
}