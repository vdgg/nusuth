package com.azoft.nusuth.jsp;

import java.io.*;
import java.net.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.TagLibraryValidator;

import com.azoft.nusuth.core.*;
import com.azoft.nusuth.util.*;

import java.util.*;

/**
 * @author vdgg, skilz
 * @version 1.20
 * @since Nusuth1.0
 */
public class JspLoader implements TagLibraryChangeListener {
    private String workDir;
    private String contextWorkDir;
    private NusuthContext context;
    private Hashtable jsp2time = new Hashtable();
    private Hashtable jsp2check = new Hashtable();
    private Hashtable jsp2class = new Hashtable();
    private Hashtable jsp2include = new Hashtable();

    private Hashtable src2util = new Hashtable();

    private static Class compilerClass;
    private String classPath = "";
    private List loadingJSPs = new ArrayList();
    private static boolean load_on_start = false;
    private static long jspRefresh = 10000;
    private final static String SER_FILENAME = "_nusuth_serialized";
    private TagLibraryRepository repository;
    private org.apache.log4j.Category cat = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jsp");
    private List validators = new ArrayList();

/*  static {
    try {
      compilerClass = Class.forName(strcompiler);
//      compilerClass = Class.forName("com.azoft.nusuth.jsp.JavacJspCompiler");
    } catch (Exception e) {
      //Logger.log(e, 1);
      org.apache.log4j.Category.getInstance("jsp").error("Missing jsp compiler", e);
    }
  }
*/

    public synchronized void onTagLibraryChange() {
        jsp2check.clear();
        jsp2time.clear();
        loadingJSPs.clear();
    }

    public synchronized void clearCache() {
        jsp2check.clear();
        jsp2time.clear();
        loadingJSPs.clear();
        jsp2include.clear();
        jsp2class.clear();
    }

    public JspLoader(String workDir, NusuthContext context, TagLibraryRepository repository) throws IOException {
        this.workDir = workDir;
        this.contextWorkDir = workDir + File.separator + "_jsp_" + convertName(context.getContextName());
        File wdFile = new File(contextWorkDir);
        if (!wdFile.exists()) {
            if (!wdFile.mkdirs()) {
                throw new IOException("Cannot create work directory " + contextWorkDir);
            }
        }
        this.context = context;
        this.repository = repository;
        ServletLoader loader = context.getServletLoader();
        loader.addClassPath(workDir);
        Iterator iter = loader.getClassPathes();
        String cpath;
        String sep = File.pathSeparator;
        while (iter.hasNext()) {
            cpath = (String) iter.next();
            if (classPath.length() > 0) {
                classPath += sep;
            }
            classPath += cpath;
        }
        String syscp = System.getProperty("java.class.path");
        if (syscp != null && syscp.length() > 0) {
            classPath += sep;
            classPath += syscp;
        }
        if (load_on_start) {
            try {
                File file = new File(contextWorkDir, SER_FILENAME);
                if (file.exists()) {
                    FileInputStream fis = new FileInputStream(file);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    jsp2time = (Hashtable) ois.readObject();
                    ois.close();
                }
            } catch (Throwable t) {
                //Logger.log("Cannot read jsp mod_time from "+workDir+File.separator+SER_FILENAME+", nested: "+t.getMessage(), 1);
                cat.error("Cannot read jsp mod_time from " + workDir + File.separator + SER_FILENAME + ", nested: " + t.getMessage());
                jsp2time = new Hashtable();
            }
        }
    }

