package powie.sixbees.screens;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import powie.sixbees.utils.PlayerStats;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static powie.sixbees.utils.StringUtils.toPossessive;

public class PlayerStatsScreen extends WindowScreen {
    private final String username;

    public PlayerStatsScreen(String username) {
        super(GuiThemes.get(),
            toPossessive(username) + " Stats");

        this.username = username;
    }

    @Override
    public void initWidgets() {
        WTable infoTable = add(theme.table()).widget();
        infoTable.add(theme.label("Loading stats..."));

        WSection overview = add(theme.section("Overview")).expandX().widget();
        WTable overviewTable = overview.add(theme.table()).widget();
        WSection more = add(theme.section("More")).expandX().widget(); // naming could be better. Please give suggestions
        WTable moreTable = more.add(theme.table()).widget();

        PlayerStats.getStatsAsync(username).thenAccept(Stats -> {
            mc.execute(() -> {
                infoTable.clear();
                fillOverviewTable(overviewTable, Stats);
                fillMoreTable(moreTable, Stats);
            });
        }).exceptionally(e -> {
            mc.execute(() -> fillInfoTable(infoTable, e.getCause().getMessage()));
//            mc.execute(() -> fillTable(infoTable, null, true, e.getMessage()));
            return null;
        });
    }

    private void fillInfoTable(WTable table, String errorText) {
        table.clear();
        table.add(theme.label("Failed to load stats:"));
        table.row();
        table.add(theme.label(errorText));
    }

    private void fillOverviewTable(WTable table, PlayerStats.Stats stats) {
        table.clear();
        table.add(theme.label("Playtime: " + stats.allTime().prettyPlaytime()));
        table.row();
        table.add(theme.label("Member since: " + stats.prettyMemberSince()));
        table.row();
        table.add(theme.label("Kills: " + stats.allTime().kills()));
        table.row();
        table.add(theme.label("Deaths: " + stats.allTime().deaths()));
        table.row();
        table.add(theme.label("K/D Ratio: " + stats.allTime().killDeathRatio()));
        table.row();
    }

    private void fillMoreTable(WTable table, PlayerStats.Stats stats) {
        table.clear();
        table.add(theme.label(""));
        table.add(theme.label("7 Days")).right().padLeft(8);
        table.add(theme.label("30 Days")).right().padLeft(8);
        table.add(theme.label("All time")).right().padLeft(8);
        table.row();

        // im so sorry for this slop
        addStatRow(table, "Playtime", stats.last7Days().prettyPlaytime(), stats.last30Days().prettyPlaytime(), stats.allTime().prettyPlaytime());
        addStatRow(table, "Blocks Walked (km)", stats.last7Days().blocksWalkedKm(), stats.last30Days().blocksWalkedKm(), stats.allTime().blocksWalkedKm());
        addStatRow(table, "Blocks Flown (km)", stats.last7Days().blocksFlownKm(), stats.last30Days().blocksFlownKm(), stats.allTime().blocksFlownKm());
        addStatRow(table, "Kills", stats.last7Days().kills(), stats.last30Days().kills(), stats.allTime().kills());
        addStatRow(table, "Deaths", stats.last7Days().deaths(), stats.last30Days().deaths(), stats.allTime().deaths());
        addStatRow(table, "Jumps", stats.last7Days().jumps(), stats.last30Days().jumps(), stats.allTime().jumps());
        addStatRow(table, "Mob Kills", stats.last7Days().mobKills(), stats.last30Days().mobKills(), stats.allTime().mobKills());
        addStatRow(table, "Netherrack Mined", stats.last7Days().netherrackMined(), stats.last30Days().netherrackMined(), stats.allTime().netherrackMined());
        addStatRow(table, "TNT Placed", stats.last7Days().tntPlaced(), stats.last30Days().tntPlaced(), stats.allTime().tntPlaced());
        addStatRow(table, "End Crystals Placed", stats.last7Days().endCrystalsPlaced(), stats.last30Days().endCrystalsPlaced(), stats.allTime().endCrystalsPlaced());
        addStatRow(table, "Totems Used", stats.last7Days().totemsUsed(), stats.last30Days().totemsUsed(), stats.allTime().totemsUsed());
        addStatRow(table, "Cake Slices Eaten", stats.last7Days().cakeSlicesEaten(), stats.last30Days().cakeSlicesEaten(), stats.allTime().cakeSlicesEaten());
        addStatRow(table, "Golden Hoes Crafted", stats.last7Days().goldenHoesCrafted(), stats.last30Days().goldenHoesCrafted(), stats.allTime().goldenHoesCrafted());
        addStatRow(table, "Enchanted Golden Apples Eaten", stats.last7Days().enchantedGoldenApplesEaten(), stats.last30Days().enchantedGoldenApplesEaten(), stats.allTime().enchantedGoldenApplesEaten());
        addStatRow(table, "Cigars Smoked", stats.last7Days().cigarsSmoked(), stats.last30Days().cigarsSmoked(), stats.allTime().cigarsSmoked());
        addStatRow(table, "Carpets Placed", stats.last7Days().carpetsPlaced(), stats.last30Days().carpetsPlaced(), stats.allTime().carpetsPlaced());
    }

    private void addStatRow(WTable table, String label, Object value7d, Object value30d, Object valueAll) {
        table.add(theme.label(label));
        table.add(theme.label(String.valueOf(value7d))).right().padLeft(8);
        table.add(theme.label(String.valueOf(value30d))).right().padLeft(8);
        table.add(theme.label(String.valueOf(valueAll))).right().padLeft(8);
        table.row();
    }

}
