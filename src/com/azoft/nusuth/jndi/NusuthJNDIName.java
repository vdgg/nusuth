package com.azoft.nusuth.jndi;

import com.azoft.nusuth.util.StrBuffer;

import javax.naming.Name;
import javax.naming.InvalidNameException;
import java.util.Enumeration;
import java.io.Serializable;

/**
 * This class implements Name interface/
 * @author skilz
 * @since Nusuth1.0
 * @version 1.2
 */
public class NusuthJNDIName implements Name, Serializable {

    private StrBuffer buff = new StrBuffer();
    private int size = 1;
    private StrBuffer res = new StrBuffer();
    private StrBuffer removed = new StrBuffer();

    /**
     * Constructor.
     * @param name Name
     */
    public NusuthJNDIName(String name) {
        buff.append(name);
        for (int i = 0; i < buff.length(); i++) {
            if (buff.charAt(i) == '/') {
                size++;
            }
        }
    }

    /**
     * Constructs an empty name
     */
    public NusuthJNDIName() {
        size = 1;
    }

    /**
     * Construct new name by given.
     * @param name Name
     */
    public NusuthJNDIName(StrBuffer name) {
        buff.append(name);
        for (int i = 0; i < buff.length(); i++) {
            if (buff.charAt(i) == '/') {
                size++;
            }
        }
    }

    /**
     * Compares name with given object.
     */
    public int compareTo(Object obj) {
        Name name = (Name) obj;
        return buff.toString().compareTo(name.toString());
    }

    /**
     * Return size of name
     * @return size of name.
     */
    public int size() {
        if (buff.length() > 0) {
            return size;
        } else {
            return 1;
        }
    }

    /**
     * Return true if name empty.
     */
    public boolean isEmpty() {
        return (buff.length() == 0);
    }

    public Enumeration getAll() {
        return null;
    }

    public String get(int posn) {
        res.clear();
        if (posn >= size) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int found = 0;
        for (int i = 0; found < (posn + 1) && i < buff.length(); i++) {
            if (buff.charAt(i) == '/') {
                found++;
                break;
            }
            if (found == posn) {
                res.append(buff.charAt(i));
            }
        }
        return res.toString();
    }

    public Name getPrefix(int posn) {
        if (posn > size) {
            throw new ArrayIndexOutOfBoundsException();
        }
        res.clear();
        int found = 0;
        for (int i = 0; found < posn && i < buff.length(); i++) {
            if (buff.charAt(i) == '/') {
                found++;
                if (found == posn) {
                    return new NusuthJNDIName(res);
                }
            }
            res.append(buff.charAt(i));
        }
        return new NusuthJNDIName(res);
    }

    public Name getSuffix(int posn) {
        if (posn > size) {
            throw new ArrayIndexOutOfBoundsException();
        }
        res.clear();
        int found = 0;
        for (int i = 0; i < buff.length(); i++) {
            if (found >= posn) {
                res.append(buff.charAt(i));
            }
            if (buff.charAt(i) == '/') {
                found++;
            }
        }
        return new NusuthJNDIName(res);
    }

    public boolean startsWith(Name n) {
        Name st = getPrefix(n.size());
        return st.toString().equals(n.toString());
    }

    public boolean endsWith(Name n) {
        Name end = getSuffix(n.size());
        return end.toString().equals(n.toString());
    }

    public Name addAll(Name suffix) throws InvalidNameException {
        if (suffix == null || !(suffix instanceof NusuthJNDIName)) {
            throw new InvalidNameException();
        }
        buff.append('/');
        buff.append(suffix.toString());
        size = size + suffix.size();
        return this;
    }

    public Name addAll(int posn, Name n) throws InvalidNameException {
        if (posn > size) {
            throw new IndexOutOfBoundsException();
        }
        if (n == null || !(n instanceof NusuthJNDIName)) {
            throw new InvalidNameException();
        }
        if (posn == size) {
            buff.append('/');
            buff.append(n.toString());
            size = size + n.size();
            return this;
        }
        res.clear();
        int found = 0;
        int position = 0;
        for (int i = 0; found < posn && i < buff.length(); i++) {
            position = i;
            res.append(buff.charAt(i));
            if (buff.charAt(i) == '/') {
                found++;
            }
        }
        res.append(n.toString());
        if (position == 0) {
            res.append('/');
        }
        for (int i = position; i < buff.length(); i++) {
            res.append(buff.charAt(i));
        }
        buff = res.cloneBuf();
        size = size + n.size();
        return this;
    }

    public Name add(String comp) throws InvalidNameException {
        if (comp == null || comp.indexOf('/') != -1) {
            throw new InvalidNameException();
        }
        buff.append('/');
        buff.append(comp);
        size = size + 1;
        return this;
    }

    public Name add(int posn, String comp) throws InvalidNameException {
        if (comp == null || comp.indexOf('/') != -1) {
            throw new InvalidNameException();
        }
        return addAll(posn, new NusuthJNDIName(comp));
    }

    public Object remove(int posn) throws InvalidNameException {
        if (posn >= size) {
            throw new IndexOutOfBoundsException();
        }
        res.clear();
        res.append(getPrefix(posn).toString());
        if (res.length() != 0 && posn != (size - 1)) {
            res.append('/');
        }
        res.append(getSuffix(posn + 1).toString());
        removed.clear();
        removed.append(get(posn).toString());
        buff = res.cloneBuf();
        size--;
        return new NusuthJNDIName(removed);
    }

    public Object clone() {
        return new NusuthJNDIName(buff);
    }

    public String toString() {
        return buff.toString();
    }

    public boolean equals(Object obj) {
        if (obj instanceof NusuthJNDIName) {
            return obj.toString().equals(toString());
        }
        return false;
    }

    public int hashCode() {
        int h = 1;
        for (int i = 0; i < buff.length(); i++) {
            h = 31 * h + buff.charAt(i);
        }
        return h;
    }

}
