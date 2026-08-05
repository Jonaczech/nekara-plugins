package cz.nekara.rpg.skills.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SkillExportService {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter
        .ofPattern("yyyyMMdd-HHmmss")
        .withZone(ZoneOffset.UTC);

    private final SkillSnapshotRepository repository;
    private final Path exportDirectory;
    private final String pluginVersion;
    private final Clock clock;

    public SkillExportService(
        SkillSnapshotRepository repository,
        Path exportDirectory,
        String pluginVersion,
        Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.exportDirectory = Objects.requireNonNull(exportDirectory, "exportDirectory");
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "pluginVersion");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SkillExportResult export() throws IOException {
        Files.createDirectories(exportDirectory);
        Instant createdAt = clock.instant();
        String unique = UUID.randomUUID().toString();
        String archiveName = "NekaraSkills-" + FILE_TIME.format(createdAt) + "-"
            + unique.substring(0, 8) + ".zip";
        Path archive = exportDirectory.resolve(archiveName);
        Path temporaryArchive = exportDirectory.resolve("." + archiveName + ".tmp-" + unique);
        Path snapshot = exportDirectory.resolve(".skills-snapshot-" + unique + ".db");

        try {
            repository.createConsistentSnapshot(snapshot);
            ExportCounts counts = writeArchive(snapshot, temporaryArchive, createdAt);
            moveAtomically(temporaryArchive, archive);
            return new SkillExportResult(
                archive,
                createdAt,
                counts.profiles(),
                Files.size(archive),
                sha256(archive)
            );
        } finally {
            Files.deleteIfExists(snapshot);
            Files.deleteIfExists(temporaryArchive);
        }
    }

    private ExportCounts writeArchive(Path snapshot, Path archive, Instant createdAt)
        throws IOException {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:" + snapshot.toAbsolutePath());
             ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            ExportCounts counts = readCounts(connection);
            String databaseHash = sha256(snapshot);
            writeManifest(zip, createdAt, databaseHash, counts);
            writeFile(zip, "data.db", snapshot);
            writeQuery(zip, connection, "profiles.csv",
                "player_key,spent_perk_points,revision",
                "SELECT player_key,spent_perk_points,revision FROM profiles ORDER BY player_key", 3);
            writeQuery(zip, connection, "skill_experience.csv",
                "player_key,skill_id,total_experience",
                "SELECT player_key,skill_id,total_experience FROM skill_experience "
                    + "ORDER BY player_key,skill_id", 3);
            writeQuery(zip, connection, "perk_ranks.csv",
                "player_key,perk_id,rank",
                "SELECT player_key,perk_id,rank FROM perk_ranks ORDER BY player_key,perk_id", 3);
            writeQuery(zip, connection, "new_game_plus.csv",
                "player_key,skill_id,rank",
                "SELECT player_key,skill_id,rank FROM skill_new_game_plus ORDER BY player_key,skill_id", 3);
            writeQuery(zip, connection, "admin_audit.csv",
                "id,actor_key,actor_name,target_player_key,target_name,operation,detail,"
                    + "occurred_at_epoch_millis,revision_before,revision_after",
                "SELECT id,actor_key,actor_name,target_player_key,target_name,operation,detail,"
                    + "occurred_at_epoch_millis,revision_before,revision_after "
                    + "FROM admin_audit ORDER BY id", 10);
            return counts;
        } catch (SQLException exception) {
            throw new IOException("Could not read the Nekara Skills snapshot", exception);
        }
    }

    private ExportCounts readCounts(Connection connection) throws SQLException, IOException {
        String schemaVersion = scalarText(connection,
            "SELECT value FROM metadata WHERE key='schema-version'");
        if (!"5".equals(schemaVersion)) {
            throw new IOException("Unsupported Nekara Skills export schema " + schemaVersion);
        }
        return new ExportCounts(
            scalarLong(connection, "SELECT COUNT(*) FROM profiles"),
            scalarLong(connection, "SELECT COUNT(*) FROM skill_experience"),
            scalarLong(connection, "SELECT COUNT(*) FROM perk_ranks"),
            scalarLong(connection, "SELECT COUNT(*) FROM skill_new_game_plus"),
            scalarLong(connection, "SELECT COUNT(*) FROM admin_audit")
        );
    }

    private void writeManifest(
        ZipOutputStream zip,
        Instant createdAt,
        String databaseHash,
        ExportCounts counts
    ) throws IOException {
        String manifest = "format=nekara-skills-export-v1\n"
            + "plugin_version=" + pluginVersion + "\n"
            + "schema_version=4\n"
            + "created_at=" + createdAt + "\n"
            + "profiles=" + counts.profiles() + "\n"
            + "experience_rows=" + counts.experienceRows() + "\n"
            + "perk_rows=" + counts.perkRows() + "\n"
            + "new_game_plus_rows=" + counts.newGamePlusRows() + "\n"
            + "audit_rows=" + counts.auditRows() + "\n"
            + "database_sha256=" + databaseHash + "\n";
        writeBytes(zip, "manifest.properties", manifest.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFile(ZipOutputStream zip, String name, Path source)
        throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(source, zip);
        zip.closeEntry();
    }

    private static void writeQuery(
        ZipOutputStream zip,
        Connection connection,
        String entryName,
        String header,
        String sql,
        int columns
    ) throws SQLException, IOException {
        zip.putNextEntry(new ZipEntry(entryName));
        writeLine(zip, header);
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                StringBuilder row = new StringBuilder();
                for (int column = 1; column <= columns; column++) {
                    if (column > 1) {
                        row.append(',');
                    }
                    row.append(csv(result.getString(column)));
                }
                writeLine(zip, row.toString());
            }
        }
        zip.closeEntry();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static void writeLine(ZipOutputStream zip, String value) throws IOException {
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.write('\n');
    }

    private static void writeBytes(ZipOutputStream zip, String name, byte[] bytes)
        throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    private static long scalarLong(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            if (!result.next()) {
                throw new SQLException("Count query returned no rows");
            }
            return result.getLong(1);
        }
    }

    private static String scalarText(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getString(1) : null;
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[16_384];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().withUpperCase().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private record ExportCounts(
        long profiles,
        long experienceRows,
        long perkRows,
        long newGamePlusRows,
        long auditRows
    ) {
    }
}