    public JspPage loadJsp(String source, File rFile, boolean isDirectory)
            throws IOException, JspException {
        int ddotsIndex = source.indexOf("/../");
        while (ddotsIndex > -1) {
            int pSlash = source.lastIndexOf("/", ddotsIndex);
            if (pSlash == -1) {
                throw new JspException("Incorrect jsp source path: " + source);
            }
            String tmp = source.substring(ddotsIndex + 4);
            source = source.substring(0, pSlash + 1) + tmp;
            ddotsIndex = source.indexOf("/../");
        }
        if (!isDirectory) {
            Long time = (Long) jsp2time.get(source);
            long modTime = time == null ? System.currentTimeMillis() : time.longValue();
            Long checkTime = (Long) jsp2check.get(source);
            if (checkTime == null) {
                modTime = rFile.lastModified();
                jsp2check.put(source, new Long(System.currentTimeMillis()));
            } else {
                if (jspRefresh >= 0) {
                    if (jspRefresh > 0) {
                        if ((System.currentTimeMillis() - checkTime.longValue()) >= jspRefresh) {
                            modTime = rFile.lastModified();
                            jsp2check.put(source, new Long(System.currentTimeMillis()));
                        }
                    } else {
                        modTime = rFile.lastModified();
                        jsp2check.put(source, new Long(System.currentTimeMillis()));
                    }
                }
            }
            if (time != null) {
//        if (modTime == time.longValue()) {
//        if (modTime == time.longValue() && allIncludesNotChange(rFile)) {
                if (modTime == time.longValue() && allIncludesNotChange(source)) {
                    JspPage pg = (JspPage) jsp2class.get(source);
                    if (pg != null) {
                        return pg;
                    }
                } else {
                    JspPage page = (JspPage) jsp2class.get(source);
                    if (page != null) {
                        page.destroy();
                    }
                    jsp2time.remove(source);
                    jsp2class.remove(source);
                }
            }

            if (!loadingJSPs.contains(source)) {
                loadingJSPs.add(source);
            }
            synchronized (loadingJSPs.get(loadingJSPs.indexOf(source))) {
                if (jsp2class.get(source) != null) {
                    return ((JspPage) jsp2class.get(source));
                }
                int loccur = source.lastIndexOf('/');
                String jspWorkDir = contextWorkDir;
                String packName = context.getContextName();
                if (loccur > 0) {
                    jspWorkDir += source.substring(0, loccur).replace('/', File.separatorChar);
                    packName += source.substring(0, loccur).replace('/', '.');
                }
                packName = convertName(packName);
                File jspWDirFile = new File(jspWorkDir);
                if (!jspWDirFile.exists() && !jspWDirFile.mkdirs()) {
                    throw new IOException("Cannot create work directory " + jspWorkDir);
                }
                String fName = "_" + rFile.getName().substring(0, rFile.getName().length() - 4) + "_jsp_";
                String srcFile = jspWorkDir + File.separator + fName + ".java";
                if (!load_on_start || time == null || modTime != time.longValue()) {
                    time = new Long(modTime);
                    String realPath = rFile.getCanonicalPath();
//          JspParser parser = new JspParser(realPath, jspWorkDir, packName, fName, null, context.getDocBase(), repository, context.getServletLoader());
                    AbstractJspParser parser = AbstractJspParser.getParser(realPath, jspWorkDir, packName, fName, null, context.getDocBase(), repository, context.getServletLoader());
                    try {
                        parser.parse();
                    } catch (JspException e) {
                        throw e;
                    } catch (IOException e) {
                        throw e;
                    } finally {
                        if (parser.getValidator() != null && !validators.contains(parser.getValidator())) {
                            validators.add(parser.getValidator());
                        }
                    }
                    jsp2include.put(source, parser.getAllIncludedFiles());
                    JspCompiler compiler = null;
                    try {
                        compiler = (JspCompiler) compilerClass.newInstance();
                    } catch (Exception ex) {
                        //Logger.log(ex, 1);
//            cat.error("Cannot instantiate compiler", ex);
                        throw new JspException("Cannot instantiate compiler, nested: " + ex);
                    }
                    compiler.setClassPath(classPath);
                    compiler.setEncoding(parser.getEncoding());
                    compiler.setOutputDir(workDir);
                    ByteArrayOutputStream errStream = new ByteArrayOutputStream();
                    compiler.setErrorOut(errStream);
                    if (!compiler.compile(srcFile)) {
                        //Logger.log("Cannot compile generated servlet from " + realPath + ", reported error: " + errStream.toString(), 1);
//            cat.error("Cannot compile generated servlet from " + realPath + ", reported error: " + errStream.toString());
                        throw new JspException("Cannot compile " + srcFile + ",\r\n compiler reported: " + errStream.toString());
                    }
                }
                try {
                    ServletLoader loader = ServletLoader.createLoader(context.getServletLoader());
                    loader.addClassPath(workDir);
                    String jspPageName = "_jsp_" + packName + "." + fName.replace(' ', '_');
                    loader.defineOnlyClass(jspPageName);
//          JspPage page = (JspPage)(loader.loadServlet("_jsp_" + packName + "." + fName));
                    JspPage page = (JspPage) (loader.loadServlet(jspPageName));
                    page.init(
                            new NusuthServletConfig(
                                    new Hashtable(), context, fName, new ClassOrJsp(srcFile.substring(0, srcFile.length() - 4), true)));
                    jsp2time.put(source, time);
                    jsp2class.put(source, page);
                    loadingJSPs.remove(source);
                    return page;
                } catch (Exception ex) {
                    //Logger.log(ex, 1);
//          cat.error("Cannot load generated jsp",  ex);
                    throw new JspException("Cannot load generated jsp, nested: " + ex);
                }
            }
        } else {
            parseAll(rFile);
            compileAll(contextWorkDir);
            return null;
        }
    }

