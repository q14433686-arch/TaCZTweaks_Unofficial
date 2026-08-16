package me.muksc.tacztweaks.mixin.feature.balancing.vertical_recoil;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.feature.balancing.RecoilState;
import me.muksc.tacztweaks.mixininterface.feature.balancing.recoil.DynamicGunRecoil;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CameraSetupEvent.class, remap = false)
public abstract class CameraSetupEventMixin {
    @WrapOperation(method = "initialCameraRecoil", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genPitchSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;"))
    private static PolynomialSplineFunction tacztweaks$initialCameraRecoil$verticalRecoilModifier(
        GunRecoil instance,
        float modifier,
        Operation<PolynomialSplineFunction> original
    ) {
        RecoilState state = RecoilState.capture();
        DynamicGunRecoil ext = DynamicGunRecoil.of(instance);
        try {
            ext.tacztweaks$setDynamicModifierMapper(value -> {
                boolean negative = value < 0;
                double recoil = Config.Balancing.VerticalRecoil.eval(Math.abs(value));
                if (state.crawling()) recoil = Config.Balancing.CrawlVerticalRecoil.eval(recoil);
                double aimingModifier = Config.Balancing.AimVerticalRecoil.eval(recoil) - recoil;
                recoil += aimingModifier * state.aimingProgress();
                return recoil * (negative ? -1 : 1);
            });
            return original.call(instance, modifier);
        } finally {
            ext.tacztweaks$setDynamicModifierMapper(null);
        }
    }
}
