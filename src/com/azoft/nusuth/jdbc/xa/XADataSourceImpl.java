package com.azoft.nusuth.jdbc.xa;

import java.io.Serializable;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.SQLException;
import java.util.*;

import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.Xid;

import org.apache.log4j.Category;

/**
 * Implements a JDBC 2.0 {@link XADataSource} for any JDBC driver
 * with JNDI persistance support. The base implementation is actually
 * provided by a different {@link DataSource} class; although this is
 * the super class, it only provides the pooling and XA specific
 * implementation.
 *
 * @version 0.5.5
 * @since 0.0.1
 */
public class XADataSourceImpl implements XADataSource,
        Serializable,
        Runnable {
    private static final Category CAT = Category.getInstance(XADataSourceImpl.class.getName());
    /**
     *
     */
    private DataSource _underlying;

    /**
     * Holds the timeout for opening a new connection, specified
     * in seconds. The default is obtained from the JDBC driver.
     */
    private int _loginTimeout;


    /**
     * Holds the log writer to which all messages should be
     * printed. The default writer is obtained from the driver
     * manager, but it can be specified at the datasource level
     * and will be passed to the driver. May be null.
     */
    private transient PrintWriter _logWriter;


    /**
     * Maps underlying JDBC connections into global transaction Xids.
     */
    private transient Map _txConnections = Collections.synchronizedMap(new HashMap());
    /**
     * A background deamon thread terminating connections that have
     * timed out.
     */
    private transient Thread _background;
    /**
     * The default timeout for all new transactions.
     */
    private int _txTimeout = DEFAULT_TX_TIMEOUT;
    /**
     * The default timeout for all new transactions is 30 seconds.
     */
    public final static int DEFAULT_TX_TIMEOUT = 30;
    /**
     * Transaction Isolation level
     */
    private int _isolationLevel = Connection.TRANSACTION_SERIALIZABLE;

    /**
     * Implementation details:
     *   If two XAConnections are associated with the same transaction
     *   (one with a start the other with a join) they must use the
     *   same underlying JDBC connection. They lookup the underlying
     *   JDBC connection based on the transaction's Xid in the
     *   originating XADataSource.
     *
     *   Currently the XADataSource must be the exact same object,
     *   this should be changed so all XADataSources that are equal
     *   share a table of all enlisted connections
     *
     *   To test is two connections should fall under the same
     *   transaction we match the resource managers by comparing the
     *   database/user they fall under using a comparison of the
     *   XADataSource properties.
     */
    public XADataSourceImpl(DataSource underlying) {
        this._underlying = underlying;
        _background = new Thread(this, "XADataSource Timeout Daemon");
        _background.setPriority(Thread.MIN_PRIORITY);
        _background.setDaemon(true);
        _background.start();
    }

    public XADataSourceImpl() {
        _background = new Thread(this, "XADataSource Timeout Daemon");
        _background.setPriority(Thread.MIN_PRIORITY);
        _background.setDaemon(true);
        _background.start();
    }

    public DataSource getDataSource() {
        return _underlying;
    }

    public void setDataSource(DataSource newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("dataSource is null");
        }
        _underlying = newValue;
    }

    /**
     * Construct a new XAConnection with no underlying connection.
     * When a JDBC method requires an underlying connection, one
     * will be created. We don't create the underlying connection
     * beforehand, as it might be coming from an existing
     * transaction.
     */
    public XAConnection getXAConnection() throws SQLException {
        return new XAConnectionImpl(this, null);
    }

    /**
     *  Since we create the connection on-demand with newConnection
     *  or obtain it from a transaction, we cannot support XA
     *  connections with a caller specified user name.
     */
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        throw new SQLException("XAConnection does not support connections with caller specified user name");
    }


    /**
     * Returns the default timeout for all transactions.
     */
    public int getTransactionTimeout() {
        return _txTimeout;
    }

    /**
     * Sets the default timeout for all transactions. The timeout is
     * specified in seconds. Use zero for the default timeout. Calling
     * this method does not affect transactions in progress.
     *
     * @param seconds The timeout in seconds
     */
    public void setTransactionTimeout(int seconds) {
        synchronized (this) {
            if (seconds <= 0)
                _txTimeout = DEFAULT_TX_TIMEOUT;
            else
                _txTimeout = seconds;
        }
        if (CAT.isDebugEnabled()) CAT.debug("Set transaction timeout to " + seconds + " seconds");
        Thread t = _background;
        if (t != null) {
            t.interrupt();
        }
    }


    /**
     * Returns an underlying connection for the global transaction,
     * if one has been associated before.
     *
     * @param xid The transaction Xid
     * @return A connection associated with that transaction, or null
     */
    TxConnection getTxConnection(Xid xid) {
        return (TxConnection) _txConnections.get(xid);
    }


    /**
     * Associates the global transaction with an underlying connection,
     * or dissociate it when null is passed.
     *
     * @param xid The transaction Xid
     * @param conn The connection to associate, null to dissociate
     */
    TxConnection setTxConnection(Xid xid, TxConnection txConn) {
        if (txConn == null)
            return (TxConnection) _txConnections.remove(xid);
        else
            return (TxConnection) _txConnections.put(xid, txConn);
    }


    /**
     * Creates a new underlying connection. Used by XA connection
     * that lost it's underlying connection when joining a
     * transaction and is now asked to produce a new connection.
     *
     * @return An open connection ready for use
     * @throws SQLException An error occured trying to open
     *   a connection
     */
    Connection newConnection() throws SQLException {
        return _underlying.getConnection();
    }


    /**
     * XXX Not fully implemented yet and no code to really
     *     test it.
     */
    Xid[] getTxRecover() {
        Vector list;
        TxConnection txConn;

        list = new Vector();

        for (Iterator iter = _txConnections.keySet().iterator(); iter.hasNext();) {
            txConn = (TxConnection) _txConnections.get(iter.next());
            if (txConn != null && txConn.conn != null && txConn.prepared)
                list.add(txConn.xid);
        }
        return (Xid[]) list.toArray();
    }

    /**
     * Returns the transaction isolation level to use with all newly
     * created transactions, or {@link Connection#TRANSACTION_NONE}
     * if using the driver's default isolation level.
     */
    public int getIsolationLevel() {
        return _isolationLevel;
    }


    public void setIsolationLevel(int level) {
        synchronized (this) {
            _isolationLevel = level;
        }
    }

    public void run() {
        int reduce;
        long timeout;
        TxConnection txConn;

        while (true) {
            // Go to sleep for the duration of a transaction
            // timeout. This mean transactions will timeout on average
            // at _txTimeout * 1.5.
            try {
                Thread.sleep(_txTimeout * 1000);
            } catch (InterruptedException except) {
            }

            // Look for all connections inside a transaction that
            // should have timed out by now.
            timeout = System.currentTimeMillis();
            for (Iterator iter = _txConnections.keySet().iterator(); iter.hasNext();) {
                txConn = (TxConnection) _txConnections.get(iter.next());
                if (txConn == null) continue;
                // If the transaction timed out, we roll it back and
                // invalidate it, but do not remove it from the transaction
                // list yet. We wait for the next iteration, minimizing the
                // chance of a NOTA exception.
                if (txConn.conn == null) {
                    _txConnections.remove(txConn.xid);
                    // Chose not to use an iterator so we must
                    // re-enumerate the list after removing
                    // an element from it.
                } else if (txConn.timeout < timeout) {
                    try {
                        Connection underlying;

                        synchronized (txConn) {
                            if (txConn.conn == null) {
                                continue;
                            }
                            // Remove the connection from the transaction
                            // association. XAConnection will now have
                            // no underlying connection and attempt to
                            // create a new one.
                            underlying = txConn.conn;
                            txConn.conn = null;
                            txConn.timedOut = true;
                        }
                        if (CAT.isInfoEnabled()) {
                            if (CAT.isDebugEnabled()) {
                                CAT.debug("Current time is " + timeout);
                            }
                            CAT.info("DataSource " + toString() + ": Transaction timed out and being aborted: " + txConn.xid + " xid");
                        }


                        // Rollback the underlying connection to
                        // abort the transaction and release the
                        // underlying connection to the pool.
                        try {
                            underlying.rollback();
                            //releaseConnection(underlying);
                        } catch (SQLException except) {
                            CAT.error("DataSource " + toString() +
                                    ": Error aborting timed out transaction: ", except);
                            try {
                                underlying.close();
                            } catch (SQLException e2) {
                                CAT.error("DataSource " + toString() +
                                        ": exception during closing of underlying (ignored)", e2);
                            }
                        }
                    } catch (Exception except) {
                        CAT.debug("DataSource " + toString() +
                                ": Error aborting timed out transaction (ignored):", except);
                    }
                }
            }
        }
    }

    private void log(String message) {
        PrintWriter pw = getLogWriter();
        if (pw != null) {
            pw.println(message);
        }
    }

    public PrintWriter getLogWriter() {
        return _logWriter;
    }


    public synchronized void setLogWriter(PrintWriter writer) {
        // Once a log writer has been set, we cannot set it since some
        // thread might be conditionally accessing it right now without
        // synchronizing.
        if (writer != null)
            _logWriter = writer;
    }


    public void setLoginTimeout(int seconds) {
        _loginTimeout = seconds;
    }


    public int getLoginTimeout() {
        return _loginTimeout;
    }


}
