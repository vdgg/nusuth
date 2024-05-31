package com.azoft.nusuth.core;

import java.io.PrintWriter;
import java.sql.*;
import javax.sql.*;

public class NusuthJdbcDataSource
        implements DataSource {
    private String url = "";

    public NusuthJdbcDataSource(String url) {
        this.url = url;
    }

    public Connection getConnection()
            throws SQLException {
        return DriverManager.getConnection(url);
    }

    public Connection getConnection(String username, String password)
            throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public PrintWriter getLogWriter()
            throws SQLException {
        return DriverManager.getLogWriter();
    }

    public void setLogWriter(PrintWriter out)
            throws SQLException {
        DriverManager.setLogWriter(out);
    }

    public int getLoginTimeout()
            throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds)
            throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }
}

