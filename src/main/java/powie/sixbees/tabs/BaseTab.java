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
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import powie.sixbees.utils.BaseUtils;
import powie.sixbees.utils.BaseUtils.Base;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static powie.sixbees.utils.Config.readBases;

public class BaseTab extends Tab {
    public BaseTab() {
        super("Bases");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new BaseTabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof BaseTabScreen;
    }

    private static class BaseTabScreen extends WindowTabScreen {
        private Map<String, Base> bases;

        public BaseTabScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
            this.bases = readBases();
        }

        @Override
        public void initWidgets() {
            WTable table = add(theme.table()).expandX().minWidth(400).widget();
            initTable(table);

            add(theme.horizontalSeparator()).expandX();

            WButton addNew = add(theme.button("Add new base")).expandX().widget();
            addNew.action = () -> mc.setScreen(new AddBaseScreen(theme, null, UUID.randomUUID().toString(), this));
        }

        private void initTable(WTable table) {
            table.clear();

            if (bases.isEmpty()) {
                table.add(theme.label("No bases yet")).expandX();
                return;
            }

            bases.entrySet().stream()
                .sorted(Comparator.comparing(v -> v.getValue().name))
                .forEach(entry -> {
                    String BaseKey = entry.getKey();
                    BaseUtils.Base baseValue = entry.getValue();

                    table.add(theme.label(baseValue.name));

                    WButton edit = table.add(theme.button(GuiRenderer.EDIT)).expandCellX().right().widget();
                    edit.action = () -> {
                        mc.setScreen(new AddBaseScreen(theme, baseValue, BaseKey, this));
                    };
                    WConfirmedMinus delete = table.add(theme.confirmedMinus()).right().widget();
                    delete.action = () -> {
                        BaseUtils.removeBase(BaseKey);
                        reload();
                    };

                    table.row();
                });
        }

        @Override
        public void reload() {
            bases = readBases();
            clear();
            initWidgets();
        }
    }

    private static class AddBaseScreen extends WindowScreen {
        private final AddBaseSettings settings;
        private final boolean isEdit;
        private final Base base;
        private final String baseId;
        private final BaseTabScreen parent;

        public AddBaseScreen(GuiTheme theme, Base base, String baseId, BaseTabScreen parent) {
            super(theme, base != null ? "Edit \"" + base.name + "\"" : "New Place");
            this.isEdit = base != null;
            this.parent = parent;
            this.base = base;
            this.baseId = baseId;
            settings = new AddBaseSettings();
        }

        @Override
        public void initWidgets() {
            if (isEdit) {
                settings.name.set(this.base.name);
                settings.coords.set(this.base.coords);
                settings.radius.set(this.base.radius);
                settings.dimension.set(this.base.dimension);
            } else if (mc.player != null) {
                settings.coords.set(new BlockPos(mc.player.blockPosition()));
                settings.dimension.set(PlayerUtils.getDimension());
            }

            add(theme.settings(settings.settings)).expandX();

            // action buttons
            add(theme.horizontalSeparator()).expandX();
            WHorizontalList actionButtons = add(theme.horizontalList()).expandX().widget();
            WButton saveBtn = actionButtons.add(theme.button(isEdit ? "Update" : "Create")).expandX().widget();
            saveBtn.action = this::saveCoords;

            WButton cancelBtn = actionButtons.add(theme.button("Cancel")).expandX().widget();
            cancelBtn.action = () -> mc.setScreen(parent);
        }

        private void saveCoords() {
            BaseUtils.saveBase(baseId,
                new Base(settings.name.get().trim(), settings.coords.get(), settings.radius.get(), settings.dimension.get()));
            parent.reload();
            mc.setScreen(parent);
        }
    }

    public static class AddBaseSettings {
        public final Settings settings = new Settings();
        private final SettingGroup sgGeneral = settings.getDefaultGroup();

        private final Setting<String> name = sgGeneral.add(new StringSetting.Builder()
            .name("name")
            .description("The name of the location.")
            .placeholder("My secret base")
            .filter((text, _) -> (text.length() < 50))
            .build()
        );

        private final Setting<BlockPos> coords = sgGeneral.add(new BlockPosSetting.Builder()
            .name("coordinates")
            .description("The coordinates of the location.")
            .build()
        );

        private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
            .name("radius")
            .description("The radius of the location.")
            .defaultValue(10000)
            .noSlider()
            .min(1)
            .max(Integer.MAX_VALUE)
            .build()
        );

        public Setting<Dimension> dimension = sgGeneral.add(new EnumSetting.Builder<Dimension>()
            .name("dimension")
            .description("Which dimension the location is in.")
            .defaultValue(Dimension.Overworld)
            .build()
        );
    }
}
