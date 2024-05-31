package com.azoft.nusuth.jdbc.pool.driver;

import org.apache.log4j.Category;

import javax.sql.DataSource;

import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import javax.naming.*;

/**
 * {@link DataSource} adapter.
 * It initializes {@link Connection} with specific sql
 * (see {@link #setConnectionInitSql(String)}).
 * Returns {@link ConnectionProxy} in <code>getConnection()</code> methods.
 *
 * @see ConnectionProxy
 * @see StatementProxy
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @version 0.5.5
 * @since 0.3.7
 */
public class DataSourceAdapter implements DataSource {
    /**
     * category to log
     */
    private final static Category CAT = Category.getInstance(DataSourceAdapter.class.getName());
    /**
     * name of datasource on jndi
     */
    private String dataSourceName;
    /**
     * underlying data source
     */
    private DataSource ds;
    /**
     * init SQL for connection
     */
    private String connectionInitSql;
    /**
     * execute retries
     */
    private int executeRetries;
    /**
     * execute exception states: map from state(String) to errorCodes(int[])
     */
    private final Map executeStates = new HashMap(10);
    /**
     * executeUpdate retries
     */
    private int executeUpdateRetries;
    /**
     * executeUpdate exception states: map from state(String) to errorCodes(int[])
     */
    private final Map executeUpdateStates = new HashMap(10);
    /**
     * executeQuery retries
     */
    private int executeQueryRetries;
    /**
     * executeQuery exception states: map from state(String) to errorCodes(int[])
     */
    private final Map executeQueryStates = new HashMap(10);

    //---------------------------------------------------------------------------
    //----- constructors --------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Constructs empty object.
     *
     * @since 0.5.0
     */
    public DataSourceAdapter() {
    }


