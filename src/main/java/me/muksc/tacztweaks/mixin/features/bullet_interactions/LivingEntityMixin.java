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

import kotlin.jvm.functions.Function1;

/**
 * Applies data-driven shield rules through 26.1.2's {@link BlocksAttacks} component.
 *
 * <p>Older Minecraft versions implemented shields in {@code Player#hurtCurrentlyUsedShield}
 * and {@code disableShield}. Both methods disappeared in 26.1.2. The exact replacement path is
 * {@code LivingEntity.applyItemBlocking}: resolve blocked damage, damage the blocking item,
 * then return the blocked amount to {@code hurtServer}. Hooking those two component calls
 * preserves vanilla angle/bypass checks while allowing the datapack to replace damage,
 * durability and cooldown values.</p>
 *
 * <h2>NeoForge 26.1.x call-site difference</h2>
 * NeoForge patches {@code BlocksAttacks} with an extra overload
 * {@code hurtBlockingItem(Level, ItemStack, LivingEntity, InteractionHand, float, int fixedDamage)}
 * and rewrites {@code LivingEntity#applyItemBlocking} to call <em>that</em> one, passing
 * {@code ShieldBlockEvent#shieldDamage()} (evidence: {@code patches/net/minecraft/world/entity/
 * LivingEntity.java.patch} and {@code patches/net/minecraft/world/item/component/BlocksAttacks.java.patch}
 * on branch {@code 26.1.x}). A wrap that only knows the vanilla 5-argument descriptor therefore
 * matches nothing and Mixin aborts with "Critical injection failure ... Scanned 0 target(s)".
 *
 * <p>Both descriptors are wrapped with {@code require = 0} so this mod keeps loading whichever
 * variant a given NeoForge build emits. If neither matches (a future call-site change), the
 * feature would silently stop working, so the RETURN handler logs a one-time warning when a
 * shield rule was resolved but no durability wrap consumed it.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private Function1<Integer, Integer> tacztweaks$shieldDurability;

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
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;resolveBlockedDamage(Lnet/minecraft/world/damagesource/DamageSource;FD)F"
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
        ItemStack shield = self.getItemBlockingWith();
        if (shield == null || shield.isEmpty()) return vanillaBlocked;

        BulletInteractionManager.ShieldInteractionResult result =
            BulletInteractionManager.INSTANCE.handleShieldInteraction(bullet, bullet.position(), shield, amount);
        if (result == null) return vanillaBlocked;
        tacztweaks$shieldDurability = result.getDurabilityDamage();
        tacztweaks$shieldDisableTicks = Math.max(0, result.getDisableDuration());
        return Math.clamp(result.getBlockedDamage(), 0.0F, amount);
    }

    /**
     * NeoForge 26.1.x call site: the extra {@code fixedDamage} argument already lets us hand the
     * custom durability cost back to vanilla/NeoForge code, so the item damage, break callback
     * and {@code onPlayerDestroyItem} event all keep running in their normal place.
     */
    @WrapOperation(
        method = "applyItemBlocking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;FI)V"
        ),
        require = 0
    )
    private void tacztweaks$applyItemBlocking$customDurabilityNeoForge(
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
        Function1<Integer, Integer> durability = tacztweaks$shieldDurability;
        if (durability == null) {
            original.call(attacks, level, stack, entity, hand, blockedDamage, fixedDamage);
            return;
        }

        // fixedDamage < 0 means "no override", i.e. use the component's own itemDamage curve.
        int vanillaDamage = fixedDamage < 0 ? attacks.itemDamage().apply(blockedDamage) : fixedDamage;
        int customDamage = Math.max(0, durability.invoke(vanillaDamage));
        original.call(attacks, level, stack, entity, hand, blockedDamage, customDamage);
        tacztweaks$applyShieldDisable(level, stack, entity);
    }

    /** Vanilla (unpatched) call site, kept as a fallback; re-implements the component body. */
    @WrapOperation(
        method = "applyItemBlocking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;F)V"
        ),
        require = 0
    )
    private void tacztweaks$applyItemBlocking$customDurabilityVanilla(
        BlocksAttacks attacks,
        Level level,
        ItemStack stack,
        LivingEntity entity,
        InteractionHand hand,
        float blockedDamage,
        Operation<Void> original
    ) {
        tacztweaks$shieldDurabilityApplied = true;
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
        if (tacztweaks$shieldDurability != null && !tacztweaks$shieldDurabilityApplied
            && !tacztweaks$warnedMissingDurabilityHook) {
            tacztweaks$warnedMissingDurabilityHook = true;
            TaCZTweaks.LOGGER.warn(
                "A bullet_interactions shield rule resolved custom durability/disable values, but neither "
                    + "BlocksAttacks#hurtBlockingItem wrap matched this Minecraft/NeoForge build. Shield "
                    + "durability and disable overrides are inactive; please report this with your exact "
                    + "Minecraft and NeoForge versions."
            );
        }
        tacztweaks$shieldDurability = null;
        tacztweaks$shieldDisableTicks = 0;
        tacztweaks$shieldDurabilityApplied = false;
    }
}
