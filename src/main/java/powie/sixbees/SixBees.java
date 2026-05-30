package powie.sixbees;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import powie.sixbees.modules.AdBlock;
import powie.sixbees.modules.AntiTinnitus;
import powie.sixbees.modules.ChatLogger;

public class SixBees extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("6Bees");
    // public static final HudGroup HUD_GROUP = new HudGroup("6Bees");

    @Override
    public void onInitialize() {
        LOG.info("Initializing 6 Bees");

        // Modules
        Modules.get().add(new AdBlock());
        Modules.get().add(new AntiTinnitus());
        Modules.get().add(new ChatLogger());

        // commands
        // Commands.add(new FreeHome());

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
    }}
