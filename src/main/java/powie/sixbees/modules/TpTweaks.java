package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import powie.sixbees.SixBees;
import powie.sixbees.utils.BaseUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TpTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgBase = settings.createGroup("While inside base");

    private final Setting<Boolean> autoCancelPreviousTpa = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-cancel-previous-tpa")
        .description("Automatically cancel previous TPA when a new one is sent")
        .defaultValue(true)
        .build()
    );

    // sgBase
    private final Setting<AutoDeny> autoDeny = sgBase.add(new EnumSetting.Builder<AutoDeny>()
        .name("auto-deny")
        .description("Automatically deny TPA requests from specific players while in base")
        .defaultValue(AutoDeny.NO_ONE)
        .build()
    );

    // stupid 2 space inconsistency
    // private static final Pattern REQUEST_SENT_PATTERN = Pattern.compile("^Request sent to: {2}(.+)$");
    // private static final String WAIT_PATTERN = "Please wait for your existing request to be accepted or denied.";
    // TPA_CANCEL_PATTERN has Duplicate in ChatListener
    private static final Pattern TPA_CANCEL_PATTERN = Pattern.compile("^You cancelled the teleport to (.+)!$");
    private static final String NO_TP_TO_CANCEL_PATTERN = "There is no teleport to cancel!";
    private static final Pattern TPA_REQUEST_PATTERN = Pattern.compile("^(.+) wants to teleport to you\\.$");

    // there inst even a message that says your request expired
    private boolean blockNextCancelMessage;

    public TpTweaks() {
        super(SixBees.CATEGORY, "tp-tweaks", "TP related utilities");
    }

    @Override
    public void onDeactivate() {
        blockNextCancelMessage = false;
    }

    // tpa
    // Request sent to:  KrynSpawn
    // Your request sent to KrynSpawn was accepted!
    // un88 wants to teleport to you.
    // You cancelled the teleport to KrynSpawn!
    // Please wait for your existing request to be accepted or denied.
    // There is no teleport to cancel!
    // Powie69, please login with the command: /login <password>

    // homes
    // You have no pending teleport requests to cancel!

    // hotspot
    // Your pending teleport has been cancelled.


    @EventHandler(priority = EventPriority.HIGH)
    private void onMessageReceive(ReceiveMessageEvent event) {
        String message = event.getMessage().getString();

        if (blockCancelMessage(message)) {
            event.cancel();
            return;
        }
        handleAutoDeny(message);
    }

    /**
     * SendCommandEvent not used for what I assume to be race condition issues.<br>
     * If y'all know why SendCommandEvent doesn't work please let me know.
     *
     * @author Powie69
     */
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundChatCommandPacket(String command))) return;
        handleRequestSent(command);
    }

    private void handleRequestSent(String command) {
        if (!autoCancelPreviousTpa.get() || !command.startsWith("tpa")) return;
        ChatUtils.sendPlayerMsg("/tpcancel", false);
        blockNextCancelMessage = true;
    }

    private boolean blockCancelMessage(String message) {
        if (!autoCancelPreviousTpa.get()) return false;
        Matcher matcher = TPA_CANCEL_PATTERN.matcher(message);
        if (!matcher.matches() && !message.equalsIgnoreCase(NO_TP_TO_CANCEL_PATTERN)) return false;
        blockNextCancelMessage = false;
        return true;
    }

    private void handleAutoDeny(String message) {
        if (autoDeny.get() == AutoDeny.NO_ONE) return;
        Matcher matcher = TPA_REQUEST_PATTERN.matcher(message);
        if (!matcher.matches() || !BaseUtils.isInBase()) return;
        String senderString = matcher.group(1);
        PlayerInfo sender = mc.getConnection().getPlayerInfo(senderString);
        if (sender == null) return;

        switch (autoDeny.get()) {
            case EVERYONE -> sendDenyCommand(senderString);
            case NON_FRIENDS -> {
                if (Friends.get().isFriend(sender)) return;
                sendDenyCommand(senderString);
            }
            case FRIENDS -> {
                if (!Friends.get().isFriend(sender)) return;
                sendDenyCommand(senderString);
            }
            case null, default -> throw new IllegalStateException("Invalid AutoDeny value: " + autoDeny.get());
        }

    }

    private void sendDenyCommand(String sender) {
        ChatUtils.sendPlayerMsg("/tpn " + sender, false);
    }

    private enum AutoDeny {
        EVERYONE,
        NON_FRIENDS,
        FRIENDS,
        NO_ONE;

        @Override
        public String toString() {
            return switch (this) {
                case EVERYONE -> "Everyone";
                case NON_FRIENDS -> "Non Friends";
                case FRIENDS -> "Friends";
                case NO_ONE -> "No one";
            };
        }
    }
}
