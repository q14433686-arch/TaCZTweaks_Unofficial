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
 * {@code IS_PROJECTILE} check (verified against the 26.2 bytecode).
 *
 * <p>26.3 renamed the target class {@code EnderMan} -> {@code Enderman} (same package).
 * The NeoForged primer marks that rename "not one-to-one", so the injection point itself
 * is <b>not</b> re-verified against 26.3 yet: the class now resolves, but whether
 * {@code hurtServer}'s first {@code DamageSource#is(TagKey)} is still the projectile check
 * needs {@code audit_port.py --minecraft-jar} plus an in-game check. This mixin failing
 * silently would only mean endermen stop dodging bullets, not a crash. The mixin class
 * keeps its old name so the entry in {@code tacztweaks.mixins.json} stays stable.</p>
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
