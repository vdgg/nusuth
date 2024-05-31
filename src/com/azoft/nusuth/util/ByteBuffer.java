/*
 * Created by IntelliJ IDEA.
 * To change template for new class use
 * "Source Code" options (Tools | IDE Options), Templates tab.
 */
package com.azoft.nusuth.util;

import java.io.UnsupportedEncodingException;

public class ByteBuffer {

    public final static int INITIAL_CAPACITY = 16;
    byte[] buffer;
    private int initialCapacity;
    int curPos = 0;

    public ByteBuffer() {
        this(INITIAL_CAPACITY);
    }

    public ByteBuffer(int capacity) {
        buffer = new byte[capacity];
        initialCapacity = capacity;
    }

    public void clear() {
        curPos = 0;
        if (buffer.length != initialCapacity) {
            buffer = new byte[initialCapacity];
        }
    }

    public void append(byte c) {
        if (curPos >= buffer.length) {
            expandCapacity();
        }
        buffer[curPos++] = c;
    }

    public void append(byte[] c) {
        append(c, 0, c.length);
    }

    public void append(byte[] c, int off, int len) {
        while (curPos + len > buffer.length) {
            expandCapacity();
        }
        System.arraycopy(c, off, buffer, curPos, len);
        curPos += len;
    }

    private void expandCapacity() {
        byte[] tmp = new byte[buffer.length * 2];
        System.arraycopy(buffer, 0, tmp, 0, curPos);
        buffer = tmp;
    }

    public String toString(String encoding) throws UnsupportedEncodingException {
        return new String(buffer, 0, curPos, encoding);
    }

    public String toString() {
        return new String(buffer, 0, curPos);
    }

    public byte[] toByteArray() {
        byte[] result = new byte[length()];
        System.arraycopy(buffer, 0, result, 0, result.length);
        return result;
    }

    public int length() {
        return curPos;
    }

    public boolean containsByte(byte b) {
        for (int i = 0; i < curPos; i++) {
            if (buffer[i] == b)
                return true;
        }
        return false;
    }

}
