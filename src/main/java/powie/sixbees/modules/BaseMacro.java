package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;
import org.meteordev.starscript.Script;
import powie.sixbees.SixBees;
import powie.sixbees.tabs.BaseTab;
import powie.sixbees.utils.BaseUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class BaseMacro extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgEnter = this.settings.createGroup("Enter");
    private final SettingGroup sgLeave = this.settings.createGroup("Leave");

    // General
    private final Setting<Integer> checkDelay = sgGeneral.add(new IntSetting.Builder()
        .name("check-delay")
        .description("The delay in ticks to check whether you're inside a base or not")
        .min(1)
        .sliderMax(200)
        .defaultValue(100) // 5 seconds
        .build()
    );

    private final Setting<Integer> intervalDelay = sgGeneral.add(new IntSetting.Builder()
        .name("interval-delay")
        .description("The delay in ticks between each macro execution")
        .min(1)
        .sliderMax(200)
        .defaultValue(20) // 1 second
        .build()
    );

    private final Setting<Boolean> shouldAddToChatHistory = sgGeneral.add(new BoolSetting.Builder()
        .name("add-to-chat-history")
        .description("Whether to add executed commands to chat history")
        .defaultValue(false)
        .build()
    );

    // Enter
    private final List<Script> enterCommandsCompiledList = new ArrayList<>();

    private final Setting<List<String>> enterCommands = sgEnter.add(new StringListSetting.Builder()
        .name("enter-commands")
        .description("The commands to run when you enter a base")
        .defaultValue(List.of("I just entered a base named {sixbees.base}"))
        .onChanged(list -> compileCommands(list, enterCommandsCompiledList))
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );

    // Leave
    private final List<Script> leaveCommandsCompiledList = new ArrayList<>();

    private final Setting<List<String>> leaveCommands = sgLeave.add(new StringListSetting.Builder()
        .name("leave-commands")
        .description("The commands to run when you leave a base")
        .defaultValue(List.of("I just left my base"))
        .onChanged(list -> compileCommands(list, leaveCommandsCompiledList))
        .renderer(StarscriptTextBoxRenderer.class)
        .build()
    );

    private final Queue<Script> commandsToRun = new ArrayDeque<>();

    private int checkDelayAccumulator, intervalDelayAccumulator;
    private boolean isFirstCommand, isRunning, isCurrentlyInsideOfBase;
    private Vec3 lastPlayerPos;

    public BaseMacro() {
        super(SixBees.CATEGORY, "base-macro", "Runs a macro when you enter and leave a base.");
        compileCommands(enterCommands.get(), enterCommandsCompiledList);
        compileCommands(leaveCommands.get(), leaveCommandsCompiledList);
    }

    @Override
    public void onActivate() {
        if (enterCommands.get().isEmpty() && leaveCommands.get().isEmpty()) {
            error("There's no commands to run.");
            toggle();
        }
        removeEmptyCommands();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton button = theme.button("Manage Bases");
        button.action = () -> mc.setScreen(Tabs.get(BaseTab.class).createScreen(GuiThemes.get()));
        return button;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        commandExecution();
        checkBase();
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        lastPlayerPos = mc.player.position();
    }

    private void commandExecution() {
        if (!isRunning) return;
        if (intervalDelayAccumulator <= intervalDelay.get() && !isFirstCommand) {
            intervalDelayAccumulator++;
            return;
        }

        ChatUtils.sendPlayerMsg(MeteorStarscript.run(commandsToRun.poll()), shouldAddToChatHistory.get());
        isFirstCommand = false;
        intervalDelayAccumulator = 0;

        if (commandsToRun.isEmpty()) isRunning = false;
    }

    private void checkBase() {
        if (isRunning) return;
        if (checkDelayAccumulator <= checkDelay.get()) {
            checkDelayAccumulator++;
            return;
        }
        if (lastPlayerPos != null && lastPlayerPos.equals(mc.player.position())) return;
        lastPlayerPos = mc.player.position();
        if (isCurrentlyInsideOfBase) {
            if (!BaseUtils.isInBase()) prepareToRun(leaveCommandsCompiledList);
        } else {
            if (BaseUtils.isInBase()) prepareToRun(enterCommandsCompiledList);
        }
        checkDelayAccumulator = 0;
    }

    private void prepareToRun(List<Script> list) {
        isCurrentlyInsideOfBase = !isCurrentlyInsideOfBase;
        if (list.isEmpty()) return;
        commandsToRun.addAll(list);
        isFirstCommand = true;
        isRunning = true;
    }

    private void removeEmptyCommands() {
        if (enterCommands.get().removeIf(String::isBlank)) warning("Removing empty enter commands");
        if (leaveCommands.get().removeIf(String::isBlank)) warning("Removing empty leave commands");
    }

    private void compileCommands(List<String> rawList, List<Script> compiledList) {
        compiledList.clear();

        for (String s : rawList) {
            try {
                compiledList.add(MeteorStarscript.compile(s));
            } catch (Exception e) {
                error("Failed to compile command: " + s);
                disable();
                return;
            }
        }
    }


}
