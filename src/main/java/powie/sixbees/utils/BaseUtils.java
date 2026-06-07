package powie.sixbees.utils;

import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.core.BlockPos;

import java.util.Map;

import static powie.sixbees.utils.Config.readBases;

public class BaseUtils {

    /**
     * @see <a href="https://en.wikipedia.org/wiki/Euclidean_distance">Formula</a>
     */
    public static boolean isInBase(BlockPos playerPos) {
        Map<String, Base> bases = readBases();
        if (bases.isEmpty()) return false;

        for (Base base : bases.values()) {
            int dx = playerPos.getX() - base.coords.getX();
            int dz = playerPos.getZ() - base.coords.getZ();
            if (dx * dx + dz * dz <= base.radius * base.radius) return true;
        }
        return false;
    }

    public static void saveBase(String key, Base base) {
        Map<String, Base> bases = readBases();
        bases.put(key, base);
        Config.writeBases(bases);
    }

    public static void removeBase(String key) {
        Map<String, Base> bases = readBases();
        bases.remove(key);
        Config.writeBases(bases);
    }

    public static class Base {
        public String name;
        public BlockPos coords;
        public int radius;
        public Dimension dimension;

        public Base(String name, BlockPos coords, int radius, Dimension dimension) {
            this.name = name;
            this.coords = coords;
            this.radius = radius;
            this.dimension = dimension;
        }
    }
}
