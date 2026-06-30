package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.SixBees;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disableOnRun = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-run")
        .description("Disables the module after the first run")
        .defaultValue(true)
        .build()
    );

    private final Setting<String> command = sgGeneral.add(new StringSetting.Builder()
        .name("command")
        .description("The Command to execute")
        .defaultValue("/login")
        .build()
    );

    private final Setting<String> password = sgGeneral.add(new StringSetting.Builder()
        .name("password")
        .description("The password to log in with")
        .defaultValue("12345")
        // Powie remember this will be publicly available on github. Don't put your actual password here
        .build()
    );

    /**
     * Not added by default because every addon has this ngl
     */
    public AutoLogin() {
        super(SixBees.CATEGORY, "auto-login", "Runs /login command when you join a server.");
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (mc.isSingleplayer()) return;
        ChatUtils.sendPlayerMsg(command.get() + " " + password.get());
        if (disableOnRun.get()) disable();
    }

}
