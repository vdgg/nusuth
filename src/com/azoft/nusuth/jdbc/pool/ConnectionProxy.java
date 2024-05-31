package com.azoft.nusuth.jdbc.pool;

import org.apache.log4j.Category;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;

/**
 * Proxy of connection.
 * <p>
 * Instances of this class intercepts close event.
 * If during JDBC operations it has raised a {@link SQLException}
 * then on close event this proxy object fires
 * {@link DriverPooledConnection#fireConnectionErrorOccurred(SQLException)}.
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @since 0.3.0
 */
class ConnectionProxy implements Connection {
    /** category to log */
    private static final Category CAT = Category.getInstance(ConnectionProxy.class.getName());
    /** reference to the parent pooled connection */
    private final PooledConnectionImpl pooledConnection;
    /** reference to phisical SQL connection */
    private final Connection phisicalConnection;
    /** closed flag */
    private boolean closed = false;
    /** fatal error occurred flag */
    private SQLException fatalSQLException = null;

    public ConnectionProxy(PooledConnectionImpl pooledConnection, Connection phisicalConnection)
            throws SQLException {
        this.phisicalConnection = phisicalConnection;
        this.pooledConnection = pooledConnection;
        //setAutoCommit(false);
    }

    /**
     * Handle SQL exception.
     * Checks <code>exc</code> on fatal attributes.
     * @return <code>exc</code>.
     */
    protected SQLException handleException(SQLException exc) {
        if (fatalSQLException == null) {
            fatalSQLException = exc;
        }

        if (fatalSQLException != null) {
            if (CAT.isInfoEnabled()) {
                CAT.info("Connection: " + phisicalConnection +
                        " will be closed due SQLException: state=" + exc.getSQLState() +
                        ", error code=" + exc.getErrorCode(), exc);
            }
        }

        return exc;
    }

