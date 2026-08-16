package me.muksc.tacztweaks.mixin.feature.balancing.friction;

import com.tacz.guns.entity.EntityKineticBullet;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin {
    @Shadow private float friction;

    @Unique
    private boolean tacztweaks$frictionApplied;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tacztweaks$init$frictionModifier(CallbackInfo ci) {
        if (tacztweaks$frictionApplied) return;
        if (((EntityKineticBullet) (Object) this).getOwner() == null) return;
        tacztweaks$frictionApplied = true;
        friction = (float) Config.Balancing.Friction.eval(friction);
    }
}
