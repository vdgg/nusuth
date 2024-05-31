package com.azoft.nusuth.jidep;

import com.azoft.nusuth.jndi.DistributedJNDIContext;
import com.azoft.nusuth.jndi.JNDICommand;
import com.azoft.nusuth.jndi.NusuthJNDIName;
import com.azoft.nusuth.security.NusuthPrincipal;
import com.azoft.nusuth.security.NusuthPermission;
import com.azoft.nusuth.management.ComponentManager;

import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.NamingException;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.acl.Acl;

public class SubscribeCommandHandler extends CommandHandler {

    private ComponentManager manager = null;

    /**
     * Constructor.
     * @param manager Component manager.
     */
    public SubscribeCommandHandler(ComponentManager manager) {
        this.manager = manager;
    }

    /**
     * This method service request for "subscribe" command.
     * @param adapter Server side for JidepProtocolAdapter.
     * @throws Exception If any error occures during service.
     */
    public void service(ServerJidepAdapter adapter) throws Exception {
        JidepSession session = adapter.getSession();
        if (session.getAttribute("jndi_client_roles") == null) {
            adapter.setStatus(404);
        } else {
            List roles = (List) session.getAttribute("jndi_client_roles");
            DistributedJNDIContext context = manager.getDistributedContext();
            String[] attr = new String[1];
            attr[0] = "ACL";
            Attributes attrs = null;
            ObjectInputStream ois
                    = new ObjectInputStream(adapter.getInputStream());
            Integer number = (Integer) ois.readObject();
            JidepSubscription sub = (JidepSubscription) ois.readObject();
            String contexts[] = sub.contexts;
            boolean allow = true;
            for (int j = 0; j < roles.size(); j++) {
                for (int i = 0; i < contexts.length; i++) {
                    attrs = context.getAttributes(contexts[i], attr);
                    Attribute attribute = attrs.get("ACL");
                    if (attrs != null) {
                        allow = ((Acl) attribute.get()).
                                checkPermission(new NusuthPrincipal((String) roles.get(j)),
                                        new NusuthPermission("read"));
                    } else {
                        allow = false;
                    }
                    if (!allow) {
                        break;
                    }
                }
                if (allow) {
                    break;
                }
            }
            if (allow) {
                JidepNotificationListener listener = new JidepNotificationListener(adapter);
                for (int i = 0; i < contexts.length; i++) {
                    context.getRootContext().addListener(contexts[i], listener);
                }
                ObjectOutputStream oos =
                        new ObjectOutputStream(adapter.getOutputStream());
                oos.writeObject("subscription");
                oos.writeObject(number);
            } else {
                adapter.setStatus(404);
                adapter.endResponse();
                adapter.cleanup();
                return;
            }
        }
        adapter.endResponse();
        adapter.cleanup();
    }

}
