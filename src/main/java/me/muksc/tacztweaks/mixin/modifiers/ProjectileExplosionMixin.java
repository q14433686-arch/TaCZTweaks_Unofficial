package me.muksc.tacztweaks.mixin.modifiers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.util.block.ProjectileExplosion;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies the player-only damage modifier to TaCZ explosive ammunition. */
@Mixin(value = ProjectileExplosion.class, remap = false)
public abstract class ProjectileExplosionMixin {
    @WrapOperation(
        method = "explode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            remap = true
        )
    )
    private void tacztweaks$explode$playerDamage(
        Entity entity,
        DamageSource source,
        float amount,
        Operation<Void> original
    ) {
        float modified = entity instanceof Player
            ? (float) Config.Modifiers.PlayerDamage.INSTANCE.eval(amount)
            : amount;
        original.call(entity, source, modified);
    }
}
