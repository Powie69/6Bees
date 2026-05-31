package powie.sixbees;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import powie.sixbees.commands.FreeHome;
import powie.sixbees.modules.AdBlock;
import powie.sixbees.modules.AntiTinnitus;
import powie.sixbees.modules.ChatLogger;
import powie.sixbees.tabs.CoordsTab;
import powie.sixbees.utils.Config;

import static powie.sixbees.utils.Config.CONFIG_FOLDER;
import static powie.sixbees.utils.Config.initialize;

public class SixBees extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("6Bees");
    // public static final HudGroup HUD_GROUP = new HudGroup("6Bees");

    @Override
    public void onInitialize() {
        LOG.info("Initializing 6 Bees");

        LOG.info(String.valueOf(MeteorClient.FOLDER));
        LOG.info(String.valueOf(Config.CONFIG_FOLDER));

        if (!CONFIG_FOLDER.exists()) {
            CONFIG_FOLDER.getParentFile().mkdirs();
            CONFIG_FOLDER.mkdir();
        }
        initialize();

        // Modules
        Modules.get().add(new AdBlock());
        Modules.get().add(new AntiTinnitus());
        Modules.get().add(new ChatLogger());

        // commands
        Commands.add(new FreeHome());

        // HUD
        // Hud.get().register(HudExample.INFO);

        Tabs.add(new CoordsTab());

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
