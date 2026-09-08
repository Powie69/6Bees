package powie.sixbees.modules;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.joml.Vector3d;
import powie.sixbees.SixBees;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MapIdNametags extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgColor = settings.createGroup("Color");

    // General
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("The scale of the nametag.")
        .defaultValue(1.25)
        .min(0.1)
        .build()
    );

    private final Setting<Boolean> canOnlySee = sgGeneral.add(new BoolSetting.Builder()
        .name("Render obstructed")
        .description("Render nametags when the item frame cannot be seen (e.g. behind blocks).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder()
        .name("culling")
        .description("Only render a certain number of nametags at a certain distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxCullRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("culling-range")
        .description("Only render nametags within this distance of your player.")
        .defaultValue(100)
        .min(0)
        .sliderMax(200)
        .visible(culling::get)
        .build()
    );

    private final Setting<Integer> maxCullCount = sgGeneral.add(new IntSetting.Builder()
        .name("culling-count")
        .description("Only render this many nametags.")
        .defaultValue(200)
        .min(1)
        .sliderRange(1, 200)
        .visible(culling::get)
        .build()
    );

    private final Setting<Boolean> highlightIfBlocked = sgGeneral.add(new BoolSetting.Builder()
        .name("hightlight-if-nsfw-map")
        .description("Highlights the nametag of a map if it is blocked.")
        .defaultValue(true)
        .build()
    );

    // Color
    private final Setting<SettingColor> background = sgColor.add(new ColorSetting.Builder()
        .name("background-color")
        .description("The color of the nametag background.")
        .defaultValue(new SettingColor(0, 0, 0, 75))
        .build()
    );

    private final Setting<SettingColor> nameColor = sgColor.add(new ColorSetting.Builder()
        .name("text-color")
        .description("The color of the nametag id.")
        .defaultValue(new SettingColor())
        .build()
    );

    /**
     * Default color is from {@link meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme#minusColor}
     */
    private final Setting<SettingColor> highlightIfBlockedColor = sgColor.add(new ColorSetting.Builder()
        .name("nsfw-map-color")
        .description("The color of the nametag id if its blocked.")
        .defaultValue(new SettingColor(255, 50, 50))
        .visible(highlightIfBlocked::get)
        .build()
    );

    private final Vector3d pos = new Vector3d();
    private final List<Entity> entityList = new ArrayList<>();

    public MapIdNametags() {
        super(SixBees.CATEGORY, "map-id-nametags", "Shows the id of maps as nametags");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        entityList.clear();

        for (Entity entity : mc.level.entitiesForRendering()) {
            EntityType<?> type = entity.getType();
            if (type != EntityTypes.ITEM_FRAME && type != EntityTypes.GLOW_ITEM_FRAME) continue;
            if (!canOnlySee.get() && !PlayerUtils.canSeeEntity(entity)) continue;

            if (!culling.get() || PlayerUtils.isWithinCamera(entity, maxCullRange.get())) entityList.add(entity);
        }

        entityList.sort(Comparator.comparingDouble(PlayerUtils::squaredDistanceTo));
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        int count = getRenderCount();
        boolean shadow = Config.get().customFont.get();

        for (int i = count - 1; i > -1; i--) {
            Entity entity = entityList.get(i);

            Utils.set(pos, entity, event.tickDelta);
            pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.2, 0);

            if (!NametagUtils.to2D(pos, scale.get())) continue;
            if (entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
                renderNametagItem(event.graphics, (ItemFrame) entity, shadow);
            }
        }
    }

    private int getRenderCount() {
        int count = culling.get() ? maxCullCount.get() : entityList.size();
        count = Mth.clamp(count, 0, entityList.size());

        return count;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(getRenderCount());
    }

    private void renderNametagItem(GuiGraphicsExtractor graphics, ItemFrame itemFrame, boolean shadow) {
        if (!itemFrame.hasFramedMap()) return;

        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        int mapId = itemFrame.getFramedMapId(itemFrame.getItem()).id();

        double nameWidth = text.getWidth(String.valueOf(mapId), shadow);
        double heightDown = text.getHeight(shadow);

        double widthHalf = nameWidth / 2;

        drawBg(-widthHalf, -heightDown, nameWidth, heightDown);

        text.beginBig(graphics);
        double hX = -widthHalf;
        double hY = -heightDown;

        if (highlightIfBlocked.get() && NsfwBlock.NSFW_MAPS.get().contains(mapId)) {
            text.render(String.valueOf(mapId), hX, hY, highlightIfBlockedColor.get(), shadow);
        } else {
            text.render(String.valueOf(mapId), hX, hY, nameColor.get(), shadow);
        }
        text.end();

        NametagUtils.end();
    }

    private void drawBg(double x, double y, double width, double height) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, background.get());
        Renderer2D.COLOR.render();
    }
}
