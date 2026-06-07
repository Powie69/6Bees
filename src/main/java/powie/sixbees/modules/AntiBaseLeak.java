package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.SixBees;

public class AntiBaseLeak extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> preventTpa = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-tpa")
        .description("Prevents you from accepting tpa requests while in your base")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> allowFriendsTpa = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-friends")
        .description("Don't block tpa request coming from friends")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> preventHotspot = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-hotspots")
        .description("Prevents you from creating hotspots while in your base")
        .defaultValue(true)
        .build()
    );

    public AntiBaseLeak() {
        super(SixBees.CATEGORY, "anti-base-leak", "Prevents you from leaking your base");
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        String message = event.message;

        info(message);

    }


}
