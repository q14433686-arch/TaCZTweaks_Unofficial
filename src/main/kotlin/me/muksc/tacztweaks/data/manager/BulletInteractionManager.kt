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
import me.muksc.tacztweaks.core.ProtectedBlockBreaking
import me.muksc.tacztweaks.core.SafeMath
import me.muksc.tacztweaks.data.BulletInteraction
import me.muksc.tacztweaks.data.old.convert
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension
import me.muksc.tacztweaks.data.old.BulletInteraction as OldBulletInteraction
import me.muksc.tacztweaks.thenPrioritizeBy
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

internal val BULLET_INTERACTION_CODEC: Codec<BulletInteraction> =
    Codec.either(BulletInteraction.CODEC, OldBulletInteraction.CODEC).xmap(
        { value: Either<BulletInteraction, OldBulletInteraction> ->
            value.map({ it }, { it.convert() })
        },
        { value: BulletInteraction -> Either.left<BulletInteraction, OldBulletInteraction>(value) }
    )

private val COMPARATOR = compareBy<BulletInteraction> { it.priority }
    .thenPrioritizeBy { it.target.isNotEmpty() }
    .thenPrioritizeBy { when (it) {
        is BulletInteraction.Block -> it.blocks.isNotEmpty()
        is BulletInteraction.Entity -> it.entities.isNotEmpty()
        is BulletInteraction.Shield -> it.predicate.isPresent
    } }

/**
 * Loads `bullet_interactions` JSON files (glass piercing, dripstone breaking, custom
 * entity damage, …).
 *
 * Block, entity and shield behavior is active. Entity rules cooperate with R2's native
 * hit loop while preserving custom gun-pierce consumption and persistent damage falloff.
 */
