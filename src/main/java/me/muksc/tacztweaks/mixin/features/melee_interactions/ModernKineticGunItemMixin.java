package me.muksc.tacztweaks.mixin.features.melee_interactions;

import com.tacz.guns.item.ModernKineticGunItem;
import me.muksc.tacztweaks.data.manager.MeleeInteractionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * When a gun melee swing hits no living entity, try the data-driven block-break rules.
 * 26.2 still has {@code doMelee(LivingEntity, FFFFF, List)} and {@code doPerLivingHurt}.
 * ThreadLocals replace MixinExtras {@code @Share} (same semantics, less sugar).
 */
@Mixin(value = ModernKineticGunItem.class, remap = false)
public abstract class ModernKineticGunItemMixin {
    @Unique
    private static final ThreadLocal<Boolean> HIT = ThreadLocal.withInitial(() -> false);

    @Inject(method = "doMelee", at = @At("HEAD"))
    private void tacztweaks$doMelee$init(CallbackInfo ci) {
        HIT.set(false);
    }

    @Inject(method = "doMelee", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/item/ModernKineticGunItem;doPerLivingHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;FFLjava/util/List;)V"))
    private void tacztweaks$doMelee$setHit(CallbackInfo ci) {
        HIT.set(true);
    }

    @Inject(method = "doMelee", at = @At("TAIL"))
    private void tacztweaks$doMelee$handle(
        LivingEntity user,
        float gunDistance,
        float meleeDistance,
        float rangeAngle,
        float knockback,
        float damage,
        List<?> effects,
        CallbackInfo ci
    ) {
        try {
            if (!(user instanceof ServerPlayer player)) return;
            if (HIT.get()) return;
            MeleeInteractionManager.INSTANCE.handleBlockInteraction(player, 1 + gunDistance + meleeDistance, damage);
        } finally {
            HIT.remove();
        }
    }
}
