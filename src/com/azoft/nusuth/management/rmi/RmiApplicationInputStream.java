package com.azoft.nusuth.management.rmi;

/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.azoft.nusuth.management.ApplicationInputStream;

public interface RmiApplicationInputStream extends ApplicationInputStream, Remote {


    /**
     * Insert the method's description here.
     * Creation date: (17.01.01 3:43:50)
     * @return int
     */
    int available() throws RemoteException;


    void close() throws RemoteException;


    boolean isEof() throws RemoteException;


    byte[] read(int length) throws RemoteException;
}