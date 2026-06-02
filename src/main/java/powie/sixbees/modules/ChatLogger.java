package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.SixBees;

public class ChatLogger extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public ChatLogger() {
        super(SixBees.CATEGORY, "chat-logger", "Used for development purposes");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        String message = event.getMessage().getString();

        if (!message.contains("[Meteor] [Chat Logger]")) { // don't crash my pc
            info(message);
        }
    }
}
