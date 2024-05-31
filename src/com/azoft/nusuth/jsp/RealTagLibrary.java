package com.azoft.nusuth.jsp;

import java.util.*;
import java.beans.*;
import java.net.URL;
import java.net.MalformedURLException;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextAttributesListener;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.HttpSessionAttributesListener;
import javax.servlet.http.HttpSessionActivationListener;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.core.*;
import com.azoft.nusuth.management.Manageable;

/**
 * This class represents the tag library.
 *
 * @author vdgg, skilz
 * @version 1.13
 * @since Nusuth1.0
 */
class RealTagLibrary implements Manageable {

    private String info;
    private String jspVersion;
    private String shortName;
    private RealTagInfo[] tags;
    private String tlibVersion;
    private String urn;
    private Hashtable tagName2className = new Hashtable();
    private LinkedList listeners = new LinkedList();
    private TagLibraryValidator validator = null;
    private Hashtable validatorParameters = new Hashtable();
    private List changeListeners = new LinkedList();
    private ClassLoader loader = null;
    private String location = null;
    private org.apache.log4j.Category cat
            = org.apache.log4j.Category.getInstance("com.azoft.nusuth.jsp");

    /**
     * Constructor for RealTagLibrary.
     * @param location Location of tag library (Full path to tag library
     * descriptor file or url).
     * @param loader ClassLoader that will be used to load classes from the
     * library.
     * @exception JspException Throws if any errors occured while creating
     * instance of this class.
     */
    RealTagLibrary(String location, ClassLoader loader) throws JspException {
        try {
            CompositeNusuthWebAppElement root = null;
            try {
                root = NusuthAppConfigFactory.createConfig("taglib", location);
            } catch (ParserException e) {
                URL url = null;
                try {
                    url = new URL(location);
                } catch (MalformedURLException e1) {
                }
                if (url != null) {
                    root = NusuthAppConfigFactory.createConfig("taglib", url);
                } else {
                    root = NusuthAppConfigFactory.createConfig("taglib", location);
                }
            }
            this.loader = loader;
            this.location = location;
            applySettings(root);
        } catch (Exception ex) {
            throw new JspException("Cannot parse taglib " + location + ", nested: " + ex);
        }
    }

