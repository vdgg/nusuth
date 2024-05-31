package com.azoft.nusuth.util;

import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Hashtable;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletInputStream;

/**
 * This class is utility class. It provides only static methods.
 * @author vdgg, skilz
 * @version 1.4
 * @since Nusuth1.0
 */
public class Utils {

    public static final byte AMP_B = '&';
    public static final byte EQUAL_B = '=';
    public static final byte PLUS_B = '+';
    public static final byte SPACE_B = ' ';
    public static final byte PERCENT_B = '%';

    public static Vector parseCookies(String cookieString) {
        Vector result = new Vector();
        if (cookieString == null)
            return result;
        StringTokenizer tok = new StringTokenizer(cookieString,
                ";", false);
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            int i = token.indexOf("=");
            if (i > -1) {

                // XXX
                // the trims here are a *hack* -- this should
                // be more properly fixed to be spec compliant

                String name = token.substring(0, i).trim();
                String value = token.substring(i + 1, token.length()).trim();
                // RFC 2109 and bug
                value = stripQuote(value);
                Cookie cookie = new Cookie(name, value);
                result.addElement(cookie);
            } else {
                // we have a bad cookie.... just let it go
            }
        }
        return result;
    }

    private static String stripQuote(String value) {
        if (((value.startsWith("\"")) && (value.endsWith("\""))) ||
                ((value.startsWith("'") && (value.endsWith("'"))))) {
            try {
                return value.substring(1, value.length() - 1);
            } catch (Exception ex) {
            }
        }
        return value;
    }

    public static int retreiveContentLength(NusuthHeaders headers) throws IOException {
        if (headers.containsHeader(HttpConstants.TRANSFER_ENCODING, HttpConstants.CHUNKED)) {
            return -1;
        } else {
            if (headers.containsHeader(HttpConstants.CONTENT_LENGTH)) {
                try {
                    return headers.getIntHeader("Content-Length");
                } catch (NumberFormatException nfe) {
                    throw new IOException("Cannot parse content length, nested: " + nfe);
                }
            } else {
                return -1;
            }
        }
    }

    /**
     * This method parse given query string.
     * @param s Query string.
     * @return Hashtable with parameters.
     */
    public static Hashtable parseQueryString(String s) {
        String valArray[] = null;
        if (s == null)
            throw new IllegalArgumentException();
        Hashtable ht = new Hashtable();
        StrBuffer sb = new StrBuffer();
        StrBuffer name = new StrBuffer();
        StrBuffer value = new StrBuffer();
        String key;
        boolean isName = true;
        boolean equalsFound = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '&') {
                switch (c) {
                    case '=':
                        equalsFound = true;
                        isName = !isName;
                        break;
                    default :
                        if (isName) {
                            name.append(c);
                        } else {
                            value.append(c);
                        }
                        break;
                }
            } else {
                if (!equalsFound) {
                    throw new IllegalArgumentException();
                }
                equalsFound = false;
                isName = true;
                key = parseName(name, sb);
                String val = parseName(value, sb);
                if (ht.containsKey(key)) {
                    String oldVals[] = (String[]) ht.get(key);
                    valArray = new String[oldVals.length + 1];
                    for (int j = 0; j < oldVals.length; j++)
                        valArray[j] = oldVals[j];
                    valArray[oldVals.length] = val;
                    ht.put(key, valArray);
                } else {
                    valArray = new String[1];
                    valArray[0] = val;
                    ht.put(key, valArray);
                }
                name.clear();
                value.clear();
            }
        }
        if (!equalsFound) {
            throw new IllegalArgumentException();
        }
        equalsFound = false;
        key = parseName(name, sb);
        String val = parseName(value, sb);
        if (ht.containsKey(key)) {
            String oldVals[] = (String[]) ht.get(key);
            valArray = new String[oldVals.length + 1];
            for (int j = 0; j < oldVals.length; j++)
                valArray[j] = oldVals[j];
            valArray[oldVals.length] = val;
            ht.put(key, valArray);
        } else {
            valArray = new String[1];
            valArray[0] = val;
            ht.put(key, valArray);
        }
        return ht;
    }

    /**
     * This method parse given query string.
     * @param buf Query string.
     * @return Hashtable with parameters.
     */
    public static Hashtable parseAndDecodeQuery(byte[] buf, String encoding) throws UnsupportedEncodingException {
        String valArray[] = null;
        if (buf == null)
            throw new IllegalArgumentException();
        Hashtable ht = new Hashtable();
        ByteBuffer name = new ByteBuffer();
        ByteBuffer value = new ByteBuffer();
        String key;
        boolean isName = true;
        boolean equalsFound = false;
        for (int i = 0; i < buf.length; i++) {
            if (buf[i] != AMP_B) {
                switch (buf[i]) {
                    case PLUS_B:
                        if (isName) {
                            name.append(SPACE_B);
                        } else {
                            value.append(SPACE_B);
                        }
                        break;
                    case PERCENT_B:
                        if (i + 3 > buf.length) {
                            throw new IllegalArgumentException();
                        }
                        if (isName) {
                            name.append((byte) (convertHexDigit(buf[i + 1]) * 16 + convertHexDigit(buf[i + 2])));
                        } else {
                            value.append((byte) (convertHexDigit(buf[i + 1]) * 16 + convertHexDigit(buf[i + 2])));

                        }
                        i += 2;
                        break;
                    case EQUAL_B:
                        equalsFound = true;
                        isName = !isName;
                        break;
                    default :
                        if (isName) {
                            name.append(buf[i]);
                        } else {
                            value.append(buf[i]);
                        }
                        break;
                }
            } else {
                if (!equalsFound) {
                    throw new IllegalArgumentException();
                }
                equalsFound = false;
                isName = true;
                key = name.toString(encoding);
                String val = value.toString(encoding);
                if (ht.containsKey(key)) {
                    String oldVals[] = (String[]) ht.get(key);
                    valArray = new String[oldVals.length + 1];
                    for (int j = 0; j < oldVals.length; j++)
                        valArray[j] = oldVals[j];
                    valArray[oldVals.length] = val;
                    ht.put(key, valArray);
                } else {
                    valArray = new String[1];
                    valArray[0] = val;
                    ht.put(key, valArray);
                }
                name.clear();
                value.clear();
            }
        }
        if (!equalsFound) {
            throw new IllegalArgumentException();
        }
        equalsFound = false;
        key = name.toString(encoding);
        String val = value.toString(encoding);
        if (ht.containsKey(key)) {
            String oldVals[] = (String[]) ht.get(key);
            valArray = new String[oldVals.length + 1];
            for (int j = 0; j < oldVals.length; j++)
                valArray[j] = oldVals[j];
            valArray[oldVals.length] = val;
            ht.put(key, valArray);
        } else {
            valArray = new String[1];
            valArray[0] = val;
            ht.put(key, valArray);
        }
        return ht;
    }

    /**
     *  Parse a name in the query string.
     */
    private static String parseName(StrBuffer s, StrBuffer sb) {
        sb.clear();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case 43: /* '+' */
                    sb.append(' ');
                    break;
                case 37: /* '%' */
                    try {
                        sb.append((char) Integer.parseInt(s.substring(i + 1, i + 3), 16));
                        i += 2;
                        break;
                    } catch (NumberFormatException _ex) {
                        throw new IllegalArgumentException();
                    } catch (StringIndexOutOfBoundsException _ex) {
                        String rest = s.substring(i);
                        sb.append(rest);
                        if (rest.length() == 2)
                            i++;
                    }
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * This method parse given string to number of milliseconds.
     * @param time Time string.
     * @return parsed time of 60000 if any error occurs during parsing.
     */
    public static long parseTimeToMillis(String time) {
        long result = -1;
        try {
            if (time.toLowerCase().trim().endsWith("h")) {
                result = Integer.parseInt(time.substring(0, time.length() - 1)) * 3600000;
            } else if (time.toLowerCase().trim().endsWith("m")) {
                result = Integer.parseInt(time.substring(0, time.length() - 1)) * 60000;
            } else if (time.toLowerCase().trim().endsWith("s")) {
                result = Integer.parseInt(time.substring(0, time.length() - 1)) * 1000;
            } else {
                result = Integer.parseInt(time) * 1000;
            }
        } catch (NumberFormatException ex) {
            System.err.println("Wrong value in \'check-interval\' init "
                    + "parameter value. Default value will be "
                    + "used (1 minute)...");
            result = 60000;
        }
        return result;
    }

    private static int convertHexDigit(byte b) throws IllegalArgumentException {
        int result = 0;
        if (b >= 0x30 && b <= 0x39) {
            result = b - 0x30;
        } else if (b >= 0x61 && b <= 0x66) {
            result = 10 + b - 0x61;
        } else if (b >= 0x41 && b <= 0x46) {
            result = 10 + b - 0x41;
        } else {
            throw new IllegalArgumentException();
        }
        return result;
    }
}

