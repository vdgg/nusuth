/*
 * @(#)TaglibUserMutableTreeNode.java 1.0 09/18/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * Class TaglibUserMutableTreeNode is the congif tree node for
 * the web application taglibs & users. Overrides the loadUserObject method.
 *
 * @version 1.0 09/18/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class TaglibUserMutableTreeNode extends ConfigMutableTreeNode {

    /**
     * The taglib node name.
     */
    public final static String TAGLIBS_NODE_NAME = "taglibs";

    /**
     * The tags node name.
     */
//  public final static String TAGS_NODE_NAME = "tags";

    /**
     * The usres node name.
     */
    public final static String USERS_NODE_NAME = "users";


    /**
     * Constructs a new taglib user mutable tree node with the specified
     * component id and type.
     *
     * @param   componentId   the specified component id.
     * @param   type          the specified type.
     */
    public TaglibUserMutableTreeNode(String componentId, String type) {
        super(componentId, type);
    }

    /**
     * Constructs a new taglib user mutable tree node with the specified
     * component id, type and name.
     *
     * @param   componentId   the specified component id.
     * @param   type          the specified type.
     * @param   name          the specified name.
     */
    public TaglibUserMutableTreeNode(String componentId, String type, String name) {
        super(componentId, type, name);
    }


    /**
     * Loads the composite user object, if the current user object is string.
     */
    public void loadUserObject() {
        if (userObject instanceof String) {
            CompositeNusuthWebAppElement web_app = getParentWebApp();
            if (web_app != null) {
                if (getType().equals(BasicPanel.SWEBTAGLIBS)) {
                    CompositeNusuthWebAppElement[] taglibs =
                            basicPanel.getWebAppTaglibs(web_app);
                    if (taglibs != null) {
                        int cnt = taglibs.length;
                        for (int i = 0; i < cnt; i++) {
                            CompositeNusuthWebAppElement nextTaglib = taglibs[i];
                            if (nextTaglib != null) {
                                String componentId = DefaultEditorPanel.
                                        getDisplay(BasicPanel.SWEBTAGLIB, nextTaglib);
                                TaglibUserMutableTreeNode node =
                                        new TaglibUserMutableTreeNode(
                                                componentId, BasicPanel.SWEBTAGLIB);
                                node.setUserObject(nextTaglib);
                                node.setElementNode(node);
                                addTagNodes(node, nextTaglib);
                                this.add(node);
                            }
                        }
                    }
                } else if (getType().equals(BasicPanel.SWEBUSERS)) {
                    CompositeNusuthWebAppElement newUserObject =
                            basicPanel.getWebAppUsers(web_app);
                    if (newUserObject != null) {
                        setUserObject(newUserObject);
                    }
                }
            }
        }
        setUserObjectLoaded();
    }

    /**
     * Gets the web-app composite element from the parent node.
     *
     * @return  the web-app composite element from the parent node.
     */
    private CompositeNusuthWebAppElement getParentWebApp() {
        Object appObject =
                ((DefaultMutableTreeNode) getParent()).getUserObject();
        if (appObject instanceof Hashtable) {
            return (CompositeNusuthWebAppElement)
                    ((Hashtable) appObject).get(BasicPanel.SWEB_APP);
        }
        return null;
    }

    /**
     * Adds all tag nodes to the specified taglib node by the
     * specified taglib composite element.
     *
     * @param   taglibNode      the specified taglib node
     * @param   taglibElement   the specified taglib composite element
     */
    void addTagNodes(TaglibUserMutableTreeNode taglibNode,
                     CompositeNusuthWebAppElement taglibElement) {
//    DefaultMutableTreeNode tagsNode = new DefaultMutableTreeNode(TAGS_NODE_NAME);
//    taglibNode.add(tagsNode);
        try {
            Enumeration childs = taglibElement.getCompositeChild("tag");
            while (childs.hasMoreElements()) {
                CompositeNusuthWebAppElement nextCh =
                        (CompositeNusuthWebAppElement) childs.nextElement();
                String componentId =
                        DefaultEditorPanel.getDisplay(BasicPanel.SWEBTAG, nextCh);
                TaglibUserMutableTreeNode tagNode =
                        new TaglibUserMutableTreeNode(componentId, BasicPanel.SWEBTAG);
                tagNode.setUserObject(nextCh);
                tagNode.setElementNode(taglibNode.getElementNode());
                taglibNode.add(tagNode);
            }
        } catch (DeploymentException e) {
            System.out.println(e);
        }
    }
}
