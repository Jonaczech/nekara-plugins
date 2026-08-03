package cz.nekara.rpg.mount;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SqliteMountRepository implements MountRepository, AutoCloseable {
    private static final int SCHEMA_VERSION = 1;

    private final Connection connection;
    private final MountRecordYamlCodec codec = new MountRecordYamlCodec();

    public SqliteMountRepository(File file) throws IOException {
        try {
            File parent = file.getParentFile();
            if (parent != null) Files.createDirectories(parent.toPath());
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA synchronous=NORMAL");
                statement.execute("PRAGMA busy_timeout=250");
                statement.execute("CREATE TABLE IF NOT EXISTS metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
                statement.execute("CREATE TABLE IF NOT EXISTS mounts (owner_id TEXT PRIMARY KEY, mount_id TEXT NOT NULL UNIQUE, payload TEXT NOT NULL)");
                statement.execute("CREATE TABLE IF NOT EXISTS combat (owner_id TEXT PRIMARY KEY, until_at TEXT NOT NULL)");
            }
            validateSchema();
        } catch (SQLException | ClassNotFoundException exception) {
            throw new IOException("Could not open NekaraMounts SQLite storage.", exception);
        }
    }

    private void validateSchema() throws SQLException, IOException {
        String version = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT value FROM metadata WHERE key='schema-version'");
             ResultSet result = statement.executeQuery()) {
            if (result.next()) version = result.getString(1);
        }
        if (version == null) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO metadata(key,value) VALUES('schema-version',?)")) {
                statement.setString(1, Integer.toString(SCHEMA_VERSION));
                statement.executeUpdate();
            }
        } else if (!Integer.toString(SCHEMA_VERSION).equals(version)) {
            throw new IOException("Unsupported NekaraMounts SQLite schema version " + version + ".");
        }
    }

    public synchronized boolean isEmpty() throws IOException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT 1 FROM mounts LIMIT 1")) {
            return !result.next();
        } catch (SQLException exception) {
            throw io("Could not inspect mount storage.", exception);
        }
    }

    public synchronized boolean isLegacyMigrationComplete() throws IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM metadata WHERE key='legacy-yaml-imported' AND value='true'");
             ResultSet result = statement.executeQuery()) {
            return result.next();
        } catch (SQLException exception) {
            throw io("Could not inspect mount migration state.", exception);
        }
    }

    public synchronized void markLegacyMigrationComplete() throws IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO metadata(key,value) VALUES('legacy-yaml-imported','true') "
                        + "ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw io("Could not record mount migration state.", exception);
        }
    }

    public synchronized void importAll(Collection<MountRecord> mounts, Map<String, Instant> combat) throws IOException {
        transaction(() -> {
            try (PreparedStatement insertMount = connection.prepareStatement(
                    "INSERT INTO mounts(owner_id,mount_id,payload) VALUES(?,?,?)");
                 PreparedStatement insertCombat = connection.prepareStatement(
                         "INSERT INTO combat(owner_id,until_at) VALUES(?,?)")) {
                for (MountRecord mount : mounts) {
                    bindMount(insertMount, mount);
                    insertMount.addBatch();
                }
                insertMount.executeBatch();
                for (Map.Entry<String, Instant> entry : combat.entrySet()) {
                    insertCombat.setString(1, entry.getKey());
                    insertCombat.setString(2, entry.getValue().toString());
                    insertCombat.addBatch();
                }
                insertCombat.executeBatch();
                try (PreparedStatement migrated = connection.prepareStatement(
                        "INSERT INTO metadata(key,value) VALUES('legacy-yaml-imported','true') "
                                + "ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
                    migrated.executeUpdate();
                }
            }
        });
    }

    @Override public synchronized Optional<MountRecord> findByOwnerId(String ownerId) {
        return find("SELECT payload FROM mounts WHERE owner_id=?", ownerId);
    }
    @Override public synchronized Optional<MountRecord> findByMountId(UUID mountId) {
        return find("SELECT payload FROM mounts WHERE mount_id=?", mountId.toString());
    }

    private Optional<MountRecord> find(String sql, String value) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(codec.decode(result.getString(1))) : Optional.empty();
            }
        } catch (SQLException | RuntimeException exception) {
            throw new IllegalStateException("Could not read NekaraMounts storage.", exception);
        }
    }

    @Override public synchronized Collection<MountRecord> findAll() {
        List<MountRecord> mounts = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT payload FROM mounts")) {
            while (result.next()) mounts.add(codec.decode(result.getString(1)));
            return List.copyOf(mounts);
        } catch (SQLException | RuntimeException exception) {
            throw new IllegalStateException("Could not read NekaraMounts storage.", exception);
        }
    }

    @Override public synchronized boolean create(MountRecord mount) throws IOException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT OR IGNORE INTO mounts(owner_id,mount_id,payload) VALUES(?,?,?)")) {
            bindMount(statement, mount);
            return statement.executeUpdate() == 1;
        } catch (SQLException | RuntimeException exception) {
            throw io("Could not create mount record.", exception);
        }
    }

    @Override public synchronized void update(MountRecord mount) throws IOException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE mounts SET mount_id=?, payload=? WHERE owner_id=? AND mount_id=?")) {
            statement.setString(1, mount.mountId().toString());
            statement.setString(2, codec.encode(mount));
            statement.setString(3, mount.ownerId());
            statement.setString(4, mount.mountId().toString());
            if (statement.executeUpdate() != 1) throw new IOException("Mount ownership does not match the persisted record.");
        } catch (SQLException | RuntimeException exception) {
            throw io("Could not update mount record.", exception);
        }
    }

    @Override public synchronized Optional<Instant> combatUntil(String ownerId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT until_at FROM combat WHERE owner_id=?")) {
            statement.setString(1, ownerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(Instant.parse(result.getString(1))) : Optional.empty();
            }
        } catch (SQLException | RuntimeException exception) {
            throw new IllegalStateException("Could not read mount combat window.", exception);
        }
    }

    @Override public synchronized Map<String, Instant> combatWindows() {
        Map<String, Instant> values = new HashMap<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT owner_id,until_at FROM combat")) {
            while (result.next()) values.put(result.getString(1), Instant.parse(result.getString(2)));
            return Map.copyOf(values);
        } catch (SQLException | RuntimeException exception) {
            throw new IllegalStateException("Could not read mount combat windows.", exception);
        }
    }

    @Override public synchronized void setCombatUntil(Map<String, Instant> windows) throws IOException {
        transaction(() -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO combat(owner_id,until_at) VALUES(?,?) ON CONFLICT(owner_id) DO UPDATE SET until_at=excluded.until_at")) {
                for (Map.Entry<String, Instant> entry : windows.entrySet()) {
                    statement.setString(1, entry.getKey());
                    statement.setString(2, entry.getValue().toString());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    private void bindMount(PreparedStatement statement, MountRecord mount) throws SQLException {
        statement.setString(1, mount.ownerId());
        statement.setString(2, mount.mountId().toString());
        statement.setString(3, codec.encode(mount));
    }

    private void transaction(SqlWork work) throws IOException {
        try {
            connection.setAutoCommit(false);
            work.run();
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            try { connection.rollback(); } catch (SQLException rollback) { exception.addSuppressed(rollback); }
            throw io("Could not commit NekaraMounts transaction.", exception);
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException exception) { throw io("Could not restore SQLite transaction mode.", exception); }
        }
    }

    @Override public synchronized void close() throws IOException {
        try { connection.close(); } catch (SQLException exception) { throw io("Could not close NekaraMounts storage.", exception); }
    }

    private IOException io(String message, Exception cause) {
        return cause instanceof IOException value ? value : new IOException(message, cause);
    }

    @FunctionalInterface private interface SqlWork { void run() throws SQLException; }
}
