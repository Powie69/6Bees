package powie.sixbees.screens;

import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import powie.sixbees.utils.PlayerStats;
import powie.sixbees.utils.PlayerStats.Stats.Period;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static powie.sixbees.utils.StringUtils.formatNumber;
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
        overviewTable.horizontalSpacing = 30;
        WSection more = add(theme.section("More")).expandX().widget(); // naming could be better. Please give suggestions
        WTable moreTable = more.add(theme.table()).widget();
        moreTable.horizontalSpacing = 30;

        PlayerStats.getStatsAsync(username).thenAccept(Stats -> { // TODO: Test if mc.execute matters
            mc.execute(() -> {
                infoTable.clear();
                fillOverviewTable(overviewTable, Stats);
                fillMoreTable(moreTable, Stats);
            });
        }).exceptionally(e -> {
            mc.execute(() -> fillInfoTable(infoTable, PlayerStats.errorMessage(e)));
            return null;
        });

//        add(theme.horizontalSeparator()).expandX();
        WHorizontalList footer = add(theme.horizontalList()).expandX().widget();
        footer.add(theme.label("6Bees Addon").color(new Color(255, 206, 43))).padLeft(6);
        footer.add(theme.label("Press ESC to close")).expandCellX().right();
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
        table.add(theme.label("Join Date: " + stats.prettyMemberSince()));
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
        table.add(theme.label("7 Days")).right();
        table.add(theme.label("30 Days")).right();
        table.add(theme.label("All time")).right();
        table.row();

        List<Period> periods = List.of(stats.last7Days(), stats.last30Days(), stats.allTime());

        addRow(table, "Playtime", periods, Period::prettyPlaytime);
        addNumberRow(table, "Blocks Walked (km)", periods, Period::blocksWalkedKm);
        addNumberRow(table, "Blocks Flown (km)", periods, Period::blocksFlownKm);
        addNumberRow(table, "Kills", periods, Period::kills);
        addNumberRow(table, "Deaths", periods, Period::deaths);
        addNumberRow(table, "Jumps", periods, Period::jumps);
        addNumberRow(table, "Mob Kills", periods, Period::mobKills);
        addNumberRow(table, "Netherrack Mined", periods, Period::netherrackMined);
        addNumberRow(table, "TNT Placed", periods, Period::tntPlaced);
        addNumberRow(table, "End Crystals Placed", periods, Period::endCrystalsPlaced);
        addNumberRow(table, "Totems Used", periods, Period::totemsUsed);
        addNumberRow(table, "Cake Slices Eaten", periods, Period::cakeSlicesEaten);
        addNumberRow(table, "Golden Hoes Crafted", periods, Period::goldenHoesCrafted);
        addNumberRow(table, "Enchanted Golden Apples Eaten", periods, Period::enchantedGoldenApplesEaten);
        addNumberRow(table, "Cigars Smoked", periods, Period::cigarsSmoked);
        addNumberRow(table, "Carpets Placed", periods, Period::carpetsPlaced);

        table.add(theme.horizontalSeparator()).expandX();
    }

    private void addNumberRow(WTable table, String label, List<Period> periods, ToIntFunction<Period> value) {
        addRow(table,
            label,
            periods,
            p -> formatNumber(value.applyAsInt(p)));
    }

    private void addRow(WTable table, String label, List<Period> periods, Function<Period, String> value) {
        table.add(theme.label(label));
        for (Period p : periods) {
            table.add(theme.label(value.apply(p))).right();
        }
        table.row();
    }

}
