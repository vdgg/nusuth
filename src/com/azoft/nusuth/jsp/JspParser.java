package com.azoft.nusuth.jsp;

import com.azoft.nusuth.util.*;

import java.io.*;
import java.util.*;
import java.beans.*;
import java.lang.reflect.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * @author vdgg, skilz
 * @version 1.32
 * @since Nusuth1.0
 */
public class JspParser extends AbstractJspParser {

    protected char[][] tFind = {{'<', '%'}, {'<', '!', '-', '-'}, {'-', '-', '>'}, {'<', 'j', 's', 'p', ':'}, {'<', '/', 'j', 's', 'p', ':'}};
    protected int[][] taglibFind = {{0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}};
    protected int[][] commentFind = {};
    protected RollbackInputStream source;

    public JspParser(String sourceLocation, String workDir, String contextName, String fileName, Hashtable jspTags,
                     String contextBase, TagLibraryRepository repository, ClassLoader loader) {
/*
        Hashtable rep = repository.getRepository();
        Enumeration enum = rep.keys();
        while (enum.hasMoreElements()) {
            Object key = enum.nextElement();
            RealTagLibrary rtl = (RealTagLibrary) rep.get(key);
            char[][] tmp = new char[tFind.length + 2][1];
            for (int k = 0; k < tFind.length; k++) {
                tmp[k] = tFind[k];
            }
            String prefix1 = "<" + rtl.getShortName() + ":";
            String prefix2 = "</" + rtl.getShortName() + ":";
            tmp[tFind.length] = prefix1.toCharArray();
            tmp[tFind.length + 1] = prefix2.toCharArray();
            tFind = tmp;
        }
*/
        this.sourceLocation = sourceLocation;
        File lastInclude = new File(sourceLocation);
        fileSet.push(lastInclude);
        this.jspTags = jspTags;
        this.contextName = contextName;
        this.fileName = fileName;
        this.contextBase = contextBase;
        this.repository = repository;
        this.loader = loader;
        this.workDir = workDir;
    }

    /**
     * @return void
     * @exception JspException - if fatal translation error occured
     * @since 1.0
     */
    public void parse() throws JspException, IOException {
        try {
            destination = new FileOutputStream(workDir + File.separator + fileName + ".java");
        } catch (IOException ioex) {
            if (destination != null) {
                destination.close();
                destination = null;
            }
            throw ioex;
        }
        if (destination == null) {
            throw new JspException("Parser not ready");
        }
        findEncoding();
        preServ = new PreServlet("_jsp_" + contextName, fileName, encoding);
        parseToPreservlet();
        try {
            preServ.writeTo(new PrintWriter(new OutputStreamWriter(destination, encoding)));
        } catch (IOException ioex) {
            //Logger.log(ioex, 1);
            cat.error("Cannot read from source", ioex);
            throw new JspException("Cannot read from source, nested: " + ioex);
        } finally {
            try {
                destination.close();
            } catch (IOException ioex) {
                cat.error("Cannot close destination", ioex);
            }
            destination = null;
        }
    }

