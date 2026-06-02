package powie.sixbees.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static powie.sixbees.SixBees.LOG;

public class Config {
    private static final Path CONFIG_FOLDER = FabricLoader.getInstance().getGameDir().resolve("6bees");

    private static final Set<String> CONFIG_FILES = Set.of(
        "coords",
        "maps"
    );

    public static void initializeConfig() {
        if (!CONFIG_FOLDER.toFile().exists()) {
            CONFIG_FOLDER.getParent().toFile().mkdirs();
            CONFIG_FOLDER.toFile().mkdir();
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

    public static Set<Integer> readMaps() {
        Path file = CONFIG_FOLDER.resolve("maps");
        if (!Files.exists(file)) return new HashSet<>();

        try {
            String content = Files.readString(file).trim();
            if (content.isEmpty()) return new HashSet<>();

            Gson gson = new Gson();
            Type setType = new TypeToken<HashSet<Integer>>() {
            }.getType();
            Set<Integer> result = gson.fromJson(content, setType);
            return result != null ? result : new HashSet<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read maps config", e);
        } catch (JsonParseException e) {
            LOG.error("Failed to parse maps config", e);
            return new HashSet<>();
        }
    }

    // todo: Get data from the internet
}

