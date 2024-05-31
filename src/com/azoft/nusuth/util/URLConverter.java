package com.azoft.nusuth.util;

import java.io.UnsupportedEncodingException;

public class URLConverter {

    public static final byte PERCENT_B = '%';
    public static final byte PLUS_B = '+';
    public static final byte SPACE_B = ' ';

    public static String decodeURL(String url) {

        if (url.indexOf('%') == -1 && url.indexOf('+') == -1) {
            return url;
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    sb.append((char) (convertHexDigit(url.charAt(i + 1)) * 16 + convertHexDigit(url.charAt(i + 2))));
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }

        return sb.toString();
    }

    public static String decodeURL(StrBuffer url) {

        if (!url.containsChar('%') && !url.containsChar('+')) {
            return url.toString();
        }

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < url.curPos; i++) {
            char c = url.buffer[i];
            switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    if (i + 1 > url.curPos) {
                        throw new IllegalArgumentException();
                    }
                    sb.append((char) (convertHexDigit(url.buffer[i + 1]) * 16 + convertHexDigit(url.buffer[i + 2])));
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }

        return sb.toString();
    }

    public static String decodeURL(ByteBuffer url, String encoding) throws UnsupportedEncodingException {

        if (!url.containsByte(PERCENT_B) && !url.containsByte(PLUS_B)) {
            return url.toString();
        }

        ByteBuffer sb = new ByteBuffer();
        for (int i = 0; i < url.curPos; i++) {
            byte b = url.buffer[i];
            switch (b) {
                case PLUS_B:
                    sb.append(SPACE_B);
                    break;
                case '%':
                    if (i + 1 > url.curPos) {
                        throw new IllegalArgumentException();
                    }
                    sb.append((byte) (convertHexDigit(url.buffer[i + 1]) * 16 + convertHexDigit(url.buffer[i + 2])));
                    i += 2;
                    break;
                default:
                    sb.append(b);
                    break;
            }
        }

        return sb.toString(encoding);
    }

    private static int convertHexDigit(char c) throws IllegalArgumentException {
        int result = 0;
        if (c >= '0' && c <= '9') {
            result = c - '0';
        } else if (c >= 'a' && c <= 'f') {
            result = 10 + c - 'a';
        } else if (c >= 'A' && c <= 'F') {
            result = 10 + c - 'A';
        } else {
            throw new IllegalArgumentException();
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

