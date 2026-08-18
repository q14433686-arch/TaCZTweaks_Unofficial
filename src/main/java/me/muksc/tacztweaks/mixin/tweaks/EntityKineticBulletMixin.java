package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.entity.EntityKineticBullet;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The counterpart of {@code EnderManMixin} for the {@code endermenEvadeBullets} tweak.
 *
 * <p>TaCZ's {@code tacz:use_magic_damage_on} tag contains {@code minecraft:enderman}, so by
 * default bullets hit endermen with <em>magic</em> damage — which never matches
 * {@code ModDamageTypes.BULLETS_TAG} and thus never triggers the enderman's projectile-evasion
 * teleport. This mixin makes endermen receive regular <em>bullet</em> damage instead, letting
 * {@code EnderManMixin} take effect.</p>
 *
 * <p>Implemented as a {@code @WrapOperation} over {@code Holder$Reference#is(TagKey)} and
 * filtering by the exact {@code USE_MAGIC_DAMAGE_ON} tag (robust against the three call sites
 * in {@code createDamageSources}: PRETEND_MELEE / USE_MAGIC / USE_VOID).</p>
 */
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin {
    @WrapOperation(method = "createDamageSources", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder$Reference;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean tacztweaks$createDamageSources$dontUseMagicOnEndermen(
        Holder.Reference instance,
        TagKey tagKey,
        Operation<Boolean> original,
        @Local(argsOnly = true) EntityKineticBullet.MaybeMultipartEntity parts
    ) {
        boolean result = original.call(instance, tagKey);
        if (tagKey != EntityKineticBullet.USE_MAGIC_DAMAGE_ON) return result;
        if (!Config.Tweaks.INSTANCE.endermenEvadeBullets()) return result;
        if (parts.hitPart().getType() != EntityType.ENDERMAN) return result;
        return false;
    }
}
