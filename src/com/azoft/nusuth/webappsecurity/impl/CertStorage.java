package com.azoft.nusuth.webappsecurity.impl;

import java.util.*;
import java.security.KeyStore;
import java.security.KeyStoreException;

import org.apache.log4j.Category;

public class CertStorage {
    private class UserCertPair {
        AppUser user;
        byte[] encodedCert;

        UserCertPair(AppUser user, byte[] encodedCert) {
            this.user = user;
            this.encodedCert = encodedCert;
        }
    }

    private Category logger = Category.getInstance(this.getClass().getName());
    private Vector[] storage;

    protected CertStorage(KeyStore keystore, Map alias2user) {
        if (keystore == null || alias2user == null || alias2user.size() == 0)
            return;

        int count = 0;
        try {
            for (Enumeration i = keystore.aliases(); i.hasMoreElements();) {
                String alias = (String) i.nextElement();
                try {
                    if (keystore.isCertificateEntry(alias)) {
                        count++;
                    }
                } catch (KeyStoreException ksex) {
                    logger.error("Couldn't get certificate \"" + alias + "\"", ksex);
                }
            }
        } catch (KeyStoreException ksex) {
            logger.error("Bad keystore", ksex);
        }

        if (count > 0) {
            storage = new Vector[count + count / 4];
            for (int i = 0; i < storage.length; i++) {
                storage[i] = new Vector();
            }
            try {
                for (Enumeration i = keystore.aliases(); i.hasMoreElements();) {
                    String alias = (String) i.nextElement();
                    try {
                        add(keystore.getCertificate(alias), (AppUser) alias2user.get(alias));
                    } catch (KeyStoreException ksex) {
                        logger.error("Couldn't get certificate \"" + alias + "\"", ksex);
                    } catch (java.security.cert.CertificateEncodingException ceex) {
                        logger.error("Couldn't encode certificate");
                    }
                }
            } catch (KeyStoreException ksex) {
                logger.error("Bad keystore", ksex);
            }
        }
    }

    private void add(java.security.cert.Certificate cert, AppUser user) throws java.security.cert.CertificateEncodingException {
        if (user != null) {
            int pos = cert.hashCode() % storage.length;
            storage[pos].add(new UserCertPair(user, cert.getEncoded()));
        }
    }

    public AppUser getUser(javax.security.cert.Certificate cert) {
        if (cert != null && storage != null) {
            try {
                byte[] encoded = cert.getEncoded();
                int pos = cert.hashCode() % storage.length;
                Vector variants = storage[pos];
                if (variants != null) {
                    for (Iterator i = variants.iterator(); i.hasNext();) {
                        UserCertPair ucp = (UserCertPair) i.next();
                        if (Arrays.equals(encoded, ucp.encodedCert)) {
                            return ucp.user;
                        }
                    }
                }
            } catch (javax.security.cert.CertificateEncodingException ceex) {
                logger.error("Couldn't encode certificate...", ceex);
            }
        }
        return null;
    }
}

