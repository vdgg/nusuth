package com.azoft.nusuth.distributor.connectionfactory;

import com.azoft.nusuth.management.*;
import com.azoft.nusuth.util.*;

import java.net.*;
import java.util.*;
import javax.naming.directory.*;

/**
 * Insert the type's description here. Creation date: (15.11.00 11:41:08)
 * @author: Administrator
 */
public class RandomFactory extends ContainerConnectionFactory {

    protected Random random;


    /**
     * Insert the method's description here.
     * Creation date: (14.12.00 22:18:33)
     */
    public RandomFactory() throws ManagementException {

        super();

        random = new Random(System.currentTimeMillis());
    }


    /**
     * Insert the method's description here. Creation date: (17.11.00 2:08:28)
     * @return int
     * @param containers int[]
     */
    protected int balanceLoading(int[] containers) {
        synchronized (random) {
            return containers[random.nextInt(containers.length)];
        }
    }

}