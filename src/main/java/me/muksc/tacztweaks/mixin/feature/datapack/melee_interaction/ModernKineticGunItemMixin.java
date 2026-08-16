package me.muksc.tacztweaks.mixin.feature.datapack.melee_interaction;

import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.pojo.data.attachment.EffectData;
import me.muksc.tacztweaks.feature.datapack.legacy.manager.MeleeInteractionManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.DoubleFunction;

@Mixin(value = ModernKineticGunItem.class, remap = false)
public abstract class ModernKineticGunItemMixin {
    @Shadow @Final
    private static DoubleFunction<AttributeModifier> AM_FACTORY = null;

    /**
     * Work at the stable doMelee boundary. Re-evaluating the same cone predicate avoids
     * binding to doPerLivingHurt's invocation bytecode or to javac local-variable layout.
     */
    @Inject(method = "doMelee", at = @At("TAIL"))
    private void tacztweaks$doMelee$handleBlockInteraction(
        LivingEntity user,
        float gunDistance,
        float meleeDistance,
        float rangeAngle,
        float knockback,
        float damage,
        List<EffectData> effects,
        CallbackInfo ci
    ) {
        double distance = gunDistance + meleeDistance;
        float xRot = (float) Math.toRadians(-user.getXRot());
        float yRot = (float) Math.toRadians(-user.getYRot());
        Vec3 eyeVec = new Vec3(0, 0, 1).xRot(xRot).yRot(yRot).normalize().scale(distance);
        Vec3 centrePos = user.getEyePosition().subtract(eyeVec);

        boolean hit = false;
        for (LivingEntity living : user.level().getEntitiesOfClass(
            LivingEntity.class,
            user.getBoundingBox().inflate(distance)
        )) {
            Vec3 targetVec = living.getEyePosition().subtract(centrePos);
            double targetLength = targetVec.length();
            if (targetLength < distance) continue;
            double degree = Math.toDegrees(Math.acos(targetVec.dot(eyeVec) / (targetLength * distance)));
            if (degree < rangeAngle / 2.0F && user.hasLineOfSight(living)) {
                hit = true;
                break;
            }
        }
        if (hit) return;

        float realDamage = damage;
        var attribute = user.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attribute != null) {
            double oldBase = attribute.getBaseValue();
            AttributeModifier modifier = AM_FACTORY.apply(damage);
            try {
                attribute.setBaseValue(0);
                attribute.addTransientModifier(modifier);
                realDamage = (float) attribute.getValue();
            } finally {
                attribute.setBaseValue(oldBase);
                attribute.removeModifier(modifier);
            }
        }
        MeleeInteractionManager.handleBlockInteraction(user, 1 + distance, realDamage);
    }
}
