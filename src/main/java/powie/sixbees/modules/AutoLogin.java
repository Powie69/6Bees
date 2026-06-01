package powie.sixbees.modules;

/**
 * Not added by default because every addon has this ngl
 */

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.SixBees;

public class AutoLogin extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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


    public AutoLogin() {
        super(SixBees.CATEGORY, "auto-login", "Runs /login command when you join a server.");
        runInMainMenu = true;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        ChatUtils.sendPlayerMsg(command.get() + " " + password.get());
    }

}
