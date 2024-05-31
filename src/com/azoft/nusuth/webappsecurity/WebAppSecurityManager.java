package com.azoft.nusuth.webappsecurity;

import com.azoft.nusuth.deployment.CompositeNusuthWebAppElement;

import javax.security.cert.Certificate;
import java.security.Principal;

/**
 * Checks HTTP users access rights to acces web aaplication resources.
 * @author IgorK
 * @author VDGG
 * @version 1.5
 * @since JBird 1.0
 */
public interface WebAppSecurityManager {
    static int ACCESS_GRANTED = 0;
    static int ACCESS_AUTENTICATION_NEEDED = 1;
    static int ACCESS_DENIED = 2;

    /** @return Principal of user if Certificate is valid, or null if not */
    Principal login(Certificate userCertificates);

    /** @return principal of user, if username/password pair is valid, or null if not */
    Principal login(String userName, String encodedPassword);

    /**
     * @param resource
     * @param isSecure true if request is secure
     * @param user
     * @return result of check. Possibly values:
     * {@link #ACCESS_GRANTED ACCESS_GRANTED},
     * {@link #ACCESS_AUTENTICATION_NEEDED ACCESS_AUTENTICATION_NEEDED},
     * {@link #ACCESS_DENIED ACCESS_DENIED}
     */
    int checkAccessRights(ResourceSecurityRecord resource, boolean isSecure, Principal user);

    boolean isUserInRole(String servletName, Principal user, String role);

    /**
     * @return <code>true</code>, if this WebAppSecurityManager sucessully
     * initialized and can work properly, or <code>false</code> if not.
     */
    boolean isEnabled();

    public CompositeNusuthWebAppElement getUsersConfig();
}

