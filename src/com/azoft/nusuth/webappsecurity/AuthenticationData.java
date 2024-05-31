package com.azoft.nusuth.webappsecurity;

import com.azoft.nusuth.gui.MD5;

import javax.security.cert.X509Certificate;

/**
 * Holds data that identifies HTTP user. In dependance of authentication method
 * it holds username and password, or user certificate.
 *
 * @author IgorK
 * @version 1.5
 * @since JBird 1.0
 */
public class AuthenticationData {
    /** Identifies basic HTTP authentication.
     * @see #AUTH_METHOD_BASIC_NAME
     */
    public final static int AUTH_METHOD_BASIC = 0;
    /** Identifies Digest authentication. Not implemented at that time.
     * @see #AUTH_METHOD_DIGEST_NAME
     */
    public final static int AUTH_METHOD_DIGEST = 1;
    /** Identifies HTTP authentication by form posting.
     * @see #AUTH_METHOD_FORM_NAME
     */
    public final static int AUTH_METHOD_FORM = 2;
    /** Identifies SSL authentication.
     * @see #AUTH_METHOD_CERT_NAME
     */
    public final static int AUTH_METHOD_CERT = 3;

    /** Identifies basic HTTP authentication. */
    public final static String AUTH_METHOD_BASIC_NAME = "BASIC";
    /** Identifies Digest authentication. Not implemented at that time. */
    public final static String AUTH_METHOD_DIGEST_NAME = "DIGEST";
    /** Identifies HTTP authentication by form posting. */
    public final static String AUTH_METHOD_FORM_NAME = "FORM";
    /** Identifies SSL authentication. */
    public final static String AUTH_METHOD_CERT_NAME = "CLIENT-CERT";

    private int authType;
    private String userName;
    private String password;
    private X509Certificate certificate;

    public static int methodName2int(String authMethod) {
        if (authMethod.equalsIgnoreCase(AUTH_METHOD_BASIC_NAME))
            return AUTH_METHOD_BASIC;
        else if (authMethod.equalsIgnoreCase(AUTH_METHOD_DIGEST_NAME))
            return AUTH_METHOD_DIGEST;
        else if (authMethod.equalsIgnoreCase(AUTH_METHOD_FORM_NAME))
            return AUTH_METHOD_FORM;
        else if (authMethod.equalsIgnoreCase(AUTH_METHOD_CERT_NAME))
            return AUTH_METHOD_CERT;
        else
            return -1;
    }

    public static String int2methodName(int authMethod) {
        switch (authMethod) {
            case AUTH_METHOD_BASIC:
                return AUTH_METHOD_BASIC_NAME;
            case AUTH_METHOD_DIGEST:
                return AUTH_METHOD_DIGEST_NAME;
            case AUTH_METHOD_FORM:
                return AUTH_METHOD_FORM_NAME;
            case AUTH_METHOD_CERT:
                return AUTH_METHOD_CERT_NAME;
            default:
                return null;
        }
    }

    public AuthenticationData(String authTypeName, String userName, String password) {
        if ((this.authType = methodName2int(authTypeName)) == -1)
            throw new IllegalArgumentException("Unknown authentication type name");
        this.userName = userName;
        this.password = new String(MD5.cryptPassword(password));
    }

    public AuthenticationData(int authType, String userName, String password) {
        if (authType < 0 || authType > 3)
            throw new IllegalArgumentException("Unknown authentication type");
        this.authType = authType;
        this.userName = userName;
        this.password = new String(MD5.cryptPassword(password));
    }

    public int getAuthType() {
        return authType;
    }

    public String getAuthTypeName() {
        return int2methodName(authType);
    }

    public String getUserName() {
        return userName;
    }

    public String getEncodedPassword() {
        return password;
    }

    /**
     * Constructs authentication data with authentication type
     * {@link #AUTH_METHOD_CERT AUTH_METHOD_CERT} and user certificate.
     * @param certificate Certificate that identifies user.
     */
    public AuthenticationData(X509Certificate certificate) {
        this.authType = AUTH_METHOD_CERT;
        this.userName = null;
        this.password = null;
        this.certificate = certificate;
    }

    /**
     * Gets user certificate. Certificate can be obtain only if this object was
     * constructed by {@link #AuthenticationData(X509Certificate)
     * AuthenticationData(X509Certificate)} with non-null parameter.
     * @return {@link X509Certificate X509Certificate} that identifies user,
     * or <code>null</code> if no certificate exists.
     */
    public X509Certificate getCertificate() {
        return certificate;
    }
}

