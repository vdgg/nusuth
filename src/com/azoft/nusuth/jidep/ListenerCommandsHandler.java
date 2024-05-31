package com.azoft.nusuth.jidep;

import com.azoft.nusuth.management.security.AdminPortListener;

import java.io.ByteArrayInputStream;

/**
 * This class extends CommandHandler for onnly 1 command ("accept" -
 * for accepting invoke requests).
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public class ListenerCommandsHandler extends CommandHandler {

    private AdminPortListener listener = null;

    /**
     * Constructor.
     * @param listener AdminPortListener.
     */
    public ListenerCommandsHandler(AdminPortListener listener) {
        this.listener = listener;
    }

    /**
     * Service the jidep request.
     * @param adapter Server side for JidepProtocolAdapter.
     * @throws Exception Throws if any error occures while service.
     */
    public void service(ServerJidepAdapter adapter) throws Exception {
        String command = adapter.getCommand();
        if (command.equals("accept")) {
            listener.acceptInvokeRequest(adapter.getInputStream());
            adapter.endResponse();
            adapter.cleanup();
            listener.registerComponent();
        }
    }

}
