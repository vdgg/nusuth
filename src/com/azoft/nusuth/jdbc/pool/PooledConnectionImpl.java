package com.azoft.nusuth.jdbc.pool;

import org.apache.log4j.Category;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Vector;
import java.util.Enumeration;

/**
 * Pooled connection.
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @version 0.5.5
 * @since 0.3.0
 */
public class PooledConnectionImpl implements PooledConnection {
    /** category to log */
    private static final Category CAT = Category.getInstance(PooledConnectionImpl.class.getName());
    /** list of listeners */
    final Vector listeners = new Vector();
    /** underlying connection */
    private Connection connection;
    /** connection proxy: object that was returned by getConnection */
    private ConnectionProxy connectionProxy;

    //---------------------------------------------------------------------------
    //--------- constructors ----------------------------------------------------
    //---------------------------------------------------------------------------

    public PooledConnectionImpl(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("the argument is null");
        }
        this.connection = connection;
        connectionProxy = null;
    }


    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    /** fire connection closed event */
    public synchronized void fireConnectionClosedEvent() {
        connectionProxy = null;

        ConnectionEvent evt = new ConnectionEvent(this);
        Enumeration e = ((Vector) listeners.clone()).elements();
        while (e.hasMoreElements()) {
            ConnectionEventListener l = (ConnectionEventListener) e.nextElement();
            l.connectionClosed(evt);
        }
    }

    /** fire connection error occurred event */
    public synchronized void fireConnectionErrorOccurred(SQLException exc) {
        connectionProxy = null;

        ConnectionEvent evt = new ConnectionEvent(this, exc);
        Enumeration e = ((Vector) listeners.clone()).elements();
        while (e.hasMoreElements()) {
            ConnectionEventListener l = (ConnectionEventListener) e.nextElement();
            l.connectionErrorOccurred(evt);
        }
    }

    public synchronized void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.addElement(listener);
    }

    public synchronized void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.removeElement(listener);
    }

    public synchronized void close() throws SQLException {
        if (connection == null) {
            throw new SQLException("Connection has been already closed.");
        }
        Connection con = connection;
        connection = null;

        if (CAT.isDebugEnabled()) {
            CAT.debug("pooled connection->close->started, closing underlying connection = " + con);
        }

        con.close();

        if (CAT.isDebugEnabled()) {
            CAT.debug("pooled connection->close->finished, closed underlying connection = " + con);
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Connection has been already closed.");
        }
        if (connectionProxy != null) {
            connectionProxy.internalClose();
        }
        connectionProxy = new ConnectionProxy(this, connection);

        return connectionProxy;
    }
}
