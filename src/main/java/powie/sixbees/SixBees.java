package powie.sixbees;

import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.PostInit;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import powie.sixbees.commands.AddBase;
import powie.sixbees.commands.GetMapId;
import powie.sixbees.commands.Seedmap;
import powie.sixbees.commands.StatsCommand;
import powie.sixbees.events.ChatListener;
import powie.sixbees.hud.SixBeesStarscript;
import powie.sixbees.hud.TextPresets;
import powie.sixbees.modules.*;
import powie.sixbees.tabs.BaseTab;

import java.util.List;

import static powie.sixbees.utils.Checks.isDevEnvOrHasExtraArgs;
import static powie.sixbees.utils.Config.firstLaunch;
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
            // avoid getting addon scrapped as these are extra/dev modules
            List<Module> devModules = List.of(
                new AutoLogin(),
                new ExportMapId()
            );
            devModules.forEach(Modules.get()::add);
        }

        // Modules
        Modules.get().add(new AdBlock());
        Modules.get().add(new AntiBaseLeak());
        Modules.get().add(new AntiBedTrap());
        Modules.get().add(new AntiTinnitus());
        Modules.get().add(new AntiWeb());
        Modules.get().add(new AutoWhisper());
        Modules.get().add(new BaseMacro());
        Modules.get().add(new DoubleEchest());
        Modules.get().add(new FreeHome());
        Modules.get().add(new MapIdNametags());
        Modules.get().add(new NoBlockEntities());
        Modules.get().add(new NsfwBlock());
        Modules.get().add(new ShowMapId());
        Modules.get().add(new StatsModule());
        Modules.get().add(new TpTweaks());
        Modules.get().add(new VerificationLink());

        // Commands
        Commands.add(new GetMapId());
        Commands.add(new AddBase());
        Commands.add(new Seedmap());
        Commands.add(new StatsCommand());

        // HUD
        SixBeesStarscript.init();
        Hud.get().register(TextPresets.INFO);

        // Tabs
        Tabs.add(new BaseTab());

        // Event listeners
        new ChatListener();
    }

    @PostInit
    public static void postInit() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) Modules.get().get(AutoLogin.class).enable();

        if (firstLaunch) {
            Modules.get().get(FreeHome.class).enable();
            Modules.get().get(VerificationLink.class).enable();
        }
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
        return new GithubRepo("Powie69", "6Bees", "main", null);
    }

    @Override
    public String getCommit() {
        String commit = FabricLoader
            .getInstance()
            .getModContainer("sixbees")
            .get().getMetadata()
            .getCustomValue("sixbees:commit")
            .getAsString();
        return commit.isEmpty() ? null : commit;
    }
}
