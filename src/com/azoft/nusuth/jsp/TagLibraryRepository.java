package com.azoft.nusuth.jsp;

import com.azoft.nusuth.core.NusuthContext;

import java.util.*;
import javax.servlet.jsp.tagext.*;
import javax.servlet.jsp.*;
import java.beans.*;
import java.io.File;
import java.net.URL;

/**
 * This class represents repository of tag libraries.
 * @author vdgg, skilz
 * @version 1.15
 * @since Nusuth1.0
 */
public class TagLibraryRepository {

    private NusuthContext context;
    private Hashtable repository = new Hashtable();
    private Hashtable definedTLDs = new Hashtable();
    private TagLibraryChangeListener listener = null;

    public TagLibraryRepository(NusuthContext context) {
        this.context = context;
    }


    public Hashtable getRepository() {
        return repository;
    }


    public void addDefinedTagLib(String tlURI, String tlLocation) {
        definedTLDs.put(tlURI.toLowerCase(), tlLocation);
    }

    public void registerTagLibraryListener(TagLibraryChangeListener listener) {
        this.listener = listener;
        Enumeration enum = repository.keys();
        while (enum.hasMoreElements()) {
            String key = (String) enum.nextElement();
            ((RealTagLibrary) repository.get(key)).registerTagLibraryListener(listener);
        }
    }

    /**
     * This method returns TagLibrary corresponding to the given parameters.
     * @param prefix Prefix for tag library.
     * @param uri Tag library uri.
     * @param tmpUri //later
     * @param isXml Indicates the type of jsp page that wants receive library.
     * @exception JspException Throws if any errors occures while getting library.
     */
    TagLibrary getLibrary(String prefix, String uri, String tmpUri, boolean isXml)
            throws JspException {
        if (repository.get(uri) == null) {
            repository.put(uri, createRealLibrary(uri, tmpUri, isXml));
        }
        RealTagLibrary rtl = (RealTagLibrary) repository.get(uri);
        if (rtl == null) {
            throw new JspException("Cannot find tag library " + uri);
        }
        TagLibrary result = new TagLibrary(prefix, uri, rtl);
        RealTagInfo[] realTags = rtl.getTags();
        NusuthTagInfo[] jTags = new NusuthTagInfo[realTags.length];
        TagExtraInfo tei;
        for (int i = 0; i < realTags.length; i++) {
            tei = null;
            if (realTags[i].getTeiClassName() != null) {
                try {
                    tei = (TagExtraInfo) Beans.instantiate(context.getServletLoader(),
                            realTags[i].getTeiClassName());
                } catch (Throwable t) {
                    throw new JspException("Cannot instantiate tag extra info class for "
                            + "tag " + realTags[i].getTagName()
                            + ", nested: " + t);
                }
            }
            jTags[i] = new NusuthTagInfo(realTags[i], result, tei);
        }
        result.setTags(jTags);
        return result;
    }

    private synchronized RealTagLibrary createRealLibrary(String uri, String tmpUri, boolean isXml) throws JspException {
        String location = (String) definedTLDs.get(uri.startsWith("urn:jsptld:") ? uri.substring(11) : uri);
        tmpUri = tmpUri.replace(File.separatorChar, '/');
        if (isXml) {
            if (uri.startsWith("urn:jsptld:")) {
                String findUri = uri.substring(11);
                if (location == null) {
                    if (findUri.startsWith("/")) {
                        location = findUri;
                    } else {
                        location = tmpUri + findUri;
                    }
                }
            } else {
                if (location == null)
                    location = uri;
            }
        } else {
            if (location == null)
                location = tmpUri + uri;
        }
        String myLocation = context.getRealPath(location);
        if (location.startsWith("/")) {
            if (!(new File(myLocation)).exists()) {
                throw new JspException("Cannot locate tag library descriptor for uri \""
                        + uri + "\"");
            }
            if (!location.toUpperCase().startsWith("/WEB-INF/")) {
                throw new JspException("Tag library descriptor files must always be "
                        + "in the \"WEB-INF\" directory, or some "
                        + "subdirectory of it.");
            }
        }
//    if (myLocation == null) {
//       throw new JspException("Cannot find tag library "+uri);
        RealTagLibrary rtl = null;
        try {
            rtl = new RealTagLibrary(myLocation, context.getServletLoader());
        } catch (JspException e) {
            if (myLocation == null) {
                rtl = new RealTagLibrary(location, context.getServletLoader());
            } else {
                throw e;
            }
        }
        if (listener != null) {
            rtl.registerTagLibraryListener(listener);
        }
        LinkedList listeners = rtl.getListeners();
        for (int i = 0; i < listeners.size(); i++) {
            context.addListener(listeners.get(i));
        }
        return rtl;
    }

}
