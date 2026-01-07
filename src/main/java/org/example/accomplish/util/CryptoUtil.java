package org.example.accomplish.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

public class CryptoUtil {
    private static SecureRandom rnd = new SecureRandom();

    public static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        rnd.nextBytes(b);
        return b;
    }

    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            return bytesToHex(d);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static byte[] xor(byte[] a, byte[] b) {
        byte[] out = new byte[a.length];
        for (int i=0;i<a.length;i++) {
            out[i] = (byte)(a[i] ^ b[i % b.length]);
        }
        return out;
    }
}