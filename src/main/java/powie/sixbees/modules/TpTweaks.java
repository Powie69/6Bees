package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import powie.sixbees.SixBees;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TpTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> autoCancelPreviousTpa = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-cancel-previous-tpa")
        .description("Automatically cancel previous TPAs when a new one is sent")
        .defaultValue(true)
        .build()
    );

    // stupid 2 space inconsistency
    // private static final Pattern REQUEST_SENT_PATTERN = Pattern.compile("^Request sent to: {2}(.+)$");
    // TPA_CANCEL_PATTERN has Duplicate in ChatListener
    private static final Pattern TPA_CANCEL_PATTERN = Pattern.compile("^You cancelled the teleport to (.+)!$");
    private static final String WAIT_PATTERN = "Please wait for your existing request to be accepted or denied.";
    private static final String NO_TP_TO_CANCEL_PATTERN = "There is no teleport to cancel!";

    private boolean blockNextCancelMessage;

    // there inst even a message that says your request expired

    public TpTweaks() {
        super(SixBees.CATEGORY, "tp-tweaks", "Tp related utilities");
    }

    // Request sent to:  KrynSpawn
    //  Your request sent to KrynSpawn was accepted!
    // un88 wants to teleport to you.
    // You cancelled the teleport to KrynSpawn!
    // Please wait for your existing request to be accepted or denied.
    // There is no teleport to cancel!


    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        String message = event.getMessage().getString();

        if (handleBlockNextCancelMessage(message)) event.cancel();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundChatCommandPacket(String command))) return;
        handleRequestSent(command);
    }

    private void handleRequestSent(String message) {
        if (!autoCancelPreviousTpa.get() || !message.startsWith("tpa")) return;

        ChatUtils.sendPlayerMsg("/tpcancel", false);
        blockNextCancelMessage = true;
    }

    private boolean handleBlockNextCancelMessage(String message) {
        if (!blockNextCancelMessage) return false;
        Matcher matcher = TPA_CANCEL_PATTERN.matcher(message);
        if (!matcher.matches() && !message.equalsIgnoreCase(NO_TP_TO_CANCEL_PATTERN)) return false;
        blockNextCancelMessage = false;
        return true;
    }
}
