package me.muksc.tacztweaks.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import me.muksc.tacztweaks.TaCZTweaks;
import me.muksc.tacztweaks.TaCZTweaksClient;
import net.minecraft.client.KeyMapping;

public class ReduceSensitivityKey {
    public static final KeyMapping KEY = new KeyMapping(
        TaCZTweaks.translatable("key.reduceSensitivity").getString(),
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        TaCZTweaksClient.CATEGORY
    );
}
