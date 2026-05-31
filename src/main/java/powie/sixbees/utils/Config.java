package powie.sixbees.utils;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

import static powie.sixbees.SixBees.LOG;

public class Config {
    public static final File CONFIG_FOLDER = FabricLoader.getInstance().getGameDir().resolve("6bees").toFile();

    // TODO: future-proof
    public static void initialize() {
        File coordsFile = new File(CONFIG_FOLDER, "coords");
        if (coordsFile.exists()) return;
        try {
            coordsFile.createNewFile();
        } catch (Exception e) {
            LOG.error("Failed to create config file", e);
        }
    }
}