    /**
     * @param ds underlying data source
     */
    public DataSourceAdapter(DataSource ds) {
        this.ds = ds;
        if (ds == null) {
            throw new IllegalArgumentException("dataSource is null");
        }
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Gets data source name.
     *
     * @return data source name.
     *
     * @since 0.5.0
     */
    public String getDataSourceName() {
        return dataSourceName;
    }


    /**
     * Sets data source name.
     *
     * @param newValue data source name.
     *
     * @since 0.5.0
     */
    public void setDataSourceName(String newValue) {
        dataSourceName = newValue;
    }


    /**
     * Gets data source.
     *
     * @return data source.
     *
     * @since 0.5.0
     */
    public DataSource getDataSource() {
        if (ds == null && dataSourceName != null) {
            try {
                ds = (DataSource) new InitialContext().lookup(dataSourceName);
                if (ds == null) {
                    CAT.error("Cannot locate object with name " + dataSourceName);
                }
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                CAT.error("Cannot locate object with name " + dataSourceName, ex);
                throw new RuntimeException("Cannot locate object with name " + dataSourceName + " : " + ex);
            }
        }
        return ds;
    }


    /**
     * Sets data source.
     *
     * @param newValue data source value.
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
     * Sets states to retry <code>executeUpdate() Statement</code> operations.
     *
     * @param rfi states info.
     *
     * @since 0.5.0
     */
    public void setExecuteUpdateRetryStates(RetryFilterInfo[] rfi) {
        executeUpdateStates.clear();
        for (int i = 0; i < rfi.length; i++) {
            addExecuteUpdateState(rfi[i].getState(), rfi[i].getCodes());
        }
    }


    /**
     * Gets states to retry <code>executeUpdate() Statement</code> operations.
     *
     * @return states info.
     *
     * @since 0.5.0
     */
    public RetryFilterInfo[] getExecuteUpdateRetryStates() {
        Set es = executeUpdateStates.entrySet();
        RetryFilterInfo[] rc = new RetryFilterInfo[es.size()];
        int i = 0;
        for (Iterator esi = es.iterator(); esi.hasNext();) {
            Map.Entry e = (Map.Entry) esi.next();
            rc[i] = new RetryFilterInfo();
            rc[i].setState((String) e.getKey());
            rc[i].setCodes((int[]) e.getValue());
            i++;
        }
        return rc;
    }


    /**
     * Sets states to retry <code>execute() Statement</code> operations.
     *
     * @param rfi states info.
     *
     * @since 0.5.0
     */
    public void setExecuteRetryStates(RetryFilterInfo[] rfi) {
        executeStates.clear();
        for (int i = 0; i < rfi.length; i++) {
            addExecuteState(rfi[i].getState(), rfi[i].getCodes());
        }
    }


    /**
     * Gets states to retry <code>execute() Statement</code> operations.
     *
     * @return states info.
     *
     * @since 0.5.0
     */
    public RetryFilterInfo[] getExecuteRetryStates() {
        Set es = executeStates.entrySet();
        RetryFilterInfo[] rc = new RetryFilterInfo[es.size()];
        int i = 0;
        for (Iterator esi = es.iterator(); esi.hasNext();) {
            Map.Entry e = (Map.Entry) esi.next();
            rc[i] = new RetryFilterInfo();
            rc[i].setState((String) e.getKey());
            rc[i].setCodes((int[]) e.getValue());
            i++;
        }
        return rc;
    }


    /**
     * Sets states to retry <code>executeQuery() Statement</code> operations.
     *
     * @param rfi states info.
     *
     * @since 0.5.0
     */
    public void setExecuteQueryRetryStates(RetryFilterInfo[] rfi) {
        executeQueryStates.clear();
        for (int i = 0; i < rfi.length; i++) {
            addExecuteQueryState(rfi[i].getState(), rfi[i].getCodes());
        }
    }


    /**
     * Gets states to retry <code>executeQuery() Statement</code> operations.
     *
     * @return states info.
     *
     * @since 0.5.0
     */
    public RetryFilterInfo[] getExecuteQueryRetryStates() {
        Set es = executeQueryStates.entrySet();
        RetryFilterInfo[] rc = new RetryFilterInfo[es.size()];
        int i = 0;
        for (Iterator esi = es.iterator(); esi.hasNext();) {
            Map.Entry e = (Map.Entry) esi.next();
            rc[i] = new RetryFilterInfo();
            rc[i].setState((String) e.getKey());
            rc[i].setCodes((int[]) e.getValue());
            i++;
        }
        return rc;
    }
    //---------------------------------------------------------------------------
    //---- helpers --------------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Initilizes connection with {@link #connectionInitSql}.
     *
     * @param con connection to initialize.
     */
    private void initConnection(Connection con) {
        if (connectionInitSql == null) {
            return;  // nothing to do
        }

        String sql = connectionInitSql;
        try {
            Statement stmt = con.createStatement();
            try {
                stmt.execute(sql);
                if (!con.getAutoCommit()) {
                    con.commit();
                }
                CAT.info("Connection init sql was executed, sql = " + sql);
            } finally {
                stmt.close();
            }
        } catch (SQLException exc) {
            CAT.error("Cannot execute connection init sql " + sql, exc);
        }
    }

    //---------------------------------------------------------------------------

    /**
     * Determines if it needs to retry.
     *
     * @param exc exception to test.
     * @param map map: state to error codes.
     * @param category category to log.
     * @param time current try.
     * @param timeLimit tries limit.
     *
     * @return <code>true</code> if it needs to repeat.
     */
    private boolean isRetryException(SQLException exc, Map map, Category category, int time, int timeLimit) {
        if (map == null) // no registred SQLStates
        {
            return false;
        }
        if (time >= timeLimit) // limit exceeded
        {
            return false;
        }

        String state = exc.getSQLState();
        int[] errorCodes = (int[]) map.get(state);
        if (errorCodes == null) // state is not registered
        {
            return false;
        }
        int errorCode = exc.getErrorCode();
        for (int i = 0; i < errorCodes.length; i++) {
            if (errorCodes[i] == errorCode) {
                if (category.isDebugEnabled()) {
                    category.debug("Retry(" + time + ") to execute due exception(state=" + state +
                            ", errorCode=" + errorCode + ") ", exc);
                } else if (category.isInfoEnabled()) {
                    category.info("Retry(" + time + ") to execute due exception(state=" + state +
                            ", errorCode=" + errorCode + ") " + exc);
                }
                return true;
            }
        }

        return false;  // error code is not registered for this state
    }


    /**
     * Returns <code>true</code> if <code>exc</code> has SQLState and errorCode
     * that are registered for exceptions to retry
     * <code>execute</code> operation.
     *
     * @param exc exception to test.
     * @param category category to log.
     * @param time current try.
     *
     * @return <code>true</code> if it needs to repeat.
     */
    boolean isExecuteRetryException(SQLException exc, Category category, int time) {
        return isRetryException(exc, executeStates, category, time, getExecuteRetries());
    }


    /**
     * Returns <code>true</code> if <code>exc</code> has SQLState and errorCode
     * that are registered for exceptions to retry
     * <code>executeUpdate</code> operation.
     *
     * @param exc exception to test.
     * @param category category to log.
     * @param time current try.
     *
     * @return <code>true</code> if it needs to repeat.
     */
    boolean isExecuteUpdateRetryException(SQLException exc, Category category, int time) {
        return isRetryException(exc, executeUpdateStates, category, time, getExecuteUpdateRetries());
    }


    /**
     * Returns <code>true</code> if <code>exc</code> has SQLState and errorCode
     * that are registered for exceptions to retry
     * <code>executeQuery</code> operation.
     *
     * @param exc exception to test.
     * @param category category to log.
     * @param time current try.
     *
     * @return <code>true</code> if it needs to repeat.
     */
    boolean isExecuteQueryRetryException(SQLException exc, Category category, int time) {
        return isRetryException(exc, executeQueryStates, category, time, getExecuteQueryRetries());
    }

    //---------------------------------------------------------------------------
    //---- tuning ---------------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Gets SQL for connection initialization.
     *
     * @return SQL for connection initialization.
     */
    public String getConnectionInitSql() {
        return connectionInitSql;
    }


    /**
     * Sets SQL to init connection.
     * This SQL is executed in <code>getConnection()</code> methods.
     *
     * @param sql SQL for connection initialization.
     */
    public void setConnectionInitSql(String sql) {
        connectionInitSql = sql;
    }


    /**
     * Gets retries count for <code>execute</code> method in statement.
     *
     * @return retries count value for <code>execute</code> method in statement.
     */
    public int getExecuteRetries() {
        return executeRetries;
    }


    /**
     * Sets retries count for <code>execute</code> method in statement.
     *
     * @param times retries count value for <code>execute</code> method in statement.
     */
    public void setExecuteRetries(int times) {
        executeRetries = times;
    }


    /**
     * Adds <code>state</code> with <code>errorCodes</code> for
     * <code>execute</code> method in statement.
     *
     * @param state state to be added to the states for <code>execute</code>.
     * @param errorCodes error codes for the <code>state</code>.
     */
    public void addExecuteState(String state, int[] errorCodes) {
        executeStates.put(state, errorCodes);
    }


    /**
     * Gets retries count for <code>executeUpdate</code> method in statement.
     *
     * @return retries count value for <code>executeUpdate</code> method in statement.
     */
    public int getExecuteUpdateRetries() {
        return executeUpdateRetries;
    }


    /**
     * Sets retries count for <code>executeUpdate</code> method in statement.
     *
     * @param times retries count value for <code>executeUpdate</code> method in statement.
     */
    public void setExecuteUpdateRetries(int times) {
        executeUpdateRetries = times;
    }


    /**
     * Adds <code>state</code> with <code>errorCodes</code> for
     * <code>executeUpdate</code> method in statement.
     *
     * @param state state to be added to the states for <code>executeUpdate</code>.
     * @param errorCodes error codes for the <code>state</code>.
     */
    public void addExecuteUpdateState(String state, int[] errorCodes) {
        executeUpdateStates.put(state, errorCodes);
    }


    /**
     * Gets retries count for <code>executeQuery</code> method in statement.
     *
     * @return retries count value for <code>executeQuery</code> method in statement.
     */
    public int getExecuteQueryRetries() {
        return executeQueryRetries;
    }


    /**
     * Sets retries count for <code>executeQuery</code> method in statement.
     *
     * @param times retries count value for <code>executeQuery</code> method in statement.
     */
    public void setExecuteQueryRetries(int times) {
        executeQueryRetries = times;
    }


    /**
     * Adds <code>state</code> with <code>errorCodes</code> for
     * <code>executeQuery</code> method in statement.
     *
     * @param state state to be added to the states for <code>executeQuery</code>.
     * @param errorCodes error codes for the <code>state</code>.
     */
    public void addExecuteQueryState(String state, int[] errorCodes) {
        executeQueryStates.put(state, errorCodes);
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * Returns {@link ConnectionProxy} over connection from underlying data source.
     *
     * @return {@link ConnectionProxy} over connection from underlying data source.
     *
     * @throws SQLException if a database access error occurs.
     */
    public Connection getConnection() throws SQLException {
        Connection con = getDataSource().getConnection();
        initConnection(con);
        return new ConnectionProxy(con, this);
    }


    /**
     * Returns {@link ConnectionProxy} over connection from underlying data source.
     *
     * @param user user name.
     * @param password password.
     *
     * @return {@link ConnectionProxy} over connection from underlying data source.
     *
     * @throws SQLException if a database access error occurs.
     */
    public Connection getConnection(String user, String password) throws SQLException {
        Connection con = getDataSource().getConnection(user, password);
        initConnection(con);
        return new ConnectionProxy(con, this);
    }


    /**
     * Delegates to underlying data source.
     *
     * @return login timeout value.
     *
     * @throws SQLException if a database access error occurs.
     */
    public int getLoginTimeout() throws SQLException {
        return getDataSource().getLoginTimeout();
    }


    /**
     * Delegates to underlying data source.
     *
     * @param timeout login timeout value.
     *
     * @throws SQLException if a database access error occurs.
     */
    public void setLoginTimeout(int timeout) throws SQLException {
        getDataSource().setLoginTimeout(timeout);
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
}