    public void findEncoding() throws IOException, JspException {
        try {
            source = new RollbackInputStream(new FileInputStream(sourceLocation));
        } catch (IOException ioex) {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException ioex1) {
                    //Logger.log(ioex1, 1);
                    cat.error("Cannot close jsp source", ioex1);
                }
            }
            throw ioex;
        }
        if (source == null) {
            throw new JspException("Parser not ready");
        }
        encoding = "ISO8859_1";
        JspFileReader reader;
        String token;
        JspDirective jspDir;
        int nFound;
        char tChar;
        StringBuffer sb = new StringBuffer();
        int row = 0;
        int column = 0;
        try {
            reader = new JspFileReader(source, "ISO8859_1");
            int commentRow = -1;
            int commentCol = -1;
            int pageRow = -1;
            int pageCol = -1;
            while ((nFound = reader.findFirstOf(tFind, sb, true)) != -1) {
                switch (nFound) {
                    case 0:
                        sb.setLength(0);
                        tChar = reader.read();
                        if (tChar == '@') {
                            token = reader.readToken();
                            if (token.equals("page")) {
                                column = reader.getCurrentColumn();
                                row = reader.getCurrentRow();
                                try {
                                    jspDir = reader.parseDirective();
                                } catch (JspException jspe) {
                                    throw new JspException("Cannot parse \'page\' directive at line " +
                                            row + ", column " + column + ", nested:\r\n" + jspe);
                                }
                                if (jspDir.getAttribute("pageEncoding") != null) {
                                    encoding = jspDir.getAttribute("pageEncoding");
                                } else if (jspDir.getAttribute("contentType") != null) {
                                    String contType = jspDir.getAttribute("contentType");
                                    int start = contType.indexOf("charset=");
                                    if (start > -1) {
                                        int end = contType.indexOf(';', start);
                                        encoding = contType.substring(start + 8, end > start ? end : contType.length());
                                        pageCol = reader.getCurrentColumn();
                                        pageRow = reader.getCurrentRow();
//                  break;
                                    }
                                }
                            } else if (token.equals("taglib")) {
                                column = reader.getCurrentColumn();
                                row = reader.getCurrentRow();
                                try {
                                    jspDir = reader.parseDirective();
                                } catch (JspException jspe) {
                                    throw new JspException("Cannot parse \'taglib\' directive at line " +
                                            row + ", column " + column + ", nested:\r\n" + jspe);
                                }
                                String prefix = jspDir.getAttribute("prefix");
                                char[][] tmp = new char[tFind.length + 2][1];
                                int[][] tmp2 = new int[tFind.length + 2][1];
                                for (int i = 0; i < tFind.length; i++) {
                                    tmp[i] = tFind[i];
                                    tmp2[i] = taglibFind[i];
                                }
                                String prefix1 = "<" + prefix + ":";
                                String prefix2 = "</" + prefix + ":";
                                tmp[tFind.length] = prefix1.toCharArray();
                                tmp[tFind.length + 1] = prefix2.toCharArray();
                                tmp2[tFind.length] = new int[2];
                                tmp2[tFind.length + 1] = new int[2];
                                tmp2[tFind.length][0] = reader.getCurrentColumn();
                                tmp2[tFind.length][1] = reader.getCurrentRow();
                                tmp2[tFind.length + 1][0] = reader.getCurrentColumn();
                                tmp2[tFind.length + 1][1] = reader.getCurrentRow();
                                tFind = tmp;
                                taglibFind = tmp2;
                            }
                        }
                        break;
                    case 1:
                        if (commentCol == -1 && commentRow == -1) {
                            commentCol = reader.getCurrentColumn();
                            commentRow = reader.getCurrentRow();
                        }
                        break;
                    case 2:
                        if (commentCol != -1 && commentRow != -1) {
                            int[][] tmp = new int[commentFind.length + 1][2];
                            for (int i = 0; i < commentFind.length; i++) {
                                tmp[i][0] = commentFind[i][0];
                                tmp[i][1] = commentFind[i][1];
                            }
                            tmp[commentFind.length][0] = commentRow;
                            tmp[commentFind.length][1] = commentCol;
                            commentFind = tmp;
                            if (pageRow != -1 && pageCol != -1) {
                                if (((commentRow < pageRow) && (pageRow < reader.getCurrentRow())) ||
                                        ((commentRow == pageRow) && (commentCol < pageCol) && (pageRow < reader.getCurrentRow())) ||
                                        ((commentRow == pageRow) && (commentCol < pageCol) && (pageRow == reader.getCurrentRow()) && (pageCol < reader.getCurrentColumn())) ||
                                        ((commentRow < pageRow) && (pageCol < reader.getCurrentColumn()) && (pageRow == reader.getCurrentRow()))) {
                                    encoding = "ISO8859_1";
                                    pageCol = -1;
                                    pageRow = -1;
                                }
                            }
                            commentCol = -1;
                            commentRow = -1;
                        }
                        break;
                }
            }
        } catch (IOException ioex) {
            //Logger.log(ioex, 1);
            cat.error("Cannot read from source", ioex);
            throw new JspException("Cannot read from source, nested: " + ioex);
        } finally {
            try {
                source.close();
                source = null;
            } catch (IOException ioex1) {
                //Logger.log(ioex1, 1);
                cat.error("Cannot close jsp source", ioex1);
            }
        }
    }

    protected void validate(String prefix, String uri, TagLibrary tagLib) throws JspException {
    }

    protected void parseToPreservlet() throws IOException, JspException {
        try {
            source = new RollbackInputStream(new FileInputStream(sourceLocation));
        } catch (IOException ioex) {
            if (source != null) {
                try {
                    source.close();
                } catch (IOException ioex1) {
                    //Logger.log(ioex1, 1);
                    cat.error("Cannot close jsp source", ioex1);
                }
            }
            throw ioex;
        }
        if (source == null) {
            throw new JspException("Parser not ready");
        }
        JspFileReader reader;
        String token;
        JspDirective jspDir;
        int nFound;
        char tChar;
        StringBuffer sb = new StringBuffer();
        int row = 0;
        int column = 0;
        try {
            source.rollback();
            reader = new JspFileReader(source, encoding, this);
            JspAction action;
            JspScript script;
            sb.setLength(0);
            while ((nFound = reader.findFirstOf(tFind, sb, true)) != -1) {
                if (allowedTags.size() > 0) {
                    /*          if (sb.toString().replace('\r', ' ').replace('\n', ' ').trim().length() > 0) {
                     throw new JspException("Unexpected input: \""+sb.toString()+"\" at line "+reader.getCurrentRow()+", column "+reader.getCurrentColumn());
                     }*/
                    if (nFound != 3 && nFound != 4) {
                        throw new JspException("Unexpected start tag: " + new String(tFind[nFound]) + " at line " + reader.getCurrentRow() + ", column " + reader.getCurrentColumn());
                    }
                }
                switch (nFound) {
                    case 0:
                        sb.deleteCharAt(sb.length() - 1);
                        if (sb.length() > 0) {
//Added by skilz...
                            sb = templateEval(sb.toString());
//
//              if (!isEmptyTemplate(sb)) {
                            preServ.addTemplateText(sb);
//              }
                        }
                        sb.setLength(0);
                        tChar = reader.read();
                        switch (tChar) {
                            case '@':
                                token = reader.readToken();
                                row = reader.getCurrentRow();
                                column = reader.getCurrentColumn();
                                if (token.equals("page")) {
                                    try {
                                        handlePageDirective(reader.parseDirective());
                                    } catch (JspException jspe) {
                                        throw new JspException("Cannot handle \'page\' directive at line " + row + ", column " + column + ", nested: " + jspe);
                                    }
                                } else if (token.equals("include")) {
                                    try {
//                      handleIncludeDirective(reader.parseDirective(), reader);
                                        handleIncludeDirective(reader.parseDirective());
                                    } catch (JspException jspe) {
                                        throw new JspException("Cannot handle \'include\' directive at line " + row + ", column " + column + ", nested: " + jspe);
                                    }
                                } else if (token.equals("taglib")) {
                                    try {
                                        handleTaglibDirective(reader.parseDirective());
                                    } catch (JspException jspe) {
                                        throw new JspException("Cannot handle \'taglib\' directive at line " + row + ", column " + column + ", nested: " + jspe);
                                    }
                                } else {
                                    throw new JspException("Unrecognized directive: " + token + " at line " + row + ", column " + column);
                                }
                                break;
                            case '!':
                                reader.readUntil("%>", sb, false);
//Added by skilz...
                                sb = runtimeEvaluate(sb);
//
                                preServ.add2Declarations(sb.toString());
                                sb.setLength(0);
                                break;
                            case '=':
                                reader.readUntil("%>", sb, false);
//                  if (tagdepend) {
//                    sb = sb.insert(0, "<%=");
//                    sb.append("%>");
//                  }
//Added by skilz...
                                sb = runtimeEvaluate(sb);
//
                                preServ.add2Service("      out.print(" + sb.toString() + ");\r\n");
                                sb.setLength(0);
                                break;
                            case '-':
                                row = reader.getCurrentRow();
                                column = reader.getCurrentColumn();
                                char nChar = reader.read();
                                if (nChar == '-') {
                                    if (!reader.skipUntil("--%>", false)) {
                                        throw new JspException("Cannot find end of comment that starts at line " + row + ", column " + column);
                                    }
                                } else {
                                    sb.append(tChar);
                                    sb.append(nChar);
                                    reader.readUntil("%>", sb, false);
                                    preServ.add2Service("      " + sb.toString());
                                    sb.setLength(0);
                                }
                                break;
                            case '%':
                                char nextChar = reader.read();
                                if (nextChar != '>') {
                                    sb.append(tChar);
                                    sb.append(nextChar);
                                    reader.readUntil("%>", sb, false);
                                    preServ.add2Service("      " + sb.toString());
                                    sb.setLength(0);
                                }
                                break;
                            default:
                                if (tChar != ' ' && tChar != '\t' && tChar != '\n' && tChar != '\r') {
                                    sb.append(tChar);
                                }
                                reader.readUntil("%>", sb, false);
//Added by skilz...
                                sb = runtimeEvaluate(sb);
//
                                preServ.add2Service("      " + sb.toString());
                                sb.setLength(0);
                                break;
                        }
                        break;
                    case 1:
                        sb.append('-');
                        boolean cont = false;
                        int rowNumber = reader.getCurrentRow();
                        int colNumber = reader.getCurrentColumn();
                        for (int i = 0; i < commentFind.length; i++) {
                            if (commentFind[i][0] == rowNumber && commentFind[i][1] == colNumber) {
                                cont = true;
                                break;
                            }
                        }
                        if (cont) {
                            reader.readUntil("-->", sb, true);
                            sb.append("-->");
                            processComments(sb);
                            sb.setLength(0);
                        }
                        break;
                    default:
                        if (nFound != 2) {
                            if (nFound > 4) {
/*
                                if (reader.getCurrentRow() < taglibFind[nFound][1]) {
                                    throw new JspException("Usage of taglib before declaration not allowed (column " + reader.getCurrentColumn() + ", row " + reader.getCurrentRow() + ")");
                                } else if (reader.getCurrentRow() == taglibFind[nFound][1] && reader.getCurrentColumn() < taglibFind[nFound][0]) {
                                    throw new JspException("Usage of taglib before declaration not allowed (column " + reader.getCurrentColumn() + ", row " + reader.getCurrentRow() + ")");
                                }
*/
                            }
                            sb.setLength(sb.length() + 1 - tFind[nFound].length);
                            if (isFallBack) {
                                fallBackText += sb.toString();
                            } else if (sb.length() > 0) {
//Added by skilz...
                                sb = templateEval(sb.toString());
//
//                if (!isEmptyTemplate(sb)) {
                                preServ.addTemplateText(sb);
//                }
                            }
                            sb.setLength(0);
                            char[] mf = {' ', '>'};
                            int chFound = reader.findFirstOf(mf, sb, false);
                            String actName = sb.toString().trim();
                            //            System.out.println(actName);
                            if (allowedTags.size() > 0 && !allowedTags.contains(actName) && nFound % 2 == 1) {
                                throw new JspException("Standart action jsp:" + actName + " not allowed here");
                            }
//            System.out.println(new String(tFind[nFound]));
                            String prefix = new String(tFind[nFound], 1, tFind[nFound].length - 2);
//            String prefix = new String(tFind[nFound]);
                            if (nFound % 2 == 0) {
                                doEndTag(prefix.substring(1, prefix.length()), actName);
                            } else {
                                if (chFound == 0) {
                                    JspAction act = reader.parseAction();
                                    doStartTag(prefix, actName, act);
                                    if (tagdepend && act.hasBody()) {
                                        sb.setLength(0);
                                        boolean find = reader.readUntil("</" + prefix + ":" + actName + ">", sb, true);
                                        if (!find)
                                            throw new JspException("Cannot find closed tag for " + prefix + ":" + actName + " action");
                                        preServ.addTemplateText(templateEval(sb.toString()));
                                        doEndTag(prefix, actName);
//                       preServ.addTemplateText();
                                    } else if (empty && act.hasBody()) {
                                        sb.setLength(0);
                                        boolean find = reader.readUntil("</" + prefix + ":" + actName + ">", sb, false);
                                        if (!find)
                                            throw new JspException("Cannot find closed tag for " + prefix + ":" + actName + " action");
                                        if (sb.length() > 0 && !onlyComments(sb.toString()))
                                            throw new JspException("Action " + prefix + ":" + actName + " must be empty");
                                        doEndTag(prefix, actName);
                                    }
                                } else {
                                    JspAction jAct = new JspAction();
                                    jAct.setHasBody(sb.charAt(sb.length() - 1) != '/');
                                    doStartTag(prefix, jAct.hasBody() ? actName : actName.substring(0, actName.length() - 1).trim(), jAct);
                                    if (tagdepend && jAct.hasBody()) {
                                        sb.setLength(0);
                                        boolean find = reader.readUntil("</" + prefix + ":" + actName + ">", sb, true);
                                        if (!find)
                                            throw new JspException("Cannot find closed tag for " + prefix + ":" + actName + " action");
                                        preServ.addTemplateText(templateEval(sb.toString()));
                                        doEndTag(prefix, actName);
                                    } else if (empty && jAct.hasBody()) {
                                        sb.setLength(0);
                                        boolean find = reader.readUntil("</" + prefix + ":" + actName + ">", sb, false);
                                        if (!find)
                                            throw new JspException("Cannot find closed tag for " + prefix + ":" + actName + " action");
                                        if (sb.toString().trim().length() > 0 && !onlyComments(sb.toString()))
                                            throw new JspException("Action " + prefix + ":" + actName + " must be empty");
                                        doEndTag(prefix, actName);
                                    }
                                }
                            }
                            sb.setLength(0);
                            break;
                        } else {
                            sb.append('>');
                        }
                }
            }
            try {
                CloseTagInfo s = (CloseTagInfo) closeTags.pop();
                if (s != null) {
                    throw new JspException("Cannot find close tag " + s.prefix + ":" + s.actName);
                }
            } catch (EmptyStackException nsee) {
            }
            if (sb.length() > 0) {
//Added by skilz...
                sb = templateEval(sb.toString());
//
//        if (!isEmptyTemplate(sb)) {
                preServ.addTemplateText(sb);
//        }
            }
        } catch (IOException ioex) {
            //Logger.log(ioex, 1);
            cat.error("Cannot read from source", ioex);
            throw new JspException("Cannot read from source, nested: " + ioex);
        } finally {
            try {
                source.close();
                source = null;
            } catch (IOException ioex1) {
                //Logger.log(ioex1, 1);
                cat.error("Cannot close jsp source", ioex1);
            }
        }
    }

    private boolean onlyComments(String src) {
        if (src.startsWith("<%--") && src.indexOf("<%--") < src.indexOf("--%>")) {
            return onlyComments(src.substring(src.indexOf("--%>") + 4));
        } else {
            return (src.length() == 0);
        }
    }

