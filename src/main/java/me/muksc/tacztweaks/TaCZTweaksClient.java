package me.muksc.tacztweaks;

import me.muksc.tacztweaks.client.input.ReduceSensitivityKey;
import me.muksc.tacztweaks.client.input.TiltGunKey;
import me.muksc.tacztweaks.client.input.UnloadKey;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.config.ConfigManager;
import me.muksc.tacztweaks.config.sync.ESyncDirection;
import me.muksc.tacztweaks.network.NetworkHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class TaCZTweaksClient implements ClientModInitializer {
    public static KeyMapping.Category CATEGORY;

    @Override
    public void onInitializeClient() {
        CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "general"));
        KeyBindingHelper.registerKeyBinding(UnloadKey.KEY);
        KeyBindingHelper.registerKeyBinding(TiltGunKey.KEY);
        KeyBindingHelper.registerKeyBinding(ReduceSensitivityKey.KEY);

        NetworkHandler.INSTANCE.registerClient();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            UnloadKey.onClientTick();
            TiltGunKey.onClientTick();
            CrawlPitchController.apply(client.player);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ConfigManager.INSTANCE.setSyncedWithServer(false);
            Config.INSTANCE.sync(ESyncDirection.RESET);
            CrawlPitchController.reset();
        });
    }
}
