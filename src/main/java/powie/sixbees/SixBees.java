package powie.sixbees;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import powie.sixbees.commands.GetMapId;
import powie.sixbees.modules.*;
import powie.sixbees.tabs.CoordsTab;

import static powie.sixbees.utils.Config.initializeConfig;
import static powie.sixbees.utils.Checks.isDevEnvOrHasExtraArgs;

public class SixBees extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("6Bees");
    // public static final HudGroup HUD_GROUP = new HudGroup("6Bees");

    @Override
    public void onInitialize() {
        LOG.info("Initializing 6 Bees");

        initializeConfig();

        if (isDevEnvOrHasExtraArgs()) {
            Modules.get().add(new AutoLogin());
            Modules.get().add(new ChatLogger());
            Commands.add(new GetMapId());
            Tabs.add(new CoordsTab());
        }

        // Modules
        Modules.get().add(new AdBlock());
        Modules.get().add(new AntiBedTrap());
        Modules.get().add(new AntiTinnitus());
        Modules.get().add(new FreeHome());
        Modules.get().add(new NsfwBlock());

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
