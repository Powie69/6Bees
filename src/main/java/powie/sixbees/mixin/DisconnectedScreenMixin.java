package powie.sixbees.mixin;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import powie.sixbees.utils.GlobalSettings;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow
    @Final
    private LinearLayout layout;

    @Shadow
    @Final
    private DisconnectionDetails details;

    // yeah, I know it's not the convention to have logic in mixins.
    // If you know a better place to store it in please let me know.

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private final Pattern VERIFICATION_LINK_PATTERN = Pattern.compile("https://verify\\.6b6t\\.org/[^\\s`)\\]]+");

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V", shift = At.Shift.BEFORE))
    private void addButtons(CallbackInfo ci) {
        if (!GlobalSettings.openVerifyLinkButton.get()) return;
        if (!details.reason().getString().startsWith("6b6t Anti Bot Protection")) return;

        Matcher matcher = VERIFICATION_LINK_PATTERN.matcher(details.reason().getString());
        if (!matcher.find()) return;

        layout.addChild(
            new Button.Builder(Component.literal("Open verification linkte"), _ -> {
                Blaze3D.openUri(URI.create(matcher.group()));
            }).build()
        );
    }
}

