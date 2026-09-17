package iwish.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Minimal salted-hash password utility.
 *
 * This is NOT bank-grade security, but it is a meaningful step above
 * storing plaintext passwords, and it's simple enough to implement
 * correctly in a 2-day sprint. Every password in the database should
 * go through hash() before being stored, and verify() should be used
 * on login instead of ever comparing raw strings.
 *
 * Stored format: "<saltBase64>:<hashBase64>"
 * Storing the salt alongside the hash is standard practice — the salt
 * doesn't need to be secret, it just needs to be unique per user so
 * two identical passwords don't produce identical hashes.
 */
public final class PasswordUtil {

    private static final int SALT_LENGTH_BYTES = 16;

    private PasswordUtil() {
        // utility class
    }

    public static String hash(String plainPassword) {
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            new SecureRandom().nextBytes(salt);

            byte[] hashed = hashWithSalt(plainPassword, salt);

            String saltB64 = Base64.getEncoder().encodeToString(salt);
            String hashB64 = Base64.getEncoder().encodeToString(hashed);
            return saltB64 + ":" + hashB64;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String plainPassword, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                return false; // malformed stored value — treat as no match
            }
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

            byte[] actualHash = hashWithSalt(plainPassword, salt);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] hashWithSalt(String plainPassword, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        return digest.digest(plainPassword.getBytes());
    }
}
