package me.muksc.tacztweaks.data.manager

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.particles.BulletHoleOption
import com.tacz.guns.util.AttachmentDataUtils
import me.muksc.tacztweaks.anyOrEmpty
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.core.BlockBreakingManager
import me.muksc.tacztweaks.core.Context
import me.muksc.tacztweaks.core.SafeMath
import me.muksc.tacztweaks.data.BulletInteraction
import me.muksc.tacztweaks.data.old.convert
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension
import me.muksc.tacztweaks.thenPrioritizeBy
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import me.muksc.tacztweaks.data.old.BulletInteraction as OldBulletInteraction

internal val BULLET_INTERACTION_CODEC: Codec<BulletInteraction> =
    Codec.either(BulletInteraction.CODEC, OldBulletInteraction.CODEC).xmap(
        { value: Either<BulletInteraction, OldBulletInteraction> -> value.map({ it }, { it.convert() }) },
        { value: BulletInteraction -> Either.left<BulletInteraction, OldBulletInteraction>(value) }
    )

private val COMPARATOR = compareBy<BulletInteraction> { it.priority }
    .thenPrioritizeBy { it.target.isNotEmpty() }
    .thenPrioritizeBy { when (it) {
        is BulletInteraction.Block -> it.blocks.isNotEmpty()
        is BulletInteraction.Entity -> it.entities.isNotEmpty()
    } }

/**
 * Loads `bullet_interactions` JSON files (glass piercing, dripstone breaking, custom
 * entity damage, …).
 */
