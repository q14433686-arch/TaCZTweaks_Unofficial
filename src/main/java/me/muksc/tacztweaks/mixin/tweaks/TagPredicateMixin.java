package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.advancements.predicates.TagPredicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Port of upstream {@code ProtectionEnchantmentMixin} for {@code bulletProtection}.
 *
 * <p>26.2 removed the {@code ProtectionEnchantment} class (enchantments are now
 * data-driven). Projectile Protection is now a data-defined
 * {@code minecraft:damage_protection} effect gated by a {@code damage_source} loot
 * condition whose tag check is {@code #minecraft:damage_type/is_projectile}, evaluated
 * through {@link TagPredicate#matches(Holder)} which calls
 * {@code holder.is(TagKey)}. Treating a TaCZ bullet damage type as matching the
 * projectile tag there re-enables the upstream behaviour with no data-pack changes.</p>
 */
@Mixin(TagPredicate.class)
public abstract class TagPredicateMixin {
    @WrapOperation(method = "matches", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean tacztweaks$matches$bulletsAreProjectiles(Holder<?> instance, TagKey<?> pTag, Operation<Boolean> original) {
        boolean result = original.call(instance, pTag);
        if (!Config.Tweaks.INSTANCE.bulletProtection()) return result;
        if (!DamageTypeTags.IS_PROJECTILE.equals(pTag)) return result;
        return result || tacztweaks$isBullet(instance);
    }

    @Unique
    private static boolean tacztweaks$isBullet(Holder<?> holder) {
        Optional<? extends ResourceKey<?>> key = holder.unwrapKey();
        if (key.isEmpty()) return false;
        Identifier id = key.get().identifier();
        return id.equals(ModDamageTypes.BULLET.identifier())
            || id.equals(ModDamageTypes.BULLET_IGNORE_ARMOR.identifier())
            || id.equals(ModDamageTypes.BULLET_VOID.identifier())
            || id.equals(ModDamageTypes.BULLET_VOID_IGNORE_ARMOR.identifier());
    }
}
