package com.azoft.nusuth.management.security;

import com.azoft.nusuth.management.Manageable;
import com.azoft.nusuth.management.ManagementUtil;
import com.azoft.nusuth.deployment.*;

import org.apache.log4j.Category;

import java.security.*;

public class SignatureChecker
        implements Manageable {
    private Category logger = Category.getInstance(this.getClass());

    private CompositeNusuthWebAppElement keystoreNode;
    private boolean isClusterManager = false;
    private Key key;
    private byte[] componentSignature;
    private Signature sign;

    public SignatureChecker(boolean isClusterManager) {
        this.isClusterManager = isClusterManager;
        logger.info("Signature checker started");
    }

    public void applySettings(CompositeNusuthWebAppElement newSettings)
            throws DeploymentException {
        logger.info("Apply new settings");
        CompositeNusuthWebAppElement newManagerNode = ManagementUtil.getCompositeElement(newSettings, "manager");
        CompositeNusuthWebAppElement newKeystoreNode = ManagementUtil.getCompositeElement(newManagerNode, "keystore");
        // process keystore node
        if (keystoreNode == null
                || !ManagementUtil.getSimpleString(keystoreNode, "location").equals(ManagementUtil.getSimpleString(newKeystoreNode, "location"))
                || !ManagementUtil.getSimpleString(keystoreNode, "password").equals(ManagementUtil.getSimpleString(newKeystoreNode, "password"))
                || !ManagementUtil.getSimpleString(keystoreNode, "cluster-key").equals(ManagementUtil.getSimpleString(newKeystoreNode, "cluster-key"))) {

            // load new keystore
            KeyStore keystore = ManagementUtil.loadKeystore(newKeystoreNode);

            // load new key
            try {
                String clusterKey = ManagementUtil.getSimpleString(newKeystoreNode, "cluster-key");
                sign = Signature.getInstance("SHA1withDSA");
                if (isClusterManager) {
                    String password = ManagementUtil.getSimpleString(newKeystoreNode, "password");
                    char[] passwordChars = new char[password.length()];
                    password.getChars(0, password.length(), passwordChars, 0);

                    this.key = (PrivateKey) keystore.getKey(clusterKey, passwordChars);
                    sign.initSign((PrivateKey) key);
                } else {
                    this.key = keystore.getCertificate(clusterKey).getPublicKey();
                    sign.initVerify((PublicKey) key);
                }
            } catch (GeneralSecurityException gsex) {
                logger.error("Couldn't get keys from keystore", gsex);
                throw new DeploymentException("Couldn't get keys from keystore, nested:" + gsex.getMessage());
            }
        }
        keystoreNode = newKeystoreNode;
    }

    public boolean isRestartNeeded(CompositeNusuthWebAppElement newSettings)
            throws DeploymentException {
        return false;
    }

    public byte[] getSignature(byte[] message) {
        logger.debug("Get signature");
        if (key == null || !isClusterManager)
            return null;

        try {
            sign.update(message);
            return sign.sign();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public boolean checkSignature(byte[] signature) {
        logger.debug("Check signature");
        if (key == null || isClusterManager)
            return false;

        try {
            sign.update(java.net.InetAddress.getLocalHost().getHostAddress().getBytes());
            return sign.verify(signature);
        } catch (Exception ex) {
            logger.info("Coudn't verify signature", ex);
            return false;
        }
    }
}
