package com.azoft.nusuth.jidep;

import com.azoft.nusuth.jndi.*;
import com.azoft.nusuth.security.AuthorizationRequiredException;
import com.azoft.nusuth.management.ComponentInfo;
import com.azoft.nusuth.core.FixedLengthServletInputStream;
import com.azoft.nusuth.core.ChunkedServletInputStream;

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.net.SocketException;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;

/**
 * This is implementation of DistributedJNDIContext for JIDEP protocol.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.14
 */
public class JidepDistributedJNDIContext extends DistributedJNDIContext {

    /**
     * Constructor for context which intend for cloning tree form other node.
     * @param host Remote node host.
     * @param Remote node port.
     */
    public JidepDistributedJNDIContext(String host, int port)
            throws SocketException, AuthorizationRequiredException,
            NamingException, IOException, ClassNotFoundException {
        Attribute attr = new BasicAttribute("Replicable", new Boolean(true));
        Attributes attrs = new BasicAttributes();
        attrs.put(attr);
        localModifyAttributes(new NusuthJNDIName(), DirContext.ADD_ATTRIBUTE, attrs);
        ClientJidepAdapter adapter = null;
        try {
            adapter = JidepConnectionFactory.getClientAdapter(host, port);
            localCreateSubcontext("internal");
            localBind("internal/" + host + ":" + port, new ComponentInfo(host, port));
            bind("internal/" + localHost + ":" + localPort,
                    new ComponentInfo(localHost, localPort));
            JidepSession session = adapter.getSession();
            if (session.getAttribute("authenticated") == null) {
                adapter.processAuthenticate();
            }
            if (session.getAttribute("jndi_client_roles") == null) {
                if (login(host, port, adapter)) {
                    replicationInProgress = true;
                    adapter.setCommand("jndioperation");
                    JNDICommand comm = new JNDICommand("", "clonejnditree", null);
                    ObjectOutputStream oos =
                            new ObjectOutputStream(adapter.getOutputStream());
                    oos.writeObject(comm);
                    adapter.endRequest();
                    adapter.parseResponse();
                    ObjectInputStream in
                            = new ObjectInputStream(adapter.getInputStream());
                    JidepDistributedJNDIContext context =
                            (JidepDistributedJNDIContext) in.readObject();
                    this.contextName = context.contextName;
                    this.bindings.putAll(context.bindings);
                    this.envProperties.putAll(context.envProperties);
                    this.attributes2names.putAll(context.attributes2names);
                    this.names2attributes.putAll(context.names2attributes);
                    Enumeration enum = this.bindings.keys();
                    while (enum.hasMoreElements()) {
                        Object key = enum.nextElement();
                        Object value = bindings.get(key);
                        if (value instanceof DistributedJNDIContext) {
                            ((DistributedJNDIContext) value).setRootContext(this);
                        }
                    }
                    replicationInProgress = false;
                } else {
                    throw new AuthorizationRequiredException("Cannot authorize at "
                            + (host + ":" + port));
                }
            } else {
                replicationInProgress = true;
                adapter.setCommand("jndioperation");
                JNDICommand comm = new JNDICommand("", "clonejnditree", null);
                ObjectOutputStream oos =
                        new ObjectOutputStream(adapter.getOutputStream());
                oos.writeObject(comm);
                adapter.endRequest();
                adapter.parseResponse();
                ObjectInputStream in
                        = new ObjectInputStream(adapter.getInputStream());
                JidepDistributedJNDIContext context =
                        (JidepDistributedJNDIContext) in.readObject();
                this.contextName = context.contextName;
                this.bindings.putAll(context.bindings);
                this.envProperties.putAll(context.envProperties);
                this.attributes2names.putAll(context.attributes2names);
                this.names2attributes.putAll(context.names2attributes);
                Enumeration enum = this.bindings.keys();
                while (enum.hasMoreElements()) {
                    Object key = enum.nextElement();
                    Object value = bindings.get(key);
                    if (value instanceof DistributedJNDIContext) {
                        ((DistributedJNDIContext) value).setRootContext(this);
                    }
                }
                replicationInProgress = false;
            }
        } finally {
            if (adapter != null) {
                JidepConnectionFactory.returnClientAdapter(host, port, adapter);
            }
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    /**
     * Constructor for creating subContext.
     * @param name name of subContext.
     */
    private JidepDistributedJNDIContext(Name name) throws NamingException {
        this.contextName = name;
    }

    /**
     * Constructor for creating subContext.
     * @param name name of subContext.
     */
    private JidepDistributedJNDIContext(String name) throws NamingException {
        this(new NusuthJNDIName(name));
    }

    /**
     * Constructor for creating root context of DistributedJNDIContext.
     * @throws NamingException Throws if any error occures while creating internal
     * contexts and putting attributes.
     */
    public JidepDistributedJNDIContext() throws NamingException {
        this.contextName = new NusuthJNDIName();
        Attribute attr = new BasicAttribute("Replicable", new Boolean(true));
        Attributes attrs = new BasicAttributes();
        attrs.put(attr);
        localModifyAttributes(new NusuthJNDIName(), DirContext.ADD_ATTRIBUTE, attrs);
        localCreateSubcontext("internal");
        localBind("internal/" + localHost + ":" + localPort,
                new ComponentInfo(localHost, localPort));
    }

    /**
     * This method is used for login to remote node.
     * @param host Remote host.
     * @param port Remote port.
     * @adapter JidepProtocolAdapter for transport userName and password to remote
     * node.
     * @throws SocketException Throws if can't create connection with remote node.
     */
    protected boolean login(String host, int port, ClientJidepAdapter adapter)
            throws SocketException {
        try {
            adapter.setCommand("login");
            ObjectOutputStream oos
                    = new ObjectOutputStream(adapter.getOutputStream());
            oos.writeObject(userName);
            oos.writeObject(password);
            adapter.endRequest();
            adapter.parseResponse();
            if (adapter.getResponseCode() == 200) {
                adapter.getSession().setAttribute("jndi_client_roles", "passed");
            }
            return (adapter.getResponseCode() == 200);
        } catch (SocketException e) {
            throw e;
        } catch (IOException e) {
            cat.error("Can't login to the " + host + ":" + port, e);
        }
        return false;
    }

    /**
     * This method used for redirecting requests from current node to remote.
     * @param host Remote host.
     * @param port Remote port/
     * @comm JNDICommand to transfer.
     * @throws AuthorizationRequiredException Throws if authorization required.
     */
    protected Object redirectRequest(String host,
                                     String port, JNDICommand comm)
            throws AuthorizationRequiredException {
        ClientJidepAdapter adapter = null;
        try {
            adapter = JidepConnectionFactory.getClientAdapter(host,
                    Integer.parseInt(port));
            JidepSession session = adapter.getSession();
            if (session.getAttribute("authenticated") == null) {
                adapter.processAuthenticate();
            }
            if (session.getAttribute("jndi_client_roles") == null) {
                if (login(host, Integer.parseInt(port), adapter)) {
                    adapter.setCommand("jndioperation");
                    ObjectOutputStream oos =
                            new ObjectOutputStream(adapter.getOutputStream());
                    oos.writeObject(comm);
                    adapter.endRequest();
                    adapter.parseResponse();
                    ObjectInputStream is =
                            new ObjectInputStream(adapter.getInputStream());
                    Object result = is.readObject();
                    if (result instanceof DistributedJNDIContext) {
                        ((DistributedJNDIContext) result).setRootContext(root);
                    } else if (result instanceof NamingEnumeration) {
                        NamingEnumeration enum = (NamingEnumeration) result;
                        while (enum.hasMore()) {
                            Object obj = enum.next();
                            if (obj instanceof DistributedJNDIContext) {
                                ((DistributedJNDIContext) obj).setRootContext(root);
                            }
                        }
                    }
                    return result;
                } else {
                    throw new AuthorizationRequiredException("Cannot authorize at "
                            + (host + ":" + port));
                }
            } else {
                adapter.setCommand("jndioperation");
                ObjectOutputStream oos =
                        new ObjectOutputStream(adapter.getOutputStream());
                oos.writeObject(comm);
                adapter.endRequest();
                adapter.parseResponse();
                InputStream ais = adapter.getInputStream();
                ObjectInputStream is = new ObjectInputStream(ais);
                Object result = is.readObject();
                if (result instanceof DistributedJNDIContext) {
                    ((DistributedJNDIContext) result).setRootContext(root);
                } else if (result instanceof NamingEnumeration) {
                    NamingEnumeration enum = (NamingEnumeration) result;
                    while (enum.hasMore()) {
                        Object obj = enum.next();
                        if (obj instanceof DistributedJNDIContext) {
                            ((DistributedJNDIContext) obj).setRootContext(root);
                        }
                    }
                }
                return result;
            }
        } catch (Exception e) {
            cat.error("Cannot redirect request to the " + host + ":" + port, e);
        } finally {
            if (adapter != null) {
                JidepConnectionFactory.returnClientAdapter(host,
                        Integer.parseInt(port),
                        adapter);
            }
        }
        return null;
    }

    /**
     * This method used for process context changings from current node to remote.
     * @param info ComponentInfo about remote node.
     * @comm JNDICommand to transfer.
     * @throws AuthorizationRequiredException Throws if authorization required.
     */
    protected void processRequest(ComponentInfo info, JNDICommand comm)
            throws AuthorizationRequiredException {
        ClientJidepAdapter adapter = null;
        try {
            adapter = JidepConnectionFactory.getClientAdapter(info.getHost(),
                    info.getAdminPort());
            JidepSession session = adapter.getSession();
            if (session.getAttribute("authenticated") == null) {
                adapter.processAuthenticate();
            }
            if (session.getAttribute("jndi_client_roles") == null) {
                if (login(info.getHost(), info.getAdminPort(), adapter)) {
                    adapter.setCommand("jndioperation");
                    ObjectOutputStream oos =
                            new ObjectOutputStream(adapter.getOutputStream());
                    oos.writeObject(comm);
                    adapter.endRequest();
                    adapter.parseResponse();
                } else {
                    throw new AuthorizationRequiredException("Cannot authorize at "
                            + (info.getHost() + ":"
                            + info.getAdminPort()));
                }
            } else {
                adapter.setCommand("jndioperation");
                ObjectOutputStream oos =
                        new ObjectOutputStream(adapter.getOutputStream());
                oos.writeObject(comm);
                adapter.endRequest();
                adapter.parseResponse();
            }
        } catch (IOException e) {
            cat.error("Cannot replicate changes to the "
                    + info.getHost() + ":" + info.getAdminPort(), e);
        } finally {
            if (adapter != null) {
                JidepConnectionFactory.returnClientAdapter(info.getHost(),
                        info.getAdminPort(),
                        adapter);
            }
        }
    }

    /**
     * This method creates subContext of current context and sends this changes to
     * all other nodes.
     * @param name Name of created subContext.
     * @param attrs Attributes of createdSubcontext.
     * @throws NamingException Throws if any error occurs while creating
     * subContext.
     */
    public DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException {
        String nameFromRoot = contextName.toString()
                + (contextName.isEmpty() ? "" : "/")
                + name.toString();
        DistributedJNDIContext context
                = new JidepDistributedJNDIContext(nameFromRoot);
        localBind(name, context);
        if (attrs.get("Replicable") == null) {
            attrs.put("Replicable", new Boolean(true));
        } else {
            Attribute attr = attrs.get("Replicable");
            if (!((Boolean) attr.get()).booleanValue()) {
                attrs.put("Node", localHost + ":" + localPort);
            }
        }
        localModifyAttributes(name, ADD_ATTRIBUTE, attrs);
        context.setRootContext(root);
        processReplication(nameFromRoot, "create", null);
        return context;
    }

    /**
     * This method creates subContext of current context.
     * @param name Name of created subContext.
     * @param attrs Attributes of createdSubcontext.
     * @throws NamingException Throws if any error occurs while creating
     * subContext.
     */
    public DirContext localCreateSubcontext(Name name, Attributes attrs)
            throws NamingException {
        DistributedJNDIContext context
                = new JidepDistributedJNDIContext(contextName.toString()
                + (contextName.isEmpty() ? "" : "/")
                + name.toString());
        localBind(name, context);
        if (attrs.get("Replicable") == null) {
            attrs.put("Replicable", new Boolean(true));
        } else {
            Attribute attr = attrs.get("Replicable");
            if (!((Boolean) attr.get()).booleanValue()) {
                if (attrs.get("Node") == null) {
                    attrs.put("Node", localHost + ":" + localPort);
                }
            }
        }
        modifyAttributes(name, ADD_ATTRIBUTE, attrs);
        context.setRootContext(root);
        return context;
    }

    /**
     * This method is used for subscribing on context changes.
     * @param subscriptions Array of context names to which subscribe.
     * @param listenr Listener that will listen for changes.
     */
    public void subscribe(String[] subscriptions,
                          DistributedJNDIContextListener listener) {
        try {
            if (subscriptions == null || listener == null) {
                throw new IllegalArgumentException();
            }
            LocalChangedListener list = new LocalChangedListener(listener);
            boolean needRemote = false;
            for (int i = 0; i < subscriptions.length; i++) {
                if (isReplicable(subscriptions[i])) {
                    if (contextName2listeners.get(subscriptions[i]) != null) {
                        List l = (List) contextName2listeners.get(subscriptions[i]);
                        l.add(list);
                    } else {
                        List l = new LinkedList();
                        l.add(list);
                        contextName2listeners.put(subscriptions[i], l);
                    }
                } else {
                    needRemote = true;
                }
            }
            if (needRemote) {
                for (int i = 0; i < subscriptions.length; i++) {
                    if (!isReplicable(subscriptions[i])) {
                        Attributes attrs = root.getAttributes(subscriptions[i]);
                        Attribute att = attrs.get("Node");
                        if (att != null) {
                            String value = (String) att.get();
                            String host = value.substring(0, value.indexOf(":"));
                            int port
                                    = Integer.parseInt(value.substring(value.indexOf(":") + 1));
                            if (!host.equals(localHost) || port != localPort) {
                                if (notificationHandler == null) {
                                    ClientJidepAdapter adapter
                                            = JidepConnectionFactory.getClientAdapter(host, port);
                                    JidepSession session = adapter.getSession();
                                    if (session.getAttribute("authenticated") == null) {
                                        adapter.processAuthenticate();
                                    }
                                    if (session.getAttribute("jndi_client_roles") == null) {
                                        login(host, port, adapter);
                                    }
                                    if (session.getAttribute("jndi_client_roles") != null) {
                                        JidepSubscription subs
                                                = new JidepSubscription(
                                                        new String[]{subscriptions[i]});
                                        ObjectOutputStream oos
                                                = new ObjectOutputStream(adapter.getOutputStream());
                                        adapter.setCommand("subscribe");
                                        oos.writeObject(new Integer(0));
                                        oos.writeObject(subs);
                                        adapter.endRequest();
                                        adapter.parseResponse();
                                        if (adapter.getResponseCode() == 200) {
                                            notificationHandler
                                                    = new JidepNotificationHandler(adapter);
                                            notificationHandler.addListener(subscriptions[i],
                                                    listener);
                                            notificationHandler.start();
                                        }
                                    }
                                } else {
                                    notificationHandler.sendSubscription(subscriptions[i],
                                            listener);
                                    notificationHandler.addListener(subscriptions[i], listener);
                                }
                            } else {
                                if (contextName2listeners.containsKey(subscriptions[i])) {
                                    List l = (List) contextName2listeners.get(subscriptions[i]);
                                    l.add(list);
                                } else {
                                    List l = new LinkedList();
                                    l.add(list);
                                    contextName2listeners.put(subscriptions[i], l);
                                }
                            }
                        }
                    }
                }
            }
        } catch (NamingException e) {
            cat.error("Cannot subscribe", e);
        } catch (IOException e) {
            cat.error("Cannot subscribe", e);
        }
    }

/*
  public void unsubscribe(String[] subscriptions) {
    try {
      if (subscriptions == null) {
        throw new IllegalArgumentException("Parameter \"subscriptions\" "
                                           +"must be not null");
      }
      boolean needRemote = false;
      for (int i=0; i<subscriptions.length; i++) {
        if (isReplicable(subscriptions[i])) {
          if (contextName2listeners.get(subscriptions[i]) != null) {
            contextName2listeners.remove(subscriptions[i]);
          }
        } else {
          needRemote = true;
        }
      }
      if (needRemote) {
        for (int i=0; i<subscriptions.length; i++) {
          if (!isReplicable(subscriptions[i])) {
            Attributes attrs = root.getAttributes(subscriptions[i]);
            Attribute att = attrs.get("Node");
            if (att != null) {
              String value = (String)att.get();
              String host = value.substring(0, value.indexOf(":"));
              int port
                      = Integer.parseInt(value.substring(value.indexOf(":")+1));
              if (!host.equals(localHost) || port != localPort) {
                if (notificationHandler == null) {
                  ClientJidepAdapter adapter
                          = JidepConnectionFactory.getClientAdapter(host, port);
                  JidepSession session = adapter.getSession();
                  if (session.getAttribute("authenticated") == null) {
                    adapter.processAuthenticate();
                  }
                  if (session.getAttribute("jndi_client_roles") == null) {
                    login(host, port, adapter);
                  }
                  if (session.getAttribute("jndi_client_roles") != null) {
                    JidepSubscription subs
                            = new JidepSubscription(
                                    new String[] {subscriptions[i]});
                    ObjectOutputStream oos
                            = new ObjectOutputStream(adapter.getOutputStream());
                    adapter.setCommand("unsubscribe");
                    oos.writeObject(new Integer(0));
                    oos.writeObject(subs);
                    adapter.endRequest();
                    adapter.parseResponse();
                  }
                } else {
                  notificationHandler.sendUnsubscription(subscriptions[i]);
                }
              } else {
                if (contextName2listeners.containsKey(subscriptions[i])) {
                   contextName2listeners.remove(subscriptions[i]);
                }
              }
            }
          }
        }
      }
    } catch (IOException e) {
      cat.error("Cannot unsubscribe", e);
    } catch (NamingException e) {
      cat.error("Cannot unsubscribe", e);
    }
  }
*/
}
