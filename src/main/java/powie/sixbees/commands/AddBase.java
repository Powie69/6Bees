package powie.sixbees.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import powie.sixbees.tabs.BaseTab.AddBaseScreen;

import java.util.UUID;

public class AddBase extends Command {
    public AddBase() {
        super("add-base", "opens the add base gui");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            WidgetScreen addBaseScreen = new AddBaseScreen(GuiThemes.get(), null, UUID.randomUUID().toString(), null);
            addBaseScreen.parent = null; // Prevents chat command from staying when pressing ESC
            Utils.screenToOpen = addBaseScreen;
            return SINGLE_SUCCESS; // TODO: chat feedback on successful creation
        });
    }
}
