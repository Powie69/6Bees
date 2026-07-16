package powie.sixbees.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final String METEOR_MOD_ID = "meteor-client";
    /**
     * Meteor's fabric.mod.json version = {@code <mcVersion>-<buildNumber>}, e.g. "26.1.2-39"
     * <a href="https://github.com/MeteorDevelopment/meteor-client/commit/2d490782460adc9ef2b1a4ecdc8dded1bf114bb7">commit</a>
     */
    private static final String THRESHOLD = "26.1.2-39";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("AddressCheckMixin")) {
            return FabricLoader.getInstance().getModContainer(METEOR_MOD_ID)
                .map(container -> {
                    Version actual = container.getMetadata().getVersion();
                    try {
                        Version threshold = SemanticVersion.parse(THRESHOLD);
                        return actual.compareTo(threshold) < 0;
                    } catch (VersionParsingException e) {
                        return false;
                    }
                })
                .orElse(false);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
