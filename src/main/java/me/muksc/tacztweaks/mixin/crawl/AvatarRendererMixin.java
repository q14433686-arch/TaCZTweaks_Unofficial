package me.muksc.tacztweaks.mixin.crawl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Ports upstream's crawl transition smoothing to 26.2's render-state renderer.
 *
 * <p>The targets below are intentionally exact and required. The first attempted port used
 * the old entity call, the wrong field owner, and {@code require = 0}; it therefore could
 * silently do nothing. In 26.2 {@code setupRotations} reads
 * {@link AvatarRenderState#isVisuallySwimming} directly and exposes the interpolation value
 * as {@link AvatarRenderState#swimAmount}, so no fragile local-variable ordinal is needed.</p>
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Unique
    private static final String SETUP_ROTATIONS =
        "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;" +
            "Lcom/mojang/blaze3d/vertex/PoseStack;FF)V";

    @ModifyExpressionValue(
        method = SETUP_ROTATIONS,
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;isVisuallySwimming:Z"
        )
    )
    private boolean tacztweaks$setupRotations$translateDuringTransition(boolean original) {
        return Config.Crawl.INSTANCE.visualTweak() || original;
    }

    @ModifyArg(
        method = SETUP_ROTATIONS,
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
        index = 1
    )
    private float tacztweaks$setupRotations$smoothY(
        float original,
        AvatarRenderState state,
        PoseStack poseStack,
        float bodyRot,
        float scale
    ) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return original;
        return Mth.lerp(Mth.clamp(state.swimAmount, 0.0F, 1.0F), 0.0F, original - 0.4F);
    }

    @ModifyArg(
        method = SETUP_ROTATIONS,
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"),
        index = 2
    )
    private float tacztweaks$setupRotations$smoothZ(
        float original,
        AvatarRenderState state,
        PoseStack poseStack,
        float bodyRot,
        float scale
    ) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return original;
        return Mth.lerp(Mth.clamp(state.swimAmount, 0.0F, 1.0F), 0.0F, original);
    }
}
