package me.muksc.tacztweaks.mixin.crawl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Ports upstream {@code PlayerRendererMixin} onto 26.2's {@link AvatarRenderer}.
 * Both the entity-based and render-state {@code isVisuallySwimming} shapes are
 * listed with {@code require = 0} so a signature drift only disables the tweak
 * instead of crashing the client.
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @ModifyExpressionValue(
        method = "setupRotations",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isVisuallySwimming()Z"),
        require = 0
    )
    private boolean tacztweaks$setupRotations$translateAlwaysEntity(boolean original) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return original;
        return true;
    }

    @ModifyExpressionValue(
        method = "setupRotations",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;isVisuallySwimming()Z"),
        require = 0
    )
    private boolean tacztweaks$setupRotations$translateAlwaysState(boolean original) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return original;
        return true;
    }

    @ModifyExpressionValue(
        method = "setupRotations",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;isVisuallySwimming:Z"),
        require = 0
    )
    private boolean tacztweaks$setupRotations$translateAlwaysField(boolean original) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return original;
        return true;
    }

    @ModifyArg(
        method = "setupRotations",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
        index = 1,
        require = 0
    )
    private float tacztweaks$setupRotations$modifyYTranslate(float pY, @Local(ordinal = 0) float swimAmount) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return pY;
        return Mth.lerp(Mth.clamp(swimAmount, 0.0F, 1.0F), 0.0F, pY - 0.4F);
    }

    @ModifyArg(
        method = "setupRotations",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
        index = 2,
        require = 0
    )
    private float tacztweaks$setupRotations$lerpZTranslate(float pZ, @Local(ordinal = 0) float swimAmount) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return pZ;
        return Mth.lerp(Mth.clamp(swimAmount, 0.0F, 1.0F), 0.0F, pZ);
    }
}
