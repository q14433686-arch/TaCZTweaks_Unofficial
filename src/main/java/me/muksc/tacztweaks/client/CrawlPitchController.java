package me.muksc.tacztweaks.client;

import me.muksc.tacztweaks.TaCZTweaks;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Applies the configured first-person crawl pitch range independently of input source. */
public final class CrawlPitchController {
    private static boolean stateInitialized;
    private static boolean wasCrawling;
    private static long lastClampLog;

    private CrawlPitchController() {
    }

    /** Called both immediately after mouse input and once at the end of every client tick. */
    public static void apply(LocalPlayer player) {
        if (player == null) {
            updateState(false, null, 0.0F, 0.0F);
            return;
        }
        boolean crawling = Config.Crawl.INSTANCE.enabled()
            && !player.isSwimming()
            && player.getPose() == Pose.SWIMMING;
        if (!crawling) {
            updateState(false, player, 0.0F, 0.0F);
            return;
        }

        float upper = Mth.clamp(Config.Crawl.INSTANCE.pitchUpperLimit(), 0.0F, 90.0F);
        float lower = Mth.clamp(getPitchLowerLimit(player), -90.0F, 0.0F);
        updateState(true, player, upper, lower);

        float pitch = -player.getXRot();
        float clampedPitch = Mth.clamp(pitch, lower, upper);
        if (Math.abs(clampedPitch - pitch) <= 1.0E-4F) return;

        player.setXRot(-clampedPitch);
        player.xRotO = player.getXRot();

        long now = System.currentTimeMillis();
        if (now - lastClampLog >= 2_000L) {
            lastClampLog = now;
            TaCZTweaks.LOGGER.info(
                "[TaCZ Tweaks] crawl pitch clamped: before={} after={} upper={} lower={} dynamic={}",
                pitch, clampedPitch, upper, lower, Config.Crawl.INSTANCE.dynamicPitchLimit()
            );
        }
    }

    private static void updateState(boolean crawling, LocalPlayer player, float upper, float lower) {
        if (!stateInitialized || crawling != wasCrawling) {
            stateInitialized = true;
            wasCrawling = crawling;
            if (crawling && player != null) {
                TaCZTweaks.LOGGER.info(
                    "[TaCZ Tweaks] crawl pitch controller active: xRot={} upper={} lower={} dynamic={}",
                    player.getXRot(), upper, lower, Config.Crawl.INSTANCE.dynamicPitchLimit()
                );
            }
        }
    }

    /** A collision within one block progressively moves the downward limit toward zero. */
    private static float getPitchLowerLimit(LocalPlayer player) {
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
        return (float) (lower * Mth.clamp(distance, 0.0, 1.0));
    }

    public static void reset() {
        stateInitialized = false;
        wasCrawling = false;
        lastClampLog = 0L;
    }
}
