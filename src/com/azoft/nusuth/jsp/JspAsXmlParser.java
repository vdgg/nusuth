package com.azoft.nusuth.jsp;

import java.io.*;
import java.util.*;
import java.beans.*;
import java.lang.reflect.Method;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagLibraryValidator;
import javax.servlet.jsp.tagext.PageData;
import javax.servlet.jsp.tagext.ValidationMessage;

import com.azoft.nusuth.util.*;

/**
 * This is a parser for jsp documents.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.12
 */
public class JspAsXmlParser extends AbstractJspParser {

    private Hashtable id2element = new Hashtable();
    private StrBuffer sbTemp = new StrBuffer();
    private int idCounter = 0;
    private ByteArrayOutputStream os = null;

    public JspAsXmlParser(String sourceLocation, String workDir,
                          String contextName, String fileName, Hashtable jspTags,
                          String contextBase, TagLibraryRepository repository,
                          ClassLoader loader) {
        jspPrefix = "";
        this.sourceLocation = sourceLocation;
        File lastInclude = new File(sourceLocation);
        fileSet.push(lastInclude);
        this.jspTags = jspTags;
        this.contextName = contextName;
        this.fileName = fileName;
        this.contextBase = contextBase;
        this.repository = repository;
        this.loader = loader;
        encoding = "ISO8859_1";
        this.workDir = workDir;
    }

    public void parse() throws JspException, IOException {
        try {
            destination = new FileOutputStream(workDir + File.separator
                    + fileName + ".java");
        } catch (IOException ioex) {
            if (destination != null) {
                destination.close();
                destination = null;
            }
            throw ioex;
        }
        findEncoding();
        preServ = new PreServlet("_jsp_" + contextName, fileName, encoding);
        parseToPreservlet();
        try {
            preServ.writeTo(new PrintWriter(new OutputStreamWriter(destination,
                    encoding)));
        } catch (IOException e) {
//      System.out.println(e);
            cat.error("Cannot write to destination", e);
        } finally {
            try {
                destination.close();
            } catch (IOException ioex) {
                cat.error("Cannot close destination", ioex);
            }
            destination = null;
        }
    }

    protected void findEncoding() throws JspException, IOException {
        DOMParser parser = new DOMParser();
        try {
            parser.parse(new InputSource(new FileInputStream(sourceLocation)));
        } catch (SAXException e) {
            throw new JspException("Cannot parse document, nested: " + e);
        } catch (IOException e) {
            throw new JspException("Cannot parse document, nested: " + e);
        }
        Document doc = parser.getDocument();
        Element rootElement
                = (Element) ((NodeList) doc.getElementsByTagName("jsp:root")).item(0);
        NamedNodeMap nodeMap = rootElement.getAttributes();
        NodeList allElements = rootElement.getChildNodes();
        String prefix;
        String actionName;
        Node childElem;
        for (int i = 0; i < allElements.getLength(); i++) {
            childElem = allElements.item(i);
            prefix = childElem.getPrefix();
            actionName = childElem.getLocalName();
            if (prefix != null) {
                if (prefix.equals(jspPrefix)) {
                    if (actionName.equals("directive.page")) {
                        JspDirective page
                                = (JspDirective) generateAttrContainer(allElements.item(i),
                                        false);
                        if (page.getAttribute("pageEncoding") != null) {
                            encoding = page.getAttribute("pageEncoding");
                            createContentForPageData(rootElement);
                            return;
                        } else if (page.getAttribute("contentType") != null) {
                            String contType = page.getAttribute("contentType");
                            int start = contType.indexOf("charset=");
                            if (start > -1) {
                                int end = contType.indexOf(';', start);
                                encoding = contType.substring(start + 8,
                                        end > start
                                        ? end : contType.length());
                                createContentForPageData(rootElement);
                                return;
                            }
                        }
                    }
                }
            }
        }
        createContentForPageData(rootElement);
    }

