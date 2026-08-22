package me.muksc.tacztweaks;

import me.muksc.tacztweaks.client.CrawlPitchController;
import me.muksc.tacztweaks.client.input.ReduceSensitivityKey;
import me.muksc.tacztweaks.client.input.TiltGunKey;
import me.muksc.tacztweaks.client.input.UnloadKey;
import me.muksc.tacztweaks.client.sound.MonoConversion;
import me.muksc.tacztweaks.compat.soundphysics.SoundPhysicsCompat;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.config.ConfigManager;
import me.muksc.tacztweaks.config.sync.ESyncDirection;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 客户端侧初始化（NeoForge 无 ClientModInitializer；由 {@link TaCZTweaks} 在
 * Dist.CLIENT 下显式调用——与 TaCZ-Renovated 的 GunModClient 同模式）。
 */
public final class TaCZTweaksClient {
    public static KeyMapping.Category CATEGORY;

    private TaCZTweaksClient() {
    }

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        if (!FMLLoader.getCurrent().getDist().isClient()) {
            return;
        }
        CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "general"));
        modEventBus.addListener(TaCZTweaksClient::onRegisterKeyMappings);
        // YACL 配置屏：21.11 无 RegisterConfigScreenEvent，走 ModContainer 扩展点
        // （等价于姊妹 Fabric 侧的 ModMenuApiImpl）。
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (container, screen) -> Config.INSTANCE.generateConfigScreen(screen));
        NeoForge.EVENT_BUS.register(TaCZTweaksClient.class);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(UnloadKey.KEY);
        event.register(TiltGunKey.KEY);
        event.register(ReduceSensitivityKey.KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        UnloadKey.onClientTick();
        TiltGunKey.onClientTick();
        CrawlPitchController.apply(net.minecraft.client.Minecraft.getInstance().player);
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
