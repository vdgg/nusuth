package com.azoft.nusuth.jsp;

import javax.servlet.jsp.JspWriter;
import java.io.*;

/**
 * @author vdgg, skilz
 * @version 1.5
 * @since 1.0
 */
public class NusuthJspWriter extends JspWriter {

    public static final int DEFAULT_BUFFER_SIZE = 8092;
    private OutputStream stream;
    private byte[] buffer;
    private int curPos = 0;
    private boolean commited = false;
    private boolean closed = false;
    private String encoding = null;
    private boolean inclFlush = true;
    private static byte[] newLine = null;
    private boolean defaultEncoding = true;

    static {
        String sep = System.getProperty("line.separator");
        if (sep == null) {
            newLine = "\r\n".getBytes();
        } else {
            newLine = sep.getBytes();
        }
    }

    public NusuthJspWriter(OutputStream stream) {
        this(DEFAULT_BUFFER_SIZE, true, stream);
    }

    public NusuthJspWriter(int bufferSize, boolean autoFlush, OutputStream stream) {
        super(bufferSize, autoFlush);
        this.stream = stream;
        buffer = new byte[bufferSize];
    }

    public NusuthJspWriter() {
        super(DEFAULT_BUFFER_SIZE, true);
    }

    public void init(int bufferSize, boolean autoFlush, OutputStream stream) {
        this.autoFlush = autoFlush;
        this.stream = stream;
        if (bufferSize != this.bufferSize) {
            this.bufferSize = bufferSize;
            buffer = new byte[bufferSize];
        }
        curPos = 0;
        commited = false;
        closed = false;
        inclFlush = true;
        defaultEncoding = true;
    }

    public void cleanUp() {
        try {
            close();
        } catch (Exception ex) {
        }
        stream = null;
        closed = true;
    }

    public void clear() throws IOException {
        if (closed) {
            throw new IOException("Writer already closed");
        }
        if (commited) {
            throw new IOException("Buffer already has been flushed");
        }
        curPos = 0;
    }

    public void clearBuffer() throws IOException {
        if (closed) {
            throw new IOException("Writer already closed");
        }
        curPos = 0;
    }

    public void close() throws IOException {
        if (!closed) {
            flush();
            closed = true;
        }
    }

    public void setFlush(boolean flush) {
        this.inclFlush = flush;
    }

    public void fakeFlush() throws IOException {
        if (closed) {
            throw new IOException("Writer already closed");
        }
        if (curPos > 0) {
            stream.write(buffer, 0, curPos);
            curPos = 0;
        }
    }

    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Writer already closed");
        }
        if (curPos > 0) {
            stream.write(buffer, 0, curPos);
            if (inclFlush) {
                stream.flush();
                commited = true;
            }
            curPos = 0;
        }
    }

    public void allFlush() throws IOException {
        if (closed) {
            throw new IOException("Writer already closed");
        }
        if (curPos > 0) {
            stream.write(buffer, 0, curPos);
            stream.flush();
            commited = true;
            curPos = 0;
        }
    }

    public int getRemaining() {
//Commented by skilz...
//    return curPos;
//Added by skilz...
        return (buffer.length - curPos);
    }

    /**
     * Writes new line.
     * @throws IOException.
     */
    public void newLine() throws IOException {
        if (curPos >= bufferSize) {
            if (autoFlush) {
                flush();
            } else {
                throw new IOException("Buffer overflow error");
            }
        }
        rawWrite(newLine);
    }

    public void print(boolean b) throws IOException {
        print(String.valueOf(b));
    }

    public void print(char c) throws IOException {
        print(String.valueOf(c));
    }

    public void print(int i) throws IOException {
        print(String.valueOf(i));
    }

    public void print(long l) throws IOException {
        print(String.valueOf(l));
    }

    public void print(float f) throws IOException {
        print(String.valueOf(f));
    }

    public void print(double d) throws IOException {
        print(String.valueOf(d));
    }

    public void print(char[] s) throws IOException {
        print(String.valueOf(s));
    }

    /**
     * Prints given string to output.
     *@param s String to print.
     * Throws IOException
     */
    public void print(String s) throws IOException {
        if (s == null) {
//      return;
            print("null");
            return;
        }
        if (curPos >= bufferSize) {
            if (autoFlush) {
                flush();
            } else {
                throw new IOException("Buffer overflow error");
            }
        }

        if (encoding != null) {
            if (defaultEncoding) {
                byte[] byteArray = new byte[s.length()];
                for (int i = 0; i < byteArray.length; i++) {
                    byteArray[i] = (byte) s.charAt(i);
                }
                rawWrite(byteArray);
            } else {
                rawWrite(s.getBytes(encoding));
            }
        } else {
            byte[] byteArray = new byte[s.length()];
            for (int i = 0; i < byteArray.length; i++) {
                byteArray[i] = (byte) s.charAt(i);
            }
            rawWrite(byteArray);
        }
/*
    if (encoding != null) {
      rawWrite(s.getBytes(encoding));
    } else {
      rawWrite(s.getBytes());
    }
*/
    }

    public void print(Object obj) throws IOException {
        if (obj != null) {
            print(obj.toString());
        } else {
            print("null");
        }
    }

    public void println() throws IOException {
        newLine();
    }

    public void println(boolean b) throws IOException {
        print(b);
        println();
    }

    public void println(char c) throws IOException {
        print(c);
        println();
    }

    public void println(int i) throws IOException {
        print(i);
        println();
    }

    public void println(long l) throws IOException {
        print(l);
        println();
    }

    public void println(float f) throws IOException {
        print(f);
        println();
    }

    public void println(double d) throws IOException {
        print(d);
        println();
    }

    public void println(char[] s) throws IOException {
        print(s);
        println();
    }

    public void println(String s) throws IOException {
        print(s);
        println();
    }

    public void println(Object obj) throws IOException {
        print(obj);
        println();
    }

    public void write(char[] s, int off, int len) throws IOException {
        print(new String(s, off, len));
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
        if (!encoding.equals("ISO8859_1") && !encoding.equals("ISO-8859-1")) {
            defaultEncoding = false;
        }
    }

    public String getEncoding() {
        return encoding;
    }

    public void write(int i) throws IOException {
        int mask = 0x0000FFFF;
        i = i & mask;
        print((char) i);
    }

    public void rawWrite(byte[] b) throws IOException {
        if (buffer.length != 0) {
            int startPos = 0;
            int toCopy;
            while (startPos < b.length) {
                toCopy = bufferSize - curPos < b.length - startPos ? bufferSize - curPos : b.length - startPos;
                System.arraycopy(b, startPos, buffer, curPos, toCopy);
                startPos += toCopy;
                curPos += toCopy;
                if (curPos >= bufferSize) {
                    if (autoFlush) {
                        allFlush();
                    } else {
                        if (startPos < b.length) {
                            throw new IOException("Buffer overflow error");
                        }
                    }
                }
            }
        } else {
            if (closed) {
                throw new IOException("Writer already closed");
            }
            stream.write(b);
            stream.flush();
            commited = true;
            curPos = 0;
        }
    }
}

