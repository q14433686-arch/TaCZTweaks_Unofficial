package me.muksc.tacztweaks.mixin.feature.core.attribute;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.muksc.tacztweaks.core.attribute.BooleanAttribute;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.Holder;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Definition(id = "translatable", method = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;")
    @Expression("translatable('attribute.modifier.plus.' + ?, ?)")
    @WrapOperation(method = "addModifierTooltip", at = @At("MIXINEXTRAS:EXPRESSION"))
    private MutableComponent tacztweaks$addModifierTooltip$modifyTooltip$1(
        String key, Object[] args, Operation<MutableComponent> original,
        @Local(argsOnly = true) Holder<Attribute> attribute,
        @Local(argsOnly = true) AttributeModifier modifier
    ) {
        if (!(attribute.value() instanceof BooleanAttribute booleanAttribute)) return original.call(key, args);
        return booleanAttribute.toComponent(modifier);
    }

    @Definition(id = "translatable", method = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;")
    @Expression("translatable('attribute.modifier.take.' + ?, ?)")
    @WrapOperation(method = "addModifierTooltip", at = @At("MIXINEXTRAS:EXPRESSION"))
    private MutableComponent tacztweaks$addModifierTooltip$modifyTooltip$2(
        String key, Object[] args, Operation<MutableComponent> original,
        @Local(argsOnly = true) Holder<Attribute> attribute,
        @Local(argsOnly = true) AttributeModifier modifier
    ) {
        if (!(attribute.value() instanceof BooleanAttribute booleanAttribute)) return original.call(key, args);
        return booleanAttribute.toComponent(modifier);
    }
}
