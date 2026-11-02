package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.entity.player.PlaceBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        .description("Prioritizes placing the Ender chest on this side relative to your facing direction.")
        .defaultValue(PreferredSide.LEFT)
        .build()
    );

    private final Setting<Boolean> preventDuplicates = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-duplicates")
        .description("Do nothing if echest is already connected")
        .defaultValue(true)
        .build()
    );

    private final Set<BlockPos> autoPlacedPositions = new HashSet<>();

    /**
     * Known issue: It does weird things if it touches grass 💀
     */
    public DoubleEchest() {
        super(SixBees.CATEGORY,
            "double-echest",
            "Automatically creates a double Ender chest when you place one.",
            "double ender chest");
    }

    @Override
    public void onDeactivate() {
        autoPlacedPositions.clear();
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

        if (preventDuplicates.get()) {
            for (PreferredSide side : PreferredSide.values()) {
                BlockPos checkPos = switch (side) {
                    case LEFT -> blockPos.west();
                    case RIGHT -> blockPos.east();
                    case FRONT -> blockPos.south();
                    case BACK -> blockPos.north();
                };
                if (mc.level.getBlockState(checkPos).getBlock() instanceof EnderChestBlock) return;
            }
        }

        Direction echestFacing = mc.player.getDirection();

        PreferredSide[] sides = PreferredSide.values();
        int startIndex = preferredSide.get().ordinal();

        for (int i = 0; i < sides.length; i++) {
            PreferredSide side = sides[(startIndex + i) % sides.length];

            Direction targetDirection = switch (side) {
                case LEFT -> echestFacing.getCounterClockWise();
                case RIGHT -> echestFacing.getClockWise();
                case FRONT -> echestFacing;
                case BACK -> echestFacing.getOpposite();
            };
            BlockPos targetPos = blockPos.relative(targetDirection);

            if (!PlayerUtils.isWithinReach(targetPos)) continue;

            if (!mc.level.getBlockState(targetPos).canBeReplaced()) continue;
            if (!mc.level.isUnobstructed(echest.defaultBlockState(), targetPos, CollisionContext.empty())) continue;

            autoPlacedPositions.add(targetPos);
            boolean placed = BlockUtils.place(
                targetPos,
                InvUtils.findInHotbar(itemStack -> Block.byItem(itemStack.getItem()) instanceof EnderChestBlock),
                0
            );

            if (!placed) autoPlacedPositions.remove(targetPos);
            return;
        }
    }

    private enum PreferredSide {
        LEFT,
        RIGHT,
        FRONT,
        BACK,
    }
}
