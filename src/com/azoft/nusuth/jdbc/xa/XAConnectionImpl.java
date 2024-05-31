package com.azoft.nusuth.jdbc.xa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;
import javax.sql.XAConnection;
import javax.sql.PooledConnection;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Status;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;

import org.apache.log4j.Category;


/**
 * Implements an X/A connection that can be pooled and managed from
 * inside a transaction monitor. This is the XA connection returned
 * to the application server from the {@link XADataSourceImpl} and
 * will be used to obtain {@link ClientConnection} for the
 * application.
 * <p>
 * If the transaction is managed through the JDBC interface, this
 * connection will reference the underlying JDBC connection directly.
 * If this resource is enlisted with a global transaction through
 * the {@link XAResource} interface, it will reference a transactional
 * connection, or {@link TxConnection}. Such a connection may be
 * shared by two or more XA connections enlisted with the same
 * transaction.
 *
 * @see ClientConnection
 * @see ConnectionEventListener
 * @see TxConnection
 *
 * @version 0.5.5
 * @since 0.0.1
 */
public final class XAConnectionImpl
        implements XAConnection, XAResource, Synchronization {
    private static final Category CAT = Category.getInstance(XAConnectionImpl.class.getName());

    /**
     * This is the underlying JDBC connection represented
     * by this pooled connection. This variable may initially be null,
     * in which case {@link #getUnderlying} will return a new
     * connection and set this variable. This variable is mutually
     * exclusive with {@link #_txConn} and is always null for
     * connections inside a transaction.
     */
    Connection _underlying;


    /**
     * If this connection is part of a global transaction, this
     * object identifies the transaction. The transaction's
     * underlying JDBC connection is exposed through this object and
     * {@link #_underlying} is null. If this connection is closed,
     * then the connection has been timedout. Commit/rollback will
     * always set this variable to null.
     */
    private TxConnection _txConn;


    /**
     * The client connection last handed to the application. If the
     * application calls {@link #getConnection} again, we should hand
     * out a new client connection and render the previous one closed.
     */
    // No longer in use, see _clientId
    //private ClientConnection            _clientConn;


    /**
     * An event listener can be registered and notified when the
     * client connection has been closed by the application or a
     * fatal error rendered it unuseable.
     */
    private ConnectionEventListener _listener;


    /**
     * The resource manager is used to share connections within the
     * same transaction.
     */
    private XADataSourceImpl _resManager;


    /**
     * This is an identifier we hand to the client connection when we
     * create it. When the client connection asks for the underlying
     * connection, we compare the identifiers. If since that point we
     * created a new client connection, we regard an old client
     * connection as discarded and do not hand it the underlying
     * connection.
     * <p>
     * Previously, when a new client connection was created, we used
     * a reference to the old one to terminate it. This proved to
     * not work well, since the client connection could never be
     * finalized.
     */
    private int _clientId = 1;


    /**
     * Construct a new XA/pooled connection with the underlying JDBC
     * connection suitable for this driver only. This is a one to one
     * mapping between this connection and the underlying connection.
     * The underlying connection is only provided for pooled
     * connections. XA connections are suspect of being enlisted with
     * a global transaction which might already bear an underlying
     * connection. If not, one will be created later on.
     */
    XAConnectionImpl(XADataSourceImpl resManager, Connection underlying) {
        _underlying = underlying;
        _resManager = resManager;
    }


    public void close() throws SQLException {
        if (CAT.isDebugEnabled()) CAT.debug("XAConnectionImpl->close()");
        // This is our indication that this connection has been
        // closed programmatically.
        if (_resManager == null)
            throw new SQLException("This connection has been closed");

        // The client connection is no longer useable.
        /* Deprecated: see _clientId
        if(_clientConn != null)
          _clientConn.terminate();
        */
        _clientId = -1;

        // The underlying connection is closed and this connection
        // is no longer useable. This method can be called any number
        // of times (e.g. we use it in finalizer).  We do not handle
        // transactions, we just kill the connection.
        Connection underlying = _underlying;
        try {
            if (underlying != null) {
                //_underlying.commit();
                underlying.close();
            } else if (_txConn != null) {
                try {
                    end(_txConn.xid, TMSUCCESS);
                } catch (XAException except) {
                    CAT.debug("XAConnection: error during end() (ignored):", except);
                }
            }
        } finally {
            synchronized (this) {
                _resManager = null;
                _underlying = null;
                _txConn = null;
                _listener = null;
            }
        }
    }


    public XAResource getXAResource() {
        // The connection acts as it's own resource manager
        return this;
    }


    public void addConnectionEventListener(ConnectionEventListener listener) {
        if (listener == null)
            throw new NullPointerException("XAConnection: Argument 'listener' is null");

        // !TODO: is there an opotunity of registring several event listeners????
        synchronized (this) {
            _listener = listener;
        }
    }


    public void removeConnectionEventListener(ConnectionEventListener listener) {
        if (listener == null)
            throw new NullPointerException("XAConnection: Argument 'listener' is null");
        if (_listener == null || _listener != listener)
            throw new IllegalStateException("XAConnection: Listener never registered with this pooled connection");
        synchronized (this) {
            _listener = null;
        }
    }


    public java.sql.Connection getConnection() throws SQLException {
        // If this pooled connection has been closed, throw an exception.
        if (_resManager == null) {
            throw new SQLException("This connection has been closed");
        }

        // If getConnection() was called before and the underlying
        // connection was not closed, we take it away from the previous
        // recieved as per the PooledConnection design.
        /* Deprecated: see _clientId
        if(_clientConn != null)
          _clientConn.terminate();
        */


        // If we are handling an underlying connection, we commit the
        // old transaction and are ready to work for a new one.
        // If we are part of a global transaction we hope that end/
        // start were called properly, but we're not longer in that
        // transaction.
        /*
        if(_underlying != null)
        {
          try
          {
            _underlying.commit();
          }
          catch(SQLException except)
          {
            ConnectionEvent event;

            if(_listener != null) {
              event = new ConnectionEvent( this, except );
              _listener.connectionErrorOccurred( event );
            }
          }
        }
        */
        /*
        TransactionManager tm = _resManager.getTransactionManager();
        if(tm != null) {
          Transaction transaction = null;
          try {
            transaction  = tm.getTransaction();
          } catch(SystemException ex) {
            CAT.error("Unable to get transaction", ex);
            throw new SQLException(ex.getMessage());
          }

          try {
            if(transaction != null) {
              transaction.enlistResource( getXAResource() );
              //transaction.registerSynchronization((Synchronization)this);
            }
          } catch(Exception ex) {
            CAT.error("Unable to enlist resource in transaction/register synchronization listener", ex);
            throw new SQLException(ex.getMessage());
          }
        }
        */

        // Create a new ClientConnection which will be returned to the
        // application. The ClientConnection cannot be closed directly
        // and cannot manage it's own transactions.
        /* Deprecated: see _clientId
        _clientConn = new ClientConnection( this );
        return _clientConn;
        */

        ClientConnection result = new ClientConnection(this, ++_clientId);
        if (CAT.isDebugEnabled()) CAT.debug("returning new ClientConnection = " + result);
        return result;
    }


    /**
     * Called by {@link ClientConnection} to notify that the application
     * has attempted to close the connection. After this call, the client
     * connection is no longer useable and this pooled connection can be
     * reused. The event listener is notified immediately.
     *
     * @param clientId The {@link ClientConnection} identifier
     */
    void notifyClose(int clientId) {
        ConnectionEvent event;
        if (CAT.isDebugEnabled()) CAT.debug("XAConnectionImpl -> notifyClose()");

        // ClientConnection has been closed, we dissociated it from
        // the underlying connection and notify any listener that this
        // pooled connection can be reused.
        /* Deprecated: see clientId
        _clientConn.terminate();
        _clientConn = null;
        */
        // We have to expect being called by a ClientConnection that we
        // no longer regard as valid. That's acceptable, we just ignore.
        if (clientId != _clientId)
            return;

        // If we are handling an underlying connection, we commit the
        // old transaction and are ready to work for a new one.
        // If we are part of a global transaction we hope that end/
        // start were called properly.
        /*
        if(_underlying != null)
        {
          try
          {
            System.out.println("Underlying : " + _underlying);
            _underlying.close();
            CAT.info("Closing underlying connection");
          }
          catch(SQLException except)
          {
            if(_listener != null)
            {
              event = new ConnectionEvent( this, except );
              _listener.connectionErrorOccurred( event );
            }
            return;
          }
        }
        */
        // Notify the listener.
        ConnectionEventListener l = _listener;
        if (l != null) {
            event = new ConnectionEvent(this);
            l.connectionClosed(event);
        }
    }


    /**
     * Called by {@link ClientConnection} to notify that an error
     * occured with the underlying connection. If the error is
     * critical, the underlying connection is closed and the listener
     * is notified.
     *
     * @param clientId The {@link ClientConnection} identifier
     * @param except The exception raised by the underlying connection
     */
    void notifyError(int clientId, SQLException except) {
        CAT.error("XAConnectionImpl : connection error on clientId = " + clientId + " exception = " + except);

        ConnectionEvent event;
        /*

        if(clientId != _clientId)
          return;

        // If the connection is not two-phase commit we cannot determine
        // whether the error is critical, we just return. If the connection
        // is two phase commit, but the error is not critical, we return.
        if(_underlying != null)
        {
          if(!(_underlying instanceof TwoPhaseConnection) ||
             !((TwoPhaseConnection)_underlying).isCriticalError(except))
            return;
          if(_txConn.conn == null ||
             !(_txConn.conn instanceof TwoPhaseConnection) ||
             !((TwoPhaseConnection)_txConn.conn).isCriticalError(except))
            return;
        }

        // The client connection is no longer useable, the underlying
        // connection (if used) is closed, the TxConnection (if used)
        // is rolledback and this connection dies (but close() may
        // still be called).
        ++_clientId;
        if(_underlying != null)
        {
          try
          {
            _underlying.close();
          }
          catch(SQLException e2)
          {
            // Ignore that, we know there's an error.
          }
          _underlying = null;
        }
        else if(_txConn != null)
        {
          try
          {
            end( _txConn.xid, TMFAIL );
          }
          catch(XAException e2)
          {
            // Ignore that, we know there's an error.
          }
          _txConn = null;
        }
        */
        // Notify the listener.

        ConnectionEventListener l = _listener;
        if (l != null) {
            event = new ConnectionEvent(this, except);
            l.connectionErrorOccurred(event);
        }
    }


    protected void finalize() throws Throwable {
        // We are no longer referenced by anyone (including the
        // connection pool). Time to close down.
        close();
    }


    public String toString() {
        if (_underlying != null)
            return "XAConnection: " + _underlying;
        else
            return "XAConnection: unused";
    }


    public void start(Xid xid, int flags)
            throws XAException {

        CAT.info("XAConnectionImpl -> start()");
        // General checks.
        if (xid == null)
            throw new XAException(XAException.XAER_INVAL);
        if (_txConn != null)
            throw new XAException(XAException.XAER_OUTSIDE);

        if (flags == TMNOFLAGS) {
            // Starting a new transaction. First, make sure it is
            // not shared with any other connection (need to join
            // for that).
            if (_resManager.getTxConnection(xid) != null)
                throw new XAException(XAException.XAER_DUPID);

            // Create a new TxConnection to describe this
            // connection in the context of a transaction and
            // register it with the resource manager so it can
            // be shared.
            TxConnection txConn;
            try {
                txConn = new TxConnection();

                if (CAT.isDebugEnabled()) CAT.debug("created new TxConnection: " + txConn);

                Connection underlying = _underlying;
                if (underlying != null) {
                    if (CAT.isDebugEnabled()) CAT.debug("Underlying connection: " + underlying);
                    txConn.conn = underlying;
                    synchronized (this) {
                        _underlying = null;
                    }
                } else {
                    if (CAT.isDebugEnabled()) CAT.debug("Creating new connection -> _txConn.conn");
                    txConn.conn = _resManager.newConnection();
                    if (CAT.isDebugEnabled()) CAT.debug("Creating new connection: _txConn.conn = " + txConn.conn);
                }
                txConn.xid = xid;
                txConn.count = 1;
                txConn.started = System.currentTimeMillis();
                txConn.timeout = txConn.started + (_resManager.getTransactionTimeout() * 1000);
                if (CAT.isDebugEnabled()) {
                    CAT.debug("Transaction started : xid = " + xid + ", time = " + txConn.started);
                    CAT.debug("Set timeout time for xid = " + xid + ", timeout = " + txConn.timeout);
                }
                _resManager.setTxConnection(xid, txConn);
                synchronized (this) {
                    _txConn = txConn;
                }

                txConn.conn.setAutoCommit(false);
                try {
                    int isolationLevel = _resManager.getIsolationLevel();
                    if (isolationLevel != Connection.TRANSACTION_NONE)
                        txConn.conn.setTransactionIsolation(isolationLevel);
                } catch (SQLException e) {
                    CAT.error("XAConnection: cannot initialize isolation level", e);
                    // The underlying driver might not support this
                    // isolation level that we use by default.
                }
                if (txConn.conn instanceof TwoPhaseConnection)
                    ((TwoPhaseConnection) txConn.conn).enableSQLTransactions(false);
            } catch (SQLException except) {
                CAT.error("XAConnection: failed to begin a transaction:", except);
                // If error occured at this point, we can only
                // report it as resource manager error.
                if (_resManager.getLogWriter() != null)
                    _resManager.getLogWriter().println("XAConnection: failed to begin a transaction: " + except);
                throw new XAException(XAException.XAER_RMERR);
            }
        } else if (flags == TMJOIN || flags == TMRESUME) {
            // We are joining another transaction with an
            // existing TxConnection.
            TxConnection txConn = _resManager.getTxConnection(xid);
            synchronized (this) {
                _txConn = txConn;
            }
            if (txConn == null)
                throw new XAException(XAException.XAER_INVAL);

            // Update the number of XAConnections sharing this
            // transaction connection.
            if (flags == TMJOIN && txConn.count == 0)
                throw new XAException(XAException.XAER_PROTO);
            ++txConn.count;

            // If we already have an underlying connection (as we can
            // expect to), we should release that underlying connection
            // and make it available to the resource manager.
            if (_underlying != null) {
                // _resManager.releaseConnection( _underlying );
                synchronized (this) {
                    _underlying = null;
                }
            }
        } else {
            // No other flags supported in start().
            throw new XAException(XAException.XAER_INVAL);
        }
    }


    public void end(Xid xid, int flags) throws XAException {
        // General checks.
        CAT.info("XAConnectionImpl -> end()");
        if (xid == null)
            throw new XAException(XAException.XAER_INVAL);
        // Note: we could get end with success or failure even it
        // we were previously excluded from the transaction.
        if (_txConn == null && flags == TMSUSPEND)
            throw new XAException(XAException.XAER_NOTA);

        if (flags == TMSUCCESS || flags == TMFAIL) {
            // We are now leaving a transaction we started or
            // joined before. We can expect any of prepare/
            // commit/rollback to be called next, so TxConnection
            // is still valid.

            // If we were suspended from the transaction, we'll
            // join it for the duration of this operation.
            // Make sure the reference count reaches zero by the
            // time we get to prepare.
            TxConnection txConn = _txConn;
            if (txConn == null) {

                txConn = _resManager.getTxConnection(xid);
                if (txConn == null) {
                    throw new XAException(XAException.XAER_NOTA);
                }
                synchronized (this) {
                    _txConn = txConn;
                }
            } else {
                if (txConn.xid != null && !txConn.xid.equals(xid))
                    throw new XAException(XAException.XAER_NOTA);
                --txConn.count;
            }

            // If transaction failed, we can rollback the
            // transaction and release the underlying connection.
            // We can expect all other resources to recieved the
            // same end notification. We don't expect forget to happen.
            if (flags == TMFAIL && txConn.conn != null) {
                try {
                    if (txConn.conn instanceof TwoPhaseConnection)
                        ((TwoPhaseConnection) txConn.conn).enableSQLTransactions(true);
                    txConn.conn.rollback();
                    // _resManager.releaseConnection(_txConn.conn);
                } catch (SQLException except) {
                    CAT.error("XAConnection: cannot rollback underlying connection:", except);
                    // There is a problem with the underlying
                    // connection, but it was not added to the poll.
                }
                _resManager.setTxConnection(txConn.xid, null);
                synchronized (this) {
                    _txConn.conn = null;
                    _txConn.xid = null;
                }
            }

            if (flags == TMSUCCESS) {
                // We should be looking for a new transaction.
                // Next thing we might be participating in a new
                // transaction while the current one is being
                // rolled back.
                synchronized (this) {
                    _txConn = null;
                }
            }
        } else if (flags == TMSUSPEND) {
            // We no longer take part in this transaction.
            // Possibly we'll be asked to resume later on, but
            // right now we have to forget about the transaction
            // and the underlying connection.

            synchronized (this) {
                if (_txConn != null) {
                    _txConn.count--;
                }
                _txConn = null;
            }
        } else {
            // No other flags supported in end().
            throw new XAException(XAException.XAER_INVAL);
        }
    }


    public void forget(Xid xid) throws XAException {
        if (CAT.isDebugEnabled()) CAT.debug("XAConnectionImpl -> forget( xid )");
        TxConnection txConn;

        // General checks.
        if (xid == null)
            throw new XAException(XAException.XAER_INVAL);

        // We have to forget about the transaction, meaning the
        // transaction no longer exists for this or any other
        // connection. We might be called multiple times.
        txConn = _resManager.setTxConnection(xid, null);
        if (_txConn == txConn) {
            synchronized (this) {
                _txConn = null;
            }
        }
        if (txConn != null) {
            if (txConn.conn != null) {
                try {
                    txConn.conn.close();
                    if (CAT.isDebugEnabled()) CAT.debug("Closed underlying connection");
                } catch (Exception ex) {
                    // TODO : what to do in this case? Throw an exception?
                    CAT.error("Failed to close connection", ex);
                }
                txConn.conn = null;
            }
            txConn.xid = null;
        }
    }


    public int prepare(Xid xid) throws XAException {
        TxConnection txConn;

        // General checks.
        if (xid == null)
            throw new XAException(XAException.XAER_INVAL);

        // Technically, prepare may be called for any connection,
        // not just this one.
        txConn = _resManager.getTxConnection(xid);
        if (txConn == null)
            throw new XAException(XAException.XAER_NOTA);

        // This is an error and should never happen. All other
        // parties in the transaction should have left it before.
        if (txConn.count > 0)
            throw new XAException(XAException.XAER_PROTO);

        // If the transaction failed, we have to force a rollback.
        // We track the case of failure due to a timeout.
        if (txConn.timedOut)
            throw new XAException(XAException.XA_RBTIMEOUT);
        if (txConn.conn == null)
            throw new XAException(XAException.XA_RBROLLBACK);

        // Since there is no preparation mechanism in a generic
        // JDBC driver, we only test for read-only transaction
        // but do not commit at this point.

        /* no SQLException as txConn.conn.isReadOnly() was commented
        try
        {
        */
        txConn.prepared = true;
        if (txConn.conn instanceof TwoPhaseConnection) {
            // For 2pc connection we ask it to prepare and determine
            // whether it's commiting or read-only. If a rollback
            // exception happens, we report it.
            try {
                if (((TwoPhaseConnection) txConn.conn).prepare()) {
                    return XA_OK;
                } else {
                    txConn.readOnly = true;
                    return XA_RDONLY;
                }
            } catch (SQLException except) {
                CAT.error("XAConnection: cannot prepare", except);
                throw new XAException(XAException.XA_RBROLLBACK);
            }
        } else {
            // For standard connection we cannot prepare, we can
            // only guess if it's read only.
            /** isReadOnly is not supported by some databases like informix.
             * maybe this will be reintroduced later.
             *
             * @author Constantine Plotnikov
             *
             if(txConn.conn.isReadOnly())
             {
             txConn.readOnly = true;
             return XA_RDONLY;
             }*/
            return XA_OK;
        }
        /* no SQLException as txConn.conn.isReadOnly() was commented
        }
        catch(SQLException except)
        {
          CAT.error("Exception during prepare",except);
          try
          {
            // Fatal error in the connection, kill it.
            txConn.conn.close();
          }
          catch(SQLException e)
          {
          }
          txConn.conn = null;
          if(_resManager.getLogWriter() != null)
            _resManager.getLogWriter().println( "XAConnection: failed to commit a transaction: " + except );
          // If we cannot commit the transaction, force a rollback.
          throw new XAException(XAException.XA_RBROLLBACK);
        }
        */
    }


    public Xid[] recover(int flags) throws XAException {
        synchronized (_resManager) {
            return _resManager.getTxRecover();
        }
    }


    public void commit(Xid xid, boolean onePhase)
            throws XAException {
        CAT.info("XAConnectionImpl -> commit(..)");
        TxConnection txConn;

        // General checks.
        if (xid == null)
            throw new XAException(XAException.XAER_INVAL);

        // Technically, commit may be called for any connection,
        // not just this one.
        txConn = _resManager.getTxConnection(xid);
        if (txConn == null)
            throw new XAException(XAException.XAER_NOTA);

        // If the transaction failed, we have to force
        // a rollback.
        if (txConn.conn == null)
            throw new XAException(XAException.XA_RBROLLBACK);

        // If connection has been prepared and is read-only,
        // nothing to do at this stage.
        if (txConn.readOnly)
            return;

        // This must be a one-phase commite, or the connection
        // should have been prepared before.
        if (onePhase || txConn.prepared) {
            try {
                // Prevent multiple commit attempts.
                txConn.readOnly = true;
                if (txConn.conn instanceof TwoPhaseConnection)
                    ((TwoPhaseConnection) txConn.conn).enableSQLTransactions(true);
                txConn.conn.commit();
                forget(xid);
            } catch (SQLException except) {
                CAT.error("XAConnection: failed to commit a transaction:", except);
                try {
                    // Unknown error in the connection, better kill it.
                    txConn.conn.close();
                } catch (SQLException e) {
                    CAT.debug("XAConnection: failed to commit a transaction. Cannot close connection", e);
                }
                txConn.conn = null;
                if (_resManager.getLogWriter() != null)
                    _resManager.getLogWriter().println("XAConnection: failed to commit a transaction: " + except);
                // If we cannot commit the transaction, a heuristic tollback.
                throw new XAException(XAException.XA_HEURRB);
            }
        } else {
            // 2pc we should have prepared before.
            if (!txConn.prepared)
                throw new XAException(XAException.XAER_PROTO);
        }
    }


    public void rollback(Xid xid) throws XAException {
        TxConnection txConn;
        CAT.info("Rollback started, xid = " + xid);

        // General checks.
        if (xid == null)
            throw new XAException(XAException.XAER_INVAL);

        // Technically, rollback may be called for any connection,
        // not just this one.
        txConn = _resManager.getTxConnection(xid);
        if (txConn == null)
            throw new XAException(XAException.XAER_NOTA);

        // If connection has been prepared and is read-only,
        // nothing to do at this stage. If connection has
        // been terminated any other way, nothing to do
        // either.
        if (txConn.readOnly || txConn.conn == null)
            return;

        try {
            txConn.prepared = false;
            if (txConn.conn instanceof TwoPhaseConnection)
                ((TwoPhaseConnection) txConn.conn).enableSQLTransactions(true);
            txConn.conn.rollback();
        } catch (SQLException except) {
            CAT.error("XAConnection: failed to rollback a transaction:", except);
            /*try
            {
              // Unknown error in the connection, better kill it.
              txConn.conn.close();
            }
            catch(SQLException e)
            {
              CAT.debug("XAConnection: failed to rollback a transaction. Cannot close connection", e);
            }
            txConn.conn = null;
            */
            if (_resManager.getLogWriter() != null)
                _resManager.getLogWriter().println("XAConnection: failed to rollback a transaction: " + except);
            // If we cannot commit the transaction, a heuristic tollback.
            throw new XAException(XAException.XA_RBROLLBACK);
        } finally {
            forget(xid);
        }

    }


    public boolean isSameRM(XAResource xaRes) throws XAException {
        // Two resource managers are equal if they produce equivalent
        // connection (i.e. same database, same user). If the two are
        // equivalent they would share a transaction by joining.
        if (xaRes == null || !(xaRes instanceof XAConnectionImpl))
            return false;
        if (_resManager.equals(((XAConnectionImpl) xaRes)._resManager))
            return true;
        return false;
    }


    public boolean setTransactionTimeout(int seconds) throws XAException {
        if (seconds < 0)
            throw new XAException(XAException.XAER_INVAL);
        // Zero resets to the default for all transactions.
        if (seconds == 0)
            seconds = _resManager.getTransactionTimeout();
        // If a transaction has started, change it's timeout to the new value.
        TxConnection txConn = _txConn;
        if (txConn != null) {
            synchronized (txConn) {
                txConn.timeout = txConn.started + (seconds * 1000);
            }
            return true;
        }
        return false;
    }


    public int getTransactionTimeout() {
        long timeout;

        TxConnection txConn = _txConn;
        if (txConn == null)
            return 0;
        return (int) (txConn.timeout - txConn.started) / 1000;
    }


    /**
     * Returns true if this connection is inside a global transaction.
     * If the connection is inside a global transaction it will not
     * allow commit/rollback directly from the {@link
     * java.sql.Connection} interface.
     */
    boolean insideGlobalTx() {
        return (_txConn != null);
    }


    /**
     * Called to obtain the underlying connections. If this connection
     * is part of a transaction, the transction's underlying connection
     * is returned, or an exception is thrown if the connection was
     * terminated due to timeout. If this connection is not part of a
     * transaction, a non-transactional connection is returned.
     *
     * @param clientId The {@link ClientConnection} identifier
     */
    Connection getUnderlying(int clientId) throws SQLException {
        // If we were notified of the client closing, or have been
        // requested to have a new client connection since then,
        // the client id will not match to that of the caller.
        // We use that to decide that the caller has been closed.
        if (clientId != _clientId)
            throw new SQLException("This application connection has been closed");

        TxConnection txConn = _txConn;
        if (txConn != null) {
            if (txConn.timedOut)
                throw new SQLException("The transaction has timed out and has been rolledback and closed");
            if (_txConn.conn == null)
                throw new SQLException("The transaction has been terminated and this connection has been closed");
            // CAT.info("Returning connection associated with a transaction");
            return txConn.conn;
        }
        if (_underlying == null) {
            // CAT.info("XAConnectionImpl->getUnderlying() creating new connection");
            synchronized (this) {
                _underlying = _resManager.newConnection();
            }
            _underlying.setAutoCommit(true);
        }
        return _underlying;
    }


    //----------
    // Synchronization methods implementation
    // ---------

    public void beforeCompletion() {
        // DO NOTHING
    }

    public void afterCompletion(int status) {
        // System.out.println("After Completion");
        if (_resManager == null) {
            CAT.warn("Resource manager in NULL");
        }

    }
}
