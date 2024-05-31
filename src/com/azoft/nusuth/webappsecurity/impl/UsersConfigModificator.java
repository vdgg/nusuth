package com.azoft.nusuth.webappsecurity.impl;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.gui.MD5;

import java.util.*;

public class UsersConfigModificator {
    private CompositeNusuthWebAppElement configNode;

    public UsersConfigModificator(CompositeNusuthWebAppElement configNode) {
        this.configNode = configNode;
    }

    public String[] getUserNames() {
        try {
            Vector result = new Vector();
            for (Enumeration users = configNode.getCompositeChild("user"); users.hasMoreElements();) {
                String userName = ManagementUtil.getSimpleString((CompositeNusuthWebAppElement) users.nextElement(), "name");
                if (userName != null && userName.length() > 0)
                    result.add(userName);
            }
            return (String[]) result.toArray(new String[0]);
        } catch (DeploymentException dex) {
            return null;
        }
    }

    public String[] getRoleNames() {
        try {
            Vector result = new Vector();
            for (Enumeration roles = configNode.getCompositeChild("role"); roles.hasMoreElements();) {
                String roleName = ManagementUtil.getSimpleString((CompositeNusuthWebAppElement) roles.nextElement(), "name");
                if (roleName != null && roleName.length() > 0)
                    result.add(roleName);
            }
            return (String[]) result.toArray(new String[0]);
        } catch (DeploymentException dex) {
            return null;
        }
    }

    public boolean convertPasswords(String[] userNames) {
        System.out.println("Convert passwords for users " + Arrays.asList(userNames));
        for (int i = 0; i < userNames.length; i++)
            try {
                if (!convertPassword(userNames[i]))
                    return false;
            } catch (DeploymentException dex) {
                return false;
            }
        return true;
    }

    public boolean convertPassword(String userName)
            throws DeploymentException {
        CompositeNusuthWebAppElement userNode = findUserNode(userName);
        if (userNode == null)
            return false;
        else {
            String pass = ManagementUtil.getSimpleString(userNode, "password");
            if (pass == null || pass.length() == 0)
                return false;
            else {
                if (!pass.startsWith("{md5}")) {
                    pass = MD5.cryptPassword(pass);
                    ManagementUtil.getSimpleElement(userNode, "password").setContent(pass);
                }
            }
            return true;
        }
    }

    private CompositeNusuthWebAppElement findUserNode(String userName)
            throws DeploymentException {
        for (Enumeration users = configNode.getCompositeChild("user"); users.hasMoreElements();) {
            CompositeNusuthWebAppElement userNode = (CompositeNusuthWebAppElement) users.nextElement();
            String user = ManagementUtil.getSimpleString(userNode, "name");
            if (user.equals(userName)) {
                return userNode;
            }
        }
        return null;
    }

    private CompositeNusuthWebAppElement findRoleNode(String roleName)
            throws DeploymentException {
        for (Enumeration roles = configNode.getCompositeChild("role"); roles.hasMoreElements();) {
            CompositeNusuthWebAppElement roleNode = (CompositeNusuthWebAppElement) roles.nextElement();
            String role = ManagementUtil.getSimpleString(roleNode, "name");
            if (role.equals(roleName)) {
                return roleNode;
            }
        }
        return null;
    }

    public boolean addUser(String userName, String password) {
        try {
            CompositeNusuthWebAppElement userNode = configNode.addCompositeChild("user");
            String cryptedPassword = password.startsWith("{md5}") ? password : MD5.cryptPassword(password);
            userNode.setSimpleChild("name").setContent(userName);
            userNode.setSimpleChild("password").setContent(cryptedPassword);
            return true;
        } catch (DeploymentException dex) {
            return false;
        }
    }

