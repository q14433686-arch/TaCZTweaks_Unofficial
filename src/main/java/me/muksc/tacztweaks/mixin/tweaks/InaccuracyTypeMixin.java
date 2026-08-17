package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.mixininterface.gun.SlideDataHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * With {@code betterGunTilt} enabled, the "tilt gun" (slide) state counts as sneaking for
 * inaccuracy purposes. Implemented by wrapping the second {@code LivingEntity#getPose()}
 * call inside {@code getInaccuracyType} (the one compared against {@code Pose.CROUCHING}).
 */
@Mixin(value = InaccuracyType.class, remap = false)
public abstract class InaccuracyTypeMixin {
    @WrapOperation(method = "getInaccuracyType", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getPose()Lnet/minecraft/world/entity/Pose;", ordinal = 1))
    private static Pose tacztweaks$getInaccuracyType$betterGunTilt(LivingEntity instance, Operation<Pose> original) {
        Pose pose = original.call(instance);
        if (!Config.Tweaks.INSTANCE.betterGunTilt()) return pose;
        if (((SlideDataHolder) instance).tacztweaks$getShouldSlide()) return Pose.CROUCHING;
        return pose;
    }
}
