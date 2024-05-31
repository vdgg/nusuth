package com.azoft.nusuth.jsp;

import javax.servlet.jsp.tagext.*;

public class NusuthTagInfo extends TagInfo {

    NusuthTagInfo(RealTagInfo realTagInfo, TagLibraryInfo tagLib, TagExtraInfo extraInfo) {
        super(realTagInfo.getTagName(), realTagInfo.getTagClassName(), realTagInfo.getBodyContent(),
                realTagInfo.getInfoString(), tagLib, extraInfo, realTagInfo.getAttributes(),
                null, null, null, realTagInfo.getVariables());
        if (extraInfo != null) {
            extraInfo.setTagInfo(this);
        }
    }
}
