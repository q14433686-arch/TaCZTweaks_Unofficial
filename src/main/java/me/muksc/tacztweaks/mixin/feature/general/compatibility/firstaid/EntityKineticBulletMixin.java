package me.muksc.tacztweaks.mixin.feature.general.compatibility.firstaid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.TacHitResult;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.feature.general.compatibility.FirstAidManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin {
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/event/common/EntityHurtByGunEvent$PreCallBack;post(Lcom/tacz/guns/api/event/common/EntityHurtByGunEvent$Pre;)V"))
    private void tacztweaks$onHitEntity$firstAidCompat(
        EntityHurtByGunEvent.PreCallBack instance, EntityHurtByGunEvent.Pre event, Operation<Void> original,
        @Local(argsOnly = true) TacHitResult result
    ) {
        original.call(instance, event);
        if (Config.General.Compatibility.firstAidCompat() && !event.isCanceled()) {
            FirstAidManager.onHitEntity(EntityKineticBullet.class.cast(this), result);
        }
    }
}
