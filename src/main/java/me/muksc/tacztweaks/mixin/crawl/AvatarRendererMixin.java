package me.muksc.tacztweaks.mixin.crawl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Ports upstream's crawl transition smoothing to 26.2's render-state renderer.
 *
 * <p>26.3 reads {@link HumanoidRenderState#isVisuallySwimming}; the field was declared
 * on {@code AvatarRenderState} in 26.2 and moved up to the {@code HumanoidRenderState}
 * base class in 26.3, so the FIELD owner must name the declaring class exactly. The
 * interpolation amount is the first float local declared by the method, after its two
 * float arguments; therefore it is float ordinal 2. {@link ModifyArgs} changes Y and Z
 * in one injector and avoids the invalid "modified argument + target method arguments"
 * signature that {@code ModifyArg} rejects at runtime.</p>
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
            target = "Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;isVisuallySwimming:Z"
        )
    )
    private boolean tacztweaks$setupRotations$translateDuringTransition(boolean original) {
        return Config.Crawl.INSTANCE.visualTweak() || original;
    }

    @ModifyArgs(
        method = SETUP_ROTATIONS,
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V")
    )
    private void tacztweaks$setupRotations$smoothTranslation(
        Args args,
        @Local(ordinal = 2) float swimAmount
    ) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return;
        float progress = Mth.clamp(swimAmount, 0.0F, 1.0F);
        float y = args.get(1);
        float z = args.get(2);
        args.set(1, Mth.lerp(progress, 0.0F, y - 0.4F));
        args.set(2, Mth.lerp(progress, 0.0F, z));
    }
}
