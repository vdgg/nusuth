package com.azoft.nusuth.management.security;

import com.azoft.nusuth.deployment.*;
import com.azoft.nusuth.management.*;

import sun.security.acl.*;

import java.security.Principal;
import java.security.acl.*;
import java.util.*;

/**
 * Holds cluster security settings. Creates ACL from security settings file
 * (usually <code>security.xml</code>)
 *
 * @author VDGG
 * @author IgorK
 * @version 1.6
 * @since JBird 1.0
 */
class SecuritySettings {
    private class UserGroup {
        String name = null;
        HashSet parentGroups = new HashSet();
        HashSet permissions = new HashSet();
    }

    private class User extends UserGroup {
        String password = null;
    }

    protected Acl acl = new NusuthAclImpl(acl_owner, acl_name);
    protected final static String acl_name = " Security Manager Access Control List ";
    protected final static Principal acl_owner = new PrincipalImpl(" nusuth ");
    protected Hashtable passwords = new Hashtable();

    private void processPermissions(UserGroup group, NusuthPermission perm) {
        Vector toRemove = new Vector();
        for (Iterator j = group.permissions.iterator(); j.hasNext();) {
            NusuthPermission oldPerm = (NusuthPermission) j.next();
            if (perm.equals(oldPerm))
                toRemove.add(oldPerm);
        }
        if (toRemove.size() > 0)
            group.permissions.removeAll(toRemove);
        group.permissions.add(perm);
    }

