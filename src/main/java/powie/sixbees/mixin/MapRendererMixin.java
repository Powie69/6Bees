package powie.sixbees.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import powie.sixbees.modules.NsfwBlock;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static powie.sixbees.utils.Config.readMaps;

@Mixin(MapRenderer.class)
public abstract class MapRendererMixin {
    @Shadow
    public abstract void render(MapRenderState mapRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean showOnlyFrame, int lightCoords);

    private final Set<Integer> nsfwMaps = readMaps();
    private final Set<MapRenderState> blockedStates = Collections.newSetFromMap(new WeakHashMap<>());

    @Inject(
        method = "extractRenderState",
        at = @At("TAIL")
    )
    private void onExtractRenderState(MapId mapId, MapItemSavedData mapData, MapRenderState state, CallbackInfo ci) {
        if (nsfwMaps.contains(mapId.id())) {
            blockedStates.add(state); // This seems kinda janky if you know any better way please let me know
        } else {
            blockedStates.remove(state);
        }
    }

    @Inject(
        method = "render",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRender(MapRenderState state, PoseStack matrices, SubmitNodeCollector queue, boolean renderDecorations, int light, CallbackInfo ci) {
        NsfwBlock nsfwBlockModule = Modules.get().get(NsfwBlock.class);
        if (nsfwBlockModule == null || !nsfwBlockModule.isActive()) return;
        if (!blockedStates.contains(state)) return;

        ci.cancel();

        if (!nsfwBlockModule.replace.get()) return;

        //vertex stuff
        MultiBufferSource.BufferSource consumers = mc.renderBuffers().bufferSource();

        //layer stuff
        RenderType layer = RenderTypes.text(Identifier.fromNamespaceAndPath("sixbees", "icon.png"));
        VertexConsumer vertexConsumer = consumers.getBuffer(layer);

        Matrix4f matrix4f = matrices.last().pose();

        int overlay = OverlayTexture.NO_OVERLAY;
        int lightU = light & 0xFFFF;
        int lightV = (light >> 16) & 0xFFFF;

        // Draw the custom PNG over the map area
        vertexConsumer.addVertex(matrix4f, 0f, 128f, -0.01f).setUv(0f, 1f).setOverlay(overlay).setUv2(lightU, lightV).setColor(255, 255, 255, 255).setLight(light);
        vertexConsumer.addVertex(matrix4f, 128f, 128f, -0.01f).setUv(1f, 1f).setOverlay(overlay).setUv2(lightU, lightV).setColor(255, 255, 255, 255).setLight(light);
        vertexConsumer.addVertex(matrix4f, 128f, 0f, -0.01f).setUv(1f, 0f).setOverlay(overlay).setUv2(lightU, lightV).setColor(255, 255, 255, 255).setLight(light);
        vertexConsumer.addVertex(matrix4f, 0f, 0f, -0.01f).setUv(0f, 0f).setOverlay(overlay).setUv2(lightU, lightV).setColor(255, 255, 255, 255).setLight(light);
        consumers.endBatch();
    }
}