package me.muksc.tacztweaks.mixin.feature.balancing.explosion_damage;

import com.tacz.guns.entity.EntityKineticBullet;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin {
    @Shadow private float explosionDamage;

    @Unique
    private boolean tacztweaks$explosionDamageApplied;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tacztweaks$init$explosionDamageModifier(CallbackInfo ci) {
        if (tacztweaks$explosionDamageApplied) return;
        if (((EntityKineticBullet) (Object) this).getOwner() == null) return;
        tacztweaks$explosionDamageApplied = true;
        explosionDamage = Mth.clamp(
            (float) Config.Balancing.ExplosionDamage.eval(explosionDamage),
            0.0F,
            Float.MAX_VALUE
        );
    }
}