    /**
     * This method applies given settings to library.
     * @param settings Tag library settings.
     */
    public void apply(CompositeNusuthWebAppElement settings)
            throws DeploymentException, JspException {
        CompositeNusuthWebAppElement root = null;
        try {
            root = NusuthAppConfigFactory.createConfig("taglib", location);
        } catch (ParserException e) {
            try {
                root = NusuthAppConfigFactory.createConfig("taglib", new URL(location));
            } catch (Exception ex) {
                throw new JspException("Cannot create config, nested: "
                        + ex.getMessage());
            }
        }
        loadListeners(root, loader, location);
        List tagList = new ArrayList();
        tlibVersion = getSimpleContent(root, "tlib-version", true, location);
        jspVersion = getSimpleContent(root, "jsp-version", false, location);
        if (jspVersion == null) {
            jspVersion = "1.1";
        }
        shortName = getSimpleContent(root, "short-name", true, location);
        urn = getSimpleContent(root, "uri", false, location);
        info = getSimpleContent(root, "description", false, location);
        Enumeration enum = root.getCompositeChild("tag");
        if (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement el = null;
            CompositeNusuthWebAppElement attEl = null;
            CompositeNusuthWebAppElement varEl = null;
            String tagName = null;
            String tagClass = null;
            String teiClass = null;
            String bodyContent = null;
            String tagInfo = null;
            String attName = null;
            String varNameGiven = null;
            String varNameFromAttr = null;
            String varClass = "java.lang.String";
            String varDeclare = "true";
            String varScope = "NESTED";
            String varDescription = null;
            String isReqStr = null;
            String isRuntimeStr = null;
            boolean isReq = false;
            boolean isRuntime = false;
            BeanInfo bInf = null;
            Class cl;
            Enumeration enum1;
            List attrList = new ArrayList();
            List varList = new ArrayList();
            PropertyDescriptor[] pdescs = null;
            String attType;
            String setterMethod;
            while (enum.hasMoreElements()) {
                varList.clear();
                attrList.clear();
                el = (CompositeNusuthWebAppElement) enum.nextElement();
                tagName = getSimpleContent(el, "name", true, location);
                tagClass = getSimpleContent(el, "tag-class", true, location);
                teiClass = getSimpleContent(el, "tei-class", false, location);
                bInf = null;
                try {
                    cl = loader.loadClass(tagClass);
                    bInf = Introspector.getBeanInfo(cl);
                    Class stTagClass = Class.forName("javax.servlet.jsp.tagext.Tag");
                    if (!stTagClass.isAssignableFrom(cl)) {
                        throw new JspException("Tag class " + cl.getName()
                                + " does not implement "
                                + "javax.servlet.jsp.tagext.Tag interface");
                    }
                } catch (Exception ex) {
                    throw new JspException("Cannot load tag class "
                            + tagClass + ", nested: " + ex);
                }
                pdescs = bInf.getPropertyDescriptors();
                bodyContent = getSimpleContent(el, "body-content", false, location);
                if (bodyContent == null) {
                    bodyContent = "JSP";
                } else {
                    if (!bodyContent.equalsIgnoreCase("JSP")
                            && !bodyContent.equalsIgnoreCase("tagdependent")
                            && !bodyContent.equalsIgnoreCase("empty")) {
                        throw new JspException("&lt;body-content&gt; element must contain "
                                + "only \"JSP\", \"tagdependent\", "
                                + "or \"empty\" values");
                    }
                }
                tagInfo = getSimpleContent(el, "description", false, location);
                enum1 = el.getCompositeChild("attribute");
                while (enum1.hasMoreElements()) {
                    attEl = (CompositeNusuthWebAppElement) enum1.nextElement();
                    attName = getSimpleContent(attEl, "name", true, location);
                    isReqStr = getSimpleContent(attEl, "required", false, location);
                    isReq = (isReqStr != null
                            && (isReqStr.equalsIgnoreCase("true")
                            || isReqStr.equalsIgnoreCase("yes")));
                    isRuntimeStr = getSimpleContent(attEl, "rtexprvalue",
                            false, location);
                    isRuntime = (isRuntimeStr != null
                            && (isRuntimeStr.equalsIgnoreCase("true")
                            || isRuntimeStr.equalsIgnoreCase("yes")));
                    attType = null;
                    attType = getSimpleContent(attEl, "type", false, location);
                    setterMethod = null;
                    if (pdescs != null) {
                        for (int i = 0; i < pdescs.length; i++) {
                            if (pdescs[i].getName().equals(attName)) {
                                if (attType == null)
                                    attType = pdescs[i].getPropertyType().getName();
                                if (pdescs[i].getWriteMethod() != null) {
                                    setterMethod = pdescs[i].getWriteMethod().getName();
                                } else {
                                    throw new JspException("Cannot find setter for property "
                                            + attName + " in bean " + cl.getName());
                                }
                                break;
                            }
                        }
                    }
                    if (attType == null) {
                        throw new JspException("Cannot find property "
                                + attName + " in bean " + cl.getName());
                    }
                    if (setterMethod == null) {
                        throw new JspException("Cannot find setter for property "
                                + attName + " in bean " + cl.getName());
                    }
                    attrList.add(new RealTagAttributeInfo(attName, isReq, attType,
                            isRuntime, setterMethod));
                }
                RealTagAttributeInfo[] attMass
                        = new RealTagAttributeInfo[attrList.size()];
                for (int i = 0; i < attMass.length; i++) {
                    attMass[i] = (RealTagAttributeInfo) attrList.get(i);
                }

                enum1 = el.getCompositeChild("variable");
                while (enum1.hasMoreElements()) {
                    varEl = (CompositeNusuthWebAppElement) enum1.nextElement();
                    varNameGiven = getSimpleContent(varEl, "name-given",
                            false, location);
                    varNameFromAttr = getSimpleContent(varEl, "name-from-attribute",
                            false, location);
                    varClass = getSimpleContent(varEl, "variable-class",
                            false, location);
                    if (varClass == null)
                        varClass = "java.lang.String";
                    varDeclare = getSimpleContent(varEl, "declare", false, location);
                    if (varDeclare == null)
                        varDeclare = "true";
                    varScope = getSimpleContent(varEl, "scope", false, location);
                    if (varScope == null)
                        varScope = "NESTED";
                    int scope = -1;
                    if (varScope.equals("AT_BEGIN")) {
                        scope = VariableInfo.AT_BEGIN;
                    } else if (varScope.equals("AT_END")) {
                        scope = VariableInfo.AT_END;
                    } else {
                        scope = VariableInfo.NESTED;
                    }
                    varList.add(new TagVariableInfo(varNameGiven, varNameFromAttr,
                            varClass,
                            varDeclare.trim().
                            equalsIgnoreCase("true"),
                            scope));
                }
                TagVariableInfo[] varMass = new TagVariableInfo[varList.size()];
                for (int i = 0; i < varMass.length; i++) {
                    varMass[i] = (TagVariableInfo) varList.get(i);
                }

                tagList.add(new RealTagInfo(attMass, varMass, bodyContent, tagInfo,
                        tagClass, teiClass, this, tagName));
            }
            tags = new RealTagInfo[tagList.size()];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = (RealTagInfo) tagList.get(i);
                tagName2className.put(tags[i].getTagName(),
                        tags[i].getTagClassName());
            }
        } else {
            throw new JspException("Cannot find required element tag in " + location);
        }
        enum = root.getCompositeChild("validator");
        if (enum.hasMoreElements()) {
            CompositeNusuthWebAppElement element
                    = (CompositeNusuthWebAppElement) enum.nextElement();
            String validatorClass
                    = ((SimpleNusuthWebAppElement)
                    ((Enumeration) element.getSimpleChild("validator-class")).
                    nextElement()).getContent().trim();
            simpleLoad(element, validatorParameters, Tags.INIT_PARAM,
                    Tags.PARAM_NAME, Tags.PARAM_VALUE);
            try {
                validator = (TagLibraryValidator) loader.loadClass(validatorClass).
                        newInstance();
                validator.setInitParameters(validatorParameters);
            } catch (Exception e) {
                throw new JspException("Cannot instantinate TagLibraryValidator "
                        + "class with name \"" + validatorClass
                        + "\", nested: " + e);
            }
        }
    }

    public void registerTagLibraryListener(TagLibraryChangeListener listener) {
        changeListeners.add(listener);
    }

    public void applySettings(CompositeNusuthWebAppElement settings) throws DeploymentException {
        for (int i = 0; i < changeListeners.size(); i++) {
            ((TagLibraryChangeListener) changeListeners.get(i)).onTagLibraryChange();
        }
        try {
            apply(settings);
        } catch (JspException e) {
            cat.error("Cannot apply settings", e);
            throw new DeploymentException(e.getMessage());
        }
    }

    public boolean isRestartNeeded(CompositeNusuthWebAppElement settings) throws DeploymentException {
        return false;
    }

    private void simpleLoad(CompositeNusuthWebAppElement config, Hashtable destination,
                            String rootTagName, String fromTagName, String toTagName) throws DeploymentException {
        CompositeNusuthWebAppElement compElem;
        SimpleNusuthWebAppElement simpleElem1;
        SimpleNusuthWebAppElement simpleElem2;
        Enumeration enum = config.getCompositeChild(rootTagName);
        while (enum.hasMoreElements()) {
            compElem = (CompositeNusuthWebAppElement) enum.nextElement();
            simpleElem1 = (SimpleNusuthWebAppElement) compElem.getSimpleChild(fromTagName).nextElement();
            simpleElem2 = (SimpleNusuthWebAppElement) compElem.getSimpleChild(toTagName).nextElement();
            destination.put(simpleElem1.getContent().trim(), simpleElem2.getContent().trim());
        }
    }


    private String getSimpleContent(CompositeNusuthWebAppElement element, String name, boolean required, String location) throws JspException {
        try {
            String result = null;
            Enumeration enum = element.getSimpleChild(name);
            if (enum.hasMoreElements()) {
                result = ((SimpleNusuthWebAppElement) enum.nextElement()).getContent();
                if (enum.hasMoreElements()) {
                    throw new JspException("Duplicate declaration of " + name + " element in " + location);
                }
            } else {
                if (required) {
                    throw new JspException("Cannot find required element" + name + " in " + location);
                }
            }
            return result;
        } catch (DeploymentException ex) {
            throw new JspException("Cannot parse taglib " + location + ", nested: " + ex);
        }
    }

    private void loadListeners(CompositeNusuthWebAppElement el, ClassLoader loader, String location) throws JspException {
        try {
            CompositeNusuthWebAppElement compElem;
            SimpleNusuthWebAppElement simpleElem;
            Enumeration enum = el.getCompositeChild(Tags.LISTENER);
            while (enum.hasMoreElements()) {
                compElem = (CompositeNusuthWebAppElement) enum.nextElement();
                String listenerClass = ((SimpleNusuthWebAppElement) compElem.getSimpleChild(Tags.LISTENER_CLASS).nextElement()).getContent().trim();
                try {
                    Object listener = loader.loadClass(listenerClass).newInstance();
                    listeners.add(listener);
                } catch (Exception e) {
                }
            }
        } catch (DeploymentException ex) {
            throw new JspException("Cannot parse taglib " + location + ", nested: " + ex);
        }
    }

    public TagLibraryValidator getValidator() {
        return validator;
    }

    public LinkedList getListeners() {
        return listeners;
    }

    public String getInfoString() {
        return info;
    }

    public String getReliableURN() {
        return urn;
    }

    public String getRequiredVersion() {
        return jspVersion;
    }

    public String getShortName() {
        return shortName;
    }

    public RealTagInfo[] getTags() {
        return tags;
    }

    public String getTagLibVersion() {
        return tlibVersion;
    }

    protected String findClass4Tag(String tagName) {
        return (String) tagName2className.get(tagName);
    }

}
