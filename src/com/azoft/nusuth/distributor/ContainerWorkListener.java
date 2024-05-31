/*
 * Created by IntelliJ IDEA.
 * User: skilz
 * Date: Aug 8, 2001
 * Time: 11:21:04 AM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package com.azoft.nusuth.distributor;

import com.azoft.nusuth.distributor.connectionfactory.ContainerConnectionFactory;
import com.azoft.nusuth.core.RemoteContainer;

import java.util.LinkedList;
import java.util.List;
import java.net.Socket;
import java.io.IOException;

public class ContainerWorkListener extends Thread {

    private ContainerConnectionFactory factory;
    private List dead = new LinkedList();

    public ContainerWorkListener(ContainerConnectionFactory factory) {
        this.factory = factory;
        this.factory.setContainerWorkListener(this);
    }

    public void addDeadContainer(RemoteContainer addr) {
        synchronized (dead) {
            dead.add(addr);
        }
    }

    public void run() {
        RemoteContainer cont = null;
        while (true) {
            try {
                sleep(30000);
            } catch (InterruptedException e) {
            }
            for (int i = 0; i < dead.size(); i++) {
                cont = (RemoteContainer) dead.get(i);
                try {
                    Socket socket = new Socket(cont.getHost(), cont.getPort());
                    factory.onContainerUp(cont);
                    synchronized (dead) {
                        dead.remove(cont);
                    }
                } catch (IOException e) {
                }
            }
        }
    }
}
