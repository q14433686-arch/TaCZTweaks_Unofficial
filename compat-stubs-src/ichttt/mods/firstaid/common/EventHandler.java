package ichttt.mods.firstaid.common;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * Compile-time stub for optional First Aid compatibility targets.
 * Not packaged into the final mod jar.
 */
public final class EventHandler {
    private EventHandler() {
    }

    public static boolean handleCustomPlayerDamage(DamageSource source, TagKey<DamageType> tag) {
        return source.is(tag);
    }

    public static void recordProjectileHit(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Entity projectile, net.minecraft.world.phys.Vec3 hit) {
    }
}
