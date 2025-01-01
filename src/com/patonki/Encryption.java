package com.patonki;

import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;

public class Encryption {
    private static SecretKey getKeyFromPassword(String password)
            throws Exception {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), "saltysalty".getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec)
                .getEncoded(), "AES");
    }

    public static String encrypt(String input, String password) throws Exception{
        SecretKey key = getKeyFromPassword(password);
        System.out.println("Encrypting with " + password + " " + key+" " +System.currentTimeMillis());
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getEncoder()
                .encodeToString(cipherText);
    }

    public static String decrypt(String cipherText, String password) throws Exception {
        SecretKey key = getKeyFromPassword(password);
        System.out.println("Decrypting with " +password +" " + key +" " +System.currentTimeMillis());
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = cipher.doFinal(Base64.getDecoder()
                .decode(cipherText));
        return new String(plainText);
    }
}
