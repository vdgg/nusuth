package com.azoft.nusuth.jndi.java;


import javax.naming.*;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;


/**
 * Implements JNDI context for "java:" namespace
 */
public final class JavaContext implements Context, Serializable {

    public static final String JavaURL = "java:";

    private static final int JavaURLLength = JavaURL.length();

    private static Context context = null;

    private Hashtable _env = new Hashtable();

    public JavaContext() throws NamingException {
    }

    public JavaContext(Hashtable env) throws NamingException {
        _env = env;
        for (Iterator i = env.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            addToEnvironment(key, env.get(key));
        }
    }

    static public void initialize(Context initContext) {
        context = initContext;
    }

    public Object addToEnvironment(String s, Object obj) throws NamingException {
        return context.addToEnvironment(s, obj);
    }

    public void bind(String s, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public void bind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public void close() {
        _env.clear();
    }

    public String composeName(String s, String s1) {
        return s1 + "/" + s;
    }

    public Name composeName(Name name, Name name1) throws NamingException {
        Name name2 = new CompositeName();
        name2.addAll(name1);
        return name2.addAll(name);
    }

    public Context createSubcontext(String s) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public void destroySubcontext(String s) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public Hashtable getEnvironment() {
        return _env;
    }

    public String getNameInNamespace() throws NamingException {
        return JavaURL;
    }

    public NameParser getNameParser(String s) throws NamingException {
        if (!s.startsWith(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + s);
        } else {
            return context.getNameParser(s.substring(JavaURLLength));
        }
    }

    public NameParser getNameParser(Name name) throws NamingException {
        if (name.isEmpty() || !name.get(0).equals(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + name);
        } else {
            return context.getNameParser(name.getSuffix(1));
        }
    }

    public NamingEnumeration list(String s) throws NamingException {
        if (!s.startsWith(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + s);
        } else {
            return context.list(s.substring(JavaURLLength));
        }
    }

    public NamingEnumeration list(Name name) throws NamingException {
        if (name.isEmpty() || !name.get(0).equals(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + name);
        } else {
            return context.list(name.getSuffix(1));
        }
    }

    public NamingEnumeration listBindings(String s) throws NamingException {
        if (!s.startsWith(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + s);
        } else {
            return context.listBindings(s.substring(JavaURLLength));
        }
    }

    public NamingEnumeration listBindings(Name name) throws NamingException {
        if (name.isEmpty() || !name.get(0).equals(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + name);
        } else {
            return context.listBindings(name.getSuffix(1));
        }
    }

    public Object lookup(String s) throws NamingException {
        if (!s.startsWith(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + s);
        } else {
            return context.lookup(s.substring(JavaURLLength));
        }
    }

    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty() || !name.get(0).equals(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + name);
        } else {
            return context.lookup(name.getSuffix(1));
        }
    }

    public Object lookupLink(String s) throws NamingException {
        if (!s.startsWith(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + s);
        } else {
            return context.lookup(s.substring(JavaURLLength));
        }
    }

    public Object lookupLink(Name name) throws NamingException {
        if (name.isEmpty() || !name.get(0).equals(JavaURL)) {
            throw new NamingException("Internal error: context not accessed as java JavaURL: " + name);
        } else {
            return context.lookupLink(name.getSuffix(1));
        }
    }

    public void rebind(String s, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public void rebind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public Object removeFromEnvironment(String s) throws NamingException {
        return context.removeFromEnvironment(s);
    }

    public void rename(String s, String s1) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public void rename(Name name, Name name1) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public String toString() {
        return JavaURL;
    }

    public void unbind(String s) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }

    public void unbind(Name name) throws NamingException {
        throw new OperationNotSupportedException("Context is read-only");
    }
}

