package me.muksc.tacztweaks.data.manager

import com.tacz.guns.entity.EntityKineticBullet
import me.muksc.tacztweaks.anyOrEmpty
import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageAirspaceSounds
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.data.BulletSounds
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension
import me.muksc.tacztweaks.network.NetworkHandler
import me.muksc.tacztweaks.thenPrioritizeBy
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import java.util.UUID

private val COMPARATOR = compareBy<BulletSounds> { it.priority }
    .thenPrioritizeBy { it.target.isNotEmpty() }
    .thenPrioritizeBy { when (it) {
        is BulletSounds.Block -> it.blocks.isNotEmpty()
        is BulletSounds.Entity -> it.entities.isNotEmpty()
        is BulletSounds.Whizz -> false
        is BulletSounds.Constant -> false
        is BulletSounds.AirSpace -> false
    } }

object BulletSoundsManager : BaseDataManager<BulletSounds>(
    "bullet_sounds", BulletSounds.CODEC, COMPARATOR
) {
    private const val MAX_AIRSPACE_CANDIDATES = 64
    private const val MAX_AIRSPACE_SOUNDS = 32

    override fun debugEnabled(): Boolean = Config.Debug.bulletSounds()

    private inline fun <reified T : BulletSounds> getSounds(entity: EntityKineticBullet, location: Vec3): List<Pair<Identifier, T>> =
        byType<T>().entries.filter { (_, sounds) ->
            sounds.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) }
        }.map { it.toPair() }

    private inline fun <reified T : BulletSounds> getSound(entity: EntityKineticBullet, location: Vec3): Pair<Identifier, T>? =
        byType<T>().entries.firstOrNull { (_, sounds) ->
            sounds.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) }
        }?.toPair()

    private inline fun <reified T : BulletSounds, E> getSound(
        entity: EntityKineticBullet,
        location: Vec3,
        selector: (T) -> List<E>,
        predicate: (E) -> Boolean
    ): Pair<Identifier, T>? = byType<T>().entries.firstOrNull { (_, sounds) ->
        sounds.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) } &&
            selector(sounds).anyOrEmpty(predicate)
    }?.toPair()

    fun handleBlockSound(type: EBlockSoundType, level: ServerLevel, entity: EntityKineticBullet, result: BlockHitResult, state: BlockState) {
        val (id, sounds) = getSound(entity, result.location, BulletSounds.Block::blocks) {
            it.test(level, result.blockPos, state)
        } ?: return
        logDebug { "Using block bullet sounds: $id" }
        for (sound in type.getSound(sounds)) {
            if (!sound.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(result.location)) }) continue
            if (!sound.blocks.anyOrEmpty { it.test(level, result.blockPos, state) }) continue
            if (!sound.isSafe()) continue
            sound.play(level, result.location, entity)
        }
    }

    fun handleEntitySound(type: EEntitySoundType, level: ServerLevel, entity: EntityKineticBullet, location: Vec3, target: Entity) {
        val (id, sounds) = getSound(entity, location, BulletSounds.Entity::entities) {
            it.test(target)
        } ?: return
        logDebug { "Using entity bullet sounds: $id" }
        for (sound in type.getSound(sounds)) {
            if (!sound.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) }) continue
            if (!sound.entities.anyOrEmpty { it.test(target) }) continue
            if (!sound.isSafe()) continue
            sound.play(level, location, entity)
        }
    }

    fun handleConstant(level: ServerLevel, entity: EntityKineticBullet) {
        val location = entity.position()
        val (id, sounds) = getSound<BulletSounds.Constant>(entity, location) ?: return
        logDebug { "Using constant bullet sounds: $id" }
        if (sounds.interval <= 0 || entity.tickCount % sounds.interval != 0) return
        for (sound in sounds.sounds) {
            if (!sound.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) }) continue
            if (!sound.isSafe()) continue
            sound.play(level, location, entity)
        }
    }

    fun handleSoundWhizz(level: ServerLevel, entity: EntityKineticBullet, ignores: Collection<ServerPlayer>, whizzedPlayers: MutableSet<UUID>) {
        for (player in level.server.playerList.players) {
            if (entity.getOwner() == player || player in ignores || player.uuid in whizzedPlayers) continue
            if (player.level().dimension() != level.dimension()) continue
            if (handleSoundWhizz(player, entity)) whizzedPlayers.add(player.uuid)
        }
    }

    fun hasAirspaceSounds(): Boolean = byType<BulletSounds.AirSpace>().isNotEmpty()

    fun logAirspace(msg: () -> String) {
        logDebug(msg)
    }

    fun handleAirspace(level: ServerLevel, entity: EntityKineticBullet) {
        val position = entity.position()
        if (!position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite()) return
        val soundsList = getSounds<BulletSounds.AirSpace>(entity, position)
            .take(MAX_AIRSPACE_CANDIDATES)
            .takeIf { it.isNotEmpty() } ?: return
        for (player in level.server.playerList.players) {
            if (player.level().dimension() != level.dimension()) continue
            val distance = player.position().distanceTo(position)
            if (!distance.isFinite()) continue
            val candidates = soundsList.mapNotNull { (id, sounds) ->
                val distanceSound = sounds.sounds.firstOrNull {
                    it.threshold.isFinite() && it.threshold >= 0.0 && distance <= it.threshold
                } ?: return@mapNotNull null
                val specs = distanceSound.sound.asSequence()
                    .filter { spec ->
                        spec.isSafe() && spec.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(position)) }
                    }
                    .take(MAX_AIRSPACE_SOUNDS)
                    .map { ServerMessageAirspaceSounds.SoundSpec(it.sound, it.volume, it.pitch, it.range) }
                    .toList()
                if (specs.isEmpty()) return@mapNotNull null
                logDebug { "Using airspace bullet sounds '$id' for player '$player'" }
                ServerMessageAirspaceSounds.AirspaceSound(
                    specs,
                    sounds.airspace.min.toNetworkFloat(),
                    sounds.airspace.max.toNetworkFloat(),
                    sounds.occlusion.min.toNetworkFloat(),
                    sounds.occlusion.max.toNetworkFloat(),
                    sounds.reflectivity.min.toNetworkFloat(),
                    sounds.reflectivity.max.toNetworkFloat()
                )
            }
            if (candidates.isEmpty()) continue
            NetworkHandler.sendS2C(player, ServerMessageAirspaceSounds(candidates, position.x, position.y, position.z))
        }
    }

    fun handleSoundWhizz(player: ServerPlayer, entity: EntityKineticBullet): Boolean {
        val ext = entity as EntityKineticBulletExtension
        val destination = ext.`tacztweaks$getPosition`()
        val (id, sounds) = getSound<BulletSounds.Whizz>(entity, destination) ?: return false
        logDebug { "Using whizz bullet sounds '$id' for player '$player'" }

        val currentPosition = entity.position()
        val playerPosition = player.eyePosition
        val trajectory = destination.subtract(currentPosition)
        val lengthSqr = trajectory.lengthSqr()
        if (!lengthSqr.isFinite() || lengthSqr <= 0) return false
        val length = playerPosition.subtract(currentPosition).dot(trajectory) / lengthSqr
        if (!length.isFinite() || length !in 0.0..1.0) return false

        val position = currentPosition.add(trajectory.scale(length))
        val distance = playerPosition.distanceTo(position)
        val whizz = sounds.sounds.firstOrNull { it.threshold.isFinite() && it.threshold >= 0.0 && distance <= it.threshold } ?: return false
        var played = false
        for (sound in whizz.sound) {
            if (!sound.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(destination)) }) continue
            if (!sound.isSafe()) continue
            sound.play(player, position, entity)
            played = true
        }
        return played
    }

    private fun BulletSounds.Sound.isSafe(): Boolean =
        volume.isFinite() && volume in 0.0F..4.0F &&
            pitch.isFinite() && pitch in 0.01F..4.0F &&
            (range == null || range.isFinite() && range in 0.01F..256.0F)

    private fun Double.toNetworkFloat(): Float = coerceIn(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble()).toFloat()

    @Suppress("DEPRECATION")
    private fun BulletSounds.Sound.play(player: ServerPlayer, position: Vec3, entity: EntityKineticBullet) {
        val soundEvent = if (range == null) SoundEvent.createVariableRangeEvent(sound) else SoundEvent.createFixedRangeEvent(sound, range)
        player.connection.send(ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent),
            entity.getSoundSource(),
            position.x,
            position.y,
            position.z,
            volume,
            pitch,
            player.random.nextLong()
        ))
    }

    private fun BulletSounds.Sound.play(level: ServerLevel, position: Vec3, entity: EntityKineticBullet) {
        val soundEvent = if (range == null) SoundEvent.createVariableRangeEvent(sound) else SoundEvent.createFixedRangeEvent(sound, range)
        level.playSound(null, position.x, position.y, position.z, soundEvent, entity.getSoundSource(), volume, pitch)
    }

    enum class EBlockSoundType(val getSound: (BulletSounds.Block) -> List<BulletSounds.Block.BlockSound>) {
        HIT(BulletSounds.Block::hit),
        PIERCE(BulletSounds.Block::pierce),
        BREAK(BulletSounds.Block::`break`)
    }

    enum class EEntitySoundType(val getSound: (BulletSounds.Entity) -> List<BulletSounds.Entity.EntitySound>) {
        HIT(BulletSounds.Entity::hit),
        PIERCE(BulletSounds.Entity::pierce),
        KILL(BulletSounds.Entity::kill)
    }
}
