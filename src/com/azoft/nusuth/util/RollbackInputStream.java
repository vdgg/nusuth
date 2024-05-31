/*****************************************************************************
 *                                                                           *
 *                      (c) 2000-2001 Thruport Technologies                  *
 *                                                                           *
 *****************************************************************************/
package com.azoft.nusuth.util;

import java.io.*;

/**
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
public class RollbackInputStream extends InputStream {

    public final static int DEFAULT_QUANT = 512;
    protected InputStream stream;
    protected byte[] buffer;
    protected int quant;
    protected int startpos = 0;
    protected int endpos = 0;
    private boolean closed;


    public RollbackInputStream(InputStream stream) {
        this(stream, DEFAULT_QUANT);
    }


    public RollbackInputStream(InputStream stream, int quant) {

        this.stream = stream;
        this.quant = quant;
        buffer = new byte[quant];
    }


    private void allocateMore(int nquants) throws IOException {

        if (closed) {
            throw new EOFException();
        }

        byte[] buffnew = new byte[buffer.length + nquants * quant];

        System.arraycopy(buffer, 0, buffnew, 0, buffer.length);

        buffer = buffnew;
    }


    public int available() throws IOException {

        if (closed) {
            throw new EOFException();
        }

        return endpos - startpos + stream.available();
    }


    public void clearBuffer() {

        buffer = new byte[quant];
        startpos = 0;
        endpos = 0;
    }


    public void close() throws IOException {

        if (closed) {
            throw new EOFException();
        }

        stream.close();

        buffer = null;
        startpos = 0;
        endpos = 0;
        quant = 0;
        stream = null;
        closed = true;
    }


    /**
     * Insert the method's description here.
     * Creation date: (21.12.00 22:47:14)
     * @param istream java.io.InputStream
     */
    public void init(InputStream istream) {

        stream = istream;
        endpos = 0;
        startpos = 0;
        closed = false;
    }


    public int read() throws IOException {

        if (closed) {
            throw new EOFException();
        }

        if (startpos < endpos) {
            return ((int) buffer[startpos++]) & 0xff;
        } else {
            int bf = stream.read();

            if (bf == -1) {
                System.out.println("Shit happens! at " + Thread.currentThread().getName());
            }

            if (endpos == buffer.length - 1) {
                allocateMore(1);
            }

            buffer[endpos++] = (byte) bf;

            startpos++;

            return bf;
        }
    }


    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }


    public int read(byte[] b, int offset, int len) throws IOException {

        if (closed) {
            throw new EOFException();
        }

        if (b == null) {
            throw new NullPointerException();
        }

        if ((offset + len > b.length) || (offset < 0) || (len < 0)) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }

        int diff = endpos - startpos;

        if (diff >= len) {
            System.arraycopy(buffer, startpos, b, offset, len);

            startpos += len;

            return len;
        } else {
            if (diff > 0) {
                System.arraycopy(buffer, startpos, b, offset, diff);
            }

            int rlen = stream.read(b, offset + diff, len - diff);

            if (rlen <= 0) {
                startpos = endpos;

                return diff;
            }

            if (endpos >= buffer.length - rlen) {
                allocateMore(rlen / quant + 1);
            }

            System.arraycopy(b, offset + diff, buffer, endpos, rlen);

            endpos += rlen;
            startpos = endpos;

            return rlen + diff;
        }
    }


    public int rollback() throws IOException {

        if (closed) {
            throw new EOFException();
        }

        int rbnum = startpos;

        startpos = 0;

        return rbnum;
    }


    public int rollback(int num) throws IOException {

        if (closed) {
            throw new EOFException();
        }

        int rbnum;

        if (num > startpos) {
            rbnum = startpos;
            startpos = 0;
        } else {
            rbnum = num;
            startpos -= num;
        }

        return rbnum;
    }


    public long skip(long n) throws IOException {

        if (closed) {
            throw new EOFException();
        }

        long skipped = 0;
        int diff = endpos - startpos;

        if (diff >= n) {
            startpos += n;

            return n;
        } else {
            byte[] fake = new byte[(int) n];

            startpos = endpos;
            skipped = read(fake, 0, (int) n - diff);
            fake = null;

            return diff + skipped;
        }
    }
}