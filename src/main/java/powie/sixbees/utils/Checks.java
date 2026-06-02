package powie.sixbees.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Set;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Checks {
    /**
     * <a href="https://github.com/6b6t/AnarchyMod/blob/main/src/main/java/net/blockhost/anarchymod/Domains.java">Source</a>
     */
    private static final Set<String> DOMAINS = Set.of("6b6t.org", "10b10t.org", "6b6t.cc", "6b6t.me", "7b7t.me", "8b8t.org", "alacity.net", "anarchypvp.pw", "l2x9.org", "simpleanarchy.org");

    public static boolean is6B6T() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) return true; // Bypass check in dev environment
        ServerData server = mc.getCurrentServer();
        if (server == null || server.isLan()) return false;
        String ip = server.ip.split(":")[0].toLowerCase();
        return DOMAINS.stream().anyMatch(d -> ip.equals(d) || ip.endsWith("." + d));
    }

    public static boolean isDevEnvOrHasExtraArgs() {
        return FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("sixbees.extra");
    }
}
