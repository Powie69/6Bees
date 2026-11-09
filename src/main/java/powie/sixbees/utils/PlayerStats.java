package powie.sixbees.utils;

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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static powie.sixbees.SixBees.LOG;
import static powie.sixbees.utils.StringUtils.formatNumber;
import static powie.sixbees.utils.StringUtils.toPossessive;

public class PlayerStats {
    private static final String BASE_URL = "https://www.6b6t.org/stats/";

    private static final DateTimeFormatter MEMBER_SINCE_FORMAT = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US);

    private static final DateTimeFormatter FALLBACK_OUTPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Map<String, CompletableFuture<Stats>> IN_FLIGHT_REQUEST = new ConcurrentHashMap<>();

     // jsoup is ~480kb which would ~3x the built jars size, so I used regex as a compromise
     // yes it's ugly.
     // I hope this doesn't bite me in the ass in the future.
    private static final int REGEX_FLAGS = Pattern.DOTALL | Pattern.CASE_INSENSITIVE;
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*([dhms])");
    private static final Pattern H1_PATTERN = Pattern.compile("<h1\\b[^>]*>(.*?)</h1\\s*>", REGEX_FLAGS);
    private static final Pattern ROW_PATTERN = Pattern.compile("<tr\\b[^>]*>(.*?)</tr\\s*>", REGEX_FLAGS);
    private static final Pattern CELL_PATTERN = Pattern.compile("<t[hd]\\b[^>]*>(.*?)</t[hd]\\s*>", REGEX_FLAGS);
    // "Member since" followed by (optional tags / whitespace / comments) and then the date
    private static final Pattern MEMBER_SINCE_PATTERN = Pattern.compile(
        "Member\\s+since(?:\\s|&nbsp;|<[^>]*>)*?(\\d{1,2}/\\d{1,2}/\\d{4})", REGEX_FLAGS);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.DOTALL);
    private static final Pattern ENTITY_PATTERN = Pattern.compile("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(java.time.Duration.ofSeconds(10))
        .executor(EXECUTOR)
        .build();

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

    private static String getStatsPage(@NotNull String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + username))
                .header("User-Agent", "Mozilla/5.0")
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status == 404) {
                throw new IllegalStateException(username + " not found");
            }
            if (status >= 400) {
                throw new IllegalStateException(
                    "Failed to load stats for " + username + " (HTTP " + status + ")");
            }

            return response.body();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load stats page for " + username, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while loading stats page for " + username, e);
        }
    }

    /**
     * Strips comments + tags, decodes entities and collapses whitespace
     */
    private static String text(String html) {
        String s = COMMENT_PATTERN.matcher(html).replaceAll("");
        s = TAG_PATTERN.matcher(s).replaceAll("");
        s = decodeEntities(s);
        return WHITESPACE_PATTERN.matcher(s).replaceAll(" ").trim();
    }

    private static String decodeEntities(String s) {
        if (s.indexOf('&') < 0) return s;

        Matcher m = ENTITY_PATTERN.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String e = m.group(1);
            String replacement = switch (e) {
                case "amp" -> "&";
                case "lt" -> "<";
                case "gt" -> ">";
                case "quot" -> "\"";
                case "apos" -> "'";
                case "nbsp" -> " ";
                default -> {
                    try {
                        if (e.startsWith("#x") || e.startsWith("#X")) {
                            yield new String(Character.toChars(Integer.parseInt(e.substring(2), 16)));
                        } else if (e.startsWith("#")) {
                            yield new String(Character.toChars(Integer.parseInt(e.substring(1))));
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                    yield m.group(); // unknown entity, leave as is
                }
            };
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Stats getStats(@NotNull String username) {
        String html = getStatsPage(username);

        Matcher heading = H1_PATTERN.matcher(html);
        String headingText = heading.find() ? text(heading.group(1)) : "";
        String name = !headingText.isEmpty() ? headingText : username; // because casing

        // label -> [7 days, 30 days, all time]
        Map<String, List<String>> rows = new HashMap<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(html);
        while (rowMatcher.find()) {
            List<String> cells = new ArrayList<>();
            Matcher cellMatcher = CELL_PATTERN.matcher(rowMatcher.group(1));
            while (cellMatcher.find()) cells.add(text(cellMatcher.group(1)));

            if (cells.size() < 4 || cells.getFirst().equalsIgnoreCase("Statistic")) continue;

            rows.put(cells.getFirst().trim().toLowerCase(Locale.ROOT), cells.subList(1, 4));
        }
        if (rows.isEmpty()) throw new IllegalStateException("Statistics table not found for " + username);

        return new Stats(
            name,
            parseMemberSince(html),
            buildPeriod(rows, 0),
            buildPeriod(rows, 1),
            buildPeriod(rows, 2)
        );
    }

    private static LocalDate parseMemberSince(String html) {
        Matcher m = MEMBER_SINCE_PATTERN.matcher(html);
        if (!m.find()) return null;
        try {
            return LocalDate.parse(m.group(1), MEMBER_SINCE_FORMAT);
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
        String text = cell(rows, "playtime", col).trim();
        if (text.startsWith("<")) return Duration.ZERO; // "< 1h" means under an hour, exact value unknown
        Duration total = Duration.ZERO;
        while (m.find()) {
            long amount = Long.parseLong(m.group(1));
            total = total.plus(switch (m.group(2)) {
                case "d" -> Duration.ofDays(amount);
                case "h" -> Duration.ofHours(amount);
                case "m" -> Duration.ofMinutes(amount);
                // minute part might be dead code. keeping just in case there's some edge case
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
//        System.out.println("member since: " + s.prettyMemberSince());
        System.out.println("All time kills: " + s.allTime().kills());
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
