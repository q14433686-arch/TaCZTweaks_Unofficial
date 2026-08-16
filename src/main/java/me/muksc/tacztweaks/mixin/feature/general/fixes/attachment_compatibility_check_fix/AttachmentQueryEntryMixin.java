package me.muksc.tacztweaks.mixin.feature.general.fixes.attachment_compatibility_check_fix;

import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.compat.jei.entry.AttachmentQueryEntry;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = AttachmentQueryEntry.class, remap = false)
public abstract class AttachmentQueryEntryMixin {
    @Shadow @Final private ItemStack attachmentStack;
    @Shadow private List<ItemStack> allowGunStacks;

    /** Filter before dividedGuns splits the visible and overflow lists. */
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/compat/jei/entry/AttachmentQueryEntry;dividedGuns()V", shift = At.Shift.BEFORE))
    private void tacztweaks$init$attachmentCompatibilityCheckFix(CallbackInfo ci) {
        if (!Config.General.Fixes.attachmentCompatibilityCheckFix()) return;
        IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentStack);
        if (attachment == null) return;
        allowGunStacks.removeIf(gun -> {
            IGun iGun = IGun.getIGunOrNull(gun);
            return iGun != null && !iGun.allowAttachmentType(gun, attachment.getType(attachmentStack));
        });
    }
}
