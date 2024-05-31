package com.azoft.nusuth.jndi;

import com.azoft.nusuth.management.ComponentInfo;
import com.azoft.nusuth.security.AuthorizationRequiredException;
import com.azoft.nusuth.security.NusuthPrincipal;
import com.azoft.nusuth.security.NusuthPermission;
import com.azoft.nusuth.security.NusuthAcl;
import com.azoft.nusuth.jidep.NotificationType;
import com.azoft.nusuth.jidep.ClientJidepAdapter;
import com.azoft.nusuth.jidep.JidepNotificationHandler;

import javax.naming.directory.*;
import javax.naming.*;
import java.util.*;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;
import java.security.acl.Acl;

/**
 * This class is abstract. It represents distributed jndi context.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.21
 */
public abstract class DistributedJNDIContext
        implements DirContext, Serializable {

    protected Hashtable envProperties = new Hashtable();
    protected Hashtable names2attributes = new Hashtable();
    protected Hashtable attributes2names = new Hashtable();
    protected Hashtable bindings = new Hashtable();
    protected LinkedList cloneRoles = new LinkedList();
    protected Name contextName = new NusuthJNDIName();
    protected DistributedJNDIContext root = this;
    protected static JidepNotificationHandler notificationHandler = null;
    protected static String userName = null;
    protected static String password = null;
    protected static String localHost = null;
    protected static int localPort = -1;
    protected static Object lock = new Object();
    protected static boolean replicationInProgress = false;
    protected static Hashtable contextName2listeners = new Hashtable();
    protected static ContextChangingListener remoteListener = null;
    protected static org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jndi");

    /**
     * Returns lock object. Lock object used for locking any jndi operations
     * deals with context modifying during tree cloning.
     * @return lock object.
     */
    public Object getLock() {
        return lock;
    }

    /**
     * This method is used for subscribing on context changes.
     * @param subscriptions Array of context names to which subscribe.
     * @param listenr Listener that will listen for changes.
     */
    public abstract void subscribe(String[] subscriptions,
                                   DistributedJNDIContextListener listener);

//  public abstract void unsubscribe(String[] subscriptions);

    /**
     * This method sets local parameters to jnde tree.
     * @param user User name.
     * @param pass User password.
     * @param host Host on which tree start.
     * @param port Port on which tree start.
     */
    public static void setLocalParameters(String user, String pass,
                                          String host, int port) {
        userName = user;
        password = pass;
        localHost = host;
        localPort = port;
    }

    public JidepNotificationHandler getNotificationHandler() {
        return notificationHandler;
    }


    /**
     * Adds ContextChangingListener to the root context.
     * @param listener Listener for context changes.
     */
    public void addListener(String contextName,
                            ContextChangingListener listener) {
        if (root.equals(this)) {
            if (contextName2listeners.get(contextName) != null) {
                List list = (List) contextName2listeners.get(contextName);
                list.add(listener);
            } else {
                List list = new LinkedList();
                list.add(listener);
                contextName2listeners.put(contextName, list);
            }
        } else {
            root.addListener(contextName, listener);
        }
    }

    /**
     * Return list of ContextChangingListsner objects.
     * @return list of listeners.
     */
    public List getListeners(String contextName) {
        if (root.equals(this)) {
            if (contextName2listeners.get(contextName) != null) {
                return (List) contextName2listeners.get(contextName);
            } else {
                return new LinkedList();
            }
        } else {
            return root.getListeners(contextName);
        }
    }

    /**
     * This method used for login to another node of jndi tree.
     * @param userName Login name.
     * @param password Password.
     * @param host Host name to connect.
     * @param port Port number to connect.
     * @throws SocketException Throws if can't open connection.
     */
    protected abstract boolean login(String host, int port,
                                     ClientJidepAdapter adapter)
            throws SocketException;

    /**
     * This method sets the root context for the current.
     * @param context Root context.
     */
    public void setRootContext(DistributedJNDIContext context) {
        this.root = context;
        if (cloneRoles == null) {
            cloneRoles = new LinkedList();
        }
        Enumeration enum = bindings.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            Object value = bindings.get(key);
            if (value instanceof DistributedJNDIContext) {
                ((DistributedJNDIContext) value).setRootContext(context);
            }
        }
    }

    /**
     * Return attributes of the named context.
     * @param name Name of context.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public Attributes getAttributes(Name name) throws NamingException {
        return getAttributes(name, null);
    }

    /**
     * Return attributes of the named context.
     * @param name Name of context.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public Attributes getAttributes(String name) throws NamingException {
        return getAttributes(new NusuthJNDIName(name));
    }

    /**
     * Return attributes of the named context whith given ids.
     * @param name Name of context.
     * @param attrIds Attributes ids.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException {
        if (name.size() == 1) {
            if (bindings.get(name.get(0)) instanceof Context || name.isEmpty()) {
                Attributes attrs = (Attributes) names2attributes.get(name);
                BasicAttributes result = new BasicAttributes();
                if (attrs == null) {
                    return result;
                } else {
                    if (attrIds == null) {
                        return attrs;
                    } else if (attrIds.length == 0) {
                        return result;
                    } else {
                        Attribute attr;
                        for (int i = 0; i < attrIds.length; i++) {
                            attr = attrs.get(attrIds[i]);
                            if (attr != null) {
                                result.put(attr);
                            }
                        }
                        return result;
                    }
                }
            } else {
                return new BasicAttributes();
            }
        } else {
            //If this name consists of more than one element.
            Object obj = bindings.get(name.get(0));
            if (obj == null) {
                throw new InvalidNameException("Context " + name.get(0) + " not found");
            }
            try {
                return ((DirContext) obj).getAttributes(name.getSuffix(1), attrIds);
            } catch (ClassCastException cce) {
                throw new NotContextException("Object " + name.get(0)
                        + " is not dircontext");
            }
        }
    }

    /**
     * Return attributes of the named context whith given ids.
     * @param name Name of context.
     * @param attrIds Attributes ids.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public Attributes getAttributes(String name, String[] attrIds)
            throws NamingException {
        return getAttributes(new NusuthJNDIName(name), attrIds);
    }

    /**
     * Modify attributes of the named context and replicates its changes to
     * all other nodes if current context is replicable.
     * @param name Name of context.
     * @param attrs Attributes to modify.
     * @param mod_op Modification type.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException {
        int attrSize = attrs.size();
        if (attrSize == 0) {
            return;
        }
        ModificationItem[] items = new ModificationItem[attrSize];
        NamingEnumeration enum = attrs.getAll();
        for (int i = 0; i < attrSize; i++) {
            items[i] = new ModificationItem(mod_op, (Attribute) enum.next());
        }
        modifyAttributes(name, items);
    }

    /**
     * Modify attributes of the named context.
     * @param name Name of context.
     * @param attrs Attributes to modify.
     * @param mod_op Modification type.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public void localModifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException {
        int attrSize = attrs.size();
        if (attrSize == 0) {
            return;
        }
        ModificationItem[] items = new ModificationItem[attrSize];
        NamingEnumeration enum = attrs.getAll();
        for (int i = 0; i < attrSize; i++) {
            items[i] = new ModificationItem(mod_op, (Attribute) enum.next());
        }
        localModifyAttributes(name, items);
    }

    /**
     * Modify attributes of the named context and replicates its changes to
     * all other nodes if current context is replicable.
     * @param name Name of context.
     * @param attrs Attributes to modify.
     * @param mod_op Modification type.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public void modifyAttributes(String name, int mod_op, Attributes attrs)
            throws NamingException {
        modifyAttributes(new NusuthJNDIName(name), mod_op, attrs);
    }

    /**
     * Modify attributes of the named context.
     * @param name Name of context.
     * @param attrs Attributes to modify.
     * @param mod_op Modification type.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public void loacalModifyAttributes(String name, int mod_op, Attributes attrs)
            throws NamingException {
        localModifyAttributes(new NusuthJNDIName(name), mod_op, attrs);
    }

    /**
     * Modify attributes of the named context and replicates its changes to
     * all other nodes if current context is replicable.
     * @param name Name of context.
     * @param mods ModificationItems.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public void modifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException {
        if (replicationInProgress) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        //If this name consists of one element
        //(it means that the object with this name belongs to this context )
        if (name.size() == 1) {
            Object result = null;
            if ((result = lookup(name)) == null && !name.isEmpty()) {
                return;
            }
            if (result instanceof Context || name.isEmpty()) {
                Attributes attrs = (Attributes) names2attributes.get(name);
                if (attrs == null) {
                    attrs = new BasicAttributes();
                    names2attributes.put(name, attrs);
                }
                Attribute attr, toModify;
                int mod_op;
                List lst;
                NamingEnumeration ne;
                // processes the modifications
                for (int i = 0; i < mods.length; i++) {
                    attr = mods[i].getAttribute();
                    mod_op = mods[i].getModificationOp();
                    toModify = attrs.get(attr.getID());
                    switch (mod_op) {
                        case ADD_ATTRIBUTE:
                            //Adds this attribute to the list of attributes of the object
                            //with the given name
                            if (toModify == null) {
                                //If there were no attribute associated with this object :
                                toModify = attr;
                                attrs.put(toModify);
                                lst = (List) attributes2names.get(attr.getID());
                                if (lst == null) {
                                    lst = Collections.synchronizedList(new ArrayList());
                                    attributes2names.put(attr.getID(), lst);
                                }
                                synchronized (lst) {
                                    lst.add(new NameAttributePair(name, toModify));
                                }
                            } else {
                                //If there were attributes associated with this object:
                                //Then it simply adds new attributes.
                                ne = attr.getAll();
                                while (ne.hasMore()) {
                                    toModify.add(ne.next());
                                }
                            }
                            break;
                        case REPLACE_ATTRIBUTE:
                            // Replaces the object attributes:
                            if (toModify == null) {
                                //If there were no attribute associated with this object :
                                toModify = attr;
                                attrs.put(toModify);
                                lst = (List) attributes2names.get(attr.getID());
                                if (lst == null) {
                                    lst = Collections.synchronizedList(new ArrayList());
                                    attributes2names.put(attr.getID(), lst);
                                }
                                synchronized (lst) {
                                    lst.add(new NameAttributePair(name, toModify));
                                }
                            } else {
                                //If there were attributes associated with this object:
                                ne = attr.getAll();
                                if (!ne.hasMore())
                                    toModify.clear();
                                else
                                    while (ne.hasMore()) {
                                        toModify.add(ne.next());
                                    }
                            }
                            break;
                        case REMOVE_ATTRIBUTE:
                            // Removes attributes which are represented in the attr variable.
                            if (toModify != null) {
                                ne = attr.getAll();
                                while (ne.hasMore()) {
                                    toModify.remove(ne.next());
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
                String nameFromRoot = (contextName.isEmpty()
                        ? ""
                        : contextName + (name.isEmpty() ? "" : "/")) + name;
                if (isReplicable(nameFromRoot)) {
                    processReplication(nameFromRoot, "modify", mods);
                }
                String conName = contextName.toString();
                if (root.getListeners(conName).size() != 0) {
                    List list = root.getListeners(conName);
                    for (int i = 0; i < list.size(); i++) {
                        ContextChangingListener listener =
                                (ContextChangingListener) list.get(i);
                        listener.onContextChanged(contextName.toString(), nameFromRoot,
                                NotificationType.ATTRCHANGED);
                    }
                }
            } else {
                throw new OperationNotSupportedException();
            }
        } else {
            // if this name consists of more that one element (it means that the
            //needed object belongs to the sub context of this context)
            Object obj = bindings.get(name.get(0));
            if (obj == null)
                throw new InvalidNameException("Context " + name.get(0) + " not found");
            try {
                String nameFromRoot = (contextName.isEmpty()
                        ? ""
                        : contextName + (name.isEmpty() ? "" : "/")) + name;
                ((DirContext) obj).modifyAttributes(name.getSuffix(1), mods);
            } catch (ClassCastException cce) {
                throw new NotContextException("Object " + name.get(0)
                        + " is not dircontext");
            }
        }
    }

    /**
     * Modify attributes of the named context.
     * @param name Name of context.
     * @param mods ModificationItems.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public void localModifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException {
        if (replicationInProgress) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        //If this name consists of one element
        //(it means that the object with this name belongs to this context )
        if (name.size() == 1) {
            Object result = null;
            if ((result = localLookup(name)) == null && !name.isEmpty()) {
                return;
            }
            if (result instanceof Context || name.isEmpty()) {
                Attributes attrs = (Attributes) names2attributes.get(name);
                if (attrs == null) {
                    attrs = new BasicAttributes();
                    names2attributes.put(name, attrs);
                }
                Attribute attr, toModify;
                int mod_op;
                List lst;
                NamingEnumeration ne;
                // processes the modifications
                for (int i = 0; i < mods.length; i++) {
                    attr = mods[i].getAttribute();
                    mod_op = mods[i].getModificationOp();
                    toModify = attrs.get(attr.getID());
                    switch (mod_op) {
                        case ADD_ATTRIBUTE:
                            //Adds this attribute to the list of attributes of the object
                            //with the given name
                            if (toModify == null) {
                                //If there were no attribute associated with this object :
                                toModify = attr;
                                attrs.put(toModify);
                                lst = (List) attributes2names.get(attr.getID());
                                if (lst == null) {
                                    lst = Collections.synchronizedList(new ArrayList());
                                    attributes2names.put(attr.getID(), lst);
                                }
                                synchronized (lst) {
                                    lst.add(new NameAttributePair(name, toModify));
                                }
                            } else {
                                //If there were attributes associated with this object:
                                //Then it simply adds new attributes.
                                ne = attr.getAll();
                                while (ne.hasMore()) {
                                    toModify.add(ne.next());
                                }
                            }
                            break;
                        case REPLACE_ATTRIBUTE:
                            // Replaces the object attributes:
                            if (toModify == null) {
                                //If there were no attribute associated with this object :
                                toModify = attr;
                                attrs.put(toModify);
                                lst = (List) attributes2names.get(attr.getID());
                                if (lst == null) {
                                    lst = Collections.synchronizedList(new ArrayList());
                                    attributes2names.put(attr.getID(), lst);
                                }
                                synchronized (lst) {
                                    lst.add(new NameAttributePair(name, toModify));
                                }
                            } else {
                                //If there were attributes associated with this object:
                                ne = attr.getAll();
                                if (!ne.hasMore())
                                    toModify.clear();
                                else
                                    while (ne.hasMore()) {
                                        toModify.add(ne.next());
                                    }
                            }
                            break;
                        case REMOVE_ATTRIBUTE:
                            // Removes attributes which are represented in the attr variable.
                            if (toModify != null) {
                                ne = attr.getAll();
                                while (ne.hasMore()) {
                                    toModify.remove(ne.next());
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
                String conName = contextName.toString();
                if (root.getListeners(conName).size() != 0) {
                    String nameFromRoot = (contextName.isEmpty()
                            ? ""
                            : contextName + (name.isEmpty() ? "" : "/")) + name;
                    List list = root.getListeners(conName);
                    for (int i = 0; i < list.size(); i++) {
                        ContextChangingListener listener =
                                (ContextChangingListener) list.get(i);
                        listener.onContextChanged(contextName.toString(), nameFromRoot,
                                NotificationType.ATTRCHANGED);
                    }
                }
            } else {
                throw new OperationNotSupportedException();
            }
        } else {
            // if this name consists of more that one element (it means that the
            //needed object belongs to the sub context of this context)
            Object obj = bindings.get(name.get(0));
            if (obj == null)
                throw new InvalidNameException("Context " + name.get(0) + " not found");
            try {
                ((DistributedJNDIContext) obj).localModifyAttributes(name.getSuffix(1),
                        mods);
            } catch (ClassCastException cce) {
                throw new NotContextException("Object " + name.get(0)
                        + " is not dircontext");
            }
        }
    }

    /**
     * Modify attributes of the named context and replicates its changes to
     * all other nodes if current context is replicable.
     * @param name Name of context.
     * @param mods ModificationItems.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public void modifyAttributes(String name, ModificationItem[] mods)
            throws NamingException {
        modifyAttributes(new NusuthJNDIName(name), mods);
    }

    /**
     * Modify attributes of the named context.
     * @param name Name of context.
     * @param mods ModificationItems.
     * @throws NamingException Throws if name is not name of context or other
     * error occures.
     */
    public void localModifyAttributes(String name, ModificationItem[] mods)
            throws NamingException {
        localModifyAttributes(new NusuthJNDIName(name), mods);
    }

    /**
     * Binds given object to the current context with given name and attributes.
     * After binding it replicates context changes to all nodes.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @param attrs Attributes to bind.
     * @throws NamingException if any error occures during binding or binding
     * object is not instance of Context.
     */
    public void bind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        if (!(obj instanceof Context)) {
            throw new OperationNotSupportedException();
        } else {
            bind(name, obj);
            modifyAttributes(name, ADD_ATTRIBUTE, attrs);
        }
    }

    /**
     * Binds given object to the current context with given name and attributes.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @param attrs Attributes to bind.
     * @throws NamingException if any error occures during binding or binding
     * object is not instance of Context.
     */
    public void localBind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        if (!(obj instanceof Context)) {
            throw new OperationNotSupportedException();
        } else {
            localBind(name, obj);
            localModifyAttributes(name, ADD_ATTRIBUTE, attrs);
        }
    }

    /**
     * Binds given object to the current context with given name and attributes.
     * After binding it replicates context changes to all nodes.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @param attrs Attributes to bind.
     * @throws NamingException if any error occures during binding or binding
     * object is not instance of Context.
     */
    public void bind(String name, Object obj, Attributes attrs)
            throws NamingException {
        bind(new NusuthJNDIName(name), obj, attrs);
    }

    /**
     * Binds given object to the current context with given name and attributes.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @param attrs Attributes to bind.
     * @throws NamingException if any error occures during binding or binding
     * object is not instance of Context.
     */
    public void localBind(String name, Object obj, Attributes attrs)
            throws NamingException {
        localBind(new NusuthJNDIName(name), obj, attrs);
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public void rebind(Name name, Object obj, Attributes attrs)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion rebind with attributes "
                + "not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public void rebind(String name, Object obj, Attributes attrs)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion rebind with attributes "
                + "not supported");
    }

    /**
     * Creates subcontext of current context and replicates its changes to all
     * other nodes if current context is replicable. If attributes not contains
     * "Replicable" attribute it automaticly added with "true" value.
     * @param name Name of creating subcontext.
     * @param attrs Attributes of creating subcontext.
     * @throws NamingException Throws if any error occures during creating.
     */
    public abstract DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException;

    /**
     * Creates subcontext of current context. If attributes not contains
     * "Replicable" attribute it automaticly added with "true" value.
     * @param name Name of creating subcontext.
     * @param attrs Attributes of creating subcontext.
     * @throws NamingException Throws if any error occures during creating.
     */
    public abstract DirContext localCreateSubcontext(Name name, Attributes attrs)
            throws NamingException;

    /**
     * Creates subcontext of current context and replicates its changes to all
     * other nodes if current context is replicable. If attributes not contains
     * "Replicable" attribute it automaticly added with "true" value.
     * @param name Name of creating subcontext.
     * @param attrs Attributes of creating subcontext.
     * @throws NamingException Throws if any error occures during creating.
     */
    public DirContext createSubcontext(String name, Attributes attrs)
            throws NamingException {
        return createSubcontext(new NusuthJNDIName(name), attrs);
    }

    /**
     * Creates subcontext of current context. If attributes not contains
     * "Replicable" attribute it automaticly added with "true" value.
     * @param name Name of creating subcontext.
     * @param attrs Attributes of creating subcontext.
     * @throws NamingException Throws if any error occures during creating.
     */
    public DirContext localCreateSubcontext(String name, Attributes attrs)
            throws NamingException {
        return localCreateSubcontext(new NusuthJNDIName(name), attrs);
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public DirContext getSchema(Name name) throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public DirContext getSchema(String name) throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public DirContext getSchemaClassDefinition(String name)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NamingEnumeration search(Name name,
                                    Attributes matchingAttributes,
                                    String[] attributesToReturn)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NamingEnumeration search(String name,
                                    Attributes matchingAttributes,
                                    String[] attributesToReturn)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NamingEnumeration search(Name name,
                                    Attributes matchingAttributes)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NamingEnumeration search(String name,
                                    Attributes matchingAttributes)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NamingEnumeration search(Name name,
                                    String filter,
                                    SearchControls cons)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NamingEnumeration search(String name,
                                    String filter,
                                    SearchControls cons)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NamingEnumeration search(Name name,
                                    String filterExpr,
                                    Object[] filterArgs,
                                    SearchControls cons)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NamingEnumeration search(String name,
                                    String filterExpr,
                                    Object[] filterArgs,
                                    SearchControls cons)
            throws NamingException {
        throw new OperationNotSupportedException("Opertion not supported");
    }

    /**
     * This method lookup object with the given name and return it.
     * If context is not replicable then this method sends lookup request to the
     * node and then return object.
     * @param name Name of object to lookup.
     * @throws NamingException Throws if any error occures while looking.
     */
    public Object lookup(Name name) throws NamingException {
        if (name.isEmpty()) {
            return this;
        }
        String nameFromRoot = (contextName.isEmpty()
                ? ""
                : contextName + (name.isEmpty() ? "" : "/")) + name;
        if (isReplicable(nameFromRoot)) {
            Object result = bindings.get(name.get(0));
            if (name.size() == 1) {
                return result;
            } else {
                if (result == null) {
                    throw new NameNotFoundException("Name " + name.get(0) + " not found");
                }
                try {
                    return ((Context) result).lookup(name.getSuffix(1));
                } catch (ClassCastException cce) {
                    throw new NotContextException("Name " + name.get(0)
                            + " doesn't represent context");
                }
            }
        } else {
            Attributes attrs = root.getAttributes(nameFromRoot);
            if (attrs.size() == 0) {
                NusuthJNDIName nameRoot = new NusuthJNDIName(nameFromRoot);
                attrs = root.getAttributes(nameRoot.getPrefix(nameRoot.size() - 1));
            }
            Attribute attrHost = attrs.get("Node");
            String host = attrHost.get().toString();
            if (!host.equals(localHost + ":" + localPort)) {
                return redirectRequest(nameFromRoot, "lookup",
                        host.substring(0, host.indexOf(':')),
                        host.substring(host.indexOf(':') + 1));
            } else {
                return localLookup(name);
            }
        }
    }

    /**
     * This method lookup object with the given name and return it.
     * @param name Name of object to lookup.
     * @throws NamingException Throws if any error occures while looking.
     */
    private Object localLookup(Name name) throws NamingException {
        if (name.isEmpty()) {
            return this;
        }
        Object result = bindings.get(name.get(0));
        if (name.size() == 1) {
            return result;
        } else {
            if (result == null) {
                throw new NameNotFoundException("Name " + name.get(0)
                        + " not found");
            }
            try {
                return ((DistributedJNDIContext) result).
                        localLookup(name.getSuffix(1));
            } catch (ClassCastException cce) {
                throw new NotContextException("Name " + name.get(0)
                        + " doesn't represent context");
            }
        }
    }

    /**
     * This method lookup object with the given name and return it.
     * If context is not replicable then this method sends lookup request to the
     * node and then return object.
     * @param name Name of object to lookup.
     * @throws NamingException Throws if any error occures while looking.
     */
    public Object lookup(String name) throws NamingException {
        return lookup(new NusuthJNDIName(name));
    }

    /**
     * This method lookup object with the given name and return it.
     * @param name Name of object to lookup.
     * @throws NamingException Throws if any error occures while looking.
     */
    public Object localLookup(String name) throws NamingException {
        return localLookup(new NusuthJNDIName(name));
    }


    /**
     * Binds given object to the current context with given name.
     * After binding it replicates context changes to all nodes.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @throws NamingException if any error occures during binding.
     */
    public void bind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new NameAlreadyBoundException();
        }
        if (replicationInProgress) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        Object res = bindings.get(name.get(0));
        if (name.size() == 1) {
            if (res != null) {
                throw new NameAlreadyBoundException();
            }
            bindings.put(name.get(0), obj);
            String nameFromRoot = (contextName.isEmpty()
                    ? ""
                    : contextName + (name.isEmpty() ? "" : "/")) + name;
            if (isReplicable(nameFromRoot) && !(obj instanceof Context)) {
                processReplication(nameFromRoot, "bind", obj);
            }
            String conName = contextName.toString();
            if (root.getListeners(conName).size() != 0) {
                List list = root.getListeners(conName);
                for (int i = 0; i < list.size(); i++) {
                    ContextChangingListener listener
                            = (ContextChangingListener) list.get(i);
                    listener.onContextChanged(contextName.toString(), nameFromRoot,
                            NotificationType.CREATED);
                }
            }
        } else {
            if (res == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                ((Context) res).bind(name.getSuffix(1), obj);
            } catch (ClassCastException cce) {
                throw new NotContextException("Name " + name.get(0)
                        + " doesn't represent context");
            }
        }
    }

    /**
     * Binds given object to the current context with given name.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @throws NamingException if any error occures during binding.
     */
    public void localBind(Name name, Object obj) throws NamingException {
        if (name.isEmpty()) {
            throw new NameAlreadyBoundException();
        }
        if (replicationInProgress) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        Object res = bindings.get(name.get(0));
        if (name.size() == 1) {
            if (res != null) {
                throw new NameAlreadyBoundException();
            }
            bindings.put(name.get(0), obj);
            String conName = contextName.toString();
            if (root.getListeners(conName).size() != 0) {
                String nameFromRoot = (contextName.isEmpty()
                        ? ""
                        : contextName + (name.isEmpty() ? "" : "/")) + name;
                List list = root.getListeners(conName);
                for (int i = 0; i < list.size(); i++) {
                    ContextChangingListener listener =
                            (ContextChangingListener) list.get(i);
                    listener.onContextChanged(contextName.toString(), nameFromRoot,
                            NotificationType.CREATED);
                }
            }
        } else {
            if (res == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                ((DistributedJNDIContext) res).localBind(name.getSuffix(1), obj);
            } catch (ClassCastException cce) {
                throw new NotContextException("Name " + name.get(0)
                        + " doesn't represent context");
            }
        }
    }

    /**
     * Binds given object to the current context with given name.
     * After binding it replicates context changes to all nodes.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @throws NamingException if any error occures during binding.
     */
    public void bind(String name, Object obj) throws NamingException {
        bind(new NusuthJNDIName(name), obj);
    }

    /**
     * Binds given object to the current context with given name.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @throws NamingException if any error occures during binding.
     */
    public void localBind(String name, Object obj) throws NamingException {
        localBind(new NusuthJNDIName(name), obj);
    }

    /**
     * Rebinds given object to the context with given name.
     * After binding it replicates context changes to all nodes.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @throws NamingException if any error occures during binding.
     */
    public void rebind(Name name, Object obj) throws NamingException {
        Object res = bindings.get(name.get(0));
        if (name.size() == 1) {
            bindings.put(name.get(0), obj);
            String nameFromRoot = (contextName.isEmpty()
                    ? ""
                    : contextName + (name.isEmpty() ? "" : "/")) + name;
            if (isReplicable(nameFromRoot) && !(obj instanceof Context)) {
                processReplication(nameFromRoot, "rebind", obj);
            }
            String conName = contextName.toString();
            if (root.getListeners(conName).size() != 0 && root.equals(this)) {
                List list = root.getListeners(conName);
                for (int i = 0; i < list.size(); i++) {
                    ContextChangingListener listener
                            = (ContextChangingListener) list.get(i);
                    listener.onContextChanged(contextName.toString(), nameFromRoot,
                            NotificationType.REBINDED);
                }
            }
        } else {
            if (res == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                ((Context) res).rebind(name.getSuffix(1), obj);
            } catch (ClassCastException cce) {
                throw new NotContextException("Name " + name.get(0)
                        + " doesn't represent context");
            }
        }
    }

    /**
     * Rebinds given object to the context with given name.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @throws NamingException if any error occures during binding.
     */
    public void localRebind(Name name, Object obj) throws NamingException {
        Object res = bindings.get(name.get(0));
        if (name.size() == 1) {
            bindings.put(name.get(0), obj);
            String conName = contextName.toString();
            if (root.getListeners(conName).size() != 0) {
                String nameFromRoot = (contextName.isEmpty()
                        ? ""
                        : contextName + (name.isEmpty() ? "" : "/")) + name;
                List list = root.getListeners(conName);
                for (int i = 0; i < list.size(); i++) {
                    ContextChangingListener listener =
                            (ContextChangingListener) list.get(i);
                    listener.onContextChanged(contextName.toString(), nameFromRoot,
                            NotificationType.REBINDED);
                }
            }
        } else {
            if (res == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                ((DistributedJNDIContext) res).localRebind(name.getSuffix(1), obj);
            } catch (ClassCastException cce) {
                throw new NotContextException("Name " + name.get(0)
                        + " doesn't represent context");
            }
        }
    }

    /**
     * Rebinds given object to the context with given name.
     * After binding it replicates context changes to all nodes.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @throws NamingException if any error occures during binding.
     */
    public void rebind(String name, Object obj) throws NamingException {
        rebind(new NusuthJNDIName(name), obj);
    }

    /**
     * Rebinds given object to the context with given name.
     * @param name Name to bind.
     * @param obj Object to bind.
     * @throws NamingException if any error occures during binding.
     */
    public void localRebind(String name, Object obj) throws NamingException {
        localRebind(new NusuthJNDIName(name), obj);
    }

    /**
     * Unbind object with the given name and then replicates changes to all nodes
     * if context which contains this binding is replicable.
     * @param name Name of object to unbind.
     * @throws NamingException Throws if any error occures during unbinding.
     */
    public void unbind(Name name) throws NamingException {
        if (replicationInProgress) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        if (name.size() == 1) {
            bindings.remove(name.get(0));
            String nameFromRoot = (contextName.isEmpty()
                    ? ""
                    : contextName + (name.isEmpty() ? "" : "/")) + name;
            if (isReplicable(nameFromRoot)) {
                processReplication(nameFromRoot, "unbind", null);
            }
            String conName = contextName.toString();
            if (root.getListeners(conName).size() != 0 && root.equals(this)) {
                List list = root.getListeners(conName);
                for (int i = 0; i < list.size(); i++) {
                    ContextChangingListener listener
                            = (ContextChangingListener) list.get(i);
                    listener.onContextChanged(contextName.toString(), nameFromRoot,
                            NotificationType.DELETED);
                }
            }
        } else {
            Object obj = bindings.get(name.get(0));
            if (obj == null) {
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            }
            try {
                ((Context) obj).unbind(name.getSuffix(1));
            } catch (ClassCastException cce) {
                throw new NameNotFoundException("Name " + name.get(0)
                        + " doesn't represent context");
            }
        }
    }

    /**
     * Unbind object with the given name.
     * @param name Name of object to unbind.
     * @throws NamingException Throws if any error occures during unbinding.
     */
    public void localUnbind(Name name) throws NamingException {
        if (name.size() == 1) {
            bindings.remove(name.get(0));
            String conName = contextName.toString();
            if (root.getListeners(conName).size() != 0) {
                String nameFromRoot = (contextName.isEmpty()
                        ? ""
                        : contextName + (name.isEmpty() ? "" : "/")) + name;
                List list = root.getListeners(conName);
                for (int i = 0; i < list.size(); i++) {
                    ContextChangingListener listener =
                            (ContextChangingListener) list.get(i);
                    listener.onContextChanged(contextName.toString(), nameFromRoot,
                            NotificationType.DELETED);
                }
            }
        } else {
            Object obj = bindings.get(name.get(0));
            if (obj == null) {
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            }
            try {
                ((DistributedJNDIContext) obj).localUnbind(name.getSuffix(1));
            } catch (ClassCastException cce) {
                throw new NameNotFoundException("Name " + name.get(0)
                        + " doesn't represent context");
            }
        }
    }

    /**
     * Unbind object with the given name and then replicates changes to all nodes
     * if context which contains this binding is replicable.
     * @param name Name of object to unbind.
     * @throws NamingException Throws if any error occures during unbinding.
     */
    public void unbind(String name) throws NamingException {
        unbind(new NusuthJNDIName(name));
    }

    /**
     * Unbind object with the given name.
     * @param name Name of object to unbind.
     * @throws NamingException Throws if any error occures during unbinding.
     */
    public void localUnbind(String name) throws NamingException {
        localUnbind(new NusuthJNDIName(name));
    }

    /**
     * Renames object to the context with given new name.
     * After binding it replicates context changes to all nodes.
     * @param oldName Old name of object.
     * @param newName New name of object.
     * @throws NamingException if any error occures during binding.
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        Object obj = lookup(oldName);
        unbind(oldName);
        bind(newName, obj);
    }

    /**
     * Renames object to the context with given new name.
     * @param oldName Old name of object.
     * @param newName New name of object.
     * @throws NamingException if any error occures during binding.
     */
    public void localRename(Name oldName, Name newName) throws NamingException {
        Object obj = localLookup(oldName);
        localUnbind(oldName);
        localBind(newName, obj);
    }

    /**
     * Renames object to the context with given new name.
     * After binding it replicates context changes to all nodes.
     * @param oldName Old name of object.
     * @param newName New name of object.
     * @throws NamingException if any error occures during binding.
     */
    public void rename(String oldName, String newName) throws NamingException {
        rename(new NusuthJNDIName(oldName), new NusuthJNDIName(newName));
    }

    /**
     * Renames object to the context with given new name.
     * @param oldName Old name of object.
     * @param newName New name of object.
     * @throws NamingException if any error occures during binding.
     */
    public void localRename(String oldName, String newName)
            throws NamingException {
        localRename(new NusuthJNDIName(oldName), new NusuthJNDIName(newName));
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the names and class names of the
     *		bindings in this context.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration list(Name name) throws NamingException {
        String nameFromRoot = (contextName.isEmpty()
                ? ""
                : contextName + (name.isEmpty() ? "" : "/")) + name;
        if (isReplicable(nameFromRoot)) {
            if (name.isEmpty()) {
                NusuthNamingEnumeration jne = new NusuthNamingEnumeration();
                Enumeration bkeys = bindings.keys();
                String cname;
                while (bkeys.hasMoreElements()) {
                    cname = (String) bkeys.nextElement();
                    jne.addElement(new Binding(cname, bindings.get(cname)));
                }
                return jne;
            } else {
                Object obj = bindings.get(name.get(0));
                if (obj == null) {
                    throw new NameNotFoundException("Name " + name.get(0) + " not found");
                }
                try {
                    return ((Context) obj).list(name.getSuffix(1));
                } catch (ClassCastException cce) {
                    throw new NameNotFoundException("Name " + name.get(0)
                            + " doesn't represent context");
                }
            }
        } else {
            Attributes attrs = root.getAttributes(nameFromRoot);
            if (attrs.size() == 0) {
                NusuthJNDIName nameRoot = new NusuthJNDIName(nameFromRoot);
                attrs = root.getAttributes(nameRoot.getPrefix(nameRoot.size() - 1));
            }
            Attribute attrHost = attrs.get("Node");
            String host = attrHost.get().toString();
            if (!host.equals(localHost + ":" + localPort)) {
                return (NamingEnumeration)
                        redirectRequest(nameFromRoot, "list",
                                host.substring(0, host.indexOf(':')),
                                host.substring(host.indexOf(':') + 1));
            } else {
                return localList(name);
            }
        }
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the names and class names of the
     *		bindings in this context.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration localList(Name name) throws NamingException {
        if (name.isEmpty()) {
            NusuthNamingEnumeration jne = new NusuthNamingEnumeration();
            Enumeration bkeys = bindings.keys();
            String cname;
            while (bkeys.hasMoreElements()) {
                cname = (String) bkeys.nextElement();
                jne.addElement(new Binding(cname, bindings.get(cname)));
            }
            return jne;
        } else {
            Object obj = bindings.get(name.get(0));
            if (obj == null) {
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            }
            try {
                return ((DistributedJNDIContext) obj).localList(name.getSuffix(1));
            } catch (ClassCastException cce) {
                throw new NameNotFoundException("Name " + name.get(0)
                        + " doesn't represent context");
            }
        }
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the names and class names of the
     *		bindings in this context.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration list(String name) throws NamingException {
        return list(new NusuthJNDIName(name));
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the names and class names of the
     *		bindings in this context.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration localList(String name) throws NamingException {
        return localList(new NusuthJNDIName(name));
    }

    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the names and class names of the
     *		bindings in this context.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration listBindings(Name name) throws NamingException {
        return list(name);
    }


    /**
     * Enumerates the names bound in the named context, along with the
     * class names of objects bound to them.
     * @param name
     *		the name of the context to list
     * @return	an enumeration of the names and class names of the
     *		bindings in this context.  Each element of the
     *		enumeration is of type <tt>NameClassPair</tt>.
     * @throws	NamingException if a naming exception is encountered
     */
    public NamingEnumeration listBindings(String name) throws NamingException {
        return listBindings(new NusuthJNDIName(name));
    }

    /**
     * This method destroy subContext with given name and then replicates this
     * changes to all nodes.
     * @param name Name of context to destroy.
     * @throws NamingException Throws if error occures during destroying.
     */
    public void destroySubcontext(Name name) throws NamingException {
        Context ctx = null;
        try {
            ctx = (Context) lookup(name);
        } catch (ClassCastException cce) {
            throw new NotContextException();
        }
        if (list(name).hasMore()) {
            throw new ContextNotEmptyException();
        }
        ctx.close();
        unbind(name);
    }

    /**
     * This method destroy subContext with given name and then replicates this
     * changes to all nodes.
     * @param name Name of context to destroy.
     * @throws NamingException Throws if error occures during destroying.
     */
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(new NusuthJNDIName(name));
    }

    /**
     * Creates subcontext of current context and replicates its changes to all
     * other nodes if current context is replicable.
     * @param name Name of creating subcontext.
     * @throws NamingException Throws if any error occures during creating.
     */
    public Context createSubcontext(Name name) throws NamingException {
        Attributes attrs = new BasicAttributes();
        attrs.put("Replicable", new Boolean(true));
        return createSubcontext(name, attrs);
    }

    /**
     * Creates subcontext of current context.
     * @param name Name of creating subcontext.
     * @throws NamingException Throws if any error occures during creating.
     */
    public Context localCreateSubcontext(Name name) throws NamingException {
        Attributes attrs = new BasicAttributes();
        attrs.put("Replicable", new Boolean(true));
        return localCreateSubcontext(name, attrs);
    }

    /**
     * Creates subcontext of current context and replicates its changes to all
     * other nodes if current context is replicable.
     * @param name Name of creating subcontext.
     * @throws NamingException Throws if any error occures during creating.
     */
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(new NusuthJNDIName(name));
    }

    /**
     * Creates subcontext of current context.
     * @param name Name of creating subcontext.
     * @throws NamingException Throws if any error occures during creating.
     */
    public Context localCreateSubcontext(String name) throws NamingException {
        return localCreateSubcontext(new NusuthJNDIName(name));
    }

    /**
     * Invoke lookup(name).
     */
    public Object lookupLink(Name name) throws NamingException {
        return lookup(name);
    }

    /**
     * Invoke lookup(name).
     */
    public Object lookupLink(String name) throws NamingException {
        return lookup(name);
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NameParser getNameParser(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public NameParser getNameParser(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * Compose name with prefix.
     * @param name Name.
     * @param prefix Prefix.
     * @throws NamingException Throws if any error occures.
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        Name result = new NusuthJNDIName();
        return result.addAll(prefix).addAll(name);
    }

    /**
     * Compose name with prefix.
     * @param name Name.
     * @param prefix Prefix.
     * @throws NamingException Throws if any error occures.
     */
    public String composeName(String name, String prefix)
            throws NamingException {
        return composeName(new NusuthJNDIName(name),
                new NusuthJNDIName(prefix)).toString();
    }

    /**
     * Add property to the environments.
     * @param propName Property name.
     * @param propValue Property value
     * @return previous property value.
     * @throws NamingException Throws if any error occures.
     */
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        Object previous = envProperties.get(propName);
        envProperties.put(propName, propVal);
        return previous;
    }

    /**
     * Removes property from the environments.
     * @param propName Property name.
     * @return Property value.
     * @throws NamingException Throws if any error occures.
     */
    public Object removeFromEnvironment(String propName)
            throws NamingException {
        Object previous = envProperties.get(propName);
        envProperties.remove(propName);
        return previous;
    }

    /**
     * Return environments.
     * @return Environments.
     * @throws NamingException Throws if any error occures.
     */
    public Hashtable getEnvironment() throws NamingException {
        return envProperties;
    }

    public void close() throws NamingException {
    }

    /**
     * Operation not supported.
     * @throws Throws OperationNotSupportedException.
     */
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**
     * Return true if context with given name is replicable.
     * @param name Name of context from the root context.
     * @return true if context with given name is replicable.
     * @throws NamingException Throws if any error occures.
     */
    protected boolean isReplicable(Name name) throws NamingException {
        if (name.isEmpty()) {
            return true;
        }
        Object result = root.localLookup(name);
        if (result != null) {
            if (result instanceof Context) {
                Attributes attrs = root.getAttributes(name);
                Attribute attr = attrs.get("Replicable");
                if (attr != null) {
                    return ((Boolean) attr.get()).booleanValue();
                } else {
                    return true;
                }
            } else {
                return isReplicable(name.getPrefix(name.size() - 1));
            }
        }
        return true;
    }

    /**
     * Return true if context with given name is replicable.
     * @param name Name of context from the root context.
     * @return true if context with given name is replicable.
     * @throws NamingException Throws if any error occures.
     */
    protected boolean isReplicable(String fullName) throws NamingException {
        return isReplicable(new NusuthJNDIName(fullName));
    }

    /**
     * Redirect request to node with given host an port.
     * @param name Name of changed resource.
     * @param host Host of node.
     * @param port port of Node.
     * @param command Command.
     * @throws NamingException Throws if any error occures.
     * @return result of request.
     */
    private Object redirectRequest(String name, String command, String host,
                                   String port) throws NamingException {
        return redirectRequest(new NusuthJNDIName(name), command, host, port);
    }

    /**
     * Redirect request to node with given host an port.
     * @param name Name of changed resource.
     * @param host Host of node.
     * @param port port of Node.
     * @param command Command.
     * @throws NamingException Throws if any error occures.
     * @return result of request.
     */
    private Object redirectRequest(Name name, String command, String host,
                                   String port) throws NamingException {
        NamingEnumeration enum = root.list("internal");
        JNDICommand comm = new JNDICommand(name.toString(), command, null);
        while (enum.hasMore()) {
            Binding binding = (Binding) enum.next();
            if (binding.getObject() instanceof ComponentInfo) {
                ComponentInfo info = (ComponentInfo) binding.getObject();
                if (info.getHost().equals(host)
                        && info.getAdminPort() == Integer.parseInt(port)) {
                    try {
                        return redirectRequest(host, port, comm);
                    } catch (AuthorizationRequiredException e) {
                        cat.error("Cannot authorize at " + host + ":" + port, e);
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Redirect request to node with given host an port.
     * @param host Host of node.
     * @param port port of Node.
     * @param comm JndiCommand.
     * @throws NamingException Throws if any error occures.
     * @return result of request.
     */
    protected abstract Object redirectRequest(String host,
                                              String port, JNDICommand comm)
            throws AuthorizationRequiredException;

    /**
     * Replicates changes to all nodes.
     * @param name Name of changed resource.
     * @param command Command.
     * @param Parameter of command.
     * @throws NamingException Throws if any error occures.
     */
    protected void processReplication(String name, String command, Object obj)
            throws NamingException {
        processReplication(new NusuthJNDIName(name), command, obj);
    }

    /**
     * Replicates changes to all nodes.
     * @param name Name of changed resource.
     * @param command Command.
     * @param Parameter of command.
     * @throws NamingException Throws if any error occures.
     */
    protected void processReplication(Name name, String command, Object obj)
            throws NamingException {
        NamingEnumeration enum = root.list("internal");
        Object[] params = null;
        if (command.equals("create")) {
            Attributes attrs = root.getAttributes(name);
            params = new Object[attrs.size()];
            NamingEnumeration en = attrs.getAll();
            for (int i = 0; i < params.length; i++) {
                params[i] = en.next();
            }
        } else if (command.equals("bind")) {
            params = new Object[1];
            params[0] = obj;
        } else if (command.equals("rebind")) {
            params = new Object[1];
            params[0] = obj;
        } else if (command.equals("modify")) {
            params = (ModificationItem[]) obj;
        }
        JNDICommand comm = new JNDICommand(name.toString(), command, params);
        while (enum.hasMore()) {
            Binding binding = (Binding) enum.next();
            if (binding.getObject() instanceof ComponentInfo) {
                ComponentInfo info = (ComponentInfo) binding.getObject();
                if (!info.getHost().equals(localHost)
                        || info.getAdminPort() != localPort) {
                    try {
                        processRequest(info, comm);
                    } catch (AuthorizationRequiredException e) {
                        cat.error("Cannot authorize at " + info.getHost() + ":"
                                + info.getAdminPort(), e);
                    }
                }
            }
        }
    }

    /**
     * Return root context for the current context.
     * @return root context for the current context.
     */
    public DistributedJNDIContext getRootContext() {
        return root;
    }

    /**
     * Processed request for replication.
     * @param info ComponentInfo where replicate.
     * @param comm JNDICommand.
     * @throws AuthorizationRequiredException Throws if can't authorize.
     */
    protected abstract void processRequest(ComponentInfo info, JNDICommand comm)
            throws AuthorizationRequiredException;


    /**
     * Return all bindings of current Context.
     * @return all bindings of current Context.
     */
    protected Hashtable getBindings() {
        return bindings;
    }

    /**
     * Set bindings to the current context.
     * @param bindings Bindings to set.
     */
    protected void setBindings(Hashtable bindings) {
        this.bindings = bindings;
    }

    /**
     * Create new binding depending of security limitations and store all bindings
     * in given Hashtable.
     * @param bindings Hastable to store new bindings.
     * @roles List of user roles.
     * @throws NamingException Throws if any error occures.
     */
    public void createClone(Hashtable bindings, List roles)
            throws NamingException {
        String[] aclAttributeName = {"ACL"};
        String[] repAttributeName = {"Replicable"};
        NamingEnumeration enum = localList("");
        while (enum.hasMore()) {
            Binding binding = (Binding) enum.next();
            String name = binding.getName();
            if (binding.getObject() instanceof DistributedJNDIContext) {
                Attributes attributes = getAttributes(name, aclAttributeName);
                Attribute aclAttr = attributes.get("ACL");
                if (aclAttr != null) {
                    NusuthAcl acl = (NusuthAcl) aclAttr.get();
                    boolean allowed = false;
                    for (int i = 0; i < roles.size(); i++) {
                        allowed = acl.checkPermission(
                                new NusuthPrincipal((String) roles.get(i)),
                                new NusuthPermission("read"));
                        if (allowed) {
                            break;
                        }
                    }
                    if (allowed) {
                        attributes = getAttributes(name, repAttributeName);
                        Attribute repAttr = attributes.get("Replicable");
                        boolean replicable = ((Boolean) repAttr.get()).booleanValue();
//            if (replicable) {
//              DistributedJNDIContext con
//                      = (DistributedJNDIContext)binding.getObject();
//              root.addContext2OldBindings(con,
//                                          (Hashtable)con.getBindings().clone());
//              con.createClone(con.getBindings(), roles);
//            }
                    } else {
                        bindings.remove(name);
                    }
                } else {
                    bindings.remove(name);
                }
            }
        }
    }

    public void addCloneRoles(List roles) {
        for (int i = 0; i < roles.size(); i++) {
            cloneRoles.add(roles.get(i));
        }
        Enumeration enum = bindings.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            Object value = bindings.get(key);
            if (value instanceof DistributedJNDIContext) {
                ((DistributedJNDIContext) value).addCloneRoles(roles);
            }
        }
    }

    private void clearCloneRoles() {
        cloneRoles.clear();
        Enumeration enum = bindings.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            Object value = bindings.get(key);
            if (value instanceof DistributedJNDIContext) {
                ((DistributedJNDIContext) value).clearCloneRoles();
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        try {
            replicationInProgress = true;
            Hashtable newBindings = (Hashtable) bindings.clone();
            createClone(newBindings, cloneRoles);
            out.writeObject(contextName);
            out.writeObject(envProperties);
            out.writeObject(newBindings);
            out.writeObject(names2attributes);
            out.writeObject(attributes2names);
            clearCloneRoles();
        } catch (NamingException e) {
            cat.error("Cannot write jndi tree to the output", e);
        } finally {
            replicationInProgress = false;
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        contextName = (Name) in.readObject();
        envProperties = (Hashtable) in.readObject();
        bindings = (Hashtable) in.readObject();
        names2attributes = (Hashtable) in.readObject();
        attributes2names = (Hashtable) in.readObject();
    }

}
