package me.muksc.tacztweaks.mixin.crawl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Client-side crawl transition smoothing for the 1.21.11 AvatarRenderer path. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Unique
    private static final String SETUP_ROTATIONS =
        "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V";

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

    @ModifyArgs(
        method = SETUP_ROTATIONS,
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V")
    )
    private void tacztweaks$setupRotations$smoothTranslation(Args args, @Local(ordinal = 2) float swimAmount) {
        if (!Config.Crawl.INSTANCE.visualTweak()) return;
        float progress = Mth.clamp(swimAmount, 0.0F, 1.0F);
        float y = args.get(1);
        float z = args.get(2);
        args.set(1, Mth.lerp(progress, 0.0F, y - 0.4F));
        args.set(2, Mth.lerp(progress, 0.0F, z));
    }
}