    public boolean delUser(String userName) {
        try {
            CompositeNusuthWebAppElement userNode = findUserNode(userName);
            if (userNode == null)
                return false;
            else {
                configNode.removeCompositeChild("user", userNode);
                String[] roles = getRoleNames();
                if (roles == null)
                    return false;
                for (int i = 0; i < roles.length; i++)
                    clearRole(userName, roles[i]);
                return true;
            }
        } catch (DeploymentException dex) {
            return false;
        }
    }

    public boolean addRole(String roleName) {
        try {
            CompositeNusuthWebAppElement roleNode = configNode.addCompositeChild("role");
            roleNode.setSimpleChild("name").setContent(roleName);
            return true;
        } catch (DeploymentException dex) {
            return false;
        }
    }

    public boolean delRole(String roleName) {
        try {
            CompositeNusuthWebAppElement roleNode = findRoleNode(roleName);
            if (roleNode == null)
                return false;
            else
                configNode.removeCompositeChild("role", roleNode);
            return true;
        } catch (DeploymentException dex) {
            return false;
        }
    }

    public boolean setRole(String userName, String roleName) {
        try {
            CompositeNusuthWebAppElement roleNode = findRoleNode(roleName);
            if (roleNode == null)
                return false;
            else
                roleNode.addSimpleChild("member").setContent(userName);
            return true;
        } catch (DeploymentException dex) {
            return false;
        }
    }

    public boolean clearRole(String userName, String roleName) {
        try {
            CompositeNusuthWebAppElement roleNode = findRoleNode(roleName);
            if (roleNode == null)
                return false;
            else {
                for (Enumeration users = roleNode.getSimpleChild("member"); users.hasMoreElements();) {
                    SimpleNusuthWebAppElement userNode = (SimpleNusuthWebAppElement) users.nextElement();
                    if (userNode.getContent().trim().equals(userName)) {
                        roleNode.removeSimpleChild("member", userNode);
                        break;
                    }
                }
                return true;
            }
        } catch (DeploymentException dex) {
            return false;
        }
    }

    public boolean setPassword(String userName, String newPassword) {
        try {
            CompositeNusuthWebAppElement userNode = findUserNode(userName);
            if (userNode == null)
                return false;
            else {
                SimpleNusuthWebAppElement passwordNode = ManagementUtil.getSimpleElement(userNode, "password");
                if (passwordNode == null) {
                    /*SimpleNusuthWebAppElement certNode = ManagementUtil.getSimpleElement(userNode, "certificate-name");
                     if (certNode != null) {
                     userNode.removeSimpleChild("certificate-name", certNode);
                     }*/
                    passwordNode = userNode.setSimpleChild("password");
                }
                String cryptedPassword = MD5.cryptPassword(newPassword);
                passwordNode.setContent(cryptedPassword);
                return true;
            }
        } catch (Exception dex) {
            return false;
        }
    }

    public String[] getUserRoles(String userName) {
        try {
            HashSet result = new HashSet();
            for (Enumeration roles = configNode.getCompositeChild("role"); roles.hasMoreElements();) {
                CompositeNusuthWebAppElement roleNode = (CompositeNusuthWebAppElement) roles.nextElement();
                String name = ManagementUtil.getSimpleString(roleNode, "name");
                for (Enumeration users = roleNode.getSimpleChild("member"); users.hasMoreElements();) {
                    String currUserName = ((SimpleNusuthWebAppElement) users.nextElement()).getContent().trim();
                    if (currUserName.equals(userName)) {
                        result.add(name);
                        break;
                    }
                }
            }
            return (String[]) result.toArray(new String[0]);
        } catch (Exception dex) {
            return null;
        }
    }

    public String[] getRoleUsers(String roleName) {
        try {
            Vector result = new Vector();
            CompositeNusuthWebAppElement roleNode = findRoleNode(roleName);
            for (Enumeration users = roleNode.getSimpleChild("member"); users.hasMoreElements();)
                result.add(((SimpleNusuthWebAppElement) users.nextElement()).getContent().trim());
            return (String[]) result.toArray(new String[0]);
        } catch (Exception dex) {
            return null;
        }
    }
}

