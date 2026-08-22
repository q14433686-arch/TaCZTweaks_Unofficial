package me.muksc.tacztweaks;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageSoundPhysicsRequired;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.core.BlockBreakingManager;
import me.muksc.tacztweaks.data.manager.BulletInteractionManager;
import me.muksc.tacztweaks.data.manager.BulletParticlesManager;
import me.muksc.tacztweaks.data.manager.BulletSoundsManager;
import me.muksc.tacztweaks.data.manager.MeleeInteractionManager;
import me.muksc.tacztweaks.mixin.accessor.InaccuracyTypeAccessor;
import me.muksc.tacztweaks.mixininterface.gun.SlideDataHolder;
import me.muksc.tacztweaks.network.NetworkHandler;
import me.muksc.tacztweaks.network.message.ClientMessageBroadcastSound;
import me.muksc.tacztweaks.network.message.ClientMessagePlayerShouldSlide;
import me.muksc.tacztweaks.registry.ModStatusEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TaCZ Tweaks — 非官方 NeoForge 1.21.11 移植。
 * 语义来源：姊妹 TaCZTweaks_Unofficial `1.21.11` 分支（冻结 commit 9d7d7b0）。
 */
@Mod(TaCZTweaks.MOD_ID)
public class TaCZTweaks {
    public static final String MOD_ID = "tacztweaks";
    /**
     * @deprecated Use {@link TaczVersionSupport#isSupportedTaczVersion(String)} for strict
     *             release-family validation. Retained as a display prefix only.
     */
    @Deprecated
    public static final String SUPPORTED_TACZ_VERSION_PREFIX =
            TaczVersionSupport.EXPECTED_CORE_VERSION + "+" + TaczVersionSupport.EXPECTED_FAMILY;
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("tacztweaks");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable("%s.%s".formatted(MOD_ID, key), args);
    }

    public TaCZTweaks(IEventBus modEventBus, ModContainer modContainer) {
        ModContainer tacz = ModList.get().getModContainerById("tacz")
                .orElseThrow(() -> new IllegalStateException("TaCZ is required"));
        String taczVersion = tacz.getModInfo().getVersion().toString();
        if (!TaczVersionSupport.isSupportedTaczVersion(taczVersion)) {
            throw new IllegalStateException(
                    "TaCZ Tweaks requires TaCZ " + TaczVersionSupport.expectedDisplay() + ", found " + taczVersion);
        }

        ModStatusEffects.INSTANCE.EFFECTS.register(modEventBus);
        modEventBus.addListener(NetworkHandler.INSTANCE::register);

        Config.INSTANCE.touch();
        TaCZTweaksClient.init(modEventBus, modContainer);

        NeoForge.EVENT_BUS.register(this);
    }

    /** 数据包重载监听（对应 Fabric 的 ResourceManagerHelper 注册 + END_DATA_PACK_RELOAD 钩子）。
     *  AddServerReloadListenersEvent 属游戏总线（NeoForge 21.x），经 EVENT_BUS 订阅。 */
    @SubscribeEvent
    public void onAddServerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(BulletInteractionManager.INSTANCE.id(), BulletInteractionManager.INSTANCE);
        event.addListener(BulletSoundsManager.INSTANCE.id(), BulletSoundsManager.INSTANCE);
        event.addListener(BulletParticlesManager.INSTANCE.id(), BulletParticlesManager.INSTANCE);
        event.addListener(MeleeInteractionManager.INSTANCE.id(), MeleeInteractionManager.INSTANCE);
        event.addListener(id("data_reload_hook"), new SimplePreparableReloadListener<Object>() {
            @Override
            protected Object prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return new Object();
            }

            @Override
            protected void apply(Object object, ResourceManager resourceManager, ProfilerFiller profiler) {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server == null || !BulletSoundsManager.INSTANCE.hasAirspaceSounds()) return;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    NetworkHandler.INSTANCE.sendS2C(player, ServerMessageSoundPhysicsRequired.INSTANCE);
                }
            }
        });
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            BlockBreakingManager.INSTANCE.onLevelTick(level);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ClientMessagePlayerShouldSlide.validateServerState(player);
        }
        BulletParticlesManager.INSTANCE.onServerTick(server);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        BlockBreakingManager.INSTANCE.clear();
        BulletParticlesManager.INSTANCE.clear();
        ClientMessageBroadcastSound.clearAll();
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BlockBreakingManager.INSTANCE.onBlockBreak(serverLevel, event.getPos());
        }
    }

    @SubscribeEvent
    public void onGunShoot(GunShootEvent event) {
        if (!Config.Gun.INSTANCE.disableUnderwater()) return;
        if (event.getShooter().isUnderWater()) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        NetworkHandler.INSTANCE.sendSyncConfig(player);
        if (BulletSoundsManager.INSTANCE.hasAirspaceSounds()) {
            NetworkHandler.INSTANCE.sendS2C(player, ServerMessageSoundPhysicsRequired.INSTANCE);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        ClientMessageBroadcastSound.clearPlayer(event.getEntity().getUUID());
    }

    public static List<InaccuracyType> getInaccuracyTypes(LivingEntity entity) {
        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        List<InaccuracyType> list = new ArrayList<>();
        if (InaccuracyTypeAccessor.isMove(entity)) list.add(InaccuracyType.MOVE);
        if (entity.getPose() == Pose.CROUCHING) list.add(InaccuracyType.SNEAK);
        if (entity.getPose() == Pose.SWIMMING && !entity.isSwimming()) list.add(InaccuracyType.LIE);
        if (operator.getSynAimingProgress() >= 1.0F) list.add(InaccuracyType.AIM);
        return list;
    }

    public static boolean isSpreadReducingTilt(LivingEntity entity) {
        if (!Config.Tweaks.INSTANCE.betterGunTilt()) return false;
        SlideDataHolder holder = (SlideDataHolder) entity;
        if (entity.level().isClientSide()) return holder.tacztweaks$getShouldSlide();
        if (!(entity instanceof ServerPlayer player)) return false;
        ClientMessagePlayerShouldSlide.validateServerState(player);
        return holder.tacztweaks$getShouldSlide();
    }

    public static float getBetterInaccuracy(Map<InaccuracyType, Float> map, LivingEntity entity) {
        List<InaccuracyType> inaccuracyTypes = getInaccuracyTypes(entity);
        float base = map.get(InaccuracyType.STAND);
        float inaccuracy = base;
        if (base > 0) {
            if (inaccuracyTypes.contains(InaccuracyType.MOVE)) inaccuracy *= map.get(InaccuracyType.MOVE) / base;
            if (inaccuracyTypes.contains(InaccuracyType.SNEAK)) inaccuracy *= map.get(InaccuracyType.SNEAK) / base;
            if (inaccuracyTypes.contains(InaccuracyType.LIE)) inaccuracy *= map.get(InaccuracyType.LIE) / base;
            if (inaccuracyTypes.contains(InaccuracyType.AIM)) inaccuracy *= map.get(InaccuracyType.AIM) / base;
            if (isSpreadReducingTilt(entity)) inaccuracy *= map.get(InaccuracyType.SNEAK) / base;
        } else {
            if (inaccuracyTypes.contains(InaccuracyType.MOVE)) inaccuracy += map.get(InaccuracyType.MOVE);
            if (inaccuracyTypes.contains(InaccuracyType.SNEAK)) inaccuracy += map.get(InaccuracyType.SNEAK);
            if (inaccuracyTypes.contains(InaccuracyType.LIE)) inaccuracy += map.get(InaccuracyType.LIE);
            if (inaccuracyTypes.contains(InaccuracyType.AIM)) inaccuracy += map.get(InaccuracyType.AIM);
            if (isSpreadReducingTilt(entity)) inaccuracy += map.get(InaccuracyType.SNEAK);
        }
        return inaccuracy;
    }
}
