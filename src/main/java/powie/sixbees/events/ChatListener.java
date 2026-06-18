package powie.sixbees.events;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.utils.Checks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener {
    private boolean active = false;
    private final Pattern HOME_TELEPORT_PATTERN = Pattern.compile("^Teleporting to (.+?) in (\\d+) seconds\\. \\[Cancel]$");
    private final Pattern YOU_TO_OTHER_PATTERN = Pattern.compile("^Teleporting in (\\d+) seconds\\.\\.\\. \\[Cancel]$");
    private final Pattern OTHER_TO_YOU_PATTERN = Pattern.compile("^Request from (.+?) accepted! \\[Cancel]$");

    /**
     * <p>Hello reader. This is for listening to chat without it being bound to a module or hud</p>
     * <p>if this is the wrong way of doing it. then please TELL ME. PLEASEEEEEE</p>
     *
     * @author Powie69
     */
    public ChatListener() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onJoinGame(GameJoinedEvent event) {
        if (Checks.is6B6T()) active = true;
    }

    @EventHandler
    private void onLeaveGame(GameLeftEvent event) {
        active = false;
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (!active) return;
        String message = event.getMessage().getString();

        handleHomeTeleport(message);
        handleYouToOther(message);
        handleOtherToYou(message);

        // tpa and home failure doesn't have the same message
        if (message.equals("Teleport failed!") || message.equals("Teleport failed."))
            MeteorClient.EVENT_BUS.post(new TeleportMessageEvent(0));
    }

    private void handleHomeTeleport(String message) {
        Matcher matcher = HOME_TELEPORT_PATTERN.matcher(message);
        if (!matcher.matches()) return;

        int seconds = Integer.parseInt(matcher.group(2));
        MeteorClient.EVENT_BUS.post(new TeleportMessageEvent(seconds));
    }

    private void handleYouToOther(String message) {
        Matcher matcher = YOU_TO_OTHER_PATTERN.matcher(message);
        if (!matcher.matches()) return;

        int seconds = Integer.parseInt((matcher.group(1)));
        MeteorClient.EVENT_BUS.post(new TeleportMessageEvent(seconds));
    }

    private void handleOtherToYou(String message) {
        Matcher matcher = OTHER_TO_YOU_PATTERN.matcher(message);
        if (!matcher.matches()) return;
        // its always 15 right??
        MeteorClient.EVENT_BUS.post(new TeleportMessageEvent(15));
    }
}
