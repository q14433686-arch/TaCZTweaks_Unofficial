package me.muksc.tacztweaks.compat;

import me.muksc.tacztweaks.TaCZTweaks;
import net.neoforged.fml.ModList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Vex;

import java.lang.reflect.Method;

/** Friendly-fire semantics shared with Pillager's Gun 3.3.x for TaCZ projectiles. */
public final class PillagersGunCompat {
    private static final boolean LOADED = ModList.get().isLoaded("pillagers_gun");
    private static Method valuesMethod;
    private static Method friendlyFireMethod;
    private static boolean lookupFailed;

    private PillagersGunCompat() {
    }

    public static boolean shouldIgnore(Entity target, Entity owner) {
        if (!LOADED || owner == null || friendlyFireEnabled()) return false;
        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(owner.getType()).is(EntityTypeTags.RAIDERS)
            && (BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(EntityTypeTags.RAIDERS)
                || target instanceof Vex);
    }

    private static boolean friendlyFireEnabled() {
        try {
            if (valuesMethod == null) {
                if (lookupFailed) return false;
                Class<?> config = Class.forName("com.scarasol.pillagers_gun.config.PillagersGunConfig");
                valuesMethod = config.getMethod("values");
                Object values = valuesMethod.invoke(null);
                friendlyFireMethod = values.getClass().getMethod("friendlyFire");
                return (boolean) friendlyFireMethod.invoke(values);
            }
            Object values = valuesMethod.invoke(null);
            return (boolean) friendlyFireMethod.invoke(values);
        } catch (ReflectiveOperationException error) {
            if (!lookupFailed) {
                lookupFailed = true;
                TaCZTweaks.LOGGER.warn("Could not read Pillager's Gun friendly-fire config; using its default", error);
            }
            return false;
        }
    }
}
