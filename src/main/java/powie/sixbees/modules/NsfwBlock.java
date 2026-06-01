package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import powie.sixbees.SixBees;

import java.util.Set;

import static powie.sixbees.utils.Config.readMaps;

public class NsfwBlock extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public final Setting<Boolean> replace = sgGeneral.add(new BoolSetting.Builder()
        .name("replace-with-6bees-logo")
        .description("Replaces the nsfw map art with 6bees logo")
        .defaultValue(true)
        .build()
    );

    public NsfwBlock() {
        super(SixBees.CATEGORY, "nsfw-blocker", "Blocks rendering of nsfw map arts");
    }

    private final Set<Integer> NSFW_MAPS = readMaps();

    @Override
    public void onActivate() {
        info(NSFW_MAPS.toString());
    }
}
