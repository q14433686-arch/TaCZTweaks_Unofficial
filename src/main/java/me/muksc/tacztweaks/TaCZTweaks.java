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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
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
    /** Lowest supported TaCZ build; Fabric metadata remains pinned to this compile/test baseline. */
    public static final String SUPPORTED_TACZ_VERSION = "1.1.8+fabric.1.21.11.R2";
    private static final String SUPPORTED_TACZ_VERSION_PREFIX =
        "1.1.8+fabric.1.21.11.R";
    private static final BigInteger MIN_SUPPORTED_TACZ_REVISION = BigInteger.valueOf(2);
    private static final Pattern SUPPORTED_TACZ_VERSION_PATTERN = Pattern.compile(
        "^" + Pattern.quote(SUPPORTED_TACZ_VERSION_PREFIX)
            + "(\\d+)(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?$"
    );
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("tacztweaks");

    /**
     * Checks the friendly version string, including Minecraft/release-family identity.
     * Revision is numeric so R10 is correctly newer than R2.
     */
    public static boolean isSupportedTaczVersion(String version) {
        if (version == null) return false;
        Matcher matcher = SUPPORTED_TACZ_VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) return false;
        return new BigInteger(matcher.group(1)).compareTo(MIN_SUPPORTED_TACZ_REVISION) >= 0;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable("%s.%s".formatted(MOD_ID, key), args);
    }

    @Override
    public void onInitialize() {
        String taczVersion = FabricLoader.getInstance().getModContainer("tacz")
            .orElseThrow(() -> new IllegalStateException("TaCZ is required"))
            .getMetadata().getVersion().getFriendlyString();
        if (!isSupportedTaczVersion(taczVersion)) {
            throw new IllegalStateException(
                "TaCZ Tweaks requires TaCZ " + SUPPORTED_TACZ_VERSION
                    + " or a later R<n> build for the 1.21.11 release family, found "
                    + taczVersion
            );
        }

        Config.INSTANCE.touch();
        NetworkHandler.INSTANCE.registerServer();
        net.minecraft.core.Holder<?> endlessAmmo = ModStatusEffects.INSTANCE.ENDLESS_AMMO;

        BulletInteractionManager.INSTANCE.register();
        BulletSoundsManager.INSTANCE.register();
        BulletParticlesManager.INSTANCE.register();
        MeleeInteractionManager.INSTANCE.register();

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

        GunShootEvent.CALLBACK.register(event -> {
            if (!Config.Gun.INSTANCE.disableUnderwater()) return;
            if (event.getShooter().isUnderWater()) event.setCanceled(true);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            NetworkHandler.INSTANCE.sendSyncConfig(handler.getPlayer());
            if (BulletSoundsManager.INSTANCE.hasAirspaceSounds()) {
                NetworkHandler.INSTANCE.sendS2C(handler.getPlayer(), ServerMessageSoundPhysicsRequired.INSTANCE);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            ClientMessageBroadcastSound.clearPlayer(handler.getPlayer().getUUID()));
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
