package powie.sixbees.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class GetMapId extends Command {
    public GetMapId() {
        super("get-map-id", "Gets the map id of the current map your holding or looking at");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            Integer id = findMapId();

            if (id != null) {
                mc.keyboardHandler.setClipboard(String.valueOf(id));
                info(id + " copied to clipboard.");
            } else {
                error("You are not holding a map or looking at a map");
            }

            return SINGLE_SUCCESS;
        });

        builder.then(literal("print").executes(_ -> {
            Integer id = findMapId();

            if (id != null) {
                info(String.valueOf(id));
            } else {
                error("You are not holding a map or looking at a map");
            }

            return SINGLE_SUCCESS;
        }));
    }

    private Integer findMapId() {
        if (mc.player.getMainHandItem().getItem() == Items.FILLED_MAP) {
            return mc.player.getMainHandItem().get(DataComponents.MAP_ID).id();
        } else if (mc.player.getOffhandItem().getItem() == Items.FILLED_MAP) {
            return mc.player.getOffhandItem().get(DataComponents.MAP_ID).id();
        } else if (mc.hitResult.getType() == HitResult.Type.ENTITY
            && ((EntityHitResult) mc.hitResult).getEntity() instanceof ItemFrame frame
            && frame.hasFramedMap()) {
            return frame.getFramedMapId(frame.getItem()).id();
        }

        return null;
    }
}
