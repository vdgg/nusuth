package com.azoft.nusuth.webappsecurity.impl;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.util.StrBuffer;

import java.io.*;
import java.util.*;

import org.apache.log4j.Category;

class WebAppSecuritySettingsReader {
    public static int TRANSPORT_GUARANTEE_UNDEFINED = -1;
    public static int TRANSPORT_GUARANTEE_NONE = 0;
    public static int TRANSPORT_GUARANTEE_INTEGRAL = 1;
    public static int TRANSPORT_GUARANTEE_CONFIDENTIAL = 2;
    private Category logger = Category.getInstance("com.azoft.nusuth.webappsecurity");
    private HashMap users;
    private Map constraints;
    /** map: certificate name -> user name
     * all values are String
     */
    private Map cerificates2users;
    /** map: servlet name -> map: role name -> role link.
     * all values are String
     */
    private Map servletRoleMappings;
    /* Password for keystore "users.keys" that contains user certificates for SSL authentication */
    private String keystorePassword;

    private CompositeNusuthWebAppElement usersElement = null;

    public static WebAppSecuritySettingsReader read(File webApp, File users) {
        WebAppSecuritySettingsReader reader = new WebAppSecuritySettingsReader();
        reader.loadUsers(users);

        CompositeNusuthWebAppElement mainNode = reader.readWebAppConfigFile(webApp);
        reader.loadResourceConstraints(mainNode);
        reader.loadServletRoleMappings(mainNode);
        return reader;
    }

    public CompositeNusuthWebAppElement getUsersConfig() {
        return usersElement;
    }

    HashMap getUsers() {
        return users;
    }

