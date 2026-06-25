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
import java.util.regex.Pattern;

import static powie.sixbees.utils.Checks.is6B6T;

public class AdBlock extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final BoolSetting filterChat = sgGeneral.add(new BoolSetting.Builder()
        .name("chat")
        .description("Blocks server messages telling you about commands and voting")
        .defaultValue(true)
        .onChanged(_ -> isAllSettingsOff())
        .build()
    );

    private final BoolSetting discordlinks = sgGeneral.add(new BoolSetting.Builder()
        .name("discord-links")
        .description("Blocks discord links sent by players")
        .defaultValue(true)
        .onChanged(_ -> isAllSettingsOff())
        .build()
    );

    private final BoolSetting filterWithers = sgGeneral.add(new BoolSetting.Builder()
        .name("wither-bars")
        .description("Hides wither bars. Except for hotspots")
        .defaultValue(true)
        .onChanged(_ -> isAllSettingsOff())
        .build()
    );

    /**
     * @see <a href="https://regex101.com/r/QNPk9U/1">Source</a>
     */
    private final Pattern discordLinkPattern = Pattern.compile("(https://)?(www\\.)?(((discord(app)?)?\\.com/invite)|((discord(app)?)?\\.gg))/(?<invite>.+)");

    public AdBlock() {
        super(SixBees.CATEGORY, "ad-block", "Blocks ads in various mediums");
    }

    @Override
    public void onActivate() {
        if (!is6B6T()) {
            error("This module is only compatible with 6b6t");
            toggle();
        }

        if (isAllSettingsOff()) toggle();
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        if (!filterChat.get()) return;
        String message = event.getMessage().getString();

        if (message.startsWith("---------------------------")
            || message.startsWith("Please add your email ")) {
            event.cancel();
            return;
        }

        if (discordlinks.get() && discordLinkPattern.matcher(message).find()) event.cancel();
    }

    @EventHandler
    private void onRenderBossBar(RenderBossBarEvent.BossIterator event) {
        if (!filterWithers.get()) return;

        List<LerpingBossEvent> filtered = new ArrayList<>();
        event.iterator.forEachRemaining(bar -> {
            if (bar.getName().getString().endsWith("- teleport with /hotspot")) filtered.add(bar);
        });
        event.iterator = filtered.iterator();
    }

    private boolean isAllSettingsOff() {
        if (!filterChat.get() && !discordlinks.get() && !filterWithers.get()) {
            error("No settings are enabled!");
            return true;
        }
        return false;
    }
}
