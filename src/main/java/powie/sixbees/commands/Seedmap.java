package powie.sixbees.commands;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.net.URI;

public class Seedmap extends Command {
    private final String SEEDMAP_URL = "https://www.chunkbase.com/apps/seed-map";
    private final String SEED = "4172144997902289642";
    private final String PLATFORM = "java_26_3";

    public Seedmap() {
        super("seedmap", "Opens a seedmap in your browser with your current location");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            if (!Utils.canUpdate()) return SINGLE_SUCCESS;

            String dimension = switch (PlayerUtils.getDimension()) {
                case Nether -> "nether";
                case End -> "end";
                default -> "overworld";
            };

            assert mc.player != null;
            String fragment = """
                #seed=-%s\
                &platform=%s&dimension=%s&x=%d&z=%d\
                """.formatted(SEED, PLATFORM, dimension, mc.player.getBlockX(), mc.player.getBlockZ());

            Blaze3D.openUri(URI.create(SEEDMAP_URL + fragment));

            return SINGLE_SUCCESS;
        });
    }
}
