package me.muksc.tacztweaks.mixin.features;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.data.manager.BulletSoundsManager;
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores data-pack burst/pellet selectors on R2's named projectile hooks and starts one
 * airspace evaluation per successful shot cycle.
 */
@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class ModernKineticGunScriptAPIMixin {
    @Unique
    private int tacztweaks$burstIndex;

    @Unique
    private int tacztweaks$pelletIndex;

    @Inject(method = "shootOnce", at = @At("HEAD"))
    private void tacztweaks$shootOnce$resetBurst(boolean consumeAmmo, CallbackInfo ci) {
        tacztweaks$burstIndex = 0;
    }

    @Inject(method = "spawnProjectiles", at = @At("HEAD"))
    private void tacztweaks$spawnProjectiles$resetPellet(
        GunData gunData,
        BulletData bulletData,
        IGunOperator gunOperator,
        float shotDamageMultiplier,
        float inaccuracy,
        float processedSpeed,
        int bulletAmount,
        float pitch,
        float yaw,
        CallbackInfo ci
    ) {
        tacztweaks$pelletIndex = 0;
    }

    @ModifyArg(
        method = "spawnProjectiles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        ),
        index = 0
    )
    private Entity tacztweaks$spawnProjectiles$tag(Entity entity) {
        if (!(entity instanceof EntityKineticBullet bullet)) return entity;
        EntityKineticBulletExtension extension = (EntityKineticBulletExtension) bullet;
        extension.tacztweaks$setBurstIndex(tacztweaks$burstIndex);
        extension.tacztweaks$setPelletIndex(tacztweaks$pelletIndex);
        if (tacztweaks$pelletIndex++ == 0 && bullet.level() instanceof ServerLevel level) {
            BulletSoundsManager.INSTANCE.handleAirspace(level, bullet);
        }
        return entity;
    }

    @Inject(method = "runShootCycle", at = @At("RETURN"))
    private void tacztweaks$runShootCycle$advanceBurst(
        boolean consumeAmmo,
        GunData gunData,
        BulletData bulletData,
        IGunOperator gunOperator,
        float shotDamageMultiplier,
        float inaccuracy,
        int soundDistance,
        boolean useSilenceSound,
        float processedSpeed,
        int bulletAmount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValueZ()) tacztweaks$burstIndex++;
    }
}
