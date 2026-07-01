package powie.sixbees.modules;

import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
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
        WVerticalList l = theme.verticalList();
        WButton button = l.add(theme.button("Manage Bases")).widget();
        button.action = () -> mc.setScreen(Tabs.get(BaseTab.class).createScreen(GuiThemes.get()));
        return button;
    }

    public AntiBaseLeak() {
        super(SixBees.CATEGORY, "anti-base-leak", "Prevents you from leaking your base");
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundChatCommandPacket p)) return;
        String command = p.command().toLowerCase();
        String[] parts = command.split(" ");
        String secondArgument = parts.length > 1 ? parts[1].toLowerCase() : "";

        if (preventTpa.get() && command.startsWith("tpy") && !secondArgument.isEmpty()) {
            if (allowFriendsTpa.get() && Friends.get().get(secondArgument) != null) return;
            if (!BaseUtils.isInBase()) return;
            // Don't print message if player ins't valid (not online). In my opinion it's bad ux
            if (mc.getConnection().getPlayerInfoIgnoreCase(secondArgument) == null) return;
            event.cancel();
            MutableComponent warningMessage = Component.literal("Prevented tpa accept from: " + secondArgument);
            warningMessage.append(getSendButton("/tpy" + secondArgument));
            ChatUtils.sendMsg(warningMessage);

//            info("Prevented tpa accept from " + secondArgument);

        }

        if (preventHotspot.get()
            && command.startsWith("hotspot")
            && secondArgument.equals("create")
            && BaseUtils.isInBase()) {
            event.cancel();
            info("Prevented hotspot creation");
        }
    }

    private MutableComponent getSendButton(String message) {
        MutableComponent sendButton = Component.literal("[ACCEPT ANYWAY]");

        sendButton.setStyle(sendButton.getStyle()
            .applyFormat(ChatFormatting.YELLOW)
            .withClickEvent(new MeteorClickEvent(Commands.get("say").toString(message))));
        return sendButton;
    }

}
