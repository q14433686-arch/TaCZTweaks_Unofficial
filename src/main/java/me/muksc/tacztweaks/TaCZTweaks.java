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
import me.muksc.tacztweaks.registry.ModStatusEffects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaCZTweaks implements ModInitializer {
    public static final String MOD_ID = "tacztweaks";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("tacztweaks");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable("%s.%s".formatted(MOD_ID, key), args);
    }

    @Override
    public void onInitialize() {
        Config.INSTANCE.touch();
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
            BulletParticlesManager.INSTANCE.onServerTick(server);
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
            if (Config.Tweaks.INSTANCE.betterGunTilt() && ((SlideDataHolder) entity).tacztweaks$getShouldSlide()) inaccuracy *= map.get(InaccuracyType.SNEAK) / base;
        } else {
            if (inaccuracyTypes.contains(InaccuracyType.MOVE)) inaccuracy += map.get(InaccuracyType.MOVE);
            if (inaccuracyTypes.contains(InaccuracyType.SNEAK)) inaccuracy += map.get(InaccuracyType.SNEAK);
            if (inaccuracyTypes.contains(InaccuracyType.LIE)) inaccuracy += map.get(InaccuracyType.LIE);
            if (inaccuracyTypes.contains(InaccuracyType.AIM)) inaccuracy += map.get(InaccuracyType.AIM);
            if (Config.Tweaks.INSTANCE.betterGunTilt() && ((SlideDataHolder) entity).tacztweaks$getShouldSlide()) inaccuracy += map.get(InaccuracyType.SNEAK);
        }
        return inaccuracy;
    }
}
