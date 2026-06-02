package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import powie.sixbees.SixBees;
import powie.sixbees.utils.JoinPayload;

import static powie.sixbees.utils.Checks.is6B6T;

public class FreeHome extends Module {
    public FreeHome() {
        super(SixBees.CATEGORY, "Free-Home", "Gives you 2 extra homes.");
    }

    @Override
    public void onActivate() {
        if (is6B6T()) mc.getConnection().send(new ServerboundCustomPayloadPacket(new JoinPayload()));
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (is6B6T()) mc.getConnection().send(new ServerboundCustomPayloadPacket(new JoinPayload()));
    }
}
