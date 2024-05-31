package com.azoft.nusuth.jndi;

import javax.naming.*;
import java.util.*;

/**This class represents a nusuth servlet context(a naming context), which consists of a set of name-to-object
 * bindings. It contains methods for examining and updating these bindings.
 * All methods of this class that have arguments
 * of type Name will throw InvalidNameException
 * exception if not a CompositeName class type object will be used as an argument .
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
public class NusuthJNDIContext implements Context {
    private Hashtable envProperties;
    protected Hashtable bindings = new Hashtable();

    /**Constructor.
     */
    public NusuthJNDIContext() {
        envProperties = new Hashtable();
    }

    /**Constructor.
     * @param envProperties the hash table that contains environment properties(String) and connected values(String).
     */
    public NusuthJNDIContext(Hashtable envProperties) {
        this.envProperties = envProperties;
    }

    /**Retrieves the named object.
     * @param name	the name of the object to look up.
     * @return the object bound to name.
     * @exception NInvalidNameException if the given name is not the composite name.
     * @exception OperationNotSupportedException if the given name is an empty name.
     * @exception NameNotFoundException if there is no element bounded up with the first component of this composite name in this context.
     * @exception NotContextException if the given composite name contains more than one element but it's first component
     * is not bounded up with any sub context of this context.
     */
    public Object lookup(Name name) throws NamingException {
        if (!(name instanceof CompositeName))
            throw new InvalidNameException("Only composite names can be used");
        if (name.isEmpty())
            throw new OperationNotSupportedException("Using of empty names not allowed in this version");
        Object result = bindings.get(name.get(0));
        if (name.size() == 1) {
            return result;
        } else {
            if (result == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                return ((Context) result).lookup(name.getSuffix(1));
            } catch (ClassCastException cce) {
                throw new NotContextException("Name " + name.get(0) + " doesn't represent context");
            }
        }
    }

    /** Retrieves the named object. See @see NusuthJNDIContext#lookup(Name) for details.
     * @param name - the name of the object to look up.
     * @return the object bound to name.
     * @exception NamingException is thrown in the same cases as the for the @see NusuthJNDIContext#lookup(Name) method.
     */
    public Object lookup(String name) throws NamingException {
        return lookup(new CompositeName(name));
    }

    /**Binds a name to an object.
     * @param name the name to bind; may not be empty.
     * @param obj the object to bind; possibly null.
     * @exception NameAlreadyBoundException if name is already bound.
     * @exception NamingException if a naming exception is encountered.
     * @exception InvalidNameException if the given name object is empty or
     * if the given name object is not the instance of the CompositeName class.
     * @exception NameNotFoundException if there is no element bounded up with the first component of this composite name in this context.
     * @exception NotContextException if the given composite name contains more than one element but it's first component
     * is not bounded up with any sub context of this context.
     */
    public void bind(Name name, Object obj) throws NamingException {
        if (name.isEmpty())
            throw new InvalidNameException("Binding of objects with empty names not allowed");
        if (!(name instanceof CompositeName))
            throw new InvalidNameException("Only composite names can be used");
        Object res = bindings.get(name.get(0));
        if (name.size() == 1) {
            if (res != null)
                throw new NameAlreadyBoundException();
            bindings.put(name.get(0), obj);
        } else {
            if (res == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                ((Context) res).bind(name.getSuffix(1), obj);
            } catch (ClassCastException cce) {
                throw new NotContextException("Name " + name.get(0) + " doesn't represent context");
            }
        }
    }

    /**Binds a name to an object. See @see NusuthJNDIContext#bind(Name, Object) for more detais.
     * @param name the name to bind; may not be empty.
     * @param obj the object to bind; possibly null.
     * @exception NamingException is thrown in the same cases of for the @see NusuthJNDIContext#bind(Name, Object) method.
     */
    public void bind(String name, Object obj) throws NamingException {
        bind(new CompositeName(name), obj);
    }

    /**Binds a name to an object, overwriting any existing binding.
     * All intermediate contexts and the target context
     * (that named by all but terminal atomic component of the name) must already exist.
     * @param name the name to bind.
     * @param obj the object to bind.
     * @exception InvalidNameException if the given name is empty. or is not a comosite name.
     * @exception NameNotFoundException if the given name is not found.
     * @exception NotContextException if the givem name contains more than one element and
     * the first element of this name is not the context name.
     */
    public void rebind(Name name, Object obj) throws NamingException {
        if (name.isEmpty())
            throw new InvalidNameException("Binding of objects with empty names not allowed");
        if (!(name instanceof CompositeName))
            throw new InvalidNameException("Only composite names can be used");
        Object res = bindings.get(name.get(0));
        if (name.size() == 1) {
            bindings.put(name.get(0), obj);
        } else {
            if (res == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                ((Context) res).rebind(name.getSuffix(1), obj);
            } catch (ClassCastException cce) {
                throw new NotContextException("Name " + name.get(0) + " doesn't represent context");
            }
        }
    }

    /**Binds a name to an object, overwriting any existing binding. See @see rebind(Name, Object)
     * for more details.
     * @param name the name to bind.
     * @param obj the object to bind.
     * @exception InvalidNameException if the given name is empty. or is not a comosite name.
     * @exception NameNotFoundException if the given name is not found.
     * @exception NotContextException if the givem name contains more than one element and
     * the first element of this name is not the context name.
     */
    public void rebind(String name, Object obj) throws NamingException {
        rebind(new CompositeName(name), obj);
    }

    /**Unbinds the named object.
     * @param name the name to unbind.
     * @exception NameNotFoundException if the given name is empty or it is not
     * the composite name or if the given name contains more than one element
     * and there is no such name in the context or if the givem name contains more than one element and
     * the first element of this name is not the name of some sub context of this context.
     */
    public void unbind(Name name) throws NamingException {
        if (name.isEmpty() || !(name instanceof CompositeName))
            throw new NameNotFoundException();
        if (name.size() == 1) {
            bindings.remove(name.get(0));
        } else {
            Object obj = bindings.get(name.get(0));
            if (obj == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                ((Context) obj).unbind(name.getSuffix(1));
            } catch (ClassCastException cce) {
                throw new NameNotFoundException("Name " + name.get(0) + " doesn't represent context");
            }
        }
    }

    /**Unbinds the named object. See @see unbind(Name) fo more details.
     * @param name the name to unbind
     * @exception NamingException if the given name is empty or it is not
     * the composite name or if the given name contains more than one element
     * and there is no such name in the context or if the givem name contains more than one element and
     * the first element of this name is not the context name.
     */
    public void unbind(String name) throws NamingException {
        unbind(new CompositeName(name));
    }

    /**Binds a new name to the object bound to an old name, and unbinds the old name.
     * @param oldName the name of the existing binding.
     * @param newName the name of the new binding.
     * @exception NamingException if any exception occures while calling the @see lookup(String),
     * @see unbind(String) or @see bind(String) method of this class.
     */
    public void rename(Name oldName, Name newName) throws NamingException {
        Object obj = lookup(oldName);
        unbind(oldName);
        bind(newName, obj);
    }

    /**Binds a new name to the object bound to an old name, and unbinds the old name. See @see rename(Name, Name ) for more details.
     * @param oldName the name of the existing binding.
     * @param newName the name of the new binding.
     * @exception NamingException is thrown in the same cases as for the @see rename(Name, Name ) method of this class.
     */
    public void rename(String oldName, String newName) throws NamingException {
        rename(new CompositeName(oldName), new CompositeName(newName));
    }

    /**Enumerates the names bound in the named context, along with the class names of objects bound to them. The contents of any subcontexts are not included.
     * @param name the name of the context to list.
     * @return if the given name is empty then it returnes an enumeration of the names and class names
     * of the bindings in this context. Each element of the enumeration is of type Binding.
     * If the given name is not empty then it returnes an enumeration of the names and class names
     * of the bindings in the context defined by this name.
     * @exception InvalidNameException if the given name is not the composite name.
     * @exception NameNotFoundException if the given name is not found.
     * @exception NameNotFoundException if the givem name contains more than one element and
     * the first element of this name is not the context name.
     */
    public NamingEnumeration list(Name name) throws NamingException {
        if (!(name instanceof CompositeName))
            throw new InvalidNameException("Only composite names can be used");
        if (name.isEmpty()) {
            NusuthNamingEnumeration jne = new NusuthNamingEnumeration();
            Enumeration bkeys = bindings.keys();
            String cname;
            while (bkeys.hasMoreElements()) {
                cname = (String) bkeys.nextElement();
                jne.addElement(new Binding(cname, bindings.get(cname)));
            }
            return jne;
        } else {
            Object obj = bindings.get(name.get(0));
            if (obj == null)
                throw new NameNotFoundException("Name " + name.get(0) + " not found");
            try {
                return ((Context) obj).list(name.getSuffix(1));
            } catch (ClassCastException cce) {
                throw new NameNotFoundException("Name " + name.get(0) + " doesn't represent context");
            }
        }
    }

    /**Enumerates the names bound in the named context, along with the class names of objects bound to them.
     * See @see list(Name) for more details.
     * @param name the name of the context to list.
     * @return an enumeration of the names and class names of the bindings in this context. Each element of the enumeration is of type Binding.
     * @exception NamingException is thrown in the same cases as for the @see list(Name) method of this class.
     */
    public NamingEnumeration list(String name) throws NamingException {
        return list((name == null || name.length() == 0) ? new CompositeName() : new CompositeName(name));
    }

    /**Enumerates the names bound in the named context, along with the objects bound to them. The contents of any subcontexts are not included.
     * If a binding is added to or removed from this context, its effect on an enumeration previously returned is undefined.
     * @param name the name of the context to list.
     * @return an enumeration of the bindings in this context. Each element of the enumeration is of type Binding.
     * @exception NamingException is thrown in the same cases os for the @see list(Name).
     */
    public NamingEnumeration listBindings(Name name) throws NamingException {
        return list(name);
    }

    /**Enumerates the names bound in the named context, along with the objects bound to them. The contents of any subcontexts are not included.
     * If a binding is added to or removed from this context, its effect on an enumeration previously returned is undefined.
     * @param name the name of the context to list.
     * @return an enumeration of the bindings in this context. Each element of the enumeration is of type Binding.
     * @exception NamingException is thrown in the same cases os for the @see list(String).
     */
    public NamingEnumeration listBindings(String name) throws NamingException {
        return list(name);
    }

    /**Destroys the named sub context and removes it from the namespace.
     * Any attributes associated with the name are also removed.
     * Intermediate contexts are not destroyed.
     * @param name the name of the context to be destroyed.
     * @exception NotContextException if there is no context with such name.
     * @exception ContextNotEmptyException if the given context is not empty.
     */
    public void destroySubcontext(Name name) throws NamingException {
        Context ctx = null;
        try {
            ctx = (Context) lookup(name);
        } catch (ClassCastException cce) {
            throw new NotContextException();
        }
        if (list(name).hasMore())
            throw new ContextNotEmptyException();
        ctx.close();
        unbind(name);
    }

    /**Destroys the named sub context and removes it from the namespace. See @see destroySubcontext(Name) for more details.
     * @param name the name of the context to be destroyed.
     * @exception NotContextException if there is no context with such name.
     * @exception ContextNotEmptyException if the given context is not empty.
     */
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(new CompositeName(name));
    }

    /**Creates and binds a new context. Creates a new context with the given name and binds
     * it in the target context (that named by all but terminal atomic component of the name).
     * All intermediate contexts and the target context must already exist.
     * @param name the name of the context to create.
     * @return the newly created context.
     * @exception NamingException is thrown in the same cases as for the @see bind(Name, Context) method of this class.
     */
    public Context createSubcontext(Name name) throws NamingException {
        Context ctx = new NusuthJNDIContext();
        bind(name, ctx);
        return ctx;
    }

    /**Creates and binds a new context. See @see createSubcontext(Name) for more details.
     * @param name  the name of the context to create.
     * @return the newly created context.
     * @exception NamingException is thrown in the same cases as for the @see bind(Name, Context) method of this class.
     */
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(new CompositeName(name));
    }

    /**Retrieves the named object, following links except for the terminal atomic component of the name. If the object bound to name is not a link, returns the object itself.
     * @depricated all ways throws OperationNotSupportedException exception.
     * @param name the name of the object to look up
     * @return the object bound to name, not following the terminal link (if any).
     * @exception NamingException if a naming exception is encountered.
     */
    public Object lookupLink(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**See @see lookupLink(Name) for more details.
     * @depricated all ways throws OperationNotSupportedException exception.
     * @param name the name of the object to look up
     * @return the object bound to name, not following the terminal link (if any).
     * @exception NamingException
     */
    public Object lookupLink(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**This method allows an application to get a parser for parsing names into
     * their atomic components using the naming convention of a particular naming system.
     * @depricated all ways throws OperationNotSupportedException exception.
     * @param name the name of the context from which to get the parser.
     * @return a name parser that can parse compound names into their atomic components.
     * @exception NamingException if a naming exception is encountered.
     */
    public NameParser getNameParser(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**This method allows an application to get a parser for parsing names into
     * their atomic components using the naming convention of a particular naming system.
     * @depricated all ways throws OperationNotSupportedException exception.
     * @param name the name of the context from which to get the parser.
     * @return a name parser that can parse compound names into their atomic components.
     * @exception NamingException if a naming exception is encountered.
     */
    public NameParser getNameParser(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**Composes the name of this context with a name relative to this context.
     * @param name a name relative to this context.
     * @param prefix the name of this context relative to one of its ancestors.
     * @return the composition of prefix and name.
     * @exception InvalidNameException if name or prefix is not the composite name.
     */
    public Name composeName(Name name, Name prefix) throws NamingException {
        if (!(name instanceof CompositeName && prefix instanceof CompositeName))
            throw new InvalidNameException("Only composite names can be used");
        Name result = new CompositeName();
        return result.addAll(prefix).addAll(name);
    }

    /**Composes the name of this context with a name relative to this context.
     * @param name a name relative to this context.
     * @param prefix the name of this context relative to one of its ancestors.
     * @return the composition of prefix and name.
     * @exception InvalidNameException if name or prefix is not the composite name.
     */
    public String composeName(String name, String prefix) throws NamingException {
        return composeName(new CompositeName(name), new CompositeName(prefix)).toString();
    }

    /**Adds a new environment property to the environment of this context.
     * If the property already exists, its value is overwritten.
     * @param propName the name of the environment property to add.
     * @param propVal the value of the property to add.
     * @return the previous value of the property, or null if the property was not in the environment before.
     * @exception NamingException if any exception occures while getting or putting environment property.
     */
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        Object previous = envProperties.get(propName);
        envProperties.put(propName, propVal);
        return previous;
    }

    /**Removes an environment property from the environment of this context.
     * @param propName of the environment property to remove.
     * @return the previous value of the property, or null if the property was not in the environment.
     * @exception NamingException  if any exception occures while getting or removing environment parameter.
     */
    public Object removeFromEnvironment(String propName) throws NamingException {
        Object previous = envProperties.get(propName);
        envProperties.remove(propName);
        return previous;
    }

    /**Retrieves the environment in effect for this context.
     * The caller should not make any changes to the object returned:
     * their effect on the context is undefined. The environment of this
     * context may be changed using addToEnvironment() and removeFromEnvironment().
     * @return the environment of this context.
     * @exception NamingException
     */
    public Hashtable getEnvironment() throws NamingException {
        return envProperties;
    }

    /**Closes this context. This method releases this context's resources immediately,
     * instead of waiting for them to be released automatically by the garbage collector.
     * @exception NamingException if a naming exception is encountered.
     */
    public void close() throws NamingException {
        bindings.clear();
        envProperties.clear();
    }

    /**Retrieves the full name of this context within its own namespace.
     * @deprecated all ways throws an OperationNotSupportedException exception.
     * @return this context's name in its own namespace; never null;
     * @exception NamingException if a naming exception is encountered
     */
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }
}

