package me.muksc.tacztweaks.mixin.gun;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.entity.IGunOperator;
import me.muksc.tacztweaks.TaCZTweaks;
import me.muksc.tacztweaks.client.input.ReduceSensitivityKey;
import me.muksc.tacztweaks.client.input.TiltGunKey;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Two tweaks, deliberately implemented on injection points that TaCZ's own
 * {@code MouseHandlerMixin} does NOT touch:
 * <ol>
 *   <li>reduce-sensitivity key: wraps the <em>call</em> to {@code turnPlayer(double)}
 *       inside {@code handleAccumulatedMovement}, scaling the sensitivity factor;</li>
 *   <li>crawl pitch limits: clamped <em>after</em> {@code turnPlayer} has finished (TAIL),
 *       by writing {@code LocalPlayer#getXRot()} directly. Doing it post-turn means it cannot
 *       be overridden by TaCZ's zoom rescaling of the turn delta.</li>
 * </ol>
 *
 * <p>Pitch sign convention (same as TaCZ): {@code playerPitch = -getXRot()}, positive when
 * looking up.</p>
 */
@Mixin(value = MouseHandler.class, priority = 1500)
public abstract class MouseHandlerMixin {
    @Unique
    private static long tacztweaks$lastPitchLog = 0L;

    // ---- reduce-sensitivity key (wrap the turnPlayer(double) call, not LocalPlayer#turn) ----
    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;turnPlayer(D)V"))
    private void tacztweaks$handleAccumulatedMovement$reduceSensitivity(MouseHandler instance, double sensitivity, Operation<Void> original) {
        double scaled = sensitivity;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && (ReduceSensitivityKey.KEY.isDown()
                || (Config.Gun.INSTANCE.tiltGunKeyTriggersReduceSensitivity() && TiltGunKey.isActive(player)))) {
            double multiplier = Config.Gun.INSTANCE.reduceSensitivityKeyMultiplier();
            if (Config.Gun.INSTANCE.disableReduceSensitivityKeyWhileAiming()) {
                multiplier = 1 + (multiplier - 1) * (1 - IGunOperator.fromLivingEntity(player).getSynAimingProgress());
            }
            scaled = sensitivity * multiplier;
        }
        original.call(instance, scaled);
    }

    // ---- crawl pitch limits (post-turn clamp) ----
    @Inject(method = "turnPlayer", at = @At("TAIL"))
    private void tacztweaks$turnPlayer$clampCrawlPitch(double sensitivity, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean crawling = !player.isSwimming() && player.getPose() == Pose.SWIMMING;
        if (!Config.Crawl.INSTANCE.enabled() || !crawling) return;

        float playerPitch = -player.getXRot();
        float upper = Config.Crawl.INSTANCE.pitchUpperLimit();
        float lower = tacztweaks$getPitchLowerLimit(player);

        boolean clamped = false;
        if (playerPitch > upper) {
            player.setXRot(-upper);
            clamped = true;
        } else if (playerPitch < lower) {
            player.setXRot(-lower);
            clamped = true;
        }

        // Diagnostic log (throttled to once per 2s).
        long now = System.currentTimeMillis();
        if (now - tacztweaks$lastPitchLog >= 2000) {
            tacztweaks$lastPitchLog = now;
            TaCZTweaks.LOGGER.info("[TaCZ Tweaks] crawlPitch crawling={} pitch={} upper={} lower={} clamped={}",
                    crawling, playerPitch, upper, lower, clamped);
        }
    }

    /**
     * Crawl pitch lower limit (degrees, negative = looking down). With
     * {@code dynamicPitchLimit}, a wall closer than 1 block in front progressively
     * restricts how far down the player may look (down to 0° when touching the wall).
     */
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
        if (distance >= 1.0) return lower;
        double t = Math.max(0.0, Math.min(1.0, distance));
        return (float) (lower * t);
    }
}
