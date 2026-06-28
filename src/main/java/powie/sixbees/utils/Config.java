package powie.sixbees.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.loader.api.FabricLoader;
import powie.sixbees.events.NewMapsDataEvent;
import powie.sixbees.utils.BaseUtils.Base;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static powie.sixbees.SixBees.LOG;
import static powie.sixbees.utils.Checks.isDevEnvOrHasExtraArgs;

public class Config {
    private static final Gson GSON = new Gson();
    // @formatter:off
    private static final Type MAPS_TYPE = new TypeToken<HashSet<Integer>>() {}.getType();
    private static final Type BASES_TYPE = new TypeToken<HashMap<String, Base>>() {}.getType();
    // @formatter:on
    private static final Path CONFIG_FOLDER = FabricLoader.getInstance().getGameDir().resolve("6bees");
    private static final Path BASES_FILE = CONFIG_FOLDER.resolve("bases");
    private static final Path MAPS_FILE = CONFIG_FOLDER.resolve("maps");
    private static final Set<Path> CONFIG_FILES = Set.of(BASES_FILE, MAPS_FILE);

    private static volatile Map<String, Base> cachedBases;

    public static void initializeConfig() {
        try {
            Files.createDirectories(CONFIG_FOLDER);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config folder", e);
        }
        for (Path file : CONFIG_FILES) {
            createIfAbsent(file);
        }
        fetchMaps();
    }

    private static void createIfAbsent(Path file) {
        if (Files.exists(file)) return;
        try {
            Files.createFile(file);
            LOG.info("Created config file: {}", file.getFileName());
        } catch (FileAlreadyExistsException e) {
            LOG.info("Config file already exists: {}", file.getFileName());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file: " + file.getFileName(), e);
        }
    }

    // Maps

    public static Set<Integer> readMaps() {
        if (!Files.exists(MAPS_FILE)) return new HashSet<>();

        try {
            String content = Files.readString(MAPS_FILE).trim();
            if (content.isEmpty()) return new HashSet<>();

            Set<Integer> result = GSON.fromJson(content, MAPS_TYPE);
            return result != null ? result : new HashSet<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read maps config", e);
        } catch (JsonParseException e) {
            LOG.error("Failed to parse maps config", e);
            return new HashSet<>();
        }
    }

    private static void fetchMaps() {
        HttpClient.newHttpClient().sendAsync(
                HttpRequest.newBuilder()
                    .uri(URI.create("https://powie69.github.io/6bees-data/0.1.0/maps.json"))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
            .thenAccept(res -> {
                if (res.statusCode() >= 400) {
                    LOG.warn("Maps request failed: HTTP {}", res.statusCode());
                    return;
                }

                String data = res.body().trim();
                if (data.isEmpty()) {
                    LOG.error("Response body is empty");
                    return;
                }

                Set<Integer> result = GSON.fromJson(data, MAPS_TYPE);
                if (result == null || result.isEmpty()) return;

                try {
                    Files.writeString(MAPS_FILE, data);
                    MeteorClient.EVENT_BUS.post(new NewMapsDataEvent(result));
                    LOG.info("Maps config updated: {}", result);
                } catch (IOException e) {
                    LOG.warn("Failed to write maps file", e);
                }
            })
            .exceptionally(e -> {
                Throwable cause = e.getCause();

                switch (cause) {
                    case HttpTimeoutException _ -> LOG.warn("Maps request timed out");
                    case JsonParseException _ -> LOG.error("Invalid JSON in maps config", cause);
                    case IOException _ -> LOG.warn("Failed to fetch maps config (no internet or IO issue)", cause);
                    case null, default -> LOG.warn("Unexpected error fetching maps config", cause);
                }

                return null;
            });
    }

    // Bases

    public static Map<String, Base> readBases() {
        if (cachedBases != null) return cachedBases;

        if (!Files.exists(BASES_FILE)) return new HashMap<>();

        try {
            String content = Files.readString(BASES_FILE).trim();
            if (content.isEmpty()) return new HashMap<>();

            Map<String, Base> result = GSON.fromJson(content, BASES_TYPE);

            cachedBases = result;
            return result != null ? result : new HashMap<>();
        } catch (IOException e) {
            cachedBases = null;
            throw new RuntimeException("Failed to read bases config", e); // Minecraft pls don't catch my shit i wanna crash it
        } catch (JsonParseException e) {
            LOG.error("Failed to parse bases config", e);
            cachedBases = null;
            return new HashMap<>();
        }
    }

    public static void writeBases(Map<String, Base> bases) {
        try {
            String json = GSON.toJson(bases);
            Files.writeString(BASES_FILE, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save bases config", e);
        } finally {
            cachedBases = null;
        }
    }
}

