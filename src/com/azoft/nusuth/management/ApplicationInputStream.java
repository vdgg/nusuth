package com.azoft.nusuth.management;

import java.io.IOException;

public interface ApplicationInputStream {


    /**
     * Insert the method's description here.
     * Creation date: (17.01.01 3:46:19)
     * @return int
     */
    int available() throws IOException;


    void close() throws IOException;


    boolean isEof() throws IOException;


    /**
     * Insert the method's description here.
     * Creation date: (08.01.01 22:47:30)
     * @return byte[]
     * @param length int
     */
    byte[] read(int length) throws IOException;
}