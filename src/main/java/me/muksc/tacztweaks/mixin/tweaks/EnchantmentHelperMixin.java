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
 * Makes Projectile Protection reduce TaCZ bullet damage. 26.2 deleted
 * {@code ProtectionEnchantment}; the datapack effect only checks {@code #is_projectile},
 * and {@code tacz:bullets} is not in that tag (on purpose — putting it there would also
 * change endermen / shields). We add the vanilla {@code 2 * level} amount ourselves.
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
        // Matches the second requirement in vanilla projectile_protection.json.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return original;

        Holder.Reference<Enchantment> holder;
        try {
            holder = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(tacztweaks$PROJECTILE_PROTECTION);
        } catch (Exception ignored) {
            return original;
        }
        // getEnchantmentLevel(entity) returns only the maximum level. Vanilla's
        // damage_protection effect runs once per equipped stack, so levels on multiple armor
        // pieces must be summed instead.
        int totalLevel = 0;
        for (ItemStack stack : holder.value().getSlotItems(entity).values()) {
            totalLevel += EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        }
        if (totalLevel <= 0) return original;
        return original + totalLevel * 2.0F;
    }
}
