package com.azoft.nusuth.jdbc.pool.driver;

import org.apache.log4j.Category;

import java.util.Map;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.sql.SQLException;

/**
 * {@link Statement} proxy.
 * It has functionality to retry
 * {@link #execute(String)},
 * {@link #executeUpdate(String)},
 * {@link #executeQuery(String)}
 * (see
 * {@link DataSourceAdapter#getExecuteRetries()},
 * {@link DataSourceAdapter#getExecuteUpdateRetries()},
 * {@link DataSourceAdapter#getExecuteQueryRetries()}
 * )
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @version 0.5.5
 * @since 0.3.7
 */
public class StatementProxy implements Statement {
    /** category to log */
    private static final Category CAT = Category.getInstance(StatementProxy.class.getName());
    /** underlying statement */
    protected final Statement stmt;
    /** data source */
    protected final DataSourceAdapter ds;

    //---------------------------------------------------------------------------
    //---- constructors ---------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * @param stmt underlying statement
     */
    public StatementProxy(Statement stmt, DataSourceAdapter ds) {
        this.stmt = stmt;
        this.ds = ds;
        if (stmt == null) {
            throw new IllegalArgumentException("statement is null");
        }
        if (ds == null) {
            throw new IllegalArgumentException("dataSource is null");
        }
    }

    //---------------------------------------------------------------------------
    //------------- helpers -----------------------------------------------------
    //---------------------------------------------------------------------------

    protected boolean isExecuteRetryException(SQLException exc, Category category, int time) {
        return ds.isExecuteRetryException(exc, category, time);
    }

    protected boolean isExecuteUpdateRetryException(SQLException exc, Category category, int time) {
        return ds.isExecuteUpdateRetryException(exc, category, time);
    }

    protected boolean isExecuteQueryRetryException(SQLException exc, Category category, int time) {
        return ds.isExecuteQueryRetryException(exc, category, time);
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    public boolean execute(String sql) throws SQLException {
        for (int i = 0; true; i++) {
            try {
                return stmt.execute(sql);
            } catch (SQLException exc) {
                if (!isExecuteRetryException(exc, CAT, i)) {
                    throw exc;
                }
            }
        }
    }

    public int executeUpdate(String sql) throws SQLException {
        for (int i = 0; true; i++) {
            try {
                return stmt.executeUpdate(sql);
            } catch (SQLException exc) {
                if (!isExecuteUpdateRetryException(exc, CAT, i)) {
                    throw exc;
                }
            }
        }
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        for (int i = 0; true; i++) {
            try {
                return stmt.executeQuery(sql);
            } catch (SQLException exc) {
                if (!isExecuteQueryRetryException(exc, CAT, i)) {
                    throw exc;
                }
            }
        }
    }


    public void close() throws SQLException {
        stmt.close();
    }

    public int getMaxFieldSize() throws SQLException {
        return stmt.getMaxFieldSize();
    }

    public void setMaxFieldSize(int max) throws SQLException {
        stmt.setMaxFieldSize(max);
    }

    public int getMaxRows() throws SQLException {
        return stmt.getMaxRows();
    }

    public void setMaxRows(int max) throws SQLException {
        stmt.setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable) throws SQLException {
        stmt.setEscapeProcessing(enable);
    }

    public int getQueryTimeout() throws SQLException {
        return stmt.getQueryTimeout();
    }

    public void setQueryTimeout(int seconds) throws SQLException {
        stmt.setQueryTimeout(seconds);
    }

    public void cancel() throws SQLException {
        stmt.cancel();
    }

    public SQLWarning getWarnings() throws SQLException {
        return stmt.getWarnings();
    }

    public void clearWarnings() throws SQLException {
        stmt.clearWarnings();
    }

    public void setCursorName(String name) throws SQLException {
        stmt.setCursorName(name);
    }

    public ResultSet getResultSet() throws SQLException {
        return stmt.getResultSet();
    }

    public int getUpdateCount() throws SQLException {
        return stmt.getUpdateCount();
    }

    public boolean getMoreResults() throws SQLException {
        return stmt.getMoreResults();
    }

    public void setFetchDirection(int direction) throws SQLException {
        stmt.setFetchDirection(direction);
    }

    public int getFetchDirection() throws SQLException {
        return stmt.getFetchDirection();
    }

    public void setFetchSize(int rows) throws SQLException {
        stmt.setFetchSize(rows);
    }

    public int getFetchSize() throws SQLException {
        return stmt.getFetchSize();
    }

    public int getResultSetConcurrency() throws SQLException {
        return stmt.getResultSetConcurrency();
    }

    public int getResultSetType() throws SQLException {
        return stmt.getResultSetType();
    }

    public void addBatch(String sql) throws SQLException {
        stmt.addBatch(sql);
    }

    public void clearBatch() throws SQLException {
        stmt.clearBatch();
    }

    public int[] executeBatch() throws SQLException {
        return stmt.executeBatch();
    }

    public Connection getConnection() throws SQLException {
        return stmt.getConnection();
    }
}
