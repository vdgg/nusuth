package com.azoft.nusuth.jndi;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.*;

/**The NusuthJNDIDirContext , contains methods for examining and updating attributes
 * associated with objects, and for searching the directory.
 * Here only the composite names are used.
 * @author VDGG (vdgg@azoft.com)
 * @version 1.0
 * @since 1.0
 */
public class NusuthJNDIDirContext extends NusuthJNDIContext implements DirContext {
    private Hashtable attributes2names = new Hashtable();
    private Hashtable names2attributes = new Hashtable();

    /**Constructor.
     */
    public NusuthJNDIDirContext() {
        super();
    }

    /**Constructor.
     * @param envProps the hash table that contains environment properties(String) and connected values(String).
     */
    public NusuthJNDIDirContext(Hashtable envProps) {
        super(envProps);
    }

    /**Retrieves all attributes associated with a named object. Calles the getAttributes(Name, String[]) of this class with the second parameter == null.
     * @param name the name of the object from which to retrieve attributes.
     * @return  the requested attributes; never null.
     * @exception NamingException if any exception occures while calling the getAttributes(Name, String[]) method of this class.
     */
    public Attributes getAttributes(Name name) throws NamingException {
        return getAttributes(name, null);
    }

    /**Retrieves all attributes associated with a named object. Calles the getAttributes(Name, String[]) of this class with the second parameter == null.
     * @param name the name of the object from which to retrieve attributes.
     * @return  the requested attributes; never null.
     * @exception NamingException if any exception occures while calling the getAttributes(Name, String[]) method of this class.
     */
    public Attributes getAttributes(String name) throws NamingException {
        return getAttributes(new CompositeName(name));
    }

