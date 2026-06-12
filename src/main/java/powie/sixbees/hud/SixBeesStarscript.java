package powie.sixbees.hud;

import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;
import powie.sixbees.utils.BaseUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static powie.sixbees.utils.BaseUtils.isInBase;

public class SixBeesStarscript {
    public static void init() {
        MeteorStarscript.ss.set("sixbees", new ValueMap()
            .set("base", SixBeesStarscript::handleBase)

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
        return Value.string(BaseUtils.getBaseAt(mc.player.blockPosition()));
    }

    private static Value handleProtectedPos(String axis) {
        if (mc.player == null) return Value.number(0);
        if (isInBase(mc.player.blockPosition())) return Value.number(0);

        return switch (axis) {
            case "x" -> Value.number(mc.player.blockPosition().getX());
            case "z" -> Value.number(mc.player.blockPosition().getZ());
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        };
    }

    private static Value handleProtectedPosOpposite(String axis) {
        if (mc.player == null) return Value.number(0);
        if (isInBase(mc.player.blockPosition())) return Value.number(0);

        double val = switch (axis) {
            case "x" -> mc.player.getX();
            case "y" -> mc.player.getY();
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        };

        Dimension dimension = PlayerUtils.getDimension();
        if (dimension == Dimension.Overworld) val /= 8;
        else if (dimension == Dimension.Nether) val *= 8;

        return Value.number(val);
    }
}
