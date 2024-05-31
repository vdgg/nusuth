/*
 * @(#)ConfigMutableTreeNode.java 1.0 12/3/2000
 */

package com.azoft.nusuth.gui;

import javax.swing.tree.DefaultMutableTreeNode;

import com.azoft.nusuth.management.ComponentType;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

import java.util.Enumeration;

/**
 * Class ConfigMutableTreeNode is the special tree node.
 * It's the default implementation.
 * The subclasses can override some methods (loading, for ex).
 *
 * @version 1.0 12/3/2000
 * @author  vdgg, tanya
 * @since Nusuth1.0
 */
public class ConfigMutableTreeNode extends DefaultMutableTreeNode {

    /**
     * the static basic panel
     */
    protected static BasicPanel basicPanel;


    /**
     * The type of this node.
     */
    protected String type;

    /**
     * The component id of this node.
     */
    protected String componentId;

    /**
     * The name of this node.
     */
    private String name;

    /**
     * Defines the composite user object is loaded yet or not.
     */
    private boolean loaded = false;

    /**
     * Defines the node of current element.
     * If this value is null - it's standert element with only one tree node.
     * In other cases - there are some nodes for one element & when commit
     * to the server is happened - all tree nodes must be checked on
     * empty elements.
     */
    protected ConfigMutableTreeNode elementNode = null;


    /**
     * Constructs a new config mutable tree node with the specified
     * component id and type.
     *
     * @param   componentId   the specified component id.
     * @param   type          the specified type.
     */
    public ConfigMutableTreeNode(String componentId, String type) {
        this(componentId, type, componentId);
    }

    /**
     * Constructs a new config mutable tree node with the specified
     * component id, type and name.
     *
     * @param   componentId   the specified component id.
     * @param   type          the specified type.
     * @param   name          the specified name.
     */
    public ConfigMutableTreeNode(String componentId, String type, String name) {
        super(name);
        this.componentId = componentId;
        this.type = type;
        this.name = name;
    }

    /**
     * Gets the name of this node.
     *
     * @return  the name of this node.
     * @see #setName(String)
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the specified name to this node.
     *
     * @param     name    the specified name.
     * @see #getName()
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the type of this node.
     *
     * @return  the type of this node.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the component id of this node.
     *
     * @return  the component id of this node.
     * @see #setComponentId(String)
     */
    public String getComponentId() {
        return (type.equals(BasicPanel.SAPP))
                ? ((ConfigMutableTreeNode) getParent()).getComponentId() + componentId
                : componentId;
    }

    /**
     * Sets the specified component id to this node.
     *
     * @param     componentId   the specified component id.
     * @see #getComponentId()
     */
    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    /**
     * Gets the diaplay name of this node.
     *
     * @return  the diaplay name of this node.
     */
    public String toString() {
        return getName();
    }

    /**
     * Sets the static basic panel to this node.
     * All loading are performed via basic panel.
     *
     * @param   basicPanel    the specified basic panel.
     */
    static void setBasicPanel(BasicPanel bp) {
        basicPanel = bp;
    }

    /**
     * Loads the composite user object, if the current user object is string.
     * Checks the new node name.
     */
    public void loadUserObject() {
        if (userObject instanceof String) {
            CompositeNusuthWebAppElement newUserObject =
                    basicPanel.getCompositeUserObject(type, getComponentId());
            if (newUserObject != null) {
                if (type.equals(BasicPanel.SCONTAINER)
                        || type.equals(BasicPanel.SDISTRIBUTOR)) {
                    String newValue = DefaultEditorPanel.getDisplay(type, newUserObject);
                    if (!newValue.equals((String) userObject)) {
                        basicPanel.fireValueChanged(type, componentId,
                                (String) userObject, newValue);
                        setName(newValue);
                    }
                }
                setUserObject(newUserObject);
            }
        }
        setUserObjectLoaded();
    }

    /**
     * Gets the composite user object is loaded yet or not.
     *
     * @return   <code>true</code>, if the composite user object is loaded yet;
     * <code>false</code> otherwise.
     */
    public boolean isUserObjectLoaded() {
        return loaded;
    }

    /**
     * Sets the loaded value of this node to <code>true</code>.
     */
    protected void setUserObjectLoaded() {
        this.loaded = true;
    }

    /**
     * Gets the element node.
     *
     * @return  the element node.
     * @see #setElementNode(ConfigMutableTreeNode)
     */
    public ConfigMutableTreeNode getElementNode() {
        return elementNode;
    }

    /**
     * Sets the specified node to this element node.
     *
     * @param     elementNode   the specified element node.
     * @see #getElementNode()
     */
    public void setElementNode(ConfigMutableTreeNode elementNode) {
        this.elementNode = elementNode;
    }

    /**
     * Calls the super children() method.
     * Some subclasses overrides this method to check
     * only necessary childs.
     *
     * @return  the necessary childrens
     */
    public Enumeration getCheckedChilds() {
        return super.children();
    }
}