    /**Retrieves selected attributes associated with a named object.
     * @param name the name of the object from which to retrieve attributes.
     * @param attrIds - the identifiers of the attributes to retrieve. null indicates that all attributes should be retrieved.
     * @return the requested attributes; never null.
     * @exception InvalidNameException if the given name is not a composite name or if the given name is empty.
     * @exception InvalidNameException if a given name consists of more than one element and there is no object bound up with the first element of this name.
     * @exception NotContextException if a given name consists of more than one element and the first element is not the name of a sub context of this context.
     */
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        if (!(name instanceof CompositeName))
            throw new InvalidNameException("Only composite name can be used");
        if (name.isEmpty())
            throw new InvalidNameException("Name cannot be empty");
//If this is the name of the object that is in this context.
        if (name.size() == 1) {
            Attributes attrs = (Attributes) names2attributes.get(name);
            BasicAttributes result = new BasicAttributes();
            if (attrs == null) {
                return result;
            } else {
                if (attrIds == null)
                    return attrs;
                else if (attrIds.length == 0)
                    return result;
                else {
                    Attribute attr;
                    for (int i = 0; i < attrIds.length; i++) {
                        attr = attrs.get(attrIds[i]);
                        if (attr != null)
                            result.put(attr);
                    }
                    return result;
                }
            }
        } else {
            //If this name consists of more than one element.
            Object obj = bindings.get(name.get(0));
            if (obj == null)
                throw new InvalidNameException("Context " + name.get(0) + " not found");
            try {
                return ((DirContext) obj).getAttributes(name.getSuffix(1));
            } catch (ClassCastException cce) {
                throw new NotContextException("Object " + name.get(0) + " is not dircontext");
            }
        }
    }

    /**Retrieves selected attributes associated with a named object.
     * @param name the name of the object from which to retrieve attributes.
     * @param attrIds - the identifiers of the attributes to retrieve. null indicates that all attributes should be retrieved.
     * @return the requested attributes; never null.
     * @exception InvalidNameException if the given name is not a composite name or if the given name is empty.
     * @exception InvalidNameException if a given name consists of more than one element and there is no object bound up with the first elemint of this name.
     * @exception NotContextException if a given name consists of more than one element and the first element is not the name of a sub context of this context.
     */
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return getAttributes(new CompositeName(name), attrIds);
    }

    /**Modifies the attributes associated with a named object. Doing nothing if there is no attributes to modify.
     * @param name the name of the object whose attributes will be updated.
     * @param mod_op the modification operation, one of: ADD_ATTRIBUTE, REPLACE_ATTRIBUTE, REMOVE_ATTRIBUTE.
     * @param attrs the attributes to be used for the modification; may not be null.
     * @exception NamingException if any error occures while modification of an attributes.
     */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        int attrsSize = attrs.size();
        if (attrsSize == 0)
            return;
        ModificationItem[] mitems = new ModificationItem[attrsSize];
        NamingEnumeration ne = attrs.getAll();
        for (int i = 0; i < attrsSize; i++) {
            mitems[i] = new ModificationItem(mod_op, (Attribute) ne.next());
        }
        modifyAttributes(name, mitems);
    }

    /**Modifies the attributes associated with a named object.Doing nothing if there is no attributes to modify.
     * @param name the name of the object whose attributes will be updated.
     * @param mod_op the modification operation, one of: ADD_ATTRIBUTE, REPLACE_ATTRIBUTE, REMOVE_ATTRIBUTE.
     * @param attrs the attributes to be used for the modification; may not be null.
     * @exception NamingException if any error occures while modification of an attributes.
     */
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        modifyAttributes(new CompositeName(name), mod_op, attrs);
    }

    /**Modifies the attributes associated with a named object using an ordered list
     * of modifications. The modifications are performed in the order specified.
     * Each modification specifies a modification operation code and an attribute
     * on which to operate.
     * @param name the name of the object whose attributes will be updated.
     * @param mods an ordered sequence of modifications to be performed; may not be null.
     * @exception InvalidNameException if the given name is not the composite name or if the name is empty or if this name consists of more than one element and it's first element couldn't be found in the context.
     * @exception NotContextException  if this name consists of more than one element and it's first element is not the name of a sub context of this context.
     */
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        if (!(name instanceof CompositeName))
            throw new InvalidNameException("Only composite names can be used");
        if (name.isEmpty())
            throw new InvalidNameException("Name cannot be empty");
        //If this name consists of one element (it means that the object with this name belongs to this context )
        if (name.size() == 1) {
            if (lookup(name) == null)
                return;
            Attributes attrs = (Attributes) names2attributes.get(name);
            if (attrs == null) {
                attrs = new BasicAttributes();
                names2attributes.put(name, attrs);
            }
            Attribute attr, toModify;
            int mod_op;
            List lst;
            NamingEnumeration ne;
            // processes the modifications
            for (int i = 0; i < mods.length; i++) {
                attr = mods[i].getAttribute();
                mod_op = mods[i].getModificationOp();
                toModify = attrs.get(attr.getID());
                switch (mod_op) {
                    case ADD_ATTRIBUTE:
                        //Adds this attribute to the list of attributes of the object with the given name
                        if (toModify == null) {
                            //If there were no attribute associated with this object :
                            toModify = attr;
                            attrs.put(toModify);
                            lst = (List) attributes2names.get(attr.getID());
                            if (lst == null) {
                                lst = Collections.synchronizedList(new ArrayList());
                                attributes2names.put(attr.getID(), lst);
                            }
                            synchronized (lst) {
                                lst.add(new NameAttributePair(name, toModify));
                            }
                        } else {
                            //If there were attributes associated with this object: Then it simply adds new attributes.
                            ne = attr.getAll();
                            while (ne.hasMore()) {
                                toModify.add(ne.next());
                            }
                        }
                        break;
                    case REPLACE_ATTRIBUTE:
                        // Replaces the object attributes:
                        if (toModify == null) {
                            //If there were no attribute associated with this object :
                            toModify = attr;
                            attrs.put(toModify);
                            lst = (List) attributes2names.get(attr.getID());
                            if (lst == null) {
                                lst = Collections.synchronizedList(new ArrayList());
                                attributes2names.put(attr.getID(), lst);
                            }
                            synchronized (lst) {
                                lst.add(new NameAttributePair(name, toModify));
                            }
                        } else {
                            //If there were attributes associated with this object:
                            ne = attr.getAll();
                            if (!ne.hasMore())
                                toModify.clear();
                            else
                                while (ne.hasMore()) {
                                    toModify.add(ne.next());
                                }
                        }
                        break;
                    case REMOVE_ATTRIBUTE:
                        // Removes attributes which are represented in the attr variable.
                        if (toModify != null) {
                            ne = attr.getAll();
                            while (ne.hasMore()) {
                                toModify.remove(ne.next());
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        } else {
            // if this name consists of more that one element (it means that the needed object belongs to the sub context of this context)
            Object obj = bindings.get(name.get(0));
            if (obj == null)
                throw new InvalidNameException("Context " + name.get(0) + " not found");
            try {
                ((DirContext) obj).modifyAttributes(name.getSuffix(1), mods);
            } catch (ClassCastException cce) {
                throw new NotContextException("Object " + name.get(0) + " is not dircontext");
            }
        }
    }

    /**Modifies the attributes associated with a named object using an ordered list
     * of modifications. For more detais see @see modifyAttributes(Name, ModificationItem[]) method of this class.
     * @param name the name of the object whose attributes will be updated.
     * @param mods an ordered sequence of modifications to be performed; may not be null.
     * @exception InvalidNameException if the given name is not the composite name or if the name is empty or if this name consists of more than one element and it's first element couldn't be found in the context.
     * @exception NotContextException  if this name consists of more than one element and it's first element is not the name of a sub context of this context.
     */
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        modifyAttributes(new CompositeName(name), mods);
    }

    /**Binds a name to an object, along with associated attributes.
     * This method calles the @see NusuthJNDIContext#bind(Name, Object) method of the super class
     * and then adds the given attributes.
     * @param name the name to bind.
     * @param obj the object to bind.
     * @param attrs the attributes to associate with the binding.
     * @exception NamingException if any error occures while binding object or modifying attributes.
     */
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        super.bind(name, obj);
        modifyAttributes(name, ADD_ATTRIBUTE, attrs);
    }

    /**Binds a name to an object, along with associated attributes.
     * See @see bind(Name , Object , Attributes ) for more details.
     * @param name the name to bind.
     * @param obj the object to bind.
     * @param attrs the attributes to associate with the binding.
     * @exception NamingException if any error occures while binding object or modifying attributes.
     */
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        bind(new CompositeName(name), obj, attrs);
    }

    /**Binds a name to an object, along with associated attributes,
     * overwriting any existing binding
     *(Calles the @see NusuthJNDIContext#rebind(Name, Object) method of the super class and @see modifyAttributes(Name,int,Attributes) method of this class)
     * and resets attributes.
     * @param name the name to bind.
     * @param obj the object to bind.
     * @param attrs the attributes to associate with the binding.
     * @exception NamingException is thrown in the same cases as for the NusuthJNDIContext#rebind(Name, Object) method of the super class and @see modifyAttributes(Name, int, Attributes )method of the super class.
     */
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        super.rebind(name, obj);
        modifyAttributes(name, REPLACE_ATTRIBUTE, attrs);
    }

    /**Binds a name to an object, along with associated attributes,
     * overwriting any existing binding. See @see rebind(Name, Object, Attributes) for more details.
     * @param name the name to bind.
     * @param obj the object to bind.
     * @param attrs the attributes to associate with the binding.
     * @exception NamingException is thrown in the same cases as for the NusuthJNDIContext#rebind(Name, Object) method of the super class and @see modifyAttributes(Name, int, Attributes )method of the super class.
     */
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        rebind(new CompositeName(name), obj, attrs);
    }

    /**Creates and binds a new context, along with associated attributes.
     * This method creates a new subcontext with the given name, binds it
     * in the target context (that named by all but terminal atomic component
     * of the name), and associates the supplied attributes with the newly
     * created object. All intermediate and target contexts must already exist.
     * @param name the name of the context to create.
     * @param attrs the attributes to associate with the newly created context.
     * @return the newly created context
     * @exception NamingException is thrown in the same cases as for the @see NusuthJNDIDirContext() constructor of this class and @see bind(Name, Object , Attributes) method of this class.
     */
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        DirContext dctx = new NusuthJNDIDirContext();
        bind(name, dctx, attrs);
        return dctx;
    }

    /**Creates and binds a new context, along with associated attributes.
     * This method creates a new subcontext with the given name, binds it
     * in the target context (that named by all but terminal atomic component
     * of the name), and associates the supplied attributes with the newly
     * created object. All intermediate and target contexts must already exist.
     * @param name the name of the context to create.
     * @param attrs the attributes to associate with the newly created context.
     * @return the newly created context
     * @exception NamingException is thrown in the same cases as for the @see NusuthJNDIDirContext() constructor of this class and @see bind(Name, Object , Attributes) method of this class.
     */
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return createSubcontext(new CompositeName(name), attrs);
    }

    /**Retrieves the schema associated with the named object.
     * The schema describes rules regarding the structure of the namespace
     * and the attributes stored within it. The schema specifies what types
     * of objects can be added to the directory and where they can be added;
     * what mandatory and optional attributes an object can have.
     * The range of support for schemas is directory-specific.
     * This method returns the root of the schema information
     * tree that is applicable to the named object. Several named objects
     * (or even an entire directory) might share the same schema.
     * Issues such as structure and contents of the schema tree,
     * permission to modify to the contents of the schema tree,
     * and the effect of such modifications on the directory are
     * dependent on the underlying directory.
     * @depricated All ways throws OperationNotSupportedException exception.
     * @param name the name of the object whose schema is to be retrieved.
     * @return the schema associated with the context; never null.
     * @exception OperationNotSupportedException - if schema not supported.
     * @exception NamingException - if a naming exception is encountered.
     */
    public DirContext getSchema(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**Retrieves the schema associated with the named object. See @see getSchema(Name) for more details.
     * @depricated All ways throws OperationNotSupportedException() exception.
     * @param name the name of the object whose schema is to be retrieved.
     * @return the schema associated with the context; never null.
     * @exception OperationNotSupportedException - if schema not supported.
     * @exception NamingException - if a naming exception is encountered.
     */
    public DirContext getSchema(String name) throws NamingException {
        return getSchema(new CompositeName(name));
    }

    /**Retrieves a context containing the schema objects of the named object's
     * class definitions.
     * @depricated All ways throws OperationNotSupportedException() exception.
     * @param name the name of the object whose object class definition is to be retrieved.
     * @return the DirContext containing the named object's class definitions; never null
     * @exception OperationNotSupportedException if schema not supported
     * @exception NamingException - if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**Retrieves a context containing the schema objects of the named object's
     * @depricated All ways throws OperationNotSupportedException() exception.
     * @param name the name of the object whose object class definition is to be retrieved.
     * @return the DirContext containing the named object's class definitions; never null
     * @exception OperationNotSupportedException if schema not supported
     * @exception NamingException - if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return getSchemaClassDefinition(new CompositeName(name));
    }

    /**Searches in a single context for objects that contain a specified set of attributes,
     * and retrieves selected attributes. The search is performed using the default
     * SearchControls settings.///XXXXXX ????? For an object to be selected, each attribute in
     * matchingAttributes must match some attribute of the object.
     * If matchingAttributes is empty or null, all objects in the
     * target context are returned.
     * @param name the name of the context to search.
     * @param matchingAttributes the attributes to search for. If empty or null, all objects in the target context are returned.
     * @param attributesToReturn the attributes to return. null indicates that all attributes are to be returned; an empty array indicates that none are to be returned.
     * @returna non-null enumeration of SearchResult objects. Each SearchResult contains the attributes identified by attributesToReturn and the name of the corresponding object, named relative to the context named by name.
     * @exception NamingException if any exception occures while performing this method.
     * @return NotContextException if the given name is not empty and there is no sub context with the given name.
     */
    public NamingEnumeration search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        if (name.isEmpty()) {
            if (matchingAttributes == null || matchingAttributes.size() == 0) {
                //All objects will be returned :
                NamingEnumeration ne1 = listBindings(name);
                NusuthNamingEnumeration jne = new NusuthNamingEnumeration();
                Binding bnd;
                while (ne1.hasMore()) {
                    bnd = (Binding) ne1.next();
                    jne.addElement(new SearchResult(bnd.getName(), bnd.getObject(), getAttributes(bnd.getName(), attributesToReturn)));
                }
                return jne;
            } else {
                // If we have to select objects :
                NamingEnumeration ne = matchingAttributes.getAll();
                List oldList = new ArrayList();
                List newList = new ArrayList();
                Enumeration enum = bindings.keys();
                while (enum.hasMoreElements()) {
                    oldList.add(enum.nextElement());
                }
                Attribute mattr, attr;
                List lst;
                Iterator iterator;
                NamingEnumeration nenum;
                NameAttributePair nap;
                boolean proceed;
                // Starts searching for an objects with the needed attributes :
                while (ne.hasMore()) {
                    newList.clear();
                    mattr = (Attribute) ne.next();
                    lst = (List) attributes2names.get(mattr.getID());
                    if (lst == null || lst.size() == 0)
                        return new NusuthNamingEnumeration();
                    iterator = lst.iterator();
                    while (iterator.hasNext()) {
                        nap = (NameAttributePair) iterator.next();
                        nenum = mattr.getAll();
                        attr = nap.getAttribute();
                        proceed = true;
                        while (proceed && nenum.hasMore()) {
                            proceed = attr.contains(nenum.next());
                        }
                        if (proceed && oldList.contains(nap.getName()))
                            newList.add(nap.getName());
                    }
                    if (newList.size() == 0)
                        return new NusuthNamingEnumeration();
                    oldList.clear();
                    oldList.addAll(newList);
                }
                NusuthNamingEnumeration jne = new NusuthNamingEnumeration();
                iterator = oldList.iterator();
                Name mname;
                while (iterator.hasNext()) {
                    mname = (Name) iterator.next();
                    jne.addElement(new SearchResult(mname.toString(), lookup(mname), getAttributes(mname, attributesToReturn)));
                }
                return jne;
            }
        } else {
            //System.out.println("Searching for "+name);
            //If name is not empty it means that we want to make search in some sub context.
            Object obj = lookup(name);
            if (obj == null)
                throw new InvalidNameException();
            try {
                return ((DirContext) obj).search(new CompositeName(), matchingAttributes, attributesToReturn);
            } catch (ClassCastException cce) {
                throw new NotContextException();
            }
        }
    }

    /**Searches in a single context for objects that contain a specified set of attributes,
     * and retrieves selected attributes. See @see search(Name , Attributes,String[]  ) method of this class for more details.
     * @param name the name of the context to search.
     * @param matchingAttributes the attributes to search for. If empty or null, all objects in the target context are returned.
     * @param attributesToReturn the attributes to return. null indicates that all attributes are to be returned; an empty array indicates that none are to be returned.
     * @returna non-null enumeration of SearchResult objects. Each SearchResult contains the attributes identified by attributesToReturn and the name of the corresponding object, named relative to the context named by name.
     * @exception NamingException if any exception occures while performing this method.
     * @return NotContextException if the given name is not empty and there is no sub context with the given name.
     */
    public NamingEnumeration search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return search(name.length() == 0 ? new CompositeName() : new CompositeName(name), matchingAttributes, attributesToReturn);
    }

    /**Searches in a single context for objects that contain a specified set of attributes. This method returns all the attributes of such objects. It is equivalent to supplying null as the atributesToReturn parameter to the method search(Name, Attributes, String[]).
     * See @see search(Name , Attributes,String[] ) method of this class for more details.
     * @param name the name of the context to search.
     * @param matchingAttributes the attributes to search for. If empty or null, all objects in the target context are returned.
     * @returna non-null enumeration of SearchResult objects. Each SearchResult contains the attributes identified by attributesToReturn and the name of the corresponding object, named relative to the context named by name.
     * @exception NamingException if any exception occures while performing this method.
     * @return NotContextException if the given name is not empty and there is no sub context with the given name.
     */
    public NamingEnumeration search(Name name, Attributes matchingAttributes) throws NamingException {
        return search(name, matchingAttributes, null);
    }

    /**Searches in a single context for objects that contain a specified set of attributes. This method returns all the attributes of such objects. It is equivalent to supplying null as the atributesToReturn parameter to the method search(Name, Attributes, String[]).
     * See @see search(Name , Attributes,String[] ) method of this class for more details.
     * @param name the name of the context to search.
     * @param matchingAttributes the attributes to search for. If empty or null, all objects in the target context are returned.
     * @returna non-null enumeration of SearchResult objects. Each SearchResult contains the attributes identified by attributesToReturn and the name of the corresponding object, named relative to the context named by name.
     * @exception NamingException if any exception occures while performing this method.
     * @return NotContextException if the given name is not empty and there is no sub context with the given name.
     */
    public NamingEnumeration search(String name, Attributes matchingAttributes) throws NamingException {
        return search(name, matchingAttributes, null);
    }

    /**Searches in the named context or object for entries that satisfy the given search filter. Performs the search as specified by the search controls.
     * @depricated All ways throws OperationNotSupportedException exception.
     * @param  name the name of the context or object to search.
     * @param filter the filter expression to use for the search; may not be null.
     * @param cons the search controls that control the search. If null, the default search controls are used.
     * @return an enumeration of SearchResults of the objects that satisfy the filter; never null
     * @exception InvalidSearchFilterException - if the search filter specified is not supported or understood by the underlying directory
     * @exception InvalidSearchControlsException - if the search controls contain invalid settings
     * @exception NamingException - if a naming exception is encountered
     */
    public NamingEnumeration search(Name name, String filter, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**Searches in the named context or object for entries that satisfy the given search filter. Performs the search as specified by the search controls.
     * @depricated All ways throws OperationNotSupportedException exception.
     * @param  name the name of the context or object to search.
     * @param filter the filter expression to use for the search; may not be null.
     * @param cons the search controls that control the search. If null, the default search controls are used.
     * @return an enumeration of SearchResults of the objects that satisfy the filter; never null
     * @exception InvalidSearchFilterException - if the search filter specified is not supported or understood by the underlying directory
     * @exception InvalidSearchControlsException - if the search controls contain invalid settings
     * @exception NamingException - if a naming exception is encountered
     */
    public NamingEnumeration search(String name, String filter, SearchControls cons) throws NamingException {
        return search(new CompositeName(name), filter, cons);
    }

    /**Searches in the named context or object for entries that satisfy the given search filter. Performs the search as specified by the search controls.
     * @depricated All ways throws OperationNotSupportedException exception.
     * @param name the name of the context or object to search
     * @param filterExpr the filter expression to use for the search. The expression may contain variables of the form "{i}" where i is a nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the variables in filterExpr. The value of filterArgs[i] will replace each occurrence of "{i}". If null, equivalent to an empty array.
     * @param cons the search controls that control the search. If null, the default search controls are used (equivalent to (new SearchControls())).
     * @return an enumeration of SearchResults of the objects that satisy the filter; never null
     * @exception ArrayIndexOutOfBoundsException - if filterExpr contains {i} expressions where i is outside the bounds of the array filterArgs
     * @exception InvalidSearchControlsException - if cons contains invalid settings
     * @exception InvalidSearchFilterException - if filterExpr with filterArgs represents an invalid search filter
     * @exception NamingException - if a naming exception is encountered
     */
    public NamingEnumeration search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        throw new OperationNotSupportedException();
    }

    /**Searches in the named context or object for entries that satisfy the given search filter. Performs the search as specified by the search controls.
     * @depricated All ways throws OperationNotSupportedException exception.
     * @param name the name of the context or object to search
     * @param filterExpr the filter expression to use for the search. The expression may contain variables of the form "{i}" where i is a nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the variables in filterExpr. The value of filterArgs[i] will replace each occurrence of "{i}". If null, equivalent to an empty array.
     * @param cons the search controls that control the search. If null, the default search controls are used (equivalent to (new SearchControls())).
     * @return an enumeration of SearchResults of the objects that satisy the filter; never null
     * @exception ArrayIndexOutOfBoundsException - if filterExpr contains {i} expressions where i is outside the bounds of the array filterArgs
     * @exception InvalidSearchControlsException - if cons contains invalid settings
     * @exception InvalidSearchFilterException - if filterExpr with filterArgs represents an invalid search filter
     * @exception NamingException - if a naming exception is encountered
     */
    public NamingEnumeration search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return search(new CompositeName(name), filterExpr, filterArgs, cons);
    }
}

