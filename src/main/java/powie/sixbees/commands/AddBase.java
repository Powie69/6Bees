package powie.sixbees.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.GuiThemes;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import powie.sixbees.tabs.BaseTab;

import java.util.UUID;

public class AddBase extends Command {
    public AddBase() {
        super("add-base", "opens the add base gui");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
//            BaseTab.BaseTabScreen parentScreen = (BaseTab.BaseTabScreen) Tabs.get(BaseTab.class).createScreen(GuiThemes.get());
            info("Opening add base gui");
            mc.setScreen(
                new BaseTab.AddBaseScreen(GuiThemes.get(), null, UUID.randomUUID().toString(), null));
            return SINGLE_SUCCESS;
        });


    }
}
