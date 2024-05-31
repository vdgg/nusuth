/*
 * @(#)WebAppMutableTreeNode.java 1.0 09/10/2001
 */

package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeploymentException;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

/**
 * Class WebAppMutableTreeNode can have hashtable as user object.
 *
 * @version 1.0 09/10/2001
 * @author  tanya
 * @since Nusuth1.0
 */
public class WebAppMutableTreeNode extends ConfigMutableTreeNode {

    /**
     * There will not be taglibs & users node in this vector.
     */
    private Vector checkedChilds;//  = new Vector();


    /**
     * Constructs a new web app mutable tree node with the specified
     * component id and type.
     *
     * @param   componentId   the specified component id.
     * @param   type          the specified type.
     */
    public WebAppMutableTreeNode(String componentId, String type) {
        super(componentId, type);
    }

    /**
     * Constructs a new web app mutable tree node with the specified
     * component id, type and name.
     *
     * @param   componentId   the specified component id.
     * @param   type          the specified type.
     * @param   name          the specified name.
     */
    public WebAppMutableTreeNode(String componentId, String type, String name) {
        super(componentId, type, name);
    }

    /**
     * Loads the composite user object, if the current user object is string.
     * Overrides the super method for all user object cases.
     */
    public void loadUserObject() {
        if (userObject instanceof String) {
            String[] names = basicPanel.getElementNames(type);
            if (names != null) {
                Hashtable newUserObject = new Hashtable();
                int cnt = names.length;
                for (int i = 0; i < cnt; i++) {
                    CompositeNusuthWebAppElement sElement = loadCompositeElement(names[i]);
                    if (sElement != null) {
                        newUserObject.put(names[i], sElement);
                        addChildNodes(names[i], sElement);
                    }
                }
                setUserObject(newUserObject);
            }
        } else if (userObject instanceof Hashtable) {
            // checks all composite elements are loaded or not
            String[] names = basicPanel.getElementNames(type);
            if (names != null) {
                Hashtable hash = (Hashtable) userObject;
                int cnt = names.length;
                for (int i = 0; i < cnt; i++) {
                    if (hash.get(names[i]) == null) {
                        CompositeNusuthWebAppElement sElement = loadCompositeElement(names[i]);
                        if (sElement != null) {
                            hash.put(names[i], sElement);
                            addChildNodes(names[i], sElement);
                        }
                    }
                }
            }
        }
        setUserObjectLoaded();
    }