    public void releaseValidators() {
        for (int i = 0; i < validators.size(); i++) {
            ((TagLibraryValidator) validators.get(i)).release();
        }
    }
/*
  private boolean allIncludesNotChange(File rFile){
    LinkedList list = (LinkedList)jsp2include.get(rFile);
    if (list == null) {
      return true;
    }
    JspInvocationCacheElement element;
    for (int i=0; i<list.size(); i++) {
      element = (JspInvocationCacheElement)list.get(i);
      if (element.realFile.lastModified() > element.lastAccess)
        return false;
    }
    return true;
  }
*/
    private boolean allIncludesNotChange(String src) {
        LinkedList list = (LinkedList) jsp2include.get(src);
        if (list == null) {
            return true;
        }
        JspInvocationCacheElement element;
        for (int i = 0; i < list.size(); i++) {
            element = (JspInvocationCacheElement) list.get(i);
            if (element.realFile.lastModified() > element.lastAccess)
                return false;
        }
        return true;
    }

    private void parseAll(File rFile) throws IOException {
        if (rFile.isDirectory()) {
            File[] files = rFile.listFiles();
            for (int i = 0; i < files.length; i++) {
                parseAll(files[i]);
            }
        } else {
            if (rFile.getAbsolutePath().endsWith(".jsp")) {
                String source = rFile.getAbsolutePath().substring((context.getDocBase()).length());
                source = source.replace(File.separatorChar, '/');
                int loccur = source.lastIndexOf('/');
                String jspWorkDir = contextWorkDir;
                String packName = context.getContextName();
                if (loccur > 0) {
                    jspWorkDir += source.substring(0, loccur).replace('/', File.separatorChar);
                    packName += source.substring(0, loccur).replace('/', '.');
                }
                packName = convertName(packName);
                File jspWDirFile = new File(jspWorkDir);
                if (!jspWDirFile.exists() && !jspWDirFile.mkdirs()) {
                    throw new IOException("Cannot create work directory " + jspWDirFile);
                }
                String fName = "_" + rFile.getName().substring(0, rFile.getName().length() - 4) + "_jsp_";
                String srcFile = jspWorkDir + File.separator + fName + ".java";
                String realPath = rFile.getCanonicalPath();
                AbstractJspParser parser = AbstractJspParser.getParser(realPath, jspWorkDir, packName, fName, null, context.getDocBase(), repository, context.getServletLoader());
                try {
                    parser.parse();
                    SrcToJspUtils util = new SrcToJspUtils(parser.getEncoding(), packName, fName, source);
                    src2util.put(srcFile, util);
                } catch (JspException ex) {
                    cat.error("Cannot parse jsp " + source, ex);
                }
            }
        }
    }

