package powie.sixbees;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import powie.sixbees.commands.GetMapId;
import powie.sixbees.modules.*;
import powie.sixbees.tabs.CoordsTab;
import powie.sixbees.utils.Config;

import java.util.List;

import static powie.sixbees.utils.Checks.isDevEnvOrHasExtraArgs;
import static powie.sixbees.utils.Config.*;

public class SixBees extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("6Bees");
    // public static final HudGroup HUD_GROUP = new HudGroup("6Bees");

    @Override
    public void onInitialize() {
        LOG.info("Initializing 6 Bees");

        initializeConfig();
        List<Config.Place> places = readPlaces();

        savePlace(new Config.Place("Example Place", new BlockPos(0, 64, 0), 10.0, Dimension.Overworld));

        for (Config.Place place : places) {
            LOG.info("Place: {}", place.name);
            LOG.info("  Coords: {}", place.coords);
            LOG.info("  Radius: {}", place.radius);
            LOG.info("  Dimension: {}", place.dimension);
        }

        if (isDevEnvOrHasExtraArgs()) {
            Modules.get().add(new AutoLogin());
            Modules.get().add(new ChatLogger());
            Tabs.add(new CoordsTab());
        }

        // Modules
        Modules.get().add(new AdBlock());
        Modules.get().add(new AntiBedTrap());
        Modules.get().add(new AntiTinnitus());
        Modules.get().add(new FreeHome());
        Modules.get().add(new NsfwBlock());
        Modules.get().add(new ShowMapId());

        Commands.add(new GetMapId());

        // HUD
        // Hud.get().register(HudExample.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "powie.sixbees";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("Powie69", "6Bees", "master", null);
    }
}
