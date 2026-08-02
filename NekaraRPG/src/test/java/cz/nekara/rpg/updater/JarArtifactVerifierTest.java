package cz.nekara.rpg.updater;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JarArtifactVerifierTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsMatchingHashIdentityAndVersion() throws Exception {
        Path jar = createJar("NekaraRPG", "1.2.0", true);
        assertDoesNotThrow(() -> JarArtifactVerifier.verify(
                jar,
                Files.size(jar),
                JarArtifactVerifier.sha256(jar),
                "1.2.0"
        ));
    }

    @Test
    void rejectsWrongHashVersionAndMissingPluginDescriptor() throws Exception {
        Path jar = createJar("NekaraRPG", "1.2.0", true);
        assertThrows(IOException.class, () -> JarArtifactVerifier.verify(
                jar, Files.size(jar), "0".repeat(64), "1.2.0"));
        assertThrows(IOException.class, () -> JarArtifactVerifier.verify(
                jar, Files.size(jar), JarArtifactVerifier.sha256(jar), "1.3.0"));

        Path missingDescriptor = createJar("NekaraRPG", "1.2.0", false);
        assertThrows(IOException.class, () -> JarArtifactVerifier.verify(
                missingDescriptor,
                Files.size(missingDescriptor),
                JarArtifactVerifier.sha256(missingDescriptor),
                "1.2.0"
        ));
    }

    private Path createJar(String title, String version, boolean includePluginYml) throws IOException {
        Path jar = temporaryDirectory.resolve(UUIDHolder.nextName());
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Implementation-Title", title);
        attributes.putValue("Implementation-Version", version);
        try (OutputStream output = Files.newOutputStream(jar);
             JarOutputStream jarOutput = new JarOutputStream(output, manifest)) {
            if (includePluginYml) {
                jarOutput.putNextEntry(new JarEntry("plugin.yml"));
                jarOutput.write(("name: NekaraRPG\nversion: " + version + "\n").getBytes(
                        java.nio.charset.StandardCharsets.UTF_8));
                jarOutput.closeEntry();
            }
        }
        return jar;
    }

    private static final class UUIDHolder {
        private UUIDHolder() {
        }

        private static String nextName() {
            return java.util.UUID.randomUUID() + ".jar";
        }
    }
}
