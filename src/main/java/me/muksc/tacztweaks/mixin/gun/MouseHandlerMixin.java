package me.muksc.tacztweaks.mixin.gun;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.entity.IGunOperator;
import me.muksc.tacztweaks.client.input.ReduceSensitivityKey;
import me.muksc.tacztweaks.client.input.TiltGunKey;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Two tweaks layered on top of TaCZ's own {@code MouseHandlerMixin#reduceSensitivity}:
 * <ol>
 *   <li>a "reduce sensitivity" key that scales the final turn by a configurable multiplier;</li>
 *   <li>crawl pitch limits. Instead of clamping the delta (which depends on wrap-nesting
 *       order relative to TaCZ's own wrap), the limits are enforced <em>after</em> the turn
 *       by clamping {@code LocalPlayer#getXRot()} directly — order-independent.</li>
 * </ol>
 */
@Mixin(value = MouseHandler.class, priority = 1500)
public abstract class MouseHandlerMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void tacztweaks$turnPlayer$tweaks(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        double finalYaw = yaw;
        double finalPitch = pitch;

        // Reduce-sensitivity key (optionally also triggered by the tilt-gun key).
        if (ReduceSensitivityKey.KEY.isDown()
                || (Config.Gun.INSTANCE.tiltGunKeyTriggersReduceSensitivity() && TiltGunKey.isActive(player))) {
            double multiplier = Config.Gun.INSTANCE.reduceSensitivityKeyMultiplier();
            if (Config.Gun.INSTANCE.disableReduceSensitivityKeyWhileAiming()) {
                multiplier = 1 + (multiplier - 1) * (1 - IGunOperator.fromLivingEntity(player).getSynAimingProgress());
            }
            finalYaw *= multiplier;
            finalPitch *= multiplier;
        }

        original.call(player, finalYaw, finalPitch);

        // Crawl pitch limits (TaCZ reuses the SWIMMING pose for crawling).
        // Enforced after the turn so it works regardless of wrap order.
        if (Config.Crawl.INSTANCE.enabled() && !player.isSwimming() && player.getPose() == Pose.SWIMMING) {
            float upper = Config.Crawl.INSTANCE.pitchUpperLimit();
            float lower = tacztweaks$getPitchLowerLimit(player);
            // playerPitch = -XRot; 向上看为正（与 TaCZ 的约定一致）。
            float playerPitch = -player.getXRot();
            if (playerPitch > upper) {
                player.setXRot(-upper);
            } else if (playerPitch < lower) {
                player.setXRot(-lower);
            }
        }
    }

    @Unique
    private static float tacztweaks$getPitchLowerLimit(LocalPlayer player) {
        float lower = Config.Crawl.INSTANCE.pitchLowerLimit();
        if (!Config.Crawl.INSTANCE.dynamicPitchLimit()) return lower;

        BlockHitResult result = player.level().clip(new ClipContext(
            player.getEyePosition(),
            player.getEyePosition().add(player.getLookAngle().scale(1.5)),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));
        if (result.getType() == HitResult.Type.MISS) return lower;

        double distance = result.getLocation().distanceTo(player.getEyePosition());
        // acos only meaningful for d <= 1; the ray is 1.5 blocks, so d < 1 means "close to a wall".
        if (distance >= 1.0) return lower;
        // 贴墙越近，允许下俯的角度越小（返回更大的下限值，单位与配置的度一致）。
        return (float) Math.max(Math.toDegrees(Math.acos(distance)), lower);
    }
}
