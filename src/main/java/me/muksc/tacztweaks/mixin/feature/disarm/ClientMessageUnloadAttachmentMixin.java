package me.muksc.tacztweaks.mixin.feature.disarm;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import me.muksc.tacztweaks.feature.disarm.DisarmManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.11 {
/*import net.fabricmc.fabric.api.networking.v1.PacketSender;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(value = ClientMessageUnloadAttachment.class, remap = false)
public abstract class ClientMessageUnloadAttachmentMixin {
    //? if >=1.21.11 {
    /*@Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$handle$disarm(ServerPlayer player, PacketSender responseSender, CallbackInfo ci) {
        if (DisarmManager.shouldDisarm(player)) ci.cancel();
    }
    *///?} else {
    //? if forge {
    @ModifyExpressionValue(method = "lambda$handle$0", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;hasAttachmentLock(Lnet/minecraft/world/item/ItemStack;)Z"))
    //?} else if fabric && 1.20.1 {
    /*@ModifyExpressionValue(method = "handle", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;hasAttachmentLock(Lnet/minecraft/world/item/ItemStack;)Z"))
    *///?} else if (fabric && 1.21.1) || neoforge {
    /*@ModifyExpressionValue(method = "lambda$handle$2", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;hasAttachmentLock(Lnet/minecraft/world/item/ItemStack;)Z"))
    *///?}
    private static boolean tacztweaks$handle$disarm(
        boolean original,
        @Local ServerPlayer player
    ) {
        return original || DisarmManager.shouldDisarm(player);
    }
    //?}
}
