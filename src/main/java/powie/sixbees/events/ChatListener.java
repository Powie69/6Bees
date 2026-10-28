package powie.sixbees.events;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.utils.Checks;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static powie.sixbees.hud.SixBeesStarscript.setTpFields;

public class ChatListener {
    private static final Pattern YOU_TO_OTHER_PATTERN = Pattern.compile("^Teleporting in (\\d+) seconds\\.\\.\\. \\[Cancel]$");
    private static final Pattern REQUEST_ACCEPT_PATTERN = Pattern.compile("^Your request sent to (.+?) was accepted!$");
    private static final Pattern TPA_CANCEL_PATTERN = Pattern.compile("^You cancelled the teleport to (.+)!$");
    private static final Pattern HOME_TELEPORT_PATTERN = Pattern.compile("^Teleporting to (.+?) in (\\d+) seconds\\. \\[Cancel]$");
    private static final Pattern OTHER_TO_YOU_PATTERN = Pattern.compile("^Request from (.+?) accepted! \\[Cancel]$");
    private static final Pattern HOTSPOT_TELEPORT_PATTERN = Pattern.compile("^Teleporting you to hotspot of (.+?) in (\\d+) seconds\\. \\[Cancel]$");

    private static final List<TeleportHandler> HANDLERS = List.of(
        new TeleportHandler(YOU_TO_OTHER_PATTERN, m -> setTpFields(Integer.parseInt(m.group(1)), null)),
        new TeleportHandler(REQUEST_ACCEPT_PATTERN, m -> setTpFields(-1, m.group(1))),
        new TeleportHandler(OTHER_TO_YOU_PATTERN, m -> setTpFields(15, m.group(1))), // its always 15 right??
        new TeleportHandler(HOME_TELEPORT_PATTERN, m -> setTpFields(Integer.parseInt(m.group(2)), m.group(1))),
        new TeleportHandler(HOTSPOT_TELEPORT_PATTERN, m -> setTpFields(Integer.parseInt(m.group(2)), m.group(1))),

        // cancels / failures (fuck these inconsistencies)
        clearOn(TPA_CANCEL_PATTERN),
        clearOn("^Teleport failed.*$"),
        clearOn("(?i)^Your pending teleport has been cancelled\\.$"),
        clearOn("^Successfully cancelled your pending teleport request to:.*$"),
        clearOn("^You have no pending teleport requests to cancel!$"),
        clearOn("^Your pending teleport has been cancelled\\.$")
    );

    private boolean active = false;

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
        setTpFields(0, "");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (!active) return;
        String message = event.getMessage().getString();

        for (TeleportHandler handler : HANDLERS) {
            if (handler.tryHandle(message)) return;
        }
    }


    private record TeleportHandler(Pattern pattern, Consumer<Matcher> action) {
        boolean tryHandle(String message) {
            Matcher matcher = pattern.matcher(message);
            if (!matcher.matches()) return false;

            action.accept(matcher);
            return true;
        }
    }

    private static TeleportHandler clearOn(Pattern pattern) {
        return new TeleportHandler(pattern, _ -> setTpFields(0, ""));
    }

    private static TeleportHandler clearOn(String regex) {
        return clearOn(Pattern.compile(regex));
    }
}
