package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.core.BulletProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;

/**
 * While vanilla is computing enchantment EPF, treat {@code tacz:bullets} as
 * {@code #minecraft:is_projectile} so Projectile Protection applies.
 * Confirmed present on 26.2: {@code EnchantmentHelper.getDamageProtection(ServerLevel, LivingEntity, DamageSource)F}.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @WrapMethod(method = "getDamageProtection")
    private static float tacztweaks$getDamageProtection$bulletsAreProjectiles(
        ServerLevel level, LivingEntity entity, DamageSource source, Operation<Float> original
    ) {
        if (!Config.Tweaks.INSTANCE.bulletProtection() || !source.is(ModDamageTypes.BULLETS_TAG)) {
            return original.call(level, entity, source);
        }
        BulletProtection.enter();
        try {
            return original.call(level, entity, source);
        } finally {
            BulletProtection.exit();
        }
    }
}
