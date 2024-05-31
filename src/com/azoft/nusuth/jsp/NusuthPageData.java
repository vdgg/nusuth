package com.azoft.nusuth.jsp;

import javax.servlet.jsp.tagext.*;
import java.io.*;

/**
 * This class represent PageData.
 * @author skilz.
 * @since Nusuth1.0
 * @version 1.2
 */
public class NusuthPageData extends PageData {

    /**Content of data*/
    private byte[] content = null;

    /**
     * Constructor.
     * @param content Content of data.
     */
    public NusuthPageData(byte[] content) {
        this.content = content;
    }

    /**
     * Return inputStream from which possible retreive data or null if any errors
     * occured.
     */
    public InputStream getInputStream() {
        try {
            return new ByteArrayInputStream(content);
        } catch (Exception e) {
            return null;
        }
    }

}