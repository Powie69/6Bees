package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import powie.sixbees.SixBees;

public class AntiWeb extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public AntiWeb() {
        super(SixBees.CATEGORY, "anti-web", "An example module that highlights the center of the world.");
    }

    public static final VoxelShape NO_TOP_COLLISION = Shapes.or(
        Block.box(0, 0, 0, 16, 1e-5, 16),
        // North wall (Z-)
        Block.box(0, 0, 0, 16, 16, 1e-5),
        // South wall (Z+)
        Block.box(0, 0, 16 - 1e-5, 16, 16, 16),
        // West wall (X-)
        Block.box(0, 0, 0, 1e-5, 16, 16),
        // East wall (X+)
        Block.box(16 - 1e-5, 0, 0, 16, 16, 16)
    );

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (event.state.getBlock() == Blocks.COBWEB) {
            event.shape = NO_TOP_COLLISION;
        }
    }
}
