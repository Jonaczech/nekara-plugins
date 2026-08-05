package cz.nekara.rpg.skills.profile;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.admin.SkillAdminActor;
import cz.nekara.rpg.skills.admin.SkillAdministrationRepository;
import cz.nekara.rpg.skills.admin.SkillAuditEntry;
import cz.nekara.rpg.skills.admin.SkillAuditRecord;
import cz.nekara.rpg.skills.export.SkillSnapshotRepository;
import cz.nekara.rpg.skills.perks.PerkId;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SqliteSkillProfileRepository
    implements SkillAdministrationRepository, SkillSnapshotRepository {
    private static final int SCHEMA_VERSION = 5;

    private final Connection connection;

    public SqliteSkillProfileRepository(File file) throws IOException {
        Connection opened = null;
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            Class.forName("org.sqlite.JDBC");
            opened = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement statement = opened.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA synchronous=NORMAL");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("PRAGMA busy_timeout=500");
                statement.execute("CREATE TABLE IF NOT EXISTS metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            }
            initializeSchema(opened);
            connection = opened;
        } catch (SQLException | ClassNotFoundException | IOException exception) {
            if (opened != null) {
                try {
                    opened.close();
                } catch (SQLException closeFailure) {
                    exception.addSuppressed(closeFailure);
                }
            }
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Could not open Nekara Skills SQLite storage", exception);
        }
    }

    @Override
    public synchronized Optional<SkillProfile> find(String playerKey) {
        requirePlayerKey(playerKey);
        try (PreparedStatement profileStatement = connection.prepareStatement(
            "SELECT spent_perk_points, admin_bonus_perk_points, revision FROM profiles WHERE player_key=?")) {
            profileStatement.setString(1, playerKey);
            try (ResultSet profileResult = profileStatement.executeQuery()) {
                if (!profileResult.next()) {
                    return Optional.empty();
                }
                int spentPerkPoints = profileResult.getInt("spent_perk_points");
                int adminBonusPerkPoints = profileResult.getInt("admin_bonus_perk_points");
                long revision = profileResult.getLong("revision");
                return Optional.of(new SkillProfile(
                    playerKey,
                    readExperience(playerKey),
                    readNewGamePlusRanks(playerKey),
                    readPerks(playerKey),
                    spentPerkPoints,
                    adminBonusPerkPoints,
                    revision
                ));
            }
        } catch (SQLException | RuntimeException exception) {
            throw storage("Could not read Nekara Skills profile", exception);
        }
    }

    @Override
    public synchronized SkillProfile save(SkillProfile profile, long expectedRevision) {
        return saveInternal(profile, expectedRevision, null);
    }

    @Override
    public synchronized SkillProfile saveAdminMutation(
        SkillProfile profile,
        long expectedRevision,
        SkillAuditRecord auditRecord
    ) {
        if (auditRecord == null) {
            throw new NullPointerException("auditRecord");
        }
        return saveInternal(profile, expectedRevision, auditRecord);
    }

    @Override
    public synchronized List<SkillAuditEntry> findRecentAuditEntries(String playerKey, int limit) {
        requirePlayerKey(playerKey);
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Audit entry limit must be between 1 and 100");
        }
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT id,actor_key,actor_name,target_player_key,target_name,operation,detail,"
                + "occurred_at_epoch_millis,revision_before,revision_after "
                + "FROM admin_audit WHERE target_player_key=? ORDER BY id DESC LIMIT ?")) {
            statement.setString(1, playerKey);
            statement.setInt(2, limit);
            try (ResultSet result = statement.executeQuery()) {
                java.util.ArrayList<SkillAuditEntry> entries = new java.util.ArrayList<>();
                while (result.next()) {
                    entries.add(new SkillAuditEntry(
                        result.getLong("id"),
                        new SkillAdminActor(
                            result.getString("actor_key"), result.getString("actor_name")),
                        result.getString("target_player_key"),
                        result.getString("target_name"),
                        result.getString("operation"),
                        result.getString("detail"),
                        result.getLong("occurred_at_epoch_millis"),
                        result.getLong("revision_before"),
                        result.getLong("revision_after")
                    ));
                }
                return List.copyOf(entries);
            }
        } catch (SQLException | RuntimeException exception) {
            throw storage("Could not read Nekara Skills administrative audit", exception);
        }
    }

    private SkillProfile saveInternal(
        SkillProfile profile,
        long expectedRevision,
        SkillAuditRecord auditRecord
    ) {
        if (profile == null) {
            throw new NullPointerException("profile");
        }
        if (expectedRevision < 0 || profile.revision() != expectedRevision) {
            throw new IllegalArgumentException("Profile revision must match the expected revision");
        }

        try {
            connection.setAutoCommit(false);
            long actualRevision = currentRevision(profile.playerKey());
            if (actualRevision != expectedRevision) {
                throw new ConcurrentProfileUpdateException(
                    profile.playerKey(), expectedRevision, actualRevision);
            }
            long nextRevision = Math.addExact(expectedRevision, 1);
            upsertProfile(profile, expectedRevision, nextRevision);
            replaceExperience(profile);
            replaceNewGamePlusRanks(profile);
            replacePerks(profile);
            if (auditRecord != null) {
                insertAudit(profile.playerKey(), expectedRevision, nextRevision, auditRecord);
            }
            connection.commit();
            return profile.withRevision(nextRevision);
        } catch (SQLException | RuntimeException exception) {
            rollback(exception);
            if (exception instanceof ConcurrentProfileUpdateException concurrent) {
                throw concurrent;
            }
            throw storage("Could not save Nekara Skills profile", exception);
        } finally {
            restoreAutoCommit();
        }
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException exception) {
            throw storage("Could not close Nekara Skills storage", exception);
        }
    }

    @Override
    public synchronized void createConsistentSnapshot(Path target) throws IOException {
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        if (parent == null) {
            throw new IOException("Nekara Skills snapshot requires a parent directory");
        }
        Files.createDirectories(parent);
        if (Files.exists(absoluteTarget)) {
            throw new IOException("Nekara Skills snapshot target already exists");
        }
        String escapedPath = absoluteTarget.toString().replace("'", "''");
        try (Statement statement = connection.createStatement()) {
            statement.execute("VACUUM INTO '" + escapedPath + "'");
        } catch (SQLException | RuntimeException exception) {
            try {
                Files.deleteIfExists(absoluteTarget);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw new IOException("Could not create a consistent Nekara Skills snapshot", exception);
        }
    }

    private static void initializeSchema(Connection connection) throws SQLException, IOException {
        String version = readSchemaVersion(connection);
        if (version == null) {
            updateSchema(connection, null);
            return;
        }
        if ("1".equals(version) || "2".equals(version) || "3".equals(version) || "4".equals(version)) {
            updateSchema(connection, version);
            return;
        }
        if (!Integer.toString(SCHEMA_VERSION).equals(version)) {
            throw new IOException("Unsupported Nekara Skills SQLite schema version " + version);
        }
        createCurrentTables(connection);
    }

    private static String readSchemaVersion(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT value FROM metadata WHERE key='schema-version'");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                return result.getString(1);
            }
        }
        return null;
    }

    private static void updateSchema(Connection connection, String previousVersion)
        throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            if ("1".equals(previousVersion) || "2".equals(previousVersion)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE profiles ADD COLUMN admin_bonus_perk_points INTEGER NOT NULL DEFAULT 0");
                }
            }
            createCurrentTables(connection);
            if (previousVersion != null) {
                migrateRenamedSkillIds(connection);
            }
            if (previousVersion == null) {
                try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO metadata(key,value) VALUES('schema-version',?)")) {
                    statement.setString(1, Integer.toString(SCHEMA_VERSION));
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE metadata SET value=? WHERE key='schema-version' AND value=?")) {
                    statement.setString(1, Integer.toString(SCHEMA_VERSION));
                    statement.setString(2, previousVersion);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Nekara Skills schema version changed during migration");
                    }
                }
            }
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollback) {
                exception.addSuppressed(rollback);
            }
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static void migrateRenamedSkillIds(Connection connection) throws SQLException {
        for (Map.Entry<String, String> entry : SkillId.renamedIds().entrySet()) {
            ensureNoSkillIdCollision(connection, entry.getKey(), entry.getValue());
            renameSkillId(connection, "skill_experience", entry.getKey(), entry.getValue());
            renameSkillId(connection, "skill_new_game_plus", entry.getKey(), entry.getValue());
            try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE perk_ranks SET perk_id=? || substr(perk_id,?) WHERE perk_id LIKE ?")) {
                statement.setString(1, entry.getValue());
                statement.setInt(2, entry.getKey().length() + 1);
                statement.setString(3, entry.getKey() + ".%");
                statement.executeUpdate();
            }
        }
    }

    private static void ensureNoSkillIdCollision(Connection connection, String oldId, String newId) throws SQLException {
        for (String table : List.of("skill_experience", "skill_new_game_plus")) {
            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM " + table + " AS legacy JOIN " + table
                    + " AS current ON legacy.player_key=current.player_key WHERE legacy.skill_id=? AND current.skill_id=? LIMIT 1")) {
                statement.setString(1, oldId);
                statement.setString(2, newId);
                if (statement.executeQuery().next()) {
                    throw new SQLException("Cannot migrate skill ID " + oldId + "; " + newId + " already exists");
                }
            }
        }
    }

    private static void renameSkillId(Connection connection, String table, String oldId, String newId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE " + table + " SET skill_id=? WHERE skill_id=?")) {
            statement.setString(1, newId);
            statement.setString(2, oldId);
            statement.executeUpdate();
        }
    }

    private static void createCurrentTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS profiles ("
                + "player_key TEXT PRIMARY KEY, spent_perk_points INTEGER NOT NULL, "
                + "admin_bonus_perk_points INTEGER NOT NULL DEFAULT 0, revision INTEGER NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS skill_experience ("
                + "player_key TEXT NOT NULL, skill_id TEXT NOT NULL, total_experience INTEGER NOT NULL, "
                + "PRIMARY KEY(player_key, skill_id), "
                + "FOREIGN KEY(player_key) REFERENCES profiles(player_key) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS skill_new_game_plus ("
                + "player_key TEXT NOT NULL, skill_id TEXT NOT NULL, rank INTEGER NOT NULL, "
                + "PRIMARY KEY(player_key, skill_id), "
                + "FOREIGN KEY(player_key) REFERENCES profiles(player_key) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS perk_ranks ("
                + "player_key TEXT NOT NULL, perk_id TEXT NOT NULL, rank INTEGER NOT NULL, "
                + "PRIMARY KEY(player_key, perk_id), "
                + "FOREIGN KEY(player_key) REFERENCES profiles(player_key) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS admin_audit ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, actor_key TEXT NOT NULL, actor_name TEXT NOT NULL, "
                + "target_player_key TEXT NOT NULL, target_name TEXT NOT NULL, operation TEXT NOT NULL, "
                + "detail TEXT NOT NULL, occurred_at_epoch_millis INTEGER NOT NULL, "
                + "revision_before INTEGER NOT NULL, revision_after INTEGER NOT NULL, "
                + "CHECK(revision_before >= 0), CHECK(revision_after = revision_before + 1))");
            statement.execute("CREATE INDEX IF NOT EXISTS admin_audit_target_time "
                + "ON admin_audit(target_player_key, occurred_at_epoch_millis DESC)");
        }
    }

    private EnumMap<SkillId, Long> readExperience(String playerKey) throws SQLException {
        EnumMap<SkillId, Long> experience = new EnumMap<>(SkillId.class);
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT skill_id, total_experience FROM skill_experience WHERE player_key=?")) {
            statement.setString(1, playerKey);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    SkillId skill = skillById(result.getString("skill_id"));
                    long total = result.getLong("total_experience");
                    if (total < 0) {
                        throw new SkillStorageException("Stored skill experience cannot be negative");
                    }
                    experience.put(skill, total);
                }
            }
        }
        return experience;
    }

    private EnumMap<SkillId, Integer> readNewGamePlusRanks(String playerKey) throws SQLException {
        EnumMap<SkillId, Integer> ranks = new EnumMap<>(SkillId.class);
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT skill_id,rank FROM skill_new_game_plus WHERE player_key=?")) {
            statement.setString(1, playerKey);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    int rank = result.getInt("rank");
                    if (rank < 1) throw new SkillStorageException("Invalid stored New Game+ rank");
                    ranks.put(skillById(result.getString("skill_id")), rank);
                }
            }
        }
        return ranks;
    }

    private Map<PerkId, Integer> readPerks(String playerKey) throws SQLException {
        Map<PerkId, Integer> perks = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT perk_id, rank FROM perk_ranks WHERE player_key=?")) {
            statement.setString(1, playerKey);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    perks.put(new PerkId(result.getString("perk_id")), result.getInt("rank"));
                }
            }
        }
        return perks;
    }

    private long currentRevision(String playerKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT revision FROM profiles WHERE player_key=?")) {
            statement.setString(1, playerKey);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0;
            }
        }
    }

    private void upsertProfile(SkillProfile profile, long expectedRevision, long nextRevision)
        throws SQLException {
        if (expectedRevision == 0) {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO profiles(player_key,spent_perk_points,admin_bonus_perk_points,revision) VALUES(?,?,?,?)")) {
                statement.setString(1, profile.playerKey());
                statement.setInt(2, profile.spentPerkPoints());
                statement.setInt(3, profile.adminBonusPerkPoints());
                statement.setLong(4, nextRevision);
                statement.executeUpdate();
            }
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE profiles SET spent_perk_points=?, admin_bonus_perk_points=?, revision=? WHERE player_key=? AND revision=?")) {
            statement.setInt(1, profile.spentPerkPoints());
            statement.setInt(2, profile.adminBonusPerkPoints());
            statement.setLong(3, nextRevision);
            statement.setString(4, profile.playerKey());
            statement.setLong(5, expectedRevision);
            if (statement.executeUpdate() != 1) {
                throw new ConcurrentProfileUpdateException(
                    profile.playerKey(), expectedRevision, currentRevision(profile.playerKey()));
            }
        }
    }

    private void replaceExperience(SkillProfile profile) throws SQLException {
        deleteRows("skill_experience", profile.playerKey());
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO skill_experience(player_key,skill_id,total_experience) VALUES(?,?,?)")) {
            for (Map.Entry<SkillId, Long> entry : profile.totalExperience().entrySet()) {
                statement.setString(1, profile.playerKey());
                statement.setString(2, entry.getKey().id());
                statement.setLong(3, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void replacePerks(SkillProfile profile) throws SQLException {
        deleteRows("perk_ranks", profile.playerKey());
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO perk_ranks(player_key,perk_id,rank) VALUES(?,?,?)")) {
            for (Map.Entry<PerkId, Integer> entry : profile.perkRanks().entrySet()) {
                statement.setString(1, profile.playerKey());
                statement.setString(2, entry.getKey().value());
                statement.setInt(3, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void replaceNewGamePlusRanks(SkillProfile profile) throws SQLException {
        deleteRows("skill_new_game_plus", profile.playerKey());
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO skill_new_game_plus(player_key,skill_id,rank) VALUES(?,?,?)")) {
            for (Map.Entry<SkillId, Integer> entry : profile.newGamePlusRanks().entrySet()) {
                statement.setString(1, profile.playerKey());
                statement.setString(2, entry.getKey().id());
                statement.setInt(3, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertAudit(
        String targetPlayerKey,
        long revisionBefore,
        long revisionAfter,
        SkillAuditRecord auditRecord
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO admin_audit(actor_key,actor_name,target_player_key,target_name,operation,"
                + "detail,occurred_at_epoch_millis,revision_before,revision_after) VALUES(?,?,?,?,?,?,?,?,?)")) {
            statement.setString(1, auditRecord.actor().key());
            statement.setString(2, auditRecord.actor().displayName());
            statement.setString(3, targetPlayerKey);
            statement.setString(4, auditRecord.targetDisplayName());
            statement.setString(5, auditRecord.operation());
            statement.setString(6, auditRecord.detail());
            statement.setLong(7, auditRecord.occurredAtEpochMillis());
            statement.setLong(8, revisionBefore);
            statement.setLong(9, revisionAfter);
            statement.executeUpdate();
        }
    }

    private void deleteRows(String table, String playerKey) throws SQLException {
        String sql = switch (table) {
            case "skill_experience" -> "DELETE FROM skill_experience WHERE player_key=?";
            case "skill_new_game_plus" -> "DELETE FROM skill_new_game_plus WHERE player_key=?";
            case "perk_ranks" -> "DELETE FROM perk_ranks WHERE player_key=?";
            default -> throw new IllegalArgumentException("Unsupported profile table");
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerKey);
            statement.executeUpdate();
        }
    }

    private static SkillId skillById(String id) {
        for (SkillId skill : SkillId.gameplaySkills()) {
            if (skill.id().equals(id)) {
                return skill;
            }
        }
        throw new SkillStorageException("Unknown stored skill ID: " + id);
    }

    private void rollback(Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollback) {
            cause.addSuppressed(rollback);
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException exception) {
            throw storage("Could not restore Nekara Skills transaction mode", exception);
        }
    }

    private static void requirePlayerKey(String playerKey) {
        if (playerKey == null || playerKey.isBlank()) {
            throw new IllegalArgumentException("Player key cannot be blank");
        }
    }

    private static SkillStorageException storage(String message, Throwable cause) {
        return cause instanceof SkillStorageException skillStorage
            ? skillStorage
            : new SkillStorageException(message, cause);
    }
}
