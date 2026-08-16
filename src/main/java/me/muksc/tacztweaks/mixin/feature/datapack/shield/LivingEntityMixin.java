package me.muksc.tacztweaks.mixin.feature.datapack.shield;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.muksc.tacztweaks.feature.datapack.shield.CustomShieldResult;
import me.muksc.tacztweaks.mixininterface.feature.datapack.shield.CustomShieldEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

//? if fabric || forge
import org.spongepowered.asm.mixin.injection.ModifyArg;

//? if >=1.21.11 {
/*import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.BlocksAttacks;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?} else if fabric {
/*import com.llamalad7.mixinextras.sugar.Local;
*///?}

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements CustomShieldEntity {
    @Unique
    private CustomShieldResult tacztweaks$customShieldResult = null;
    @Unique
    private float tacztweaks$incomingBlockingDamage;

    @Override
    public CustomShieldResult tacztweaks$getShieldResult() {
        return tacztweaks$customShieldResult;
    }

    @Override
    public void tacztweaks$setShieldResult(CustomShieldResult result) {
        tacztweaks$customShieldResult = result;
    }

    //? if >=1.21.11 {
    /*@Inject(
        method = "applyItemBlocking",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F")
    )
    private void tacztweaks$applyItemBlocking$rememberDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        tacztweaks$incomingBlockingDamage = amount;
    }

    @ModifyExpressionValue(
        method = "applyItemBlocking",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F")
    )
    private float tacztweaks$applyItemBlocking$damage(float vanillaBlockedDamage) {
        if (!(tacztweaks$customShieldResult instanceof CustomShieldResult.Blocked blocked)) return vanillaBlockedDamage;
        return Math.max(0.0F, Math.min(tacztweaks$incomingBlockingDamage,
            tacztweaks$incomingBlockingDamage - blocked.damage()));
    }

    @ModifyArg(
        method = "applyItemBlocking",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;F)V"),
        index = 4
    )
    private float tacztweaks$applyItemBlocking$durabilityDamage(float damageAmount) {
        if (!(tacztweaks$customShieldResult instanceof CustomShieldResult.Blocked blocked)) return damageAmount;
        return blocked.durabilityDamage().apply(damageAmount);
    }

    @Definition(id = "is", method = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z")
    @Definition(id = "IS_PROJECTILE", field = "Lnet/minecraft/tags/DamageTypeTags;IS_PROJECTILE:Lnet/minecraft/tags/TagKey;")
    @Expression("?.is(IS_PROJECTILE)")
    @ModifyExpressionValue(method = "applyItemBlocking", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean tacztweaks$applyItemBlocking$knockback(boolean original) {
        if (!(tacztweaks$customShieldResult instanceof CustomShieldResult.Blocked blocked)) return original;
        return blocked.knockback();
    }

    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void tacztweaks$applyItemBlocking$disable(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (!(tacztweaks$customShieldResult instanceof CustomShieldResult.Blocked blocked)) return;
        if (blocked.disableDuration() <= 0 || !(LivingEntity.class.cast(this) instanceof Player player)) return;
        player.getCooldowns().addCooldown(player.getUseItem(), blocked.disableDuration());
    }
    *///?} else {
    //? if fabric {
    /*@Definition(id = "amount", local = @Local(type = float.class, argsOnly = true, ordinal = 0))
    @Expression("amount = @(0.0)")
    @ModifyExpressionValue(method = "hurt", at = @At("MIXINEXTRAS:EXPRESSION"))
    private float tacztweaks$hurt$shield$damage(float original) {
        if (!(tacztweaks$customShieldResult instanceof CustomShieldResult.Blocked blocked)) return original;
        return blocked.damage();
    }
    *///?}

    //? if fabric || forge {
    @ModifyArg(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtCurrentlyUsedShield(F)V"), index = 0)
    private float tacztweaks$hurt$shield$durabilityDamage(float damageAmount) {
        if (!(tacztweaks$customShieldResult instanceof CustomShieldResult.Blocked blocked)) return damageAmount;
        return blocked.durabilityDamage().apply(damageAmount);
    }
    //?}

    @Definition(id = "is", method = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z")
    @Definition(id = "IS_PROJECTILE", field = "Lnet/minecraft/tags/DamageTypeTags;IS_PROJECTILE:Lnet/minecraft/tags/TagKey;")
    @Expression("?.is(IS_PROJECTILE)")
    @ModifyExpressionValue(method = "hurt", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean tacztweaks$hurt$shield$knockback(boolean original) {
        if (!(tacztweaks$customShieldResult instanceof CustomShieldResult.Blocked blocked)) return original;
        return blocked.knockback();
    }
    //?}
}
