package powie.sixbees.modules;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import powie.sixbees.SixBees;

public class AntiTinnitus extends Module {
    /**
     * skidded from <a href="https://github.com/H1ggsK/BlackOut">Blackout</a>
     */

    private final SettingGroup sgExplosion = this.settings.createGroup("Explosion");

    public final BoolSetting explosionSound = sgExplosion.add(new BoolSetting.Builder()
        .name("explosion-sound")
        .description("modifies the explosion sound")
        .defaultValue(true)
        .build()
    );

    public final Setting<Double> explosionVolume = sgExplosion.add(new DoubleSetting.Builder()
        .name("Explosion Volume")
        .description("Multiplies explosion volumes.")
        .defaultValue(0.07)
        .sliderRange(0, 10)
        .visible(explosionSound::get)
        .build()
    );

    public final Setting<Double> explosionPitch = sgExplosion.add(new DoubleSetting.Builder()
        .name("Explosion Pitch")
        .description("Multiplies pitch of explosions sounds.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(explosionSound::get)
        .build()
    );

    public AntiTinnitus() {
        super(SixBees.CATEGORY, "anti-tinnitus", "Makes the game less noisy");
    }



}
