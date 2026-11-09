package powie.sixbees.modules;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import powie.sixbees.SixBees;
import powie.sixbees.utils.PlayerStats;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class StatsModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // sgGeneral
    public final Setting<DateFormat> dateFormat = sgGeneral.add(new EnumSetting.Builder<DateFormat>()
        .name("date-format")
        .description("""
            The date format to use
             - DD_MM_YYYY: 27/04/2026
             - MM_DD_YYYY: 04/27/2026
             - MONTH_DD_YYYY: April 27, 2026
             - MON_DD_YYYY: Apr 27, 2026
            """)
        .defaultValue(DateFormat.DD_MM_YYYY)
        .build()
    );

    private final Setting<Boolean> ignoreOffsreenPlayer = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-offscreen-players")
        .description("Won't target players that you can't see")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> chatKeybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("chat-keybind")
        .description("Keybind to show the player's statistics in chat")
        .action(() -> onKeybind(PlayerStats::sendStatsAsMessage))
        .build()
    );

    private final Setting<Keybind> screenKeybind = sgGeneral.add(new KeybindSetting.Builder()
        .name("screen-keybind")
        .description("Keybind to open the player's statistics in a GUI")
        .action(() -> onKeybind(PlayerStats::openStatsScreen))
        .build()
    );

    public StatsModule() {
        super(SixBees.CATEGORY,
            "Stats",
            "Shows statistics of the player nearest to your crosshair",
            "Player Statistics",
            "Statistics");
    }

    private String getTarget() {
        if (!Utils.canUpdate()) return "";

        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 end = start.add(mc.player.getViewVector(1.0f).scale(512));

        Entity target = TargetUtils.get(entity ->
                entity instanceof Player && entity.getBoundingBox().clip(start, end).isPresent(),
            SortPriority.LowestDistance);

        if (target != null) return target.getName().getString();

        target = TargetUtils.get(entity -> {
            if (!(entity instanceof Player player) || entity == mc.player) return false;
            if (ignoreOffsreenPlayer.get()) { // if their feet are offscreen tehey dont get selected
            Vector3d pos = new Vector3d(entity.xo, entity.yo, entity.zo);
                if (!NametagUtils.to2D(pos, 1, false, false)) return false;
                if (pos.x <= 0 || pos.x >= mc.getWindow().getWidth()) return false;
                if (pos.y <= 0 || pos.y >= mc.getWindow().getHeight()) return false;
            }
            return !player.isDeadOrDying() && !(player.getHealth() <= 0);
        }, SortPriority.ClosestAngle);

        if (target == null) return "";
        return target.getName().getString();
    }

    private void onKeybind(Consumer<String> action) {
        if (mc.gui.screen() != null) return;
        String target = getTarget();
        if (target.isEmpty()) {
            error("No Player Found");
            return;
        }
        action.accept(target);
    }

    public enum DateFormat {
        DD_MM_YYYY(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
        MM_DD_YYYY(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
        MONTH_DD_YYYY(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)),
        MON_DD_YYYY(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US));

        private final DateTimeFormatter formatter;

        DateFormat(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        public DateTimeFormatter formatter() {
            return formatter;
        }
    }
}
