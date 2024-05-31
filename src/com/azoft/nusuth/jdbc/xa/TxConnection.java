package com.azoft.nusuth.jdbc.xa;

import java.sql.Connection;
import javax.transaction.xa.Xid;


/**
 * Describes an open connection associated with a transaction. When a
 * transaction is opened for a connection, this record is created for
 * the connection. It indicates the underlying JDBC connection and
 * transaction Xid. Multiple XA connection that fall under the same
 * transaction Xid will share the same TxConnection object.
 *
 *
 * @author <a href="arkin@exoffice.com">Assaf Arkin</a>
 *
 * @see Xid
 * @see XAConnectionImpl
 *
 * @version 0.5.5
 * @since 0.0.1
 */
final class TxConnection {


    /**
     * The Xid of the transactions. Connections that are not
     * associated with a transaction are not represented here.
     */
    Xid xid;


    /**
     * Holds the underlying JDBC connection for as long as this
     * connection is useable. If the connection has been rolled back,
     * timed out or had any other error, this variable will null
     * and the connection is considered failed.
     */
    Connection conn;


    /**
     * Indicates the clock time (in ms) when the transaction should
     * time out. The transaction times out when
     * <tt>System.currentTimeMillis() > timeout</tt>.
     */
    long timeout;


    /**
     * Indicates the clock time (in ms) when the transaction started.
     */
    long started;


    /**
     * Reference counter indicates how many XA connections share this
     * underlying connection and transaction. Always one or more.
     */
    int count;


    /**
     * True if the transaction has failed due to time out.
     */
    boolean timedOut;


    /**
     * True if the transaction has already been prepared.
     */
    boolean prepared;


    /**
     * True if the transaction has been prepared and found out to be
     * read-only. Read-only transactions do not require commit/rollback.
     */
    boolean readOnly;

}

