package me.muksc.tacztweaks.mixin.feature.disarm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import me.muksc.tacztweaks.feature.disarm.DisarmManager;
import net.minecraft.client.player.LocalPlayer;
//? if >=1.21.11 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunItemRendererWrapper.class, remap = false)
public abstract class GunItemRendererWrapperMixin {
    //~ if neoforge 'renderFirstPerson' -> 'renderFirstPersonInner'
    @Inject(method = "renderFirstPerson", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$renderFirstPerson$disarm(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, /*? if >=1.21.11 {*/ /*SubmitNodeCollector*/ /*?} else {*/ MultiBufferSource /*?}*/ bufferSource, int light, float partialTick, CallbackInfo ci) {
        if (!DisarmManager.getStatus().render) ci.cancel();
    }
}