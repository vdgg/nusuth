package com.azoft.nusuth.util;

import java.io.*;

public class AccessLogger {

    private byte[] cache;
    private int endPos;
    private OutputStream out;
    private int writeCount;
    private int cacheSize;
    private int maxBackupIndex;
    private int maxFileSize;
    private File[] files;
    private String realFileName;

    public AccessLogger(File file, int maxFileSize, int maxBackupIndex) throws IOException {
        this(file, maxFileSize, maxBackupIndex, 65536);
    }

    public AccessLogger(File file, int maxFileSize, int maxBackupIndex, int cacheSize) throws IOException {
        this.maxFileSize = maxFileSize;
        this.maxBackupIndex = maxBackupIndex;
        this.cacheSize = cacheSize;
        cache = new byte[cacheSize];
        files = new File[maxBackupIndex + 1];
        files[0] = file;
        realFileName = file.getCanonicalPath();
        for (int i = 1; i < files.length; i++) {
            File tmpFile = new File(realFileName + "." + i);
            if (tmpFile.exists() && !tmpFile.isDirectory()) {
                files[i] = tmpFile;
            }
        }
        endPos = 0;
        out = new BufferedOutputStream(new FileOutputStream(realFileName, true), cacheSize);
        writeCount = file.exists() ? (int) file.length() : 0;
        (new AccessLogWriter()).start();
    }

    public final void log(StrBuffer buffer) {
        synchronized (cache) {
            if (cacheSize < buffer.length() + endPos) {
                write();
            }
            buffer.copy2Bytes(cache, endPos);
            endPos += buffer.length();
        }
    }

    public void setCacheSize(int cacheSize) {
        synchronized (cache) {
            this.cacheSize = cacheSize;
        }
    }

    private void rollover() throws IOException {
        if (files[files.length - 1] != null) {
            files[files.length - 1].delete();
        }
        out.close();
        for (int i = files.length - 2; i >= 0; i--) {
            if (files[i] != null) {
                File tmpFile = new File(realFileName + "." + (i + 1));
                files[i].renameTo(tmpFile);
                files[i + 1] = tmpFile;
            }
        }
        files[0] = new File(realFileName);
        out = new BufferedOutputStream(new FileOutputStream(files[0]), cacheSize);
    }


    private synchronized void write() {
        if (endPos > 0) {
            try {
                out.write(cache, 0, endPos);
                out.flush();
                writeCount += endPos;
                if (writeCount >= maxFileSize) {
                    rollover();
                    writeCount = 0;
                }
                endPos = 0;
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }

    class AccessLogWriter extends Thread {

        AccessLogWriter() {
            super("AccessLogWriter");
        }

        public void run() {
            while (true) {
                try {
                    sleep(1000);
                } catch (Exception ex) {
                }
                write();
            }
        }
    }
}