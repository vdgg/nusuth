package com.azoft.nusuth.webappsecurity.impl;

import org.apache.log4j.Category;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.util.StrBuffer;
import com.azoft.nusuth.webappsecurity.ResourceSecurityRecord;
import com.azoft.nusuth.webappsecurity.WebAppSecurityManager;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Principal;
import java.util.Map;
import javax.security.cert.Certificate;

/**
 * Realization of WebAppSecurityManager. Constructs from web-app root folder
 * and assumes that <code>WEB-INF</code> folder contains files
 * <code>web.xml</code>, <code>users.xml</code> and <code>users.keys</code>.
 *
 * <p>
 * If <code>web.xml</code> not presented, than
 * {@link #checkAccessRights checkAccessRights} will always return
 * {@link #ACCESS_GRANTED ACCESS_GRANTED}. <br>
 *
 * If <code>users.xml</code> not presented, than
 * {@link #login(String, String) login(String, String)} will
 * always return <code>null</code>, and {@link #isUserInRole isUserInRole} will
 * always return false.<br>
 *
 * If <code>users.xml</code> or <code>users.keys</code> not presented, than
 * {@link #login(Certificate) login(Certificate)} will
 * always return <code>null</code>.
 */
public class WebAppSecurityManagerImpl implements WebAppSecurityManager {
    protected final static StrBuffer ANY_HTTP_METHOD;
    protected final static StrBuffer slash;
    private Map users;
    private Map certificates2users;
    /**
     * map: uri -> HTTP method ->
     * {@link ResourceConstraint ResourceConstraint}.<br>
     * Getted by {@link WebAppSecuritySettingsReader WebAppSecuritySettingsReader}
     */
    private Map resourceConstraints;
    private Map servletRoleMappings;
    private KeyStore keystore;
    private File keystoreFile;
    private File usersFile;
    private Category logger = Category.getInstance(this.getClass().getName());
    private boolean isResourcesInitialized;
    private boolean isUsersInitialized;
    private CertStorage certStorage;
    private WebAppSecuritySettingsReader reader = null;

    static {
        ANY_HTTP_METHOD = new StrBuffer(1);
        ANY_HTTP_METHOD.append('*');
        slash = new StrBuffer(1);
        slash.append('/');
    }

    public WebAppSecurityManagerImpl(File appRootDirectory) {
        NusuthAppConfigFactory.addEntityResolver("web-app-users",
                new WebAppUsersEntityResolver());
        NusuthAppConfigFactory.addEntityResolver("web-app", new WebEntityResolver());
        reader = WebAppSecuritySettingsReader.read(
                new File(appRootDirectory, "WEB-INF/web.xml"),
                new File(appRootDirectory, "WEB-INF/users.xml"));
        keystoreFile = new File(appRootDirectory, "WEB-INF/users.keys");
        String keystorePassword = reader.getKeystorePassword();
        if (keystoreFile.exists() && keystorePassword.length() > 0) {
            try {
                keystore = KeyStore.getInstance("JKS");
                keystore.load(new FileInputStream(keystoreFile),
                        keystorePassword.toCharArray());
            } catch (Exception ex) {
                keystore = null;
                logger.info("Couldn't load keystore, nested: " + ex.getMessage());
            }
        } else {
            keystore = null;
        }

        users = reader.getUsers();
        certStorage = new CertStorage(keystore, reader.getCertificates2Users());
        isUsersInitialized = users != null && users.size() > 0;
        resourceConstraints = reader.getConstraints();
        isResourcesInitialized = resourceConstraints != null
                && resourceConstraints.size() > 0;
        servletRoleMappings = reader.getServletRoleMappings();
    }

    public CompositeNusuthWebAppElement getUsersConfig() {
        return reader.getUsersConfig();
    }

    public Principal login(Certificate userCertificate) {
        if (!isUsersInitialized || keystore == null || userCertificate == null)
            return null;

        AppUser user = certStorage.getUser(userCertificate);
        return user != null ? user.principal : null;
    }

    public Principal login(String userName, String encodedPassword) {
        if (!isUsersInitialized)
            return null;

        AppUser user = (AppUser) users.get(userName);
        if (user != null && user.password.equals(encodedPassword)) {
            return user.principal;
        }
        return null;
    }

    public int checkAccessRights(ResourceSecurityRecord resource,
                                 boolean isSecure,
                                 Principal user) {
        if (!isResourcesInitialized)
            return ACCESS_GRANTED;

        StrBuffer uri = resource.getPath2Resource();
        StrBuffer method = resource.getMethod();
        Map constraints = findConstraints(uri);
        if (constraints == null) {
            return ACCESS_GRANTED;
        }
        ResourceConstraint constr = (ResourceConstraint) constraints.get(method);
        if (constr == null) {
            constr = (ResourceConstraint) constraints.get(ANY_HTTP_METHOD);
        }
        return constr == null
                ? ACCESS_GRANTED
                : constr.checkRequest(isSecure, user);
    }

    public boolean isUserInRole(String servletName, Principal user, String role) {
        //System.out.println("isUserInRole(servletName = " + servletName);
        if (user == null || users == null || !isUsersInitialized) {
            return false;
        }
        AppUser appUser = (AppUser) users.get(user.getName());
        if (appUser == null) {
            return false;
        }

        Map mappings = (Map) servletRoleMappings.get(servletName);
        if (mappings != null)
            return appUser.roles.contains(mappings.get(role));
        else
            return appUser.roles.contains(role);
    }

    private Map findConstraints(StrBuffer url) {
        if (resourceConstraints == null)
            return null;

        if (url.length() == 0) {
            return (Map) resourceConstraints.get(slash);
        } else {
            StrBuffer name = url.cloneBuf();
            do {
                Object result = resourceConstraints.get(name);
                if (result != null)
                    return (Map) result;
                name.cutToChar('/', true);
            } while (name.length() > 0);
            return (Map) resourceConstraints.get(slash);
        }
    }

    public boolean isEnabled() {
        return resourceConstraints != null && resourceConstraints.size() > 0;
    }
}

