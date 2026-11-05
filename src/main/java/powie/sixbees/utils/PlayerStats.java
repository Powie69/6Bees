package powie.sixbees.utils;

import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import powie.sixbees.modules.StatsModule;
import powie.sixbees.screens.PlayerStatsScreen;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static powie.sixbees.utils.StringUtils.formatNumber;
import static powie.sixbees.utils.StringUtils.toPossessive;

// This is so brainrot
// TODO: unslop
public class PlayerStats {
    private static final String BASE_URL = "https://www.6b6t.org/stats/";

    private static final DateTimeFormatter MEMBER_SINCE_FORMAT = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{4}");
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*([dhms])");

    private static final DateTimeFormatter FALLBACK_OUTPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

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
        return CompletableFuture.supplyAsync(() -> getStats(username), EXECUTOR);
    }

    private static Document getStatsPage(@NotNull String username) {
        try {
            Connection.Response response = Jsoup.connect(BASE_URL + username)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .ignoreHttpErrors(true)
                .execute();

            int status = response.statusCode();
            if (status == 404) {
                throw new IllegalStateException(username + " not found");
            }
            if (status >= 400) {
                throw new IllegalStateException(
                    "Failed to load stats for " + username + " (HTTP " + status + ")");
            }

            return response.parse();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load stats page for " + username, e);
        }
    }

    /**
     * I let Claude do all the CSS queries
     */
    public static Stats getStats(@NotNull String username) {
        Document doc = getStatsPage(username);

        Element heading = doc.selectFirst("h1");
        String name = heading != null ? heading.text().trim() : username; // because casing

        // label -> [7 days, 30 days, all time]
        Map<String, List<String>> rows = new HashMap<>();
        for (Element row : doc.select("table tr")) {
            List<String> cells = row.select("th, td").eachText();
            if (cells.size() < 4 || cells.getFirst().equalsIgnoreCase("Statistic")) continue;

            rows.put(cells.getFirst().trim().toLowerCase(Locale.ROOT), cells.subList(1, 4));
        }
        if (rows.isEmpty()) throw new IllegalStateException("Statistics table not found for " + username);

        return new Stats(
            name,
            parseMemberSince(doc),
            buildPeriod(rows, 0),
            buildPeriod(rows, 1),
            buildPeriod(rows, 2)
        );
    }

    private static LocalDate parseMemberSince(Document doc) {
        Element el = doc.selectFirst(":containsOwn(Member since)");
        if (el == null) return null;

        Matcher m = DATE_PATTERN.matcher(el.text());
        if (!m.find()) return null;
        try {
            return LocalDate.parse(m.group(), MEMBER_SINCE_FORMAT);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static Stats.Period buildPeriod(Map<String, List<String>> rows, int col) {
        return new Stats.Period(
            duration(rows, col),
            number(rows, "blocks walked", col),
            number(rows, "blocks flown", col),
            number(rows, "kills", col),
            number(rows, "deaths", col),
            number(rows, "jumps", col),
            number(rows, "mob kills", col),
            number(rows, "netherrack mined", col),
            number(rows, "tnt placed", col),
            number(rows, "end crystals placed", col),
            number(rows, "totems used", col),
            number(rows, "cake slices eaten", col),
            number(rows, "golden hoes crafted", col),
            number(rows, "enchanted golden apples eaten", col),
            number(rows, "cigars smoked", col),
            number(rows, "carpets placed", col)
        );
    }

    private static String cell(Map<String, List<String>> rows, String label, int col) {
        List<String> values = rows.get(label);
        return values == null ? "" : values.get(col);
    }

    /**
     * "2,966km" -> 2966, "1,524" -> 1524; missing/unparseable -> 0.
     */
    private static int number(Map<String, List<String>> rows, String label, int col) {
        String cleaned = cell(rows, label, col).replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return 0;
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * "3d 10h" -> 3 days + 10 hours; "12h" -> 12 hours.
     */
    private static Duration duration(Map<String, List<String>> rows, int col) {
        Matcher m = DURATION_PART.matcher(cell(rows, "playtime", col));
        Duration total = Duration.ZERO;
        while (m.find()) {
            long amount = Long.parseLong(m.group(1));
            total = total.plus(switch (m.group(2)) {
                case "d" -> Duration.ofDays(amount);
                case "h" -> Duration.ofHours(amount);
                case "m" -> Duration.ofMinutes(amount);
                default -> Duration.ofSeconds(amount);
            });
        }
        return total;
    }

    /**
     * for testing
     */
    static void main() {
        Stats s = getStats("Powie69");
        System.out.println(s.name());
        System.out.println("member since: " + s.prettyMemberSince());
        System.out.println("All time kills: " + s.allTime().kills());
        System.out.println("All time K/D: " + s.allTime().killDeathRatio());
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

            DateTimeFormatter format = getOutputFormat();
            long daysBetween = ChronoUnit.DAYS.between(memberSince, LocalDate.now());

            if (daysBetween == 0) return memberSince.format(format) + " (today)";
            if (daysBetween == 1) return memberSince.format(format) + " (1 day ago)";
            return memberSince.format(format) + " (" + daysBetween + " days ago)";
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