/*
  protected void handleIncludeDirective(JspDirective directive, JspFileReader reader) throws JspException {
    Enumeration enum = directive.getAttributeNames();
    String attrName = (String)enum.nextElement();
    if (!attrName.equals("file")) {
      throw new JspException("Unrecognized attribute " + attrName + " in include directive");
    }
    if (enum.hasMoreElements()) {
      throw new JspException("More than one attribute found in include directive");
    }
    try {
      reader.includeStream(new RollbackInputStream(new FileInputStream(contextBase + directive.getAttribute(attrName))));
    } catch (Exception ex) {
      Logger.log(ex, 1);
      throw new JspException("Error occured while including " + directive.getAttribute(attrName) + ", nested: " + ex);
    }
  }
*/

    protected TagLibrary getTagLibrary(String prefix, String uri, String tmpUri) throws JspException {
        return repository.getLibrary(prefix, uri, tmpUri, false);
    }
/*
    protected void handleIncludeDirective(JspDirective directive, JspFileReader reader) throws JspException {
      Enumeration enum = directive.getAttributeNames();
      String attrName = (String)enum.nextElement();
      String includeName = directive.getAttribute(attrName);
      if (!attrName.equals("file")) {
        throw new JspException("Unrecognized attribute " + attrName + " in include directive");
      }
      if (enum.hasMoreElements()) {
        throw new JspException("More than one attribute found in include directive");
      }
      String src = null;
      try {
        if (includeName.startsWith("/")){
          src = contextBase;
        }
        else{
          src = sourceLocation.substring(0, sourceLocation.lastIndexOf(File.separator)+1);
        }
        File lastInclude = new File(src, includeName);
        if (fileSet.search(lastInclude) != -1) {
          throw new JspException("Cycled include of jsp " + lastInclude.getName() + " found");
        } else {
          fileSet.push(lastInclude);
        }
        realLocation.push(sourceLocation);
        sourceLocation = lastInclude.getAbsolutePath();
        AbstractJspParser parser = AbstractJspParser.getParser(sourceLocation, workDir, contextName, fileName, jspTags, contextBase, repository, loader);
        parser.setPreServlet(preServ);
        parser.addIncludedFiles(fileSet);
        parser.findEncoding();
        parser.parseToPreservlet();
        sourceLocation = (String)realLocation.pop();
        try {
          fileSet.pop();
        } catch(EmptyStackException e) {
        }
//        reader.includeStream(new RollbackInputStream(new FileInputStream(lastInclude)));
      } catch (Exception ex) {
        //Logger.log(ex, 1);
        cat.error("Error occured while including " + sourceLocation + ", nested: ", ex);
        throw new JspException("Error occured while including " +sourceLocation+ ", nested: " + ex);
      }
    }
*/
    public static void main(String[] args) {
        if (args.length != 2) {
            System.exit(0);
        }
        try {
            long t1 = System.currentTimeMillis();
            //      (new JspParser(args[0], args[1], "test", "Test", null, null, null)).parse();
            System.out.println("Generate time: " + (System.currentTimeMillis() - t1) + "ms");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//Added by skilz...
    protected StringBuffer templateEval(String str) {
        StringBuffer buf = new StringBuffer();
        int prev = -1;
        int i = 0;
        while (i < (str.length() - 2)) {
            if (str.charAt(i) == '\\') {
                char c = str.charAt(i + 2);
                if ((str.charAt(i + 1) == '\\') && ((c == '>' && prev != -1 && str.charAt(prev) == '%') || (c == '%' && prev != -1 && str.charAt(prev) == '<'))) {
                    buf.append(str.charAt(i + 2));
                    prev = i;
                    i = i + 3;
                } else {
                    buf.append(str.charAt(i));
                    prev = i;
                    i++;
                }
            } else {
                buf.append(str.charAt(i));
                prev = i;
                i++;
            }
        }
        for (int j = prev + 1; j < str.length(); j++) {
            buf.append(str.charAt(j));
        }
        return buf;
    }

    /**
     * This method process html comments and add it to preServlet.
     * @param sb StringBuffer that contains comments.
     */
    protected void processComments(StringBuffer sb) {
        String content = sb.toString();
        int start = -1;
        int stop = -1;
        while (((start = content.indexOf("<%="))
                < (stop = content.indexOf("%>", start))) && (start != -1)) {
            preServ.addTemplateText(content.substring(0, start));
            preServ.add2Service("      out.print("
                    + deleteChar('\\',
                            content.substring(start + 3, stop))
                    + ");\r\n");
            content = content.substring(stop + 2);
        }
        preServ.addTemplateText(content);
    }
//

    protected String deleteChar(char c, String str) {
        StrBuffer buf = new StrBuffer();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != c) buf.append(str.charAt(i));
        }
        return buf.toString();
    }


    void endOfInclude() {
        sourceLocation = (String) realLocation.pop();
        try {
            fileSet.pop();
        } catch (EmptyStackException e) {
        }
    }

    protected boolean isRuntime(String str) {
        return str.startsWith("<%=");
    }

    /**
     * This method evaluates request time parameter. It replace \" by ", \' by '
     * and \\ by \.
     * @param param Parameter to evaluate.
     * @return Evaluted parameter.
     * @throws JspException Throws if given parameter is not request time.
     */
    protected String evaluateParameter(String param) throws JspException {
        if (isRuntime(param.trim())) {
            String res = param.trim().substring(3);
            if (!res.endsWith("%>")) {
                throw new JspException("Cannot find script close tag in request-time"
                        + " parameter value " + param);
            }
            res = res.substring(0, res.length() - 2).trim();
            StringBuffer sb = new StringBuffer();
            if (res.length() > 0) {
                for (int i = 0; i < (res.length() - 1); i++) {
                    if (res.charAt(i) == '\\'
                            && (res.charAt(i + 1) == '\"' || res.charAt(i + 1) == '\'')) {
                    } else if (res.charAt(i) == '\\'
                            && ((res.charAt(i - 1) == '<' && res.charAt(i + 1) == '%')
                            || (res.charAt(i - 1) == '%' && res.charAt(i + 1) == '>'))) {
                    } else if (res.charAt(i) == '\\' && res.charAt(i + 1) == '\\') {
                        sb.append(res.charAt(i));
                        i = i + 1;
                    } else {
                        sb.append(res.charAt(i));
                    }
                }
                sb.append(res.charAt(res.length() - 1));
            }
            StringBuffer realResult = new StringBuffer();
            realResult.append(sb.charAt(0));
            for (int i = 1; i < sb.length() - 1; i++) {
                if (sb.charAt(i) == '\\'
                        && ((sb.charAt(i - 1) == '<' && sb.charAt(i + 1) == '%')
                        || (sb.charAt(i - 1) == '%' && sb.charAt(i + 1) == '>'))) {
                    realResult.append('\\');
                    realResult.append('\\');
                } else {
                    realResult.append(sb.charAt(i));
                }
            }
            if (sb.length() > 1) {
                realResult.append(sb.charAt(sb.length() - 1));
            }
//      return sb.toString();
            return realResult.toString();
        } else {
            return param;
        }
    }

    protected StringBuffer runtimeEvaluate(StringBuffer buf) {
        StringBuffer result = new StringBuffer();
        String src = new String(buf);
        if (src.length() != 0) {
            result.append(src.charAt(0));
            for (int i = 1; i < (src.length() - 1); i++) {
                if (!(src.charAt(i) == '\\' && (((src.charAt(i + 1) == '>' && src.charAt(i - 1) == '%') || (src.charAt(i - 1) == '<' && src.charAt(i + 1) == '%'))))) {
                    result.append(src.charAt(i));
                }
            }
            if (src.length() > 1) {
                result.append(src.charAt(src.length() - 1));
            }
        }
        return result;
    }

    protected void addTFind(char[][] othTFind, int[][] othTaglibFind) {
        if (othTFind.length <= 5) {
            return;
        }
        char[][] tmp = new char[tFind.length + othTFind.length - 5][1];
        int[][] tmp2 = new int[tFind.length + othTaglibFind.length - 5][1];
        for (int i = 0; i < tFind.length; i++) {
            tmp[i] = tFind[i];
            tmp2[i] = taglibFind[i];
        }
        for (int i = 5; i < othTFind.length; i++) {
            tmp[tFind.length + i - 5] = othTFind[i];
            tmp2[tFind.length + i - 5] = othTaglibFind[i];
        }

        this.tFind = tmp;
        this.taglibFind = tmp2;
    }

    public char[][] getTFind() {
        return tFind;
    }

    public int[][] getTaglibFind() {
        return taglibFind;
    }

}

