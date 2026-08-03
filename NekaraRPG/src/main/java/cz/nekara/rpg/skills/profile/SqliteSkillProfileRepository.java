package cz.nekara.rpg.skills.profile;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.PerkId;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SqliteSkillProfileRepository implements SkillProfileRepository {
    private static final int SCHEMA_VERSION = 1;

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
            validateSchema(opened);
            try (Statement statement = opened.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS profiles ("
                    + "player_key TEXT PRIMARY KEY, spent_perk_points INTEGER NOT NULL, revision INTEGER NOT NULL)");
                statement.execute("CREATE TABLE IF NOT EXISTS skill_experience ("
                    + "player_key TEXT NOT NULL, skill_id TEXT NOT NULL, total_experience INTEGER NOT NULL, "
                    + "PRIMARY KEY(player_key, skill_id), "
                    + "FOREIGN KEY(player_key) REFERENCES profiles(player_key) ON DELETE CASCADE)");
                statement.execute("CREATE TABLE IF NOT EXISTS perk_ranks ("
                    + "player_key TEXT NOT NULL, perk_id TEXT NOT NULL, rank INTEGER NOT NULL, "
                    + "PRIMARY KEY(player_key, perk_id), "
                    + "FOREIGN KEY(player_key) REFERENCES profiles(player_key) ON DELETE CASCADE)");
            }
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
            "SELECT spent_perk_points, revision FROM profiles WHERE player_key=?")) {
            profileStatement.setString(1, playerKey);
            try (ResultSet profileResult = profileStatement.executeQuery()) {
                if (!profileResult.next()) {
                    return Optional.empty();
                }
                int spentPerkPoints = profileResult.getInt("spent_perk_points");
                long revision = profileResult.getLong("revision");
                return Optional.of(new SkillProfile(
                    playerKey,
                    readExperience(playerKey),
                    readPerks(playerKey),
                    spentPerkPoints,
                    revision
                ));
            }
        } catch (SQLException | RuntimeException exception) {
            throw storage("Could not read Nekara Skills profile", exception);
        }
    }

    @Override
    public synchronized SkillProfile save(SkillProfile profile, long expectedRevision) {
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
            replacePerks(profile);
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

    private static void validateSchema(Connection connection) throws SQLException, IOException {
        String version = null;
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT value FROM metadata WHERE key='schema-version'");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                version = result.getString(1);
            }
        }
        if (version == null) {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO metadata(key,value) VALUES('schema-version',?)")) {
                statement.setString(1, Integer.toString(SCHEMA_VERSION));
                statement.executeUpdate();
            }
        } else if (!Integer.toString(SCHEMA_VERSION).equals(version)) {
            throw new IOException("Unsupported Nekara Skills SQLite schema version " + version);
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
                "INSERT INTO profiles(player_key,spent_perk_points,revision) VALUES(?,?,?)")) {
                statement.setString(1, profile.playerKey());
                statement.setInt(2, profile.spentPerkPoints());
                statement.setLong(3, nextRevision);
                statement.executeUpdate();
            }
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE profiles SET spent_perk_points=?, revision=? WHERE player_key=? AND revision=?")) {
            statement.setInt(1, profile.spentPerkPoints());
            statement.setLong(2, nextRevision);
            statement.setString(3, profile.playerKey());
            statement.setLong(4, expectedRevision);
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

    private void deleteRows(String table, String playerKey) throws SQLException {
        String sql = switch (table) {
            case "skill_experience" -> "DELETE FROM skill_experience WHERE player_key=?";
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
