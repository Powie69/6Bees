package powie.sixbees.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;

public class GetMapId extends Command {
    public GetMapId() {
        super("get-map-id", "Gets the map id of the current map your holding");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            if (mc.player.getMainHandItem().getItem() != Items.FILLED_MAP) return SINGLE_SUCCESS;
            info(String.valueOf(mc.player.getMainHandItem().get(DataComponents.MAP_ID)));

            return SINGLE_SUCCESS;
        });
    }
}
