package me.muksc.tacztweaks.mixin.feature.core.attribute;

//? if >=1.21.11 {
/*import me.muksc.tacztweaks.core.attribute.BooleanAttribute;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(targets = "net.minecraft.world.item.component.ItemAttributeModifiers$Display$Default")
public abstract class BooleanAttributeDisplayMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$apply$booleanAttributeTooltip(
        Consumer<Component> output,
        Player player,
        Holder<Attribute> attribute,
        AttributeModifier modifier,
        CallbackInfo ci
    ) {
        if (!(attribute.value() instanceof BooleanAttribute booleanAttribute)) return;
        output.accept(booleanAttribute.toComponent(modifier));
        ci.cancel();
    }
}
*///?} else {
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class BooleanAttributeDisplayMixin { }
//?}
