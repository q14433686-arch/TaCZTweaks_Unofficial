package me.muksc.tacztweaks.mixin.feature.gameplay.behaviour.bullet_protection;

//? if >=1.21.11 {
/*import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tacz.guns.init.ModDamageTypes;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.feature.gameplay.behaviour.BulletProtectionContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @WrapMethod(method = "getDamageProtection")
    private static float tacztweaks$getDamageProtection$bulletProtection(
        ServerLevel level,
        LivingEntity entity,
        DamageSource source,
        Operation<Float> original
    ) {
        if (!Config.Gameplay.Behaviour.bulletProtection() || !source.is(ModDamageTypes.BULLETS_TAG)) {
            return original.call(level, entity, source);
        }
        return BulletProtectionContext.evaluate(() -> original.call(level, entity, source));
    }
}
*///?} else {
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class EnchantmentHelperMixin { }
//?}
