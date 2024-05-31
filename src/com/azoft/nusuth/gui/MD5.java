package com.azoft.nusuth.gui;

import java.security.*;

public class MD5 {
    private static String method = "{md5}";
    private static MessageDigest md = null;

    static {
        try {
            md = MessageDigest.getInstance("MD5", "SUN");
        } catch (NoSuchAlgorithmException e) {
        } catch (NoSuchProviderException e) {
        }
    }

    /**
     * MD5 constructor comment.
     */
    public MD5() {
    }

    private static String cutHead(String s) {
        if (s.startsWith(method)) {
            return s.substring(method.length());
        }
        return s;
    }

    public static byte[] getPassword(String p) {
        if (md == null) return null;
        if (p.equals("")) return "".getBytes();
        md.reset();
        md.update(p.getBytes());
        byte[] bb = md.digest();
        byte[] b = new String(Base64.encode(bb)).getBytes();
        byte[] ret = new byte[method.length() + b.length];
        byte[] b1 = method.getBytes();
        for (int i = 0; i < method.length(); i++) {
            ret[i] = b1[i];
        }
        ;
        for (int i = method.length(); i < ret.length; i++) {
            ret[i] = b[i - method.length()];
        }
        return ret;
    }

    public static boolean verifyPassword(String ss, String s1) {
        if (md == null || ss == null || s1 == null) return false;
        if (ss.equals("") && s1.equals("")) return true;
        String s = cutHead(ss);
        md.reset();
        md.update(s1.getBytes());
        byte[] b1 = md.digest();
        String sDig = new String(Base64.encode(b1));
        return s.equals(sDig);
    }

    public static String cryptPassword(String p) {
        return new String(getPassword(p));
    }
}
