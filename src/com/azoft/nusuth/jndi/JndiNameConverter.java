package com.azoft.nusuth.jndi;

/**
 * This class provides methods for encode strings to use it's as a JNDI names,
 * and decode from it.
 * @author igork
 * @version 1.0
 * @since JBird 1.0
 */

public class JndiNameConverter {
    private static char SPECIAL_CHAR = '_';

    /** hex-encode all non-alphanumeric symbols.
     * @return String encoded string
     */
    public static String encode(String str) {
        String result = "";
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')) { // is alphanumeric
                result += c;
            } else {
                result += SPECIAL_CHAR + char2hex(c);
            }
        }
        return result;
    }

    /** decode encoded string
     * @return String decoded (original) string
     */
    public static String decode(String str) {
        String result = "";
        int pos = -1;
        for (int i = str.indexOf(SPECIAL_CHAR);
             i > -1;
             i = str.indexOf(SPECIAL_CHAR, pos)) {
            result += str.substring((pos == -1) ? 0 : pos, i);
            pos = i + 3;

            result += hex2char(str.charAt(i + 1), str.charAt(i + 2));
        }
        result += str.substring(pos == -1 ? 0 : pos);

        return result;
    }

    private static String char2hex(char c) {
        int i1 = (c & 0xF0) >> 4;
        int i2 = c & 0xF;
        char c1 = i1 < 10 ? (char) (i1 + '0') : (char) (i1 + 'A' - 10);
        char c2 = i2 < 10 ? (char) (i2 + '0') : (char) (i2 + 'A' - 10);
        return new String(new char[]{c1, c2});
    }

    private static char hex2char(char c1, char c2) {
        int i1 = c1 >= '0' && c1 <= '9' ? c1 - '0' : c1 - 'A' + 10;
        int i2 = c2 >= '0' && c2 <= '9' ? c2 - '0' : c2 - 'A' + 10;
        return (char) ((i1 << 4) + i2);
    }
}
