package powie.sixbees.hud;

import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import powie.sixbees.SixBees;

public class TextPresets {
    public static final HudElementInfo<TextHud> INFO =
        new HudElementInfo<>(
            SixBees.HUD_GROUP,
            "6Bees",
            "Hud features",
            TextPresets::create);

    static {
        addPreset("Base name", "Base: #1{sixbees.base}");
    }

    private static TextHud create() {
        return new TextHud(INFO);
    }

    private static void addPreset(String title, String text) {
        INFO.addPreset(title, textHud -> {
            if (text != null) textHud.text.set(text);
            textHud.updateDelay.set(1);
        });
    }
}


