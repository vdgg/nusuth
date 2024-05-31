package com.azoft.nusuth.jsp;

import java.util.*;

public class CustomTagFactory implements TagLibraryChangeListener {

    private Hashtable uri2name2tags = new Hashtable();
    private ClassLoader loader;
    private TagLibraryRepository repository;

    public CustomTagFactory(ClassLoader loader, TagLibraryRepository repository) {
        this.loader = loader;
        this.repository = repository;
    }

    public void onTagLibraryChange() {
        synchronized (uri2name2tags) {
            uri2name2tags.clear();
        }
    }

    public Object getCustomTag(String uri, String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Hashtable name2tags = (Hashtable) uri2name2tags.get(uri);
        if (name2tags != null) {
            Stack tags = (Stack) name2tags.get(name);
            if (tags != null) {
                try {
                    synchronized (tags) {
                        return tags.pop();
                    }
                } catch (EmptyStackException ex) {
                }
            }
        }
        RealTagLibrary lib = (RealTagLibrary) ((Hashtable) repository.getRepository()).get(uri);
        String className = lib.findClass4Tag(name);
/*    for (int i=0; i<infos.length; i++) {
      if (infos[i].getTagName().equals(name)) {
        className = infos[i].getTagClassName();
        break;
      }
    }*/
        return loader.loadClass(className).newInstance();
    }

    public void returnToPool(String uri, String name, Object tag) {
        Hashtable name2tags;
        synchronized (uri2name2tags) {
            name2tags = (Hashtable) uri2name2tags.get(uri);
            if (name2tags == null) {
                name2tags = new Hashtable();
                uri2name2tags.put(uri, name2tags);
            }
        }
        Stack tags;
        synchronized (name2tags) {
            tags = (Stack) name2tags.get(name);
            if (tags == null) {
                tags = new Stack();
                name2tags.put(name, tags);
            }
        }
        synchronized (tags) {
            if (tags.size() < 10) {
                tags.push(tag);
            }
        }
    }

    public void clearPool() {
        synchronized (uri2name2tags) {
            uri2name2tags.clear();
        }
    }

    public void setClassLoader(ClassLoader loader) {
        this.loader = loader;
    }

}