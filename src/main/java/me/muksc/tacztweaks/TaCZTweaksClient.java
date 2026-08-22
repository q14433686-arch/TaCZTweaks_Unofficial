package me.muksc.tacztweaks;

import me.muksc.tacztweaks.client.CrawlPitchController;
import me.muksc.tacztweaks.client.config.ConfigScreenFactory;
import me.muksc.tacztweaks.client.input.ReduceSensitivityKey;
import me.muksc.tacztweaks.client.input.TiltGunKey;
import me.muksc.tacztweaks.client.input.UnloadKey;
import me.muksc.tacztweaks.client.sound.MonoConversion;
import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsCompat;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.config.ConfigManager;
import me.muksc.tacztweaks.config.sync.ESyncDirection;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Physical-client entry point. Keeping this in a dist-scoped {@link Mod} entry prevents the
 * common entry point from resolving client-only classes on a dedicated server. TaCZ: Renovated
 * 26.2 uses the same split between {@code GunMod} and its client-only mod entry.
 */
@Mod(value = TaCZTweaks.MOD_ID, dist = Dist.CLIENT)
public final class TaCZTweaksClient {
    public static KeyMapping.Category CATEGORY;

    public TaCZTweaksClient(IEventBus modEventBus, ModContainer modContainer) {
        CATEGORY = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "general"));
        modEventBus.addListener(TaCZTweaksClient::onRegisterKeyMappings);
        // NeoForge's mod-list config button replaces the Fabric Mod Menu entry point.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, screen) -> ConfigScreenFactory.generateConfigScreen(screen));
        NeoForge.EVENT_BUS.register(TaCZTweaksClient.class);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(UnloadKey.KEY);
        event.register(TiltGunKey.KEY);
        event.register(ReduceSensitivityKey.KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        UnloadKey.onClientTick();
        TiltGunKey.onClientTick();
        CrawlPitchController.apply(client.player);
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ConfigManager.INSTANCE.setSyncedWithServer(false);
        Config.INSTANCE.sync(ESyncDirection.RESET);
        MonoConversion.INSTANCE.clear();
        SoundPhysicsCompat.INSTANCE.clearAll();
        CrawlPitchController.reset();
    }
}
