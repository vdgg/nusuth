/*
 * Created by IntelliJ IDEA.
 * To change template for new class use
 * "Source Code" options (Tools | IDE Options), Templates tab.
 */
package com.azoft.nusuth.util;

import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.xerces.parsers.DOMParser;

import java.io.*;

public class TLDConvertor {

    public static void main(String[] args) {
        if (args == null || args.length != 2)
            throw new IllegalArgumentException("You must give 2 parameters to this method : \"sourcePath\" and \"resultFile\"");
        if (args[0].equals(args[1]))
            throw new IllegalArgumentException("You must give 2 different parameters to this method");
        FileWriter out = null;
        try {
            File src = new File(args[0]);
            File result = new File(args[1]);
            if (result.exists())
                throw new IllegalArgumentException("Resulting file " + result.getAbsolutePath() + " already exist, give another second parameter");
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(new FileInputStream(src.getAbsolutePath())));
            Document document = parser.getDocument();
            DocumentType type = document.getDoctype();
            NodeList list = document.getChildNodes();
            out = new FileWriter(result);
            out.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\r\n");
            if (type != null) {
                out.write("<!DOCTYPE " + type.getName() + " " + ((type.getSystemId() != null) ? "SYSTEM \"" + type.getSystemId() + "\"" : "PUBLIC \"" + type.getPublicId() + "\"") + ">\r\n\r\n");
            }
            processChildNodes(document.getElementsByTagName("taglib"), out);
        } catch (Exception ex) {
            System.err.println(ex);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
    }

    private static void processChildNodes(NodeList list, FileWriter out) throws IOException {
        if (list == null) return;
        String name;
        String value;
        for (int i = 0; i < list.getLength(); i++) {
            name = list.item(i).getNodeName();
            value = list.item(i).getNodeValue();
            if (name.equals("tlibversion")) {
                out.write("<tlib-version>");
                processChildNodes(list.item(i).getChildNodes(), out);
                out.write("</tlib-version>");
            } else if (name.equals("jspversion")) {
                out.write("<jsp-version>");
                processChildNodes(list.item(i).getChildNodes(), out);
                out.write("</jsp-version>");
            } else if (name.equals("shortname")) {
                out.write("<short-name>");
                processChildNodes(list.item(i).getChildNodes(), out);
                out.write("</short-name>");
            } else if (name.equals("info")) {
                out.write("<description>");
                processChildNodes(list.item(i).getChildNodes(), out);
                out.write("</description>");
            } else if (name.equals("tagclass")) {
                out.write("<tag-class>");
                processChildNodes(list.item(i).getChildNodes(), out);
                out.write("</tag-class>");
            } else if (name.equals("teiclass")) {
                out.write("<tei-class>");
                processChildNodes(list.item(i).getChildNodes(), out);
                out.write("</tei-class>");
            } else if (name.equals("bodycontent")) {
                out.write("<body-content>");
                processChildNodes(list.item(i).getChildNodes(), out);
                out.write("</body-content>");
            } else if (name.equals("#text")) {
                out.write(value);
            } else if (name.equals("#comment")) {
                out.write("<!--" + value + "-->");
            } else {
                out.write("<" + name + ">");
                processChildNodes(list.item(i).getChildNodes(), out);
                out.write("</" + name + ">");
            }
        }
    }

}