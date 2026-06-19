package powie.sixbees.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import powie.sixbees.utils.BaseUtils.Base;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static powie.sixbees.SixBees.LOG;

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
        getMaps();
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

    private static void getMaps() {
        try {
            HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                    .uri(URI.create("https://powie69.github.io/6bees-data/0.1.0/maps.json"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

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
            Files.writeString(MAPS_FILE, data);
            LOG.info("Maps config updated");
        } catch (JsonParseException e) {
            LOG.error("Invalid JSON in maps config", e);
        } catch (IOException e) {
            // Covers: no internet, DNS fail, connection refused, file write issues, etc.
            LOG.warn("Failed to fetch maps config (no internet or IO issue)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Maps request interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

