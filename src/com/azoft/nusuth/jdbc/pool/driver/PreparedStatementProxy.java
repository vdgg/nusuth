package com.azoft.nusuth.jdbc.pool.driver;

import org.apache.log4j.Category;

import java.io.InputStream;
import java.io.Reader;

import java.math.BigDecimal;
import java.util.Calendar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLWarning;
import java.sql.SQLException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * {@link PreparedStatement} proxy.
 * It has functionality to retry
 * {@link #execute()},
 * {@link #executeUpdate()},
 * {@link #executeQuery()}
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
public class PreparedStatementProxy extends StatementProxy implements PreparedStatement {
    /** category to log */
    private static final Category CAT = Category.getInstance(PreparedStatementProxy.class.getName());
    //---------------------------------------------------------------------------
    //---- constructors ---------------------------------------------------------
    //---------------------------------------------------------------------------

    public PreparedStatementProxy(PreparedStatement pstmt, DataSourceAdapter ds) {
        super(pstmt, ds);
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    public boolean execute() throws SQLException {
        for (int i = 0; true; i++) {
            try {
                return ((PreparedStatement) stmt).execute();
            } catch (SQLException exc) {
                if (!isExecuteRetryException(exc, CAT, i)) {
                    throw exc;
                }
            }
        }
    }

    public int executeUpdate() throws SQLException {
        for (int i = 0; true; i++) {
            try {
                return ((PreparedStatement) stmt).executeUpdate();
            } catch (SQLException exc) {
                if (!isExecuteUpdateRetryException(exc, CAT, i)) {
                    throw exc;
                }
            }
        }
    }

    public ResultSet executeQuery() throws SQLException {
        for (int i = 0; true; i++) {
            try {
                return ((PreparedStatement) stmt).executeQuery();
            } catch (SQLException exc) {
                if (!isExecuteQueryRetryException(exc, CAT, i)) {
                    throw exc;
                }
            }
        }
    }


    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        ((PreparedStatement) stmt).setNull(parameterIndex, sqlType);
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        ((PreparedStatement) stmt).setBoolean(parameterIndex, x);
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        ((PreparedStatement) stmt).setByte(parameterIndex, x);
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        ((PreparedStatement) stmt).setShort(parameterIndex, x);
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        ((PreparedStatement) stmt).setInt(parameterIndex, x);
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        ((PreparedStatement) stmt).setLong(parameterIndex, x);
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        ((PreparedStatement) stmt).setFloat(parameterIndex, x);
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        ((PreparedStatement) stmt).setDouble(parameterIndex, x);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        ((PreparedStatement) stmt).setBigDecimal(parameterIndex, x);
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        ((PreparedStatement) stmt).setString(parameterIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        ((PreparedStatement) stmt).setBytes(parameterIndex, x);
    }

    public void setDate(int parameterIndex, Date x) throws SQLException {
        ((PreparedStatement) stmt).setDate(parameterIndex, x);
    }

    public void setTime(int parameterIndex, Time x) throws SQLException {
        ((PreparedStatement) stmt).setTime(parameterIndex, x);
    }

    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        ((PreparedStatement) stmt).setTimestamp(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ((PreparedStatement) stmt).setAsciiStream(parameterIndex, x, length);
    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ((PreparedStatement) stmt).setUnicodeStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        ((PreparedStatement) stmt).setBinaryStream(parameterIndex, x, length);
    }


    public void clearParameters() throws SQLException {
        ((PreparedStatement) stmt).clearParameters();
    }


    //-------- Advanced features ------------------------------------------------

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
        ((PreparedStatement) stmt).setObject(parameterIndex, x, targetSqlType, scale);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        ((PreparedStatement) stmt).setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        ((PreparedStatement) stmt).setObject(parameterIndex, x);
    }


    //-------------------------- JDBC 2.0 ---------------------------------------

    public void addBatch() throws SQLException {
        ((PreparedStatement) stmt).addBatch();
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        ((PreparedStatement) stmt).setCharacterStream(parameterIndex, reader, length);
    }

    public void setRef(int i, Ref x) throws SQLException {
        ((PreparedStatement) stmt).setRef(i, x);
    }

    public void setBlob(int i, Blob x) throws SQLException {
        ((PreparedStatement) stmt).setBlob(i, x);
    }

    public void setClob(int i, Clob x) throws SQLException {
        ((PreparedStatement) stmt).setClob(i, x);
    }

    public void setArray(int i, Array x) throws SQLException {
        ((PreparedStatement) stmt).setArray(i, x);
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return ((PreparedStatement) stmt).getMetaData();
    }

    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        ((PreparedStatement) stmt).setDate(parameterIndex, x, cal);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        ((PreparedStatement) stmt).setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        ((PreparedStatement) stmt).setTimestamp(parameterIndex, x, cal);
    }

    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        ((PreparedStatement) stmt).setNull(paramIndex, sqlType, typeName);
    }
}
