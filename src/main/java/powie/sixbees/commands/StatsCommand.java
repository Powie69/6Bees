package powie.sixbees.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;

import static powie.sixbees.utils.stats.PlayerStats.openStatsScreen;
import static powie.sixbees.utils.stats.PlayerStats.sendStatsAsMessage;

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
            // .stats <player>
            .executes(context -> {
                sendStatsAsMessage(StringArgumentType.getString(context, "player"));
                return SINGLE_SUCCESS;
            })
            // .stats <player> all
            .then(literal("all").executes(context -> {
                openStatsScreen(StringArgumentType.getString(context, "player"));
                return SINGLE_SUCCESS;
            }))
        );
    }
}
