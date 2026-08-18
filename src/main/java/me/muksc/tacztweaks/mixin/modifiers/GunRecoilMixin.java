package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.resource.pojo.data.gun.GunRecoil;
import me.muksc.tacztweaks.mixininterface.modifiers.GunRecoilExtension;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

/**
 * Lets {@code CameraSetupEventMixin} apply per-keyframe recoil modifiers to the recoil
 * spline. Implemented by wrapping the {@code SplineInterpolator#interpolate} call inside
 * {@code GunRecoil#getSplineFunction} and rewriting the {@code y} values before the
 * interpolation happens. This avoids the {@code @Expression} bytecode matcher entirely
 * (compile-time verifiable, no MixinExtras version coupling).
 */
@Mixin(value = GunRecoil.class, remap = false)
public abstract class GunRecoilMixin implements GunRecoilExtension {
    @Unique
    private Function<Double, Double> tacztweaks$modifier = null;

    @Override
    public void tacztweaks$setModifier(Function<Double, Double> modifier) {
        tacztweaks$modifier = modifier;
    }

    @WrapOperation(method = "getSplineFunction", at = @At(value = "INVOKE", target = "Lorg/apache/commons/math3/analysis/interpolation/SplineInterpolator;interpolate([D[D)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;"))
    private PolynomialSplineFunction tacztweaks$getSplineFunction$modifier(SplineInterpolator instance, double[] x, double[] y, Operation<PolynomialSplineFunction> original) {
        if (tacztweaks$modifier != null) {
            for (int i = 0; i < y.length; i++) {
                y[i] = tacztweaks$modifier.apply(y[i]);
            }
        }
        return original.call(instance, x, y);
    }
}
