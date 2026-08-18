package me.muksc.tacztweaks.mixin.features.bullet_interactions;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.data.manager.BulletInteractionManager;
import me.muksc.tacztweaks.mixininterface.features.bullet_interaction.ShieldInteractionBehaviour;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

/**
 * Fabric/26.2 replacement for Forge {@code ShieldBlockEvent}:
 * apply data-driven shield rules at the start of {@code hurtServer}, then
 * make bullets blockable even if they carry {@code BYPASSES_SHIELD}.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ShieldInteractionBehaviour {
    @Unique
    private Function<Integer, Integer> tacztweaks$customShieldDurabilityDamage = null;

    @Unique
    private Integer tacztweaks$customShieldDisableDuration = null;

    @Unique
    private boolean tacztweaks$handlingBullets = false;

    @Override
    public Function<Integer, Integer> tacztweaks$getCustomShieldDurabilityDamage() {
        return tacztweaks$customShieldDurabilityDamage;
    }

    @Override
    public void tacztweaks$setCustomShieldDurabilityDamage(Function<Integer, Integer> damage) {
        tacztweaks$customShieldDurabilityDamage = damage;
    }

    @Override
    public Integer tacztweaks$getCustomShieldDisableDuration() {
        return tacztweaks$customShieldDisableDuration;
    }

    @Override
    public void tacztweaks$setCustomShieldDisableDuration(Integer duration) {
        tacztweaks$customShieldDisableDuration = duration;
    }

    @WrapMethod(method = "hurtServer")
    private boolean tacztweaks$hurtServer$shieldContext(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity self = (LivingEntity) (Object) this;
        try {
            if (source.is(ModDamageTypes.BULLETS_TAG)) {
                tacztweaks$handlingBullets = true;
                if (self.isBlocking() && source.getDirectEntity() instanceof EntityKineticBullet ammo) {
                    Vec3 location = source.getSourcePosition() != null ? source.getSourcePosition() : ammo.position();
                    var result = BulletInteractionManager.INSTANCE.handleShieldInteraction(ammo, location, self.getUseItem(), amount);
                    if (result.getBlockedDamage() > 0) {
                        tacztweaks$customShieldDurabilityDamage = result.getDurabilityDamage();
                        tacztweaks$customShieldDisableDuration = result.getDisableDuration();
                    }
                }
            }
            return original.call(level, source, amount);
        } finally {
            tacztweaks$handlingBullets = false;
            tacztweaks$customShieldDurabilityDamage = null;
            tacztweaks$customShieldDisableDuration = null;
        }
    }

    @WrapOperation(method = "isDamageSourceBlocked", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean tacztweaks$isDamageSourceBlocked$blockableBullets(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original) {
        boolean result = original.call(instance, tag);
        if (!tacztweaks$handlingBullets) return result;
        if (tag == DamageTypeTags.BYPASSES_SHIELD) return false;
        return result;
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean tacztweaks$hurtServer$customShieldDisable(DamageSource instance, TagKey<DamageType> tag, Operation<Boolean> original) {
        if (!tacztweaks$handlingBullets) return original.call(instance, tag);
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return original.call(instance, tag);
        if (tacztweaks$customShieldDisableDuration == null) return original.call(instance, tag);
        if (tag != DamageTypeTags.IS_PROJECTILE) return original.call(instance, tag);
        if (tacztweaks$customShieldDisableDuration > 0) player.disableShield();
        return true;
    }
}