    /**
     * SQL statements without parameters are normally
     * executed using Statement objects. If the same SQL statement
     * is executed many times, it is more efficient to use a
     * PreparedStatement
     *
     * @return a new Statement object
     * @exception SQLException if a database-access error occurs.
     */
    public Statement createStatement() throws SQLException {
        try {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->createStatement");
            }

            return phisicalConnection.createStatement();
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * A SQL statement with or without IN parameters can be
     * pre-compiled and stored in a PreparedStatement object. This
     * object can then be used to efficiently execute this statement
     * multiple times.
     *
     * <P><B>Note:</B> This method is optimized for handling
     * parametric SQL statements that benefit from precompilation. If
     * the driver supports precompilation, prepareStatement will send
     * the statement to the database for precompilation. Some drivers
     * may not support precompilation. In this case, the statement may
     * not be sent to the database until the PreparedStatement is
     * executed.  This has no direct affect on users; however, it does
     * affect which method throws certain SQLExceptions.
     *
     * @param sql a SQL statement that may contain one or more '?' IN
     * parameter placeholders
     * @return a new PreparedStatement object containing the
     * pre-compiled statement
     * @exception SQLException if a database-access error occurs.
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        try {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->prepareStatement, sql = " + sql);
            }

            return phisicalConnection.prepareStatement(sql);
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * A SQL stored procedure call statement is handled by creating a
     * CallableStatement for it. The CallableStatement provides
     * methods for setting up its IN and OUT parameters, and
     * methods for executing it.
     *
     * <P><B>Note:</B> This method is optimized for handling stored
     * procedure call statements. Some drivers may send the call
     * statement to the database when the prepareCall is done; others
     * may wait until the CallableStatement is executed. This has no
     * direct affect on users; however, it does affect which method
     * throws certain SQLExceptions.
     *
     * @param sql a SQL statement that may contain one or more '?'
     * parameter placeholders. Typically this  statement is a JDBC
     * function call escape string.
     * @return a new CallableStatement object containing the
     * pre-compiled SQL statement
     * @exception SQLException if a database-access error occurs.
     */
    public CallableStatement prepareCall(String sql) throws SQLException {
        try {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->prepareCall, sql = " + sql);
            }

            return phisicalConnection.prepareCall(sql);
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }


    /**
     * A driver may convert the JDBC sql grammar into its system's
     * native SQL grammar prior to sending it; nativeSQL returns the
     * native form of the statement that the driver would have sent.
     *
     * @param sql a SQL statement that may contain one or more '?'
     * parameter placeholders
     * @return the native form of this statement
     * @exception SQLException if a database-access error occurs.
     */
    public String nativeSQL(String sql) throws SQLException {
        try {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->nativeSQL, sql = " + sql);
            }

            return phisicalConnection.nativeSQL(sql);
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * If a connection is in auto-commit mode, then all its SQL
     * statements will be executed and committed as individual
     * transactions.  Otherwise, its SQL statements are grouped into
     * transactions that are terminated by either commit() or
     * rollback().  By default, new connections are in auto-commit
     * mode.
     *
     * The commit occurs when the statement completes or the next
     * execute occurs, whichever comes first. In the case of
     * statements returning a ResultSet, the statement completes when
     * the last row of the ResultSet has been retrieved or the
     * ResultSet has been closed. In advanced cases, a single
     * statement may return multiple results as well as output
     * parameter values. Here the commit occurs when all results and
     * output param values have been retrieved.
     *
     * @param autoCommit true enables auto-commit; false disables
     * auto-commit.
     * @exception SQLException if a database-access error occurs.
     */
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        try {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->setAutoCommit->started to autoCommit = " + autoCommit);
            }

            phisicalConnection.setAutoCommit(autoCommit);

            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->setAutoCommit->finished");
            }
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * Get the current auto-commit state.
     *
     * @return Current state of auto-commit mode.
     * @exception SQLException if a database-access error occurs.
     * @see #setAutoCommit
     */
    public boolean getAutoCommit() throws SQLException {
        try {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->getAutoCommit->started");
            }

            boolean autoCommit = phisicalConnection.getAutoCommit();

            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->getAutoCommit->finished, autoCommit = " + autoCommit);
            }

            return autoCommit;
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * Commit makes all changes made since the previous
     * commit/rollback permanent and releases any database locks
     * currently held by the Connection. This method should only be
     * used when auto commit has been disabled.
     *
     * @exception SQLException if a database-access error occurs.
     * @see #setAutoCommit
     */
    public void commit() throws SQLException {
        try {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->commit->started");
            }

            phisicalConnection.commit();

            if (CAT.isDebugEnabled()) {
                CAT.debug("connection proxy->commit->finished");
            }
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * Rollback drops all changes made since the previous
     * commit/rollback and releases any database locks currently held
     * by the Connection. This method should only be used when auto
     * commit has been disabled.
     *
     * @exception SQLException if a database-access error occurs.
     * @see #setAutoCommit
     */
    public void rollback() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->rollback->started");

            phisicalConnection.rollback();

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->rollback->finished");
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * In some cases, it is desirable to immediately release a
     * Connection's database and JDBC resources instead of waiting for
     * them to be automatically released; the close method provides this
     * immediate release.
     *
     * <P><B>Note:</B> A Connection is automatically closed when it is
     * garbage collected. Certain fatal errors also result in a closed
     * Connection.
     *
     * @exception SQLException if a database-access error occurs.
     */
    public void close() throws SQLException {
        if (CAT.isDebugEnabled()) {
            CAT.debug("connection proxy->close->started");
        }

        if (closed) {
            throw new SQLException("connection is already closed.");
        }
        SQLException thrownSQLException = null;
        // undo this connection changes
        try {
            phisicalConnection.clearWarnings();
        } catch (SQLException exc) {
            thrownSQLException = exc;
            CAT.error("connection proxy->close->clearWarnings->error: connection will be removed from the pool due", exc);
        }
        // rollback uncommitted data
        try {
            if (!phisicalConnection.getAutoCommit()) {
                phisicalConnection.rollback();  //trash
            }
        } catch (SQLException exc) {
            if (thrownSQLException == null) {
                thrownSQLException = exc;
                CAT.error("connection proxy->close->rollback->error: connection will be removed from the pool due", exc);
            } else {
                //thrownSQLException.setNextException(exc);
                CAT.error("connection proxy->close->rollback->error: cannot roll back", exc);
            }
        }

        // clear connection
        final SQLException fatalExc = (fatalSQLException != null ? fatalSQLException : thrownSQLException);
        if (fatalExc == null) {
            pooledConnection.fireConnectionClosedEvent();
        } else {
            pooledConnection.fireConnectionErrorOccurred(fatalExc);
        }

        closed = true;

        if (thrownSQLException != null) {
            SQLException sqlExc = new SQLException("Connection was closed but errors occured. Check next exception (" + thrownSQLException + ")");
            sqlExc.setNextException(thrownSQLException);
            throw sqlExc;
        }
        // finished successfully
        if (CAT.isDebugEnabled()) {
            CAT.debug("connection proxy->close->finished");
        }
    }

    /**
     * Called from {@link DriverPolledConnection#getConnection()}.
     */
    public void internalClose() throws SQLException {
        if (CAT.isDebugEnabled()) {
            CAT.debug("connection proxy->internal close->started");
        }

        if (closed) {
            throw new SQLException("connection is already closed.");
        }
        SQLException thrownSQLException = null;
        // undo this connection changes
        try {
            phisicalConnection.clearWarnings();
        } catch (SQLException exc) {
            thrownSQLException = exc;
            CAT.error("connection proxy->internal close->clearWarnings->error: connection will be removed from the pool due", exc);
        }
        // rollback uncommitted data
        try {
            phisicalConnection.rollback();  //trash
        } catch (SQLException exc) {
            if (thrownSQLException == null) {
                thrownSQLException = exc;
                CAT.error("connection proxy->internal close->rollback->error: connection will be removed from the pool due", exc);
            } else {
                //thrownSQLException.setNextException(exc);
                CAT.error("connection proxy->internal close->rollback->error: cannot roll back", exc);
            }
        }

        closed = true;

        if (thrownSQLException != null) {
            SQLException sqlExc = new SQLException("Connection was closed but errors occured. Check next exception (" + thrownSQLException + ")");
            sqlExc.setNextException(thrownSQLException);
            throw sqlExc;
        }
        // finished successfully
        if (CAT.isDebugEnabled()) {
            CAT.debug("connection proxy->internal close->finished");
        }
    }

    /**
     * Tests to see if a Connection is closed.
     *
     * @return true if the connection is closed; false if it's still open
     * @exception SQLException if a database-access error occurs.
     */
    public boolean isClosed() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->isClosed->started");

            boolean isClosed = /*phisicalConnection==null || */phisicalConnection.isClosed();

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->isClosed->finished, isClosed = " + isClosed);

            return isClosed;
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    //======================================================================
    // Advanced features:

    /**
     * A Connection's database is able to provide information
     * describing its tables, its supported SQL grammar, its stored
     * procedures, the capabilities of this connection, etc. This
     * information is made available through a DatabaseMetaData
     * object.
     *
     * @return a DatabaseMetaData object for this Connection
     * @exception SQLException if a database-access error occurs.
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->getMetaData");

            return phisicalConnection.getMetaData();
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * You can put a connection in read-only mode as a hint to enable
     * database optimizations.
     *
     * <P><B>Note:</B> setReadOnly cannot be called while in the
     * middle of a transaction.
     *
     * @param readOnly true enables read-only mode; false disables
     * read-only mode.
     * @exception SQLException if a database-access error occurs.
     */
    public void setReadOnly(boolean readOnly) throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->setReadOnly->started to readOnly = " + readOnly);

            phisicalConnection.setReadOnly(readOnly);

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->setReadOnly->finished");
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * Tests to see if the connection is in read-only mode.
     *
     * @return true if connection is read-only
     * @exception SQLException if a database-access error occurs.
     */
    public boolean isReadOnly() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->isReadOnly->started");

            boolean isReadOnly = phisicalConnection.isReadOnly();

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->isReadOnly->finished, isReadOnly = " + isReadOnly);

            return isReadOnly;
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * A sub-space of this Connection's database may be selected by setting a
     * catalog name. If the driver does not support catalogs it will
     * silently ignore this request.
     *
     * @exception SQLException if a database-access error occurs.
     */
    public void setCatalog(String catalog) throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->setCatalog->started to catalog = " + catalog);

            phisicalConnection.setCatalog(catalog);

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->setCatalog->finished");
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * Return the Connection's current catalog name.
     *
     * @return the current catalog name or null
     * @exception SQLException if a database-access error occurs.
     */
    public String getCatalog() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->getCatalog->started");

            String catalog = phisicalConnection.getCatalog();

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->getCatalog->finished, catalog = " + catalog);

            return catalog;
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * You can call this method to try to change the transaction
     * isolation level using one of the TRANSACTION_* values.
     *
     * <P><B>Note:</B> setTransactionIsolation cannot be called while
     * in the middle of a transaction.
     *
     * @param level one of the TRANSACTION_* isolation values with the
     * exception of TRANSACTION_NONE; some databases may not support
     * other values
     * @exception SQLException if a database-access error occurs.
     * @see DatabaseMetaData#supportsTransactionIsolationLevel
     */
    public void setTransactionIsolation(int level) throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->setTransactionIsolation->started to level = " + level);

            phisicalConnection.setTransactionIsolation(level);

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->setTransactionIsolation->finished");
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * Get this Connection's current transaction isolation mode.
     *
     * @return the current TRANSACTION_* mode value
     * @exception SQLException if a database-access error occurs.
     */
    public int getTransactionIsolation() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->getTransactionIsolation->started");
            int level = phisicalConnection.getTransactionIsolation();

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->getTransactionIsolation->finished, level = " + level);

            return level;
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * The first warning reported by calls on this Connection is
     * returned.
     *
     * <P><B>Note:</B> Subsequent warnings will be chained to this
     * SQLWarning.
     *
     * @return the first SQLWarning or null
     * @exception SQLException if a database-access error occurs.
     */
    public SQLWarning getWarnings() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->getWarnings");

            return phisicalConnection.getWarnings();
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * After this call, getWarnings returns null until a new warning is
     * reported for this Connection.
     *
     * @exception SQLException if a database-access error occurs.
     */
    public void clearWarnings() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->clearWarnings->started");

            phisicalConnection.clearWarnings();

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->clearWarnings->finished");
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    //---------------------------------------------------------------------------
    //------ JDK 1.2 methods ----------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * JDBC 2.0
     *
     * Creates a <code>Statement</code> object that will generate
     * <code>ResultSet</code> objects with the given type and concurrency.
     * This method is the same as the <code>createStatement</code> method
     * above, but it allows the default result set
     * type and result set concurrency type to be overridden.
     *
     * @param resultSetType a result set type; see ResultSet.TYPE_XXX
     * @param resultSetConcurrency a concurrency type; see ResultSet.CONCUR_XXX
     * @return a new Statement object
     * @exception SQLException if a database access error occurs
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency)
            throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->createStatement, resultSetType = " + resultSetType +
                        ", resultSetConcurrency = " + resultSetConcurrency);

