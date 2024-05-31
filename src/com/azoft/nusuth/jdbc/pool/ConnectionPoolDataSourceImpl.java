package com.azoft.nusuth.jdbc.pool;

import org.apache.log4j.Category;

import javax.sql.PooledConnection;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import java.sql.SQLException;

import java.io.PrintWriter;

/**
 * {@link ConnectionPoolDataSource} implementation.
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @version 0.5.5
 * @since 0.3.0
 */
public class ConnectionPoolDataSourceImpl implements ConnectionPoolDataSource {
    /**
     * category to log
     */
    private final static Category CAT = Category.getInstance(ConnectionPoolDataSourceImpl.class.getName());

    /**
     * underlying data source
     */
    private DataSource ds;

    //---------------------------------------------------------------------------
    //---- constructors ---------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Constructs empty object.
     *
     * @since 0.5.0
     */
    public ConnectionPoolDataSourceImpl() {
    }


    /**
     * Constructor
     *
     * @param ds underlying data source.
     */
    public ConnectionPoolDataSourceImpl(DataSource ds) {
        if (ds == null) {
            throw new IllegalArgumentException("the argument is null");
        }
        this.ds = ds;
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Gets underlying data source.
     *
     * @return underlying data source.
     *
     * @since 0.5.0
     */
    public DataSource getDataSource() {
        return ds;
    }


    /**
     * Sets underlying data source.
     *
     * @param newValue underlying data source.
     *
     * @since 0.5.0
     */
    public void setDataSource(DataSource newValue) {
        ds = newValue;
        if (ds == null) {
            throw new IllegalArgumentException("dataSource is null");
        }
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
     * @param writer log writer.
     */
    public void setLogWriter(PrintWriter writer) {
        if (writer != null) {
            writer.println("Use log4j \"" + CAT.getName() + "\" category instead of setLogWriter(PrintWriter)");
            writer.flush();
        }
    }


    /**
     * Delegates to the underlying data source.
     *
     * @return login timeout value of the underlying data source.
     *
     * @see DataSource#getLoginTimeout()
     *
     * @throws SQLException if a database access error occurs.
     */
    public int getLoginTimeout() throws SQLException {
        return ds.getLoginTimeout();
    }


    /**
     * Delegates to the underlying data source.
     *
     * @param timeout login timeout value.
     *
     * @see DataSource#setLoginTimeout(int)
     *
     * @throws SQLException if a database access error occurs.
     */
    public void setLoginTimeout(int timeout) throws SQLException {
        ds.setLoginTimeout(timeout);
    }


    /**
     * Gets the pooled connection over connection from the underlying data source.
     *
     * @return pooled connection.
     *
     * @throws SQLException if a database access error occurs.
     */
    public PooledConnection getPooledConnection() throws SQLException {
        return new PooledConnectionImpl(ds.getConnection());
    }


    /**
     * Gets the pooled connection over connection from the underlying data source.
     *
     * @param user user name.
     * @param password password value.
     *
     * @return pooled connection.
     *
     * @throws SQLException if a database access error occurs.
     */
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        return new PooledConnectionImpl(ds.getConnection(user, password));
    }
}

