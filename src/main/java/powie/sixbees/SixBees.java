package powie.sixbees;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import powie.sixbees.commands.AddBase;
import powie.sixbees.commands.GetMapId;
import powie.sixbees.hud.SixBeesStarscript;
import powie.sixbees.hud.TextPresets;
import powie.sixbees.modules.*;
import powie.sixbees.tabs.BaseTab;

import static powie.sixbees.utils.Checks.isDevEnvOrHasExtraArgs;
import static powie.sixbees.utils.Config.initializeConfig;

public class SixBees extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("6Bees");
    public static final HudGroup HUD_GROUP = new HudGroup("6Bees");

    @Override
    public void onInitialize() {
        LOG.info("Initializing 6 Bees");

        initializeConfig();

        if (isDevEnvOrHasExtraArgs()) {
            Modules.get().add(new AutoLogin());
            Modules.get().add(new ChatLogger());
            Tabs.add(new BaseTab());
        }

        // Modules
        Modules.get().add(new AdBlock());
        Modules.get().add(new AntiBaseLeak());
        Modules.get().add(new AntiBedTrap());
        Modules.get().add(new AntiTinnitus());
        Modules.get().add(new Bees());
        Modules.get().add(new FreeHome());
        Modules.get().add(new NsfwBlock());
        Modules.get().add(new ShowMapId());

        Commands.add(new GetMapId());
        Commands.add(new AddBase());

        // HUD
        SixBeesStarscript.init();
        Hud.get().register(TextPresets.INFO);
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
