package com.azoft.nusuth.deployment;

import org.xml.sax.*;

/**
 * Entity resolver for tag library descriptor.
 * @author vdgg, skilz
 * @version 1.3
 * @since Nusuth1.0
 */
public class JspEntityResolver implements EntityResolver {


    /**
     * Constructor.
     */
    public JspEntityResolver() {
        super();
    }

    /**
     * Resolve entity.
     * @param publicId public id.
     * @param systemId system id.
     * @return InputSource Entity as an InputSource.
     */
    public InputSource resolveEntity(String publicId, String systemId) {
        return new InputSource(getClass().getClassLoader().
                getResourceAsStream("com/azoft/nusuth/deployment/"
                + "web-jsptaglibrary_1_2.dtd"));
    }
}