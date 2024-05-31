package com.azoft.nusuth.webappsecurity.impl;

import com.azoft.nusuth.util.StrBuffer;

import java.util.*;

class UrlPattern {
    final static int TYPE_PATH = 0;
    final static int TYPE_EXTENSION = 1;
    final static int TYPE_EXACT_MATCH = 2;
    final static int TYPE_ANY = 3;
    final static char[] slash = {'/'};
    int type = TYPE_PATH;
    StrBuffer pattern;
    Map constraints;

    UrlPattern(String patternString, Map constraints) throws IllegalArgumentException {
        this.pattern = new StrBuffer(patternString.length());
        if (patternString.equals(slash)) {
            type = TYPE_ANY;
            pattern.append(slash);
        } else if (patternString.startsWith("/") && patternString.endsWith("/*")) {
            type = TYPE_PATH;
            pattern.append(patternString.substring(0, patternString.length() - 2));
        } else if (pattern.startsWith("*.")) {
            type = TYPE_EXTENSION;
            pattern.append(patternString.substring(2));
        } else {
            type = TYPE_EXACT_MATCH;
            pattern.append(patternString);
        }
        this.constraints = constraints;
    }

    public boolean isMatches(StrBuffer uri) {
        StrBuffer testPattern = pattern;
        switch (type) {
            case TYPE_ANY:
                return true;
            case TYPE_EXACT_MATCH:
                return testPattern.equals(uri);
            case TYPE_EXTENSION:
                return uri.toString().endsWith(pattern.toString());
            case TYPE_PATH:
                return uri.startsWith(testPattern);
        }
        return false;
    }

    public boolean equals(Object obj) {
        if (obj instanceof UrlPattern) {
            UrlPattern o = (UrlPattern) obj;
            return type == o.type && pattern.equals(o.pattern);
        } else if (obj instanceof StrBuffer) {
            return pattern.equals((StrBuffer) obj);
        } else
            return false;
    }

    public int hashCode() {
        return pattern.hashCode();
    }
}

