package com.azoft.nusuth.jidep;

import com.azoft.nusuth.management.ComponentManager;
import com.azoft.nusuth.jndi.DistributedJNDIContext;
import com.azoft.nusuth.jndi.JNDICommand;
import com.azoft.nusuth.jndi.NusuthJNDIName;
import com.azoft.nusuth.security.NusuthPrincipal;
import com.azoft.nusuth.security.NusuthPermission;

import javax.naming.directory.*;
import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.acl.Acl;
import java.util.List;

/**
 * This class implement CommandHandler. It intend for handling all jidep
 * requests with commands, which intend for working with DistributedJNDIContext.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class JndiCommandHandler extends CommandHandler {

    private ComponentManager manager = null;

    /**
     * Constructor.
     * @param manager Component manager.
     */
    public JndiCommandHandler(ComponentManager manager) {
        this.manager = manager;
    }

    /**
     * This method service the jidep request.
     * @param adapter Server side of JidepProtocolAdapter.
     * @throws Exception Throws if any errors occures while service.
     */
    public void service(ServerJidepAdapter adapter) throws Exception {
        JidepSession session = adapter.getSession();
        if (session.getAttribute("jndi_client_roles") == null) {
            adapter.setStatus(404);
        } else {
            List roles = (List) session.getAttribute("jndi_client_roles");
            boolean allowed = false;
            DistributedJNDIContext context = manager.getDistributedContext();
            ObjectInputStream ois
                    = new ObjectInputStream(adapter.getInputStream());
            JNDICommand command = (JNDICommand) ois.readObject();
            String curContext = command.currentContext;
            String opName = command.operationName;
            String[] attr = new String[1];
            attr[0] = "ACL";
            Attributes attrs = null;
            if (!opName.equals("modify")) {
                if (curContext.indexOf("/") != -1) {
                    attrs = context.getAttributes(curContext.substring(0, curContext.lastIndexOf("/")), attr);
                } else {
                    attrs = context.getAttributes("", attr);
                }
            } else {
                attrs = context.getAttributes(curContext, attr);
            }
            if (attrs != null) {
                Attribute attribute = attrs.get("ACL");
                if (attribute != null) {
                    String permission = null;
                    if (opName.equals("lookup") || opName.equals("list")) {
                        permission = "read";
                    } else {
                        permission = "write";
                    }
                    for (int i = 0; i < roles.size(); i++) {
                        allowed = ((Acl) attribute.get()).
                                checkPermission(new NusuthPrincipal((String) roles.get(i)),
                                        new NusuthPermission(permission));
                        if (allowed) {
                            break;
                        }
                    }
                } else {
                    allowed = false;
                }
            } else {
                allowed = false;
            }
            if (allowed) {
                if (opName.equals("clonejnditree")) {
                    context.addCloneRoles(roles);
                    ObjectOutputStream oos =
                            new ObjectOutputStream(adapter.getOutputStream());
                    oos.writeObject(context);
                    adapter.endResponse();
                    adapter.cleanup();
                    return;
                } else if (opName.equals("create")) {
                    Attributes attributes = new BasicAttributes();
                    for (int i = 0; i < command.parameters.length; i++) {
                        attributes.put(((Attribute) command.parameters[i]).getID(),
                                ((Attribute) command.parameters[i]).get());
                    }
                    try {
                        context.localCreateSubcontext(curContext, attributes);
                    } catch (NamingException e) {
                        sendException(adapter, e);
                    }
                } else if (opName.equals("bind")) {
                    try {
                        context.localBind(curContext, command.parameters[0]);
                    } catch (NamingException e) {
                        sendException(adapter, e);
                    }
                } else if (opName.equals("unbind")) {
                    try {
                        context.localUnbind(curContext);
                    } catch (NamingException e) {
                        sendException(adapter, e);
                    }
                } else if (opName.equals("rebind")) {
                    try {
                        context.localRebind(curContext, command.parameters[0]);
                    } catch (NamingException e) {
                        sendException(adapter, e);
                    }
                } else if (opName.equals("modify")) {
                    boolean isOwner = false;
                    for (int i = 0; i < roles.size(); i++) {
                        Attribute attribute = attrs.get("ACL");
                        isOwner = ((Acl) attribute.get()).
                                isOwner(new NusuthPrincipal((String) roles.get(i)));
                        if (isOwner) {
                            break;
                        }
                    }
                    if (isOwner) {
                        try {
                            context.localModifyAttributes(
                                    new NusuthJNDIName(command.currentContext),
                                    (ModificationItem[]) (command.parameters));
                        } catch (NamingException e) {
                            sendException(adapter, e);
                        }
                    } else {
                        adapter.setStatus(404);
                        adapter.getOutputStream().write("not owner".getBytes());
                    }
                } else if (opName.equals("lookup")) {
                    Object result = context.localLookup(curContext);
                    ObjectOutputStream oos
                            = new ObjectOutputStream(adapter.getOutputStream());
                    oos.writeObject(result);
                } else {
                    Object result = context.localList(curContext);
                    ObjectOutputStream oos
                            = new ObjectOutputStream(adapter.getOutputStream());
                    oos.writeObject(result);
                }
            } else {
                adapter.setStatus(404);
                adapter.getOutputStream().
                        write("Don't have such permission".getBytes());
            }
        }
        adapter.endResponse();
        adapter.cleanup();
    }

    private void sendException(ServerJidepAdapter adapter, Exception e)
            throws IOException {
        adapter.setStatus(500);
        ObjectOutputStream oos
                = new ObjectOutputStream(adapter.getOutputStream());
        oos.writeObject(e);
    }

}
