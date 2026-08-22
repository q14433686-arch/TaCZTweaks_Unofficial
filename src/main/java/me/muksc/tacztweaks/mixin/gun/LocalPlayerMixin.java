package me.muksc.tacztweaks.mixin.gun;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.resource.GunDisplayInstance;
import me.muksc.tacztweaks.mixininterface.gun.SlideDataHolder;
import me.muksc.tacztweaks.network.NetworkHandler;
import me.muksc.tacztweaks.network.message.ClientMessagePlayerShouldSlide;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Detects the "tilt gun" (slide) state every client tick and keeps the server informed via
 * {@link ClientMessagePlayerShouldSlide}. The {@code shouldSlide} flag itself lives on
 * {@code LivingEntity} (see {@code gun.LivingEntityMixin}); this mixin only declares the
 * {@link SlideDataHolder} interface for the cast and publishes state changes.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements SlideDataHolder {
    @Inject(method = "tick", at = @At("TAIL"))
    private void tacztweaks$tick$tiltCheck(CallbackInfo ci) {
        Supplier<Boolean> supplier = () -> {
            LocalPlayer player = LocalPlayer.class.cast(this);
            if (!IGun.mainHandHoldGun(player)) return false;
            GunDisplayInstance display = TimelessAPI.getGunDisplay(player.getMainHandItem()).orElse(null);
            if (display == null) return false;
            LuaAnimationStateMachine<GunAnimationStateContext> state = display.getAnimationStateMachine();
            if (state == null) return false;
            GunAnimationStateContext context = state.getContext();
            if (context == null) return false;
            return context.shouldSlide();
        };
        boolean shouldSlide = supplier.get();
        if (shouldSlide != tacztweaks$getShouldSlide()) NetworkHandler.INSTANCE.sendC2S(ClientMessagePlayerShouldSlide.create(shouldSlide));
        tacztweaks$setShouldSlide(shouldSlide);
    }
}
