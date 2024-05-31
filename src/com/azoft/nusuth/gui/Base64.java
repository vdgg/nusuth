package com.azoft.nusuth.gui;

/**
 * Insert the type's description here.
 * Creation date: (17.09.2000 16:39:16)
 * @author: Administrator
 * @since Nusuth1.0
 */
public class Base64 {
    private static char alphabet[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();
    private static byte codes[];

    static {
        codes = new byte[256];
        for (int i = 0; i < 256; i++)
            codes[i] = -1;
        for (int j = 65; j <= 90; j++)
            codes[j] = (byte) (j - 65);
        for (int k = 97; k <= 122; k++)
            codes[k] = (byte) ((26 + k) - 97);
        for (int l = 48; l <= 57; l++)
            codes[l] = (byte) ((52 + l) - 48);
        codes[43] = 62;
        codes[47] = 63;
    }

    /**
     * Base64 constructor comment.
     */
    public Base64() {
        super();
    }

    public static byte[] decode(char ac[]) {
        int i = ((ac.length + 3) / 4) * 3;
        if (ac.length > 0 && ac[ac.length - 1] == '=')
            i--;
        if (ac.length > 1 && ac[ac.length - 2] == '=')
            i--;
        byte abyte0[] = new byte[i];
        int j = 0;
        int k = 0;
        int l = 0;
        for (int i1 = 0; i1 < ac.length; i1++) {
            byte byte0 = codes[ac[i1] & 0xff];
            if (byte0 >= 0) {
                k <<= 6;
                j += 6;
                k |= byte0;
                if (j >= 8) {
                    j -= 8;
                    abyte0[l++] = (byte) (k >> j & 0xff);
                }
            }
        }
        if (l != abyte0.length)
            throw new Error("miscalculated data length!");
        else
            return abyte0;
    }

    public static char[] encode(byte abyte0[]) {
        char ac[] = new char[((abyte0.length + 2) / 3) * 4];
        int i = 0;
        for (int j = 0; i < abyte0.length; j += 4) {
            boolean flag = false;
            boolean flag1 = false;
            int k = 0xff & abyte0[i];
            k <<= 8;
            if (i + 1 < abyte0.length) {
                k |= 0xff & abyte0[i + 1];
                flag1 = true;
            }
            k <<= 8;
            if (i + 2 < abyte0.length) {
                k |= 0xff & abyte0[i + 2];
                flag = true;
            }
            ac[j + 3] = alphabet[flag ? k & 0x3f : 64];
            k >>= 6;
            ac[j + 2] = alphabet[flag1 ? k & 0x3f : 64];
            k >>= 6;
            ac[j + 1] = alphabet[k & 0x3f];
            k >>= 6;
            ac[j] = alphabet[k & 0x3f];
            i += 3;
        }
        return ac;
    }
}