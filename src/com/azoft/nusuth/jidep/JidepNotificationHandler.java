package com.azoft.nusuth.jidep;

import com.azoft.nusuth.jndi.DistributedJNDIContextListener;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;

/**
 * This class represent handler for JIDEP notifications.
 * It wait for notification and then give this notification to the
 * DistributedJNDIContextListener.
 * @author skilz.
 * @since Nusuth1.0
 * @version 1.0
 */
public class JidepNotificationHandler extends Thread {

    private Hashtable contextName2listeners = new Hashtable();
    /**Adapter for notification parsind*/
    private ClientJidepAdapter adapter = null;
    /**Logger*/
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jidep");

    private static int counter = 1;

    private Hashtable lis = new Hashtable();
    private Hashtable con = new Hashtable();

    private DistributedJNDIContextListener listener = null;

    /**
     * Constructor.
     * @param listener Listener for handling notifications.
     * @param adapter Adapter for notifications parsing.
     */
    public JidepNotificationHandler(ClientJidepAdapter adapter) {
        this.adapter = adapter;
    }

    public void addListener(String contextName, DistributedJNDIContextListener listener) {
        if (contextName2listeners.get(contextName) != null) {
            List list = (List) contextName2listeners.get(contextName);
            list.add(listener);
        } else {
            List list = new LinkedList();
            list.add(listener);
            contextName2listeners.put(contextName, list);
        }
    }

    public void sendSubscription(String contextName,
                                 DistributedJNDIContextListener listener) {
        try {
            incrementCounter(contextName, listener);
            JidepSubscription subs
                    = new JidepSubscription(new String[]{contextName});
            ObjectOutputStream oos
                    = new ObjectOutputStream(adapter.getOutputStream());
            adapter.setCommand("subscribe");
            oos.writeObject(new Integer(counter));
            oos.writeObject(subs);
            adapter.endRequest();
        } catch (IOException e) {
            decrementCounter();
            cat.error("Cannot send subscription", e);
        }
    }
/*
  public void sendUnsubscription(String contextName) {
    try {
      ObjectOutputStream oos
              = new ObjectOutputStream(adapter.getOutputStream());
      adapter.setCommand("unsubscribe");
      oos.writeObject(contextName);
      adapter.endRequest();
    } catch (IOException e) {
      cat.error("Cannot send request for unsubscribe", e);
    }
  }
*/
    /**
     * This method parse notifications and give its to listener.
     */
    public void run() {
        while (true) {
            try {
                adapter.parseResponse();
                int code = 0;
                code = adapter.getResponseCode();
                if (code == 200) {
                    ObjectInputStream ois
                            = new ObjectInputStream(adapter.getInputStream());
                    String type = (String) ois.readObject();
                    if (type.equals("notification")) {
                        JidepNotification notif = (JidepNotification) ois.readObject();
                        List listeners = (List) contextName2listeners.get(notif.contextName);
                        for (int i = 0; i < listeners.size(); i++) {
                            ((DistributedJNDIContextListener) listeners.get(i)).notify(notif);
                        }
                    } else if (type.equals("subscription")) {
                        Integer in = (Integer) ois.readObject();
                        synchronized (this) {
                            if (in.intValue() != 0) {
                                addListener((String) con.remove(in), (DistributedJNDIContextListener) lis.remove(in));
                            }
                        }
                    } else if (type.equals("unsibscribe")) {
                        String name = (String) ois.readObject();
                        contextName2listeners.remove(name);
                    }
                }
            } catch (SocketException e) {
                cat.debug("Cannot parse notification. Server closed socket");
                return;
            } catch (IOException e) {
                cat.error("Cannot parse notification", e);
            } catch (ClassNotFoundException e) {
                cat.error("Cannot parse notification", e);
            }
        }
    }

    private synchronized void incrementCounter(String name, DistributedJNDIContextListener listener) {
        counter++;
        Integer in = new Integer(counter);
        lis.put(in, listener);
        con.put(in, name);
    }

    private synchronized void decrementCounter() {
        Integer in = new Integer(counter);
        lis.remove(in);
        con.remove(in);
        counter--;
    }

}
