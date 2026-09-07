package me.muksc.tacztweaks.mixin.features.bullet_interactions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.TaCZTweaks;
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

import java.util.function.IntUnaryOperator;

/** Applies data-driven shield rules through LivingEntity's blocking-item path. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private IntUnaryOperator tacztweaks$shieldDurability;

    @Unique
    private int tacztweaks$shieldDisableTicks;

    @Unique
    private boolean tacztweaks$shieldDurabilityApplied;

    @Unique
    private static boolean tacztweaks$warnedMissingDurabilityHook;

    @Inject(method = "applyItemBlocking", at = @At("HEAD"))
    private void tacztweaks$applyItemBlocking$reset(
        ServerLevel level,
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Float> cir
    ) {
        tacztweaks$shieldDurability = null;
        tacztweaks$shieldDisableTicks = 0;
        tacztweaks$shieldDurabilityApplied = false;
    }

    @WrapOperation(
        method = "applyItemBlocking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F",
            remap = true
        )
    )
    private float tacztweaks$applyItemBlocking$customDamage(
        BlocksAttacks attacks,
        DamageSource source,
        float amount,
        double angle,
        Operation<Float> original
    ) {
        float vanillaBlocked = original.call(attacks, source, amount, angle);
        if (vanillaBlocked <= 0.0F || !source.is(ModDamageTypes.BULLETS_TAG)) return vanillaBlocked;
        if (!(source.getDirectEntity() instanceof EntityKineticBullet bullet)) return vanillaBlocked;

        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack shield = self.getUseItem();
        if (shield == null || shield.isEmpty()) return vanillaBlocked;

        BulletInteractionManager.ShieldInteractionResult result =
            BulletInteractionManager.INSTANCE.handleShieldInteraction(bullet, bullet.position(), shield, amount);
        if (result == null) return vanillaBlocked;
        tacztweaks$shieldDurability = result.getDurabilityDamage()::invoke;
        tacztweaks$shieldDisableTicks = Math.max(0, result.getDisableDuration());
        return Math.clamp(result.getBlockedDamage(), 0.0F, amount);
    }

    @WrapOperation(
        method = "applyItemBlocking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;F)V",
            remap = true
        ),
        require = 0
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
        tacztweaks$customDurability(attacks, level, stack, entity, hand, blockedDamage, original);
    }

    // 1.21.11 专服 jar 的 applyItemBlocking 与服务端/客户端合并 jar 不同：
    // hurtBlockingItem 在专服版多一个 int 形参。两处 require=0，各自只命中一种形态。
    @WrapOperation(
        method = "applyItemBlocking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;FI)V",
            remap = true
        ),
        require = 0
    )
    private void tacztweaks$applyItemBlocking$customDurabilityServer(
        BlocksAttacks attacks,
        Level level,
        ItemStack stack,
        LivingEntity entity,
        InteractionHand hand,
        float blockedDamage,
        int fixedDamage,
        Operation<Void> original
    ) {
        tacztweaks$shieldDurabilityApplied = true;
        IntUnaryOperator durability = tacztweaks$shieldDurability;
        if (durability == null) {
            original.call(attacks, level, stack, entity, hand, blockedDamage, fixedDamage);
            return;
        }
        // 见下方“必须先核实”：int 形参按 fixedDamage 语义处理（<0 = 不覆盖）
        int vanillaDamage = fixedDamage < 0 ? attacks.itemDamage().apply(blockedDamage) : fixedDamage;
        int customDamage = Math.max(0, durability.applyAsInt(vanillaDamage));
        original.call(attacks, level, stack, entity, hand, blockedDamage, customDamage);
        tacztweaks$applyShieldDisable(level, stack, entity);
    }

    @Unique
    private void tacztweaks$customDurability(
        BlocksAttacks attacks,
        Level level,
        ItemStack stack,
        LivingEntity entity,
        InteractionHand hand,
        float blockedDamage,
        Operation<Void> original
    ) {
        tacztweaks$shieldDurabilityApplied = true;
        IntUnaryOperator durability = tacztweaks$shieldDurability;
        if (durability == null) {
            original.call(attacks, level, stack, entity, hand, blockedDamage);
            return;
        }

        if (!level.isClientSide() && entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        }
        int vanillaDamage = attacks.itemDamage().apply(blockedDamage);
        int customDamage = Math.max(0, durability.applyAsInt(vanillaDamage));
        if (customDamage > 0) {
            stack.hurtAndBreak(customDamage, entity, hand.asEquipmentSlot());
        }
        tacztweaks$applyShieldDisable(level, stack, entity);
    }

    @Unique
    private void tacztweaks$applyShieldDisable(Level level, ItemStack stack, LivingEntity entity) {
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
        if (tacztweaks$shieldDurability != null && !tacztweaks$shieldDurabilityApplied && !tacztweaks$warnedMissingDurabilityHook) {
            tacztweaks$warnedMissingDurabilityHook = true;
            TaCZTweaks.LOGGER.warn(
                "盾牌规则已解析但没有任何 hurtBlockingItem 调用点命中本构建，耐久/禁用覆盖失效，请上报精确的 Minecraft/NeoForge/TaCZ Tweaks 版本"
            );
        }
        tacztweaks$shieldDurability = null;
        tacztweaks$shieldDisableTicks = 0;
        tacztweaks$shieldDurabilityApplied = false;
    }
}
