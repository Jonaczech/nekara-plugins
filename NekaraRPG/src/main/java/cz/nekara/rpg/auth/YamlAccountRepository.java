package cz.nekara.rpg.auth;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlAccountRepository implements AccountRepository {
    private static final int SCHEMA_VERSION = 1;

    private final File file;
    private final Map<String, AuthAccount> accounts = new HashMap<>();

    public YamlAccountRepository(File file) throws IOException {
        this.file = file;
        load();
    }

    @Override
    public synchronized Optional<AuthAccount> findByUsername(String username) {
        return Optional.ofNullable(accounts.get(AuthAccount.normalize(username)));
    }

    @Override
    public synchronized boolean create(AuthAccount account) throws IOException {
        if (accounts.containsKey(account.normalizedUsername())) {
            return false;
        }
        accounts.put(account.normalizedUsername(), account);
        try {
            save();
            return true;
        } catch (IOException exception) {
            accounts.remove(account.normalizedUsername());
            throw exception;
        }
    }

    @Override
    public synchronized void update(AuthAccount account) throws IOException {
        AuthAccount previous = accounts.put(account.normalizedUsername(), account);
        try {
            save();
        } catch (IOException exception) {
            if (previous == null) {
                accounts.remove(account.normalizedUsername());
            } else {
                accounts.put(account.normalizedUsername(), previous);
            }
            throw exception;
        }
    }

    @Override
    public synchronized boolean delete(String username) throws IOException {
        String key = AuthAccount.normalize(username);
        AuthAccount removed = accounts.remove(key);
        if (removed == null) {
            return false;
        }
        try {
            save();
            return true;
        } catch (IOException exception) {
            accounts.put(key, removed);
            throw exception;
        }
    }

    @Override
    public synchronized int count() {
        return accounts.size();
    }

    private void load() throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        if (!file.exists()) {
            save();
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (InvalidConfigurationException exception) {
            throw new IOException("Invalid NekaraAuth account storage.", exception);
        }
        if (yaml.getInt("schema-version", -1) != SCHEMA_VERSION) {
            throw new IOException("Unsupported or missing NekaraAuth storage schema version.");
        }
        ConfigurationSection section = yaml.getConfigurationSection("accounts");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String path = "accounts." + key;
            try {
                String username = required(yaml.getString(path + ".username"), "username");
                String normalized = AuthAccount.normalize(username);
                UUID uuid = UUID.fromString(required(yaml.getString(path + ".last-known-uuid"), "last-known-uuid"));
                String passwordHash = required(yaml.getString(path + ".password-hash"), "password-hash");
                Instant createdAt = Instant.parse(required(yaml.getString(path + ".created-at"), "created-at"));
                String lastLoginValue = yaml.getString(path + ".last-login-at");
                Instant lastLoginAt = lastLoginValue == null ? null : Instant.parse(lastLoginValue);
                if (!key.equals(normalized)) {
                    throw new IllegalArgumentException("Account key is not normalized.");
                }
                AuthAccount previous = accounts.put(normalized, new AuthAccount(
                        username, normalized, uuid, passwordHash, createdAt, lastLoginAt));
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate normalized account.");
                }
            } catch (IllegalArgumentException | DateTimeParseException exception) {
                throw new IOException("Invalid NekaraAuth account '" + key + "'.", exception);
            }
        }
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + field + ".");
        }
        return value;
    }

    private void save() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", SCHEMA_VERSION);
        for (AuthAccount account : accounts.values()) {
            String path = "accounts." + account.normalizedUsername();
            yaml.set(path + ".username", account.username());
            yaml.set(path + ".last-known-uuid", account.lastKnownUuid().toString());
            yaml.set(path + ".password-hash", account.passwordHash());
            yaml.set(path + ".created-at", account.createdAt().toString());
            yaml.set(path + ".last-login-at",
                    account.lastLoginAt() == null ? null : account.lastLoginAt().toString());
        }

        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        yaml.save(temporary);
        try {
            Files.move(temporary.toPath(), file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
