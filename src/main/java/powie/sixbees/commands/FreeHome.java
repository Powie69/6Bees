package powie.sixbees.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import powie.sixbees.utils.JoinPayload;

public class FreeHome extends Command {
    public FreeHome() {
        super("free-home", "Gives you 2 free homes. This spoofs anarchy-mod (not clickbait)", "gimmehouse");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            mc.getConnection().send(new ServerboundCustomPayloadPacket(new JoinPayload()));
            info("Command sent. run /homes to check");
            return SINGLE_SUCCESS;
        });


    }
}