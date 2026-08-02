package cz.nekara.rpg.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT_ALGORITHM = "PBKDF2-SHA256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final int iterations;
    private final SecureRandom random;

    public PasswordHasher(int iterations) {
        this(iterations, new SecureRandom());
    }

    PasswordHasher(int iterations, SecureRandom random) {
        if (iterations < 1) {
            throw new IllegalArgumentException("Iterations must be positive.");
        }
        this.iterations = iterations;
        this.random = random;
    }

    public String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(password, salt, iterations);
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return "$" + FORMAT_ALGORITHM + "$" + iterations + "$"
                    + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
        } finally {
            java.util.Arrays.fill(hash, (byte) 0);
            java.util.Arrays.fill(salt, (byte) 0);
        }
    }

    public boolean verify(char[] password, String encodedHash) {
        ParsedHash parsed = parse(encodedHash);
        if (parsed == null) {
            return false;
        }
        byte[] actual = derive(password, parsed.salt(), parsed.iterations());
        try {
            return MessageDigest.isEqual(parsed.hash(), actual);
        } finally {
            java.util.Arrays.fill(actual, (byte) 0);
            java.util.Arrays.fill(parsed.salt(), (byte) 0);
            java.util.Arrays.fill(parsed.hash(), (byte) 0);
        }
    }

    public boolean needsRehash(String encodedHash) {
        ParsedHash parsed = parse(encodedHash);
        if (parsed == null) {
            return true;
        }
        try {
            return parsed.iterations() < iterations;
        } finally {
            java.util.Arrays.fill(parsed.salt(), (byte) 0);
            java.util.Arrays.fill(parsed.hash(), (byte) 0);
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterationCount) {
        PBEKeySpec specification = new PBEKeySpec(password, salt, iterationCount, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(specification).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA256 is unavailable.", exception);
        } finally {
            specification.clearPassword();
        }
    }

    private ParsedHash parse(String encodedHash) {
        if (encodedHash == null) {
            return null;
        }
        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 5 || !parts[0].isEmpty() || !FORMAT_ALGORITHM.equals(parts[1])) {
            return null;
        }
        try {
            int parsedIterations = Integer.parseInt(parts[2]);
            if (parsedIterations < 1 || parsedIterations > 5_000_000) {
                return null;
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] salt = decoder.decode(parts[3]);
            byte[] hash = decoder.decode(parts[4]);
            if (salt.length < SALT_BYTES || hash.length != HASH_BITS / 8) {
                java.util.Arrays.fill(salt, (byte) 0);
                java.util.Arrays.fill(hash, (byte) 0);
                return null;
            }
            return new ParsedHash(parsedIterations, salt, hash);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record ParsedHash(int iterations, byte[] salt, byte[] hash) {
    }
}
