package com.azoft.nusuth.jsp;

import javax.servlet.jsp.tagext.*;

public class TagLibrary extends TagLibraryInfo {

    private TagLibraryValidator validator = null;

    TagLibrary(String prefix, String uri, RealTagLibrary realLib) {
        super(prefix, uri);
        this.info = realLib.getInfoString();
        this.jspversion = realLib.getRequiredVersion();
        this.shortname = realLib.getShortName();
        this.tlibversion = realLib.getTagLibVersion();
        this.urn = realLib.getReliableURN();
        this.validator = realLib.getValidator();
    }

    void setTags(NusuthTagInfo[] tags) {
        this.tags = tags;
    }

    public TagLibraryValidator getValidator() {
        return validator;
    }

}
