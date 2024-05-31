package com.azoft.nusuth.core;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.OutputStream;

/**
 * This is wrapper for ServletOutputStream. It used in XSLT Filter.
 * @author skilz.
 * @version 1.2
 * @since Nusuth1.0
 */
public class XmlFilterServletOutputStream extends ServletOutputStream {

    /**Stream to write*/
    private DataOutputStream stream;

    /**
     * Constructor.
     * @param out OutputStream to use.
     */
    public XmlFilterServletOutputStream(OutputStream out) {
        stream = new DataOutputStream(out);
    }

    /**
     * Writes int to out.
     * @param i int to write.
     */
    public void write(int i) throws IOException {
        stream.write(i);
    }

    /**
     * Writes array of bytes to out.
     * @param abyte0[] array to write.
     */
    public void write(byte abyte0[]) throws IOException {
        stream.write(abyte0);
    }

    /**
     * Writes len bytes from the specified byte array starting
     * at offset off to this output stream.
     * b - the data.
     * off - the start offset in the data.
     * len - the number of bytes to write.
     */
    public void write(byte b[], int off, int len) throws IOException {
        stream.write(b, off, len);
    }

}
