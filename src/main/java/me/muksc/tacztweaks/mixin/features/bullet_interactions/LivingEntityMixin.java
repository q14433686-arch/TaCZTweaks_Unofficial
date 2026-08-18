package me.muksc.tacztweaks.mixin.features.bullet_interactions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.data.manager.BulletInteractionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import kotlin.jvm.functions.Function1;

/**
 * Applies data-driven shield rules through 26.2's {@link BlocksAttacks} component.
 *
 * <p>Older Minecraft versions implemented shields in {@code Player#hurtCurrentlyUsedShield}
 * and {@code disableShield}. Both methods disappeared in 26.2. The exact replacement path is
 * {@code LivingEntity.applyItemBlocking}: resolve blocked damage, damage the blocking item,
 * then return the blocked amount to {@code hurtServer}. Hooking those two component calls
 * preserves vanilla angle/bypass checks while allowing the datapack to replace damage,
 * durability and cooldown values.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private Function1<Integer, Integer> tacztweaks$shieldDurability;

    @Unique
    private int tacztweaks$shieldDisableTicks;

    @Inject(method = "applyItemBlocking", at = @At("HEAD"))
    private void tacztweaks$applyItemBlocking$reset(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Float> cir
    ) {
        tacztweaks$shieldDurability = null;
        tacztweaks$shieldDisableTicks = 0;
    }

    @WrapOperation(
        method = "applyItemBlocking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"
        )
    )
    private float tacztweaks$applyItemBlocking$customDamage(
        BlocksAttacks attacks,
        DamageSource source,
        float amount,
        double angle,
        Operation<Float> original,
        ServerLevel level,
        DamageSource methodSource,
        float methodAmount
    ) {
        float vanillaBlocked = original.call(attacks, source, amount, angle);
        if (vanillaBlocked <= 0.0F || !source.is(ModDamageTypes.BULLETS_TAG)) return vanillaBlocked;
        if (!(source.getDirectEntity() instanceof EntityKineticBullet bullet)) return vanillaBlocked;

        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack shield = self.getItemBlockingWith();
        if (shield == null || shield.isEmpty()) return vanillaBlocked;

        BulletInteractionManager.ShieldInteractionResult result =
            BulletInteractionManager.INSTANCE.handleShieldInteraction(bullet, bullet.position(), shield, amount);
        tacztweaks$shieldDurability = result.getDurabilityDamage();
        tacztweaks$shieldDisableTicks = Math.max(0, result.getDisableDuration());
        return Math.clamp(result.getBlockedDamage(), 0.0F, amount);
    }

    @WrapOperation(
        method = "applyItemBlocking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;F)V"
        )
    )
    private void tacztweaks$applyItemBlocking$customDurability(
        BlocksAttacks attacks,
        Level level,
        ItemStack stack,
        LivingEntity entity,
        InteractionHand hand,
        float blockedDamage,
        Operation<Void> original
    ) {
        Function1<Integer, Integer> durability = tacztweaks$shieldDurability;
        if (durability == null) {
            original.call(attacks, level, stack, entity, hand, blockedDamage);
            return;
        }

        if (!level.isClientSide() && entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        }
        int vanillaDamage = attacks.itemDamage().apply(blockedDamage);
        int customDamage = Math.max(0, durability.invoke(vanillaDamage));
        if (customDamage > 0) {
            stack.hurtAndBreak(customDamage, entity, hand.asEquipmentSlot());
        }
        if (tacztweaks$shieldDisableTicks > 0 && entity instanceof Player player && !stack.isEmpty()) {
            player.getCooldowns().addCooldown(stack, tacztweaks$shieldDisableTicks);
            player.stopUsingItem();
        }
    }

    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void tacztweaks$applyItemBlocking$clear(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Float> cir
    ) {
        tacztweaks$shieldDurability = null;
        tacztweaks$shieldDisableTicks = 0;
    }
}
