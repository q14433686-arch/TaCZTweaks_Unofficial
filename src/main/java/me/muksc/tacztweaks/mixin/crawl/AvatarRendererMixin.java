package me.muksc.tacztweaks.mixin.crawl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Port of the 1.20 {@code PlayerRenderer#setupRotations} visual crawl tweak.
 * 26.2 renamed the renderer to {@link AvatarRenderer} and feeds it an
 * {@link AvatarRenderState}; {@code isVisuallySwimming} / {@code swimAmount}
 * live on the render state instead of the player entity.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @ModifyExpressionValue(
        method = "setupRotations",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;isVisuallySwimming:Z"),
        require = 0
    )
    private boolean tacztweaks$setupRotations$translateAlways(boolean original) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return original;
        return true;
    }

    @WrapOperation(
        method = "setupRotations",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
        require = 0
    )
    private void tacztweaks$setupRotations$lerpTranslate(
        PoseStack instance, float x, float y, float z, Operation<Void> original,
        AvatarRenderState state
    ) {
        if (!Config.Crawl.INSTANCE.visualTweak()) {
            original.call(instance, x, y, z);
            return;
        }
        float f = state.swimAmount;
        original.call(instance, x, Mth.lerp(f, 0.0F, y - 0.4F), Mth.lerp(f, 0.0F, z));
    }
}
