package local.aicenter.core;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** A salted one-way password verifier. API keys use Android Keystore encryption separately. */
public final class PasswordCredential {
    private static final int ITERATIONS = 600_000;
    private PasswordCredential() {}
    public static String create(char[] password) throws GeneralSecurityException {
        if (password == null || password.length < 8 || password.length > 1024) throw new IllegalArgumentException("密码需为 8–1024 个字符");
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return "v1$"+ITERATIONS+"$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(hash);
    }
    public static boolean verify(char[] password, String encoded) throws GeneralSecurityException {
        if (password == null || password.length > 1024 || encoded == null || encoded.length() > 512) return false;
        try {
            String[] parts = encoded.split("\\$");
            if (parts.length != 4 || !parts[0].equals("v1")) return false;
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < ITERATIONS || rounds > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length != 16 || expected.length != 32) return false;
            byte[] actual = derive(password, salt, rounds);
            try { return MessageDigest.isEqual(expected, actual); }
            finally { Arrays.fill(actual, (byte)0); }
        } catch (IllegalArgumentException e) { return false; }
    }
    private static byte[] derive(char[] password, byte[] salt, int rounds) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, rounds, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
