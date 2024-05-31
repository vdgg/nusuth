package com.azoft.nusuth.distributor.connectionfactory;

import java.net.Socket;
import java.util.*;

import com.azoft.nusuth.management.ContainerInfo;
import com.azoft.nusuth.management.ManagementException;
import com.azoft.nusuth.util.StrBuffer;

public class TestFactory extends ContainerConnectionFactory {
    private Random random = new Random(System.currentTimeMillis());
    private Hashtable container2load = new Hashtable();
    public int[] containerNumber2loadFactor = new int[0];


    public TestFactory() throws ManagementException {
    }

    protected int balanceLoading(int[] containers) {
        int[] currentLoad = new int[containers.length];
        for (int i = 0; i < containers.length; i++) {
            currentLoad[i] = containerNumber2socketsStack[containers[i]].isEmpty() ? 0 : ((Integer) container2load.get(containerNumber2containerID[containers[i]])).intValue();
        }
        int commonLoadFactor = 0;
        int[] currentLoadFactor = new int[containers.length];
        for (int i = 0; i < containers.length; i++) {
            currentLoadFactor[i] = containerNumber2loadFactor[containers[i]];

            for (int j = 0; j < containers.length; j++) {
                if (i != j) {
                    currentLoadFactor[i] *= (currentLoad[j] + 1);
                }
            }
            commonLoadFactor += currentLoadFactor[i];
        }
        if (commonLoadFactor <= 0) {
            return containers[random.nextInt(containers.length)];
        }
        int rint = random.nextInt(commonLoadFactor);
        int i = containers.length;
        while (rint >= 0) {
            rint -= currentLoadFactor[--i];
        }

        int result = containers[i];
        Object resultId = containerNumber2containerID[result];
        Integer oldLoad = (Integer) container2load.get(resultId);
        Integer newLoad = new Integer(oldLoad.intValue() + 1);
        container2load.put(resultId, newLoad);

        return result;
    }


    public void returnSocket(StrBuffer container, Socket socket) {
        super.returnSocket(container, socket);
        Integer currentLoad = (Integer) container2load.get(container);
        if (currentLoad != null) {
            container2load.put(container, new Integer(currentLoad.intValue() - 1));
        } else {
            logger.error("Attempt to return socket to unexisting container " + container);
        }
    }

    public void addContainer(String name, ContainerAddress newContainer) {
        super.addContainer(name, newContainer);
        int[] newFactors = new int[containerNumber2containerID.length];
        System.arraycopy(containerNumber2loadFactor, 0, newFactors, 0, containerNumber2loadFactor.length);
        String newFactorString = (String) parameters.get(name);
        if (newFactorString != null) {
            try {
                newFactors[containerNumber2containerID.length - 1] = Integer.parseInt(newFactorString);
            } catch (NumberFormatException nfex) {
                newFactors[containerNumber2containerID.length - 1] = 1;
                if (logProxy.isInfoEnabled())
                    logger.info("Wrong load factor for container \"" + name + "\", nested: " + nfex.getMessage());
            }
        } else {
            newFactors[containerNumber2containerID.length - 1] = 1;
            if (logProxy.isInfoEnabled())
                logger.info("Unknown load factor for container \"" + name + "\", setted to \"1\"");
        }
        containerNumber2loadFactor = newFactors;
    }

    protected void removeContainer(String name) {
        StrBuffer id = new StrBuffer(name.length());
        id.append(name);
        int number = ((Integer) containerID2number.get(id)).intValue();
        super.removeContainer(name);
        int[] newFactors = new int[containerNumber2containerID.length];
        System.arraycopy(containerNumber2loadFactor, 0, newFactors, 0, number);
        System.arraycopy(containerNumber2loadFactor, number + 1, newFactors, number, containerNumber2containerID.length - number);
    }
}
