package cz.nekara.rpg.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

public final class GitHubReleaseParser {
    public static final String ASSET_NAME = "NekaraRPG.jar";
    private static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final String DOWNLOAD_PATH_PREFIX =
            "/Jonaczech/nekara-plugins/releases/download/";

    public GitHubRelease parse(String json) {
        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) {
            throw new IllegalArgumentException("GitHub release response is not an object");
        }
        JsonObject root = rootElement.getAsJsonObject();
        if (booleanValue(root, "draft") || booleanValue(root, "prerelease")) {
            throw new IllegalArgumentException("GitHub release is not stable");
        }
        SemanticVersion version = SemanticVersion.parseStable(requiredString(root, "tag_name"));
        JsonArray assets = requiredArray(root, "assets");

        JsonObject matchingAsset = null;
        for (JsonElement element : assets) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject asset = element.getAsJsonObject();
            if (!ASSET_NAME.equals(optionalString(asset, "name"))) {
                continue;
            }
            if (matchingAsset != null) {
                throw new IllegalArgumentException("GitHub release contains duplicate " + ASSET_NAME + " assets");
            }
            matchingAsset = asset;
        }
        if (matchingAsset == null) {
            throw new IllegalArgumentException("GitHub release does not contain " + ASSET_NAME);
        }

        long size = requiredLong(matchingAsset, "size");
        if (size <= 0L) {
            throw new IllegalArgumentException("GitHub release asset has an invalid size");
        }
        String digest = requiredString(matchingAsset, "digest").toLowerCase(Locale.ROOT);
        if (!digest.startsWith("sha256:")) {
            throw new IllegalArgumentException("GitHub release asset does not provide a SHA-256 digest");
        }
        String sha256 = digest.substring("sha256:".length());
        if (!SHA_256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("GitHub release asset has an invalid SHA-256 digest");
        }

        URI downloadUri = URI.create(requiredString(matchingAsset, "browser_download_url"));
        if (!"https".equalsIgnoreCase(downloadUri.getScheme())
                || !"github.com".equalsIgnoreCase(downloadUri.getHost())
                || downloadUri.getPath() == null
                || !downloadUri.getPath().startsWith(DOWNLOAD_PATH_PREFIX)
                || !downloadUri.getPath().endsWith("/" + ASSET_NAME)) {
            throw new IllegalArgumentException("GitHub release asset has an untrusted download URL");
        }
        return new GitHubRelease(version, downloadUri, size, sha256);
    }

    private boolean booleanValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    private String requiredString(JsonObject object, String key) {
        String value = optionalString(object, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GitHub release is missing " + key);
        }
        return value;
    }

    private String optionalString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private JsonArray requiredArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalArgumentException("GitHub release is missing " + key);
        }
        return value.getAsJsonArray();
    }

    private long requiredLong(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            throw new IllegalArgumentException("GitHub release is missing " + key);
        }
        try {
            return value.getAsLong();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("GitHub release has an invalid " + key, exception);
        }
    }
}
