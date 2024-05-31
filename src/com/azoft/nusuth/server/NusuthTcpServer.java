package com.azoft.nusuth.server;

import com.azoft.nusuth.deployment.SimpleNusuthWebAppElement;

import java.util.*;

import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.InetAddress;

import com.azoft.nusuth.management.Manageable;

/**
 * This class represent TCP Server.
 * @author vdgg, skilz, igork
 * @since Nusuth1.0
 * @version 1.16
 */
public class NusuthTcpServer implements Manageable {

    protected String serverName = "TCP";
    private Socket[] sockQueue;
    private int queueHead;
    private int queueTail;
    private int nextQueueTail;
    private boolean queueFull = false;
    private int maxSockets;
    private int minHandlers;
    private int maxHandlers;
    private int nowHandlers;
    private int freeHandlers;
    private int permanentHandlers;
    private long threadTimeout;
    private Object lockHandlers = new Object();
    private int maxKeepalive;
    private int nowKeepalive;
    private Object lockKeepalive = new Object();
    protected int port;
    private int so_timeout;
    private Class connHandlerClass;
    private ThreadGroup handlersGroup;
    private long threadCounter;
//  protected ServerSocket     serverSocket;
    private boolean running = false;
    private boolean reducingSockets = false;
    protected org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.server");
    private java.lang.Object lockActive = new Object();
    private int nowActive = 0;

    // allowed number of sockets handlers can get from queue without wait
    // Using for normalizing creation/death of handlers
    private int woWait = 0;

    private List allowHosts = new ArrayList();
    private List denyHosts = new ArrayList();
    protected List allHosts = new ArrayList();
    protected List startedHandlers = new ArrayList();
    protected Hashtable ip2listOfHosts = new Hashtable();
    protected Hashtable ip2handler = new Hashtable();
    protected boolean defaultHostExist = false;

    /**
     * Constructor.
     * @patam setings Server settings.
     * @param connHandlerClass Connection handler class.
     */
    public NusuthTcpServer(CompositeNusuthWebAppElement settings,
                           Class connHandlerClass) throws DeploymentException {
        super();
//    setName("JBird_"+serverName+"_Server");
        this.connHandlerClass = connHandlerClass;

        int intValue;
        long longValue;
        List ipFilter;
        if ((intValue = retrieveSimpleInt(settings, "max-handlers"))
                != maxHandlers) {
            maxHandlers = intValue;
        }
        if ((intValue = retrieveSimpleInt(settings, "port")) != maxHandlers) {
            port = intValue;
        }
        if ((intValue = retrieveSimpleInt(settings, "min-handlers"))
                != minHandlers) {
            minHandlers = intValue;
        }
        if ((intValue = retrieveSimpleInt(settings, "max-keepalives"))
                != maxKeepalive) {
            maxKeepalive = intValue;
        }
        if ((intValue = retrieveSimpleInt(settings, "queue"))
                != maxSockets - 1) {
            maxSockets = intValue + 1;
        }
        if ((longValue = retrieveTime(settings, "handler-timeout"))
                != threadTimeout) {
            threadTimeout = longValue;
        }
        if ((intValue = (int) retrieveTime(settings, "so-timeout"))
                != getSoTimeout()) {
            so_timeout = intValue;
        }
        if (!checkIpChanges((ipFilter = convertIpFilter(settings, "allow-hosts")),
                allowHosts)) {
            synchronized (allowHosts) {
                this.allowHosts = ipFilter;
            }
        }
        if (!checkIpChanges((ipFilter = convertIpFilter(settings, "deny-hosts")),
                denyHosts)) {
            synchronized (allowHosts) {
                this.denyHosts = ipFilter;
            }
        }
        sockQueue = new Socket[this.maxSockets];
        handlersGroup = new ThreadGroup(serverName + "_Handlers");
        queueHead = 0;
        queueTail = 0;
        nextQueueTail = 1;
        boolean startHandler = false;
        permanentHandlers = 0;
        queueFull = false;
        for (int i = 0; i < minHandlers; i++) {
            startConnectionHandler();
        }
        running = true;
    }

