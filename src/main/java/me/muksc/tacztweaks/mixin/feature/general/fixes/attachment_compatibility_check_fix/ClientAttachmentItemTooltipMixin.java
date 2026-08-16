package me.muksc.tacztweaks.mixin.feature.general.fixes.attachment_compatibility_check_fix;

import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import me.muksc.tacztweaks.config.Config;
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ClientAttachmentItemTooltip.class, remap = false)
public abstract class ClientAttachmentItemTooltipMixin {
    @Inject(method = "getAllAllowGuns", at = @At("RETURN"))
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    private static void tacztweaks$getAllAllowGuns$attachmentCompatibilityCheckFix(List<ItemStack> output, ResourceLocation attachmentId, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!Config.General.Fixes.attachmentCompatibilityCheckFix()) return;
        ItemStack attachmentStack = AttachmentItemBuilder.create().setId(attachmentId).build();
        IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (attachment == null) return;
        output.removeIf(gun -> {
            IGun iGun = IGun.getIGunOrNull(gun);
            return iGun != null && !iGun.allowAttachmentType(gun, attachment.getType(attachmentStack));
        });
    }
}
