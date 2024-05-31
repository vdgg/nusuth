package com.azoft.nusuth.util;

import java.math.BigInteger;
import java.util.Random;

/**
 * This class intend for generating keys using RSA algorithm.
 * @author skilz
 * @version 1.0
 * @since Nusuth1.0
 */
public class RsaUtil {

    public BigInteger n, d, e;

    /**
     * Default constructor. It invoke second constructor with 1024 parameter
     */
    public RsaUtil() {
        this(1024);
    }

    /**
     * Constructor.
     * @param bitlen Size of public key
     */
    public RsaUtil(int bitlen) {
        Random r = new Random();
        BigInteger p = new BigInteger(bitlen / 2, 20, r);
        BigInteger q = new BigInteger(bitlen / 2, 20, r);
        n = p.multiply(q);
        BigInteger m
                = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        e = new BigInteger("3");
        while (m.gcd(e).intValue() > 1) e = e.add(new BigInteger("2"));
        d = e.modInverse(m);
    }

    /**
     * Constructor.
     */
    public RsaUtil(BigInteger e, BigInteger n) {
        this.e = e;
        this.n = n;
    }

    /**
     * Encrypts message
     * @param message Message to encrypt.
     * @return BigInteger Encrypted message.
     */
    public BigInteger encrypt(BigInteger message) {
        return message.modPow(e, n);
    }

    /**
     * Decrypts message
     * @param message Message to decrypt.
     * @return BigInteger Decrypted message.
     */
    public BigInteger decrypt(BigInteger message) {
        return message.modPow(d, n);
    }

}
