package powie.sixbees.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import powie.sixbees.utils.BaseUtils.Base;

import javax.management.RuntimeErrorException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static powie.sixbees.SixBees.LOG;

public class Config {
    private static final Path CONFIG_FOLDER = FabricLoader.getInstance().getGameDir().resolve("6bees");
    private static final Gson GSON = new Gson();
    private static final Set<String> CONFIG_FILES = Set.of(
        "bases",
        "maps"
    );

    private static volatile Map<String, Base> cachedBases;

    public static void initializeConfig() {
        try {
            Files.createDirectories(CONFIG_FOLDER);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config folder", e);
        }
        for (String filename : CONFIG_FILES) {
            createIfAbsent(filename);
        }
    }

    private static void createIfAbsent(String filename) {
        Path file = CONFIG_FOLDER.resolve(filename);
        if (Files.exists(file)) return;
        try {
            Files.createFile(file);
            LOG.info("Created config file: {}", filename);
        } catch (FileAlreadyExistsException e) {
            LOG.info("Config file already exists: {}", filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config file: " + filename, e);
        }
    }

    // Maps

    public static Set<Integer> readMaps() {
        Path file = CONFIG_FOLDER.resolve("maps");
        if (!Files.exists(file)) return new HashSet<>();

        try {
            String content = Files.readString(file).trim();
            if (content.isEmpty()) return new HashSet<>();

            Type setType = new TypeToken<HashSet<Integer>>() {
            }.getType();
            Set<Integer> result = GSON.fromJson(content, setType);
            return result != null ? result : new HashSet<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read maps config", e);
        } catch (JsonParseException e) {
            LOG.error("Failed to parse maps config", e);
            return new HashSet<>();
        }
    }

    // Bases

    public static Map<String, Base> readBases() {
        if (cachedBases != null) return cachedBases;

        Path file = CONFIG_FOLDER.resolve("bases");
        if (!Files.exists(file)) return new HashMap<>();

        try {
            String content = Files.readString(file).trim();
            if (content.isEmpty()) return new HashMap<>();

            Type type = new TypeToken<HashMap<String, Base>>() {
            }.getType();
            Map<String, Base> result = GSON.fromJson(content, type);

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
        Path file = CONFIG_FOLDER.resolve("bases");
        if (!Files.exists(file)) throw new RuntimeException("Bases file does not exist");

        try {
            String json = GSON.toJson(bases);
            Files.writeString(file, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save bases config", e);
        } finally {
            cachedBases = null;
        }
    }

    // todo: Get data from the internet
}

