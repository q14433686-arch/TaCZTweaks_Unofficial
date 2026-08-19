package me.muksc.tacztweaks.mixin.compat.firstaid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.init.ModDamageTypes;
import ichttt.mods.firstaid.common.EventHandler;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Lets First Aid use its projectile body-part distribution for TaCZ bullet damage. */
@Pseudo
@Mixin(value = EventHandler.class, remap = false)
public abstract class EventHandlerMixin {
    @WrapOperation(
        method = "handleCustomPlayerDamage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z",
            remap = true
        )
    )
    private static boolean tacztweaks$handleCustomPlayerDamage$bulletsAreProjectiles(
        DamageSource source,
        TagKey<DamageType> tag,
        Operation<Boolean> original
    ) {
        boolean result = original.call(source, tag);
        if (!Config.Compat.INSTANCE.firstAidCompat()) return result;
        return result || tag == DamageTypeTags.IS_PROJECTILE && source.is(ModDamageTypes.BULLETS_TAG);
    }
}
