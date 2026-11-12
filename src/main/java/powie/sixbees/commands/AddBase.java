package powie.sixbees.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import powie.sixbees.tabs.BaseTab.AddBaseScreen;

public class AddBase extends Command {
    public AddBase() {
        super("add-base", "opens the add base gui");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            Utils.screenToOpen = new AddBaseScreen(null);
            return SINGLE_SUCCESS; // TODO: chat feedback on successful creation
        });
    }
}
