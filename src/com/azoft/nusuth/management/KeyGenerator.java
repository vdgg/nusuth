package com.azoft.nusuth.management;

import java.io.*;
import java.util.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;

public class KeyGenerator {
    private final static String ALGHORITM = "RSA";
    private final static String KEYSTORE_TYPE = "JKS";
    private final static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /** contains mappings 'component alias' -> 'keystore filename' */
    private HashMap keystores;

    private class NoMoreComponentsException extends Exception {
    }

    private class KeystoreInfo {
        String location;
        String password;

        KeystoreInfo(String location, String password) {
            this.location = location;
            this.password = password;
        }
    }

    public KeyGenerator() {
        keystores = new HashMap();
    }


    public static void main(String[] args) {
        if (args.length != 2)
            System.out.println("Usage: java " + KeyGenerator.class.getName() + " <keytool command> <path to store generated keystore files>");
        else {
            KeyGenerator generator = new KeyGenerator();
            generator.generate(args[0], args[1]);
        }
    }

    private void generate(String keytoolPath, String keystoresPath) {
        try {
            String clusterKeyAlias = createClusterKeystore(keytoolPath, keystoresPath);
            while (true) {
                try {
                    createComponentKeystore(keytoolPath, keystoresPath);
                } catch (NoMoreComponentsException nmcex) {
                    break;
                }
            }

            exchangeKeys(clusterKeyAlias);
        } catch (NoMoreComponentsException nmcex) {
        } catch (Exception ex) {
            System.out.println("Keystore creating failed: " + ex.getMessage());
        }
    }

    private static String ask(String message, String defaultValue) throws IOException {
        System.out.print(message);
        String result = reader.readLine().trim();
        if (result.length() == 0)
            return defaultValue;
        else
            return result;
    }


    private String createClusterKeystore(String keytoolPath, String keystoresPath) throws NoMoreComponentsException, Exception {
        String type = "cluster";

        String alias = ask("Enter cluster key alias (<Enter> for exit):", "");
        if (alias.length() == 0)
            throw new NoMoreComponentsException();

        String fileName = ask("Enter filename for cluster manager keystore (<Enter> for exit):", "");
        if (fileName.length() == 0)
            throw new NoMoreComponentsException();
        if (fileName.indexOf('.') < 0)
            fileName += ".keys";
        fileName = new File(keystoresPath, fileName).getAbsolutePath();

        String password = "";
        while ((password = ask("Enter password for cluster manager keystore (at least 6 characters, or <Enter> for exit):", "")).length() < 6) {
            if (password.length() == 0)
                throw new NoMoreComponentsException();
        }
        keystores.put(alias, new KeystoreInfo(fileName, password));

        createKeystore(keytoolPath, fileName, password, alias);
        return alias;
    }

    private void createComponentKeystore(String keytoolPath, String keystoresPath) throws NoMoreComponentsException, Exception {
        String type = ask("Enter component type (1-distributor, 2-container, 3-deployer, <Enter> for exit):", "");
        if (type.length() == 0)
            throw new NoMoreComponentsException();
        switch (Integer.parseInt(type)) {
            case 1:
                type = "distributor";
                break;
            case 2:
                type = "container";
                break;
            case 3:
                type = "deployer";
                break;
        }

        String name = ask("Enter component ID (<Enter> for exit):", "");
        if (name.length() == 0)
            throw new NoMoreComponentsException();

        String alias = type + '/' + name;
        if (keystores.containsKey(alias)) {
            System.out.println(type + " \"" + name + "\" already defined!");
            throw new NoMoreComponentsException();
        }

        String fileName = ask("Enter filename for keystore (<Enter> for exit):", "");
        if (fileName.length() == 0)
            throw new NoMoreComponentsException();
        if (fileName.indexOf('.') < 0)
            fileName += ".keys";
        fileName = new File(keystoresPath, fileName).getAbsolutePath();

        String password = ask("Enter password for keystore (<Enter> for exit):", "");
        if (password.length() == 0)
            throw new NoMoreComponentsException();

        keystores.put(alias, new KeystoreInfo(fileName, password));

        createKeystore(keytoolPath, fileName, password, alias);
    }


    private static void createKeystore(String keytoolPath, String keystoreFileName, String password, String alias) throws NoMoreComponentsException, Exception {
        String[] cmdArray = new String[14];
        cmdArray[0] = keytoolPath;
        cmdArray[1] = "-genkey";
        cmdArray[2] = "-alias";
        cmdArray[3] = alias;
        cmdArray[4] = "-dname";
        cmdArray[5] = "CN=unknown, OU=unknown, O=unknown, L=unknown, ST=unknown, C=RU";
        cmdArray[6] = "-keypass";
        cmdArray[7] = password;
        cmdArray[8] = "-keystore";
        cmdArray[9] = keystoreFileName;
        cmdArray[10] = "-storepass";
        cmdArray[11] = password;
        cmdArray[12] = "-storetype";
        cmdArray[13] = KEYSTORE_TYPE;

        Process keytoolProcess = Runtime.getRuntime().exec(cmdArray);
        if (keytoolProcess.waitFor() != 0)
            throw new Exception("keytool \"" + keytoolPath + "\" fails");
    }

    private void exchangeKeys(String clusterKeyAlias) throws Exception {
        KeystoreInfo clusterKeystoreInfo = (KeystoreInfo) keystores.get(clusterKeyAlias);
        KeyStore clusterKeystore = KeyStore.getInstance(KEYSTORE_TYPE);
        clusterKeystore.load(new FileInputStream(clusterKeystoreInfo.location), clusterKeystoreInfo.password.toCharArray());

        Certificate clusterCert = clusterKeystore.getCertificate(clusterKeyAlias);

        for (Iterator i = keystores.keySet().iterator(); i.hasNext();) {
            String alias = (String) i.next();
            if (alias.equals(clusterKeyAlias))
                continue;
            KeystoreInfo info = (KeystoreInfo) keystores.get(alias);
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE);
            keystore.load(new FileInputStream(info.location), info.password.toCharArray());
            Certificate cert = keystore.getCertificate(alias);
            keystore.setCertificateEntry(clusterKeyAlias, clusterCert);
            clusterKeystore.setCertificateEntry(alias, cert);
            keystore.store(new FileOutputStream(info.location), info.password.toCharArray());
        }

        clusterKeystore.store(new FileOutputStream(clusterKeystoreInfo.location), clusterKeystoreInfo.password.toCharArray());
    }
}
