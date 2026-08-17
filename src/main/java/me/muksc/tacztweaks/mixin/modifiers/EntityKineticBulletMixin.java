package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.TacHitResult;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Global bullet modifiers:
 * <ul>
 *   <li>gravity / friction applied in the main constructor</li>
 *   <li>player-only damage / headshot multipliers applied in {@code onHitEntity}</li>
 * </ul>
 */
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin {
    private static final String INIT = "(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/Identifier;Lnet/minecraft/resources/Identifier;Lnet/minecraft/resources/Identifier;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V";

    @ModifyExpressionValue(method = "<init>" + INIT, at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/BulletData;getGravity()F"))
    private float tacztweaks$init$gravityModifier(float original) {
        return (float) Config.Modifiers.Gravity.INSTANCE.eval(original);
    }

    @ModifyExpressionValue(method = "<init>" + INIT, at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/BulletData;getFriction()F"))
    private float tacztweaks$init$frictionModifier(float original) {
        return (float) Config.Modifiers.Friction.INSTANCE.eval(original);
    }

    @ModifyExpressionValue(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;getDamage(Lnet/minecraft/world/phys/Vec3;)F", ordinal = 1))
    private float tacztweaks$onHitEntity$playerDamageModifier(float original, @Local(argsOnly = true) TacHitResult result) {
        if (!(result.getEntity() instanceof Player)) return original;
        return (float) Config.Modifiers.PlayerDamage.INSTANCE.eval(original);
    }

    @ModifyExpressionValue(method = "onHitEntity", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lcom/tacz/guns/entity/EntityKineticBullet;headShot:F"))
    private float tacztweaks$onHitEntity$playerHeadshotModifier(float original, @Local(argsOnly = true) TacHitResult result) {
        if (!(result.getEntity() instanceof Player)) return original;
        return (float) Config.Modifiers.PlayerHeadshot.INSTANCE.eval(original);
    }
}