            return phisicalConnection.createStatement(resultSetType, resultSetConcurrency);
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * JDBC 2.0
     *
     * Creates a <code>PreparedStatement</code> object that will generate
     * <code>ResultSet</code> objects with the given type and concurrency.
     * This method is the same as the <code>prepareStatement</code> method
     * above, but it allows the default result set
     * type and result set concurrency type to be overridden.
     *
     * @param resultSetType a result set type; see ResultSet.TYPE_XXX
     * @param resultSetConcurrency a concurrency type; see ResultSet.CONCUR_XXX
     * @return a new PreparedStatement object containing the
     * pre-compiled SQL statement
     * @exception SQLException if a database access error occurs
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->prepareStatement, sql = " + sql +
                        ", resultSetType = " + resultSetType +
                        ", resultSetConcurrency = " + resultSetConcurrency);

            return phisicalConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * JDBC 2.0
     *
     * Creates a <code>CallableStatement</code> object that will generate
     * <code>ResultSet</code> objects with the given type and concurrency.
     * This method is the same as the <code>prepareCall</code> method
     * above, but it allows the default result set
     * type and result set concurrency type to be overridden.
     *
     * @param resultSetType a result set type; see ResultSet.TYPE_XXX
     * @param resultSetConcurrency a concurrency type; see ResultSet.CONCUR_XXX
     * @return a new CallableStatement object containing the
     * pre-compiled SQL statement
     * @exception SQLException if a database access error occurs
     */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->prepareCall, sql = " + sql +
                        ", resultSetType = " + resultSetType +
                        ", resultSetConcurrency = " + resultSetConcurrency);

            return phisicalConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * JDBC 2.0
     *
     * Gets the type map object associated with this connection.
     * Unless the application has added an entry to the type map,
     * the map returned will be empty.
     *
     * @return the <code>java.util.Map</code> object associated
     *         with this <code>Connection</code> object
     */
    public java.util.Map getTypeMap() throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->getTypeMap");

            return phisicalConnection.getTypeMap();
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }

    /**
     * JDBC 2.0
     *
     * Installs the given type map as the type map for
     * this connection.  The type map will be used for the
     * custom mapping of SQL structured types and distinct types.
     *
     * @param map the <code>java.util.Map</code> object to install
     *        as the replacement for this <code>Connection</code>
     *        object's default type map
     */
    public void setTypeMap(java.util.Map map) throws SQLException {
        try {
            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->setTypeMap->started");

            phisicalConnection.setTypeMap(map);

            if (CAT.isDebugEnabled())
                CAT.debug("connection proxy->setTypeMap->finished");
        } catch (SQLException exc) {
            throw handleException(exc);
        }
    }
}
