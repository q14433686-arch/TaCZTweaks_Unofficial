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

/** Restores gunsmith safety checks and guards scrolling when recipes are absent. */
@Mixin(value = GunSmithTableScreen.class, remap = false)
public abstract class GunSmithTableScreenMixin extends AbstractContainerScreen<GunSmithTableMenu> {
    @Shadow
    private List<Identifier> selectedRecipeList;

    protected GunSmithTableScreenMixin(GunSmithTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @WrapOperation(
        method = "isSuitableForMainHand",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/IGun;allowAttachment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
            remap = true
        )
    )
    private boolean tacztweaks$isSuitableForMainHand$checkType(
        IGun gunApi,
        ItemStack gun,
        ItemStack attachment,
        Operation<Boolean> original
    ) {
        boolean allowed = original.call(gunApi, gun, attachment);
        IAttachment attachmentApi = IAttachment.getIAttachmentOrNull(attachment);
        return allowed && (attachmentApi == null || gunApi.allowAttachmentType(gun, attachmentApi.getType(attachment)));
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, remap = true)
    private void tacztweaks$mouseScrolled$avoidNullRecipes(
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (selectedRecipeList == null) {
            cir.setReturnValue(super.mouseScrolled(mouseX, mouseY, scrollX, scrollY));
        }
    }
}
