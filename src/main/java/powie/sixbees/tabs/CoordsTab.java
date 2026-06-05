package powie.sixbees.tabs;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import powie.sixbees.utils.Config;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static powie.sixbees.SixBees.LOG;
import static powie.sixbees.utils.Config.*;

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
            addNew.action = () -> mc.setScreen(new AddCoordsScreen(theme, false, this));
        }

        private void initTable(WTable table) {
            table.clear();

            for (Config.Place place : readPlaces()) {
                table.add(theme.label(place.name));

                WButton edit = table.add(theme.button(GuiRenderer.EDIT)).expandCellX().right().widget();
                edit.action = () -> mc.setScreen(new AddCoordsScreen(theme, true, this));

                WConfirmedMinus delete = table.add(theme.confirmedMinus()).right().widget();
                delete.action = () -> {
                    removePlace(place.name);
                    reload();
                };

                table.row();
            }
        }

//        @Override
//        public boolean shouldCloseOnEsc() {
//            return true;
//        }
    }

    private static class AddCoordsScreen extends WindowScreen {
        private final CoordsSettings coordsSettings;
        private final boolean isEdit;
        private final Screen parent;

        public AddCoordsScreen(GuiTheme theme, boolean isEdit, Screen parent) {
            super(theme, isEdit ? "Edit" : "New Place");
            this.isEdit = isEdit;
            this.parent = parent;
            coordsSettings = new CoordsSettings();
        }

        @Override
        public void initWidgets() {
            add(theme.settings(coordsSettings.settings)).expandX();

            // Save button
            add(theme.horizontalSeparator()).expandX();
            WHorizontalList actionButtons = add(theme.horizontalList()).expandX().widget();
            WButton saveBtn = actionButtons.add(theme.button(isEdit ? "Update" : "Create")).expandX().widget();
            if (isEdit) {
                WButton cancelBtn = actionButtons.add(theme.button("Cancel")).expandX().widget();
                cancelBtn.action = () -> mc.setScreen(parent);
            }
            saveBtn.action = this::saveCoords;
        }

        private void saveCoords() {
            LOG.info(coordsSettings.name.get());
            savePlace(new Config.Place(coordsSettings.name.get(), coordsSettings.coords.get(), coordsSettings.radius.get(), coordsSettings.dimension.get()));
            reload();
            mc.setScreen(parent);
        }
    }

    public static class CoordsSettings {
        public final Settings settings = new Settings();
        private final SettingGroup sgGeneral = settings.getDefaultGroup();

        private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("The name of the location.")
            .placeholder("My secret base")
            .build()
        );

        private final Setting<BlockPos> coords = sgGeneral.add(new BlockPosSetting.Builder()
            .name("coordinates")
            .description("The coordinates of the location.")
            .build()
        );

        private final Setting<Double> radius = sgGeneral.add(new DoubleSetting.Builder()
            .name("radius")
            .description("The radius of the location.")
            .defaultValue(10000)
            .noSlider()
            .min(1)
            .max(Double.POSITIVE_INFINITY)
            .build()
        );

        public Setting<Dimension> dimension = sgGeneral.add(new EnumSetting.Builder<Dimension>()
            .name("dimension")
            .description("Which dimension the base is in.")
            .defaultValue(Dimension.Overworld)
            .build()
        );
    }
}
