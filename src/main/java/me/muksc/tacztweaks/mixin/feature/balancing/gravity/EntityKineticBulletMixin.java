package me.muksc.tacztweaks.mixin.feature.balancing.gravity;

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
    @Shadow private float gravity;

    @Unique
    private boolean tacztweaks$gravityApplied;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tacztweaks$init$gravityModifier(CallbackInfo ci) {
        if (tacztweaks$gravityApplied) return;
        if (((EntityKineticBullet) (Object) this).getOwner() == null) return;
        tacztweaks$gravityApplied = true;
        gravity = (float) Config.Balancing.Gravity.eval(gravity);
    }
}
