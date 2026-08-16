package me.muksc.tacztweaks.mixin.feature.attribute;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import me.muksc.tacztweaks.feature.attribute.AttributeManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AttachmentPropertyManager.class, remap = false)
public abstract class AttachmentPropertyManagerMixin {
    /** Consume the final installed cache instead of binding to the Optional lambda. */
    @Inject(method = "postChangeEvent", at = @At("RETURN"))
    private static void tacztweaks$postChangeEvent$attribute$onPropertyUpdated(LivingEntity shooter, ItemStack gunItem, CallbackInfo ci) {
        if (!(gunItem.getItem() instanceof IGun)) return;
        AttributeManager.INSTANCE.onPropertyUpdated(
            shooter,
            gunItem,
            IGunOperator.fromLivingEntity(shooter).getCacheProperty()
        );
    }

    @Inject(method = "postChangeEvent", at = @At("HEAD"))
    private static void tacztweaks$postChangeEvent$attribute$onPropertyReset(LivingEntity shooter, ItemStack gunItem, CallbackInfo ci) {
        if (gunItem.getItem() instanceof IGun) return;
        AttributeManager.INSTANCE.onPropertyUpdated(shooter, gunItem, null);
    }
}
