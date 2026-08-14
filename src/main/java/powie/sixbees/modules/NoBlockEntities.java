package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.render.RenderBlockEntityEvent;
import meteordevelopment.meteorclient.mixin.BlockEntityRenderStateAccessor;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import powie.sixbees.SixBees;

import java.util.List;

public class NoBlockEntities extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("render-radius")
        .description("The radius in which the blocks will render.")
        .defaultValue(0)
        .min(0)
        .sliderMax(128)
        .build()
    );

    private final Setting<List<Block>> blockEntities = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("Select which block entities to not render")
        .filter(block -> block instanceof EntityBlock)
        .build()
    );

    public NoBlockEntities() {
        super(SixBees.CATEGORY, "no-block-entities", "Disables rendering for specified block entities. supports radius");
    }

    @EventHandler
    private void onRenderBlockEntity(RenderBlockEntityEvent event) {
        BlockEntityRenderState block = event.blockEntityState;

        if (blockEntities.get().contains(((BlockEntityRenderStateAccessor) event.blockEntityState).meteor$getBlockState().getBlock())) {
            if (PlayerUtils.squaredDistanceTo(block.blockPos) > radius.get() * radius.get()) event.cancel();
        }
    }
}
