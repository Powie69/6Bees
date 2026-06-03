package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import powie.sixbees.SixBees;

public class AntiBedTrap extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Boolean> allowExplosiveBypass = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-explosive-bypass")
        .description("Allows interaction with respawn blocks in dimensions where they explode (e.g., beds in the Nether).")
        .defaultValue(true)
        .build()
    );

    /**
     * <blockquote>
     * At the end of each game tick only the highest single explosion damage is chosen. Note that the main damage value
     * has a +1 at the end, <b>so all entities in range (within 2⋅power) of an explosion receive at least 1 damage even
     * when the explosion is fully blocked.</b> The only exceptions are invulnerable entities, Nether stars, and Peaceful
     * players.
     * </blockquote>
     * @see <a href="https://minecraft.wiki/w/Explosion#Damage">source</a>
     */
    private final Setting<Double> maxDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-self-damage-threshold")
        .description("Will not interact with respawn block if explosion damage of it is greater than this.\nSet to 0 to disable.")
        .defaultValue(1)
        .min(0)
        .max(2048)
        .sliderMax(20)
        .visible(allowExplosiveBypass::get)
        .build()
    );

    // todo: integrate with coords. interact with beds if in coords range
    public AntiBedTrap() {
        super(SixBees.CATEGORY, "anti-bed-trap", "Prevents you from interacting with any respawn blocks");
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        Block block = mc.level.getBlockState(event.result.getBlockPos()).getBlock();
        if (block instanceof BedBlock) {
            if (allowExplosiveBypass.get() &&
                mc.level.dimension() != Level.OVERWORLD &&
                maxDamage.get() > DamageUtils.bedDamage(mc.player, event.result.getBlockPos().getCenter())) {
                return;
            }
            event.cancel();
            return;
        }
        if (block instanceof RespawnAnchorBlock) {
            if (allowExplosiveBypass.get() &&
                mc.level.dimension() != Level.NETHER &&
                maxDamage.get() > DamageUtils.anchorDamage(mc.player, event.result.getBlockPos().getCenter())) {
                return;
            }
            event.cancel();
        }
    }
}
