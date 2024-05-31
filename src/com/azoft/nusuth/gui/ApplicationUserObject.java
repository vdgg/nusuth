package com.azoft.nusuth.gui;

import com.azoft.nusuth.deployment.*;

public class ApplicationUserObject {
    public CompositeNusuthWebAppElement webElement;
    public CompositeNusuthWebAppElement contextElement;

    public ApplicationUserObject(CompositeNusuthWebAppElement contextElement) {
        this.contextElement = contextElement;
        this.webElement = null;
    }

    public ApplicationUserObject(CompositeNusuthWebAppElement webElement, CompositeNusuthWebAppElement contextElement) {
        this.contextElement = contextElement;
        this.webElement = webElement;
    }

    public void setWebElement(CompositeNusuthWebAppElement webElement) {
        this.webElement = webElement;
    }

    public void setContextElement(CompositeNusuthWebAppElement contextElement) {
        this.contextElement = contextElement;
    }

    public CompositeNusuthWebAppElement getWebElement() {
        return this.webElement;
    }

    public CompositeNusuthWebAppElement getContextElement() {
        return this.contextElement;
    }
}