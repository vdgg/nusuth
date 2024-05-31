package com.azoft.nusuth.jdbc.pool;

import org.apache.log4j.Category;

import javax.sql.DataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;

import java.io.PrintWriter;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.HashSet;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Implementation of {@link DataSource} interface with
 * connection pooling support.
 * If work with connection causes {@link SQLException} then
 * this connection is removed from connection pool,
 * if closing of this connection causes {@link SQLException} then
 * connection pool is refreshed (each free connection is checking for
 * valid phisical connection).
 * <p>
 * Connection pool construction example: <br>
 * <code>
 * public {@link DataSource} createConnectionPool({@link String} url,
 *                                                {@link String} user,
 *                                                {@link String} password,
 *                                                int poolSizeLimit,
 *                                                int loginTimeout)<br>
 * { <br>
 *    &nbsp;&nbsp;
 *    <i>// data source over {@link java.sql.DriverManager DriverManager} </i> <br>
 *    &nbsp;&nbsp;
 *    {@link DataSource} driverDS = new
 *    {@link ru.novosoft.jdbc.pool.driver.DriverDataSource#DriverDataSource(String,String,String) DriverDataSource}
 *    (url, user, password); <br>
 *
 *    &nbsp;&nbsp;
 *    driverDS.{@link ru.novosoft.jdbc.pool.driver.DriverDataSource#setLoginTimeout(int) setLoginTimeout}
 *    (loginTimeout); <br>
 *
 *    &nbsp;&nbsp; <br>
 *    &nbsp;&nbsp;
 *    {@link ConnectionPoolDataSourceImpl} poolDS = new
 *    {@link ConnectionPoolDataSourceImpl#ConnectionPoolDataSourceImpl(DataSource) ConnectionPoolDataSourceImpl}
 *    (driverDS); <br>
 *
 *    &nbsp;&nbsp;
 *    poolDS.{@link ConnectionPoolDataSourceImpl#setLoginTimeout(int) setLoginTimeout}
 *    (loginTimeout); <br>
 *
 *    &nbsp;&nbsp; <br>
 *    &nbsp;&nbsp;
 *    <i>// connection pool</i> <br>
 *    &nbsp;&nbsp;
 *    {@link ConnectionPoolWithQueue} pooledDS = new
 *    {@link ConnectionPoolWithQueue#ConnectionPoolWithQueue(ConnectionPoolDataSource,int) ConnectionPoolWithQueue}
 *    (poolDS, poolSizeLimit); <br>
 *
 *    &nbsp;&nbsp;
 *    pooledDS.{@link ConnectionPoolWithQueue#setLoginTimeout setLoginTimeout}
 *    (loginTimeout); <br>
 *
 *    &nbsp;&nbsp; <br>
 *    &nbsp;&nbsp;
 *    return pooledDS; <br>
 * } <br>
 * </code>
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @version 0.5.5
 * @since 0.3.0
 */
public class ConnectionPoolWithQueue implements DataSource {
    /** category to log */
    private static final Category CAT = Category.getInstance(ConnectionPoolWithQueue.class.getName());

    /** underlying data source */
    private ConnectionPoolDataSource ds;
    /** connection pool queue */
    private final Queue queue = new Queue();
    /** getConnection timeout in ms */
    private long timeout;
    /** limit of pool size */
    private int poolSizeLimit;
    /** count of activated connections */
    private int activeConnectionsCount;
    /** allocated connections */
    private final HashSet allocated = new HashSet();
    /** lock */
    private final Object lock = new Object();
    /** is closed flag */
    private boolean closed = false;
    /**
     * fill factor to fill connection pool after refresh
     *
     * @since 0.4.10
     */
    private float fillFactor = 0f;
    /**
     * Flag to check metadata during refresh.
     * This flag is useful to refresh connection pool over InterBase database as
     * only via fetching data from server it is possible to check validity
     * of connection to InterBase database.
     *
     * @since 0.4.10
     */
    private boolean checkMetadataOnRefresh = false;
    /**
     * Flag to check metadata before connection close by occurred error.
     * This flag is useful to refresh connection pool over MSSQL database as
     * only via fetching data from server it is possible to check validity
     * of connection to MSSQL database.
     *
     * @since 0.4.11
     */
    private boolean checkMetadataOnConnectionClose = false;

    //---------------------------------------------------------------------------
    //---- constructors ---------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * @since 0.5.0
     */
    public ConnectionPoolWithQueue() {
    }

    public ConnectionPoolWithQueue(ConnectionPoolDataSource ds, int poolSizeLimit) {
        this.ds = ds;
        this.poolSizeLimit = poolSizeLimit;
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    /**
     * @since 0.5.0
     */
    public int getPoolSizeLimit() {
        return poolSizeLimit;
    }

    /**
     * @since 0.5.0
     */
    public void setPoolSizeLimit(int newValue) {
        poolSizeLimit = newValue;
    }

    /**
     * @since 0.5.0
     */
    public ConnectionPoolDataSource getDataSource() {
        return ds;
    }

    /**
     * @since 0.5.0
     */
    public void setDataSource(ConnectionPoolDataSource newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("dataSource is null");
        }
        ds = newValue;
    }

    public Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Pool has bean already closed.");
        }

        if (CAT.isInfoEnabled()) {
            CAT.info("connection pool->get connection, pool size = " + allocated.size());
        }

        PooledConnectionBox box = (PooledConnectionBox) queue.get(0);
        if (box != null) {
            return box.getConnection();
        }

        boolean allocate = false;
        synchronized (lock) {
            if (activeConnectionsCount < poolSizeLimit) {
                activeConnectionsCount++;
                allocate = true;
            }
        }

        if (allocate) {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection pool->get connection, no free connections. Trying to allocate " + activeConnectionsCount + "th.");
            }
            try {
                box = new PooledConnectionBox(ds.getPooledConnection());
                if (closed) {
                    try {
                        box.close();
                    } catch (Exception exc) {
                        CAT.warn("connection pool->close connection on pool close->PooledConnectionBox->close->error (ignored)", exc);
                    }

                    throw new SQLException("Pool has bean already closed.");
                }
                synchronized (lock) {
                    allocated.add(box);
                }
                return box.getConnection();
            } catch (Throwable exc) {
                synchronized (lock) {
                    activeConnectionsCount--;
                }

                if (exc instanceof SQLException) {
                    throw (SQLException) exc;
                } else if (exc instanceof RuntimeException) {
                    throw (RuntimeException) exc;
                } else if (exc instanceof Error) {
                    throw (Error) exc;
                } else {
                    throw new SQLException(exc.toString());
                }
            }
        } else {
            if (CAT.isDebugEnabled()) {
                CAT.debug("connection pool->get connection, no free connections. Waiting for connection.");
            }
            try {
                box = (PooledConnectionBox) queue.get(timeout);
                if (box == null) {
                    if (CAT.isDebugEnabled()) {// log stack trace of allocated connections
                        ArrayList connections;
                        synchronized (lock) {
                            connections = new ArrayList(allocated);
                        }

                        for (ListIterator i = connections.listIterator(); i.hasNext();) {
                            PooledConnectionBox b = (PooledConnectionBox) i.next();
                            CAT.debug("Allocated connection(" + i.nextIndex() + ") stack trace:", b.getLastGetConnectionMark());
                        }
                    }
                    throw new SQLException("Could not allocate connection due timeout");
                }
                return box.getConnection();
            } catch (RuntimeException exc) {
                throw new SQLException(exc.toString());
            }
        }
    }


    /**
     * Unsupported Operation.
     *
     * @throws UnsupportedOperationException always.
     */
    public Connection getConnection(String user, String password) throws SQLException {
        return getConnection();
//        throw new UnsupportedOperationException("Method getConnection(String,String) not supported");
    }


    /**
     * Returns <code>null</code>.
     * @return <code>null</code>.
     */
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }


    /**
     * Ignored as log4j logging is used.
     */
    public void setLogWriter(PrintWriter writer) throws SQLException {
        if (writer != null) {
            writer.println("Use log4j \"" + CAT.getName() + "\" category instead of setLogWriter(PrintWriter)");
            writer.flush();
        }
    }

    public int getLoginTimeout() throws SQLException {
        return (int) (timeout / 1000);
    }

    public void setLoginTimeout(int timeout) throws SQLException {
        this.timeout = timeout * 1000;
    }

    /**
     * Get fill factor.
     *
     * @see #setFillFactor(float factor)
     *
     * @since 0.4.10
     */
    public float getFillFactor() {
        return fillFactor;
    }

    /**
     * Sets fill factor and fills connection pool
     * (see {@link #fillConnectionPool(float factor)}).
     * Fill factor is a factor to refill connection pool after connection pool refresh.
     *
     * @since 0.4.10
     */
    public void setFillFactor(float factor) {
        if (factor < 0f || factor > 1f) {
            throw new IllegalArgumentException("Illegal value for fillFactor. Must be in [0..1]. Argument value = " + factor);
        }

        fillFactor = factor;
        try {
            fillConnectionPool(fillFactor);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("SQL related problem: " + ex);
        }
    }

    /**
     * Get <code>checkMetadataOnRefresh</code> flag.
     *
     * @see #setCheckMetadataOnRefresh(boolean b)
     *
     * @since 0.4.10
     */
    public boolean getCheckMetadataOnRefresh() {
        return checkMetadataOnRefresh;
    }

    /**
     * Flag to check metadata during refresh.
     * This flag is useful to refresh connection pool over InterBase database as
     * only via fetching data from server it is possible to check validity
     * of connection to InterBase.
     *
     * @since 0.4.10
     */
    public void setCheckMetadataOnRefresh(boolean b) {
        checkMetadataOnRefresh = b;
    }


    /**
     * Get <code>heckMetadataOnConnectionClose</code> flag.
     *
     * @see #setCheckMetadataOnConnectionClose(boolean b)
     *
     * @since 0.4.11
     */
    public boolean getCheckMetadataOnConnectionClose() {
        return checkMetadataOnConnectionClose;
    }

    /**
     * Flag to check metadata before connection close by occurred error.
     * This flag is useful to refresh connection pool over MSSQL database as
     * only via fetching data from server it is possible to check validity
     * of connection to MSSQL database.
     *
     * @since 0.4.11
     */
    public void setCheckMetadataOnConnectionClose(boolean b) {
        checkMetadataOnConnectionClose = b;
    }

    public void close() throws SQLException {
        CAT.info("connection pool->close started");
        synchronized (lock) {
            closed = true;

            for (Iterator i = allocated.iterator(); i.hasNext();) {
                PooledConnectionBox box = (PooledConnectionBox) i.next();

                box.close();
            }
        }
        CAT.info("connection pool->close finished");
    }

    /**
     * Fills connection pool up to <code>fillFactor*poolSizeLimit</code>
     * connections (for <code>poolSizeLimit</code> see
     * {@link #ConnectionPoolWithQueue(ConnectionPoolDataSource ds, int poolSizeLimit)}).
     *
     * @since 0.4.10
     */
    public void fillConnectionPool(float fillFactor) throws SQLException {
        if (fillFactor < 0f || fillFactor > 1f) {
            throw new IllegalArgumentException("Illegal value for fillFactor. Must be in [0..1]. Argument value = " + fillFactor);
        }

        synchronized (lock) {
            final int toAllocate = (int) (fillFactor * poolSizeLimit) - activeConnectionsCount;
            if (CAT.isInfoEnabled()) {
                CAT.info("connection pool->fillConnectionPool->start to allocate " +
                        toAllocate + " connections (fillFactor=" + fillFactor +
                        ", activeConnectionsCount = " + activeConnectionsCount + ").");
            }

            SQLException sqlExc = null;
            if (toAllocate > 0) {
                Connection cons[] = new Connection[toAllocate];
                try {
                    for (int i = 0; i < toAllocate; i++) {
                        cons[i] = getConnection();
                    }
                } catch (SQLException ex) {
                    CAT.error("connection pool->fillConnectionPool->cannot allocate connections due exception:", ex);
                    sqlExc = ex;
                } finally {
                    CAT.info("connection pool->fillConnectionPool->started to fill pool");

                    for (int i = 0; i < cons.length; i++) {
                        if (cons[i] != null) {
                            if (CAT.isInfoEnabled()) {
                                CAT.info("connection pool->fillConnectionPool->connection(" + i + "): " + cons[i]);
                            }

                            try {
                                cons[i].close();
                            } catch (Exception ex) {
                                CAT.error("connection pool->fillConnectionPool->cannot put connection(" + i + "): ", ex);
                            }
                        }
                    }
                }
            }

            if (sqlExc != null) {
                CAT.error("connection pool->fillConnectionPool->finish with error:", sqlExc);
                throw sqlExc;
            } else {
                CAT.info("connection pool->fillConnectionPool->finish");
            }
        }
    }

    /**
     * Refresh connection pool.
     * Tries to rollback free allocated connections.
     * If rollback on connection caused error then the connection is removed
     * from pool.
     *
     * @since 0.4.10
     */
    private void refreshConnections() {
        if (CAT.isInfoEnabled()) {
            CAT.info("connection pool->refresh started. Active connections count = " + activeConnectionsCount);
        }

        int closed;
        synchronized (lock) {
            closed = refreshConnectionsRollback();
            //if(closed == 0) // no closed connections by rollback
            if (checkMetadataOnRefresh) {
                closed += refreshConnectionsMetadata();
            }

            try {
                fillConnectionPool(getFillFactor());
            } catch (SQLException exc) {
                CAT.error("connection pool->refresh->fill pool->error:", exc);
            }
        }

        if (CAT.isInfoEnabled()) {
            CAT.info("connection pool->refresh finished. Active connections count = " + activeConnectionsCount +
                    ", closed = " + closed);
        }
    }

    /**
     * Refresh connection pool.
     * Tries to rollback free allocated connections.
     * If rollback on connection caused error then the connection is removed
     * from pool.
     *
     * @return quantity of closed connections.
     *
     * @since 0.4.10
     */
    private int refreshConnectionsRollback() {
        synchronized (lock) {
            final int connections = activeConnectionsCount;

            ArrayList validConns = new ArrayList(allocated.size()); // valid connections

            while (true) {
                PooledConnectionBox box = (PooledConnectionBox) queue.get(0);
                if (box == null) {
                    break;
                } else {
                    try {
                        box.getConnection().setAutoCommit(false);
                        box.getConnection().rollback();
                        validConns.add(box); // valid connection
                    } catch (SQLException exc) {
                        box.close();
                        allocated.remove(box);
                        activeConnectionsCount--;
                    }
                }
            }

            for (Iterator i = validConns.iterator(); i.hasNext();) {
                queue.put(i.next());
            }

            return connections - activeConnectionsCount;
        }
    }

    /**
     * Refresh connection pool.
     * Tries to get tables metadata from free allocated connections.
     * If this query on connection caused error then the connection is removed
     * from pool.
     *
     * @return quantity of closed connections.
     *
     * @since 0.4.10
     */
    private int refreshConnectionsMetadata() {
        synchronized (lock) {
            final int connections = activeConnectionsCount;

            ArrayList validConns = new ArrayList(allocated.size()); // valid connections

            while (true) {
                PooledConnectionBox box = (PooledConnectionBox) queue.get(0);
                if (box == null) {
                    break;
                } else {
                    try {
                        box.getConnection().getMetaData().getTableTypes().close();
                        validConns.add(box); // valid connection
                    } catch (SQLException exc) {
                        box.close();
                        allocated.remove(box);
                        activeConnectionsCount--;
                    }
                }
            }

            for (Iterator i = validConns.iterator(); i.hasNext();) {
                queue.put(i.next());
            }

            return connections - activeConnectionsCount;
        }
    }

    class PooledConnectionBox implements ConnectionEventListener {
        /** connection */
        private final PooledConnection connection;
        /** exception with stack trace of last getConnection() */
        private final Exception lastGetConnection = new Exception("getConnection() stack trace");

        /** constructor */
        PooledConnectionBox(PooledConnection connection) {
            this.connection = connection;
            connection.addConnectionEventListener(this);
        }

        public void connectionClosed(ConnectionEvent event) {
            queue.put(this);
        }

        public void connectionErrorOccurred(ConnectionEvent event) {
            synchronized (lock) {
                activeConnectionsCount--;
                allocated.remove(this);
            }

            CAT.info("connectionErrorOccurred: connection was removed from pool, connection = " + connection);

            try {
                try {
                    if (checkMetadataOnConnectionClose) {
                        connection.getConnection().getMetaData().getTableTypes().close();
                    }
                } finally {
                    connection.close();
                }
            } catch (SQLException exc) {
                refreshConnections();
            } catch (Exception exc) {
            }
        }

        Connection getConnection() throws SQLException {
            lastGetConnection.fillInStackTrace();
            return connection.getConnection();
        }

        void close() {
            try {
                //connection.getConnection().close();
                connection.close();
            } catch (Exception exc) {
                CAT.warn("PooledConnectionBox->close->error (ignored)", exc);
            }

        }

        Exception getLastGetConnectionMark() {
            return lastGetConnection;
        }
    }
}


