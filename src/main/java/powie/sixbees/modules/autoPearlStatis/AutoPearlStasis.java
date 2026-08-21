package powie.sixbees.modules.autoPearlStatis;

import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import powie.sixbees.SixBees;

import java.util.Set;

public class AutoPearlStasis extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTriggers = settings.createGroup("Tiggers");
    //    private final SettingGroup sgHost = settings.createGroup("Host (Main)");
    private final SettingGroup sgPuller = settings.createGroup("Worker (Puller)");

    // General
    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("""
            - Main: The account that has the pearl loaded in the stasis
            - Puller: The account that will pull the pearl
            """)
        .defaultValue(Mode.Main)
        .build()
    );

    private final Setting<Integer> serverPort = sgGeneral.add(new IntSetting.Builder()
        .name("port")
        .description("The network port used for the connection. (so Main and Puller can connect)")
        .defaultValue(67)
        .range(1, 65535)
        .noSlider()
        .build()
    );

    // Triggers
    private final Setting<Keybind> triggerBind = sgTriggers.add(new KeybindSetting.Builder()
        .name("trigger-bind")
        .description("The keybind to manually trigger pearl stasis")
        .visible(() -> mode.get() == Mode.Main)
        .action(() -> sendMessage("pressed bind"))
        .build()
    );

    private final Setting<Integer> health = sgTriggers.add(new IntSetting.Builder()
        .name("health")
        .description("Automatically disconnects when health is lower or equal to this value. Set to 0 to disable.")
        .defaultValue(6)
        .range(0, 19)
        .sliderMax(19)
        .visible(() -> mode.get() == Mode.Main)
        .build()
    );

    private final Setting<Boolean> smart = sgTriggers.add(new BoolSetting.Builder()
        .name("predict-incoming-damage")
        .description("Disconnects when it detects you're about to take enough damage to set you under the 'health' setting.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Main)
        .build()
    );

    private final Setting<Integer> totemPops = sgTriggers.add(new IntSetting.Builder()
        .name("totem-pops")
        .description("Disconnects when you have popped this many totems. Set to 0 to disable.")
        .defaultValue(0)
        .min(0)
        .visible(() -> mode.get() == Mode.Main)
        .build()
    );

    private final Setting<Boolean> onlyTrusted = sgTriggers.add(new BoolSetting.Builder()
        .name("only-trusted")
        .description("Disconnects when a player not on your friends list appears in render distance.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Main)
        .build()
    );

    private final Setting<Set<EntityType<?>>> entities = sgTriggers.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Disconnects when a specified entity is present within a specified range.")
        .visible(() -> mode.get() == Mode.Main)
        .build()
    );

    // Main
//    private final Setting<Boolean> autoReloadPearl = sgHost.add(new BoolSetting.Builder()
//        .name("auto-reload-pearl")
//        .description("Automatically reloads your pearl upon activation")
//        .defaultValue(true)
//        .build()
//    );

    // Puller
    private final Setting<BlockPos> trapdoorPos = sgPuller.add(new BlockPosSetting.Builder()
        .name("trapdoor-position")
        .description("The position of the trapdoor")
        .visible(() -> mode.get() == Mode.Puller)
        .build()
    );

    private HiveHost host;
    private HiveWorker worker;

    private int pops;
    private boolean hasPearlLoaded;

    /**
     * <blockquote>FAREX PULL</blockquote>
     * <cite>dr donut</cite>
     * TODO: auto pearl reload
     */
    public AutoPearlStasis() {
        super(SixBees.CATEGORY,
            "Auto-pearl-stasis",
            "Automatically triggers your pearl stasis you when certain requirements are met.\n2nd account required");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList l = theme.verticalList();

        WHorizontalList b = l.add(theme.horizontalList()).expandX().widget();

        WButton start = b.add(theme.button("start")).expandX().widget();
        start.action = () -> {
            if (!isActive()) return;

            closeConnection();
            if (mode.get() == Mode.Main) host = new HiveHost(serverPort.get());
            else worker = new HiveWorker("localhost", serverPort.get());
        };

        WButton stop = b.add(theme.button("Stop")).expandX().widget();
        stop.action = this::closeConnection;

        WButton guide = l.add(theme.button("Guide")).expandX().widget();
        guide.action = () -> Util.getPlatform().openUri("https://github.com/MeteorDevelopment/meteor-client/wiki/Swarm-Guide");

        return l;
    }

    @Override
    public void toggle() {
        closeConnection();
        super.toggle();
    }

    @Override
    public void onActivate() {
        pops = 0;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundEntityEventPacket p)) return;
        if (p.getEventId() != EntityEvent.PROTECTED_FROM_DEATH) return;

        Entity entity = p.getEntity(mc.level);
        if (entity == null || !entity.equals(mc.player)) return;

        pops++;
        if (totemPops.get() > 0 && pops >= totemPops.get()) sendMessage("Popped " + pops + " totems.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        float playerHealth = mc.player.getHealth();

        if (playerHealth <= health.get()) {
            sendMessage("Health was lower than " + health.get() + ".");
            return;
        }

        if (smart.get() && playerHealth + mc.player.getAbsorptionAmount() - PlayerUtils.possibleHealthReductions() < health.get()) {
            sendMessage("Health was going to be lower than " + health.get() + ".");
            return;
        }

        if (!onlyTrusted.get() && entities.get().isEmpty())
            return; // only check all entities if needed

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player player && player.getUUID() != mc.player.getUUID()) {
                if (onlyTrusted.get() && player != mc.player && !Friends.get().isFriend(player)) {
                    sendMessage("Non-trusted player '" + player.getName().getString() + "' appeared in your render distance.");
                    return;
                }
            } else if (entities.get().contains(entity.getType())) {
                sendMessage(entity.getType().getDescription().getString() + " appeared in your render distance.");
            }
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof ThrownEnderpearl pearl) {
            if (pearl.getOwner().getName().getString().equals(mc.player.getName().getString())
                && Math.abs(Math.round(pearl.getZ()) - trapdoorPos.get().getZ()) <= 3
                && Math.abs(Math.round(pearl.getX()) - trapdoorPos.get().getX()) <= 3) {
                hasPearlLoaded = true;
                info("pearl loaded");
            }
        }
    }

    private void sendMessage(String reason) {
        if (!hasPearlLoaded) return;
        if (host != null) host.sendMessage("pull");
        info("Pulled: " + reason);
        hasPearlLoaded = false;
    }

    public void pullPearl() {
        if (mode.get().equals(Mode.Main)) return;

        if (!(mc.level.getBlockState(trapdoorPos.get()).getBlock() instanceof TrapDoorBlock)) {
            error("selected position is not a trapdoor");
            return;
        }
        if (!PlayerUtils.isWithinReach(trapdoorPos.get())) {
            error("selected position is out of reach");
            return;
        }

        BlockUtils.interact(new BlockHitResult(Utils.vec3(trapdoorPos.get()), Direction.UP, trapdoorPos.get(), false), InteractionHand.MAIN_HAND, true);
    }

    public void closeConnection() {
        try {
            if (host != null) {
                host.disconnect();
                host = null;
            }
            if (worker != null) {
                worker.disconnect();
                worker = null;
            }
        } catch (Exception _) {
        }
    }

    public enum Mode {
        Main,
        Puller
    }
}
