package powie.sixbees.hud;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;
import powie.sixbees.utils.BaseUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static powie.sixbees.utils.BaseUtils.isInBase;

public class SixBeesStarscript {
    private static int tpSeconds;
    private static String tpDestination = "";

    public static void setTpFields(int seconds, String destination) {
        if (seconds != -1) tpSeconds = seconds * 20;
        if (destination != null) tpDestination = destination;
    }

    private static class EventListener {
        @EventHandler
        private void onTick(TickEvent.Post event) {
            if (tpSeconds > 0) tpSeconds--;
        }
    }

    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(new EventListener()); // Ts so buns

        MeteorStarscript.ss.set("sixbees", new ValueMap()
            .set("base", SixBeesStarscript::handleBase)
            .set("is_in_base", () -> Value.bool(mc.player != null && isInBase()))

            .set("tp", new ValueMap()
                .set("countdown", () -> Value.number((double) tpSeconds / 20))
                .set("destination", () -> Value.string(tpSeconds != 0 ? tpDestination : ""))
            )

            .set("protected_pos", new ValueMap()
                .set("x", () -> handleProtectedPos("x"))
                .set("z", () -> handleProtectedPos("z"))
            )

            .set("opposite_dim_protected", new ValueMap()
                .set("x", () -> handleProtectedPosOpposite("x"))
                .set("z", () -> handleProtectedPosOpposite("z"))
            )
        );
    }

    private static Value handleBase() {
        if (mc.player == null) return Value.string("");
        return Value.string(BaseUtils.getBaseName());
    }

    private static Value handleProtectedPos(String axis) {
        if (mc.player == null) return Value.number(0);
        if (isInBase()) return Value.number(0);

        return switch (axis) {
            case "x" -> Value.number(mc.player.blockPosition().getX());
            case "z" -> Value.number(mc.player.blockPosition().getZ());
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        };
    }

    private static Value handleProtectedPosOpposite(String axis) {
        if (mc.player == null) return Value.number(0);
        Dimension dimension = PlayerUtils.getDimension();
        if (dimension == Dimension.End) return Value.number(0);
        if (isInBase()) return Value.number(0);

        double val = switch (axis) {
            case "x" -> mc.player.getX();
            case "z" -> mc.player.getZ();
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        };

        if (dimension == Dimension.Overworld) val /= 8;
        else if (dimension == Dimension.Nether) val *= 8;

        return Value.number(val);
    }
}
