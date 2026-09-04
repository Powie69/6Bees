package powie.sixbees.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import powie.sixbees.modules.NsfwBlock;

@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @Inject(
        method = "extractRenderState",
        at = @At("TAIL")
    )
    private void onExtractRenderState(MapId mapId, MapItemSavedData mapData, MapRenderState state, CallbackInfo ci) {
        NsfwBlock nsfwBlockModule = Modules.get().get(NsfwBlock.class);
        if (nsfwBlockModule == null || !nsfwBlockModule.isActive()) return;
        if (!NsfwBlock.NSFW_MAPS.get().contains(mapId.id())) return;
        if (!nsfwBlockModule.replace.get()) state.texture = Identifier.fromNamespaceAndPath("sixbees", "transparent.png");
        else state.texture = Identifier.fromNamespaceAndPath("sixbees", "icon.png");
    }
}
