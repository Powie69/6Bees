package powie.sixbees.events;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.utils.Checks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static powie.sixbees.hud.SixBeesStarscript.setTpFields;

public class ChatListener {
    private static final Pattern YOU_TO_OTHER_PATTERN = Pattern.compile("^Teleporting in (\\d+) seconds\\.\\.\\. \\[Cancel]$");
    private static final Pattern REQUEST_ACCEPT_PATTERN = Pattern.compile("^Your request sent to (.+?) was accepted!$");
    private static final Pattern HOME_TELEPORT_PATTERN = Pattern.compile("^Teleporting to (.+?) in (\\d+) seconds\\. \\[Cancel]$");
    private static final Pattern OTHER_TO_YOU_PATTERN = Pattern.compile("^Request from (.+?) accepted! \\[Cancel]$");
    private static final Pattern HOTSPOT_TELEPORT_PATTERN = Pattern.compile("^Teleporting you to hotspot of (.+?) in (\\d+) seconds\\. \\[Cancel]$");

    private boolean active = false;

    /**
     * <p>Hello reader. This is for listening to ReceiveMessageEvent without it being bound to a module or hud</p>
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

        handleYouToOther(message);
        handleRequestAccept(message);
        handleOtherToYou(message);
        handleHomeTeleport(message);
        handleHotspotTeleport(message);
        // tpa and home failure doesn't have the same message
        if (message.equals("Teleport failed!") || message.equals("Teleport failed.")) {
            setTpFields(0, "");
        }
    }

    // teleports
    private void handleYouToOther(String message) {
        Matcher matcher = YOU_TO_OTHER_PATTERN.matcher(message);
        if (!matcher.matches()) return;

        int seconds = Integer.parseInt((matcher.group(1)));
        setTpFields(seconds, null);
    }

    private void handleRequestAccept(String message) {
        Matcher matcher = REQUEST_ACCEPT_PATTERN.matcher(message);
        if (!matcher.matches()) return;

        String destination = matcher.group(1);
        setTpFields(-1, destination);
    }

    private void handleOtherToYou(String message) {
        Matcher matcher = OTHER_TO_YOU_PATTERN.matcher(message);
        if (!matcher.matches()) return;
        // its always 15 right??
        String destination = matcher.group(1);
        setTpFields(15, destination);
    }

    private void handleHomeTeleport(String message) {
        Matcher matcher = HOME_TELEPORT_PATTERN.matcher(message);
        if (!matcher.matches()) return;

        String destination = matcher.group(1);
        int seconds = Integer.parseInt(matcher.group(2));
        setTpFields(seconds, destination);
    }

    private void handleHotspotTeleport(String message) {
        Matcher matcher = HOTSPOT_TELEPORT_PATTERN.matcher(message);
        if (!matcher.matches()) return;

        String destination = matcher.group(1);
        int seconds = Integer.parseInt(matcher.group(2));
        setTpFields(seconds, destination);
    }

    // pvp mode
    private void handlePvpMode(String message) {
        // TODO
    }
}
