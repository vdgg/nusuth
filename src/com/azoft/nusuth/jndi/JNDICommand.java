package com.azoft.nusuth.jndi;

import java.io.*;

/**
 * This class is wrapper for jndi command.
 * @author skilz
 * @since Nusuth1.0
 * @version 1.2
 */
public class JNDICommand implements Serializable {

    /** Name */
    public String currentContext = null;
    /** jndi operation name*/
    public String operationName = null;
    /** parameters for the concrete jndi operation name */
    public Object[] parameters = null;

    /**
     * Constructor.
     * @param currentContext Name parameter from jndi operation.
     * @param opName jndi operation name.
     * @param parameters parameters for the concrete jndi operation.
     */
    public JNDICommand(String currentContext, String opName, Object[] params) {
        this.currentContext = currentContext;
        this.operationName = opName;
        this.parameters = params;
    }

    /**
     * Constructor.
     */
//  public JNDICommand() {
//  }

    /**
     * Writes this object to output stream.
     * @param os ObjectOutputStream
     * @throws IOException Throws if any error occures while writing.
     */

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(currentContext);
        out.writeObject(operationName);
        out.writeObject(parameters);
    }

    /**
     * Read object from the input stream.
     * @param is ObjectInputStream.
     * @throws IOException Throws if any error occures while writing.
     */

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        currentContext = (String) in.readObject();
        operationName = (String) in.readObject();
        parameters = (Object[]) in.readObject();
    }

}