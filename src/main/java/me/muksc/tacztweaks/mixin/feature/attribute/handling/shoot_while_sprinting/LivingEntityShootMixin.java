package me.muksc.tacztweaks.mixin.feature.attribute.handling.shoot_while_sprinting;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.entity.shooter.LivingEntityShoot;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import me.muksc.tacztweaks.core.extension.DeferredHolderExt;
import me.muksc.tacztweaks.registry.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(value = LivingEntityShoot.class, remap = false)
public abstract class LivingEntityShootMixin {
    @Shadow @Final private LivingEntity shooter;
    @Shadow @Final private ShooterDataHolder data;

    //? if >=1.21.11 {
    /*@WrapMethod(method = "shootInternal")
    private ShootResult tacztweaks$shoot$attribute$handling$shootWhileSprinting(
        Supplier<Float> pitch,
        Supplier<Float> yaw,
        long timestamp,
        float chargeProgress,
        boolean hasChargeContext,
        Operation<ShootResult> original
    ) {
        if (!shooter.getAttributes().hasAttribute(DeferredHolderExt.valueOrDelegate(ModAttributes.SHOOT_WHILE_SPRINTING))
            || shooter.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.SHOOT_WHILE_SPRINTING)) <= 0.0) {
            return original.call(pitch, yaw, timestamp, chargeProgress, hasChargeContext);
        }
        float previous = data.sprintTimeS;
        data.sprintTimeS = 0.0F;
        try {
            return original.call(pitch, yaw, timestamp, chargeProgress, hasChargeContext);
        } finally {
            data.sprintTimeS = previous;
        }
    }
    *///?} else {
    @Definition(id = "sprintTimeS", field = "Lcom/tacz/guns/entity/shooter/ShooterDataHolder;sprintTimeS:F")
    @Expression("?.sprintTimeS > 0.0")
    @ModifyExpressionValue(method = "shoot(Ljava/util/function/Supplier;Ljava/util/function/Supplier;JFZ)Lcom/tacz/guns/api/entity/ShootResult;", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean tacztweaks$shoot$attribute$handling$shootWhileSprinting(boolean original) {
        if (!shooter.getAttributes().hasAttribute(DeferredHolderExt.valueOrDelegate(ModAttributes.SHOOT_WHILE_SPRINTING))) return original;
        double value = shooter.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.SHOOT_WHILE_SPRINTING));
        return original && value <= 0.0;
    }
    //?}
}
