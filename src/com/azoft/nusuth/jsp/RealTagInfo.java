package com.azoft.nusuth.jsp;

import javax.servlet.jsp.tagext.*;

class RealTagInfo {

    private RealTagAttributeInfo[] attributes;
    private TagVariableInfo[] variables;
    private String bodyContent;
    private String infoString;
    private String tagClassName;
    private String teiClassName;
    private RealTagLibrary library;
    private String tagName;

    RealTagInfo(RealTagAttributeInfo[] attributes,
                TagVariableInfo[] variables,
                String bodyContent,
                String infoString,
                String tagClassName,
                String teiClassName,
                RealTagLibrary library,
                String tagName) {

        this.attributes = attributes;
        this.variables = variables;
        this.bodyContent = bodyContent;
        this.infoString = infoString;
        this.tagClassName = tagClassName;
        this.teiClassName = teiClassName;
        this.library = library;
        this.tagName = tagName;
    }

    public RealTagAttributeInfo[] getAttributes() {
        return attributes;
    }

    public TagVariableInfo[] getVariables() {
        return variables;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public String getInfoString() {
        return infoString;
    }

    public String getTagClassName() {
        return tagClassName;
    }

    public String getTeiClassName() {
        return teiClassName;
    }

    public RealTagLibrary getRealTagLibrary() {
        return library;
    }

    public String getTagName() {
        return tagName;
    }

}
