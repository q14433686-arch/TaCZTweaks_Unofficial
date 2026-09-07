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
 * Applies data-driven shield rules through 26.2's {@link BlocksAttacks} component.
 *
 * <p>Older Minecraft versions implemented shields in {@code Player#hurtCurrentlyUsedShield}
 * and {@code disableShield}. Both methods disappeared in 26.2. The exact replacement path is
 * {@code LivingEntity.applyItemBlocking}: resolve blocked damage, damage the blocking item,
 * then return the blocked amount to {@code hurtServer}. Hooking those two component calls
 * preserves vanilla angle/bypass checks while allowing the datapack to replace damage,
 * durability and cooldown values.</p>
 *
 * <h2>Variant {@code hurtBlockingItem} call sites</h2>
 * Vanilla 26.2 calls {@code BlocksAttacks#hurtBlockingItem(Level, ItemStack, LivingEntity,
 * InteractionHand, float)} from {@code applyItemBlocking}, but that call site is not stable
 * across 26.2-family builds: NeoForge patches an extra
 * {@code hurtBlockingItem(..., float, int fixedDamage)} overload and rewrites the call to it
 * (evidence: {@code neoforged/NeoForge} branch {@code 26.2.x}, patches for
 * {@code net/minecraft/world/item/component/BlocksAttacks} and
 * {@code net/minecraft/world/entity/LivingEntity}), and the 1.21.11 dedicated-server jar
 * already ships the extra-argument variant (see the 1.21.11 NeoForge line's field notes).
 * A wrap pinned to a single descriptor therefore fails in the other environments: it aborts
 * at class load with "Critical injection failure ... 0 target(s)", or — when one variant's
 * {@link Operation} is handed to code that calls it with the other variant's argument count
 * — it crashes at the first shield block with MixinExtras'
 * {@code IncorrectArgumentCountException} ("Expected 7 but got 6" /
 * "throwIncorrectArgumentCount"). This was the reported creeper-explosion crash on the
 * 1.21.11 NeoForge line.
 *
 * <p>Both known descriptors are wrapped with {@code require = 0}, and every handler passes
 * exactly the argument count of <em>its own</em> call site (receiver included). If a future
 * 26.2 build emits yet another variant, the mod keeps loading and degrades gracefully: the
 * RETURN handler logs a one-time warning when a shield rule was resolved but no durability
 * wrap consumed it. {@code remap = false} on the optional variants keeps the Mixin AP from
 * refmap-validating descriptors that are absent from the unobfuscated vanilla 26.2 compile
 * classpath; the 26.2 runtime uses final names, so the strings are used as-is.</p>
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
     * Vanilla 26.2 call site; re-implements the component body because the vanilla method
     * applies the {@code itemDamage} curve internally. {@code require = 0}: a diverged build
     * may present only the fixed-damage variant instead, and the mod must still load.
     */
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

    /**
     * NeoForge-style 26.2 call site: the extra {@code fixedDamage} argument already lets us
     * hand the custom durability cost back to the patched component body, so the item
     * damage, break callback and item-destroyed hooks all keep running in their normal
     * place. {@code fixedDamage < 0} means "no override", i.e. use the component's own
     * {@code itemDamage} curve.
     *
     * <p>{@code remap = false} + {@code require = 0}: this overload is absent from the
     * unobfuscated vanilla 26.2 compile classpath and from plain Fabric runtimes, so it must
     * neither be refmap-validated nor required at apply time. This handler MUST call
     * {@code original} with 7 values (receiver + 6 arguments) — the argument count of this
     * call site — never the 6-value vanilla form (that mismatch is exactly the
     * {@code IncorrectArgumentCountException} regression).</p>
     */
    @WrapOperation(
        method = "applyItemBlocking",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/BlocksAttacks;hurtBlockingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;FI)V",
            remap = false
        ),
        require = 0
    )
    private void tacztweaks$applyItemBlocking$customDurabilityFixedDamage(
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

        int vanillaDamage = fixedDamage < 0 ? attacks.itemDamage().apply(blockedDamage) : fixedDamage;
        int customDamage = Math.max(0, durability.invoke(vanillaDamage));
        original.call(attacks, level, stack, entity, hand, blockedDamage, customDamage);
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
                "A bullet_interactions shield rule resolved custom durability/disable values, but no "
                    + "BlocksAttacks#hurtBlockingItem call site matched this Minecraft build. Shield "
                    + "durability and disable overrides are inactive; please report this with your exact "
                    + "Minecraft, Fabric and TaCZ Tweaks versions."
            );
        }
        tacztweaks$shieldDurability = null;
        tacztweaks$shieldDisableTicks = 0;
        tacztweaks$shieldDurabilityApplied = false;
    }
}
