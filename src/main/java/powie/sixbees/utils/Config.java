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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static powie.sixbees.SixBees.LOG;

public class Config {
    private static final Path CONFIG_FOLDER = FabricLoader.getInstance().getGameDir().resolve("6bees");

    private static final Set<String> CONFIG_FILES = Set.of(
        "places",
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

    // Places

    public static List<Place> readPlaces() {
        Path file = CONFIG_FOLDER.resolve("places");
        if (!Files.exists(file)) return new ArrayList<>();

        try {
            String content = Files.readString(file).trim();
            if (content.isEmpty()) return new ArrayList<>();

            Gson gson = new Gson();

            Type listType = new TypeToken<ArrayList<Place>>() {
            }.getType();

            List<Place> result = gson.fromJson(content, listType);

            return result != null ? result : new ArrayList<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read places config", e);
        } catch (JsonParseException e) {
            LOG.error("Failed to parse places config", e);
            return new ArrayList<>();
        }
    }

    public static void savePlaces(List<Place> places) {
        Path file = CONFIG_FOLDER.resolve("places");
        if (!Files.exists(file)) throw new RuntimeException("Places file does not exist");

        try {
            Gson gson = new Gson();

            String json = gson.toJson(places);

            Files.writeString(file, json);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to save places config", e);
        }
    }

    public static void savePlace(Place place) {
        List<Place> places = readPlaces();
        places.removeIf(p -> p.name.equalsIgnoreCase(place.name));
        places.add(place);
        savePlaces(places);
    }

    public static void removePlace(String name) {
        List<Place> places = readPlaces();
        places.removeIf(place -> place.name.equalsIgnoreCase(name));
        savePlaces(places);
    }

    public static class Place {
        public String name;
        public BlockPos coords;
        public double radius;
        public Dimension dimension;

        public Place(String name, BlockPos Coords, double radius, Dimension dimension) {
            this.name = name;
            this.coords = Coords;
            this.radius = radius;
            this.dimension = dimension;
        }
    }

    // todo: Get data from the internet
}

