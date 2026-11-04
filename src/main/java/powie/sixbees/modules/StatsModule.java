package powie.sixbees.modules;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import powie.sixbees.SixBees;

public class StatsModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgChat = settings.createGroup("Chat");
    private final SettingGroup sgScreen = settings.createGroup("Window");

    // sgGeneral
    private final Setting<DateFormat> dateFormat = sgGeneral.add(new EnumSetting.Builder<DateFormat>()
        .name("date-format")
        .description("Date format")
        .defaultValue(DateFormat.DD_MM_YYYY)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("priority")
        .description("Which target to select")
        .defaultValue(SortPriority.ClosestAngle)
        .build()
    );

    // sgChat
    private final Setting<Keybind> chatKeybind = sgChat.add(new KeybindSetting.Builder()
        .name("chat-keybind")
        .description("Keybind to show stats of a targeted player in chat") // TODO: Improve description
        .build()
    );

    // sgScreen
    private final Setting<Keybind> screenKeybind = sgScreen.add(new KeybindSetting.Builder()
        .name("screen-keybind")
        .description("Keybind to show stats of a targeted player in a window gui")
        .build()
    );

    public StatsModule() {
        super(SixBees.CATEGORY,
            "Stats",
            "An example module that highlights the center of the world.",
            "Player Statistics",
            "Statistics");
    }

    public enum DateFormat {
        DD_MM_YYYY,
        MM_DD_YYYY
    }
}
