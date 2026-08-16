package me.muksc.tacztweaks.mixin.feature.general.misc.dynamic_attachment_slots;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.gui.GunRefitScreen;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

@Mixin(value = GunRefitScreen.class, remap = false)
public abstract class GunRefitScreenMixin {
    @ModifyExpressionValue(method = "addAttachmentTypeButtons", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/attachment/AttachmentType;values()[Lcom/tacz/guns/api/item/attachment/AttachmentType;"))
    private AttachmentType[] tacztweaks$addAttachmentTypeButtons$dynamicAttachmentSlots(AttachmentType[] original) {
        if (!Config.General.Miscellaneous.dynamicAttachmentSlots()) return original;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return original;
        IGun gun = IGun.getIGunOrNull(player.getMainHandItem());
        if (gun == null) return original;
        return Arrays.stream(original)
            .filter(type -> gun.allowAttachmentType(player.getMainHandItem(), type))
            .toArray(AttachmentType[]::new);
    }
}
