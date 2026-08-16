package me.muksc.tacztweaks.fabric.mixin;

import me.muksc.tacztweaks.fabric.event.ShieldBlockEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
//? if >=1.21.11
/*import net.minecraft.server.level.ServerLevel;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    //? if >=1.21.11 {
    /*@Inject(
        method = "applyItemBlocking",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F")
    )
    private void tacztweaks$applyItemBlocking$shield(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        ShieldBlockEvent.CALLBACK.invoker().onShieldBlock(LivingEntity.class.cast(this), source, amount);
    }
    *///?} else {
    @Inject(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private void tacztweaks$hurt$shield(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ShieldBlockEvent.CALLBACK.invoker().onShieldBlock(LivingEntity.class.cast(this), source, amount);
    }
    //?}
}
