package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes Projectile Protection reduce TaCZ bullet damage.
 *
 * 1.21.11 uses data-driven enchantments, so we must hook EnchantmentHelper's aggregate
 * protection lookup instead of the deleted ProtectionEnchantment class.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Unique
    private static final ResourceKey<Enchantment> tacztweaks$PROJECTILE_PROTECTION = ResourceKey.create(
        Registries.ENCHANTMENT,
        Identifier.fromNamespaceAndPath("minecraft", "projectile_protection")
    );

    @ModifyReturnValue(method = "getDamageProtection", at = @At("RETURN"))
    private static float tacztweaks$getDamageProtection$bullets(float original, ServerLevel level, LivingEntity entity, DamageSource source) {
        if (!Config.Tweaks.INSTANCE.bulletProtection()) return original;
        if (!source.is(ModDamageTypes.BULLETS_TAG)) return original;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return original;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return original;

        Holder.Reference<Enchantment> holder;
        try {
            holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(tacztweaks$PROJECTILE_PROTECTION);
        } catch (Exception ignored) {
            return original;
        }

        int totalLevel = 0;
        for (ItemStack stack : holder.value().getSlotItems(entity).values()) {
            totalLevel += EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        }
        if (totalLevel <= 0) return original;
        return original + totalLevel * 2.0F;
    }
}
