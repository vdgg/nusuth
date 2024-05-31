package com.azoft.nusuth.jsp;

import java.beans.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.JspException;

class RealTagAttributeInfo extends TagAttributeInfo {

    private String setter;

    RealTagAttributeInfo(String attName, boolean isRequired, String attType, boolean isRuntime,
                         String setterName) throws JspException {
        super(attName, isRequired, attType, isRuntime);
        this.setter = setterName;
    }

    String getSetter() {
        return setter;
    }
}
