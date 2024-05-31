package com.azoft.nusuth.jdbc.pool.driver;

import org.apache.log4j.Category;

import javax.sql.DataSource;

import java.io.PrintWriter;

import java.util.Properties;
import java.util.Enumeration;

import java.sql.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Data source over {@link DriverManager}.
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @version 0.5.5
 * @since 1.1
 */
public class DriverDataSource implements DataSource {
    /**
     * category to log.
     */
    private final static Category CAT = Category.getInstance(DriverDataSource.class.getName());
    /**
     * class name for the driver.
     *
     * @since 0.5.0
     */
    private String driverClassName;
    /**
     * a database url.
     */
    private String url;
    /**
     * user.
     */
    private String user;
    /**
     * password.
     */
    private String password;
    /**
     * info to connection.
     */
    private Properties properties;

    //---------------------------------------------------------------------------
    //------ constructors -------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Constructs empty object. Use <code>setXXX</code> methods to set properties.
     *
     * @since 0.5.0
     */
    public DriverDataSource() {
    }


    /**
     * @param url a database url.
     *
     * @see DriverManager#getConnection(String)
     */
    public DriverDataSource(String url) {
        this(url, null);
    }


    /**
     * @param url a database url
     * @param user a database user
     * @param password user's password
     *
     * @see DriverManager#getConnection(String,String,String)
     */
    public DriverDataSource(String url, String user, String password) {
        if (url == null) {
            throw new IllegalArgumentException("URL is null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user is null");
        }

        this.url = url;
        this.user = user;
        this.password = password;
        this.properties = null;
    }


    /**
     * @param url a database url
     * @param info a list of arbitrary string tag/value pairs as
     * connection arguments.
     *
     * @see DriverManager#getConnection(String,Properties)
     */
    public DriverDataSource(String url, Properties info) {
        if (url == null) {
            throw new IllegalArgumentException("URL is null");
        }

        this.url = url;
        this.properties = info;
        this.user = this.password = null;
    }

    //---------------------------------------------------------------------------
    //---- public part ----------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Delegates to {@link DriverManager#getConnection(String)} or if this object
     * was initialized with not <code>null</code> properties
     * (see {@link #DriverDataSource(String url, Properties info)})
     * then it delegates to
     * {@link DriverManager#getConnection(String,Properties)}.
     *
     * @return the allocated connection.
     *
     * @throws SQLException if a database access error occurs.
     */
    public Connection getConnection() throws SQLException {
        if (CAT.isInfoEnabled()) {
            CAT.info("DriverManager.getConnection(), dbURL = " + url);
        }

        Connection con;
        if (user != null) {
            con = DriverManager.getConnection(url, user, password);

            if (CAT.isDebugEnabled()) {
                CAT.debug("DriverManager.getConnection(String,String,String) finished, dbURL = " + url +
                        ", user = " + user + ", password = " + password);
            }
        } else if (properties != null) {
            con = DriverManager.getConnection(url, properties);

            if (CAT.isDebugEnabled()) {
                CAT.debug("DriverManager.getConnection(String,Properties) finished, dbURL = " + url);
            }
        } else {
            try {
                con = DriverManager.getConnection(url);
            } catch (Throwable e) {
                throw new SQLException("Couldn't get connection from driverManager, nested: " + e.getMessage());
            }
            //con = DriverManager.getConnection(url);

            if (CAT.isDebugEnabled()) {
                CAT.debug("DriverManager.getConnection(String) finished, dbURL = " + url);
            }
        }

        return con;
    }


    /**
     * Delegates to {@link DriverManager#getConnection(String,String,String)}.
     *
     * @param username user name.
     * @param password password.
     *
     * @return the allocated connection.
     *
     * @throws SQLException if a database access error occurs.
     */
    public Connection getConnection(String username, String password) throws SQLException {
        if (CAT.isInfoEnabled()) {
            CAT.info("DriverManager.getConnection(String,String,String), dbURL = " + url);
        }

        Connection connection = DriverManager.getConnection(url, username, password);

        if (CAT.isDebugEnabled()) {
            CAT.debug("DriverManager.getConnection(String,String,String) finished, dbURL = " + url);
        }

        return connection;
    }


    /**
     * Returns <code>null</code>.
     *
     * @return <code>null</code>.
     */
    public PrintWriter getLogWriter() {
        return null;
    }


    /**
     * Does not set log writer as log4j logging is used and it also
     * writes to <code>writer</code> log4j category name.
     *
     * @param writer Log Writer.
     */
    public void setLogWriter(PrintWriter writer) {
        if (writer != null) {
            writer.println("Use log4j \"" + CAT.getName() + "\" category instead of setLogWriter(PrintWriter)");
            writer.flush();
        }
    }


    /**
     * Delegates to {@link DriverManager#getLoginTimeout()}.
     *
     * @return {@link DriverManager#getLoginTimeout()}.
     *
     * @throws SQLException if a database access error occurs.
     */
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }


    /**
     * Delegates to {@link DriverManager#setLoginTimeout(int)}.
     *
     * @param timeout {@link DriverManager#setLoginTimeout(int)}.
     *
     * @throws SQLException if a database access error occurs.
     */
    public void setLoginTimeout(int timeout) throws SQLException {
        DriverManager.setLoginTimeout(timeout);
    }


    /**
     * get class name for the driver.
     *
     * @return driver class name.
     *
     * @since 0.5.0
     */
    public String getDriverClassName() {
        return driverClassName;
    }


    /**
     * Set class name for the driver.
     *
     * @param newValue driver class name.
     *
     * @since 0.5.0
     */
    public void setDriverClassName(String newValue) {
        try {
            Class.forName(newValue).newInstance();

            driverClassName = newValue;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid class name is specified " + newValue + " : " + ex);
        }
    }


    /**
     * Get a database url.
     *
     * @return database url value.
     *
     * @since 0.5.0
     */
    public String getUrl() {
        return url;
    }


    /**
     * Set a database url.
     *
     * @param newValue url value.
     *
     * @since 0.5.0
     */
    public void setUrl(String newValue) {
        url = newValue;
    }


    /**
     * Get user.
     *
     * @return user value.
     *
     * @since 0.5.0
     */
    public String getUser() {
        return user;
    }


    /**
     * Set user.
     *
     * @param newValue user value.
     *
     * @since 0.5.0
     */
    public void setUser(String newValue) {
        user = newValue;
    }


    /**
     * Get password.
     *
     * @return password value.
     *
     * @since 0.5.0
     */
    public String getPassword() {
        return password;
    }


    /**
     * Set a password for user
     *
     * @param newValue password value.
     *
     * @since 0.5.0
     */
    public void setPassword(String newValue) {
        password = newValue;
    }
}

