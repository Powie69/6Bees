package powie.sixbees.tabs;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screens.Screen;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static powie.sixbees.SixBees.LOG;

public class CoordsTab extends Tab {

    public CoordsTab() {
        super("Coords");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new CoordsTabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof CoordsTabScreen;
    }

    private static class CoordsTabScreen extends WindowTabScreen {
        public CoordsTabScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
        }

        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().minWidth(400).widget();
            initTable(table);

            add(theme.horizontalSeparator()).expandX();

            WButton addNew = add(theme.button("Add new place")).expandX().widget();
            addNew.action = () -> mc.setScreen(new AddCoordsScreen(theme));
        }

        private void initTable(WTable table) {
            table.clear();
        }

//        @Override
//        public boolean shouldCloseOnEsc() {
//            return true;
//        }
    }

    private static class AddCoordsScreen extends WindowScreen {
        private WIntEdit xInput;
        private WIntEdit zInput;
        private WIntEdit radiusInput;

        public AddCoordsScreen(GuiTheme theme) {
            super(theme, "Add Coords");
        }

        @Override
        public void initWidgets() {

            WHorizontalList axisList = add(theme.horizontalList()).minWidth(350).expandX().widget();
            WVerticalList list = add(theme.verticalList()).minWidth(350).expandX().widget();
            // X coordinate
            axisList.add(theme.label("X:"));
            xInput = axisList.add(theme.intEdit(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0, true)).expandX().widget();

            // Z coordinate
            axisList.add(theme.label("Z:"));
            zInput = axisList.add(theme.intEdit(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0, true)).expandX().widget();

            // Radius
            list.add(theme.label("Radius:"));

            radiusInput = list.add(theme.intEdit(10000, Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0, true)).expandX().widget();

            // Save button
            add(theme.horizontalSeparator()).expandX();
            WButton saveBtn = add(theme.button("Save")).expandX().widget();
            saveBtn.action = () -> {
                saveCoords();
            };
        }

        private void saveCoords() {
            // TODO: Implement saving to coords file
            int x = xInput.get();
            int z = zInput.get();
            int radius = radiusInput.get();
            LOG.info("Saving coords: X=" + x + ", Z=" + z + ", Radius=" + radius);
        }
    }

    public static class CoordsLocation {
        public int x;
        public int z;
        public int radius;

        public CoordsLocation(int x, int z, int radius) {
            this.x = x;
            this.z = z;
            this.radius = radius;
        }
    }
}
