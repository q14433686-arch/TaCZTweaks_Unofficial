package me.muksc.tacztweaks.compat;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.TacHitResult;
import me.muksc.tacztweaks.TaCZTweaks;
import me.muksc.tacztweaks.config.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/** Reflection-only bridge so First Aid remains an optional dependency. */
public final class FirstAidCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("firstaid");
    private static Method recordProjectileHit;
    private static boolean lookupFailed;

    private FirstAidCompat() {
    }

    public static void recordProjectileHit(EntityKineticBullet bullet, TacHitResult hit) {
        if (!LOADED || !Config.Compat.INSTANCE.firstAidCompat()) return;
        if (!(hit.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        try {
            Method method = recordProjectileHit;
            if (method == null) {
                if (lookupFailed) return;
                Class<?> eventHandler = Class.forName("ichttt.mods.firstaid.common.EventHandler");
                method = eventHandler.getMethod("recordProjectileHit", Player.class, Entity.class, Vec3.class);
                recordProjectileHit = method;
            }
            method.invoke(null, player, bullet, hit.getLocation());
        } catch (ReflectiveOperationException error) {
            if (!lookupFailed) {
                lookupFailed = true;
                TaCZTweaks.LOGGER.error("First Aid compatibility hook is unavailable", error);
            }
        }
    }
}
