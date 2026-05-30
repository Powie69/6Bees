package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.render.RenderBossBarEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.components.LerpingBossEvent;
import powie.sixbees.SixBees;

import java.util.ArrayList;
import java.util.List;

import static powie.sixbees.utils.ServerCheck.is6B6T;

public class AdBlock extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final BoolSetting filterChat = sgGeneral.add(new BoolSetting.Builder()
        .name("chat")
        .description("Blocks server messages telling you about commands and voting")
        .defaultValue(true)
        .build()
    );

    private final BoolSetting filterWithers = sgGeneral.add(new BoolSetting.Builder()
        .name("wither-bars")
        .description("Hides wither bars. Except for hotspots")
        .defaultValue(true)
        .build()
    );

    public AdBlock() {
        super(SixBees.CATEGORY, "ad-block", "Blocks ads in various mediums");
    }

    @Override
    public void onActivate() {
        if (!is6B6T()) {
            error("This module is only compatible with 6b6t");
            toggle();
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (!filterWithers.get()) return;

        String message = event.getMessage().getString();

        if (message.startsWith("---------------------------")) {
            event.cancel();
        }
    }

    @EventHandler
    private void onRenderBossBar(RenderBossBarEvent.BossIterator event) {
        if (!filterWithers.get()) return;

        List<LerpingBossEvent> filtered = new ArrayList<>();
        event.iterator.forEachRemaining(bar -> {
            if (bar.getName().getString().endsWith("- teleport with /hotspot")) {
                filtered.add(bar);
            }
        });
        event.iterator = filtered.iterator();
    }
}
