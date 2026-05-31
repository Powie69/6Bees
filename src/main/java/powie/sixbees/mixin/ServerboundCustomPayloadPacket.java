package powie.sixbees.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import powie.sixbees.utils.JoinPayload;

import java.util.List;

@Mixin(net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket.class)
public class ServerboundCustomPayloadPacket {

    // wow, thanks Dario Amodei
    @WrapOperation(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;codec(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$FallbackProvider;Ljava/util/List;)Lnet/minecraft/network/codec/StreamCodec;"
        )
    )
    private static StreamCodec<FriendlyByteBuf, CustomPacketPayload> anarchymod_injectJoinPayload(
        CustomPacketPayload.FallbackProvider<FriendlyByteBuf> fallbackProvider,
        List<CustomPacketPayload.TypeAndCodec<FriendlyByteBuf, ?>> knownTypes,
        Operation<StreamCodec<FriendlyByteBuf, CustomPacketPayload>> original
    ) {
        knownTypes.add(new CustomPacketPayload.TypeAndCodec<>(JoinPayload.TYPE, JoinPayload.CODEC));
        return original.call(fallbackProvider, knownTypes);
    }
}