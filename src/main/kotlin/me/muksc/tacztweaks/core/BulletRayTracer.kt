package me.muksc.tacztweaks.core

import com.tacz.guns.entity.EntityKineticBullet
import me.muksc.tacztweaks.data.manager.BulletInteractionManager
import me.muksc.tacztweaks.data.manager.BulletParticlesManager
import me.muksc.tacztweaks.data.manager.BulletSoundsManager
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

/**
 * Handles a single block hit during the bullet's ray-trace.
 *
 * Called from the {@code BlockRayTrace} mixin. When the matching bullet interaction says
 * "pierce", this returns null so the ray-tracer skips the block and keeps going; otherwise
 * the original hit result is returned and the bullet stops on the block as usual.
 *
 * Entity hits are deliberately NOT handled here — in the 26.2 refabricated port
 * {@code EntityKineticBullet#onBulletTick} already iterates entities and calls
 * {@code onHitEntity} itself (the entity mixin hooks that call instead).
 */
class BulletRayTracer(
    val entity: EntityKineticBullet,
    val level: ServerLevel
) {
    private val ext = entity as EntityKineticBulletExtension

    fun handle(original: BlockHitResult, state: BlockState): BlockHitResult? {
        ext.`tacztweaks$setPosition`(original.location)
        val interactionResult = BulletInteractionManager.handleBlockInteraction(entity, original, state)
        BulletParticlesManager.handleBlockParticle(interactionResult.toBlockParticleType(), level, entity, original, state)
        BulletSoundsManager.handleBlockSound(interactionResult.toBlockSoundType(), level, entity, original, state)
        return if (interactionResult.pierce) null else original
    }

    private fun BulletInteractionManager.InteractionResult.toBlockParticleType(): BulletParticlesManager.EBlockParticleType = when {
        condition -> BulletParticlesManager.EBlockParticleType.BREAK
        pierce -> BulletParticlesManager.EBlockParticleType.PIERCE
        else -> BulletParticlesManager.EBlockParticleType.HIT
    }

    private fun BulletInteractionManager.InteractionResult.toBlockSoundType(): BulletSoundsManager.EBlockSoundType = when {
        condition -> BulletSoundsManager.EBlockSoundType.BREAK
        pierce -> BulletSoundsManager.EBlockSoundType.PIERCE
        else -> BulletSoundsManager.EBlockSoundType.HIT
    }
}
