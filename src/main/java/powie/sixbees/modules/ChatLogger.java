package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import powie.sixbees.SixBees;
import powie.sixbees.utils.ServerCheck;

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
