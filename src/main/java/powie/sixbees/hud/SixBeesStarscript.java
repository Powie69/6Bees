package powie.sixbees.hud;

import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import org.meteordev.starscript.value.Value;
import org.meteordev.starscript.value.ValueMap;
import powie.sixbees.utils.BaseUtils;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SixBeesStarscript {
    public static void init() {
        MeteorStarscript.ss.set("sixbees", new ValueMap()
            .set("base", SixBeesStarscript::handleBase)
        );
    }

    /**
     * It is bad that it reads every time it runs???
     * TODO: if above statement is true, then do something about it.
     */
    private static Value handleBase() {
        if (mc.player == null) return Value.string("");
        return Value.string(BaseUtils.getBaseAt(mc.player.blockPosition()));
    }


}
