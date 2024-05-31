package com.azoft.nusuth.jndi;

import javax.naming.*;
import javax.naming.spi.*;

/**This interface class a factory that creates an initial context.
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0ú
 */
public class NusuthInitialContextFactory implements InitialContextFactory {

    /**Constructor.
     */
    public NusuthInitialContextFactory() {
    }

    /**Creates an Initial Context for beginning name resolution. Special requirements of this context are supplied using environment.
     * @param environment environment specifying information to be used in the creation of the initial context,not null.
     * @return A non-null initial context object of the NusuthJNDIDirContext class type.
     * @exception NamingException if any exception occures while creation initial context.
     */
    public Context getInitialContext(java.util.Hashtable environment) throws NamingException {
        return new NusuthJNDIDirContext(environment);
    }
}

