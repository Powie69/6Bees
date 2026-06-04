package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.game.ItemStackTooltipEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import powie.sixbees.SixBees;

public class ShowMapId extends Module {
    public ShowMapId() {
        super(SixBees.CATEGORY, "show-map-id", "Will always show the map id tooltip");
    }

    @EventHandler
    private void getTooltipData(ItemStackTooltipEvent event) {
        if (event.itemStack().getItem() != Items.FILLED_MAP) return;
        MapId mapIdComponent = event.itemStack().get(DataComponents.MAP_ID);
        if (mapIdComponent != null) event.appendStart(Component.nullToEmpty("#" + mapIdComponent.id()));
    }
}