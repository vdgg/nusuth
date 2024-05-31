package com.azoft.nusuth.jidep;

import com.azoft.nusuth.management.ComponentManager;
import com.azoft.nusuth.management.ContainerManager;
import com.azoft.nusuth.core.LocalContainer;

/**
 * This class extends CommandHandler. It intends for handling all jidep requests
 * which work with session failover mechanism.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class SessionCommandsHandler extends CommandHandler {

    private ComponentManager manager = null;

    /**
     * Constructor.
     * @param manager Component manager.
     */
    public SessionCommandsHandler(ComponentManager manager) {
        this.manager = manager;
    }

    /**
     * This method service the jidep request.
     * @param adapter Server side fo JidepProtocolAdapter.
     * @throws Exception Throws if any error occures while service.
     */
    public void service(ServerJidepAdapter adapter) throws Exception {
        String command = adapter.getCommand();
        if (!manager.getComponentType().equals("container")) {
            adapter.setStatus(500);
            adapter.endResponse();
            adapter.cleanup();
            return;
        }
        LocalContainer localContainer =
                ((ContainerManager) manager).getLocalContainer();
        if (command.equals("containers")) {
            localContainer.addContainers(adapter.getInputStream());
        } else if (command.equals("update")) {
            localContainer.updateSession(adapter.getInputStream());
        } else if (command.equals("remove")) {
            localContainer.removeSession(adapter.getInputStream());
        } else if (command.equals("down")) {
            localContainer.removeContainer(adapter.getInputStream());
        }
        adapter.endResponse();
        adapter.cleanup();
    }

}
