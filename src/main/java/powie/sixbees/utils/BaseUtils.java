package powie.sixbees.utils;

import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.core.BlockPos;

import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.player.PlayerUtils.getDimension;
import static powie.sixbees.utils.Config.readBases;

public class BaseUtils {
    /**
     * @return The name of the base
     * @see <a href="https://en.wikipedia.org/wiki/Euclidean_distance">Formula</a>
     */
    public static String getBaseName() {
        Map<String, Base> bases = readBases();
        if (bases.isEmpty()) return "";

        for (Base base : bases.values()) {
            BlockPos scaledPos = convertCoords(mc.player.blockPosition(), getDimension(), base.dimension);
            if (scaledPos == null) continue;

            long dx = scaledPos.getX() - base.coords.getX();
            long dz = scaledPos.getZ() - base.coords.getZ();
            if (dx * dx + dz * dz <= (long) base.radius * base.radius) return base.name;
        }
        return "";
    }

    public static boolean isInBase() {
        return !getBaseName().isEmpty();
    }

    private static BlockPos convertCoords(BlockPos pos, Dimension from, Dimension to) {
        if (from == to) return pos;
        if (from == Dimension.Nether && to == Dimension.Overworld) {
            return new BlockPos(pos.getX() * 8, pos.getY(), pos.getZ() * 8);
        }
        if (from == Dimension.Overworld && to == Dimension.Nether) {
            return new BlockPos(pos.getX() / 8, pos.getY(), pos.getZ() / 8);
        }
        // if there's no valid conversion (e.g. involving the End).
        return null;
    }

    public static Map<String, Base> saveBase(String key, Base base) {
        Map<String, Base> bases = readBases();
        bases.put(key, base);
        Config.writeBases(bases);
        return bases;
    }

    public static Map<String, Base> removeBase(String key) {
        Map<String, Base> bases = readBases();
        bases.remove(key);
        Config.writeBases(bases);
        return bases;
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
