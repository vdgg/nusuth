package com.azoft.nusuth.core;

import com.azoft.nusuth.jsp.JspCompiler;
import com.azoft.nusuth.jsp.JavacJspCompiler;

import java.util.Hashtable;
import java.io.File;

/**
 * This thread search all java src files in specified directory and compile
 * them to given directory.
 * @author skilz.
 * @version 1.2
 * @since Nusuth1.0
 */
public class ContextCompileListener extends Thread {

    /**Time to sleep*/
    private long checkTime = -1;
    /**Directory of source files*/
    private String srcDir = null;
    /**Compiler*/
    private JspCompiler compiler = null;
    /**Hashtable with file name to modified time mapping;*/
    private Hashtable fileName2lastModified = new Hashtable();
    /**Servlet context*/
    private NusuthContext context = null;
    /**life indicator*/
    private boolean alive = true;

    /**
     * Constructor.
     * @param srcDir Source directory.
     * @param checkTime Time to sleep.
     * @param compiler Compiler to use.
     */
    public ContextCompileListener(NusuthContext context, String srcDir,
                                  long checkTime, JspCompiler compiler) {
        this.checkTime = checkTime;
        this.srcDir = srcDir;
        this.compiler = compiler;
        this.context = context;
    }

    public void run() {
        File src = new File(srcDir);
        fillTable(src);
        while (!context.isShuttingDown() && alive) {
            compileFiles(src);
            try {
                sleep(checkTime);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Creates file name to file last modification time mapping;
     * @param dir Root directory. Mappings creates for all files in this
     * directory.
     */
    private void fillTable(File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                fillTable(files[i]);
            } else if (files[i].getAbsolutePath().endsWith(".java")) {
                fileName2lastModified.put(files[i].getAbsolutePath(),
                        new Long(files[i].lastModified()));
            }
        }
    }

    /**
     * Compile all files from given directory.
     * @param dir Directory from which compile source files.
     */
    private void compileFiles(File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                compileFiles(files[i]);
            } else if (files[i].getAbsolutePath().endsWith(".java")) {
                if (files[i].lastModified()
                        != ((Long) fileName2lastModified.
                        get(files[i].getAbsolutePath())).longValue()) {
                    if (compiler.compile(files[i].getAbsolutePath())) {
                        fileName2lastModified.put(files[i].getAbsolutePath(),
                                new Long(files[i].lastModified()));
                    }
                }
            }
        }
    }

    /**
     * This method shutdown compiling thread.
     */
    public void shutDown() {
        alive = false;
    }

}
