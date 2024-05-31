package com.azoft.nusuth.deployment;

import org.w3c.dom.Node;

import java.io.Serializable;

/**
 * Interface for web app configuration element as defined in Servlet API 2.2.
 * @version 1.0
 * @author VDGG (vdgg@azoft.com)
 * @since 1.0
 */
public class NusuthWebAppElement implements Serializable {
    protected String ID = null;
    protected int depth = 0;
    protected String tag = null;
    protected Node node = null;

    /** This is the constructor of the NusuthWebAppElement class.
     * @param node this is the DOM tree node which is associated with this element.
     * @param depth the depth of this element in the xml tree.
     */
    protected NusuthWebAppElement(Node node, int depth) {
        super();
        this.node = node;
        this.depth = depth;
        if (node != null) {
            tag = node.getNodeName();
            if (node.getAttributes() != null) {
                Node n = node.getAttributes().getNamedItem("id");
                ID = n == null ? null : node.getNodeValue();
            }
        }
    }


    /** This is the constructor of the NusuthWebAppElement class.
     * @param name this is the tag name of the xml configuration file element which is associated with this object.
     * @param depth the depth of this element in the xml tree.
     */
    protected NusuthWebAppElement(String name, int depth) {
        super();
        this.depth = depth;
        tag = name;
    }


    /** This method gets the element ID
     * @return element ID
     */
    public final String getID() {
        return ID;
    }


    /** This method sets the new element ID
     * @param newID new element ID.
     */
    public final void setID(String newID) {
        ID = newID;
    }


    /** This method gets the element depth.
     * @return element depth.
     */
    public int getDepth() {
        return depth;
    }


    /** This method gets the element tag.
     * @return element tag.
     */
    public final String getTag() {
        return tag;
    }

    protected Node getNode() {
        return node;
    }

    public boolean equals(Object obj) {
        if (obj instanceof NusuthWebAppElement) {
            NusuthWebAppElement other = (NusuthWebAppElement) obj;
            return ((tag == null && other.getTag() == null) || (tag != null && other.getTag() != null && tag.equals(other.getTag())))
                    && ((ID == null && other.getID() == null) || (ID != null && other.getID() != null && ID.equals(other.getID())));
        }
        return false;
    }
}