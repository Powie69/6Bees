package powie.sixbees.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import powie.sixbees.SixBees;

import java.util.Set;

import static powie.sixbees.utils.Checks.isDevEnvOrHasExtraArgs;
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
        if (isDevEnvOrHasExtraArgs()) info(NSFW_MAPS.toString());
    }
}
