package me.muksc.tacztweaks.mixin.gun;

import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.entity.EntityKineticBullet;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityBulletRenderer.class, remap = false)
public abstract class EntityBulletRendererMixin {
    // 26.3: EntityRenderer#shouldRender gained a trailing float partialTicks; TaCZ's
    // EntityBulletRenderer override was re-signatured the same way (verified in source).
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$shouldRender$always(EntityKineticBullet bullet, Frustum camera, double pCamX, double pCamY, double pCamZ, float partialTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!Config.Gun.INSTANCE.disableBulletCulling()) return;
        cir.setReturnValue(true);
    }
}
