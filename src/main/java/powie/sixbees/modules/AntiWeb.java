package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.Shapes;
import powie.sixbees.SixBees;

public class AntiWeb extends Module {
    public AntiWeb() {
        super(SixBees.CATEGORY, "anti-web", "Prevents you from walking into webs. but not falling into them.");
    }

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (mc.level == null || mc.player == null) return;
        if (event.state.getBlock() == Blocks.COBWEB && mc.player.fallDistance <= 0) event.shape = Shapes.block();
    }
}

