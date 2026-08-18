package me.muksc.tacztweaks.data.manager

import me.muksc.tacztweaks.anyOrEmpty
import me.muksc.tacztweaks.compat.lrtactical.LRTacticalCompat
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.core.BlockBreakingManager
import me.muksc.tacztweaks.core.Context
import me.muksc.tacztweaks.data.BulletInteraction
import me.muksc.tacztweaks.data.MeleeInteraction
import me.muksc.tacztweaks.data.manager.BulletInteractionManager.calcBlockBreakingDelta
import me.muksc.tacztweaks.thenPrioritizeBy
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.HitResult

private val COMPARATOR = compareBy<MeleeInteraction> { it.priority }
    .thenPrioritizeBy { it.target.isNotEmpty() }
    .thenPrioritizeBy { when (it) {
        is MeleeInteraction.Block -> it.blocks.isNotEmpty()
    } }

/**
 * Loads `melee_interactions` JSON and, when a melee swing misses entities, breaks the
 * looked-at block according to the matching rule (same semantics as upstream).
 */
object MeleeInteractionManager : BaseDataManager<MeleeInteraction>(
    "melee_interactions", MeleeInteraction.CODEC, COMPARATOR
) {
    override fun debugEnabled(): Boolean = Config.Debug.meleeInteractions()

    private inline fun <reified T : MeleeInteraction, E> getMeleeInteraction(
        weaponId: Identifier,
        damage: Float,
        selector: (T) -> List<E>,
        predicate: (E) -> Boolean
    ): Pair<Identifier, T>? = byType<T>().entries.firstOrNull { (_, interaction) ->
        interaction.target.anyOrEmpty { it.test(null, weaponId, damage) }
            && selector(interaction).anyOrEmpty(predicate)
    }?.toPair()

    fun handleBlockInteraction(player: ServerPlayer, reach: Double, damage: Float) {
        val level = player.level()
        val stack = player.mainHandItem
        val weaponId = Context.Gun(stack).id ?: LRTacticalCompat.getWeaponId(stack) ?: return
        val eye = player.eyePosition
        val result = level.clip(ClipContext(
            eye,
            eye.add(player.lookAngle.scale(reach)),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ))
        if (result.type == HitResult.Type.MISS) return

        val blockPos = result.blockPos
        val state = level.getBlockState(blockPos)
        val (id, interaction) = getMeleeInteraction(weaponId, damage, MeleeInteraction.Block::blocks) {
            it.test(level, result.blockPos, state)
        } ?: return
        logDebug { "Using block melee interaction: $id" }

        val breakBlock = run {
            val hardness = state.getDestroySpeed(level, blockPos)
            if (hardness !in interaction.blockBreak.hardness) return@run false
            val tier = interaction.blockBreak.tier
            if (tier != null && state.`is`(tier.material.incorrectBlocksForDrops())) return@run false

            when (interaction.blockBreak) {
                is BulletInteraction.Block.BlockBreak.Never -> false
                is BulletInteraction.Block.BlockBreak.Instant -> true
                is BulletInteraction.Block.BlockBreak.Count -> {
                    val delta = BlockBreakingManager.addCurrentProgress(level, blockPos, 1.0F / interaction.blockBreak.count)
                    delta >= 1.0F
                }
                is BulletInteraction.Block.BlockBreak.FixedDamage -> {
                    var delta = calcBlockBreakingDelta(interaction.blockBreak.damage, 0.0, state, level, blockPos)
                    if (interaction.blockBreak.accumulate) delta = BlockBreakingManager.addCurrentProgress(level, blockPos, delta)
                    delta >= 1.0F
                }
                is BulletInteraction.Block.BlockBreak.DynamicDamage -> {
                    val scaled = (damage + interaction.blockBreak.modifier) * interaction.blockBreak.multiplier
                    var delta = calcBlockBreakingDelta(scaled, 0.0, state, level, blockPos)
                    if (interaction.blockBreak.accumulate) delta = BlockBreakingManager.addCurrentProgress(level, blockPos, delta)
                    delta >= 1.0F
                }
            }
        }
        if (breakBlock) run {
            val blockEntity = level.getBlockEntity(blockPos)
            val allowed = PlayerBlockBreakEvents.BEFORE.invoker()
                .beforeBlockBreak(level, player, blockPos, state, blockEntity)
            if (!allowed) return@run
            if (level.destroyBlock(blockPos, interaction.blockBreak.drop, player, Block.UPDATE_ALL)) {
                PlayerBlockBreakEvents.AFTER.invoker()
                    .afterBlockBreak(level, player, blockPos, state, blockEntity)
            }
        }
    }
}
