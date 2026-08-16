package me.muksc.tacztweaks.mixin.feature.audio_and_visuals.visuals.cancel_inspection;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.gameplay.LocalPlayerInspect;
import com.tacz.guns.client.sound.SoundPlayManager;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerInspect.class, remap = false)
public abstract class LocalPlayerInspectMixin {
    @Shadow @Final private LocalPlayer player;

    @Unique
    private long tacztweaks$inspectTimestamp = -1L;

    /** Avoid binding to the compiler-generated display Optional lambda. */
    @Inject(method = "inspect", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$inspect$cancelInspection(CallbackInfo ci) {
        if (!Config.AudioAndVisuals.Visuals.cancelInspection()) return;
        long now = System.currentTimeMillis();
        if (now - tacztweaks$inspectTimestamp > 3_000L) {
            tacztweaks$inspectTimestamp = now;
            return;
        }

        TimelessAPI.getGunDisplay(player.getMainHandItem()).ifPresent(display -> {
            LuaAnimationStateMachine<GunAnimationStateContext> state = display.getAnimationStateMachine();
            if (state == null) return;
            SoundPlayManager.stopPlayGunSound();
            state.trigger("inspect_retreat");
            tacztweaks$inspectTimestamp = -1L;
            ci.cancel();
        });
    }
}
