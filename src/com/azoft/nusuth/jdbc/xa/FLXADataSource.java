package com.azoft.nusuth.jdbc.xa;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Properties;
import java.util.Hashtable;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Driver;
import java.rmi.Remote;

import javax.transaction.*;
import javax.transaction.xa.*;
import javax.sql.*;
import javax.naming.*;
import javax.naming.spi.ObjectFactory;

import org.apache.log4j.Category;

/**
 * @version 0.5.5
 * @since 0.0.1
 */
public class FLXADataSource implements DataSource,
        ConnectionEventListener,
        /*Referenceable,*/
        /*ObjectFactory,*/
        Serializable {
    /** Logging category */
    private static final Category CAT = Category.getInstance(FLXADataSource.class.getName());

    /** Underlying XADataSource */
    XADataSource _xaUnderlying;

    /** Undelying DataSource */
    DataSource _underlying;

    /** Transaction Manager */
    TransactionManager transactionManager = null;


    /** Description of this datasource */
    private String _description = "FLXADataSource";


    /**
     * The default isolation level for all global transactions.
     * <tt>TRANSACTION_NONE</tt> specifies that the driver picks
     * it's own default isolation level.
     */
    private int _isolationLevel = Connection.TRANSACTION_SERIALIZABLE;


    /**
     * Each datasource maintains it's own driver, in case of
     * driver-specific setup (e.g. pools, log writer).
     */
    private transient Driver _driver;

    private HashMap enlistedResources = new HashMap();


    public FLXADataSource(XADataSource xaUnderlying, DataSource underlying) throws SQLException {
        if (xaUnderlying == null) {
            throw new IllegalArgumentException("XA DataSource is null");
        }

        if (underlying == null) {
            throw new IllegalArgumentException("DataSource is null");
        }

        _xaUnderlying = xaUnderlying;
        _xaUnderlying.setLogWriter(DriverManager.getLogWriter());
        _xaUnderlying.setLoginTimeout(DriverManager.getLoginTimeout());
        _underlying = underlying;
    }


    public FLXADataSource() throws SQLException {
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

    public XADataSource getXaDataSource() {
        return _xaUnderlying;
    }

    public void setXaDataSource(XADataSource newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("dataSource is null");
        }
        _xaUnderlying = newValue;
    }

    public Connection getConnection() throws SQLException {
        Transaction transaction = null;
        if (transactionManager != null) {
            try {
                transaction = transactionManager.getTransaction();
            } catch (javax.transaction.SystemException ex) {
                String msg = "Unable to get transaction from transaction manager";
                CAT.error(msg, ex);
                throw new SQLException(msg);
            }
        } else {
            if (CAT.isDebugEnabled()) CAT.debug("Transaction manager is null");
        }

        if (transaction != null) {
            // transactional case
            XAConnection xaCon = _xaUnderlying.getXAConnection();
            if (xaCon == null) throw new SQLException("Can not get connection : connection in underlying ds is null");
            try {
                xaCon.addConnectionEventListener(this);
            } catch (IllegalStateException ex) {
                // do nothing.
            }

            try {
                XAResource resource = xaCon.getXAResource();
                if (resource != null /*&& !enlistedResources.containsKey(resource)*/) {
                    transaction.enlistResource(resource);
                    synchronized (enlistedResources) {
                        enlistedResources.put(resource, resource);
                    }
                    if (CAT.isDebugEnabled()) CAT.debug("Enlisted resource into a transaction");
                } else {
                    String msg = "Resource for XAConnection is null";
                    CAT.error(msg);
                    throw new SQLException(msg);
                }
            } catch (Exception ex) {
                CAT.error("Unable to enlist resource into a transaction", ex);
                throw new SQLException("unable to enlist resource into a transaction : " + ex.getMessage());
            }
            return xaCon.getConnection();
        } else {
            // non-transactional case
            if (CAT.isDebugEnabled()) CAT.debug("Transaction is null, getting non-transactional connection");
            Connection con = _underlying.getConnection();
            con.setAutoCommit(true);
            return con;
        }
    }


    public Connection getConnection(String user, String password)
            throws SQLException {
        return _xaUnderlying.getXAConnection(user, password).getConnection();
    }


    /**
     * Sets the description of this datasource.
     * The standard name for this property is <tt>description</tt>.
     *
     * @param description The description of this datasource
     */
    public synchronized void setDescription(String description) {
        if (description == null)
            throw new NullPointerException("DataSource: Argument 'description' is null");
        _description = description;
    }


    /**
     * Returns the description of this datasource.
     * The standard name for this property is <tt>description</tt>.
     *
     * @return The description of this datasource
     */
    public String getDescription() {
        return _description;
    }


    //--------------------------------------
    // ConnectionEventListener methods
    //--------------------------------------

    /**
     * Connection Closed.
     */
    public void connectionClosed(ConnectionEvent e) {
        if (transactionManager == null) return;
        try {
            Transaction transaction = transactionManager.getTransaction();
            if (transaction != null) {
                XAConnection source = (XAConnection) e.getSource();
                int flag;
                int status = transaction.getStatus();
                switch (status) {
                    case Status.STATUS_ACTIVE:
                        flag = XAResource.TMSUCCESS;
                        break;
                    default:
                        flag = XAResource.TMFAIL;
                }
                XAResource xaResource = source.getXAResource();
                if (xaResource != null) {
                    transaction.delistResource(xaResource, flag);
                    synchronized (enlistedResources) {
                        enlistedResources.remove(xaResource);
                    }
                    CAT.info("delisted resource from a transaction");
                } else {
                    CAT.error("invalid XAResource : null");
                }
            }
        } catch (Exception ex) {
            CAT.error("error", ex);
        }
    }


    public void connectionErrorOccurred(ConnectionEvent event) {
        String msg = "FLXADataSource -> Connection error occured : ";
        if (event != null) {
            msg += "connection = " + event.getSource() + " exception = " + event.getSQLException();
        } else {
            msg += "connectionEvent = null";
        }
        CAT.error(msg);
    }


    public int getLoginTimeout() throws SQLException {
        return _xaUnderlying.getLoginTimeout();
    }

    public void setLoginTimeout(int t) throws SQLException {
        _xaUnderlying.setLoginTimeout(t);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return _xaUnderlying.getLogWriter();
    }

    public void setLogWriter(PrintWriter pw) throws SQLException {
        _xaUnderlying.setLogWriter(pw);
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager tm) {
        transactionManager = tm;
    }

    /**
     * Returns true if this datasource and the other are equal.
     * The two datasources are equal if and only if they will produce
     * the exact same connections. Connection properties like database
     * name, user name, etc are comapred. Setup properties like
     * description, log writer, etc are not compared.
     */
/*
  public synchronized boolean equals( Object other )
  {
    if(other == this)
      return true;
    if(other == null || ! (other instanceof FLXADataSource))
      return false;

    FLXADataSource with;

    with = (FLXADataSource) other;
    String _url = getDbURL();
    if(_url == null || !_url.equals(with.getDbURL()))
      return false;
    String _user = getUser();
    String _password = getPassword();
    if((_user == null && with.getUser() == null) ||
       (_user != null && _password != null && _user.equals(with.getUser()) &&
        _password.equals(with.getPassword())))
      return true;
    return false;
  }
*/

    public String toString() {
        if (_description != null) {
            return _description;
        } else {
            String url;

            return "DataSource";
        }
    }

/*
  public synchronized Reference getReference()
  {
    Reference ref;

    // We use same object as factory.
    ref = new Reference( getClass().getName(), getClass().getName(), null );
    // Mandatory properties
    ref.add( new StringRefAddr( "description", _description ));
    ref.add( new StringRefAddr( "loginTimeout", Integer.toString(_loginTimeout)));

    if(getDbURL() == null)
      ref.add( new StringRefAddr( "driverURL", "no driver url" ) );
    else
      ref.add( new StringRefAddr( "driverURL", getDbURL() ));

    // Optional properties

    String _driverClassName = getDriverClassName();
    String _user = getUser();
    String _password = getPassword();
    if(_driverClassName != null)
      ref.add( new StringRefAddr( "driverClassName", _driverClassName ));
    if(_user != null)
      ref.add( new StringRefAddr( "user", _user ));
    if(_password != null)
      ref.add( new StringRefAddr( "password", _password ));
    ref.add( new StringRefAddr( "isolationLevel", Integer.toString(getIsolationLevel()) ) );
    ref.add( new StringRefAddr( "transactionTimeout", Integer.toString(getTransactionTimeout())));
    return ref;
  }
*/
/*
  public Object getObjectInstance( Object refObj, Name name, Context nameCtx, Hashtable env )
  throws NamingException
  {
    Reference ref;

    // Can only reconstruct from a reference.
    if(refObj instanceof Reference)
    {
      ref = (Reference) refObj;
      // Make sure reference is of datasource class.
      if(ref.getClassName().equals( getClass().getName() ))
      {

        FLXADataSource ds;
        RefAddr        addr;

        try
        {
          ds = (FLXADataSource)Class.forName(ref.getClassName()).newInstance();
        }
        catch(Exception except)
        {
          throw new NamingException( except.toString() );
        }
        // Mandatory properties
        ds.setDbURL((String)ref.get("driverURL").getContent());
        ds._description = (String)ref.get("description").getContent();
        ds._loginTimeout = Integer.parseInt((String)ref.get("loginTimeout").getContent());
        // Optional properties
        addr = ref.get("driverClassName");
        if(addr != null)
          ds.setDriverClassName((String)addr.getContent());
        addr = ref.get("user");
        if(addr != null)
          ds.setUser((String)addr.getContent());
        addr = ref.get("password");
        if(addr != null)
          ds.setPassword((String)addr.getContent());
        addr = ref.get("transactionTimeout");
        if(addr != null)
          setTransactionTimeout( Integer.parseInt((String)addr.getContent() ));
        addr = ref.get("isolationLevel");
        if(addr != null)
          setIsolationLevel(Integer.parseInt((String)addr.getContent()));
        return ds;

      }
      else
        throw new NamingException( "DataSource: Reference not constructed from class " + getClass().getName() );
    }
    else if(refObj instanceof Remote)
    {
      return refObj;
    }
    else
    {
      return null;
    }
  }
*/
}

