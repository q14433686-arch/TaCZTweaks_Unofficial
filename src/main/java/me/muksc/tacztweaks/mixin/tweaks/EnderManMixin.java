package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes endermen treat gun bullets as projectiles, so they teleport away from them.
 * The first {@code DamageSource#is(TagKey)} call in {@code hurtServer} is the
 * {@code IS_PROJECTILE} check (re-verified against 26.3 decompiled sources;
 * {@code hurtServer(ServerLevel, DamageSource, float)} is unchanged).
 *
 * <p>26.3 renamed the class {@code EnderMan} → {@code Enderman} (package unchanged);
 * the mixin class name below is kept as registered in {@code tacztweaks.mixins.json}.</p>
 */
@Mixin(Enderman.class)
public abstract class EnderManMixin {
    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean tacztweaks$hurtServer$bulletsAreProjectiles(DamageSource instance, TagKey<DamageType> pDamageTypeKey, Operation<Boolean> original) {
        boolean result = original.call(instance, pDamageTypeKey);
        if (!Config.Tweaks.INSTANCE.endermenEvadeBullets()) return result;
        return result || instance.is(ModDamageTypes.BULLETS_TAG);
    }
}