    /**
     * Loads the composite element by the specified type.
     * Finds the web-app composite element & gets the composite childs.
     *
     * @param   elementType the specified element type.
     * @return  the necessary composite element
     */
    private CompositeNusuthWebAppElement loadCompositeElement(String elementType) {
        CompositeNusuthWebAppElement res = null;
        if (elementType.equals(BasicPanel.SWEB_APP)) {
            res = basicPanel.getCompositeUserObject(elementType, getComponentId());
            if (res == null) {
                res = BasicPanel.getCompositeElement(elementType);
                BasicPanel.deactivateNotRequired(res);
            }
        } else if (elementType.startsWith(BasicPanel.SWEB_APP + ".")) {
            String childName = elementType.substring(BasicPanel.SWEB_APP.length() + 1);
            CompositeNusuthWebAppElement web_app = getParentWebApp();
            if (web_app != null) {
                try {
                    Enumeration en = web_app.getCompositeChild(childName);
                    if (en != null && en.hasMoreElements()) {
                        res = (CompositeNusuthWebAppElement) en.nextElement();
                    }
                } catch (DeploymentException e) {
                    System.out.println(e);
                }
            }
/*
    } else if (elementType.equals(BasicPanel.SWEBTAGLIBS)) {
      CompositeNusuthWebAppElement web_app = getParentWebApp();
      System.out.println("Here it will be the taglib loading");
    } else if (elementType.equals(BasicPanel.SWEBUSERS)) {
      CompositeNusuthWebAppElement web_app = getParentWebApp();
      System.out.println("Here it will be the web users loading");
*/
        }
        return res;
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
     * Adds children nodes by the specified
     * child name & composite element, if necessary.
     *
     * @param   childName the specified child name
     * @param   sElement  the specified composite element
     */
    private void addChildNodes(String childName,
                               CompositeNusuthWebAppElement sElement) {
        if (type.equals(BasicPanel.SAPP) && childName.equals(BasicPanel.SWEB_APP)) {
            DefaultMutableTreeNode servletsNode = new DefaultMutableTreeNode("servlets");
            DefaultMutableTreeNode filtersNode = new DefaultMutableTreeNode("filters");
            this.add(servletsNode);
            this.add(filtersNode);
            addCheckedChild(servletsNode);
            addCheckedChild(filtersNode);
            addFSNodes("servlet", BasicPanel.SSERVLET, servletsNode, sElement);
            addFSNodes("filter", BasicPanel.SFILTER, filtersNode, sElement);
            addWebNode(WebSecurityEditorPanel.WEB_SECURITY_NODE_NAME,
                    BasicPanel.SWEBSECURITY, sElement);
            addWebNode(WebJndiEditorPanel.WEB_JNDI_NODE_NAME,
                    BasicPanel.SWEBJNDI, sElement);
            addTaglibUsersNode(BasicPanel.SWEBTAGLIBS);
            addTaglibUsersNode(BasicPanel.SWEBUSERS);
        }
    }

    /**
     * Adds the filter or servlet nodes.
     *
     * @param   childName     the specified child name.
     * @param   childType     the specified child type.
     * @param   parentNode    the specified parent node.
     * @param   parentElement the specified parent composite element.
     */
    private void addFSNodes(String childName, String childType,
                            DefaultMutableTreeNode parentNode,
                            CompositeNusuthWebAppElement parentElement) {
        try {
            Enumeration childs = parentElement.getCompositeChild(childName);
            while (childs.hasMoreElements()) {
                CompositeNusuthWebAppElement nextCh =
                        (CompositeNusuthWebAppElement) childs.nextElement();
                String componentId = DefaultEditorPanel.getDisplay(childType, nextCh);
                WebAppMutableTreeNode sn =
                        new WebAppMutableTreeNode(componentId, childType);
                sn.setUserObject(nextCh);
                sn.setElementNode(this.getElementNode());
                parentNode.add(sn);
            }
        } catch (DeploymentException e) {
            System.out.println(e);
        }
    }

    /**
     * Creates a new web app mutable node by the specified
     * component id, type & user object and adds it to this node.
     *
     * @param   componentId   the specified component id
     * @param   type          the specified type
     * @param   webElement    the specified user object
     */
    private void addWebNode(String componentId, String type,
                            CompositeNusuthWebAppElement webElement) {
        WebAppMutableTreeNode node = new WebAppMutableTreeNode(componentId, type);
        node.setUserObject(webElement);
        node.setElementNode(this.getElementNode());
        this.add(node);
        addCheckedChild(node);
    }

    /**
     * Creates a new web app mutable node by the specified
     * component id & type and adds it to this node.
     *
     * @param   componentId   the specified component id
     * @param   type          the specified type
     */
    private void addWebNode(String componentId, String type) {
        WebAppMutableTreeNode node = new WebAppMutableTreeNode(componentId, type);
        node.setElementNode(this.getElementNode());
        this.add(node);
        addCheckedChild(node);
    }

    /**
     * Creates a new taglib users mutable node by the specified
     * component id & type and adds it to this node.
     *
     * @param   componentId   the specified component id
     * @param   type          the specified type
     */
    private void addTaglibUsersNode(String type) {
        String componentId = (type.equals(BasicPanel.SWEBTAGLIBS))
                ? TaglibUserMutableTreeNode.TAGLIBS_NODE_NAME
                : TaglibUserMutableTreeNode.USERS_NODE_NAME;
        TaglibUserMutableTreeNode node =
                new TaglibUserMutableTreeNode(componentId, type);
        this.add(node);
    }

    /**
     * Adds the specified child node to the checkedChildren vector.
     *
     * @param   node    the specified child node.
     */
    private void addCheckedChild(DefaultMutableTreeNode node) {
        if (checkedChilds == null) {
            checkedChilds = new Vector();
        }
        checkedChilds.addElement(node);
    }

    /**
     * Overrides the super method to check
     * only necessary childs.
     *
     * @return  the necessary childrens
     */
    public Enumeration getCheckedChilds() {
        if (type.equals(BasicPanel.SAPP) || checkedChilds != null) {
            return checkedChilds.elements();
        }
        return super.children();
    }
}
