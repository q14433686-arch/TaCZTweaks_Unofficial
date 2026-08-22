package me.muksc.tacztweaks;

import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.entity.IGunOperator;
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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaCZTweaks implements ModInitializer {
    public static final String MOD_ID = "tacztweaks";
    public static final String SUPPORTED_TACZ_VERSION = "1.1.8+fabric.26.2.R2";
    private static final String SUPPORTED_TACZ_VERSION_PREFIX = "1.1.8+fabric.26.2.R";
    private static final BigInteger MIN_SUPPORTED_TACZ_REVISION = BigInteger.valueOf(2);
    private static final Pattern SUPPORTED_TACZ_VERSION_PATTERN = Pattern.compile(
        "^" + Pattern.quote(SUPPORTED_TACZ_VERSION_PREFIX)
            + "(\\d+)(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?$"
    );
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("tacztweaks");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable("%s.%s".formatted(MOD_ID, key), args);
    }

    static boolean isSupportedTaczVersion(String version) {
        if (version == null) return false;
        Matcher matcher = SUPPORTED_TACZ_VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) return false;
        return new BigInteger(matcher.group(1)).compareTo(MIN_SUPPORTED_TACZ_REVISION) >= 0;
    }

    @Override
    public void onInitialize() {
        String taczVersion = FabricLoader.getInstance().getModContainer("tacz")
            .orElseThrow(() -> new IllegalStateException("TaCZ is required"))
            .getMetadata().getVersion().getFriendlyString();
        // Fabric's version predicates ignore the part after '+'. Keep the Minecraft,
        // TaCZ core version, release family and R2 minimum strict, while accepting R2,
        // R2-hotfix and later R<n> builds from the same release family.
        if (!isSupportedTaczVersion(taczVersion)) {
            throw new IllegalStateException(
                "TaCZ Tweaks requires TaCZ " + SUPPORTED_TACZ_VERSION
                    + " or a later R<n> build for Minecraft 26.2, found " + taczVersion
            );
        }
        Config.INSTANCE.initialize();
        NetworkHandler.INSTANCE.registerServer();
        // Force initialization of the ModStatusEffects object so the effect registers at startup.
        net.minecraft.core.Holder<?> endlessAmmo = ModStatusEffects.INSTANCE.ENDLESS_AMMO;

        // Register the data-driven bullet interaction / sound / particle loaders.
        BulletInteractionManager.INSTANCE.register();
        BulletSoundsManager.INSTANCE.register();
        BulletParticlesManager.INSTANCE.register();
        MeleeInteractionManager.INSTANCE.register();

        // Drive per-level state (block-breaking progress, delayed particle emitters).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                BlockBreakingManager.INSTANCE.onLevelTick(level);
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ClientMessagePlayerShouldSlide.validateServerState(player);
            }
            BulletParticlesManager.INSTANCE.onServerTick(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            BlockBreakingManager.INSTANCE.clear();
            BulletParticlesManager.INSTANCE.clear();
            ClientMessageBroadcastSound.clearAll();
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success || !BulletSoundsManager.INSTANCE.hasAirspaceSounds()) return;
            for (var player : server.getPlayerList().getPlayers()) {
                NetworkHandler.INSTANCE.sendS2C(player, ServerMessageSoundPhysicsRequired.INSTANCE);
            }
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel serverLevel) {
                BlockBreakingManager.INSTANCE.onBlockBreak(serverLevel, pos);
            }
        });

        // Disable shooting while underwater (server authoritative).
        GunShootEvent.CALLBACK.register(event -> {
            if (!Config.Gun.INSTANCE.disableUnderwater()) return;
            if (event.getShooter().isUnderWater()) event.setCanceled(true);
        });

        // Push server-authoritative config to players as they join.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkHandler.INSTANCE.sendSyncConfig(handler.getPlayer());
            if (BulletSoundsManager.INSTANCE.hasAirspaceSounds()) {
                NetworkHandler.INSTANCE.sendS2C(handler.getPlayer(), ServerMessageSoundPhysicsRequired.INSTANCE);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            ClientMessageBroadcastSound.clearPlayer(handler.getPlayer().getUUID()));
    }

    /**
     * Collects every inaccuracy "state" an entity is currently in, ordered by importance.
     * Mirrors the upstream TaCZ Tweaks implementation (uses {@code InaccuracyType#isMove}
     * via an invoker since it is package-private in the refabricated port).
     */
    public static List<InaccuracyType> getInaccuracyTypes(LivingEntity entity) {
        IGunOperator operator = IGunOperator.fromLivingEntity(entity);
        List<InaccuracyType> list = new ArrayList<>();
        if (InaccuracyTypeAccessor.isMove(entity)) list.add(InaccuracyType.MOVE);
        if (entity.getPose() == Pose.CROUCHING) list.add(InaccuracyType.SNEAK);
        if (entity.getPose() == Pose.SWIMMING && !entity.isSwimming()) list.add(InaccuracyType.LIE);
        if (operator.getSynAimingProgress() >= 1.0F) list.add(InaccuracyType.AIM);
        return list;
    }

    /**
     * Returns whether tilt may affect spread. Client state is only prediction/visuals;
     * server gameplay requires a recent request and re-validates the held gun each use.
     */
    public static boolean isSpreadReducingTilt(LivingEntity entity) {
        if (!Config.Tweaks.INSTANCE.betterGunTilt()) return false;
        SlideDataHolder holder = (SlideDataHolder) entity;
        if (entity.level().isClientSide()) return holder.tacztweaks$getShouldSlide();
        if (!(entity instanceof ServerPlayer player)) return false;
        ClientMessagePlayerShouldSlide.validateServerState(player);
        return holder.tacztweaks$getShouldSlide();
    }

    /**
     * The "better inaccuracy" calculation: combines the inaccuracy map multiplicatively
     * (or additively when the base is <= 0) across all active states.
     */
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
