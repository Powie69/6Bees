package powie.sixbees.utils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static powie.sixbees.SixBees.LOG;

public class Config {
    private static final Path CONFIG_FOLDER = FabricLoader.getInstance().getGameDir().resolve("6bees");

    private static final Set<String> CONFIG_FILES = Set.of(
        "bases",
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

    // Maps

    public static Set<Integer> readMaps() {
        Path file = CONFIG_FOLDER.resolve("maps");
        if (!Files.exists(file)) return new HashSet<>();

        try {
            String content = Files.readString(file).trim();
            if (content.isEmpty()) return new HashSet<>();

            Type setType = new TypeToken<HashSet<Integer>>() {
            }.getType();
            Set<Integer> result = new Gson().fromJson(content, setType);
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
        Path file = CONFIG_FOLDER.resolve("bases");
        if (!Files.exists(file)) return new HashMap<>();

        try {
            String content = Files.readString(file).trim();
            if (content.isEmpty()) return new HashMap<>();

            Type type = new TypeToken<HashMap<String, Base>>() {}.getType();
            Map<String, Base> result = new Gson().fromJson(content, type);

            return result != null ? result : new HashMap<>();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to read bases config", e);
        }
        catch (JsonParseException e) {
            LOG.error("Failed to parse bases config", e);
            return new HashMap<>();
        }
    }

    private static void writeBases(Map<String, Base> bases) {
        Path file = CONFIG_FOLDER.resolve("bases");
        if (!Files.exists(file)) throw new RuntimeException("Places file does not exist");

        try {
            Gson gson = new Gson();

            String json = gson.toJson(bases);

            Files.writeString(file, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save bases config", e);
        }
    }

    public static void saveBase(String key, Base base) {
        Map<String, Base> bases = readBases();
        bases.put(key, base);
        writeBases(bases);
    }

    public static void removeBase(String key) {
        Map<String, Base> bases = readBases();
        bases.remove(key);
        writeBases(bases);
    }

    public static class Base {
        public String name;
        public BlockPos coords;
        public int radius;
        public Dimension dimension;

        public Base(String name, BlockPos Coords, int radius, Dimension dimension) {
            this.name = name;
            this.coords = Coords;
            this.radius = radius;
            this.dimension = dimension;
        }
    }

    // todo: Get data from the internet
}

