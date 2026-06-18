package powie.sixbees.modules;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.util.Util;
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
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList l = theme.verticalList();
        l.add(theme.label("If a nsfw map ins't blocked,\nplease consider contributing to the data set <3"));
        WButton button = l.add(theme.button("Open github repo")).widget();
        button.action = () -> Util.getPlatform().openUri("https://github.com/Powie69/6bees-data");
        return l;
    }

    @Override
    public void onActivate() {
        if (isDevEnvOrHasExtraArgs()) info(NSFW_MAPS.toString());
    }
}
