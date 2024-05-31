package com.azoft.nusuth.jndi.java;


import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import java.lang.reflect.Constructor;
import java.util.Hashtable;


public class javaURLContextFactory implements ObjectFactory {

    public javaURLContextFactory() {
    }

    public Object getObjectInstance(Object obj, Name name, Context context, Hashtable hashtable) throws NamingException {
        if (obj == null) {
            return createJavaContext(hashtable);
        } else if (obj instanceof String) {
            if (((String) obj).startsWith("java:")) {
                return createJavaContext(hashtable);
            } else {
                return null;
            }
        } else if (obj instanceof String[]) {
            String[] objUrls = (String[]) obj;
            if (objUrls.length > 0 || objUrls[0].startsWith("java:")) {
                return createJavaContext(hashtable);
            } else {
                return null;
            }
        } else {
            return createJavaContext(hashtable);
        }
    }

    private Object createJavaContext(Hashtable env) throws NamingException {
        try {
            Class javaContextClass = Class.forName(JavaContext.class.getName(), true, Thread.currentThread().getContextClassLoader());
            Constructor constructor = javaContextClass.getConstructor(new Class[]{Hashtable.class});
            return constructor.newInstance(new Object[]{env});
        } catch (Exception ex) {
            throw new NamingException("Couldn't create JavaContext, nested: " + ex.getMessage());
        }
    }
}