object BulletInteractionManager : BaseDataManager<BulletInteraction>(
    "bullet_interactions", BULLET_INTERACTION_CODEC, COMPARATOR
) {
    init { BulletInteraction }

    override fun debugEnabled(): Boolean = Config.Debug.bulletInteractions()

    private val DEFAULT = Identifier.fromNamespaceAndPath("tacztweaks", "default")

    private inline fun <reified T : BulletInteraction, E> getBulletInteraction(
        entity: EntityKineticBullet,
        location: Vec3,
        selector: (T) -> List<E>,
        predicate: (E) -> Boolean
    ): Pair<Identifier, T>? = byType<T>().entries.firstOrNull { (_, interaction) ->
        interaction.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) }
                && selector(interaction).anyOrEmpty(predicate)
    }?.toPair()

    private inline fun <reified T : BulletInteraction> getBulletInteraction(
        entity: EntityKineticBullet,
        location: Vec3,
        predicate: (T) -> Boolean
    ): Pair<Identifier, T>? = byType<T>().entries.firstOrNull { (_, interaction) ->
        interaction.target.anyOrEmpty { it.test(entity, entity.getGunId(), entity.getDamage(location)) }
                && predicate(interaction)
    }?.toPair()

    fun handleBlockInteraction(ammo: EntityKineticBullet, result: BlockHitResult, state: BlockState): InteractionResult {
        val level = ammo.level() as ServerLevel
        val blockPos = result.blockPos
        val ext = ammo as EntityKineticBulletExtension
        val (id, interaction) = getBulletInteraction(ammo, result.location, BulletInteraction.Block::blocks) {
            it.test(level, blockPos, state)
        } ?: (DEFAULT to BulletInteraction.Block.DEFAULT)
        logDebug { "Using block bullet interaction: $id" }

        val breakBlock = run {
            val hardness = state.getDestroySpeed(level, blockPos)
            if (hardness !in interaction.blockBreak.hardness) return@run false

            val gun = Context.Gun(ext.`tacztweaks$getGunStack`())
            val gunStack = gun.stack
            val gunIndex = gun.index
            val armorIgnore = if (gunStack == null || gunIndex == null || gunIndex.gunData == null) 0.0
            else AttachmentDataUtils.getArmorIgnoreWithAttachment(gunStack, gunIndex.gunData)
            when (interaction.blockBreak) {
                is BulletInteraction.Block.BlockBreak.Never -> false
                is BulletInteraction.Block.BlockBreak.Instant -> true
                is BulletInteraction.Block.BlockBreak.Count -> {
                    val delta = BlockBreakingManager.addCurrentProgress(level, blockPos, 1.0F / interaction.blockBreak.count)
                    delta >= 1.0F
                }
                is BulletInteraction.Block.BlockBreak.FixedDamage -> {
                    var delta = calcBlockBreakingDelta(interaction.blockBreak.damage, armorIgnore, state, level, blockPos)
                    if (interaction.blockBreak.accumulate) delta = BlockBreakingManager.addCurrentProgress(level, blockPos, delta)
                    delta >= 1.0F
                }
                is BulletInteraction.Block.BlockBreak.DynamicDamage -> {
                    val damage = interaction.blockBreak.run { (ammo.getDamage(result.location) + modifier) * multiplier }
                    var delta = calcBlockBreakingDelta(damage, armorIgnore, state, level, blockPos)
                    if (interaction.blockBreak.accumulate) delta = BlockBreakingManager.addCurrentProgress(level, blockPos, delta)
                    delta >= 1.0F
                }
            }
        }
        if (breakBlock) run {
            val owner = ammo.getOwner()
            level.destroyBlock(blockPos, interaction.blockBreak.drop, owner)
            val replaceWith = interaction.blockBreak.replaceWith
            if (!replaceWith.state.isAir && replaceWith.place(level, blockPos, Block.UPDATE_CLIENTS)) {
                level.sendBlockUpdated(blockPos, replaceWith.state, replaceWith.state, Block.UPDATE_CLIENTS)
            }
        }
        val pierce = shouldPierce(
            ammo, result, interaction.pierce, breakBlock,
            ext::`tacztweaks$incrementBlockPierce`, ext::`tacztweaks$getBlockPierce`
        )
        if (pierce && !breakBlock && interaction.pierce.renderBulletHole) {
            val bulletHoleOption = BulletHoleOption(
                result.direction,
                blockPos,
                ammo.getAmmoId().toString(),
                ammo.getGunId().toString(),
                ammo.getGunDisplayId().toString()
            )
            level.sendParticles(bulletHoleOption, result.location.x, result.location.y, result.location.z, 1, 0.0, 0.0, 0.0, 0.0)
        }
        return InteractionResult(pierce, breakBlock).also { logDebug { it.toString() } }
    }

    fun getEntityDamage(ammo: EntityKineticBullet, location: Vec3, entity: Entity): Pair<Float, Float>? {
        val (id, interaction) = getBulletInteraction(ammo, location, BulletInteraction.Entity::entities) {
            it.test(entity)
        } ?: return null
        logDebug { "Using entity bullet interaction: $id" }
        return interaction.damage.modifier to interaction.damage.multiplier
    }

    private fun shouldPierce(
        ammo: EntityKineticBullet,
        result: HitResult,
        pierce: BulletInteraction.Pierce,
        condition: Boolean,
        incrementCustomPierce: () -> Unit,
        getCustomPierce: () -> Int
    ): Boolean {
        when (pierce) {
            is BulletInteraction.Pierce.Never -> return false
            is BulletInteraction.Pierce.Default -> {}
            is BulletInteraction.Pierce.Count -> {
                incrementCustomPierce.invoke()
                if (getCustomPierce.invoke() >= pierce.count) return false
            }
            is BulletInteraction.Pierce.Damage -> if (ammo.getDamage(result.location) <= 0.0F) return false
        }
        if (pierce.conditional && !condition) return false
        val ext = ammo as EntityKineticBulletExtension
        ext.`tacztweaks$addDamageModifier`(-pierce.damageFalloff, pierce.damageMultiplier)
        return true
    }

    /** Maps bullet damage into bounded vanilla-style block-breaking progress. */
    fun calcBlockBreakingDelta(
        damage: Float,
        armorIgnore: Double,
        state: BlockState,
        level: ServerLevel,
        pos: BlockPos
    ): Float = SafeMath.blockBreakingDelta(damage, armorIgnore, state.getDestroySpeed(level, pos))

    data class InteractionResult(
        val pierce: Boolean,
        val condition: Boolean
    )
}
