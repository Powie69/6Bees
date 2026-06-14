package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import powie.sixbees.SixBees;

public class Bees extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public Bees() {
        super(SixBees.CATEGORY, "Bees", "Places and breaks beehives at an insane rate");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        BlockPos eyePosition = mc.player.blockPosition().relative(mc.player.getDirection());
//        info(String.valueOf(eyePosition));

        BlockPos pos = eyePosition;
        BlockUtils.place(pos, InvUtils.find(Items.BEE_NEST), 0, false);

        BlockUtils.breakBlock(pos, true);

    }
}
