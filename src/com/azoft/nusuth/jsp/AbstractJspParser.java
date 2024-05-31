package com.azoft.nusuth.jsp;

import com.azoft.nusuth.util.RollbackInputStream;

import java.io.*;
import java.util.*;
import java.beans.*;
import java.lang.reflect.Method;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;


/**
 * This is an Abstract class for Jsp parsers.
 * @author vdgg, skilz
 * @version 1.19
 * @since Nusuth1.0
 */
public abstract class AbstractJspParser {

    protected static Class booleanWrapper = null;
    protected static Class byteWrapper = null;
    protected static Class charWrapper = null;
    protected static Class doubleWrapper = null;
    protected static Class intWrapper = null;
    protected static Class floatWrapper = null;
    protected static Class longWrapper = null;
    protected static Class shortWrapper = null;
    protected static Class objectWrapper = null;
    protected static Class stringWrapper = null;
    protected static String wrapperName = null;
    protected final static String IE_CLSID
            = "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";
    protected String contextName;
    protected String fileName;
    protected OutputStream destination;
    protected Hashtable jspTags;
    protected String extClass = null;
    protected List importList = new ArrayList();
    protected String sessionEnabled;
    protected int bufferSize = -1;
    protected String autoFlush;
    protected String flush = "false";
    protected String threadSafe;
    protected String info;
    protected String errorPage;
    protected String isErrorPage;
    protected String contentType;
    protected PreServlet preServ;
    protected String contextBase;
    protected String encoding;
    protected String pageEncoding;
    protected TagLibraryRepository repository;
    protected Hashtable prefix2lib = new Hashtable();
    protected Stack closeTags = new Stack();
    protected int tagCounter = 0;
    protected Hashtable availBeans = new Hashtable();
    protected Hashtable availBeansClasses = new Hashtable();
    protected int setterCounter = 0;
    protected String queryString = null;
    protected String pageUrl = null;
    protected List allowedTags = new ArrayList();
    protected Hashtable pluginParams = new Hashtable();
    protected Hashtable iePluginParams = new Hashtable();
    protected String fallBackText = null;
    protected boolean isFallBack = false;
    protected boolean tagdepend = false;
    protected boolean empty = false;
    protected String nspluginurl
            = "http://java.sun.com/products/plugin/";
    protected String iepluginurl
            = "http://java.sun.com/products/plugin/1.2.2/jinstall-1_2_2-win"
            + ".cab#Version=1,2,2,0";
    protected ClassLoader loader;
    protected org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jsp");
    protected Stack fileSet = new Stack();
    protected String sourceLocation;
    protected Stack realLocation = new Stack();
    protected String workDir;
    protected LinkedList allIncludedFiles = new LinkedList();
    protected TagLibraryValidator validator;
    protected String jspPrefix = "jsp";

    static {
        try {
            booleanWrapper = Class.forName("java.lang.Boolean");
            byteWrapper = Class.forName("java.lang.Byte");
            charWrapper = Class.forName("java.lang.Character");
            doubleWrapper = Class.forName("java.lang.Double");
            intWrapper = Class.forName("java.lang.Integer");
            floatWrapper = Class.forName("java.lang.Float");
            longWrapper = Class.forName("java.lang.Long");
            shortWrapper = Class.forName("java.lang.Short");
            objectWrapper = Class.forName("java.lang.Object");
            stringWrapper = Class.forName("java.lang.String");
        } catch (Exception ex) {
            org.apache.log4j.Category.getInstance("jsp").error("Cannot init JspParser!", ex);
        }
    }

    public abstract void parse() throws JspException, IOException;

    protected abstract void parseToPreservlet() throws JspException, IOException;

    protected abstract void findEncoding() throws JspException, IOException;

    public static AbstractJspParser getParser(String sourceLocation, String workDir, String contextName, String fileName, Hashtable jspTags,
                                              String contextBase, TagLibraryRepository repository, ClassLoader loader) throws IOException {

        InputStream is = new FileInputStream(sourceLocation);
        JspFileReader tempReader = new JspFileReader(is, "ISO8859_1");
        if (tempReader.readUntil("<jsp:root", new StringBuffer(), false)) {
            return new JspAsXmlParser(sourceLocation, workDir, contextName, fileName, jspTags, contextBase, repository, loader);
        } else {
            JspParser parser =  new JspParser(sourceLocation, workDir, contextName, fileName, jspTags, contextBase, repository, loader);
            return parser;
        }
    }

    public void setTagCounter(int counter) {
        this.tagCounter = counter;
    }

    public void setSetterCounter(int counter) {
        this.setterCounter = counter;
    }

    public int getTagCounter() {
        return tagCounter;
    }

    public int getSetterCounter() {
        return setterCounter;
    }

    protected void setPreServlet(PreServlet preServ) {
        this.preServ = preServ;
    }

    protected void addIncludedFiles(Stack fileSet) {
        this.fileSet.addAll(fileSet);
    }

    public TagLibraryValidator getValidator() {
        return validator;
    }

