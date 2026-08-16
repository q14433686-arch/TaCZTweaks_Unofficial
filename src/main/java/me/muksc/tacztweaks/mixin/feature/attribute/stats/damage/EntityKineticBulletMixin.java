package me.muksc.tacztweaks.mixin.feature.attribute.stats.damage;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.core.extension.DeferredHolderExt;
import me.muksc.tacztweaks.registry.ModAttributes;
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedList;
import java.util.stream.Collectors;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin {
    @Shadow private LinkedList<ExtraDamage.DistanceDamagePair> damageAmount;

    /** Transform the completed constructor state instead of its cache-assignment expression. */
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V", at = @At("TAIL"))
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    private void tacztweaks$init$attribute$stats$damage(
        EntityType<? extends Projectile> type,
        Level level,
        LivingEntity shooter,
        ItemStack gunItem,
        ResourceLocation ammoId,
        ResourceLocation gunId,
        ResourceLocation gunDisplayId,
        boolean tracer,
        GunData gunData,
        BulletData bulletData,
        CallbackInfo ci
    ) {
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
