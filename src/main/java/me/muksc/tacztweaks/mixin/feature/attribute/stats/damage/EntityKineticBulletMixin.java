package me.muksc.tacztweaks.mixin.feature.attribute.stats.damage;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import me.muksc.tacztweaks.core.extension.DeferredHolderExt;
import me.muksc.tacztweaks.registry.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;
import java.util.stream.Collectors;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin {
    @Shadow private LinkedList<ExtraDamage.DistanceDamagePair> damageAmount;

    @Unique
    private boolean tacztweaks$damageAttributeApplied;

    /**
     * Transform the completed projectile state once its delegating constructor has assigned the
     * owner. Selecting all constructors by name avoids named/intermediary descriptor mismatches;
     * the owner/once guards restrict the transform to the full server-side construction path.
     */
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void tacztweaks$init$attribute$stats$damage(CallbackInfo ci) {
        if (tacztweaks$damageAttributeApplied) return;
        if (!(((EntityKineticBullet) (Object) this).getOwner() instanceof LivingEntity shooter)) return;
        tacztweaks$damageAttributeApplied = true;

        AttributeInstance attribute = shooter.getAttribute(DeferredHolderExt.valueOrDelegate(ModAttributes.DAMAGE));
        if (attribute == null || attribute.getModifiers().isEmpty()) return;
        double originalBaseValue = attribute.getBaseValue();
        try {
            damageAmount = damageAmount.stream()
                .map(pair -> {
                    attribute.setBaseValue(pair.getDamage());
                    return new ExtraDamage.DistanceDamagePair(pair.getDistance(), (float) attribute.getValue());
                })
                .collect(Collectors.toCollection(LinkedList::new));
        } finally {
            attribute.setBaseValue(originalBaseValue);
        }
    }
}
