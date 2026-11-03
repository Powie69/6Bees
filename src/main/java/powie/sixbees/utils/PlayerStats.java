package powie.sixbees.utils;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// This is so brainrot
public class PlayerStats {
    private static final String BASE_URL = "https://www.6b6t.org/en/stats/";
    private static final DateTimeFormatter MEMBER_SINCE_FORMAT =
        DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US);
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{4}");
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*([dhms])");
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    public static CompletableFuture<Stats> getStatsAsync(@NotNull String username) {
        return CompletableFuture.supplyAsync(() -> getStats(username), EXECUTOR);
    }

    public static void sendStatsAsMessage(@NotNull String username) {
        String MessagePrefix = "Player Stats";
        getStatsAsync(username).thenAccept(Stats -> {

        }).exceptionally(e -> {
            ChatUtils.error(MessagePrefix, "Failed to load stats for " + username + ": " + e.getMessage());
            return null;
        });

    }

    // TODO: try my best to make response size smaller
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

        LocalDate memberSince = parseMemberSince(doc);

        // label -> [7 days, 30 days, all time]
        Map<String, List<String>> rows = new HashMap<>();
        for (Element row : doc.select("table tr")) {
            List<String> cells = row.select("th, td").eachText();
            if (cells.size() < 4 || cells.getFirst().equalsIgnoreCase("Statistic")) continue;

            rows.put(cells.getFirst().trim().toLowerCase(Locale.ROOT), cells.subList(1, 4));
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("Statistics table not found for " + username);
        }

        return new Stats(
            name,
            memberSince,
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
            (long) number(rows, "kills", col),
            (long) number(rows, "deaths", col),
            (long) number(rows, "jumps", col),
            (long) number(rows, "mob kills", col),
            (long) number(rows, "netherrack mined", col),
            (long) number(rows, "tnt placed", col),
            (long) number(rows, "end crystals placed", col),
            (long) number(rows, "totems used", col),
            (long) number(rows, "cake slices eaten", col),
            (long) number(rows, "golden hoes crafted", col),
            (long) number(rows, "enchanted golden apples eaten", col),
            (long) number(rows, "cigars smoked", col),
            (long) number(rows, "carpets placed", col)
        );
    }

    private static String cell(Map<String, List<String>> rows, String label, int col) {
        List<String> values = rows.get(label);
        return values == null ? "" : values.get(col);
    }

    /**
     * "2,966km" -> 2966.0, "1,524" -> 1524.0; missing/unparseable -> 0.
     */
    private static double number(Map<String, List<String>> rows, String label, int col) {
        String cleaned = cell(rows, label, col).replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return 0;
        try {
            return Double.parseDouble(cleaned);
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
     * testing only. Don't use in prod.
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

            long daysBetween = ChronoUnit.DAYS.between(memberSince, LocalDate.now());

            if (daysBetween == 0) return memberSince + " (today)";
            if (daysBetween == 1) return memberSince + " (1 day ago)";
            return memberSince + " (" + daysBetween + " days ago)";
        }

        public record Period(
            Duration playtime,
            double blocksWalkedKm,
            double blocksFlownKm,
            long kills,
            long deaths,
            long jumps,
            long mobKills,
            long netherrackMined,
            long tntPlaced,
            long endCrystalsPlaced,
            long totemsUsed,
            long cakeSlicesEaten,
            long goldenHoesCrafted,
            long enchantedGoldenApplesEaten,
            long cigarsSmoked,
            long carpetsPlaced
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
                if (hours > 0 || days > 0) sb.append(hours).append("h ");
                if (minutes > 0 || (days == 0 && hours == 0)) sb.append(minutes).append("m");

                return sb.toString().trim();
            }
        }
    }
}
