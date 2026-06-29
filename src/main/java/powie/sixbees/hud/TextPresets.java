package powie.sixbees.hud;

import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import powie.sixbees.SixBees;

public class TextPresets {
    public static final HudElementInfo<TextHud> INFO =
        new HudElementInfo<>(
            SixBees.HUD_GROUP,
            "6Bees",
            "6bees custom text element",
            TextPresets::create);

    static {
        addPreset("Base name", "Base: #1{sixbees.base}", 10);
        addPreset("Protected position", "Pos#1{floor(sixbees.protected_pos.x)}, {floor(camera.pos.y)}, {floor(sixbees.protected_pos.z)}", 1);
        addPreset("Teleport countdown", "Tp countdown: #1{sixbees.tp.countdown}", 1);
        addPreset("Teleport destination", "Tp destination: #1{sixbees.tp.destination}", 1);
    }

    private static TextHud create() {
        return new TextHud(INFO);
    }

    private static void addPreset(String title, String text, int updateDelay) {
        INFO.addPreset(title, textHud -> {
            if (text != null) textHud.text.set(text);
            textHud.updateDelay.set(updateDelay);
        });
    }
}


