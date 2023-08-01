package com.valuephone.image.utilities;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
public final class SecurityUtil {

    private static final String SECURITY_PREFIX = "4909450f781f0153427ea6d7dcaced20";

    private static final String SECURITY_REPLACEMENT = "*****";

    private SecurityUtil() {
    }

    public static String encodePassword(String plainPassword) {

        CheckUtilities.checkStringArgumentNotEmpty(plainPassword, "plainPassword");

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-512");

            byte[] output = digest.digest(plainPassword.getBytes());

            String base64Encoded = encodeBase64(output);

            return base64Encoded.replaceAll(System.lineSeparator(), "");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    public static String hidePassword(String password) {
        return password != null ? SECURITY_REPLACEMENT : null;
    }

    public static String encodeBase64(byte[] data) {
        return new String(Base64.getEncoder().encode(data));
    }

    public static boolean isPasswordHashValid(String hash, String password) {

        if (hash == null || password == null) {
            return false;
        }

        if (isValidMD5(hash)) {
            return hash.equals(md5sum(password));
        }

        return hash.equals(hashPassword(password));
    }

    public static String hashPassword(String password) {
        return sha256sum(SECURITY_PREFIX + password);
    }

    public static String sha256sum(String string) {
        return internalSum("SHA-256", string);
    }

    /**
     * @param string
     * @return
     * @deprecated No more MD5!
     */
    @Deprecated
    public static String md5sum(String string) {
        return internalSum("MD5", string);
    }

    private static String internalSum(String algorithm, String string) {

        CheckUtilities.checkStringArgumentNotEmpty(algorithm, "algorithm");

        CheckUtilities.checkStringArgumentNotEmpty(string, "string");

        try {

            MessageDigest md = MessageDigest.getInstance(algorithm);

            byte[] buffer = md.digest(string.getBytes());

            StringBuilder stringBuilder = new StringBuilder();

            for (byte aBuffer : buffer) {

                String b = Integer.toHexString(aBuffer & 255);

                if (b.length() < 2) {
                    b = "0" + b;
                }

                stringBuilder.append(b);
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            throw new IllegalStateException(e.getMessage());
        }
    }

    public static boolean isValidMD5(String s) {
        return s.matches("[a-fA-F0-9]{32}");
    }
}
