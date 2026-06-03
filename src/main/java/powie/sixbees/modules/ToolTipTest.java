package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.events.render.TooltipDataEvent;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import powie.sixbees.SixBees;

public class ToolTipTest extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    public ToolTipTest() {
        super(SixBees.CATEGORY, "tool-tip-test", "test");
    }

    /**
     * Example event handling method.
     * Requires {@link SixBees#getPackage()} to be setup correctly, otherwise the game will crash whenever the module is enabled.
     */
    @EventHandler
    private void getTooltipData(ItemStackTooltipEvent event) {
        if (event.itemStack().getItem() == Items.FILLED_MAP) {
            MapId mapIdComponent = event.itemStack().get(DataComponents.MAP_ID);
            if (mapIdComponent != null) event.appendStart(Component.nullToEmpty("#" + mapIdComponent.id()));

        }
    }

}