    private void createContentForPageData(Element rootElement)
            throws IOException {
        NamedNodeMap nodeMap = rootElement.getAttributes();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Attr attr = (Attr) nodeMap.item(i);
            String prefix = attr.getName();
            prefix = prefix.substring(prefix.indexOf(':') + 1);
            String uri = attr.getValue();
            if (uri.equals("http://java.sun.com/JSP/Page")) {
                jspPrefix = prefix;
            }
        }
        NodeList list = rootElement.getChildNodes();
        for (int j = 0; j < list.getLength(); j++) {
            if (list.item(j) instanceof Element) {
                createIds((Element) list.item(j));
            }
        }
        OutputFormat outFormat = new OutputFormat(rootElement.getOwnerDocument(),
                encoding, true);
        os = new ByteArrayOutputStream();
        XMLSerializer ser = new XMLSerializer(os, outFormat);
        ser.serialize(rootElement.getOwnerDocument());
    }

    private void createIds(Element node) {
        String id = node.getAttribute(jspPrefix + ":id");
        if (id == null || id.length() == 0) {
            id = "_element_id_" + idCounter;
            idCounter++;
            node.setAttribute(jspPrefix + ":id", id);
        }
        NamedNodeMap map = node.getAttributes();
        StrBuffer buf = new StrBuffer();
        getXml(node, buf);
        int index = buf.lastIndexOf('\n');
        StrBuffer newBuf = new StrBuffer();
        for (int i = index + 1; i < buf.length(); i++) {
            if (buf.charAt(i) == ' ') {
                newBuf.append(' ');
            } else {
                break;
            }
        }
        newBuf.append(buf);
        id2element.put(id, newBuf.toString());
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i) instanceof Element) {
                createIds((Element) list.item(i));
            }
        }
    }

    /**
     * This method parse jsp source file to preServlet object
     * @Throws JspException, IOExcpetion Throws if any errors occurs during
     * parsing.
     */
    protected void parseToPreservlet() throws JspException, IOException {
        DOMParser parser = new DOMParser();
        try {
            InputSource iSource
                    = new InputSource(new FileInputStream(sourceLocation));
            iSource.setEncoding(encoding);
            parser.parse(iSource);
        } catch (SAXException e) {
            throw new JspException("Cannot parse document, nested: " + e);
        } catch (IOException e) {
            throw new JspException("Cannot parse document, nested: " + e);
        }
        Document doc = parser.getDocument();
        if (((NodeList) doc.getElementsByTagName("jsp:root")).getLength() != 1) {
            throw new JspException("Only one element jsp:root allowed "
                    + "in jsp document");
        }
        Element rootElement
                = (Element) ((NodeList) doc.getElementsByTagName("jsp:root")).item(0);
        if (!rootElement.getParentNode().getNodeName().equals("#document")) {
            throw new JspException("The root element of the document "
                    + "must be jsp:root");
        }
        NamedNodeMap nodeMap = rootElement.getAttributes();
        JspDirective taglib;
        boolean versionFound = false;
        for (int i = 0; i < nodeMap.getLength(); i++) {
            taglib = new JspDirective();
            Attr attr = (Attr) nodeMap.item(i);
            String prefix = attr.getName();
            if (prefix.equalsIgnoreCase("version")) {
                versionFound = true;
                continue;
            }
            prefix = prefix.substring(prefix.indexOf(':') + 1);
            String uri = attr.getValue();
            if (uri.equals("http://java.sun.com/JSP/Page")) {
                jspPrefix = prefix;
            }
            taglib.putAttribute("prefix", prefix);
            taglib.putAttribute("uri", uri);
            if (!prefix.equals(jspPrefix)) {
                handleTaglibDirective(taglib);
            }
        }
        if (!versionFound) {
            throw new JspException("Cannot find mandatory \"version\" attribute "
                    + "in jsp:root element");
        }
        processElementBody(rootElement.getChildNodes());
    }

    protected TagLibrary getTagLibrary(String prefix, String uri, String tmpUri)
            throws JspException {
        return repository.getLibrary(prefix, uri, tmpUri, true);
    }

    private void generateTemplate(Node elem) throws JspException {
        if (elem.getNodeName().equals(jspPrefix + ":text")) {
            NodeList list = elem.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                processXml(list.item(i));
            }
        } else if (elem.getNodeName().equals("#text")) {
            if (elem.getNodeValue().trim().length() > 0)
                throw new JspException("Only " + jspPrefix
                        + ":text element allowed inside tag "
                        + "which body-content is tagdependent");
        } else {
            throw new JspException("Only " + jspPrefix
                    + ":text element allowed inside tag which "
                    + "body-content is tagdependent");
        }
    }

    private void checkForEmpty(Node node) throws JspException {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeName().equals("#text")) {
                if (list.item(i).getNodeValue().trim().length() > 0)
                    throw new JspException("Body of the tag which body-content empty "
                            + "must be empty");
            } else {
                throw new JspException("Body of the tag which body-content empty must "
                        + "be empty");
            }
        }
    }

    private void processElementBody(NodeList allElements) throws JspException {
        Node childElem;
        String prefix;
        String actionName;
        for (int i = 0; i < allElements.getLength(); i++) {
            childElem = allElements.item(i);
            prefix = childElem.getPrefix();
            actionName = childElem.getLocalName();
            if (prefix != null) {
                if (tagdepend) {
                    sbTemp.clear();
                    generateTemplate(childElem);
                } else if (empty) {
                    checkForEmpty(childElem);
                } else if (prefix.equals(jspPrefix)) {
                    if (allowedTags.size() > 0
                            && !allowedTags.contains(childElem.getLocalName())) {
                        throw new JspException("Action or directive "
                                + childElem.getNodeName()
                                + " not allowed here");
                    }
                    if (actionName.equals("directive.page")) {
                        if (childElem.getChildNodes().getLength() > 0)
                            throw new JspException("Page directive cannot have "
                                    + "any child elements");
                        handlePageDirective(
                                (JspDirective) generateAttrContainer(childElem, false));
                    } else if (actionName.equals("directive.include")) {
                        if (childElem.getChildNodes().getLength() > 0)
                            throw new JspException("Include directive cannot have any "
                                    + "child elements");
                        handleIncludeDirective(
                                (JspDirective) generateAttrContainer(childElem, false));
                    } else if (actionName.equals("declaration")) {
                        preServ.add2Declarations(processTemplateContent(childElem));
                    } else if (actionName.equals("scriptlet")) {
                        preServ.add2Service(processTemplateContent(childElem));
                    } else if (actionName.equals("param")) {
                        handleParamStart((JspAction) generateAttrContainer(childElem,
                                true));
                    } else if (actionName.equals("expression")) {
                        preServ.add2Service("      out.print("
                                + processTemplateContent(childElem)
                                + ");\r\n");
                    } else if (actionName.equals("useBean")) {
                        JspAction action = (JspAction) generateAttrContainer(childElem,
                                true);
                        doStartTag(jspPrefix, "useBean", action);
                        if (action.hasBody()) {
                            NodeList list = allElements.item(i).getChildNodes();
                            processElementBody(list);
                        }
                    } else if (actionName.equals("getProperty")) {
                        JspAction action = (JspAction) generateAttrContainer(childElem,
                                true);
                        doStartTag(jspPrefix, "getProperty", action);
                    } else if (actionName.equals("setProperty")) {
                        JspAction action = (JspAction) generateAttrContainer(childElem,
                                true);
                        doStartTag(jspPrefix, "setProperty", action);
                    } else if (actionName.equals("include")) {
                        JspAction action = (JspAction) generateAttrContainer(childElem,
                                true);
                        handleIncludeStart(action);
                        if (action.hasBody()) {
                            NodeList list = childElem.getChildNodes();
                            processElementBody(list);
                            handleIncludeEnd();
                        }
                    } else if (actionName.equals("params")) {
                        JspAction action = (JspAction) generateAttrContainer(childElem,
                                true);
                        handleParamsStart(action);
                        if (action.hasBody()) {
                            NodeList list = childElem.getChildNodes();
                            processElementBody(list);
                            handleParamsEnd();
                        }
                    } else if (actionName.equals("forward")) {
                        JspAction action = (JspAction) generateAttrContainer(childElem,
                                true);
                        handleForwardStart(action);
                        if (action.hasBody()) {
                            NodeList list = childElem.getChildNodes();
                            processElementBody(list);
                            handleForwardEnd();
                        }
                    } else if (actionName.equals("fallback")) {
                        JspAction action = (JspAction) generateAttrContainer(childElem,
                                true);
                        handleFallbackStart(action);
                        if (action.hasBody()) {
                            fallBackText = processTemplateContent(childElem);
                            handleFallbackEnd();
                        }
                    } else if (actionName.equals("plugin")) {
                        JspAction action = (JspAction) generateAttrContainer(childElem,
                                true);
                        handlePluginStart(action);
                        if (action.hasBody()) {
                            NodeList list = childElem.getChildNodes();
                            processElementBody(list);
                            handlePluginEnd();
                        }
                    } else if (actionName.equals("text")) {
                        StringBuffer buf
                                = changeTemplate(
                                        processTemplateContent(allElements.item(i)));
                        if (!isEmptyTemplate(buf))
                            preServ.addTemplateText(buf);
                    } else {
                        throw new JspException("Unexpected action or directive name \""
                                + actionName + "\" for "
                                + jspPrefix + " prefix");
                    }
                } else {
                    JspAction act = (JspAction) generateAttrContainer(childElem, true);
                    doStartTag(prefix, actionName, act);
                    if (act.hasBody()) {
                        processElementBody(childElem.getChildNodes());
                        doEndTag(prefix, actionName);
                    }
                }
            } else {
                String nodeName = childElem.getNodeName();
                if (nodeName.equals("#text")) {
                    if (childElem.getNodeValue().trim().length() > 0) {
                        throw new JspException("Unexpected text found in the document");
                    }
                } else if (nodeName.equals("#cdata-section")) {
                    throw new JspException("CDATA section not allowed inside "
                            + "&lt;jsp:root&gt; element");
                } else if (nodeName.equals("#comment")) {
                    preServ.addTemplateText("<!--" + childElem.getNodeValue() + "-->");
                } else {
                    processXmlFragment(childElem);
                }
            }
        }
    }

    private void processXmlFragment(Node node) throws JspException {
        if (!node.getNodeName().equals("#text")) {
            int numberOfChild = node.getChildNodes().getLength();
            NamedNodeMap map = node.getAttributes();
            StrBuffer result = new StrBuffer();
            result.append("<" + node.getNodeName());
            if (map != null) {
                for (int i = 0; i < map.getLength(); i++) {
                    String attName = ((Attr) map.item(i)).getName();
                    String attValue = ((Attr) map.item(i)).getValue();
                    if (isRuntime(attValue)) {
                        preServ.addTemplateText(result.toString());
                        result.clear();
                        preServ.add2Service(" out.print(\" " + attName + "=\\\"\"+" + evaluateParameter(attValue) + "+\"\\\"\");");
                    } else {
                        result.append(" " + attName + "=\\\""
                                + attValue + "\\\"");
                    }
//          result.append(" "+attName
//                        +"=\\\""+attValue+"\\\"");
                }
            }
            result.append((numberOfChild == 0) ? "/>\\r\\n" : ">");
            preServ.addTemplateText(result.toString());
            NodeList all = node.getChildNodes();
            processElementBody(all);
            if (numberOfChild > 0) {
                preServ.addTemplateText("</" + node.getNodeName() + ">\\t\\n");
            }
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append((node.getNodeValue() != null)
                    ? (changeTemplate(node.getNodeValue()).toString()) : "");
            if (!isEmptyTemplate(buf))
                preServ.addTemplateText(buf);
        }
    }

    /**
     * This method transform given node to xml format and and store it
     * in target StrBuffer.
     * @param node Node to transform.
     * @param target Target to store xml representation.
     */
    private void getXml(Node node, StrBuffer target) {
        int numberOfChild = node.getChildNodes().getLength();
        if (!node.getNodeName().equals("#text")) {
            NamedNodeMap map = node.getAttributes();
            target.append("&lt;" + node.getNodeName());
            if (map != null) {
                for (int i = 0; i < map.getLength(); i++) {
                    if (!((Attr) map.item(i)).getName().equals(jspPrefix + ":id")) {
                        target.append(" " + ((Attr) map.item(i)).getName() + "=\""
                                + ((Attr) map.item(i)).getValue() + "\"");
                    }
                }
            }
            target.append((numberOfChild == 0) ? "/&gt;\r\n" : "&gt;");
            NodeList all = node.getChildNodes();
            for (int i = 0; i < numberOfChild; i++) {
                getXml(all.item(i), target);
            }
        } else {
            target.append((node.getNodeValue() != null)
                    ? (node.getNodeValue()).toString() : "");
        }
        if (!node.getNodeName().equals("#text") && numberOfChild > 0) {
            target.append("&lt;/" + node.getNodeName() + "&gt;");
        }
    }

    private void processXml(Node node) throws JspException {
        if (!node.getNodeName().equals("#text")) {
            int numberOfChild = node.getChildNodes().getLength();
            NamedNodeMap map = node.getAttributes();
            StrBuffer result = new StrBuffer();
            result.append("<" + node.getNodeName());
            if (map != null) {
                for (int i = 0; i < map.getLength(); i++) {
                    String attName = ((Attr) map.item(i)).getName();
                    String attValue = ((Attr) map.item(i)).getValue();
                    result.append(" " + attName + "=\\\""
                            + attValue + "\\\"");
                }
            }
            result.append((numberOfChild == 0) ? "/>\\r\\n" : ">");
            preServ.addTemplateText(result.toString());
            NodeList all = node.getChildNodes();
            for (int i = 0; i < numberOfChild; i++) {
                processXml(all.item(i));
            }
            if (numberOfChild > 0) {
                preServ.addTemplateText("</" + node.getNodeName() + ">\\t\\n");
            }
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append((node.getNodeValue() != null)
                    ? (changeTemplate(node.getNodeValue()).toString()) : "");
            if (!isEmptyTemplate(buf))
                preServ.addTemplateText(buf);
        }
    }

    private String processTemplateContent(Node node) throws JspException {
        NodeList list = node.getChildNodes();
        StrBuffer result = new StrBuffer();
        for (int j = 0; j < list.getLength(); j++) {
            String nodeName = list.item(j).getNodeName();
            String nodeValue = list.item(j).getNodeValue();
            if ((nodeName.equals("#text") || nodeName.equals("#cdata-section"))
                    && nodeValue != null) {
                result.append(nodeValue.trim());
            } else if (nodeName.equals(jspPrefix + ":text")) {
                result.append(processTemplateContent(list.item(j)));
            } else {
                throw new JspException("Action or directive "
                        + nodeName + " not allowed here");
            }
        }
        return result.toString();
    }

    private StringBuffer changeTemplate(String str) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
                case '\t':
                    result.append("\\t");
                    break;
                case '\'':
                    result.append("\\\'");
                    break;
                case '\"':
                    result.append("\\\"");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                default   :
                    result.append(str.charAt(i));
                    break;
            }
        }
        return result;
    }

    private JspAttributeContainer generateAttrContainer(Node node,
                                                        boolean isAction)
            throws JspException {
        JspAttributeContainer result = null;
        if (isAction) {
            result = new JspAction();
        } else {
            result = new JspDirective();
        }
        NamedNodeMap attributes = node.getAttributes();
        for (int j = 0; j < attributes.getLength(); j++) {
            result.putAttribute(attributes.item(j).getNodeName(),
                    attributes.item(j).getNodeValue());
        }
        if (isAction) {
            ((JspAction) result).setHasBody(node.getChildNodes().getLength() > 0);
        }
        return result;
    }

    protected boolean isRuntime(String str) {
        return str.startsWith("%=");
    }

    protected String evaluateParameter(String param) throws JspException {
        if (isRuntime(param.trim())) {
            String res = param.trim().substring(2);
            if (!res.endsWith("%")) {
                throw new JspException("Cannot find script close tag in request-time "
                        + "parameter value " + param);
            }
            res = res.substring(0, res.length() - 1).trim();
            StringBuffer sb = new StringBuffer();
            if (res.length() > 0) {
                for (int i = 0; i < (res.length() - 1); i++) {
                    if (res.charAt(i) == '\\' && res.charAt(i + 1) == '\"') {
                    } else {
                        sb.append(res.charAt(i));
                    }
                }
                sb.append(res.charAt(res.length() - 1));
            }
            return sb.toString();
        } else {
            return param;
        }
    }

    protected StringBuffer runtimeEvaluate(StringBuffer buf) {
        return buf;
    }

    protected void validate(String prefix, String uri, TagLibrary tagLib)
            throws JspException {
        validator = tagLib.getValidator();
        if (validator != null) {
            PageData pageData = new NusuthPageData(os.toByteArray());
            ValidationMessage[] errorCause = validator.validate(prefix, uri,
                    pageData);
            if (errorCause != null && errorCause.length != 0) {
                String errorMessage = "Cannot parse " + sourceLocation
                        + ", validator reported : \r\n";
                for (int i = 0; i < errorCause.length; i++) {
                    errorCause[i].getId();
                    errorCause[i].getMessage();
                    errorMessage = errorMessage + errorCause[i].getMessage();
                    if (errorCause[i].getId() != null) {
                        errorMessage = errorMessage + " inside element \r\n"
                                + id2element.get(errorCause[i].getId()) + "\r\n";
                    }
                }
                throw new JspException(errorMessage);
            }
        }
    }

    protected void addTFind(char[][] tFind, int[][] taglibFind) {
    }

    public char[][] getTFind() {
        return null;
    }

    public int[][] getTaglibFind() {
        return null;
    }

}