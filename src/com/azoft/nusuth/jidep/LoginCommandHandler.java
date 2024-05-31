package com.azoft.nusuth.jidep;

import com.azoft.nusuth.management.ComponentManager;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.jndi.DistributedJNDIContext;
import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;
import com.azoft.nusuth.gui.MD5;

import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.LinkedList;

/**
 * This class extends CommandHandler for onnly 1 command ("login" -
 * for client authorization).
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class LoginCommandHandler extends CommandHandler {

    private ComponentManager manager = null;

    /**
     * Constructor.
     * @param manager Component manager.
     */
    public LoginCommandHandler(ComponentManager manager) {
        this.manager = manager;
    }

    /**
     * This method service the jidep request.
     * @param adapter Server side for JidepProtocolAdapter.
     * @throws Exception Throws if any error occures while service.
     */
    public void service(ServerJidepAdapter adapter) throws Exception {
        ObjectInputStream ois
                = new ObjectInputStream(adapter.getInputStream());
        String userName = (String) ois.readObject();
        String userPassword = (String) ois.readObject();
        DistributedJNDIContext root = manager.getDistributedContext();
        CompositeNusuthWebAppElement config
                = (CompositeNusuthWebAppElement) root.localLookup("usersroles");
        CompositeNusuthWebAppElement users
                = ManagementUtil.getCompositeElement(config, "users");
        Enumeration enum = users.getCompositeChild("user");
        CompositeNusuthWebAppElement user = null;
        while (enum.hasMoreElements()) {
            user = (CompositeNusuthWebAppElement) enum.nextElement();
            String name = ManagementUtil.getSimpleString(user, "name");
            if (userName.equals(name)) {
                break;
            }
        }
        if (user != null) {
            String hashPassword = ManagementUtil.getSimpleString(user, "password");
            if (!MD5.verifyPassword(hashPassword, userPassword)) {
                adapter.setStatus(404);
            } else {
                String[] roles = ManagementUtil.getSimpleStrings(user, "role");
                LinkedList list = new LinkedList();
                if (roles != null) {
                    for (int i = 0; i < roles.length; i++) {
                        list.add(roles[i]);
                    }
                }
                JidepSession session = adapter.getSession();
                session.setAttribute("jndi_client_roles", list);
            }
        } else {
            adapter.setStatus(404);
        }
        adapter.endResponse();
        adapter.cleanup();
    }

}
