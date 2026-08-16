package me.muksc.tacztweaks.mixin.feature.attribute.handling.shoot_while_sprinting;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.event.TickAnimationEvent;
import me.muksc.tacztweaks.core.extension.DeferredHolderExt;
import me.muksc.tacztweaks.registry.ModAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TickAnimationEvent.class, remap = false)
public abstract class TickAnimationEventMixin {
    //? if >=1.21.11 {
    /** Reproduce the small stable animation dispatcher instead of naming its Optional lambda. */
    /*@Inject(method = "tickAnimation(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), cancellable = true)
    private static void tacztweaks$tickAnimation$attribute$handling$shootWhileSprinting(Minecraft client, CallbackInfo ci) {
        LocalPlayer player = client.player;
        if (player == null) return;
        double value = player.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.SHOOT_WHILE_SPRINTING));
        if (value <= 0.0) return;

        ItemStack mainHandItem = player.getMainHandItem();
        TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
            var animationStateMachine = display.getAnimationStateMachine();
            if (animationStateMachine == null) return;
            if (player.input == null) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_IDLE);
                return;
            }
            boolean moving = player.input.getMoveVector().length() > 0.01;
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
            boolean aiming = operator.isAim();
            boolean running = player.isSprinting() && operator.getClientShootCoolDown() <= 0;
            if (!aiming && !player.isMovingSlowly() && running) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_RUN);
            } else if (moving && (aiming || !player.isMovingSlowly())) {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_WALK);
            } else {
                animationStateMachine.trigger(GunAnimationConstant.INPUT_IDLE);
            }
        });
        ci.cancel();
    }
    *///?} else {
    @SuppressWarnings("MixinExtrasOperationParameters") // MinecraftDev :(
    @WrapOperation(method = "lambda$tickAnimation$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z", remap = true))
    private static boolean tacztweaks$tickAnimation$attribute$handling$shootWhileSprinting$cancelSprintAnimation(LocalPlayer instance, Operation<Boolean> original) {
        double value = instance.getAttributeValue(DeferredHolderExt.valueOrDelegate(ModAttributes.SHOOT_WHILE_SPRINTING));
        if (value <= 0.0) return original.call(instance);
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(instance);
        return original.call(instance) && operator.getClientShootCoolDown() <= 0;
    }
    //?}
}
