package powie.sixbees.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import powie.sixbees.screens.PlayerStatsScreen;

public class StatsCommand extends Command {
    public StatsCommand() {
        super("stats", "Shows statistics of a specified player");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("player", StringArgumentType.word())
            .suggests((_, suggestions) -> SharedSuggestionProvider.suggest(
                mc.getConnection().getOnlinePlayers().stream().map(info -> info.getProfile().name()),
                suggestions
            ))
            .executes(context -> {
                WidgetScreen playerStatsScreen = new PlayerStatsScreen(StringArgumentType.getString(context, "player"));
                playerStatsScreen.parent = null; // Prevents chat command from staying when pressing ESC
                Utils.screenToOpen = playerStatsScreen;
                return SINGLE_SUCCESS;
            })
        );
    }
}
