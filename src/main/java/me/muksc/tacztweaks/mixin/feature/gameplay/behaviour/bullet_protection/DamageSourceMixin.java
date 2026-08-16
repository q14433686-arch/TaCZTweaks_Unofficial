package me.muksc.tacztweaks.mixin.feature.gameplay.behaviour.bullet_protection;

//? if >=1.21.11 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.muksc.tacztweaks.feature.gameplay.behaviour.BulletProtectionContext;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DamageSource.class)
public abstract class DamageSourceMixin {
    @ModifyReturnValue(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("RETURN"))
    private boolean tacztweaks$is$bulletProtection(boolean original, TagKey<DamageType> tag) {
        return original || (BulletProtectionContext.isEvaluatingBulletProtection() && tag == DamageTypeTags.IS_PROJECTILE);
    }
}
*///?} else {
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class DamageSourceMixin { }
//?}
