package me.muksc.tacztweaks.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import me.muksc.tacztweaks.TaCZTweaks;
import me.muksc.tacztweaks.TaCZTweaksClient;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class UnloadKey {
    public static final KeyMapping KEY = new KeyMapping(
        TaCZTweaks.translatable("key.unload").getString(),
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_U,
        TaCZTweaksClient.CATEGORY
    );

    public static void onClientTick() {
        if (!Config.Gun.INSTANCE.allowUnload()) return;
        while (KEY.consumeClick()) {
            NetworkHandler.INSTANCE.sendPlayerUnload();
        }
    }
}
