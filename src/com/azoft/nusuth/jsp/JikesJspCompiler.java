package com.azoft.nusuth.jsp;

import java.io.*;

/**
 * This class represent jikes compiler.
 * @author skilz, vdgg
 * @version 1.6
 * @since Nusuth1.0
 */
public class JikesJspCompiler extends JspCompiler {

    private static String executable = "jikes";

    public JikesJspCompiler() {
        super();
    }

    /**
     * This methid compile given source.
     * @param source Path to source file.
     * @return true if compilation passed successfuly, otherwise false.
     */
    public boolean compile(String source) {
        String[] start = new String[]{JikesJspCompiler.executable, "-classpath",
                                      classPath, "-d", outputDir,
                                      "-nowarn", source};
        Process pr = null;
        int exit = -1;
        try {
            pr = Runtime.getRuntime().exec(start);
        } catch (IOException e) {
            try {
                errorOut.write("Cannot find path to jikes compiler".getBytes());
            } catch (IOException e1) {
                return false;
            }
            return false;
        }
        try {
            BufferedInputStream compilerErr
                    = new BufferedInputStream(pr.getErrorStream());
            StreamPumper errPumper = new StreamPumper(compilerErr, errorOut);
            errPumper.start();
            pr.waitFor();
            exit = pr.exitValue();
            errPumper.join();
            compilerErr.close();
            errorOut.close();
        } catch (Exception ex) {
            try {
                errorOut.write("Error occured during working with streams".getBytes());
            } catch (IOException e) {
                return false;
            }
            return false;
        }
        if (exit == 0) {
            return true;
        } else {
            return false;
        }
    }

    public static void setExecutable(String exec) {
        executable = exec;
    }


    class StreamPumper extends Thread {
        private BufferedInputStream stream;
        private boolean endOfStream = false;
        private boolean stopSignal = false;
        private int BUFFER_SIZE = 1024;
        private OutputStream out;

        public StreamPumper(BufferedInputStream is, OutputStream out) {
            this.stream = is;
            this.out = out;
        }

        public void pumpStream() throws IOException {
            byte[] buf = new byte[BUFFER_SIZE];
            if (!endOfStream) {
                int bytesRead = stream.read(buf, 0, BUFFER_SIZE);

                if (bytesRead > 0) {
                    out.write(buf, 0, bytesRead);
                } else if (bytesRead == -1) {
                    endOfStream = true;
                }
            }
        }

        public void run() {
            try {
                while (!endOfStream) {
                    pumpStream();
                }
            } catch (IOException ioe) {
            }
        }

    }


}

