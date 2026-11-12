package powie.sixbees.utils.stats;

import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import powie.sixbees.modules.StatsModule;
import powie.sixbees.screens.PlayerStatsScreen;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static powie.sixbees.SixBees.LOG;
import static powie.sixbees.utils.StringUtils.formatNumber;
import static powie.sixbees.utils.StringUtils.toPossessive;
import static powie.sixbees.utils.stats.FetchStats.EXECUTOR;
import static powie.sixbees.utils.stats.FetchStats.getStats;

public class PlayerStats {
    private static final DateTimeFormatter FALLBACK_OUTPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Map<String, CompletableFuture<Stats>> IN_FLIGHT_REQUEST = new ConcurrentHashMap<>();

    public static void sendStatsAsMessage(@NotNull String username) {
        String chatPrefix = "Player Stats";
        ChatUtils.infoPrefix(chatPrefix, "Loading stats for " + username + "...");
        getStatsAsync(username).thenAccept(Stats -> {
            MutableComponent msg = Component.empty();
            msg.append(Component.literal("\n==============================\n").withStyle(ChatFormatting.YELLOW));
            msg.append(Component.literal(toPossessive(Stats.name()) + " Stats:\n").withStyle(ChatFormatting.YELLOW));
            msg.append(addLine("Playtime: ", Stats.allTime.prettyPlaytime()));
            msg.append(addLine("Join Date: ", Stats.prettyMemberSince()));
            msg.append(addLine("Kills: ", formatNumber(Stats.allTime.kills())));
            msg.append(addLine("Deaths: ", formatNumber(Stats.allTime.deaths())));
            msg.append(addLine("K/D ratio: ", String.valueOf(Stats.allTime.killDeathRatio())));
            msg.append(Component.literal("==============================").withStyle(ChatFormatting.YELLOW));
            ChatUtils.sendMsg(msg);
        }).exceptionally(e -> {
            LOG.error("Failed to load stats for {}", username, e);
            ChatUtils.errorPrefix(chatPrefix, "Failed to load stats for " + username + ": " + errorMessage(e));
            return null;
        });
    }

    private static Component addLine(String label, String value) {
        return Component.empty()
            .append(Component.literal(label).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(value + "\n").withStyle(ChatFormatting.WHITE));
    }

    public static void openStatsScreen(@NotNull String username) {
        WidgetScreen playerStatsScreen = new PlayerStatsScreen(username);
        playerStatsScreen.parent = null;
        Utils.screenToOpen = playerStatsScreen;
    }

    //
    public static String errorMessage(Throwable e) {
        Throwable cause = (e instanceof CompletionException && e.getCause() != null) ? e.getCause() : e;
        String message = cause.getMessage();
        return (message != null && !message.isBlank()) ? message : "Something went wrong";
    }

    private static DateTimeFormatter getOutputFormat() {
        StatsModule module = Modules.get().get(StatsModule.class);
        return module != null ? module.dateFormat.get().formatter() : FALLBACK_OUTPUT_DATE_FORMAT;
    }

    // fetching
    public static CompletableFuture<Stats> getStatsAsync(@NotNull String username) {
        String key = username.toLowerCase();
        CompletableFuture<Stats> future = new CompletableFuture<>();
        CompletableFuture<Stats> existing = IN_FLIGHT_REQUEST.putIfAbsent(key, future);
        if (existing != null) return existing;

        CompletableFuture.supplyAsync(() -> getStats(username), EXECUTOR)
            .whenComplete((result, error) -> {
                IN_FLIGHT_REQUEST.remove(key);
                if (error != null) future.completeExceptionally(error);
                else future.complete(result);
            });

        return future;
    }

    /**
     * for testing
     */
    static void main() {
        Stats s = getStats("Powie69");
        System.out.println(s.name());
//        System.out.println("member since: " + s.prettyMemberSince());
        System.out.println("All time kills: " + s.allTime().kills());
        System.out.println("All time deaths: " + s.allTime().deaths());
        System.out.println("All time K/D: " + s.allTime().killDeathRatio());
        System.out.println("playtime: " + s.allTime().prettyPlaytime());
        System.out.println(s);
    }

    public record Stats(
        String name,
        LocalDate memberSince,
        Period last7Days,
        Period last30Days,
        Period allTime
    ) {
        public String prettyMemberSince() {
            if (memberSince == null) return "Unknown";

            String date = memberSince.format(getOutputFormat());
            long daysBetween = ChronoUnit.DAYS.between(memberSince, LocalDate.now());
            String suffix = switch ((int) daysBetween) {
                case 0 -> " (today)";
                case 1 -> " (1 day ago)";
                default -> " (" + daysBetween + " days ago)";
            };
            return date + suffix;
        }

        public record Period(
            Duration playtime,
            int blocksWalkedKm,
            int blocksFlownKm,
            int kills,
            int deaths,
            int jumps,
            int mobKills,
            int netherrackMined,
            int tntPlaced,
            int endCrystalsPlaced,
            int totemsUsed,
            int cakeSlicesEaten,
            int goldenHoesCrafted,
            int enchantedGoldenApplesEaten,
            int cigarsSmoked,
            int carpetsPlaced
        ) {
            public double killDeathRatio() {
                return deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
            }

            public String prettyPlaytime() {
                if (playtime.isZero()) return "< 1h";
                long days = playtime.toDays();
                long hours = playtime.toHoursPart();
                long minutes = playtime.toMinutesPart();

                StringBuilder sb = new StringBuilder();
                if (days > 0) sb.append(days).append("d ");
                if (hours > 0) sb.append(hours).append("h ");
                if (minutes > 0 || (days == 0 && hours == 0)) sb.append(minutes).append("m");

                return sb.toString().trim();
            }
        }
    }
}