object BulletInteractionManager : BaseDataManager<BulletInteraction>(
    "bullet_interactions", BULLET_INTERACTION_CODEC, COMPARATOR
) {
    override fun debugEnabled(): Boolean = Config.Debug.bulletInteractions()

    /** Whether a data rule can pierce entities without requiring the gun's native pierce. */
    fun needsExtendedEntityTrace(): Boolean =
        byType<BulletInteraction.Entity>().values.any { !it.gunPierce.required }

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

    /**
     * Handles a bullet hitting a block: applies the matching `block_break` rule (breaking
     * or replacing the block), then decides whether the bullet should pierce through.
     * Called from the block-raytrace mixin.
     */
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
            if (hardness < 0.0F || hardness !in interaction.blockBreak.hardness) return@run false
            val tier = interaction.blockBreak.tier
            if (tier != null && state.`is`(tier.material.incorrectBlocksForDrops())) return@run false

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
        var blockBroken = false
        if (breakBlock) run {
            blockBroken = ProtectedBlockBreaking.destroy(
                level,
                blockPos,
                state,
                ammo.getOwner(),
                interaction.blockBreak.drop,
                Block.UPDATE_NEIGHBORS or Block.UPDATE_CLIENTS
            )
            if (!blockBroken) return@run
            val replaceWith = interaction.blockBreak.replaceWith
            if (!replaceWith.state.isAir && replaceWith.place(level, blockPos, Block.UPDATE_CLIENTS)) {
                level.sendBlockUpdated(blockPos, replaceWith.state, replaceWith.state, Block.UPDATE_CLIENTS)
            }
        }
        val pierce = shouldPierce(
            ammo, result.location, interaction.pierce, interaction.gunPierce, blockBroken,
            ext::`tacztweaks$incrementBlockPierce`, ext::`tacztweaks$getBlockPierce`
        )
        if (pierce && !blockBroken && interaction.pierce.renderBulletHole) {
            val bulletHoleOption = BulletHoleOption(
                result.direction,
                blockPos,
                ammo.getAmmoId().toString(),
                ammo.getGunId().toString(),
                ammo.getGunDisplayId().toString()
            )
            level.sendParticles(bulletHoleOption, result.location.x, result.location.y, result.location.z, 1, 0.0, 0.0, 0.0, 0.0)
        }
        return InteractionResult(pierce, blockBroken).also { logDebug { it.toString() } }
    }

    /** Resolve an entity rule once so random predicates cannot change between damage and pierce. */
    fun prepareEntityInteraction(ammo: EntityKineticBullet, location: Vec3, entity: Entity): EntityInteraction {
        val (id, interaction) = getBulletInteraction(ammo, location, BulletInteraction.Entity::entities) {
            it.test(entity)
        } ?: (DEFAULT to BulletInteraction.Entity.DEFAULT)
        logDebug { "Using entity bullet interaction: $id" }
        return EntityInteraction(interaction)
    }

    /**
     * Called after damage. The native 26.2 loop decrements gun pierce immediately after
     * our wrapped onHitEntity returns, so this reports whether to continue and whether that
     * upcoming decrement should be compensated.
     */
    fun finishEntityInteraction(
        ammo: EntityKineticBullet,
        location: Vec3,
        prepared: EntityInteraction,
        condition: Boolean
    ): EntityInteractionResult {
        val interaction = prepared.interaction
        val ext = ammo as EntityKineticBulletExtension
        val remainingAfterNativeConsume = ext.`tacztweaks$getGunPierce`() -
            if (interaction.gunPierce.consume) 1 else 0
        if (interaction.gunPierce.required && remainingAfterNativeConsume <= 0) {
            return EntityInteractionResult(false, interaction.gunPierce.consume)
        }
        val pierce = shouldPierceRule(
            ammo,
            location,
            interaction.pierce,
            condition,
            ext::`tacztweaks$incrementEntityPierce`,
            ext::`tacztweaks$getEntityPierce`
        )
        return EntityInteractionResult(pierce, interaction.gunPierce.consume)
    }

    fun handleShieldInteraction(
        ammo: EntityKineticBullet,
        location: Vec3,
        shield: ItemStack,
        originalDamage: Float
    ): ShieldInteractionResult? {
        val (id, interaction) = getBulletInteraction<BulletInteraction.Shield>(ammo, location) {
            it.predicate.map { predicate -> predicate.test(shield) }.orElse(true)
        } ?: return null
        logDebug { "Using shield bullet interaction: $id" }

        val damage = ((originalDamage - interaction.damage.falloff) * interaction.damage.multiplier)
            .coerceIn(0.0F, originalDamage)
        val durabilityDamage = label@{ durabilityDamage: Int ->
            if (interaction.durability.conditional && damage <= 0) return@label 0
            when (val durability = interaction.durability) {
                is BulletInteraction.Shield.Durability.DynamicDamage ->
                    ((durabilityDamage + durability.modifier) * durability.multiplier).toInt().coerceAtLeast(0)
                is BulletInteraction.Shield.Durability.FixedDamage -> durability.damage.coerceAtLeast(0)
            }
        }
        val disableDuration = run {
            if (interaction.disable.conditional && damage <= 0) return@run 0
            if (ammo.getRandom().nextFloat() < interaction.disable.chance) interaction.disable.duration else 0
        }
        return ShieldInteractionResult(originalDamage - damage, durabilityDamage, disableDuration)
    }

    private fun shouldPierce(
        ammo: EntityKineticBullet,
        location: Vec3,
        pierce: BulletInteraction.Pierce,
        gunPierce: BulletInteraction.GunPierce,
        condition: Boolean,
        incrementCustomPierce: () -> Unit,
        getCustomPierce: () -> Int
    ): Boolean {
        val ext = ammo as EntityKineticBulletExtension
        if (gunPierce.consume) ext.`tacztweaks$decrementGunPierce`()
        if (gunPierce.required && ext.`tacztweaks$getGunPierce`() <= 0) return false
        return shouldPierceRule(
            ammo, location, pierce, condition, incrementCustomPierce, getCustomPierce
        )
    }

    private fun shouldPierceRule(
        ammo: EntityKineticBullet,
        location: Vec3,
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
            is BulletInteraction.Pierce.Damage -> if (ammo.getDamage(location) <= 0.0F) return false
        }
        if (pierce.conditional && !condition) return false
        (ammo as EntityKineticBulletExtension).`tacztweaks$addDamageModifier`(
            -pierce.damageFalloff,
            pierce.damageMultiplier
        )
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

    class EntityInteraction internal constructor(
        internal val interaction: BulletInteraction.Entity
    ) {
        val damageModifier: Float get() = interaction.damage.modifier
        val damageMultiplier: Float get() = interaction.damage.multiplier
    }

    data class EntityInteractionResult(
        val pierce: Boolean,
        val consumeGunPierce: Boolean
    )

    data class InteractionResult(
        val pierce: Boolean,
        val condition: Boolean
    )

    data class ShieldInteractionResult(
        val blockedDamage: Float,
        val durabilityDamage: (Int) -> Int,
        val disableDuration: Int
    )
}
