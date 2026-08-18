package me.muksc.tacztweaks.mixin.features;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.data.manager.BulletSoundsManager;
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tags each spawned bullet with burst/pellet index (for data-driven {@code Target})
 * and kicks off airspace sound evaluation on the first pellet of a cycle.
 * 26.2 hook: {@code runShootCycle} (was {@code lambda$shootOnce$2}).
 */
@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class ModernKineticGunScriptAPIMixin {
    @Shadow
    private LivingEntity shooter;

    @Unique
    private int tacztweaks$burstIndex = 0;

    @Unique
    private int tacztweaks$pelletIndex = 0;

    @Inject(method = "shootOnce", at = @At("HEAD"))
    private void tacztweaks$shootOnce$resetBurst(boolean consumeAmmo, CallbackInfo ci) {
        tacztweaks$burstIndex = 0;
    }

    @WrapMethod(method = "runShootCycle")
    private boolean tacztweaks$runShootCycle$trackBurst(
        boolean consumeAmmo,
        GunData gunData,
        BulletData bulletData,
        IGunOperator gunOperator,
        float shotDamageMultiplier,
        float processedSpeed,
        int bulletAmount,
        boolean useSilenceSound,
        float inaccuracy,
        int soundDistance,
        Operation<Boolean> original
    ) {
        try {
            tacztweaks$pelletIndex = 0;
            return original.call(consumeAmmo, gunData, bulletData, gunOperator, shotDamageMultiplier, processedSpeed, bulletAmount, useSilenceSound, inaccuracy, soundDistance);
        } finally {
            tacztweaks$burstIndex++;
        }
    }

    @ModifyArg(method = "runShootCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true), index = 0)
    private Entity tacztweaks$runShootCycle$tagBullet(Entity entity) {
        if (entity instanceof EntityKineticBulletExtension ext) {
            ext.tacztweaks$setBurstIndex(tacztweaks$burstIndex);
            ext.tacztweaks$setPelletIndex(tacztweaks$pelletIndex++);
        }
        return entity;
    }

    @WrapOperation(method = "runShootCycle", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;doBulletSpread(Lcom/tacz/guns/entity/shooter/ShooterDataHolder;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;IFFFF)V"))
    private void tacztweaks$runShootCycle$handleAirspace(
        AbstractGunItem instance,
        ShooterDataHolder dataHolder,
        ItemStack gunItem,
        LivingEntity shooter,
        Projectile projectile,
        int bulletCnt,
        float processedSpeed,
        float inaccuracy,
        float pitch,
        float yaw,
        Operation<Void> original
    ) {
        original.call(instance, dataHolder, gunItem, shooter, projectile, bulletCnt, processedSpeed, inaccuracy, pitch, yaw);
        if (!(shooter.level() instanceof ServerLevel level)) return;
        if (!(projectile instanceof EntityKineticBullet bullet)) return;
        if (bulletCnt != 0) return;
        BulletSoundsManager.INSTANCE.handleAirspace(level, bullet);
    }
}