    private void processUsers(CompositeNusuthWebAppElement configNode) {
        try {
            for (Enumeration i = configNode.getCompositeChild("user"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement userNode = (CompositeNusuthWebAppElement) i.nextElement();
                String name = ManagementUtil.getSimpleString(userNode, "name");
                if (name.length() > 0) {
                    String password = ManagementUtil.getSimpleString(userNode, "password");
                    String[] certificateNames = ManagementUtil.getSimpleStrings(userNode, "certificate-name");
                    AppUser appUser = (AppUser) users.get(name);
                    if (appUser == null) {
                        appUser = new AppUser(name, password, certificateNames);
                        users.put(name, appUser);
                    } else {
                        if (password != null) {
                            if (appUser.password == null) {
                                appUser.password = password;
                            } else {
                                logger.info("Second password for user \"" + name + "\"");
                            }
                        }
                        if (certificateNames != null) {
                            if (appUser.certificateNames == null) {
                                appUser.certificateNames = certificateNames;
                            } else {
                                String[] newSertificates = new String[certificateNames.length + appUser.certificateNames.length];
                                System.arraycopy(appUser.certificateNames, 0, newSertificates, 0, appUser.certificateNames.length);
                                System.arraycopy(certificateNames, 0, newSertificates, appUser.certificateNames.length, certificateNames.length);
                                appUser.certificateNames = newSertificates;
                            }
                        }
                    }
                    for (int j = 0; j < certificateNames.length; j++) {
                        cerificates2users.put(certificateNames[j], appUser);
                    }
                }
            }
        } catch (DeploymentException dex) {
            users = null;
            logger.error("Couldn't load users data", dex);
        }
    }

    private void loadResourceConstraints(CompositeNusuthWebAppElement mainNode) {
        Set allowedRoles = loadAllowedRoles(mainNode);
        constraints = processSecurityConstraints(mainNode, allowedRoles);
        if (constraints == null || constraints.size() == 0)
            constraints = null;
    }

    private HashSet processAuthConstraint(CompositeNusuthWebAppElement authNode, Set allowedRoles) throws DeploymentException {
        if (authNode != null) {
            HashSet result = new HashSet();
            for (Enumeration i = authNode.getSimpleChild("role-name"); i.hasMoreElements();) {
                String roleName = ((SimpleNusuthWebAppElement) i.nextElement()).getContent().trim();
                if (roleName.length() > 0 && (roleName.equals("*") || allowedRoles.contains(roleName))) {
                    for (Iterator j = users.values().iterator(); j.hasNext();) {
                        AppUser user = (AppUser) j.next();
                        if (roleName.equals("*") || user.roles.contains(roleName)) {
                            result.add(user.principal);
                        }
                    }
                }
            }
            return result;
        } else {
            return null;
        }
    }

    private int processUserDataConstraint(CompositeNusuthWebAppElement userDataConstraintNode) throws DeploymentException {
        if (userDataConstraintNode != null) {
            String transportGuarantee = ManagementUtil.getSimpleString(userDataConstraintNode, "transport-guarantee");
            if (transportGuarantee.equalsIgnoreCase("NONE")) {
                return TRANSPORT_GUARANTEE_NONE;
            } else if (transportGuarantee.equalsIgnoreCase("INTEGRAL")) {
                return TRANSPORT_GUARANTEE_INTEGRAL;
            } else if (transportGuarantee.equalsIgnoreCase("CONFIDENTIAL")) {
                return TRANSPORT_GUARANTEE_CONFIDENTIAL;
            } else {
                return TRANSPORT_GUARANTEE_UNDEFINED;
            }
        } else {
            return TRANSPORT_GUARANTEE_NONE;
        }
    }

    private void loadUsers(File usersFile) {
        try {
            keystorePassword = "";
            CompositeNusuthWebAppElement mainNode = NusuthAppConfigFactory.createConfig("web-app-users",
                    new FileInputStream(usersFile));
            usersElement = (CompositeNusuthWebAppElement) mainNode.clone();
            keystorePassword = ManagementUtil.getSimpleString(mainNode, "keystore-password");
            users = new HashMap();
            cerificates2users = new HashMap();
            processUsers(mainNode);
            processRoles(mainNode);
            if (users == null || users.size() == 0)
                users = null;
        } catch (ParserException ex) {
            users = null;
            logger.info("Couldn't load users data", ex);
        } catch (FileNotFoundException fnfex) {
            users = null;
            logger.info("Users data file not found, web app user authentication turned off. Nested: " + fnfex.getMessage());
        } catch (DeploymentException dex) {
            users = null;
            logger.warn("Couldn't understand users data file", dex);
        }
    }

    private void processRoles(CompositeNusuthWebAppElement configNode) {
        if (users == null)
            return;

        try {
            for (Enumeration i = configNode.getCompositeChild("role"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement roleNode = (CompositeNusuthWebAppElement) i.nextElement();
                String name = ManagementUtil.getSimpleString(roleNode, "name");
                if (name.length() > 0) {
                    for (Enumeration j = roleNode.getSimpleChild("member"); j.hasMoreElements();) {
                        String member = ((SimpleNusuthWebAppElement) j.nextElement()).getContent();
                        AppUser appUser = (AppUser) users.get(member);
                        if (appUser != null) {
                            appUser.roles.add(name);
                        } else {
                            logger.error("User \"" + member + "\" defined in role \"" + name + "\" is unknown");
                        }
                    }
                }
            }
        } catch (DeploymentException dex) {
            users = null;
            logger.error("Couldn't load users data", dex);
        }
    }

    Map getConstraints() {
        return constraints;
    }

    private Map processSecurityConstraints(CompositeNusuthWebAppElement appNode, Set allowedRoles) {
        try {
            Enumeration securityConstraintNodes = appNode.getCompositeChild("security-constraint");
            if (securityConstraintNodes.hasMoreElements()) {
                Map result = new HashMap();
                while (securityConstraintNodes.hasMoreElements()) {
                    CompositeNusuthWebAppElement constraintNode = (CompositeNusuthWebAppElement) securityConstraintNodes.nextElement();
                    int transport = processUserDataConstraint(ManagementUtil.getCompositeElement(constraintNode,
                            "user-data-constraint"));
                    boolean isSSLNeeded = transport == TRANSPORT_GUARANTEE_INTEGRAL || transport == TRANSPORT_GUARANTEE_CONFIDENTIAL;
                    HashSet allowedUsers = processAuthConstraint(ManagementUtil.getCompositeElement(constraintNode, "auth-constraint"), allowedRoles);
                    processWebResources(isSSLNeeded, allowedUsers, constraintNode, result);
                }
                return result;
            } else {
                return null;
            }
        } catch (DeploymentException dex) {
            logger.error("Couldn't load security constraints", dex);
            return null;
        }
    }

    private void processWebResources(boolean isSSLNeeded, HashSet allowedUsers, CompositeNusuthWebAppElement constraintNode, Map result)
            throws DeploymentException {
        for (Enumeration i = constraintNode.getCompositeChild("web-resource-collection"); i.hasMoreElements();) {
            CompositeNusuthWebAppElement resNode = (CompositeNusuthWebAppElement) i.nextElement();
            HashMap currConstraints = new HashMap();
            for (Enumeration j = resNode.getSimpleChild("http-method"); j.hasMoreElements();) {
                SimpleNusuthWebAppElement methodNode = (SimpleNusuthWebAppElement) j.nextElement();
                String methodString = methodNode.getContent();
                StrBuffer method = new StrBuffer(methodString.length());
                method.append(methodString);
                currConstraints.put(method,
                        new ResourceConstraint(isSSLNeeded, allowedUsers));
            }
            if (currConstraints.size() == 0) {
                currConstraints.put(WebAppSecurityManagerImpl.ANY_HTTP_METHOD,
                        new ResourceConstraint(isSSLNeeded, allowedUsers));
            }
            Vector patterns = new Vector();
            for (Enumeration j = resNode.getSimpleChild("url-pattern"); j.hasMoreElements();) {
                SimpleNusuthWebAppElement patternNode = (SimpleNusuthWebAppElement) j.nextElement();
                UrlPattern pattern = new UrlPattern(patternNode.getContent(), currConstraints);
                patterns.add(pattern);
            }
            putResourceConstraintsToUrlPatterns(currConstraints, patterns, result);
        }
    }

    private void putResourceConstraintsToUrlPatterns(Map newConstraints, Collection patterns, Map result) {
        for (Iterator i = patterns.iterator(); i.hasNext();) {
            UrlPattern pattern = (UrlPattern) i.next();
            Map oldConstraints = (Map) result.get(pattern.pattern);
            if (oldConstraints == null) {
                oldConstraints = new HashMap();
                result.put(pattern.pattern, oldConstraints);
            }
            oldConstraints.putAll(newConstraints);
        }
    }

    /** processes "security-role-ref" element in web application config (web.xml)
     * fills servletRoleMappings
     */
    private void loadServletRoleMappings(CompositeNusuthWebAppElement mainNode) {
        try {
            //read URL mappings
            Map urlMappings = new HashMap();
            for (Enumeration i = mainNode.getCompositeChild("servlet-mapping"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement servlet = (CompositeNusuthWebAppElement) i.nextElement();
                String urlPattern = ManagementUtil.getSimpleString(servlet, "url-pattern");
                if (urlPattern.endsWith("*"))
                    urlPattern = urlPattern.substring(0, urlPattern.length() - 1);
                if (urlPattern.endsWith("/"))
                    urlPattern = urlPattern.substring(0, urlPattern.length() - 1);
                String servletName = ManagementUtil.getSimpleString(servlet, "servlet-name");
                urlMappings.put(servletName, urlPattern);
            }

            // read role mappings
            servletRoleMappings = null;
            HashMap result = new HashMap();
            for (Enumeration i = mainNode.getCompositeChild("servlet"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement servlet = (CompositeNusuthWebAppElement) i.nextElement();
                String name = ManagementUtil.getSimpleString(servlet, "servlet-name");
                HashMap roles = new HashMap();
                for (Enumeration j = servlet.getCompositeChild("security-role-ref"); j.hasMoreElements();) {
                    CompositeNusuthWebAppElement role_ref = (CompositeNusuthWebAppElement) j.nextElement();
                    roles.put(ManagementUtil.getSimpleString(role_ref, "role-name"),
                            ManagementUtil.getSimpleString(role_ref, "role-link"));
                }
                if (roles.size() > 0) {
                    String url = (String) urlMappings.get(name);
                    if (url != null)
                        result.put(url, roles);
                }
            }
            servletRoleMappings = result;
        } catch (DeploymentException dex) {
            logger.error("Couldn't parse servlet's role mappings", dex);
            return;
        }
    }

    Map getServletRoleMappings() {
        return servletRoleMappings;
    }

    private CompositeNusuthWebAppElement readWebAppConfigFile(File wabAppFile) {
        try {
            CompositeNusuthWebAppElement mainNode = NusuthAppConfigFactory.createConfig("web-app", new FileInputStream(wabAppFile));
            return mainNode;
        } catch (ParserException ex) {
            constraints = null;
            logger.info("Couldn't load web-app config", ex);
            return null;
        } catch (FileNotFoundException fnfex) {
            constraints = null;
            logger.info("Web-app config file not found, web app authorization turned off. Nested: " + fnfex);
            return null;
        }
    }

    private Set loadAllowedRoles(CompositeNusuthWebAppElement mainNode) {
        Set result = new HashSet();
        try {
            for (Enumeration roleNodes = mainNode.getCompositeChild("security-role"); roleNodes.hasMoreElements();) {
                CompositeNusuthWebAppElement roleNode = (CompositeNusuthWebAppElement) roleNodes.nextElement();
                String roleName = ManagementUtil.getSimpleString(roleNode, "role-name");
                result.add(roleName);
            }
        } catch (DeploymentException dex) {
            logger.error("Couldn't load allowed roles", dex);
        }
        return result;
    }

    protected Map getCertificates2Users() {
        return cerificates2users;
    }

    protected String getKeystorePassword() {
        return keystorePassword;
    }
}
