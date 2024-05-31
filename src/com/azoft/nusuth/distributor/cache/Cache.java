package com.azoft.nusuth.distributor.cache;

import com.azoft.nusuth.util.*;
import com.azoft.nusuth.core.FileServlet;
import com.azoft.nusuth.management.Manageable;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.deployment.*;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.*;

import org.apache.log4j.Category;

public class Cache
        implements Manageable {
    public final static char[][] deprecatedHeaders = {HttpConstants.TRANSFER_ENCODING,
                                                      HttpConstants.DATE,
                                                      HttpConstants.CONNECTION,
                                                      HttpConstants.COOKIE
    };

    Category logger = Category.getInstance(this.getClass().getName());
    LogCategoryProxy logProxy = LogCategoryProxy.getInstance(this.getClass().getName());

    private File cacheDirectory;

    /**
     * @clientCardinality 1
     * @supplierCardinality 1
     */
    private EvictQueue evictQueue = new EvictQueue();

    /**
     *@link aggregation
     *      @associates <{CachedElement}>
     * @clientCardinality 1
     * @supplierCardinality 0..*
     */
    private Hashtable cachedElements = new Hashtable();

    private boolean enabled = false;

    private int usedMem;
    private long usedDisk;
    private int maxMem;
    private long maxDisk;
    private int maxPage;
    private long minRefresh;


    public synchronized CachedElement getPage(StrBuffer url) {
        if (!enabled)
            return null;

        if (logProxy.isDebugEnabled())
            logger.debug("getPage(\"" + url + "\")");

        CachedElement elem = (CachedElement) cachedElements.get(url);
        if (elem == null)
            return null;

        if (!elem.isLoaded())
            loadPage(elem);
        //evictQueue.touch(elem);
        return elem;
    }

    public synchronized void putPage(StrBuffer url, NusuthHeaders headers, byte[] body)
            throws IOException {
        if (!enabled)
            return;

        if (logProxy.isDebugEnabled())
            logger.debug("putPage(\"" + url + "\", NusuthHeaders[" + headers.length() + "], byte[" + body.length + "])");

        StrBuffer url_ = new StrBuffer(url.length());
        url_.append(url);

        CachedElement elem = (CachedElement) cachedElements.get(url_);

        NusuthHeaders h = new NusuthHeaders();
        for (Enumeration i = headers.getHeaderNames(); i.hasMoreElements();) {
            String name = (String) i.nextElement();
            for (Enumeration j = headers.getHeaders(name); j.hasMoreElements();) {
                h.addHeader(name, (String) j.nextElement());
            }
        }

        for (int i = 0; i < deprecatedHeaders.length; i++)
            h.clearHeader(deprecatedHeaders[i]);

        h.putIntHeader(String.valueOf(HttpConstants.CONTENT_LENGTH), body.length);

        if (elem == null) {
            elem = new CachedElement(url_, h, body, cacheDirectory);
            cachedElements.put(url_, elem);
            evictQueue.add(elem);
            if (logProxy.isDebugEnabled())
                logger.debug("putPage(\"" + url + "\", NusuthHeaders[" + headers.length() + "], byte[" + body.length + "]) - new page");
        } else {
            usedMem -= getPageMemSize(elem);
            usedDisk -= getPageDiskSize(elem);
            elem.update(h, body, cacheDirectory);
            evictQueue.touch(elem);
            if (logProxy.isDebugEnabled())
                logger.debug("putPage(\"" + url + "\", NusuthHeaders[" + headers.length() + "], byte[" + body.length + "]) - existing page");
        }

        usedMem += getPageMemSize(elem);
        usedDisk += getPageDiskSize(elem);
        checkMem();
        checkDisk();
    }

    private void checkMem() {
        if (logProxy.isDebugEnabled())
            logger.debug("check memory usage limits...");

        while (usedMem > maxMem) {
            unloadPage(evictQueue.unload());
        }
    }

    private void checkDisk() {
        if (logProxy.isDebugEnabled())
            logger.debug("check disk usage limits...");

        while (usedDisk > maxDisk) {
            CachedElement elem = evictQueue.remove();
            cachedElements.remove(elem.getUrl());
            deletePage(elem);
        }
    }

    private int getPageMemSize(CachedElement elem) {
        if (elem.isLoaded())
            return elem.getBody().length;
        else
            return 0;
    }

    private long getPageDiskSize(CachedElement elem) {
        return elem.file.length();
    }

    private void loadPage(CachedElement elem) {
        if (elem != null && !elem.isLoaded()) {
            if (logProxy.isDebugEnabled())
                logger.debug("loadPage(\"" + elem.getUrl() + "\")");

            try {
                elem.load();
                usedMem += getPageMemSize(elem);
            } catch (IOException ioex) {
                logger.warn("Couldn't load page \"" + elem.getUrl() + '"', ioex);
                deletePage(elem);
            }
        }
    }

    private void unloadPage(CachedElement elem) {
        if (logProxy.isDebugEnabled())
            logger.debug("unloadPage(\"" + elem.getUrl() + "\")");

        if (elem != null && elem.isLoaded()) {
            usedMem -= getPageMemSize(elem);
            elem.unload();
        }
    }

    private void deletePage(CachedElement elem) {
        if (logProxy.isDebugEnabled())
            logger.debug("deletePage(\"" + elem.getUrl() + "\")");

        unloadPage(elem);
        usedDisk -= getPageDiskSize(elem);
        elem.delete();
    }

    // implements Manageable

    public synchronized void applySettings(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        logger.info("Applying new settings");
        CompositeNusuthWebAppElement node = ManagementUtil.getCompositeElement(settings, "cache");
        if (node != null) {
            String cacheDir = ManagementUtil.getSimpleString(node, "location");

            File newCacheDirectory = new File(cacheDir);
            boolean reInitNeeds = !newCacheDirectory.equals(cacheDirectory);
            cacheDirectory = newCacheDirectory;
            if (!cacheDirectory.exists())
                cacheDirectory.mkdirs();
            if (!cacheDirectory.exists() || !cacheDirectory.isDirectory())
                throw new DeploymentException("Couldn't find or create cache dir \"" + cacheDir + '"');

            maxMem = ManagementUtil.getSimpleInt(node, "max-used-memory-size");
            maxDisk = ManagementUtil.getSimpleInt(node, "max-used-disk-size");
            maxPage = ManagementUtil.getSimpleInt(node, "max-page-size");
            minRefresh = ManagementUtil.getSimpleTime(node, "min-refresh-time");

            if (reInitNeeds)
                reInit();

            checkMem();
            checkDisk();
            enabled = true;
            logger.debug("New settings applied");
        } else {
            logger.warn("Cache initializing parameters not presenting in config. Cache is disabled");
            enabled = false;
        }
    }

    public synchronized boolean isRestartNeeded(CompositeNusuthWebAppElement settings)
            throws DeploymentException {
        return false;
    }

    public void reInit() {
        if (logProxy.isDebugEnabled())
            logger.debug("reInit cache");

        evictQueue.clear();
        cachedElements.clear();
        File[] cachedPages = cacheDirectory.listFiles();
        for (int i = 0; i < cachedPages.length; i++) {
            try {
                CachedElement elem = new CachedElement(cachedPages[i]);
                cachedElements.put(elem.getUrl(), elem);
                evictQueue.add(elem);
                usedDisk += getPageDiskSize(elem);
            } catch (IOException ioex) {
                logger.warn("Couldn't load cached page from file \"" + cachedPages[i].getAbsolutePath() + '"', ioex);
                cachedPages[i].delete();
            }
        }
    }


    public synchronized long getMinRefreshTime() {
        return enabled ? minRefresh : Long.MAX_VALUE;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public boolean isPageExpired(CachedElement elem, long requestIfModifiedSince) {
        return
                elem.getLastModified() < requestIfModifiedSince ||
                elem.getLastChecked() + minRefresh < System.currentTimeMillis();
    }

    public void touchPage(CachedElement elem) {
        if (!enabled)
            return;

        if (logProxy.isDebugEnabled())
            logger.debug("touchPage(\"" + elem.getUrl() + "\")");

        evictQueue.touch(elem);
    }

    public void removePage(CachedElement elem) {
        if (!enabled)
            return;

        if (logProxy.isDebugEnabled())
            logger.debug("removePage(\"" + elem.getUrl() + "\")");

        usedMem -= getPageMemSize(elem);
        usedDisk -= getPageDiskSize(elem);
        cachedElements.remove(elem.getUrl());
        evictQueue.remove(elem);
    }

    public int getMaxPageSize() {
        return enabled ? maxPage : Integer.MIN_VALUE;
    }
}

