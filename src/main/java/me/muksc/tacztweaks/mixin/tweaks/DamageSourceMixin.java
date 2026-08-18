package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.core.BulletProtection;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DamageSource.class)
public abstract class DamageSourceMixin {
    @ModifyReturnValue(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"))
    private boolean tacztweaks$is$treatBulletsAsProjectile(boolean original, TagKey<DamageType> tag) {
        if (original) return true;
        if (!BulletProtection.isActive()) return false;
        if (tag != DamageTypeTags.IS_PROJECTILE) return false;
        return ((DamageSource) (Object) this).is(ModDamageTypes.BULLETS_TAG);
    }
}
