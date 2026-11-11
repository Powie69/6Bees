package powie.sixbees.utils;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;

// TODO: custom update checker

public class GlobalSettings {
    public static Setting<Boolean> openVerifyLinkButton;

    public static void init() {
        SettingGroup sg = Config.get().settings.createGroup("6bees");

        openVerifyLinkButton = sg.add(new BoolSetting.Builder()
            .name("open-verification-link-button")
            .description("Shows a button to quickly open the verification website")
            .defaultValue(true)
            .build()
        );
    }
}
