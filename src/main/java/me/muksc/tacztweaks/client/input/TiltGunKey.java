package me.muksc.tacztweaks.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.resource.GunDisplayInstance;
import me.muksc.tacztweaks.TaCZTweaks;
import me.muksc.tacztweaks.TaCZTweaksClient;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class TiltGunKey {
    public static final KeyMapping KEY = new KeyMapping(
        TaCZTweaks.translatable("key.tiltGun").getString(),
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        TaCZTweaksClient.CATEGORY
    );

    public static boolean isActive(LocalPlayer player) {
        if (!KEY.isDown() || !IGun.mainHandHoldGun(player)) return false;
        GunDisplayInstance display = TimelessAPI.getGunDisplay(player.getMainHandItem()).orElse(null);
        if (display == null) return false;
        LuaAnimationStateMachine<GunAnimationStateContext> state = display.getAnimationStateMachine();
        if (state == null) return false;
        GunAnimationStateContext context = state.getContext();
        if (context == null) return false;
        return context.shouldSlide();
    }

    public static void onClientTick() {
        if (!Config.Gun.INSTANCE.tiltGunKeyCancelsSprint()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !isActive(player)) return;
        player.setSprinting(false);
    }
}
