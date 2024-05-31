/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.management.rmi;

import com.azoft.nusuth.management.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

public interface RmiDeployer
        extends RmiComponentManager {
    void addApplication(Vector hosts) throws RemoteException;

    RmiApplicationInputStream getWebInf(String docBase, String location) throws RemoteException;

    void patchApplication(Vector hosts, boolean overwrite) throws RemoteException;

    void replaceContent(Vector hosts) throws RemoteException;
}
