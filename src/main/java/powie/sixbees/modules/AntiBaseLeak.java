package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import powie.sixbees.SixBees;
import powie.sixbees.tabs.BaseTab;
import powie.sixbees.utils.BaseUtils;

public class AntiBaseLeak extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> preventTpa = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-tpa")
        .description("Prevents you from accepting tpa requests while in your base")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> allowFriendsTpa = sgGeneral.add(new BoolSetting.Builder()
        .name("allow-friends")
        .description("Don't block tpa request coming from friends")
        .defaultValue(true)
        .visible(preventTpa::get)
        .build()
    );

    private final Setting<Boolean> preventHotspot = sgGeneral.add(new BoolSetting.Builder()
        .name("prevent-hotspot-creation")
        .description("Prevents you from creating hotspots while in your base")
        .defaultValue(true)
        .build()
    );

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton button = theme.button("Manage Bases");
        button.action = () -> mc.setScreen(Tabs.get(BaseTab.class).createScreen(GuiThemes.get()));
        return button;
    }

    private String acceptAnyway;
    private boolean createAnyway;

    public AntiBaseLeak() {
        super(SixBees.CATEGORY, "anti-base-leak", "Prevents you from leaking your base");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundChatCommandPacket p)) return;
        String command = p.command().toLowerCase();
        String[] parts = command.split(" ", 3);
        String secondArgument = parts.length > 1 ? parts[1].toLowerCase() : "";

        handlePreventTpa(event, command, secondArgument);

        handlePreventHotspot(event, command, secondArgument);
    }

    private void handlePreventTpa(PacketEvent.Send event, String command, String playerName) {
        if (!preventTpa.get()) return;
        if (!command.startsWith("tpy")) return;
        if (playerName.isEmpty()) return;

        if (allowFriendsTpa.get() && Friends.get().get(playerName) != null) return;
        if (!BaseUtils.isInBase()) return;
        if (mc.getConnection().getPlayerInfoIgnoreCase(playerName) == null) return;

        if (acceptAnyway != null && acceptAnyway.equals("/tpy " + playerName)) {
            acceptAnyway = null;
            return;
        }

        event.cancel();

        MutableComponent warningMessage = Component.literal("Prevented TPA accept from: " + playerName);
        warningMessage.append(getSendAnywayButton("/tpy " + playerName));
        ChatUtils.sendMsg(title, warningMessage);
    }

    private void handlePreventHotspot(PacketEvent.Send event, String command, String secondArgument) {
        if (!preventHotspot.get()) return;
        if (!command.startsWith("hotspot") && !secondArgument.equals("create")) return;
        if (!BaseUtils.isInBase()) return;

        if (createAnyway) {
            createAnyway = false;
            return;
        }

        event.cancel();

        MutableComponent warningMessage = Component.literal("Prevented hotspot creation");
        warningMessage.append(Component.literal(" [CREATE ANYWAY]")
            .withStyle(style -> style
                .applyFormat(ChatFormatting.YELLOW)
                .withClickEvent(new RunnableClickEvent(() -> {
                    createAnyway = true;
                    ChatUtils.sendPlayerMsg("/hotspot create");
                }))
            )
        );
        ChatUtils.sendMsg(title, warningMessage);
    }

    private MutableComponent getSendAnywayButton(String command) {
        return Component.literal(" [ACCEPT ANYWAY]")
            .withStyle(style -> style
                .applyFormat(ChatFormatting.YELLOW)
                .withClickEvent(new RunnableClickEvent(() -> {
                    acceptAnyway = command;
                    ChatUtils.sendPlayerMsg(command);
                }))
            );
    }
}
