package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.entity.player.PlaceBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import powie.sixbees.SixBees;

import java.util.HashSet;
import java.util.Set;

public class DoubleEchest extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<PreferredSide> preferredSide = sgGeneral.add(new EnumSetting.Builder<PreferredSide>()
        .name("preferred-side")
        .description("Will prioritize placing the Ender chest on this side.")
        .defaultValue(PreferredSide.LEFT)
        .build()
    );

    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("The delay in ticks between placing blocks.")
        .defaultValue(1)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Set<BlockPos> autoPlacedPositions = new HashSet<>();
    private int placeCooldown;

    /**
     * Known issue: It does weird things if it touches grass 💀
     */
    public DoubleEchest() {
        super(SixBees.CATEGORY, "double-echest", "Automatically creates a double Ender chest when you place one.");
    }

    @Override
    public void onDeactivate() {
        autoPlacedPositions.clear();
        placeCooldown = 0;
    }

    @EventHandler
    private void onBlockPlace(PlaceBlockEvent event) {
        if (!(event.block instanceof EnderChestBlock echest)) return;
        BlockPos blockPos = event.blockPos;
        if (!Level.isInSpawnableBounds(blockPos)) return;

        if (autoPlacedPositions.contains(blockPos)) {
            autoPlacedPositions.remove(blockPos);
            return;
        }
        if (placeCooldown > 0) return;

        PreferredSide[] sides = PreferredSide.values();
        int startIndex = preferredSide.get().ordinal();

        for (int i = 0; i < sides.length; i++) {
            PreferredSide side = sides[(startIndex + i) % sides.length];

            BlockPos targetPos = switch (side) {
                case LEFT -> event.blockPos.west();
                case RIGHT -> event.blockPos.east();
                case FRONT -> event.blockPos.south();
                case BACK -> event.blockPos.north();
            };

            if (!PlayerUtils.isWithinReach(targetPos)) continue;

            if (!mc.level.getBlockState(targetPos).canBeReplaced()) continue;
            if (!mc.level.isUnobstructed(echest.defaultBlockState(), targetPos, CollisionContext.empty())) continue;

//            BlockState state = mc.level.getBlockState(targetPos);
//            if (state. isAir() || isClickable(state.getBlock())) continue;

            autoPlacedPositions.add(targetPos);
            boolean placed = BlockUtils.place(
                targetPos,
                InvUtils.findInHotbar(itemStack -> Block.byItem(itemStack.getItem()) instanceof EnderChestBlock),
                0
            );

            if (placed) {
                placeCooldown = placeDelay.get();
            } else {
                autoPlacedPositions.remove(targetPos);
            }
            return;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (placeCooldown > 0) {
            placeCooldown--;
        }
    }

    private enum PreferredSide {
        LEFT,
        RIGHT,
        FRONT,
        BACK,
    }

}
