package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.advancements.criterion.TagPredicate;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
 * via {@link TagPredicate#matches(Holder)}. Treating a bullet damage type as a
 * projectile there re-enables the upstream behaviour with no data-pack changes.</p>
 */
@Mixin(TagPredicate.class)
public abstract class TagPredicateMixin {
    @Shadow
    @Final
    private TagKey<?> tag;

    @Shadow
    @Final
    private boolean expected;

    @ModifyReturnValue(method = "matches(Lnet/minecraft/core/Holder;)Z", at = @At("RETURN"))
    private boolean tacztweaks$matches$bulletsAreProjectiles(boolean original, Holder<?> holder) {
        if (original) return true;
        if (!expected) return false;
        if (!DamageTypeTags.IS_PROJECTILE.equals(tag)) return false;
        if (!Config.Tweaks.INSTANCE.bulletProtection()) return false;
        return tacztweaks$isBullet(holder);
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