    public void compileAll(String dir) throws JspException {
        JspCompiler compiler = null;
        try {
            compiler = (JspCompiler) compilerClass.newInstance();
        } catch (Exception ex) {
//      cat.error("Cannot instantiate compiler", ex);
            throw new JspException("Cannot instantiate compiler, nested: " + ex);
        }
        ServletLoader loader = ServletLoader.createLoader(context.getServletLoader());
        Enumeration enum = src2util.keys();
        while (enum.hasMoreElements()) {
            String src = (String) enum.nextElement();
            String encoding = ((SrcToJspUtils) src2util.get(src)).encoding;
            String fName = ((SrcToJspUtils) src2util.get(src)).fName;
            String pack = ((SrcToJspUtils) src2util.get(src)).pack;
            String jspSource = ((SrcToJspUtils) src2util.get(src)).jspSource;
            compiler.setClassPath(classPath);
            compiler.setEncoding(encoding);
            compiler.setOutputDir(workDir);
            ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            compiler.setErrorOut(errStream);
            if (!compiler.compile(src)) {
                cat.error("Cannot compile generated servlet from " + jspSource + ", reported error: " + errStream.toString());
                continue;
            }
            try {
                loader.addClassPath(workDir);
                JspPage page = (JspPage) (loader.loadServlet("_jsp_" + pack + "." + fName));
                page.init(
                        new NusuthServletConfig(
                                new Hashtable(), context, fName, new ClassOrJsp(src.substring(0, src.length() - 4), true)));
                long time = (new File(context.getDocBase(), jspSource)).lastModified();
                jsp2time.put(jspSource, new Long(time));
                jsp2class.put(jspSource, page);
                loadingJSPs.remove(jspSource);
            } catch (Exception ex) {
                cat.error("Cannot load generated jsp " + jspSource, ex);
            }
        }
    }

    public NusuthContext getContext() {
        return context;
    }

    public static void setCompilerClass(Class compClass) {
        compilerClass = compClass;
    }

    protected synchronized void destroyAll() {
        Enumeration enum = jsp2class.keys();
        JspPage page;
        while (enum.hasMoreElements()) {
            page = (JspPage) jsp2class.get(enum.nextElement());
            page.destroy();
        }
        if (load_on_start) {
            try {
                File file = new File(contextWorkDir, SER_FILENAME);
                if (file.exists()) {
                    file.delete();
                }
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(jsp2time);
                oos.flush();
                oos.close();
            } catch (Throwable t) {
                //Logger.log("Cannot serialize jsp mod_time to "+workDir+File.separator+SER_FILENAME+", nested: "+t.getMessage(), 1);
                cat.error("Cannot serialize jsp mod_time to " + workDir + File.separator + SER_FILENAME + ", nested: " + t.getMessage());
            }
        }
    }

    public static void setLoadOnStart(boolean val) {
        load_on_start = val;
    }

    public static void setJspRefresh(long val) {
        jspRefresh = val;
    }

    private String convertName(String name) {
        StringBuffer sb = new StringBuffer();
        char tmp;
        for (int i = 0; i < name.length(); i++) {
            tmp = name.charAt(i);
            if ((tmp >= 'a' && tmp <= 'z') || (tmp >= 'A' && tmp <= 'Z') ||
                    (tmp >= '0' && tmp <= '9') || tmp == '_' || tmp == '/' || tmp == '\\' || tmp == '.') {
                sb.append(tmp);
            } else {
                sb.append("d" + (int) tmp);
            }
        }
        return sb.toString();
    }


}
