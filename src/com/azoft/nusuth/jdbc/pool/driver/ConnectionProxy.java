package com.azoft.nusuth.jdbc.pool.driver;

import java.util.Map;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;


/**
 * Connection proxy.
 * Delegates all methods to underlying connection.
 * Creates statement proxy in
 * {@link #createStatement()},
 * {@link #prepareStatement(String sql)}, and
 * {@link #prepareCall(String sql)}.
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @version 0.5.5
 * @since 0.3.7
 */
public class ConnectionProxy implements Connection {
    /** underlying connection */
    private final Connection connection;
    /** data source */
    private final DataSourceAdapter ds;

    //---------------------------------------------------------------------------
    //---- constructors ---------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * @param connection underlying connection.
     */
    public ConnectionProxy(Connection connection, DataSourceAdapter dataSource) {
        this.connection = connection;
        this.ds = dataSource;
        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (ds == null) {
            throw new IllegalArgumentException("dataSource is null");
        }
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    public Statement createStatement() throws SQLException {
        return new StatementProxy(connection.createStatement(), ds);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new PreparedStatementProxy(connection.prepareStatement(sql), ds);
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return new CallableStatementProxy(connection.prepareCall(sql), ds);
    }

    public String nativeSQL(String sql) throws SQLException {
        return connection.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        connection.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    public void commit() throws SQLException {
        connection.commit();
    }

    public void rollback() throws SQLException {
        connection.rollback();
    }

    public void close() throws SQLException {
        connection.close();
    }

    public boolean isClosed() throws SQLException {
        return connection.isClosed();
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        connection.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
    }

    public void setCatalog(String catalog) throws SQLException {
        connection.setCatalog(catalog);
    }

    public String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    public void setTransactionIsolation(int level) throws SQLException {
        connection.setTransactionIsolation(level);
    }

    public int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
    }

    public SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        connection.clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return connection.createStatement(resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public Map getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    public void setTypeMap(Map map) throws SQLException {
        connection.setTypeMap(map);
    }
}
