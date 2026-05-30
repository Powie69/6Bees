package powie.sixbees.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import powie.sixbees.modules.AntiTinnitus;

@Mixin(ClientPacketListener.class)
public class MixinExplosion {
    @WrapOperation(
        method = "handleExplosion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"
        )
    )
    private void onExplosion(ClientLevel instance, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean useDistance, Operation<Void> original) {
        AntiTinnitus module = Modules.get().get(AntiTinnitus.class);

        if (module.isActive() && module.explosionSound.get()) {
            original.call(instance, x, y, z, sound, category, (float) (volume * module.explosionVolume.get()), (float) (pitch * module.explosionPitch.get()), useDistance);
        }
    }
}