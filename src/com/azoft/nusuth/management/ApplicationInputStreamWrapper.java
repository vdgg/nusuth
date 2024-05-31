package com.azoft.nusuth.management;

import java.io.*;

import com.azoft.nusuth.management.rmi.*;

public class ApplicationInputStreamWrapper extends InputStream {
    private ApplicationInputStream stream = null;


    /**
     * ApplicationInputStreamImpl constructor comment.
     */
    public ApplicationInputStreamWrapper(ApplicationInputStream source) {
        super();
        stream = source;
    }


    /**
     * Insert the method's description here.
     * Creation date: (17.01.01 13:49:16)
     * @return int
     * @exception java.io.IOException The exception description.
     */
    public int available() throws java.io.IOException {
        return stream.available();
    }


    /**
     * read method comment.
     */
    public int read() throws IOException {
        byte[] buff = stream.read(1);
        return buff.length > 0 ? buff[0] : -1;
    }


    /**
     * Insert the method's description here.
     * Creation date: (17.01.01 13:50:45)
     * @return int
     * @param b byte[]
     * @param off int
     * @param len int
     * @exception java.io.IOException The exception description.
     */
    public int read(byte[] b) throws java.io.IOException {
        return read(b, 0, b.length);
    }


    /**
     * Insert the method's description here.
     * Creation date: (17.01.01 13:50:45)
     * @return int
     * @param b byte[]
     * @param off int
     * @param len int
     * @exception java.io.IOException The exception description.
     */
    public int read(byte[] b, int off, int len) throws java.io.IOException {
        byte[] buf = stream.read(len);
        System.arraycopy(buf, 0, b, off, buf.length);
        return buf.length == 0 ? -1 : buf.length;
    }


    /**
     * read method comment.
     */
    public byte[] read(int length) throws IOException {
        return stream.read(length);
    }
}