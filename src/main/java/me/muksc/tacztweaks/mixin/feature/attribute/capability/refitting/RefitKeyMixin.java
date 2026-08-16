package me.muksc.tacztweaks.mixin.feature.attribute.capability.refitting;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.client.input.RefitKey;
import me.muksc.tacztweaks.core.extension.DeferredHolderExt;
import me.muksc.tacztweaks.registry.ModAttributes;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.11 {
/*import cn.sh1rocu.tacz.api.event.InputEvent;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(value = RefitKey.class, remap = false)
public abstract class RefitKeyMixin {
    //? if >=1.21.11 {
    /*@Inject(method = "onRefitPress", at = @At("HEAD"), cancellable = true)
    private static void tacztweaks$onRefitPress$attribute$capability$refitting(InputEvent.Key event, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GunRefitScreen) return;
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        double value = player.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.REFITTING));
        if (value <= 0.0) ci.cancel();
    }
    *///?} else {
    @ModifyExpressionValue(method = "onRefitPress", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;hasAttachmentLock(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean tacztweaks$onRefitPress$attribute$capability$refitting(
        boolean original,
        @Local LocalPlayer player
    ) {
        double value = player.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.REFITTING));
        return original || value <= 0.0;
    }
    //?}
}
