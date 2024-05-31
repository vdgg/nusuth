package com.azoft.nusuth.distributor.cache;

import com.azoft.nusuth.util.*;
import com.azoft.nusuth.core.HttpNusuthServletInputStream;

import java.util.*;
import java.io.*;

public class CachedElement {
    private final static String HTTP_LAST_MODIFIED = new String(HttpConstants.LAST_MODIFIED);

    private final static char hexPrefix = '_';

    /* previous element in EvictQueue */
    CachedElement prev;

    /* next element in EvictQueue */
    CachedElement next;

    /* time when this element was be modified */
    long lastModified;

    /* time when this element was be checked */
    long lastChecked;

    /* url of cached page */
    private StrBuffer url;

    /* file that holds this cached element */
    protected File file;

    /* cached response headers */
    private NusuthHeaders headers;

    /* cached response body */
    private byte[] body;

    /* Creates a new CacheElement and writes it to disk. */
    public CachedElement(StrBuffer url, NusuthHeaders headers, byte[] body, File cacheDir)
            throws IOException {
        this.url = url;
        this.headers = headers;
        this.body = body;
        this.file = new File(cacheDir, encodeUrl());
        setLastModified(headers.getDateHeader(HTTP_LAST_MODIFIED));
        setLastChecked();
        write();
    }

    /* Loads element from disk. */
    public CachedElement(StrBuffer url, File cacheDir)
            throws IOException {
        this.url = url;
        this.file = new File(cacheDir, encodeUrl());
        headers = null;
        body = null;
    }

    /* Loads element from disk. */
    public CachedElement(File file)
            throws IOException {
        this.file = file;
        this.url = decodeUrl();
        headers = null;
        body = null;
    }

    /* updates CacheElement by new data and writes it to disk. */
    public synchronized void update(NusuthHeaders headers, byte[] body, File cacheDir)
            throws IOException {
        this.headers = headers;
        this.body = body;
        this.file = new File(cacheDir, encodeUrl());
        setLastModified(headers.getDateHeader(HTTP_LAST_MODIFIED));
        setLastChecked();
        write();
    }

    private void createFile()
            throws IOException {
        if (!file.createNewFile()) {
            file.delete();
            file.createNewFile();
        }
    }

    private String encodeUrl() {
        String encodedUrl = "";
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            int ci = (int) c;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))
                encodedUrl += c;
            else
                encodedUrl += hexPrefix + char2hex(c);
        }
        return encodedUrl;
    }

    private StrBuffer decodeUrl()
            throws IOException {
        String fileName = file.getCanonicalPath();
        String encodedUrl = fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
        StrBuffer result = new StrBuffer(encodedUrl.length());
        for (int i = 0; i < encodedUrl.length(); i++) {
            char c = encodedUrl.charAt(i);
            if (c != hexPrefix)
                result.append(c);
            else {
                char c1 = encodedUrl.charAt(++i);
                char c2 = encodedUrl.charAt(++i);
                result.append(hex2char(c1, c2));
            }
        }
        return result;
    }

    private String char2hex(char c) {
        int i1 = (c & 0xF0) >> 4;
        int i2 = c & 0xF;
        char c1 = i1 < 10 ? (char) (i1 + '0') : (char) (i1 + 'A' - 10);
        char c2 = i2 < 10 ? (char) (i2 + '0') : (char) (i2 + 'A' - 10);
        return new String(new char[]{c1, c2});
    }

    private char hex2char(char c1, char c2) {
        int i1 = c1 >= '0' && c1 <= '9' ? c1 - '0' : c1 - 'A' + 10;
        int i2 = c2 >= '0' && c2 <= '9' ? c2 - '0' : c2 - 'A' + 10;
        return (char) ((i1 << 4) + i2);
    }

    public synchronized StrBuffer getUrl() {
        return url;
    }

    public synchronized NusuthHeaders getHeaders() {
        return headers;
    }

    public synchronized byte[] getBody() {
        return body;
    }

    /** Writes self to disk */
    private synchronized void write()
            throws IOException {
        if (!isLoaded())
            throw new IllegalStateException("Cache Element unloaded now and can't be written");

        DataOutputStream os = new DataOutputStream(new FileOutputStream(file));
        headers.write(os);
        os.writeInt(body.length);
        os.writeLong(lastModified);
        os.write(body);
    }

    /** loads self from disk */
    public synchronized void load()
            throws IOException {
        unload();
        //DataInputStream is = new DataInputStream(new FileInputStream(file));
        HttpNusuthServletInputStream is = new HttpNusuthServletInputStream(new FileInputStream(file));
        headers = new NusuthHeaders();
        headers.read(is);
        DataInputStream dis = new DataInputStream(is);
        int length = dis.readInt();
        body = new byte[length];
        lastModified = dis.readLong();
        int readed = 0;
        for (int i = 0; i < length && readed > 0;) {
            readed = is.read(body, i, length - i);
        }
    }

    public synchronized void unload() {
        headers = null;
        body = null;
    }

    /* removes self from disk */
    public synchronized void delete() {
        unload();
        file.delete();
    }

    public synchronized boolean isLoaded() {
        return headers != null && body != null;
    }

    public synchronized long getLastModified() {
        return lastModified;
    }

    public synchronized void setLastModified(long modifiedTime) {
        lastModified = modifiedTime;
    }

    public synchronized long getLastChecked() {
        return lastChecked;
    }

    public synchronized void setLastChecked() {
        lastChecked = System.currentTimeMillis();
    }

}

