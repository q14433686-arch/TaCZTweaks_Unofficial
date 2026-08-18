package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.inventory.GunSmithTableMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 26.2: {@code mouseScrolled} grew a fourth double (discrete scroll X).
 * {@code selectedRecipeList} is now {@code List<Identifier>}.
 */
@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableScreenMixin extends AbstractContainerScreen<GunSmithTableMenu> {
    public GunSmithTableScreenMixin(GunSmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Shadow private List<Identifier> selectedRecipeList;

    @WrapOperation(method = "isSuitableForMainHand", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;allowAttachment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean tacztweaks$isSuitableForMainHand$attachmentCheckFix(IGun instance, ItemStack gun, ItemStack attachmentItem, Operation<Boolean> original) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        boolean allowAttachment = original.call(instance, gun, attachmentItem);
        if (iAttachment == null) return allowAttachment;
        return allowAttachment && instance.allowAttachmentType(gun, iAttachment.getType(attachmentItem));
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, remap = true)
    private void tacztweaks$mouseScrolled$fixNPE(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (selectedRecipeList != null) return;
        cir.setReturnValue(super.mouseScrolled(mouseX, mouseY, scrollX, scrollY));
    }
}
