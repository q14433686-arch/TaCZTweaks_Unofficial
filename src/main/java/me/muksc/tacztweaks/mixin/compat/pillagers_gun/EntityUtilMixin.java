package me.muksc.tacztweaks.mixin.compat.pillagers_gun;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.util.EntityUtil;
import me.muksc.tacztweaks.compat.PillagersGunCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/** Filters friendly raiders before TaCZ chooses the closest entity hit. */
@Mixin(value = EntityUtil.class, remap = false)
public abstract class EntityUtilMixin {
    @ModifyExpressionValue(
        method = {"findEntityOnPath", "findEntitiesOnPath"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            remap = true
        )
    )
    private static List<Entity> tacztweaks$findEntitiesOnPath$filterFriendlyFire(
        List<Entity> original,
        Projectile bullet,
        Vec3 start,
        Vec3 end
    ) {
        Entity owner = bullet.getOwner();
        if (owner == null || original.stream().noneMatch(entity -> PillagersGunCompat.shouldIgnore(entity, owner))) {
            return original;
        }
        List<Entity> filtered = new ArrayList<>(original);
        filtered.removeIf(entity -> PillagersGunCompat.shouldIgnore(entity, owner));
        return filtered;
    }
}
