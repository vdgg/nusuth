package com.azoft.nusuth.deployment;

import org.w3c.dom.*;

import java.util.Arrays;

/**
 * SimpleNusuthWebAppElement represents simple element of web app configuration. It consists of ID of element and some
 * character data (#PCDATA in dtd).
 * @version 1.0
 * @author VDGG (vdgg@azoft.com)
 * @since 1.0*/
public class SimpleNusuthWebAppElement extends NusuthWebAppElement {
    private String content;


    /**This is the constructor of the SimpleNusuthWebAppElement.
     * @param node the node from the DOM tree that is associated with this simple element of web app configuration.
     * @param depth the of the simple element of web app configuration.
     */
    protected SimpleNusuthWebAppElement(Node node, int depth) {
        super(node, depth);
        //    System.out.println(getTag());
        //    System.out.println((CharacterData)node.getFirstChild());
        content = node.getFirstChild() == null ? "" : ((CharacterData) node.getFirstChild()).getData().trim();
    }


    /**This is the constructor of the SimpleNusuthWebAppElement.
     * @param name the tag name of the xml tree element that is associated with this simple element of web app configuration.
     * @param depth the depth of the simple element of web app configuration.
     */
    protected SimpleNusuthWebAppElement(String name, int depth) {
        super(name, depth);
    }


    /** This method returns the character data (#PCDATA in dtd) of this web app configuration element.
     * @return the character data (#PCDATA in dtd) of this web app configuration element.
     */
    public String getContent() {
        return content;
    }


    /** This method sets the character data (#PCDATA in dtd) of this web app configuration element.
     * @param the new character data (#PCDATA in dtd) of this web app configuration element.
     */
    public void setContent(String content) {
        this.content = content;
    }


    /**This method creates string from the element information. Needed for serialization.
     * When we wont to save the changed xml configuration of the web application in the xml file.
     * @return the formatted xml representation of this simple element.
     */
    public String toString() {
        if (content == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        char[] tmp = null;
        if (getDepth() > 0) {
            tmp = new char[getDepth() * 2];
            Arrays.fill(tmp, ' ');
            sb.append(tmp);
        }
        sb.append("<" + getTag() + (getID() == null ? ">" : " id=\"" + getID() + "\">"));
        sb.append(getContent() + "</" + getTag() + ">\r\n");
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (obj instanceof SimpleNusuthWebAppElement && super.equals(obj)) {
            SimpleNusuthWebAppElement other = (SimpleNusuthWebAppElement) obj;
            return (content == null && other.getContent() == null) ||
                    (content != null && other.getContent() != null && content.equals(other.getContent()));
        }
        return false;
    }
}