    protected void handlePageDirective(JspDirective directive) throws JspException {
        String attrName;
        String attrValue;
        Enumeration enum = directive.getAttributeNames();
        while (enum.hasMoreElements()) {
            attrName = (String) enum.nextElement();
            attrValue = runtimeEvaluate(new StringBuffer(directive.getAttribute(attrName))).toString();
            if (attrName.equals("language")) {
                if (!attrValue.equals("java")) {
                    throw new JspException("Unrecognized language: " + attrValue + ". Only java language supported");
                }
            } else if (attrName.equals("extends")) {
                if (extClass != null) {
                    throw new JspException("Duplicate declaration of \'extends\' attribute in page directive found");
                }
                extClass = attrValue;
                preServ.setExtends(extClass);
            } else if (attrName.equals("import")) {
                StringTokenizer tokenizer = new StringTokenizer(attrValue, ",");
                while (tokenizer.hasMoreElements()) {
                    preServ.add2Imports(tokenizer.nextToken().trim());
                }
            } else if (attrName.equals("session")) {
                if (sessionEnabled != null) {
                    throw new JspException("Duplicate declaration of \'session\' attribute in page directive found");
                }
                if (attrValue.equals("true")) {
                    preServ.setNeedSession(true);
                } else if (attrValue.equals("false")) {
                    preServ.setNeedSession(false);
                } else {
                    throw new JspException("Unrecognized session attribute value: " + attrValue + " in page directive");
                }
                sessionEnabled = attrValue;
            } else if (attrName.equals("buffer")) {
                if (bufferSize > -1) {
                    throw new JspException("Duplicate declaration of \'buffer\' attribute in page directive found");
                }
// Modified by skilz...
                String tempAttrValue;
                if (attrValue.equals("none") || attrValue.equals("0") || attrValue.equals("0b") || attrValue.equals("0kb") || attrValue.equals("0mb")) {
                    if (autoFlush != null && autoFlush.trim().toLowerCase().equals("false")) {
                        throw new JspException("Illegal combination of buffer=\"none\"(\"0\") and autoFlush=\"false\"");
                    }
                    bufferSize = 0;
                    preServ.setBufferSize(0);
                } else {
                    try {
                        if (!attrValue.equals("0")) {
                            if (!(attrValue.substring(attrValue.length() - 1).equals("b") ||
                                    attrValue.substring(attrValue.length() - 1).equals("k") ||
                                    attrValue.substring(attrValue.length() - 1).equals("m"))) {
                                bufferSize = 1024 * Integer.parseInt(attrValue);
                                preServ.setBufferSize(bufferSize);
                            } else {
                                if (attrValue.substring(attrValue.length() - 2).equals("kb")) {
                                    tempAttrValue = attrValue.substring(0, attrValue.length() - 2);
                                    bufferSize = 1024 * Integer.parseInt(attrValue.substring(0, attrValue.length() - 2));
                                    preServ.setBufferSize(bufferSize);
                                } else {
                                    if (attrValue.substring(attrValue.length() - 2).equals("mb")) {
                                        tempAttrValue = attrValue.substring(0, attrValue.length() - 2);
                                        bufferSize = 1048576 * Integer.parseInt(attrValue.substring(0, attrValue.length() - 2));
                                        preServ.setBufferSize(bufferSize);
                                    }
                                }
                                if (attrValue.substring(attrValue.length() - 1).equals("k")) {
                                    tempAttrValue = attrValue.substring(0, attrValue.length() - 1);
                                    bufferSize = 1024 * Integer.parseInt(attrValue.substring(0, attrValue.length() - 1));
                                    preServ.setBufferSize(bufferSize);
                                } else {
                                    if (attrValue.substring(attrValue.length() - 1).equals("m")) {
                                        tempAttrValue = attrValue.substring(0, attrValue.length() - 1);
                                        bufferSize = 1048576 * Integer.parseInt(attrValue.substring(0, attrValue.length() - 1));
                                        preServ.setBufferSize(bufferSize);
                                    } else {
                                        if (attrValue.substring(attrValue.length() - 1).equals("b")) {
                                            tempAttrValue = attrValue.substring(attrValue.length() - 2, attrValue.length() - 1);
                                            if (!(tempAttrValue.equals("k") || tempAttrValue.equals("m"))) {
                                                bufferSize = Integer.parseInt(attrValue.substring(0, attrValue.length() - 1));
                                                preServ.setBufferSize(bufferSize);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        throw new JspException("Unrecognized value of \'buffer\' attribute in page directive: " + attrValue);
                    }
                }
            } else if (attrName.equals("autoFlush")) {
                if (autoFlush != null) {
                    throw new JspException("Duplicate declaration of \'autoFlush\' attribute in page directive found");
                }
                if (attrValue.equals("true")) {
                    preServ.setAutoFlush(true);
                } else if (attrValue.equals("false")) {
                    if (bufferSize == 0) {
                        throw new JspException("Illegal combination of buffer=\"none\"(\"0\") and autoFlush=\"false\"");
                    }
                    preServ.setAutoFlush(false);
                } else {
                    throw new JspException("Unrecognized autoFlush attribute value: " + attrValue + " in page directive");
                }
                autoFlush = attrValue;
            } else if (attrName.equals("isThreadSafe")) {
                if (threadSafe != null) {
                    throw new JspException("Duplicate declaration of \'isThreadSafe\' attribute in page directive found");
                }
                if (attrValue.equals("true")) {
                    preServ.setIsThreadSafe(true);
                } else if (attrValue.equals("false")) {
                    preServ.setIsThreadSafe(false);
                } else {
                    throw new JspException("Unrecognized isThreadSafe attribute value: " + attrValue + " in page directive");
                }
                threadSafe = attrValue;
            } else if (attrName.equals("info")) {
                if (info != null) {
                    throw new JspException("Duplicate declaration of \'info\' attribute in page directive found");
                }
                info = attrValue;
                preServ.setInfo(info);
            } else if (attrName.equals("errorPage")) {
                if (errorPage != null) {
                    throw new JspException("Duplicate declaration of \'errorPage\' attribute in page directive found");
                }
                preServ.setErrorPage(attrValue);
                errorPage = attrValue;
            } else if (attrName.equals("isErrorPage")) {
                if (isErrorPage != null) {
                    throw new JspException("Duplicate declaration of \'isErrorPage\' attribute in page directive found");
                }
                if (attrValue.equals("true")) {
                    preServ.setIsErrorPage(true);
                } else if (attrValue.equals("false")) {
                    preServ.setIsErrorPage(false);
                } else {
                    throw new JspException("Unrecognized isErrorPage attribute value: " + attrValue + " in page directive");
                }
                isErrorPage = attrValue;
            } else if (attrName.equals("contentType")) {
                if (contentType != null) {
                    throw new JspException("Duplicate declaration of \'contentType\' attribute in page directive found");
                }
                contentType = attrValue;
//        if (contentType.lastIndexOf('=') != -1) {
//          encoding = contentType.substring(contentType.lastIndexOf('=')+1, contentType.length());
//          contentType = contentType.substring(0, contentType.indexOf(';'));
//          preServ.setEncoding(encoding);
//        }
                preServ.setContentType(contentType);
//      preServ.setContentType(contentType+";charset="+encoding);
            } else if (attrName.equals("pageEncoding")) {
                if (pageEncoding != null) {
                    throw new JspException("Duplicate declaration of \'pageEncoding\' attribute in page directive found");
                }
                pageEncoding = attrValue;
//        preServ.setEncoding(pageEncoding);
            } else {
                throw new JspException("Unrecognized attribute " + attrName + " in page directive");
            }
        }
    }

    protected void handleIncludeDirective(JspDirective directive) throws JspException {
        Enumeration enum = directive.getAttributeNames();
        String attrName = (String) enum.nextElement();
        String includeName = directive.getAttribute(attrName);
        if (!attrName.equals("file")) {
            throw new JspException("Unrecognized attribute " + attrName + " in include directive");
        }
        if (enum.hasMoreElements()) {
            throw new JspException("More than one attribute found in include directive");
        }
        String src = null;
        try {
            if (includeName.startsWith("/")) {
                src = contextBase;
            } else {
                src = sourceLocation.substring(0, sourceLocation.lastIndexOf(File.separator) + 1);
            }
            File lastInclude = new File(src, includeName);
            if (fileSet.search(lastInclude) != -1) {
                throw new JspException("Cycled include of jsp " + lastInclude.getName() + " found");
            } else {
                fileSet.push(lastInclude);
            }
            realLocation.push(sourceLocation);
            sourceLocation = lastInclude.getAbsolutePath();
            JspInvocationCacheElement el = new JspInvocationCacheElement();
            el.realFile = new File(sourceLocation);
            el.lastAccess = el.realFile.lastModified();
            allIncludedFiles.add(el);
            AbstractJspParser parser = AbstractJspParser.getParser(sourceLocation, workDir, contextName, fileName, jspTags, contextBase, repository, loader);
            if (parser instanceof JspParser && getTFind() != null) {
                parser.addTFind(getTFind(), getTaglibFind());
            }
            Enumeration enumeration = prefix2lib.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                parser.prefix2lib.put(key, prefix2lib.get(key));
            }

            parser.setSetterCounter(setterCounter + 1);
            parser.setTagCounter(tagCounter + 1);
            parser.setPreServlet(preServ);
            parser.addIncludedFiles(fileSet);
            parser.findEncoding();
            if (parser.getEncoding().equals("ISO8859_1")) {
                parser.encoding = encoding;
            }
            parser.parseToPreservlet();
            allIncludedFiles.addAll(parser.getAllIncludedFiles());
            sourceLocation = (String) realLocation.pop();
            try {
                fileSet.pop();
            } catch (EmptyStackException e) {
            }
            tagCounter = parser.getTagCounter() + 1;
            setterCounter = parser.getSetterCounter() + 1;
            if (parser instanceof JspParser) {
                addTFind(((JspParser)parser).tFind, ((JspParser)parser).taglibFind);
            }
            enumeration = parser.prefix2lib.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                this.prefix2lib.put(key, parser.prefix2lib.get(key));
            }
        } catch (Exception ex) {
            //Logger.log(ex, 1);
            cat.error("Error occured while including " + sourceLocation + ", nested: ", ex);
            throw new JspException("Error occured while including " + sourceLocation + ", nested: " + ex);
        }
    }

    public LinkedList getAllIncludedFiles() {
        return allIncludedFiles;
    }

    protected abstract void addTFind(char[][] tFind, int[][] taglibFind);

    protected abstract char[][] getTFind();
    protected abstract int[][] getTaglibFind();



    /**
     * This method handle taglib directive from jsp page
     * @param directive JspDirective object that represents directive
     * @exception JspException if any error occures while handling
     */
    protected void handleTaglibDirective(JspDirective directive)
            throws JspException {
        String uri = null;
        String prefix = null;
        String attr;
        Enumeration enum = directive.getAttributeNames();
        while (enum.hasMoreElements()) {
            attr = (String) enum.nextElement();
            if (attr.equals("uri")) {
                if (uri == null) {
                    uri = directive.getAttribute(attr);
                } else {
                    throw new JspException("Duplicate uri attribute in taglib "
                            + "directive");
                }
            } else if (attr.equals("prefix")) {
                if (prefix == null) {
                    prefix = directive.getAttribute(attr);
                } else {
                    throw new JspException("Duplicate prefix attribute in taglib "
                            + "directive");
                }
            } else {
                throw new JspException("Unrecognized attribute " + attr + " in "
                        + "taglib directive");
            }
        }
        if (prefix == null || uri == null) {
            throw new JspException("Cannot find required attribute in taglib"
                    + " directive");
        }
        if (prefix.equals("jsp") || prefix.equals("jspx") || prefix.equals("java")
                || prefix.equals("javax") || prefix.equals("servlet")
                || prefix.equals("sun") || prefix.equals("sunw")) {
            throw new JspException("Prefix " + prefix + " is reserved");
        }
        if (prefix.trim().length() == 0) {
            throw new JspException("It is not possible to use empty prefix");
        }
        if (prefix2lib.containsKey(prefix)) {
            throw new JspException("Tag library with prefix \"" + prefix +
                    "\" already defined in this jsp page");
        }
        String tmpUri = "";
        if (!uri.startsWith("/")) {
            tmpUri = sourceLocation.substring(contextBase.length());
            tmpUri = tmpUri.substring(0, tmpUri.lastIndexOf('\\') + 1);
        }
//      TagLibrary tagLib = repository.getLibrary(prefix, uri, tmpUri);
        TagLibrary tagLib = getTagLibrary(prefix, uri, tmpUri);
        validate(prefix, uri, tagLib);
        if (tagLib == null) {
            throw new JspException("Cannot find taglib " + uri);
        }
        prefix2lib.put(prefix, tagLib);
    }

    protected abstract void validate(String prefix, String uri,
                                     TagLibrary tagLib) throws JspException;

    protected abstract TagLibrary getTagLibrary(String prefix, String uri,
                                                String tmpUri)
            throws JspException;

    protected void doStartTag(String prefix, String actName, JspAction action)
            throws JspException {
        if (prefix.equals(jspPrefix)) {
            if (actName.equals("useBean")) {
                handleUseBeanStart(action);
            } else if (actName.equals("setProperty")) {
                handleSetPropertyStart(action);
            } else if (actName.equals("getProperty")) {
                handleGetPropertyStart(action);
            } else if (actName.equals("include")) {
                handleIncludeStart(action);
            } else if (actName.equals("forward")) {
                handleForwardStart(action);
            } else if (actName.equals("param")) {
                handleParamStart(action);
            } else if (actName.equals("plugin")) {
                handlePluginStart(action);
            } else if (actName.equals("params")) {
                handleParamsStart(action);
            } else if (actName.equals("fallback")) {
                handleFallbackStart(action);
            } else {
                throw new JspException("Standart action jsp:" + actName + " does "
                        + "not exist");
            }
            return;
        }
        TagLibrary tagLib = (TagLibrary) prefix2lib.get(prefix);
        if (tagLib == null) {
            throw new JspException("Cannot find declared tag library with prefix "
                    + prefix);
        }
        TagInfo tinfo = tagLib.getTag(actName);
        if (tinfo == null) {
            throw new JspException("Cannot find tag " + actName + " in taglib with"
                    + " prefix " + prefix);
        }
        TagAttributeInfo[] tais = tinfo.getAttributes();
        int acount = 0;
        String attrVal;
        for (int i = 0; i < tais.length; i++) {
            attrVal = action.getAttribute(tais[i].getName());
            if (attrVal == null) {
                if (tais[i].isRequired()) {
                    throw new JspException("Cannot find required attribute "
                            + tais[i].getName() + " in tag " + prefix + ":"
                            + actName);
                }
            } else {
                acount++;
                if (isRuntime(attrVal.trim()) && !tais[i].canBeRequestTime()) {
                    throw new JspException("Attribute " + tais[i].getName() + " in tag "
                            + prefix + ":" + actName
                            + " cannot be request-time");
                }
            }
        }
        TagExtraInfo tei = tinfo.getTagExtraInfo();
        boolean tryCatchFinaly = false;
        VariableInfo[] vais = null;
        TagVariableInfo[] tvais = tinfo.getTagVariableInfos();
        if (tei != null) {
            Hashtable attributes = new Hashtable();
            Enumeration enum = action.attributes.keys();
            while (enum.hasMoreElements()) {
                String key = (String) enum.nextElement();
                String val = (String) action.getAttribute(key);
                if (isRuntime(val))
                    attributes.put(key, TagData.REQUEST_TIME_VALUE);
                else
                    attributes.put(key, val);
            }
            TagData tdata = new TagData(attributes);
            if (!tei.isValid(tdata)) {
                throw new JspException("Invalid tag data for tag "
                        + prefix + ":" + actName);
            }
            vais = tei.getVariableInfo(tdata);
        }
        if ((vais != null && vais.length > 0)
                && (tvais != null && tvais.length > 0))
            throw new JspException("It is not possible for a tag that has one or "
                    + "more variable subelements to have a "
                    + "TagExtraInfo class that returns a non-null"
                    + " object.");
        boolean isBodyTag = false;
        boolean isIteration = false;
        try {
            Class cl = loader.loadClass(tinfo.getTagClassName());
            Class bcl = loader.loadClass("javax.servlet.jsp.tagext.BodyTag");
            Class tcfcl = loader.loadClass("javax.servlet.jsp.tagext.TryCatchFinally");
            Class icl = loader.loadClass("javax.servlet.jsp.tagext.IterationTag");
            isBodyTag = bcl.isAssignableFrom(cl);
            tryCatchFinaly = tcfcl.isAssignableFrom(cl);
            isIteration = icl.isAssignableFrom(cl);
        } catch (Exception ex) {
            throw new JspException("Cannot load " + tinfo.getTagClassName()
                    + ", nested: " + ex);
        }
        String name = "_jspTag" + (tagCounter);
        preServ.addDeclaredTag(tinfo.getTagClassName(), name);
        preServ.add2Service("      " + name + " = (" + tinfo.getTagClassName()
                + ")_jsp_TagFactory.getCustomTag(\"" + tagLib.getURI()
                + "\",\"" + actName + "\");\r\n");
        preServ.add2Service("      " + name + ".setPageContext(pageContext);\r\n");
        String parent = "null";
        try {
            AbstractJspParser.CloseTagInfo cti =
                    (AbstractJspParser.CloseTagInfo) closeTags.peek();
            parent = "_jspTag" + cti.counterValue;
        } catch (EmptyStackException ese) {
        }
        preServ.add2Service("      " + name + ".setParent(" + parent + ");\r\n");
        AbstractJspParser.CloseTagInfo cti =
                new AbstractJspParser.CloseTagInfo(prefix, actName);
        cti.counterValue = tagCounter;
        cti.vais = vais;
        cti.tvais = tvais;
        cti.isIteration = isIteration;
        cti.isBodyTag = isBodyTag;
        cti.tryCatchFinally = tryCatchFinaly;
        cti.action = action;
        closeTags.push(cti);
        Enumeration enum = action.getAttributeNames();
        String attName;
        TagAttributeInfo tai = null;
        while (enum.hasMoreElements()) {
            attName = (String) enum.nextElement();
            tai = null;
            for (int i = 0; i < tais.length; i++) {
                if (tais[i].getName().equals(attName)) {
                    tai = tais[i];
                    break;
                }
            }
//      if (tai == null && !attName.equals("id") && !attName.equals("scope")) {
            if (tai == null) {
                throw new JspException("Non-declared attribute " + attName
                        + " found in tag " + prefix + ":" + actName);
            }
            if (isRuntime(action.getAttribute(attName.trim()))) {
                preServ.add2Service("      " + name + "."
                        + ((RealTagAttributeInfo) tai).getSetter() + "("
                        + "(" + tai.getTypeName() + ")"
                        + (runtimeEvaluate(new StringBuffer(evaluateParameter(action.getAttribute(attName))))).toString() + ");\r\n");
            } else {
                boolean type = (tai.getTypeName().equals("int") ||
                        tai.getTypeName().equals("float") ||
                        tai.getTypeName().equals("boolean") ||
                        tai.getTypeName().equals("double") ||
                        tai.getTypeName().equals("byte") ||
                        tai.getTypeName().equals("long") ||
                        tai.getTypeName().equals("short"));
                if (type) {
                    preServ.add2Service("      " + name + "."
                            + ((RealTagAttributeInfo) tai).getSetter()
                            + "(" + evaluateParameter(action.getAttribute(attName)) + ");\r\n");
                } else {
                    if (tai.getTypeName().equals("char")) {
                        preServ.add2Service("      " + name + "."
                                + ((RealTagAttributeInfo) tai).getSetter()
                                + "(\'" + evaluateParameter(action.getAttribute(attName)) + "\');\r\n");
                    } else {
                        preServ.add2Service("      " + name + "."
                                + ((RealTagAttributeInfo) tai).getSetter()
                                + "(new String(\"" + (runtimeEvaluate(new StringBuffer((evaluateParameter(action.getAttribute(attName)))))).toString() + "\"));\r\n");
                    }
                }
            }
        }
        if (tryCatchFinaly) {
            declareVariables(vais, VariableInfo.AT_BEGIN);
            declareVariables(tvais, VariableInfo.AT_BEGIN, action);
            preServ.add2Service("      int _jspTempValue_" + tagCounter + " = -1;\r\n");
            preServ.add2Service("      try {\r\n");
            preServ.add2Service("        _jspTempValue_" + tagCounter + " = " + name
                    + ".doStartTag();\r\n");
        } else {
            preServ.add2Service("        int _jspTempValue_" + tagCounter + " = " + name
                    + ".doStartTag();\r\n");
        }
        generateVariables(vais, VariableInfo.AT_BEGIN, tryCatchFinaly);
        generateVariables(tvais, VariableInfo.AT_BEGIN, action, tryCatchFinaly);
        preServ.add2Service("        switch(_jspTempValue_" + tagCounter
                + ") {\r\n          case Tag.SKIP_BODY:\r\n          "
                + "break;\r\n");
        if (isBodyTag) {
            preServ.add2Service("          case BodyTag.EVAL_BODY_BUFFERED:\r\n");
            preServ.add2Service("            out = pageContext.pushBody();\r\n");
            preServ.add2Service("            " + name
                    + ".setBodyContent((BodyContent)out);\r\n");
            preServ.add2Service("            " + name + ".doInitBody();\r\n");
        }
        if (isIteration) {
            preServ.add2Service("          case BodyTag.EVAL_BODY_INCLUDE:\r\n");
            preServ.add2Service("            int _jsp_tempDoAfter"
                    + tagCounter + ";\r\n");
            preServ.add2Service("            do {\r\n");
        } else {
            preServ.add2Service("          case BodyTag.EVAL_BODY_INCLUDE:\r\n");
        }
        generateVariables(vais, VariableInfo.NESTED, tryCatchFinaly);
        generateVariables(tvais, VariableInfo.NESTED, action, tryCatchFinaly);
        if (tinfo.getBodyContent().equalsIgnoreCase("tagdependent")) {
            tagdepend = true;
        } else if (tinfo.getBodyContent().equalsIgnoreCase("empty")) {
            empty = true;
        }
        if (!action.hasBody()) {
            doEndTag(prefix, actName);
        }
        preServ.setUseRawStream(false);
        tagCounter++;
    }

    protected void doEndTag(String prefix, String actName) throws JspException {
        AbstractJspParser.CloseTagInfo cti = null;
        tagdepend = false;
        empty = false;
        try {
            cti = (AbstractJspParser.CloseTagInfo) closeTags.pop();
        } catch (EmptyStackException ese) {
            throw new JspException("Start tag for close tag " + prefix + ":" + actName + " not found");
        }
        if (!(cti.prefix.equals(prefix) && cti.actName.equals(actName))) {
            throw new JspException("Found close tag for " + prefix + ":" + actName + ", when expected close tag for " + cti.prefix + ":" + cti.actName);
        }
        if (prefix.equals(jspPrefix)) {
            if (actName.equals("useBean")) {
            } else if (actName.equals("setProperty")) {
                throw new JspException("Unexpected end of tag jsp:setProperty");
            } else if (actName.equals("getProperty")) {
                throw new JspException("Unexpected end of tag jsp:getProperty");
            } else if (actName.equals("include")) {
                handleIncludeEnd();
            } else if (actName.equals("forward")) {
                handleForwardEnd();
            } else if (actName.equals("param")) {
                throw new JspException("Unexpected end of tag jsp:param");
            } else if (actName.equals("plugin")) {
                handlePluginEnd();
            } else if (actName.equals("params")) {
                handleParamsEnd();
            } else if (actName.equals("fallback")) {
                handleFallbackEnd();
            } else {
                throw new JspException("Standart action jsp:" + actName + " does not exist");
            }
            return;
        }
        if (cti.isIteration) {
            preServ.add2Service("            } while((_jsp_tempDoAfter" + cti.counterValue + " = _jspTag" + cti.counterValue + ".doAfterBody()) == IterationTag.EVAL_BODY_AGAIN);\r\n");
        }
        if (cti.isBodyTag) {
//        preServ.add2Service("            } while((_jsp_tempDoAfter"+cti.counterValue+" = _jspTag"+cti.counterValue+".doAfterBody()) == BodyTag.EVAL_BODY_AGAIN);\r\n");
            preServ.add2Service("            out = pageContext.popBody();\r\n");
            preServ.add2Service("            if (_jsp_tempDoAfter" + cti.counterValue + " != Tag.SKIP_BODY) {\r\n");
            preServ.add2Service("              throw new JspException(\"Unrecognized return value of \"+_jspTag" + cti.counterValue + ".getClass().getName()+\".doAfterBody() :\"+_jspTempValue_" + cti.counterValue + ");\r\n");
            preServ.add2Service("            }\r\n");
        }
        preServ.add2Service("            break;\r\n");
        preServ.add2Service("          default:\r\n");
        preServ.add2Service("            throw new JspException(\"Unrecognized return value of \"+_jspTag" + cti.counterValue + ".getClass().getName()+\".doStartTag() :\"+_jspTempValue_" + cti.counterValue + ");\r\n");
        preServ.add2Service("        }\r\n");
        preServ.add2Service("        _jspTempValue_" + cti.counterValue + " = _jspTag" + cti.counterValue + ".doEndTag();\r\n");
        preServ.add2Service("        switch(_jspTempValue_" + cti.counterValue + ") {\r\n");
        preServ.add2Service("          case Tag.SKIP_PAGE:\r\n");
        preServ.add2Service("            return;\r\n");
        preServ.add2Service("          case Tag.EVAL_PAGE:\r\n");
        preServ.add2Service("            break;\r\n");
        preServ.add2Service("          default:\r\n");
        preServ.add2Service("            throw new JspException(\"Unrecognized return value of \"+_jspTag" + cti.counterValue + ".getClass().getName()+\".doEndTag() :\"+_jspTempValue_" + cti.counterValue + ");\r\n");
        preServ.add2Service("        }\r\n");
        if (cti.tryCatchFinally) {
            preServ.add2Service("      } catch(Throwable t) {\r\n");
            preServ.add2Service("        _jspTag" + cti.counterValue + ".doCatch(t);\r\n");
            preServ.add2Service("      } finally {\r\n");
            preServ.add2Service("        _jspTag" + cti.counterValue + ".doFinally();\r\n");
            preServ.add2Service("      }\r\n");
        }
        generateVariables(cti.vais, VariableInfo.AT_END, cti.tryCatchFinally);
        generateVariables(cti.tvais, VariableInfo.AT_END, cti.action, cti.tryCatchFinally);

//    if (preServ.getTagDeclarations().size() > 0) {
//      Enumeration enum = preServ.getTagDeclarations().keys();
//      String varName;
//      while(enum.hasMoreElements()) {
//        varName = (String)enum.nextElement();
//        preServ.add2Service("      if ("+varName+"!=null) {\r\n        "+varName+".release();\r\n      }");
//      }
//    }


//preServ.add2Service("      if (_jspTag"+cti.counterValue+"!=null) {\r\n        _jspTag"+cti.counterValue+".release();\r\n      }");
        preServ.add2Service("      if (_jspTag" + cti.counterValue + "!=null) {\r\n        _jspTag" + cti.counterValue + ".release();\r\n");
        preServ.add2Service("        _jsp_TagFactory.returnToPool(\"" + ((TagLibrary) prefix2lib.get(prefix)).getURI() + "\", \"" + actName + "\", _jspTag" + cti.counterValue + ");\r\n      }\r\n");
        if (closeTags.isEmpty()) {
            preServ.setUseRawStream(true);
        }
    }

    protected abstract String evaluateParameter(String param) throws JspException;

    protected abstract StringBuffer runtimeEvaluate(StringBuffer buf);

    /**
     * This method handles standart jsp:useBean action and generate content deals
     * with loading bean to PreServlet.
     * @param action JspAction that represent jsp:useBean action.
     * @exception JspException Throws if any errors occures while handling.
     */
    protected void handleUseBeanStart(JspAction action) throws JspException {
        String id = null;
        String scope = "page";
        String className = null;
        String type = null;
        String beanName = null;
        Enumeration enum = action.getAttributeNames();
        String name;
        while (enum.hasMoreElements()) {
            name = (String) enum.nextElement();
            if (name.equals("id")) {
                id = action.getAttribute(name);
            } else if (name.equals("scope")) {
                scope = action.getAttribute(name);
            } else if (name.equals("class")) {
                className = action.getAttribute(name);
            } else if (name.equals("beanName")) {
                beanName = action.getAttribute(name);
            } else if (name.equals("type")) {
                type = action.getAttribute(name);
            } else {
                throw new JspException("Unrecognized attribute " + name
                        + " in jsp:useBean action");
            }
        }
        if (id == null) {
            throw new JspException("Required attribute \'id\' not found in jsp:"
                    + "useBean action");
        }
        if (className != null && beanName != null) {
            throw new JspException("Illegal pair of attributes beanName and class "
                    + "in jsp:useBean action");
        }
        if (className == null && type == null) {
            throw new JspException("At least one of attributes \'type\' or "
                    + "\'class\' must be present in jsp:useBean "
                    + "action");
        }
        String scopeInt = null;
        if (scope.equals("page")) {
            scopeInt = "PageContext.PAGE_SCOPE";
        } else if (scope.equals("request")) {
            scopeInt = "PageContext.REQUEST_SCOPE";
        } else if (scope.equals("session")) {
            scopeInt = "PageContext.SESSION_SCOPE";
        } else if (scope.equals("application")) {
            scopeInt = "PageContext.APPLICATION_SCOPE";
        } else {
            throw new JspException("Unrecognized scope \'" + scope
                    + "\' in jsp:useBean action");
        }
        if (type == null) {
            preServ.add2Service("      " + className + " " + id + " = (" + className);
        } else {
            preServ.add2Service("      " + type + " " + id + " = (" + type);
        }
        preServ.add2Service(")pageContext.getAttribute(\"" + id + "\", "
                + scopeInt + ");\r\n");
        preServ.add2Service("      if (" + id + " == null) {\r\n");
        if (className == null && beanName == null) {
            preServ.add2Service("        throw new InstantiationException(\"Either"
                    + " type or class name must be present in "
                    + "jsp:useBean action\");\r\n");
        } else if (className != null) {
            preServ.add2Service("        " + id + " = (" + className + ")Class.forName(\""
                    + className + "\").newInstance();\r\n");
            preServ.add2Service("        pageContext.setAttribute(\"" + id + "\", "
                    + id + ", " + scopeInt + ");\r\n");
        } else {
            if (isRuntime(beanName.trim())) {
                beanName = evaluateParameter(beanName);
                preServ.add2Service("        " + id + " = (" + type + ")java.beans.Beans."
                        + "instantiate(Class.forName(\"" + type
                        + "\").getClassLoader(), " + beanName + ");\r\n");
            } else {
                preServ.add2Service("        " + id + " = (" + type
                        + ")java.beans.Beans.instantiate(Class.forName(\""
                        + type + "\").getClassLoader(), \""
                        + beanName + "\");\r\n");
            }
            preServ.add2Service("        pageContext.setAttribute(\"" + id + "\", "
                    + id + ", " + scopeInt + ");\r\n");
        }
        preServ.add2Service("      }\r\n");
        if (action.hasBody()) {
            closeTags.push(new AbstractJspParser.CloseTagInfo("jsp", "useBean"));
        }
        try {
            Class cl = loader.loadClass(className == null ? type : className);
            BeanInfo beanInfo = Introspector.getBeanInfo(cl);
            availBeans.put(id, beanInfo);
            availBeansClasses.put(id, className == null ? type : className);
        } catch (Throwable ex) {
            throw new JspException("Cannot introspect bean "
                    + (className == null ? beanName : className)
                    + ", nested: " + ex);
        }
    }

    protected void handleSetPropertyStart(JspAction action) throws JspException {
        if (action.hasBody()) {
            throw new JspException("jsp:setProperty cannot have body");
        }
        String beanName = null;
        String propertyName = null;
        String param = null;
        String value = null;
        Enumeration enum = action.getAttributeNames();
        String name;
        while (enum.hasMoreElements()) {
            name = (String) enum.nextElement();
            if (name.equals("name")) {
                beanName = action.getAttribute(name);
            } else if (name.equals("property")) {
                propertyName = action.getAttribute(name);
            } else if (name.equals("param")) {
                param = action.getAttribute(name);
            } else if (name.equals("value")) {
                value = runtimeEvaluate(new StringBuffer(action.getAttribute(name))).toString();
            } else {
                throw new JspException("Unrecognized attribute " + name + " in jsp:setProperty action");
            }
        }
        if (beanName == null) {
            throw new JspException("Cannot find required attribute \'name\' in jsp:setProperty");
        }
        if (propertyName == null) {
            throw new JspException("Cannot find required attribute \'property\' in jsp:setProperty");
        }
        if (param != null && value != null) {
            throw new JspException("Illegal pair of attributes param and value in jsp:setProperty action");
        }
        BeanInfo bInfo = (BeanInfo) availBeans.get(beanName);
        if (bInfo == null) {
            throw new JspException("Bean " + beanName + " not found: cannot set property");
        }
        PropertyDescriptor[] pds = bInfo.getPropertyDescriptors();
//Added by skilz...
        boolean exist = false;
        for (int i = 0; i < pds.length; i++) {
            if (pds[i].getDisplayName().equals(propertyName)) {
                exist = true;
                break;
            }
        }
        if (!(exist || propertyName.equals("*"))) {
            throw new JspException("Cannot find property " + propertyName + " in bean " + beanName);
        }
//Commented by skilz...
//    if (pds == null) {
//      throw new JspException("Cannot find property "+propertyName+" in bean "+beanName);
//    }
        Method setMethod = null;
        String varName = "_jspSetterValue" + setterCounter;
        boolean setterFound = false;
        if (propertyName.equals("*")) {
            setterFound = true;
            if (param != null || value != null) {
                throw new JspException("Param and value attributes not allowed when property is *");
            }
            String tmpProp = null;
            preServ.add2Service("      String[] " + varName + " = null;\r\n");
            for (int i = 0; i < pds.length; i++) {
                setMethod = pds[i].getWriteMethod();
                if (setMethod != null) {
                    preServ.add2Service("      " + varName + " = request.getParameterValues(\"" + pds[i].getName() + "\");\r\n");
                    preServ.add2Service("      if(" + varName + " != null && " + varName + ".length > 0) {\r\n");
                    preServ.add2Service("        boolean change = false;\r\n");
                    preServ.add2Service("        for(int i=0; i<" + varName + ".length; i++) {\r\n");
                    preServ.add2Service("          if (!" + varName + "[i].equals(\"\")) change=true;\r\n");
                    preServ.add2Service("        }\r\n");
                    preServ.add2Service("        if (change) {\r\n  ");
                    generateParamSetter(beanName, varName, pds[i]);
                    preServ.add2Service("        }\r\n");
                    preServ.add2Service("      }");
                }
            }
            setterCounter++;
        } else {
            if (param != null) {
                for (int i = 0; i < pds.length; i++) {
                    if (pds[i].getName().equals(propertyName)) {
                        setMethod = pds[i].getWriteMethod();
                        if (setMethod != null) {
                            preServ.add2Service("      String[] " + varName + " = request.getParameterValues(\"" + param + "\");\r\n");
                            preServ.add2Service("      if(" + varName + " != null && " + varName + ".length > 0) {\r\n");
                            preServ.add2Service("        boolean change = false;\r\n");
                            preServ.add2Service("        for(int i=0; i<" + varName + ".length; i++) {\r\n");
                            preServ.add2Service("          if (!" + varName + "[i].equals(\"\")) change=true;\r\n");
                            preServ.add2Service("        }\r\n");
                            preServ.add2Service("        if (change) {\r\n  ");
                            generateParamSetter(beanName, varName, pds[i]);
                            preServ.add2Service("        }\r\n");
                            preServ.add2Service("      }");
                            setterCounter++;
                            setterFound = true;
                            break;
                        } else {
                            throw new JspException("Cannot find setter for property " + propertyName + " in bean " + beanName);
                        }
                    }
                }
            } else if (value != null) {
                String genValue = isRuntime(value) ? evaluateParameter(value) : value;
                for (int i = 0; i < pds.length; i++) {
                    if (pds[i].getName().equals(propertyName)) {
                        setMethod = pds[i].getWriteMethod();
                        if (setMethod != null) {
                            if (isRuntime(value)) {
                                preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(" + genValue + ");\r\n");
                            } else {
                                generateConstantSetter(beanName, value, pds[i]);
                            }
                            setterFound = true;
                            break;
                        } else {
                            throw new JspException("Cannot find setter for property " + propertyName + " in bean " + beanName);
                        }
                    }
                }
            } else {
                for (int i = 0; i < pds.length; i++) {
                    if (pds[i].getName().equals(propertyName)) {
                        setMethod = pds[i].getWriteMethod();
                        if (setMethod != null) {
                            preServ.add2Service("      String[] " + varName + " = request.getParameterValues(\"" + pds[i].getName() + "\");\r\n");
                            preServ.add2Service("      if(" + varName + " != null && " + varName + ".length > 0) {\r\n");
                            generateParamSetter(beanName, varName, pds[i]);
                            preServ.add2Service("      }");
                            setterCounter++;
                            setterFound = true;
                            break;
                        } else {
                            throw new JspException("Cannot find setter for property " + propertyName + " in bean " + beanName);
                        }
                    }
                }
            }
            if (!setterFound) {
                throw new JspException("Cannot find setter for property " + propertyName + " in bean " + beanName);
            }
        }
    }

    protected void generateParamSetter(String beanName, String varName, PropertyDescriptor pds) throws JspException {
        Class propType = pds.getPropertyType();
        boolean isArray = propType.isArray();
        if (isArray) {
            propType = propType.getComponentType();
        }
        Method setMethod = pds.getWriteMethod();
        if (setMethod == null) {
            return;
        }

        Class editorClass = pds.getPropertyEditorClass();
        if (editorClass != null) {
            PropertyEditor editor = null;
            try {
                editor = (PropertyEditor) editorClass.newInstance();
                editor.setAsText(varName);
                Object var = editor.getValue();
                preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(" + editor.getJavaInitializationString() + ");\r\n");
            } catch (Exception ex) {
                throw new JspException("Unable to convert string \"" + varName + "\" to class using PropertyEditor, nested : " + ex);
            }
            return;
        }

        if (propType.isAssignableFrom(objectWrapper)) {
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(" + varName + ");\r\n");
        } else if (propType.isAssignableFrom(booleanWrapper)) {
            if (isArray) {
                preServ.add2Service("        Boolean[] " + varName + "_bool = new Boolean[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_bool[_jsp_i] = Boolean.valueOf(" + varName + "[_jsp_i]);\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_bool);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Boolean.valueOf(" + varName + "[0]));\r\n");
            }
        } else if (propType.isAssignableFrom(byteWrapper)) {
            if (isArray) {
                preServ.add2Service("        Byte[] " + varName + "_byte = new Byte[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_byte[_jsp_i] = Byte.valueOf(" + varName + "[_jsp_i]);\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_byte);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Byte.valueOf(" + varName + "[0]));\r\n");
            }
        } else if (propType.isAssignableFrom(charWrapper)) {
            if (isArray) {
                preServ.add2Service("        Character[] " + varName + "_char = new Character[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          if (" + varName + "[_jsp_i] == 1) {\r\n");
                preServ.add2Service("            " + varName + "_char[_jsp_i] = new Character(" + varName + "[_jsp_i].charAt(0));\r\n");
                preServ.add2Service("          } else {\r\n");
                preServ.add2Service("            throw new JspException(\"Cannot convert parameter value \"+" + varName + "[_jsp_i]+\" to char\");\r\n");
                preServ.add2Service("          }\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_char);\r\n");
            } else {
                preServ.add2Service("        if (" + varName + "[0].length() == 1) {\r\n");
                preServ.add2Service("          " + beanName + "." + setMethod.getName() + "(new Character(" + varName + "[0].charAt(0)));\r\n");
                preServ.add2Service("        } else {\r\n");
                preServ.add2Service("          throw new JspException(\"Cannot convert parameter value \"+" + varName + "+\" to char\");\r\n");
                preServ.add2Service("        }\r\n");
            }
        } else if (propType.isAssignableFrom(doubleWrapper)) {
            if (isArray) {
                preServ.add2Service("        Double[] " + varName + "_double = new Double[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_double[_jsp_i] = Double.valueOf(" + varName + "[_jsp_i]);\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_double);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Double.valueOf(" + varName + "[0]));\r\n");
            }
        } else if (propType.isAssignableFrom(intWrapper)) {
            if (isArray) {
                preServ.add2Service("        Integer[] " + varName + "_int = new Integer[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_int[_jsp_i] = Integer.valueOf(" + varName + "[_jsp_i]);\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_int);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Integer.valueOf(" + varName + "[0]));\r\n");
            }
        } else if (propType.isAssignableFrom(floatWrapper)) {
            if (isArray) {
                preServ.add2Service("        Float[] " + varName + "_float = new Float[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_float[_jsp_i] = Float.valueOf(" + varName + "[_jsp_i]);\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_float);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Float.valueOf(" + varName + "[0]));\r\n");
            }
        } else if (propType.isAssignableFrom(longWrapper)) {
            if (isArray) {
                preServ.add2Service("        Long[] " + varName + "_long = new Long[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_long[_jsp_i] = Long.valueOf(" + varName + "[_jsp_i]);\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_long);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Long.valueOf(" + varName + "[0]));\r\n");
            }
        } else if (propType.isAssignableFrom(shortWrapper)) {
            if (isArray) {
                preServ.add2Service("        Short[] " + varName + "_short = new Short[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_short[_jsp_i] = Short.valueOf(" + varName + "[_jsp_i]);\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_short);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Short.valueOf(" + varName + "[0]));\r\n");
            }
        } else if (propType.isAssignableFrom(Boolean.TYPE)) {
            if (isArray) {
                preServ.add2Service("        boolean[] " + varName + "_bool = new boolean[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_bool[_jsp_i] = Boolean.valueOf(" + varName + "[_jsp_i]).booleanValue();\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_bool);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Boolean.valueOf(" + varName + "[0]).booleanValue());\r\n");
            }
        } else if (propType.isAssignableFrom(Byte.TYPE)) {
            if (isArray) {
                preServ.add2Service("        byte[] " + varName + "_byte = new byte[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_byte[_jsp_i] = Byte.valueOf(" + varName + "[_jsp_i]).byteValue();\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_byte);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Byte.valueOf(" + varName + "[0]).byteValue());\r\n");
            }
        } else if (propType.isAssignableFrom(Character.TYPE)) {
            if (isArray) {
                preServ.add2Service("        char[] " + varName + "_char = new char[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          if (" + varName + "[_jsp_i] == 1) {\r\n");
                preServ.add2Service("            " + varName + "_char[_jsp_i] = " + varName + "[_jsp_i].charAt(0);\r\n");
                preServ.add2Service("          } else {\r\n");
                preServ.add2Service("            throw new JspException(\"Cannot convert parameter value \"+" + varName + "[_jsp_i]+\" to char\");\r\n");
                preServ.add2Service("          }\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_char);\r\n");
            } else {
                preServ.add2Service("        if (" + varName + "[0].length() == 1) {\r\n");
                preServ.add2Service("          " + beanName + "." + setMethod.getName() + "(" + varName + "[0].charAt(0));\r\n");
                preServ.add2Service("        } else {\r\n");
                preServ.add2Service("          throw new JspException(\"Cannot convert parameter value \"+" + varName + "+\" to char\");\r\n");
                preServ.add2Service("        }\r\n");
            }
        } else if (propType.isAssignableFrom(Double.TYPE)) {
            if (isArray) {
                preServ.add2Service("        double[] " + varName + "_double = new double[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_double[_jsp_i] = Double.valueOf(" + varName + "[_jsp_i]).doubleValue();\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_double);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Double.valueOf(" + varName + "[0]).doubleValue());\r\n");
            }
        } else if (propType.isAssignableFrom(Integer.TYPE)) {
            if (isArray) {
                preServ.add2Service("        int[] " + varName + "_int = new int[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_int[_jsp_i] = Integer.valueOf(" + varName + "[_jsp_i]).intValue();\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_int);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Integer.valueOf(" + varName + "[0]).intValue());\r\n");
            }
        } else if (propType.isAssignableFrom(Float.TYPE)) {
            if (isArray) {
                preServ.add2Service("        float[] " + varName + "_float = new float[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_float[_jsp_i] = Float.valueOf(" + varName + "[_jsp_i]).floatValue();\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_float);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Float.valueOf(" + varName + "[0]).floatValue());\r\n");
            }
        } else if (propType.isAssignableFrom(Long.TYPE)) {
            if (isArray) {
                preServ.add2Service("        long[] " + varName + "_long = new long[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_long[_jsp_i] = Long.valueOf(" + varName + "[_jsp_i]).longValue();\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_long);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Long.valueOf(" + varName + "[0]).longValue());\r\n");
            }
        } else if (propType.isAssignableFrom(Short.TYPE)) {
            if (isArray) {
                preServ.add2Service("        short[] " + varName + "_short = new short[" + varName + ".length];\r\n");
                preServ.add2Service("        for (int _jsp_i=0; _jsp_i<" + varName + ".length; _jsp_i++)\r\n{");
                preServ.add2Service("          " + varName + "_short[_jsp_i] = Short.valueOf(" + varName + "[_jsp_i]).shortValue();\r\n        }\r\n");
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "_short);\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(Short.valueOf(" + varName + "[0]).shortValue());\r\n");
            }
        } else if (propType.isAssignableFrom(stringWrapper)) {
            if (isArray) {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + ");\r\n");
            } else {
                preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + varName + "[0]);\r\n");
            }
        } else {
//Added by skilz...
            preServ.add2Service("          throw new JspException(\"Cannot convert java.lang.String to " + propType.getName() + " in " + setMethod.getName() + "() method\");\r\n");
//Commented by skilz...
//      throw new JspException("Cannot convert java.lang.String to "+propType.getName()+" in "+setMethod.getName()+"() method");
        }
    }

    protected void generateConstantSetter(String beanName, String varName, PropertyDescriptor pds) throws JspException {
        Class propType = pds.getPropertyType();
        Method setMethod = pds.getWriteMethod();
        if (setMethod == null) {
            return;
        }

        Class editorClass = pds.getPropertyEditorClass();
        if (editorClass != null) {
            PropertyEditor editor = null;
            try {
                editor = (PropertyEditor) editorClass.newInstance();
                editor.setAsText(varName);
                Object var = editor.getValue();
                preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(" + editor.getJavaInitializationString() + ");\r\n");
            } catch (Exception ex) {
                throw new JspException("Unable to convert string \"" + varName + "\" to class using PropertyEditor, nested : " + ex);
            }
            return;
        }

        if (propType.isAssignableFrom(objectWrapper)) {
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new String(\"" + varName + "\"));\r\n");
        } else if (propType.getName().equals("java.lang.String")) {
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(\"" + varName + "\");\r\n");
        } else if (propType.isAssignableFrom(booleanWrapper)) {
            Boolean b = Boolean.valueOf(varName);
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new Boolean(" + b.toString() + "));\r\n");
        } else if (propType.isAssignableFrom(byteWrapper)) {
            Byte b = null;
            try {
                b = Byte.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to Byte");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new Byte((byte)" + b.toString() + "));\r\n");
        } else if (propType.isAssignableFrom(charWrapper)) {
            if (varName.length() != 1) {
                throw new JspException("Cannot convert " + varName + " to Character: length != 1");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new Character(\'" + varName + "\'));\r\n");
        } else if (propType.isAssignableFrom(doubleWrapper)) {
            Double d = null;
            try {
                d = Double.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to Double");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new Double((double)" + d.toString() + "));\r\n");
        } else if (propType.isAssignableFrom(intWrapper)) {
            Integer i = null;
            try {
                i = Integer.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to Integer");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new Integer((int)" + i.toString() + "));\r\n");
        } else if (propType.isAssignableFrom(floatWrapper)) {
            Float f = null;
            try {
                f = Float.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to Float");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new Float((float)" + f.toString() + "));\r\n");
        } else if (propType.isAssignableFrom(longWrapper)) {
            Long l = null;
            try {
                l = Long.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to Long");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new Long((long)" + l.toString() + "));\r\n");
        } else if (propType.isAssignableFrom(shortWrapper)) {
            Short s = null;
            try {
                s = Short.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to Short");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(new Short((short)" + s.toString() + "));\r\n");
        } else if (propType.isAssignableFrom(Boolean.TYPE)) {
            Boolean b = Boolean.valueOf(varName);
            preServ.add2Service("        " + beanName + "." + setMethod.getName() + "(" + b.toString() + ");\r\n");
        } else if (propType.isAssignableFrom(Byte.TYPE)) {
            Byte b = null;
            try {
                b = Byte.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to byte");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "((byte)" + b.toString() + ");\r\n");
        } else if (propType.isAssignableFrom(Character.TYPE)) {
            if (varName.length() != 1) {
                throw new JspException("Cannot convert " + varName + " to char: length != 1");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(\'" + varName + "\');\r\n");
        } else if (propType.isAssignableFrom(Double.TYPE)) {
            Double d = null;
            try {
                d = Double.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to double");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "((double)" + d.toString() + ");\r\n");
        } else if (propType.isAssignableFrom(Integer.TYPE)) {
            Integer i = null;
            try {
                i = Integer.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to int");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "((int)" + i.toString() + ");\r\n");
        } else if (propType.isAssignableFrom(Float.TYPE)) {
            Float f = null;
            try {
                f = Float.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to float");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "((float)" + f.toString() + ");\r\n");
        } else if (propType.isAssignableFrom(Long.TYPE)) {
            Long l = null;
            try {
                l = Long.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to long");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "((long)" + l.toString() + ");\r\n");
        } else if (propType.isAssignableFrom(Short.TYPE)) {
            Short s = null;
            try {
                s = Short.valueOf(varName);
            } catch (NumberFormatException nex) {
                throw new JspException("Cannot convert " + varName + " to short");
            }
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "((short)" + s.toString() + ");\r\n");
        } else if (propType.isAssignableFrom(stringWrapper)) {
            preServ.add2Service("      " + beanName + "." + setMethod.getName() + "(" + varName + ");\r\n");
        } else {
//Added by skilz...
            preServ.add2Service("          throw new JspException(\"Cannot convert java.lang.String to " + propType.getName() + " in " + setMethod.getName() + "() method\");\r\n");
//Commented by skilz...
//      throw new JspException("Cannot convert java.lang.String to "+propType.getName()+" in "+setMethod.getName()+"() method");
        }
    }

    protected void handleGetPropertyStart(JspAction action) throws JspException {
        if (action.hasBody()) {
            throw new JspException("jsp:getProperty cannot have body");
        }
        String beanName = null;
        String propertyName = null;
        Enumeration enum = action.getAttributeNames();
        String name;
        while (enum.hasMoreElements()) {
            name = (String) enum.nextElement();
            if (name.equals("name")) {
                beanName = action.getAttribute(name);
            } else if (name.equals("property")) {
                propertyName = action.getAttribute(name);
            } else {
                throw new JspException("Unrecognized attribute " + name + " in jsp:getProperty action");
            }
        }
        if (beanName == null) {
            throw new JspException("Cannot find required attribute \'name\' in jsp:getProperty action");
        }
        if (propertyName == null) {
            throw new JspException("Cannot find required attribute \'property\' in jsp:getProperty action");
        }
        BeanInfo bInfo = (BeanInfo) availBeans.get(beanName);
//    preServ.add2Service("      );\r\n");
        preServ.add2Service("      out.print(com.azoft.nusuth.jsp.JspBeanUtils.processGetProperty(\"" + beanName + "\", \"" + propertyName + "\", pageContext.findAttribute(\"" + beanName + "\")));\r\n");
//    if (bInfo == null) {
//      throw new JspException("Bean "+beanName+" not found: cannot get property");
//    }
//    PropertyDescriptor[] pds = bInfo.getPropertyDescriptors();
//Added by skilz...
//    boolean exist = false;
//    for (int i = 0; i < pds.length; i++ ){
//      if (pds[i].getDisplayName().equals(propertyName)) {
//        exist=true;
//        break;
//      }
//    }
//    if (!exist) {
//      throw new JspException("Cannot find property "+propertyName+" in bean "+beanName);
//    }
//Commented by skilz...
//    if (pds == null) {
//      throw new JspException("Cannot find property "+propertyName+" in bean "+beanName);
//    }
/*
  Method getMethod = null;
  for (int i=0; i<pds.length; i++) {
    if (pds[i].getName().equals(propertyName)) {
      getMethod = pds[i].getReadMethod();
      if (getMethod != null) {
        preServ.add2Service("      out.print(String.valueOf("+beanName+"."+getMethod.getName()+"()));\r\n");
        break;
      } else {
        throw new JspException("Cannot find getter for property "+propertyName+" in bean "+beanName);
      }
    }
  }
*/
    }

    protected void handleIncludeStart(JspAction action) throws JspException {
        String pageName = null;
//flush = null;
        Enumeration enum = action.getAttributeNames();
        String name;
        while (enum.hasMoreElements()) {
            name = (String) enum.nextElement();
            if (name.equals("page")) {
                pageName = action.getAttribute(name);
            } else if (name.equals("flush")) {
                flush = action.getAttribute(name);
            } else {
                throw new JspException("Unrecognized attribute " + name + " in jsp:include action");
            }
        }
        if (pageName == null) {
            throw new JspException("Cannot find required attribute \'page\' in jsp:include action");
        }
        if (!(flush.equals("false") || flush.equals("true"))) {
            throw new JspException("Unrecocnized value of attribute flush \"" + flush + "\"");
        }
        int pos = pageName.indexOf("?");
        if (pos == -1) {
            pageUrl = pageName;
        } else {
            pageUrl = pageName.substring(0, pos);
//      queryString = pageName.substring(pos+1, pageName.length());
            queryString = "\"" + pageName.substring(pos, pageName.length()) + "\"";
        }
        if (!action.hasBody()) {
            handleIncludeEnd();
        } else {
            closeTags.push(new AbstractJspParser.CloseTagInfo("jsp", "include"));
            allowedTags.add("param");
        }
    }

    protected void handleIncludeEnd() throws JspException {
        if (pageUrl == null) {
            //Logger.log("FATAL ERROR: Cannot find pageUrl on jsp:include end", 1);
            cat.error("JSP PROCESSING FATAL ERROR: Cannot find pageUrl on jsp:include end");
            throw new JspException("FATAL ERROR: Cannot find pageUrl on jsp:include end");
        }
        pageUrl = isRuntime(pageUrl) ? evaluateParameter(pageUrl) : "\"" + pageUrl + "\"";
        String path = queryString == null ? pageUrl : pageUrl + "+" + queryString;
//      preServ.add2Service("      out.flush();\r\n");
//      preServ.add2Service("      request.getRequestDispatcher("+path+").include(request, response);\r\n");
        preServ.add2Service("      _jsp_nusuthWriter.setFlush(" + flush + ");\r\n");
        preServ.add2Service("      out.flush();\r\n");
        preServ.add2Service("      pageContext.include(" + path + ");\r\n");
        pageUrl = null;
        queryString = null;
        allowedTags.clear();
    }

    protected void handleForwardStart(JspAction action) throws JspException {
        String pageName = null;
        Enumeration enum = action.getAttributeNames();
        String name;
        while (enum.hasMoreElements()) {
            name = (String) enum.nextElement();
            if (name.equals("page")) {
                pageName = action.getAttribute(name);
            } else {
                throw new JspException("Unrecognized attribute " + name + " in jsp:forward action");
            }
        }
        if (pageName == null) {
            throw new JspException("Cannot find required attribute \'page\' in jsp:forward action");
        }
        int pos = pageName.indexOf("?");
        if (pos == -1) {
            pageUrl = pageName;
        } else {
            pageUrl = pageName.substring(0, pos);
//      queryString = pageName.substring(pos+1, pageName.length());
            queryString = "\"" + pageName.substring(pos, pageName.length()) + "\"";
        }
        if (!action.hasBody()) {
            handleForwardEnd();
        } else {
            closeTags.push(new AbstractJspParser.CloseTagInfo("jsp", "forward"));
            allowedTags.add("param");
        }
    }

    protected void handleForwardEnd() throws JspException {
        if (pageUrl == null) {
            //Logger.log("FATAL ERROR: Cannot find pageUrl on jsp:forward end", 1);
            cat.error("JSP PROCESSING FATAL ERROR: Cannot find pageUrl on jsp:forward end");
            throw new JspException("FATAL ERROR: Cannot find pageUrl on jsp:forward end");
        }
        pageUrl = isRuntime(pageUrl) ? evaluateParameter(pageUrl) : "\"" + pageUrl + "\"";
        String path = queryString == null ? pageUrl : pageUrl + "+" + queryString;
//    preServ.add2Service("      out.flush();\r\n");
        preServ.add2Service("      request.getRequestDispatcher(" + path + ").forward(request, response);\r\n");
        pageUrl = null;
        queryString = null;
        allowedTags.clear();
/*
  String path = queryString == null ? pageUrl : pageUrl+"?"+queryString;
//Added by skilz...
  String query;
  if (path.indexOf('?') != -1) {
    if (path.startsWith("<%=")) {
      query = "+\"&\" + request.getQueryString()";
    } else {
      query = "&\" + request.getQueryString()";
    }
  } else {
    if (path.startsWith("<%=")) {
      query = "+\"?\" + request.getQueryString()";
    } else {
      query = "?\" + request.getQueryString()";
    }
  }
//
  if (path.startsWith("<%=")) {
    preServ.add2Service("      request.getRequestDispatcher("+evaluateParameter(path)+query+").forward(request, response);\r\n");
  } else {
    preServ.add2Service("      request.getRequestDispatcher(\""+path+query+").forward(request, response);\r\n");
  }
*/
        pageUrl = null;
        queryString = null;
        allowedTags.clear();
    }

    protected void handleParamStart(JspAction action) throws JspException {
        if (action.hasBody()) {
            throw new JspException("jsp:param cannot have body");
        }
        AbstractJspParser.CloseTagInfo cti = null;
        try {
            cti = (AbstractJspParser.CloseTagInfo) closeTags.peek();
        } catch (EmptyStackException ese) {
            throw new JspException("jsp:param tag allowed only in body of jsp:include, jsp:forward and jsp:params");
        }
        if (!(cti.prefix.equals("jsp") && (cti.actName.equals("include") || cti.actName.equals("forward") || cti.actName.equals("params")))) {
            throw new JspException("jsp:param tag allowed only in body of jsp:include, jsp:forward and jsp:params");
        }
        String paramName = null;
        String paramValue = null;
        Enumeration enum = action.getAttributeNames();
        String name;
        while (enum.hasMoreElements()) {
            name = (String) enum.nextElement();
            if (name.equals("name")) {
                paramName = action.getAttribute(name);
            } else if (name.equals("value")) {
                paramValue = action.getAttribute(name);
            } else {
                throw new JspException("Unrecognized attribute " + name + " in jsp:param action");
            }
        }
        if (paramName == null) {
            throw new JspException("Cannot find required attribute \'name\' in jsp:param action");
        }
        if (paramValue == null) {
            throw new JspException("Cannot find required attribute \'value\' in jsp:param action");
        }
        if (cti.actName.equals("params")) {
            pluginParams.put(paramName, paramValue);
        } else {
            if (queryString == null) {
                queryString = "\"?" + paramName + "=" + (isRuntime(paramValue) ? "\"+" + evaluateParameter(paramValue) : paramValue + "\"");
            } else {
                queryString += ("+\"&" + paramName + "=" + (isRuntime(paramValue) ? "\"+" + evaluateParameter(paramValue) : paramValue + "\""));
            }
        }
    }

    protected void handlePluginStart(JspAction action) throws JspException {
        pluginParams.clear();
        iePluginParams.clear();
        String type = null;
        String code = null;
        String codebase = null;
        String archive = null;
        String jreversion = null;
        String tmpName;
        Enumeration enum = action.getAttributeNames();
        while (enum.hasMoreElements()) {
            tmpName = (String) enum.nextElement();
            if (tmpName.equals("type")) {
                type = (String) action.getAttribute(tmpName);
            } else if (tmpName.equals("code")) {
                code = (String) action.getAttribute(tmpName);
            } else if (tmpName.equals("codebase")) {
                codebase = (String) action.getAttribute(tmpName);
            } else if (tmpName.equals("align") || tmpName.equals("hspace")
                    || tmpName.equals("name") || tmpName.equals("vspace")
                    || tmpName.equals("title")) {
                iePluginParams.put(tmpName, (String) action.getAttribute(tmpName));
            } else if (tmpName.equals("archive")) {
                archive = (String) action.getAttribute(tmpName);
            } else if (tmpName.equals("jreversion")) {
                jreversion = (String) action.getAttribute(tmpName);
            } else if (tmpName.equals("nspluginurl")) {
                nspluginurl = (String) action.getAttribute(tmpName);
            } else if (tmpName.equals("iepluginurl")) {
                iepluginurl = (String) action.getAttribute(tmpName);
            } else if (tmpName.equals("height") || tmpName.equals("width")) {
                String val = (String) action.getAttribute(tmpName);
                if (isRuntime(val)) {
                    val = evaluateParameter(val);
                }
                iePluginParams.put(tmpName, val);
                pluginParams.put(tmpName, val);
            } else {
                throw new JspException("Unrecognized attribute "
                        + tmpName + " in jsp:plugin action");
            }
        }
        if (type == null) {
            throw new JspException("Cannot find required attribute\'type\' "
                    + "in jsp:plugin action");
        }
        if (code == null) {
            throw new JspException("Cannot find required attribute\'code\' "
                    + "in jsp:plugin action");
        }
        if (codebase == null) {
            throw new JspException("Cannot find required attribute\'codebase\' "
                    + "in jsp:plugin action");
        }
        if (!(type.equals("applet") || type.equals("bean"))) {
            throw new JspException("Only applet and bean values allowed in "
                    + "attribute type in jsp:plugin action");
        }
        pluginParams.put("java_code", code);
        pluginParams.put("java_codebase", codebase);
        if (archive != null) {
            pluginParams.put("java_archive", archive);
        }
        pluginParams.put("type", "application/x-java-" + type
                + (jreversion == null ? "" : "; version=" + jreversion));
        allowedTags.add("params");
        allowedTags.add("fallback");
        if (!action.hasBody()) {
            handlePluginEnd();
        } else {
            closeTags.push(new AbstractJspParser.CloseTagInfo("jsp", "plugin"));
        }
    }

    protected void handlePluginEnd() throws JspException {
        preServ.addTemplateText("<object classid=\\\"" + IE_CLSID + "\\\" codebase=\\\""
                + iepluginurl + "\\\"");
        Enumeration enum = iePluginParams.keys();
        String key;
        while (enum.hasMoreElements()) {
            key = (String) enum.nextElement();
            preServ.addTemplateText(" " + key + "=\\\""
                    + (String) iePluginParams.get(key) + "\\\"");
        }
        preServ.addTemplateText(">\\r\\n");
        enum = pluginParams.keys();
        while (enum.hasMoreElements()) {
            key = (String) enum.nextElement();
            preServ.addTemplateText("<param name=\\\"" + key + "\\\" value=\\\"" + (String) pluginParams.get(key) + "\\\">\\r\\n");
        }
        preServ.addTemplateText("<comment>\\r\\n");
        preServ.addTemplateText("<embed codebase=\\\"" + nspluginurl + "\\\"");
        enum = pluginParams.keys();
        while (enum.hasMoreElements()) {
            key = (String) enum.nextElement();
            preServ.addTemplateText(" " + key + "=\\\"" + (String) pluginParams.get(key) + "\\\"");
        }
        enum = iePluginParams.keys();
        while (enum.hasMoreElements()) {
            key = (String) enum.nextElement();
            preServ.addTemplateText(" " + key + "=\\\"" + (String) pluginParams.get(key) + "\\\"");
        }
        preServ.addTemplateText(">");
        if (fallBackText == null) {
            preServ.addTemplateText("\\r\\n</comment>\\r\\n");
        } else {
            preServ.addTemplateText("<noembed>\\r\\n</comment>\\r\\n" + fallBackText + "</noembed>\\r\\n");
        }
        preServ.addTemplateText("</embed></object>\\r\\n");
        allowedTags.clear();
        nspluginurl = "http://java.sun.com/products/plugin/";
        iepluginurl = "http://java.sun.com/products/plugin/1.2.2/jinstall-1_2_2-win.cab#Version=1,2,2,0";
    }

    protected void handleParamsStart(JspAction action) throws JspException {
        AbstractJspParser.CloseTagInfo cti = null;
        try {
            cti = (AbstractJspParser.CloseTagInfo) closeTags.peek();
        } catch (EmptyStackException ese) {
            throw new JspException("jsp:params tag allowed only in body of jsp:plugin");
        }
        if (!(cti.prefix.equals("jsp") && cti.actName.equals("plugin"))) {
            throw new JspException("jsp:params tag allowed only in body of jsp:plugin");
        }
        closeTags.push(new AbstractJspParser.CloseTagInfo("jsp", "params"));
        allowedTags.clear();
        allowedTags.add("param");
    }

    protected void handleParamsEnd() {
        allowedTags.clear();
        allowedTags.add("fallback");
        allowedTags.add("params");
    }

    protected void handleFallbackStart(JspAction action) throws JspException {
        if (action.getAttributeNames().hasMoreElements()) {
            throw new JspException("Standart action jsp:fallback may not have attributes");
        }
        if (!action.hasBody()) {
            return;
        }
        isFallBack = true;
        fallBackText = "";
        closeTags.push(new AbstractJspParser.CloseTagInfo("jsp", "fallback"));
    }

    /**
     * This method declare scripting variables which declared in TagExtraInfo
     * class to PreServlet. This method invoked if and only if Tag implements
     * TryCatchFinally interface and scope of the variable is AT_BEGIN
     * @param vais Array of VariableInfo
     * @param scope Scope of scripting variable
     */
    protected void declareVariables(VariableInfo[] vais, int scope) {
        if (vais != null) {
            for (int i = 0; i < vais.length; i++) {
                if (vais[i].getScope() == scope) {
                    if (scope == VariableInfo.AT_BEGIN) {
                        preServ.add2Service("      ");
                        if (vais[i].getDeclare()) {
                            preServ.add2Service(vais[i].getClassName() + " ");
                            preServ.add2Service(vais[i].getVarName() + " = null;\r\n");
                        }
                    }
                }
            }
        }
    }

    /**
     * This method declare scripting variables which declared in TagExtraInfo
     * class to PreServlet. This method invoked if and only if Tag implements
     * TryCatchFinally interface and scope of the variable is AT_BEGIN
     * @param vais Array of TagVariableInfo
     * @param scope Scope of scripting variable
     * @param action JspAction which represents Tag
     */
    protected void declareVariables(TagVariableInfo[] vais, int scope, JspAction action) {
        if (vais != null) {
            for (int i = 0; i < vais.length; i++) {
                if (vais[i].getScope() == scope) {
                    if (scope == VariableInfo.AT_BEGIN) {
                        preServ.add2Service("      ");
                        if (vais[i].getDeclare()) {
                            preServ.add2Service(vais[i].getClassName() + " ");
                        }
                        boolean nameFromAttr = (vais[i].getNameGiven() == null);
                        String name = (vais[i].getNameGiven() != null ? vais[i].getNameGiven() : vais[i].getNameFromAttribute());
                        if (!nameFromAttr) {
                            preServ.add2Service(name + " = null;\r\n");
                        } else {
                            preServ.add2Service(action.getAttribute(name) + " = null;\r\n");
                        }
                    }
                }
            }
        }
    }

    /**
     * This method generate scripting variables which declared in TagExtraInfo
     * class to PreServlet
     * @param vais Array of VariableInfo
     * @param scope Scope of scripting variable
     * @param trycatch true if Tag implements TryCatchFinally interface
     */
    protected void generateVariables(VariableInfo[] vais, int scope, boolean trycatch) {
        if (vais != null) {
            for (int i = 0; i < vais.length; i++) {
                if (vais[i].getScope() == scope) {
                    preServ.add2Service("      ");
                    if (vais[i].getDeclare()) {
                        if (!(trycatch && scope == VariableInfo.AT_BEGIN)) {
                            preServ.add2Service(vais[i].getClassName() + " ");
                        }
                    }
                    preServ.add2Service(vais[i].getVarName() + " = (" + vais[i].getClassName() + ")pageContext.getAttribute(\"" + vais[i].getVarName() + "\");\r\n");
                }
            }
        }
    }

    /**
     * This method generate scripting variables which declared in tag library
     * descriptor file to PreServlet
     * @param vais Array of TagVariableInfo
     * @param scope Scope of scripting variable
     * @param action JspAction which represent tag
     * @param trycatch true if Tag implements TryCatchFinally interface
     */
    protected void generateVariables(TagVariableInfo[] vais, int scope, JspAction action, boolean trycatch) {
        if (vais != null) {
            for (int i = 0; i < vais.length; i++) {
                if (vais[i].getScope() == scope) {
                    preServ.add2Service("      ");
                    if (vais[i].getDeclare()) {
                        if (!(trycatch && scope == VariableInfo.AT_BEGIN)) {
                            preServ.add2Service(vais[i].getClassName() + " ");
                        }
                    }
                    boolean nameFromAttr = (vais[i].getNameGiven() == null);
                    String name = (vais[i].getNameGiven() != null ? vais[i].getNameGiven() : vais[i].getNameFromAttribute());
                    if (!nameFromAttr) {
                        preServ.add2Service(name + " = (" + vais[i].getClassName() + ")pageContext.getAttribute(\"" + name + "\");\r\n");
                    } else {
                        preServ.add2Service(action.getAttribute(name) + " = (" + vais[i].getClassName() + ")pageContext.getAttribute(\"" + action.getAttribute(name) + "\");\r\n");
                    }
                }
            }
        }
    }

    protected void handleFallbackEnd() {
        isFallBack = false;
    }

    public String getEncoding() {
        return encoding;
    }

    protected abstract boolean isRuntime(String str);

    protected boolean isEmptyTemplate(StringBuffer buf) {
        String str = buf.toString();
        int i = 0;
        while (i < str.length()) {
            if ((str.charAt(i) == ' ' || str.charAt(i) == '\\') && i < str.length() - 1) {
                if (str.charAt(i) == '\\') {
                    if (str.charAt(i + 1) == 'r' || str.charAt(i + 1) == 'n' || str.charAt(i + 1) == 't') {
                        i++;
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
            i++;
        }
        return true;
    }

    class CloseTagInfo {
        JspAction action;
        String prefix;
        String actName;
        boolean isBodyTag = false;
        boolean tryCatchFinally = false;
        boolean isIteration = false;
        int counterValue = 0;
        VariableInfo[] vais = null;
        TagVariableInfo[] tvais = null;

        CloseTagInfo(String prefix, String actName) {
            this.prefix = prefix;
            this.actName = actName;
        }
    }


}