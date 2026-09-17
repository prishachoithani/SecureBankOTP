package com.securebank.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String KEY_FILE = "data/secret.key";

    private final SecretKey secretKey;

    public EncryptionUtil() throws NoSuchAlgorithmException, IOException {
        this.secretKey = loadOrCreateKey();
    }

    private SecretKey loadOrCreateKey() throws NoSuchAlgorithmException, IOException {
        Path keyPath = Path.of(KEY_FILE);
        if (Files.exists(keyPath)) {
            byte[] encoded = Base64.getDecoder().decode(Files.readString(keyPath).trim());
            return new SecretKeySpec(encoded, ALGORITHM);
        }

        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(128);
        SecretKey key = keyGen.generateKey();

        Files.createDirectories(keyPath.getParent());
        Files.writeString(keyPath, Base64.getEncoder().encodeToString(key.getEncoded()));
        return key;
    }

    public void encryptToFile(String plainText, String filePath) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());

        try (FileOutputStream fos = new FileOutputStream(filePath);
             OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            writer.write(Base64.getEncoder().encodeToString(encrypted));
        }
    }

    public String decryptFromFile(String filePath) throws Exception {
        String encodedText;
        try (FileInputStream fis = new FileInputStream(filePath);
             InputStreamReader reader = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            encodedText = sb.toString();
        }

        byte[] decoded = Base64.getDecoder().decode(encodedText);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(decoded));
    }
}
