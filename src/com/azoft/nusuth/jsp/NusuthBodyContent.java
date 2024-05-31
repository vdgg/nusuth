package com.azoft.nusuth.jsp;

import java.io.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

//import com.azoft.nusuth.util.Logger;

public class NusuthBodyContent extends BodyContent {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private Reader reader;
    private String content = null;
    private final static byte[] crlf = {13, 10};

    NusuthBodyContent(JspWriter prevOut) {
        super(prevOut);
    }

    public void clearBody() {
        baos.reset();
        content = null;
    }

    public Reader getReader() {
        if (reader == null) {
            reader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        }
        return reader;
    }

    public String getString() {
        if (content == null) {
            content = new String(baos.toByteArray());
        }
        return content;
    }

    public void writeOut(Writer out) throws IOException {
        out.write(getString());
    }

    public void clear() throws IOException {
        clearBody();
    }

    public void clearBuffer() throws IOException {
        clearBody();
    }

    public int getBufferSize() {
        return 0;
    }

    public int getRemaining() {
        return 0;
    }

    public boolean isAutoFlush() {
        return false;
    }

    public void newLine() throws IOException {
        baos.write(crlf);
    }

    public void print(boolean b) throws IOException {
        print(new Boolean(b));
    }

    public void println(boolean b) throws IOException {
        println(new Boolean(b));
    }

    public void print(char c) throws IOException {
        write(c);
    }

    public void println(char c) throws IOException {
        write(c);
        baos.write(crlf);
    }

    public void print(char[] s) throws IOException {
        write(s);
    }

    public void println(char[] s) throws IOException {
        write(s);
        baos.write(crlf);
    }

    public void print(double d) throws IOException {
        print(new Double(d));
    }

    public void println(double d) throws IOException {
        println(new Double(d));
    }

    public void print(float f) throws IOException {
        print(new Float(f));
    }

    public void println(float f) throws IOException {
        println(new Float(f));
    }

    public void print(int i) throws IOException {
        print(new Integer(i));
    }

    public void println(int i) throws IOException {
        println(new Integer(i));
    }

    public void print(long l) throws IOException {
        print(new Long(l));
    }

    public void println(long l) throws IOException {
        println(new Long(l));
    }

    public void print(Object o) throws IOException {
        print(o.toString());
    }

    public void println(Object o) throws IOException {
        println(o.toString());
    }

    public void print(String s) throws IOException {
        write(s);
    }

    public void println(String s) throws IOException {
        write(s);
        baos.write(crlf);
    }
//
    public void write(String s) throws IOException {
        write(s.toCharArray(), 0, s.length());
    }
//
    public void close() throws IOException {
        baos.close();
    }

    public void println() throws IOException {
        baos.write(crlf);
    }

    public void write(char[] c, int off, int len) throws IOException {
        byte[] b = new byte[len];
        for (int i = 0; i < len; i++) {
            b[i] = (byte) c[i + off];
        }
        baos.write(b, 0, b.length);
        if (content != null) {
            content = content + (new String(b));
        } else {
            content = new String(b);
        }
    }
}
