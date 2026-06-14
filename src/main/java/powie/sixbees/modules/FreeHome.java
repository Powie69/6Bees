package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import powie.sixbees.SixBees;
import powie.sixbees.utils.JoinPayload;

import static powie.sixbees.utils.Checks.is6B6T;

public class FreeHome extends Module {
    public FreeHome() {
        super(SixBees.CATEGORY, "Free-Home", "Gives you 2 extra homes.");
    }

    /**
     * A GameJoinedEvent event handler in this module is not needed because this method runs every GameJoinedEvent
     *
     * @see meteordevelopment.meteorclient.systems.modules.Modules#onGameJoined(GameJoinedEvent)
     */
    @Override
    public void onActivate() {
        if (is6B6T()) mc.getConnection().send(new ServerboundCustomPayloadPacket(new JoinPayload()));
    }
}
