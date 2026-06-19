package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import powie.sixbees.SixBees;
import powie.sixbees.utils.StringUtils;

import java.util.*;

public class AutoWhisper extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgKeyword = settings.createGroup("Keywords");
    private final SettingGroup sgMessageToSend = settings.createGroup("Messages");
    private final SettingGroup sgIgnoredPlayers = settings.createGroup("Ignored players");

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("(Not recommended to modify. only use if you have issues)\nThe cooldown of messages being send in ticks")
        .defaultValue(60) // 3 seconds
        .min(1)
        .sliderMax(100)
        .build()
    );

    // TODO: regex support
    private final Setting<List<String>> keyword = sgKeyword.add(new StringListSetting.Builder()
        .name("keyword")
        .description("The keyword to trigger the whisper.")
        .defaultValue(List.of("kit"))
        .build()
    );

    private final Setting<List<String>> messageToSend = sgMessageToSend.add(new StringListSetting.Builder()
        .name("message")
        .description("The list of possible messages to whisper. (randomly selected)")
        .defaultValue(List.of("Get kits at discord.gg/link"))
        .build()
    );

    private final Setting<List<String>> ignoredPlayers = sgIgnoredPlayers.add(new StringListSetting.Builder()
        .name("ignore-player")
        .description("Will not whisper to these players\nRecommended to add bots")
        .build()
    );

    private int messageCooldown = 0;
    private final Queue<String> messagesQueue = new ArrayDeque<>();

    public AutoWhisper() {
        super(SixBees.CATEGORY, "auto-whisper", "Automatically whispers a message to someone whenever they say a specified keyword");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        return theme.label("Might get you /ignored\nUse responsibly");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        String unparsedMessage = event.getMessage().getString();
        if (!unparsedMessage.contains("»")) return;

        String message = StringUtils.parsePlayerMessage(unparsedMessage).toLowerCase();
        String name = StringUtils.parsePlayerName(unparsedMessage);

        if (ignoredPlayers.get().contains(name) || mc.player.getName().equals(name)) return;
        if (keyword.get().stream()
            .filter(k -> !k.isBlank())
            .map(String::toLowerCase)
            .noneMatch(message::contains)) return;

        if (messageToSend.get().isEmpty()) {
            error("There are no specified messages to send!");
            return;
        }

        String randomMessageToSend = messageToSend.get().get(new Random().nextInt(messageToSend.get().size())).trim();
        messagesQueue.add("/w " + name + " " + randomMessageToSend);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (messageCooldown > 0) {
            messageCooldown--;
            return;
        }
        if (messagesQueue.isEmpty()) return;

        ChatUtils.sendPlayerMsg(Objects.requireNonNull(messagesQueue.poll()));
        messageCooldown = cooldown.get();
    }
}