    private void process(CompositeNusuthWebAppElement node, String componentType, String componentId, HashMap groups, HashMap users) throws ManagementException {
        try {
            for (Enumeration i = node.getCompositeChild("right"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement rightNode = (CompositeNusuthWebAppElement) i.nextElement();
                UserGroup principal = null;
                String name;

                if (rightNode.getSimpleChild("group-name").hasMoreElements()) {
                    name = ManagementUtil.getSimpleString(rightNode, "group-name");
                    principal = (UserGroup) groups.get(name);
                } else {
                    name = ManagementUtil.getSimpleString(rightNode, "user-name");
                    principal = (User) users.get(name);
                }
                if (principal == null)
                    throw new ManagementException("Unknown group or user \"" + name + "\"");

                for (Enumeration j = rightNode.getSimpleChild("permission"); j.hasMoreElements();) {
                    String action = ((SimpleNusuthWebAppElement) j.nextElement()).getContent();
                    NusuthPermission perm = new NusuthPermission(componentType, componentId, action);
                    processPermissions(principal, perm);
                }
            }
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't process " + componentType + " \"" + componentId + "\" resources, nested:" + dex.getMessage());
        }
    }

    private void processComponent(CompositeNusuthWebAppElement componentNode, String componentType, HashMap groups, HashMap users) throws ManagementException {
        try {
            String name = ManagementUtil.getSimpleString(componentNode, "name");
            process(componentNode, componentType, name, groups, users);
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't process " + componentType + " resources, nested:" + dex.getMessage());
        }
    }

    /**
     * Fills internal ACL with users and groups.
     *
     * @param groups map groupName -> {@link UserGroup UserGroup}
     * @param users map userName -> {@link User User}
     */
    protected void fillAcl(HashMap groups, HashMap users) throws NotOwnerException, ManagementException {
        HashMap aclGroups = new HashMap();

        for (Iterator i = groups.values().iterator(); i.hasNext();) {
            UserGroup group = (UserGroup) i.next();
            Group item = new GroupImpl(group.name);
            AclEntry entry = new AclEntryImpl(item);

            for (Iterator perm = group.permissions.iterator(); perm.hasNext();) {
                entry.addPermission((Permission) perm.next());
            }

            aclGroups.put(group.name, entry);
        }

        for (Iterator i = groups.values().iterator(); i.hasNext();) {
            UserGroup child = (UserGroup) i.next();
            Group aclChild = (Group) ((AclEntry) aclGroups.get(child.name)).getPrincipal();

            for (Iterator j = child.parentGroups.iterator(); j.hasNext();) {
                String parent = (String) j.next();
                AclEntry aclParentEntry = (AclEntry) aclGroups.get(parent);
                if (aclParentEntry != null) {
                    Group aclParent = (Group) (aclParentEntry).getPrincipal();
                    aclParent.addMember(aclChild);
                } else {
                    throw new ManagementException("Unknown group: \"" + parent + '"');
                }
            }
        }

        for (Iterator i = users.values().iterator(); i.hasNext();) {
            User user = (User) i.next();
            Principal item = new PrincipalImpl(user.name);
            AclEntry entry = new AclEntryImpl(item);

            for (Iterator perm = user.permissions.iterator(); perm.hasNext();) {
                entry.addPermission((Permission) perm.next());
            }

            acl.addEntry(acl_owner, entry);
            for (Iterator j = user.parentGroups.iterator(); j.hasNext();) {
                String parent = (String) j.next();
                Group aclParent = (Group) ((AclEntry) aclGroups.get(parent)).getPrincipal();

                if (aclParent == null) {
                    throw new ManagementException("Undefined group \"" + parent + "\" in user \"" + user.name + "\"");
                }

                aclParent.addMember(item);
            }
        }

        for (Iterator i = aclGroups.values().iterator(); i.hasNext();) {
            acl.addEntry(acl_owner, (AclEntry) i.next());
        }
    }

    public Acl getAcl() {
        return acl;
    }

    Hashtable getPasswords() {
        return passwords;
    }

    protected HashMap takeGroups(CompositeNusuthWebAppElement usersNode) throws ManagementException {
        HashMap groups = new HashMap();

        try {
            for (Enumeration i = usersNode.getCompositeChild("group"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement groupNode = (CompositeNusuthWebAppElement) i.nextElement();
                String name = ManagementUtil.getSimpleString(groupNode, "name");
                UserGroup group = new UserGroup();
                group.name = name;
                for (Enumeration j = groupNode.getSimpleChild("parent-group"); j.hasMoreElements();) {
                    String parent = ((SimpleNusuthWebAppElement) j.nextElement()).getContent().trim();
                    if (group.parentGroups.contains(parent))
                        throw new ManagementException("Duplicate parent group \"" + parent + "\" for group \"" + name + "\"");
                    group.parentGroups.add(parent);
                }
                if (groups.containsKey(name))
                    throw new ManagementException("Duplicate group \"" + name + "\"");
                groups.put(name, group);
            }
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't collect groups, nested:" + dex.getMessage());
        }

        return groups;
    }

    protected HashMap takeUsers(CompositeNusuthWebAppElement usersNode) throws ManagementException {
        HashMap users = new HashMap();

        try {
            for (Enumeration i = usersNode.getCompositeChild("user"); i.hasMoreElements();) {
                CompositeNusuthWebAppElement userNode = (CompositeNusuthWebAppElement) i.nextElement();
                String name = ManagementUtil.getSimpleString(userNode, "name");

                User user = new User();
                user.name = name;
                user.password = ManagementUtil.getSimpleString(userNode, "password");
                passwords.put(user.name, user.password);
                for (Enumeration j = userNode.getSimpleChild("parent-group"); j.hasMoreElements();) {
                    String parent = ((SimpleNusuthWebAppElement) j.nextElement()).getContent().trim();
                    if (user.parentGroups.contains(parent))
                        throw new ManagementException("Duplicate parent group \"" + parent + "\" for user \"" + name + "\"");
                    user.parentGroups.add(parent);
                }
                if (users.containsKey(name))
                    throw new ManagementException("Duplicate user \"" + name + "\"");
                users.put(name, user);
            }
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't collect users, nested:" + dex.getMessage());
        }

        return users;
    }

    /**
     * Processes component permissions from <code>security.xml</code>.
     * @param resourcesNode &ltresources&gt node that contains permissions for
     * varios components.
     * @param componentType component type name. For this components permissions
     * will be processed
     * @param groups map contains groupName -&gt {@link UserGroup UserGroup}
     * @param users map contains userName -&gt {@link User User}
     */
    private void processResource(CompositeNusuthWebAppElement resourcesNode,
                                 String componentType,
                                 HashMap groups,
                                 HashMap users)
            throws ManagementException {
        try {
            CompositeNusuthWebAppElement startNode = ManagementUtil.getCompositeElement(resourcesNode, componentType + 's');
            if (startNode != null) {
                for (Enumeration i = startNode.getCompositeChild(componentType); i.hasMoreElements();) {
                    CompositeNusuthWebAppElement componentNode = (CompositeNusuthWebAppElement) i.nextElement();
                    processComponent(componentNode, componentType, groups, users);
                }
                process(startNode, componentType, "*", groups, users);
            }
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't collect resources, nested:" + dex.getMessage());
        }
    }

    private void processResources(CompositeNusuthWebAppElement resourcesNode, HashMap groups, HashMap users) throws ManagementException {
        processResource(resourcesNode, ComponentType.SDISTRIBUTOR, groups, users);
        processResource(resourcesNode, ComponentType.SCONTAINER, groups, users);
        processResource(resourcesNode, ComponentType.SDEPLOYER, groups, users);
        processResource(resourcesNode, ComponentType.SAPPLICATION, groups, users);
        process(resourcesNode, "*", "*", groups, users);
    }

    public void read(CompositeNusuthWebAppElement configNode) throws ManagementException, NotOwnerException {
        try {
            CompositeNusuthWebAppElement resourcesNode = ManagementUtil.getCompositeElement(configNode, "resources");
            CompositeNusuthWebAppElement usersNode = ManagementUtil.getCompositeElement(configNode, "users");

            HashMap users = takeUsers(usersNode);
            HashMap groups = takeGroups(usersNode);

            processResources(resourcesNode, groups, users);
            fillAcl(groups, users);
        } catch (DeploymentException dex) {
            throw new ManagementException("Couldn't apply settings, nested: " + dex.getMessage());
        }
    }
}
