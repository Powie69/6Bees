package powie.sixbees.modules;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import meteordevelopment.meteorclient.systems.modules.Module;
import powie.sixbees.SixBees;

public class StashMover extends Module {
    public StashMover() {
        super(SixBees.CATEGORY, "Stash Mover", "Moves your stash to a different location");
    }

    // bro where do I even begin 💀

    //    IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

    // huh?
    public IBaritone getBaritone() {
        return baritone;
    }

    public void erm() {
        getBaritone().getCommandManager().execute("goto 69 69");
    }
}
