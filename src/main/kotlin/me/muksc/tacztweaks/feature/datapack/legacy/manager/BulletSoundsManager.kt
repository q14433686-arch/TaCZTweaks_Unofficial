package me.muksc.tacztweaks.feature.datapack.legacy.manager

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.entity.EntityKineticBullet.EntityResult
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.core.anyOrEmpty
import me.muksc.tacztweaks.feature.datapack.legacy.BulletSounds
import me.muksc.tacztweaks.feature.raytracer.BulletHandler
import me.muksc.tacztweaks.mixininterface.feature.raytracer.RayTracingBullet
import me.muksc.tacztweaks.mixininterop.currentHitPosition
import me.muksc.tacztweaks.network.NetworkManager
import me.muksc.tacztweaks.network.message.ServerMessageAirspaceSounds
import me.muksc.tacztweaks.network.message.ServerMessageSoundPhysicsRequired
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

