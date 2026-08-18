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
import net.minecraft.world.level.Level
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
    private const val MAX_EMITTERS = 1_024
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
            val level = server.getLevel(emitter.dimension)
            if (level == null) {
                iterator.remove()
                continue
            }
            val force = emitter.particle.force
            level.sendParticles(
                emitter.options,
                force,
                force,
                emitter.coordinates.x,
                emitter.coordinates.y,
                emitter.coordinates.z,
                emitter.particle.count.coerceIn(0, 4096),
                emitter.deltaCoordinates.x,
                emitter.deltaCoordinates.y,
                emitter.deltaCoordinates.z,
                emitter.particle.speed
            )
            emitter.remainingDuration -= 1
            if (emitter.remainingDuration <= 0) iterator.remove()
        }
    }

    fun clear() {
        emitters.clear()
    }

    fun handleBlockParticle(type: EBlockParticleType, level: ServerLevel, entity: EntityKineticBullet, result: BlockHitResult, state: BlockState) {
        val (id, particles) = getParticle(entity, result.location, BulletParticles.Block::blocks) {
            it.test(level, result.blockPos, state)
        } ?: return
        logDebug { "Using block bullet particles: $id" }
        for (particle in type.getParticle(particles)) {
            if (!particle.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(result.location)) }) continue
            if (!particle.blocks.anyOrEmpty { it.test(level, result.blockPos, state) }) continue
            particle.summon(level, entity, BuiltInRegistries.BLOCK.getKey(state.block).toString())
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
            particle.summon(level, entity)
        }
    }

    private fun BulletParticles.Particle.summon(level: ServerLevel, entity: EntityKineticBullet, context: String? = null) {
        if (emitters.size >= MAX_EMITTERS) {
            logDebug { "Dropping bullet particle emitter because the cap ($MAX_EMITTERS) was reached" }
            return
        }
        if (!speed.isFinite() || speed !in 0.0..64.0 || count !in 0..4096 || duration !in 1..1200) return
        if (!position.hasFiniteComponents() || !delta.hasFiniteComponents()) return

        val ext = entity as EntityKineticBulletExtension
        val base = ext.`tacztweaks$getPosition`()
        if (!base.hasFiniteComponents()) return
        // Only %s is a supported context token. String.format accepted arbitrary format
        // directives and could throw outside the parser's exception boundary.
        val particleString = if (context != null) particle.replace("%s", context) else particle
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

        val velocity = entity.deltaMovement
        val forward = when {
            velocity.lengthSqr() > 1.0E-8 -> velocity.normalize()
            entity.lookAngle.lengthSqr() > 1.0E-8 -> entity.lookAngle.normalize()
            else -> Vec3(0.0, 0.0, 1.0)
        }
        var referenceUp = Vec3(0.0, 1.0, 0.0)
        if (kotlin.math.abs(forward.dot(referenceUp)) > 0.999) referenceUp = Vec3(0.0, 0.0, 1.0)
        val left = referenceUp.cross(forward).normalize()
        val up = forward.cross(left).normalize()

        fun resolve(value: BulletParticles.Particle.Coordinates, origin: Vec3?): Vec3 = when (value.type) {
            BulletParticles.Particle.Coordinates.ECoordinatesType.ABSOLUTE -> Vec3(value.x, value.y, value.z)
            BulletParticles.Particle.Coordinates.ECoordinatesType.RELATIVE ->
                (origin ?: Vec3.ZERO).add(value.x, value.y, value.z)
            BulletParticles.Particle.Coordinates.ECoordinatesType.LOCAL ->
                (origin ?: Vec3.ZERO)
                    .add(left.scale(value.x))
                    .add(up.scale(value.y))
                    .add(forward.scale(value.z))
        }

        val coordinates = resolve(position, base)
        // Delta is a vector, so relative/local values are resolved around zero rather than
        // accidentally adding the world-space hit position.
        val deltaCoordinates = resolve(delta, null)
        if (!coordinates.hasFiniteComponents() || !deltaCoordinates.hasFiniteComponents()) return

        emitters.add(ParticleEmitter(
            this,
            particleOptions,
            level.dimension(),
            coordinates,
            deltaCoordinates,
            duration.coerceIn(1, 1200)
        ))
    }

    private fun BulletParticles.Particle.Coordinates.hasFiniteComponents(): Boolean =
        x.isFinite() && y.isFinite() && z.isFinite()

    private fun Vec3.hasFiniteComponents(): Boolean =
        x.isFinite() && y.isFinite() && z.isFinite()

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
        val dimension: net.minecraft.resources.ResourceKey<Level>,
        val coordinates: Vec3,
        val deltaCoordinates: Vec3,
        var remainingDuration: Int
    )
}
