package com.azoft.nusuth.util;

import com.azoft.nusuth.core.NusuthIOException;

import java.io.*;

/**
 * This class represents buffered output stream.
 * @author vdgg, skilz
 * @since Nusuth1.0
 * @version 1.7
 */
public class BufferedServletOutputStream extends OutputStream {

    private final static int DEFAULT_BUFFER_SIZE = 8192;
    private OutputStream stream;
    private byte[] buffer;
    private int curPos;
//  private int transfered = 0;
    private boolean closed;
    private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("com.azoft.nusuth.core");
    private LogCategoryProxy catProxy = LogCategoryProxy.getInstance("com.azoft.nusuth.core");

    public BufferedServletOutputStream() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public BufferedServletOutputStream(int bufferSize) {
//    transfered = 0;
        buffer = new byte[bufferSize];
        curPos = 0;
        closed = true;
    }

    public void init(OutputStream stream) {
        this.stream = stream;
        closed = false;
//    transfered = 0;
    }

    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
        if (curPos == buffer.length) {
            flush();
        }
        buffer[curPos++] = (byte) b;
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
/*    if (closed) {
      throw new IOException("Stream closed");
    }
    if (len < 0) {
      throw new IllegalArgumentException("Negative length");
    }*/
        if (curPos >= buffer.length) {
            flush();
        }
        if (len < buffer.length - curPos) {
            System.arraycopy(b, off, buffer, curPos, len);
            curPos += len;
        } else {
            int tow = buffer.length - curPos;
            System.arraycopy(b, off, buffer, curPos, tow);
            off += tow;
            len -= tow;
            curPos = buffer.length;
            flush();
            write(b, off, len);
        }
    }

    public void close() throws IOException {
        stream.close();
        closed = true;
    }

    /**
     * Flush the content of the stream to inner stream.
     * @throws IOException Throws if any error occures while flushing.
     */
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
        try {
            if (curPos > 0) {
                stream.write(buffer, 0, curPos);
                if (catProxy.isDebugEnabled()) {
                    cat.debug(new String(buffer, 0, curPos));
                }
            }
            stream.flush();
        } catch (IOException e) {
            throw new NusuthIOException("Broken pipe");
        }
        curPos = 0;
    }

    public void cleanup() {
        stream = null;
        curPos = 0;
        closed = true;
    }

//  public int getNumberOfBytesTransfered() {
//    return transfered;
//  }

}