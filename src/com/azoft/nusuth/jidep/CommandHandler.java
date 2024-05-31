package com.azoft.nusuth.jidep;

/**
 * This class is abstract class for all command handlers for jidep request
 * commands.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.0
 */
public abstract class CommandHandler {

    /**
     * This method service request
     * @param adapter Server side for JidepProtocolAdapter.
     * @throws Exception If any error occures during service.
     */
    public abstract void service(ServerJidepAdapter adapter) throws Exception;

}
