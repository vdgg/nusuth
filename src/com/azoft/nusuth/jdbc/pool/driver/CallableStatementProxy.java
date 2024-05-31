package com.azoft.nusuth.jdbc.pool.driver;

import java.io.InputStream;
import java.io.Reader;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Map;

import java.sql.Connection;
import java.sql.CallableStatement;
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
 * {@link CallableStatement} proxy.
 *
 * @see PreparedStatementProxy
 * @see StatementProxy
 *
 * @author Constantine Plotnikov
 * @author Vladislav Dutov
 *
 * @version 0.5.5
 * @since 0.3.7
 */
public class CallableStatementProxy extends PreparedStatementProxy implements CallableStatement {
    //---------------------------------------------------------------------------
    //---- constructors ---------------------------------------------------------
    //---------------------------------------------------------------------------

    public CallableStatementProxy(CallableStatement cstmt, DataSourceAdapter ds) {
        super(cstmt, ds);
    }

    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------
    //---------------------------------------------------------------------------

    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        ((CallableStatement) stmt).registerOutParameter(parameterIndex, sqlType);
    }

    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        ((CallableStatement) stmt).registerOutParameter(parameterIndex, sqlType, scale);
    }

    public boolean wasNull() throws SQLException {
        return ((CallableStatement) stmt).wasNull();
    }

    public String getString(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getString(parameterIndex);
    }

    public boolean getBoolean(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getBoolean(parameterIndex);
    }

    public byte getByte(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getByte(parameterIndex);
    }

    public short getShort(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getShort(parameterIndex);
    }

    public int getInt(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getInt(parameterIndex);
    }

    public long getLong(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getLong(parameterIndex);
    }

    public float getFloat(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getFloat(parameterIndex);
    }

    public double getDouble(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getDouble(parameterIndex);
    }

    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return ((CallableStatement) stmt).getBigDecimal(parameterIndex, scale);
    }

    public byte[] getBytes(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getBytes(parameterIndex);
    }

    public Date getDate(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getDate(parameterIndex);
    }

    public Time getTime(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getTime(parameterIndex);
    }

    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getTimestamp(parameterIndex);
    }

    //----- Advanced features ---------------------------------------------------

    public Object getObject(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getObject(parameterIndex);
    }

    //-------------------------- JDBC 2.0 ---------------------------------------

    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return ((CallableStatement) stmt).getBigDecimal(parameterIndex);
    }

    public Object getObject(int i, Map map) throws SQLException {
        return ((CallableStatement) stmt).getObject(i, map);
    }

    public Ref getRef(int i) throws SQLException {
        return ((CallableStatement) stmt).getRef(i);
    }

    public Blob getBlob(int i) throws SQLException {
        return ((CallableStatement) stmt).getBlob(i);
    }

    public Clob getClob(int i) throws SQLException {
        return ((CallableStatement) stmt).getClob(i);
    }

    public Array getArray(int i) throws SQLException {
        return ((CallableStatement) stmt).getArray(i);
    }

    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return ((CallableStatement) stmt).getDate(parameterIndex, cal);
    }

    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return ((CallableStatement) stmt).getTime(parameterIndex, cal);
    }

    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return ((CallableStatement) stmt).getTimestamp(parameterIndex, cal);
    }

    public void registerOutParameter(int paramIndex, int sqlType, String typeName) throws SQLException {
        ((CallableStatement) stmt).registerOutParameter(paramIndex, sqlType, typeName);
    }
}
