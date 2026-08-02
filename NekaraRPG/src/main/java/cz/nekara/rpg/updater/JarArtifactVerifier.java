package cz.nekara.rpg.updater;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public final class JarArtifactVerifier {
    private JarArtifactVerifier() {
    }

    public static void verify(
            Path jarPath,
            long expectedSize,
            String expectedSha256,
            String expectedVersion
    ) throws IOException {
        long actualSize = Files.size(jarPath);
        if (actualSize != expectedSize) {
            throw new IOException("Downloaded JAR size does not match GitHub metadata");
        }
        String actualSha256 = sha256(jarPath);
        if (!MessageDigest.isEqual(
                actualSha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                expectedSha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            throw new IOException("Downloaded JAR SHA-256 does not match GitHub metadata");
        }
        try (JarFile jar = new JarFile(jarPath.toFile(), true)) {
            if (jar.getJarEntry("plugin.yml") == null) {
                throw new IOException("Downloaded JAR does not contain plugin.yml");
            }
            if (jar.getManifest() == null) {
                throw new IOException("Downloaded JAR does not contain a manifest");
            }
            Attributes attributes = jar.getManifest().getMainAttributes();
            String title = attributes.getValue("Implementation-Title");
            String version = attributes.getValue("Implementation-Version");
            if (!"NekaraRPG".equals(title) || !expectedVersion.equals(version)) {
                throw new IOException("Downloaded JAR identity or version does not match the release tag");
            }
        }
    }

    public static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[16_384];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
