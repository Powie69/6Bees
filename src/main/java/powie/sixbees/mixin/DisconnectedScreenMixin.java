package powie.sixbees.mixin;

import com.mojang.blaze3d.Blaze3D;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import powie.sixbees.modules.VerificationLink;

import java.net.URI;

import static powie.sixbees.SixBees.LOG;

@Mixin(value = DisconnectedScreen.class, priority = 900)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow
    @Final
    private LinearLayout layout;

    @Shadow
    @Final
    private DisconnectionDetails details;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V", shift = At.Shift.BEFORE))
    private void addButtons(CallbackInfo ci) {
        VerificationLink verificationLinkModule = Modules.get().get(VerificationLink.class);
        if (verificationLinkModule == null) {
            LOG.error("verificationLinkModule not found");
            return;
        }
        if (!verificationLinkModule.isActive()) return;

        String url = verificationLinkModule.getVerificationUrl(details);
        if (url.isEmpty()) return;

        layout.addChild(
            new Button.Builder(
                Component.literal("Open Verification Link"),
                _ -> Blaze3D.openUri(URI.create(url))).build()
        );
    }
}