    public final void setConnectionHandlerClass(Class connHandlerClass) {
        this.connHandlerClass = connHandlerClass;
    }

    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 17:29:03)
     */
    void decActive() {

        synchronized (lockActive) {
            nowActive--;
        }
    }


    void freeKeepAlive() {

        synchronized (lockKeepalive) {
            nowKeepalive--;
        }

        //    Logger.log("Now keep alives: "+nowKeepalive, 0);
    }


    public int getMaxHandlers() {
        return maxHandlers;
    }


    public int getMaxKeepAlive() {
        return maxKeepalive;
    }


    public int getMaxSockets() {
        return maxSockets - 1;
    }


    public int getMinHandlers() {
        return minHandlers;
    }


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 17:28:11)
     * @return int
     */
    public int getNowActive() {
        return nowActive;
    }


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 17:22:21)
     * @return int
     */
    public int getNowHandlers() {
        return nowHandlers;
    }


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 17:25:26)
     * @return int
     */
    public int getNowKeepalive() {
        return nowKeepalive;
    }


    Socket getSocket() {
        Socket socket = null;

        while (true) {
            synchronized (sockQueue) {
                if (woWait > 0) {
                    socket = sockQueue[queueHead];
                    sockQueue[queueHead] = null;
                    queueHead = (queueHead + 1) % maxSockets;
                    woWait--;

                    if (queueFull) {
                        sockQueue.notifyAll();
                    }

                    break;
                }

                freeHandlers++;

                if (permanentHandlers >= minHandlers) {
                    try {
                        sockQueue.wait(threadTimeout);
                    } catch (Throwable t) {
                    }

                    if (queueHead == queueTail) {
                        freeHandlers--;
                        break;
                    }
                } else {
                    permanentHandlers++;

                    try {
                        sockQueue.wait();
                    } catch (Throwable t) {
                    }

                    permanentHandlers--;
                }

                if (queueHead != queueTail) {
                    socket = sockQueue[queueHead];
                    sockQueue[queueHead] = null;
                    queueHead = (queueHead + 1) % maxSockets;
                    break;
                }
            }
        }

        return socket;
    }

    public final int getSoTimeout() {
        return so_timeout;
    }


    /**
     * Insert the method's description here.
     * Creation date: (16.01.01 18:16:14)
     * @return long
     */
    public long getThreadTimeout() {
        return threadTimeout;
    }


    /**
     * Insert the method's description here.
     * Creation date: (11.01.01 17:29:03)
     */
    void incActive() {

        synchronized (lockActive) {
            nowActive++;
        }
    }


    /**
     * Return true if server is still running.
     * @return true if server is still running.
     */
    public boolean isRunning() {
        return running;
    }


    boolean requestKeepAlive() {

        boolean result = false;

        synchronized (lockKeepalive) {
            if (nowKeepalive < maxKeepalive) {
                result = true;

                nowKeepalive++;
            }
        }

        //    Logger.log("Now keep alives: "+nowKeepalive, 0);
        return result;
    }

    /**
     * This method put given socket to socket queue.
     * @param socket Socket to put.
     */
    protected void addSocketToQueue(Socket socket) {
        boolean startHandler = false;
        synchronized (sockQueue) {
            if ((queueHead == nextQueueTail) || reducingSockets) {
                queueFull = true;
                try {
                    sockQueue.wait();
                } catch (Exception ex) {
                }
            }
            queueFull = false;
            sockQueue[queueTail] = socket;
            queueTail = nextQueueTail;
            nextQueueTail = (nextQueueTail + 1) % maxSockets;
            startHandler = ((freeHandlers == 0) && (nowHandlers < maxHandlers));
            if (freeHandlers > 0) {
                sockQueue.notify();
                freeHandlers--;
            } else {
                woWait++;
            }
        }
        if (startHandler) {
            startConnectionHandler();
        }
    }

    public void setMaxHandlers(int maxHandlers) {

        synchronized (lockHandlers) {
            this.maxHandlers = maxHandlers;
        }

        synchronized (sockQueue) {
            if (queueFull) {

                //        System.out.println("Notify 2");
                sockQueue.notifyAll();
            }
        }
    }


    public void setMaxKeepAlive(int maxKeepalive) {

        synchronized (lockKeepalive) {
            this.maxKeepalive = maxKeepalive;
        }
    }


    public void setMaxSockets(int maxSockets) {

        synchronized (sockQueue) {
            int numSockets
                    = (queueTail < queueHead)
                    ? this.maxSockets - queueHead + queueTail + 1
                    : queueTail - queueHead;

            while (numSockets > maxSockets) {
                reducingSockets = true;

                try {
                    wait();
                } catch (Exception ex) {
                }

                numSockets
                        = (queueTail < queueHead)
                        ? this.maxSockets - queueHead + queueTail + 1
                        : queueTail - queueHead;
            }

            reducingSockets = false;

            Socket[] nextSockQueue = new Socket[maxSockets + 1];

            for (int i = 0; i < numSockets; i++) {
                nextSockQueue[i] = sockQueue[(queueHead + i) % this.maxSockets];
            }

            queueHead = 0;
            queueTail = numSockets;
            nextQueueTail = (queueTail + 1) % (maxSockets + 1);
            this.maxSockets = maxSockets + 1;
        }
    }


    public void setMinHandlers(int minHandlers) {

        synchronized (lockHandlers) {
            this.minHandlers = minHandlers;
            if (minHandlers > nowHandlers) {
                for (int i = nowHandlers; i < minHandlers; i++) {
                    startConnectionHandler();
                }
            }
        }
    }


    /**
     * Insert the method's description here.
     * Creation date: (16.01.01 18:16:14)
     * @param newThreadTimeout long
     */
    public void setThreadTimeout(long newThreadTimeout) {

        synchronized (sockQueue) {
            threadTimeout = newThreadTimeout;
        }
    }


    protected void startConnectionHandler() {

        synchronized (lockHandlers) {
            TcpConnectionHandler handler = null;

            try {
                handler = (TcpConnectionHandler) connHandlerClass.newInstance();
            } catch (Throwable t) {

                //Logger.log("FATAL ERROR: cannot start connection handler", t, 1);
                cat.error("FATAL: cannot start connection handler", t);

                throw new RuntimeException("FATAL ERROR: cannot start "
                        + "connection handler");
            }

            handler.init(this);

            //      Logger.log("Starting handler TCP_Handler_"+threadCounter, 0);
            Thread execThread = new Thread(handlersGroup, handler,
                    serverName + "_Handler_" + threadCounter++);

            execThread.start();

            nowHandlers++;

            //      Logger.log("Handlers: "+nowHandlers, 0);
        }
    }


    public void startServer() {
        startServerSockets();
        System.out.println(serverName + " Server listening port " + port + "...");
    }


    void stopConnectionHandler() {

        synchronized (lockHandlers) {
            nowHandlers--;
        }
    }


    /**
     * This method stop server.
     */
    public void stopServer() {
        running = false;
    }

    public void applySettings(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        if (isRestartNeeded(settings)) {
            throw new IllegalArgumentException("Cannot apply settings - "
                    + "restart needed");
        }
        int intValue;
        long longValue;
        List ipFilter;
        if ((intValue = retrieveSimpleInt(settings,
                "max-handlers")) != maxHandlers) {
            setMaxHandlers(intValue);
        }
        if ((intValue = retrieveSimpleInt(settings,
                "min-handlers")) != minHandlers) {
            setMinHandlers(intValue);
        }
        if ((intValue = retrieveSimpleInt(settings,
                "max-keepalives")) != maxKeepalive) {
            setMaxKeepAlive(intValue);
        }
        if ((intValue = retrieveSimpleInt(settings, "queue")) != maxSockets - 1) {
            setMaxSockets(intValue);
        }
        if ((longValue = retrieveTime(settings,
                "handler-timeout")) != threadTimeout) {
            setThreadTimeout(longValue);
        }
        if ((intValue = (int) retrieveTime(settings,
                "so-timeout")) != getSoTimeout()) {
            this.so_timeout = intValue;
        }
        if (!(ipFilter = convertIpFilter(settings,
                "allow-hosts")).equals(allowHosts)) {
            synchronized (allowHosts) {
                this.allowHosts = ipFilter;
            }
        }
        if (!(ipFilter = convertIpFilter(settings,
                "deny-hosts")).equals(denyHosts)) {
            synchronized (denyHosts) {
                this.denyHosts = ipFilter;
            }
        }
    }


    private boolean checkIpChanges(List newIp, List oldIp) {
        if (newIp.size() != oldIp.size()) {
            return false;
        }
        Iterator iterator = newIp.iterator();
        while (iterator.hasNext()) {
            String[] nip = (String[]) iterator.next();
            boolean found = false;
            for (int i = 0; i < oldIp.size(); i++) {
                String[] oip = (String[]) oldIp.get(i);
                boolean match = true;
                for (int j = 0; j < 4; j++) {
                    if (nip[i] != oip[i]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private List convertIpFilter(CompositeNusuthWebAppElement settings,
                                 String elementName) throws DeploymentException {
        Enumeration simpleEnumeration = settings.getSimpleChild(elementName);
        if (!simpleEnumeration.hasMoreElements()) {
            return new ArrayList();
        }
        String ipFilterString
                = ((SimpleNusuthWebAppElement) simpleEnumeration.nextElement()).
                getContent();
        StringTokenizer tokenizer = new StringTokenizer(ipFilterString.trim(), ",");
        List resultList = new ArrayList();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            StringTokenizer nextTokenizer = new StringTokenizer(token, ".");
            if (nextTokenizer.countTokens() != 4) {
                throw new DeploymentException("Cannot recognize ip filter token "
                        + token);
            }
            String[] ipStringArray = new String[4];
            for (int i = 0; i < 4; i++) {
                String ipByte = nextTokenizer.nextToken();
                if (!ipByte.equals("*")) {
                    int res = -1;
                    try {
                        res = Integer.parseInt(ipByte);
                    } catch (NumberFormatException nfe) {
                        throw new DeploymentException("Cannot recognize element "
                                + ipByte + " in ip filter token "
                                + token);
                    }
                    if (res < 0 || res > 255) {
                        throw new DeploymentException("Element " + ipByte
                                + " in ip filter token " + token
                                + " is out of range [0..255]");
                    }
                }
                ipStringArray[i] = ipByte;
            }
            resultList.add(ipStringArray);
        }
        return resultList;
    }


    public boolean isRestartNeeded(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        Enumeration enum = settings.getSimpleChild("port");
        if (!enum.hasMoreElements()) {
            throw new DeploymentException("Cannot find port in tcp server settings");
        }
        String portString
                = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent();
        int newPort = 0;
        try {
            newPort = Integer.parseInt(portString);
        } catch (Exception ex) {
            throw new DeploymentException("Cannot convert port to int, nested: "
                    + ex);
        }
        return port != newPort;
    }


    private int retrieveSimpleInt(CompositeNusuthWebAppElement settings,
                                  String elementName) throws DeploymentException {
        Enumeration simpleEnumeration = settings.getSimpleChild(elementName);
        if (!simpleEnumeration.hasMoreElements()) {
            throw new DeploymentException("Cannot find " + elementName
                    + " element, wrong config");
        }
        try {
            return Integer.parseInt(((SimpleNusuthWebAppElement) simpleEnumeration.
                    nextElement()).getContent());
        } catch (Exception ex) {
            throw new DeploymentException("Cannot convert element " + elementName
                    + " to int");
        }
    }


    private long retrieveTime(CompositeNusuthWebAppElement settings,
                              String elementName) throws DeploymentException {
        Enumeration simpleEnumeration = settings.getSimpleChild(elementName);
        if (!simpleEnumeration.hasMoreElements()) {
            throw new DeploymentException("Cannot find " + elementName
                    + " element, wrong config");
        }
        String timeString
                = ((SimpleNusuthWebAppElement) simpleEnumeration.nextElement()).
                getContent();
        timeString = timeString.trim();
        char measure = timeString.charAt(timeString.length() - 1);
        String realSTString = timeString;
        int multiplier = 1000;
        switch (measure) {
            case 's':
                realSTString = timeString.substring(0, timeString.length() - 1).trim();
                break;
            case 'm':
                realSTString = timeString.substring(0, timeString.length() - 1).trim();
                multiplier = 60000;
                break;
            case 'h':
                realSTString = timeString.substring(0, timeString.length() - 1).trim();
                multiplier = 3600000;
                break;
            default :
                break;
        }
        try {
            return Long.parseLong(realSTString) * multiplier;
        } catch (Exception ex) {
            throw new DeploymentException("Cannot convert element " + elementName
                    + " to long");
        }
    }

    /**
     * Initialize all defined server sockets and start threads for each socket.
     */
    public void startServerSockets() {
        ServerSocket serverSocket = null;
        try {
            if (allHosts.contains("*") && !startedHandlers.contains("*")) {
                if (startedHandlers.size() > 0) {
                    for (int i = 0; i < startedHandlers.size(); i++) {
                        InetAddress addr = (InetAddress) startedHandlers.get(i);
                        NusuthSocketHandler handler
                                = (NusuthSocketHandler) ip2handler.get(addr.getHostAddress());
                        handler.shutdown();
                    }
                    startedHandlers.clear();
                }
                serverSocket = new ServerSocket(port);
                NusuthSocketHandler handler
                        = new NusuthSocketHandler(this, serverSocket);
                handler.start();
                startedHandlers.add("*");
                ip2handler.put("*", handler);
                return;
            } else if (!allHosts.contains("*")) {
                for (int i = 0; i < allHosts.size(); i++) {
                    String host = (String) allHosts.get(i);
                    InetAddress addr = InetAddress.getByName(host);
                    String ip = addr.getHostAddress();
                    if (startedHandlers.contains(addr)) {
                        List list = (List) ip2listOfHosts.get(ip);
                        if (!list.contains(host)) {
                            list.add(host);
                        }
                    } else {
                        serverSocket = new ServerSocket(port, 50,
                                InetAddress.getByName(host));
                        NusuthSocketHandler handler
                                = new NusuthSocketHandler(this, serverSocket);
                        ip2handler.put(ip, handler);
                        handler.start();
                        startedHandlers.add(addr);
                        List list = new ArrayList();
                        list.add(host);
                        ip2listOfHosts.put(ip, list);
                    }
                }
            }
        } catch (IOException e) {
            cat.error("Cannot start server socket", e);
            throw new RuntimeException("Server fatal error: cannot "
                    + "start server, see log for details");
        }
    }

    final boolean isAllowed(Socket socket) {
        synchronized (allowHosts) {
            if (allowHosts.size() == 0 && denyHosts.size() == 0) {
                return true;
            }
        }

        String host = socket.getInetAddress().getHostAddress();

        boolean allow = false;
        StringTokenizer ipString = new StringTokenizer(host, ".");
        String[] ip = new String[4];

        //added by IgorK for syncronization reasons
        List allowHosts, denyHosts;

        synchronized (this.allowHosts) {
            allowHosts = this.allowHosts;
            denyHosts = this.denyHosts;
        }

        for (int i = 0; i < 4; i++) {
            ip[i] = ipString.nextToken();
        }

        for (int i = 0; i < denyHosts.size(); i++) {
            if (maskEquals(ip, ((String[]) (denyHosts.get(i))))) {
                return false;
            }
        }

        if (allowHosts.size() == 0) {
            return true;
        } else {
            for (int i = 0; i < allowHosts.size(); i++) {
                if (maskEquals(ip, ((String[]) (allowHosts.get(i))))) {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean maskEquals(String[] str, String[] mask) {

        for (int i = 0; i < 4; i++) {
            if (!(str[i].equals(mask[i]) || mask[i].equals("*"))) {
                return false;
            }
        }

        return true;
    }

    public int getPort() {
        return port;
    }

    public void addVirtualHost(String hostId) {
        if (hostId.equals("*")) {
            defaultHostExist = true;
        }
        allHosts.add(hostId);
    }

    public void virtualHostRemoved(String hostId) {
        InetAddress addr = null;
        allHosts.remove(hostId);
        if (hostId.equals("*")) {
            NusuthSocketHandler handler = (NusuthSocketHandler) ip2handler.remove("*");
            handler.shutdown();
            startedHandlers.remove("*");
            startServerSockets();
            return;
        } else {
            try {
                addr = InetAddress.getByName(hostId);
            } catch (UnknownHostException e) {
                cat.warn("Cannot remove virtual host \"" + hostId + "\" from server", e);
                return;
            }
        }
        String ip = addr.getHostAddress();
        List list = (List) ip2listOfHosts.get(ip);
        if (list != null) {
            if (list.size() == 1) {
                list.clear();
                NusuthSocketHandler handler = (NusuthSocketHandler) ip2handler.remove(ip);
                handler.shutdown();
                startedHandlers.remove(addr);
            } else {
                list.remove(ip);
            }
        }
    }

    public void setName(String name) {
        this.serverName = name;
    }

}
