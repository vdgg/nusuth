package com.azoft.nusuth.management;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.deployment.DeployerEntityResolver;
import com.azoft.nusuth.deployment.DeploymentException;
import com.azoft.nusuth.deployment.NusuthAppConfigFactory;
import com.azoft.nusuth.management.rmi.*;
import com.azoft.nusuth.management.security.AdminPortListener;
import com.azoft.nusuth.util.LogCategoryProxy;

import java.io.*;
import java.rmi.RemoteException;
import java.security.*;
import java.util.*;
import java.util.zip.*;

import org.apache.log4j.Category;

/**
 * Deployer class
 * @author vdgg, igork
 * @since Nusuth1.0
 * @version 1.7
 */
public class DeployerImpl
        extends ComponentManagerImpl
        implements Deployer {
    private HashMap containers = new HashMap();

    public DeployerImpl(String newConfigFileName)
            throws ManagementException, DeploymentException, FileNotFoundException {
        NusuthAppConfigFactory.addEntityResolver(getComponentType(),
                new DeployerEntityResolver());
        configFileName
                = ManagementUtil.getConfigFile(newConfigFileName, "deployer.xml").
                getAbsolutePath();
        InputStream config
                = new BufferedInputStream(new FileInputStream(configFileName));
        this.settings
                = NusuthAppConfigFactory.createConfig(getComponentType(), config);
        loadLogger(ManagementUtil.getCompositeElement(settings, "logger"));
        logger.info("Starting deployer");
        CompositeNusuthWebAppElement managerNode
                = ManagementUtil.getCompositeElement(settings, "manager");
        listener = new AdminPortListener(settings, this);
        listener.start();
        setComponentId(ManagementUtil.getSimpleString(this.settings, "name"));
        logger.info("Deployer started");
    }

    private InputStream getApplicationStream(VirtualHostInfo host,
                                             String appName,
                                             HashMap failed,
                                             HashMap notFinded)
            throws ManagementException {
        String hostName = host.getName();
        File docBase = new File(host.getDocBase());
        if (!docBase.exists())
            docBase = new File(jbirdHome, host.getDocBase());
        DeployerApplicationInfo app = (DeployerApplicationInfo) host.getApplications().get(appName);
        String appLocation = app.getLocation();

        HashSet appContainers = app.getContainers();

        File appSrc = new File(docBase, appLocation);

        if (appSrc.exists()) {
            InputStream srcStream = null;
            if (appSrc.isFile()) {
                try {
                    srcStream = new BufferedInputStream(new FileInputStream(appSrc));
                } catch (FileNotFoundException fnfex) {
                    logger.error("File \"" + appSrc.getAbsolutePath() + "\" (which contain application \"" + hostName + '/' + appName + "\") not found.");
                    throw new ManagementException("File \"" + appSrc.getAbsolutePath() + "\" (which contain application \"" + hostName + '/' + appName + "\") not found.");
                }
            } else if (appSrc.isDirectory()) {
                try {
                    srcStream = zipDir(hostName, docBase, appLocation);
                } catch (IOException ioex) {
                    logger.error("Couldn't zip directory \"" + appSrc.getAbsolutePath() + "\"", ioex);
                    throw new ManagementException("Couldn't zip directory \"" + appSrc.getAbsolutePath() + "\", nested:" + ioex.getMessage());
                }
            }
            return srcStream;
        }

        processError(host, appName, appContainers, notFinded);
        return null;
    }

    private void addApplication(String hostName,
                                String appUrl,
                                InputStream applicationStream,
                                HashSet containerIds)
            throws ManagementException {
        Vector notFinded = new Vector();
        Vector failed = new Vector();
        for (Iterator i = containerIds.iterator(); i.hasNext();) {
            String curr = (String) i.next();
            ContainerManager cmanager = getContainerStub(curr);
            if (cmanager != null) {
                try {
                    cmanager.addApplication(hostName, appUrl, applicationStream);
                } catch (ManagementException mex) {
                    failed.add(curr);
                    forgetContainerStub(curr);
                }
            } else
                notFinded.add(curr);
        }
        if (!notFinded.isEmpty() || !failed.isEmpty()) {
            logger.error("Couldn't find containers to deploy application \"" + appUrl + "\": not finded " + notFinded + " and failed " + failed);
            throw new DeploingException("Couldn't find containers to deploy application \"" + appUrl + "\"", notFinded, failed);
        }
    }


    public void addApplication(Vector hosts)
            throws ManagementException {
        logger.debug("Add application");
        HashMap failed = new HashMap();
        HashMap notFinded = new HashMap();

        for (Iterator i = hosts.iterator(); i.hasNext();) {
            VirtualHostInfo host = (VirtualHostInfo) i.next();
            String hostName = host.getName();
            for (Iterator j = host.getApplications().keySet().iterator(); j.hasNext();) {
                String appName = (String) j.next();
                InputStream srcStream = getApplicationStream(host, appName, failed, notFinded);

                if (srcStream != null) {
                    DeployerApplicationInfo app = (DeployerApplicationInfo) host.getApplications().get(appName);
                    HashSet appContainers = app.getContainers();
                    try {
                        addApplication(hostName, appName, srcStream, appContainers);
                    } catch (DeploingException dex) {
                        if (dex.failedContainers != null && dex.failedContainers.size() > 0)
                            processError(host, appName, dex.failedContainers, failed);
                        if (dex.notFindedContainers != null && dex.notFindedContainers.size() > 0)
                            processError(host, appName, dex.notFindedContainers, notFinded);
                    }
                }
            }
        }
        if (!notFinded.isEmpty() || !failed.isEmpty()) {
            logger.error("Couldn't deploy: not finded " + notFinded.values() + " and failed " + failed.values());
            throw new DeploingException("Couldn't deploy", notFinded.values(), failed.values());
        }
    }


    public final String getComponentType() {
        return "deployer";
    }


    private ContainerManager getContainerStub(String containerId) {
        logger.debug("Get container \"" + containerId + "\" stub");
        ContainerManager result = (ContainerManager) containers.get(containerId);
        if (result == null) {
            try {
                result = listener.getContainerStub(ManagementUtil.getSimpleString(settings, "name"), containerId);
            } catch (DeploymentException dex) {
                result = null;
            }
            if (result != null)
                containers.put(containerId, result);
        }
        return result;
    }


    private void patchApplication(String hostName,
                                  String appUrl,
                                  InputStream applicationStream,
                                  boolean overwrite,
                                  HashSet containerIds)
            throws ManagementException {
        Vector notFinded = new Vector();
        Vector failed = new Vector();
        for (Iterator i = containerIds.iterator(); i.hasNext();) {
            String curr = (String) i.next();
            ContainerManager cmanager = getContainerStub(curr);
            if (cmanager != null) {
                try {
                    cmanager.patchApplication(hostName, appUrl, applicationStream, overwrite);
                } catch (ManagementException mex) {
                    failed.add(curr);
                    forgetContainerStub(curr);
                }
            } else
                notFinded.add(curr);
        }
        if (!notFinded.isEmpty() || !failed.isEmpty()) {
            logger.error("Couldn't find containers to patch application \"" + appUrl + "\": not finded " + notFinded + " and failed " + failed);
            throw new DeploingException("Couldn't find containers to patch application \"" + appUrl + "\"", notFinded, failed);
        }
    }


    public void patchApplication(Vector hosts, boolean overwrite)
            throws ManagementException {
        logger.debug("Patch application");
        HashMap failed = new HashMap();
        HashMap notFinded = new HashMap();

        for (Iterator i = hosts.iterator(); i.hasNext();) {
            VirtualHostInfo host = (VirtualHostInfo) i.next();
            String hostName = host.getName();
            for (Iterator j = host.getApplications().keySet().iterator(); j.hasNext();) {
                String appName = (String) j.next();
                InputStream srcStream = getApplicationStream(host, appName, failed, notFinded);
                if (srcStream != null) {
                    try {
                        DeployerApplicationInfo app = (DeployerApplicationInfo) host.getApplications().get(appName);
                        HashSet appContainers = app.getContainers();
                        patchApplication(hostName, appName, srcStream, overwrite, appContainers);
                    } catch (DeploingException dex) {
                        if (dex.failedContainers != null && dex.failedContainers.size() > 0)
                            processError(host, appName, dex.failedContainers, failed);
                        if (dex.notFindedContainers != null && dex.notFindedContainers.size() > 0)
                            processError(host, appName, dex.notFindedContainers, notFinded);
                    }
                } else
                    processError(host, appName, null, notFinded);
            }
        }
        if (!notFinded.isEmpty() || !failed.isEmpty()) {
            logger.error("Couldn't patch: not finded " + notFinded.values() + " and failed " + failed.values());
            throw new DeploingException("Couldn't patch", notFinded.values(), failed.values());
        }
    }


    private void replaceContent(String hostName,
                                String appUrl,
                                InputStream applicationStream,
                                HashSet containerIds)
            throws ManagementException {
        Vector notFinded = new Vector();
        Vector failed = new Vector();
        for (Iterator i = containerIds.iterator(); i.hasNext();) {
            String curr = (String) i.next();
            ContainerManager cmanager = getContainerStub(curr);
            if (cmanager != null) {
                try {
                    cmanager.replaceApplicationContent(hostName, appUrl, applicationStream);
                } catch (ManagementException mex) {
                    failed.add(curr);
                    forgetContainerStub(curr);
                }
            } else
                notFinded.add(curr);
        }
        if (!notFinded.isEmpty() || !failed.isEmpty()) {
            logger.error("Couldn't find containers to replace content \"" + appUrl + "\": not finded " + notFinded + "and failed " + failed);
            throw new DeploingException("Couldn't find containers to replace content \"" + appUrl + "\"", notFinded, failed);
        }
    }


    public void replaceContent(Vector hosts)
            throws ManagementException {
        logger.debug("Replace content");
        HashMap failed = new HashMap();
        HashMap notFinded = new HashMap();

        for (Iterator i = hosts.iterator(); i.hasNext();) {
            VirtualHostInfo host = (VirtualHostInfo) i.next();
            String hostName = host.getName();
            for (Iterator j = host.getApplications().keySet().iterator(); j.hasNext();) {
                String appName = (String) j.next();
                InputStream srcStream = getApplicationStream(host, appName, failed, notFinded);
                if (srcStream != null) {
                    try {
                        DeployerApplicationInfo app = (DeployerApplicationInfo) host.getApplications().get(appName);
                        HashSet appContainers = app.getContainers();
                        replaceContent(hostName, appName, srcStream, appContainers);
                    } catch (DeploingException dex) {
                        if (dex.failedContainers != null && dex.failedContainers.size() > 0)
                            processError(host, appName, dex.failedContainers, failed);
                        if (dex.notFindedContainers != null && dex.notFindedContainers.size() > 0)
                            processError(host, appName, dex.notFindedContainers, notFinded);
                    }
                } else
                    processError(host, appName, null, notFinded);
            }
        }
        if (!notFinded.isEmpty() || !failed.isEmpty()) {
            logger.error("Couldn't replace content: not finded " + notFinded.values() + " and failed " + failed.values());
            throw new DeploingException("Couldn't replace content", notFinded.values(), failed.values());
        }
    }


    public void setSettings(InputStream newSettings)
            throws ManagementException {
        logger.info("Apply new settings");
        try {
            CompositeNusuthWebAppElement newNode = NusuthAppConfigFactory.createConfig(getComponentType(), newSettings);

            //  loadLogger(getCompositeElement(settings, "logger"));

            if (listener.isRestartNeeded(newNode)) {
                listener.stopListener();
                listener = new AdminPortListener(newNode, this);
                listener.start();
            } else
                listener.applySettings(newNode);

            settings = newNode;
            saveSettings();

        } catch (Exception ex) {
            logger.error("Couldn't apply new settings", ex);
            throw new ManagementException("Couldn't apply new settings, nested:" + ex.getMessage());
        }
    }


    private InputStream zipDir(String hostName,
                               File docBase,
                               String location)
            throws IOException, ManagementException {
        File temp = File.createTempFile("dep", ".tmp");
        temp.deleteOnExit();
        ZipOutputStream zstream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(temp)));
        zip(docBase, location, zstream);
        zstream.flush();
        zstream.close();
        return new BufferedInputStream(new FileInputStream(temp));
    }


    private InputStream extractFile(File zippedFile, String fileNameToExtract)
            throws ManagementException {
        try {
            ZipInputStream zistream = new ZipInputStream(new FileInputStream(zippedFile));
            File etalon = new File(fileNameToExtract);
            for (ZipEntry entry = zistream.getNextEntry(); entry != null; entry = zistream.getNextEntry()) {
                if (etalon.equals(new File(entry.getName())))
                    return zistream;
            }
        } catch (IOException ioex) {
            logger.error("Couldn't extract \"" + fileNameToExtract + "\" from \"" + zippedFile.getAbsolutePath() + "\"", ioex);
            throw new ManagementException("Couldn't extract \"" + fileNameToExtract + "\" from \"" + zippedFile.getAbsolutePath() + "\", nested :" + ioex.getMessage());
        }
        logger.error("Couldn't extract \"" + fileNameToExtract + "\" from \"" + zippedFile.getAbsolutePath() + "\" - entry not found");
        throw new ManagementException("Couldn't extract \"" + fileNameToExtract + "\" from \"" + zippedFile.getAbsolutePath() + "\" - entry not found");
    }


    private void forgetContainerStub(String container) {
        containers.remove(container);
    }


    public InputStream getWebInf(String docBase, String location)
            throws ManagementException {
        logger.debug("Get web-inf \"" + location + '"');
        File appSrc = new File(docBase, location);
        if (!appSrc.exists()) {
            appSrc = new File(new File(jbirdHome, docBase), location);
        }
        if (!appSrc.exists()) {
            logger.error("Application not found");
            throw new ManagementException("Application not found");
        }

        InputStream infSrc = null;
        if (appSrc.isDirectory()) {
            try {
                infSrc = new BufferedInputStream(new FileInputStream(new File(appSrc, "WEB-INF" + File.separator + "web.xml")));
            } catch (FileNotFoundException fnfex) {
                logger.error("Application not found");
                throw new ManagementException("Application not found");
            }
        } else if (appSrc.isFile()) {
            infSrc = extractFile(appSrc, "WEB-INF" + File.separator + "web.xml");
        } else {
            logger.error("Function not yet implemented");
            throw new ManagementException("Function not yet implemented");
        }

        try {
            File temp = File.createTempFile("deployer", ".tmp");
            ZipOutputStream otemp = new ZipOutputStream(new FileOutputStream(temp));
            otemp.putNextEntry(new ZipEntry("WEB-INF" + File.separator + "web.xml"));
            byte[] buf = new byte[2048];
            for (int readed = infSrc.read(buf); readed > -1; readed = infSrc.read(buf))
                otemp.write(buf, 0, readed);
            otemp.closeEntry();
            otemp.close();

            return new BufferedInputStream(new FileInputStream(temp));
        } catch (IOException ioex) {
            logger.error("Couldn't zip web.xml", ioex);
            throw new ManagementException("Couldn't zip web.xml, nested:" + ioex.getMessage());
        }
    }


    private void processError(VirtualHostInfo host,
                              String appName,
                              Collection failedContainers,
                              HashMap resultFailedHosts) {
        String hostName = host.getName();
        if (host == null) {
            host = new VirtualHostInfo(hostName, "", new HashMap(1));
            resultFailedHosts.put(hostName, host);
        }
        DeployerApplicationInfo dapp = (DeployerApplicationInfo) host.getApplications().get(appName);
        if (dapp == null) {
            dapp = new ApplicationInfoImpl(true, new HashSet(failedContainers == null ? 0 : failedContainers.size()), null, "[unknown]");
            host.getApplications().put(appName, dapp);
        }
        if (failedContainers == null)
            failedContainers = dapp.getContainers();
        dapp.getContainers().addAll(failedContainers);
    }


    private void zip(File base, String location, ZipOutputStream stream)
            throws IOException {
        File file = new File(base, location);
        if (file.isDirectory()) {
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                zip(base, location + File.separator + files[i], stream);
            }
        } else {
            ZipEntry entry = new ZipEntry(location);
            stream.putNextEntry(entry);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            byte[] buf = new byte[65536];
            for (int readed = bis.read(buf); readed > 0;) {
                stream.write(buf, 0, readed);
                readed = bis.read(buf);
            }
            stream.closeEntry();
        }
    }
}
