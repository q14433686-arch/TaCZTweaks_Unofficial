package me.muksc.tacztweaks.data.manager

import com.mojang.brigadier.StringReader
import com.tacz.guns.entity.EntityKineticBullet
import me.muksc.tacztweaks.anyOrEmpty
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.data.BulletParticles
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension
import me.muksc.tacztweaks.thenPrioritizeBy
import net.minecraft.commands.arguments.ParticleArgument
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

private val COMPARATOR = compareBy<BulletParticles> { it.priority }
    .thenPrioritizeBy { it.target.isNotEmpty() }
    .thenPrioritizeBy { when (it) {
        is BulletParticles.Block -> it.blocks.isNotEmpty()
        is BulletParticles.Entity -> it.entities.isNotEmpty()
    } }

object BulletParticlesManager : BaseDataManager<BulletParticles>(
    "bullet_particles", BulletParticles.CODEC, COMPARATOR
) {
    private val emitters: MutableList<ParticleEmitter> = mutableListOf()

    override fun debugEnabled(): Boolean = Config.Debug.bulletParticles()

    private inline fun <reified T : BulletParticles, E> getParticle(
        entity: EntityKineticBullet,
        location: Vec3,
        selector: (T) -> List<E>,
        predicate: (E) -> Boolean
    ): Pair<Identifier, T>? = byType<T>().entries.firstOrNull { (_, particles) ->
        particles.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) }
                && selector(particles).anyOrEmpty(predicate)
    }?.toPair()

    fun onServerTick(server: MinecraftServer) {
        if (emitters.isEmpty()) return
        val iterator = emitters.iterator()
        while (iterator.hasNext()) {
            val emitter = iterator.next()
            emitter.remainingDuration -= 1
            val force = emitter.particle.force
            for (level in server.allLevels) {
                level.sendParticles(
                    emitter.options,
                    force,
                    force,
                    emitter.coordinates.x,
                    emitter.coordinates.y,
                    emitter.coordinates.z,
                    emitter.particle.count,
                    emitter.deltaCoordinates.x,
                    emitter.deltaCoordinates.y,
                    emitter.deltaCoordinates.z,
                    emitter.particle.speed
                )
            }
            if (emitter.remainingDuration <= 0) iterator.remove()
        }
    }

    fun handleBlockParticle(type: EBlockParticleType, level: ServerLevel, entity: EntityKineticBullet, result: BlockHitResult, state: BlockState) {
        val (id, particles) = getParticle(entity, result.location, BulletParticles.Block::blocks) {
            it.test(level, result.blockPos, state)
        } ?: return
        logDebug { "Using block bullet particles: $id" }
        for (particle in type.getParticle(particles)) {
            if (!particle.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(result.location)) }) continue
            if (!particle.blocks.anyOrEmpty { it.test(level, result.blockPos, state) }) continue
            particle.summon(level.server, entity, BuiltInRegistries.BLOCK.getKey(state.block)?.toString())
        }
    }

    fun handleEntityParticle(type: EEntityParticleType, level: ServerLevel, entity: EntityKineticBullet, location: Vec3, target: Entity) {
        val (id, particles) = getParticle(entity, location, BulletParticles.Entity::entities) {
            it.test(target)
        } ?: return
        logDebug { "Using entity bullet particles: $id" }
        for (particle in type.getParticle(particles)) {
            if (!particle.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) }) continue
            if (!particle.entities.anyOrEmpty { it.test(target) }) continue
            particle.summon(level.server, entity)
        }
    }

    private fun BulletParticles.Particle.summon(server: MinecraftServer, entity: EntityKineticBullet, context: String? = null) {
        val ext = entity as EntityKineticBulletExtension
        val base = ext.`tacztweaks$getPosition`()
        val particleString = if (context != null) particle.format(context) else particle
        val reader = StringReader(particleString)
        val particleOptions: ParticleOptions = try {
            ParticleArgument.readParticle(reader, entity.registryAccess())
        } catch (e: Exception) {
            // 26.2 把粒子参数改成了 SNBT/codec 格式（如 minecraft:block{block_state:"..."}），
            // 旧语法或第三方包的错误语法在这里解析失败。粒子只是视觉效果，解析失败
            // 绝不能拖垮实体 tick —— 记日志并跳过即可。
            logger.error("Failed to parse bullet particle '{}': {}", particleString, e.message)
            return
        }

        val coordinates = when (position.type) {
            BulletParticles.Particle.Coordinates.ECoordinatesType.ABSOLUTE -> Vec3(position.x, position.y, position.z)
            BulletParticles.Particle.Coordinates.ECoordinatesType.RELATIVE -> base.add(position.x, position.y, position.z)
            BulletParticles.Particle.Coordinates.ECoordinatesType.LOCAL -> base
        }
        val deltaCoordinates = Vec3(delta.x, delta.y, delta.z)

        emitters.add(ParticleEmitter(this, particleOptions, coordinates, deltaCoordinates, duration))
    }

    enum class EBlockParticleType(val getParticle: (BulletParticles.Block) -> List<BulletParticles.Block.BlockParticle>) {
        HIT(BulletParticles.Block::hit),
        PIERCE(BulletParticles.Block::pierce),
        BREAK(BulletParticles.Block::`break`)
    }

    enum class EEntityParticleType(val getParticle: (BulletParticles.Entity) -> List<BulletParticles.Entity.EntityParticle>) {
        HIT(BulletParticles.Entity::hit),
        PIERCE(BulletParticles.Entity::pierce),
        KILL(BulletParticles.Entity::kill)
    }

    private class ParticleEmitter(
        val particle: BulletParticles.Particle,
        val options: ParticleOptions,
        val coordinates: Vec3,
        val deltaCoordinates: Vec3,
        var remainingDuration: Int
    )
}
