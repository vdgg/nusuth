package com.azoft.nusuth.jdbc.xa;

import java.sql.SQLException;


/**
 * Defines two-phase commit support for a JDBC connection used by
 * {@link XAConnectionImpl}. A JDBC connection that can implement any
 * of these features should extend this interface and attempt to
 * implement as much as it can.
 * <p>
 * {@link #prepare} is used as part of the two phase commit protocol
 * to determine whether the transaction can commit or must rollback.
 * Failure to implement this method will cause all connections to vote
 * for commit, whether or not they can actually commit, leading to
 * mixed heuristics.
 * <p>
 * {@link #enableSQLTransactions} allows the SQL begin/commit/rollback
 * commands to be disabled for the duration of a transaction managed
 * through an {@link javax.transaction.xa.XAResource}, preventing the
 * application from demarcating transactions directly.
 * <p>
 * {@link #isCriticalError} is used to tell if an exception thrown by
 * the connection is fatal and the connection should not be returned
 * to the pool.
 *
 * @version 0.5.5
 * @since 0.0.1
 */
public interface TwoPhaseConnection {

    /**
     * Enables or disables transaction demarcation through SQL commit
     * and rollback. When the connection falls under control of
     * {@link XAConnectionImpl}, SQL commit/rollback commands will be
     * disabled to prevent direct transaction demarcation.
     *
     * @param flag True to enable SQL transactions (the default)
     */
    public void enableSQLTransactions(boolean flag);


    /**
     * Called to prepare the transaction for commit. Returns true if
     * the transaction is prepared, false if the transaction is
     * read-only. If the transaction has been marked for rollback,
     * throws a {@link RollbackException}.
     *
     * @return True if can commit, false if read-only
     * @throws SQLException If transaction has been marked for
     *   rollback or cannot commit for any reason
     */
    public boolean prepare() throws SQLException;


    /**
     * Returns true if the error issued by this connection is a
     * critical error and the connection should be terminated.
     *
     * @param except The exception thrown by this connection
     */
    public boolean isCriticalError(SQLException except);

}
