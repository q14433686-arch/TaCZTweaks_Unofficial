package me.muksc.tacztweaks.data.manager

import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.data.MeleeInteraction
import me.muksc.tacztweaks.thenPrioritizeBy

private val COMPARATOR = compareBy<MeleeInteraction> { it.priority }
    .thenPrioritizeBy { it.target.isNotEmpty() }
    .thenPrioritizeBy { when (it) {
        is MeleeInteraction.Block -> it.blocks.isNotEmpty()
    } }

/**
 * Loads `melee_interactions` JSON files (LRTactical knives breaking blocks, …). Behavior
 * is deferred to the behavior-layer round.
 */
object MeleeInteractionManager : BaseDataManager<MeleeInteraction>(
    "melee_interactions", MeleeInteraction.CODEC, COMPARATOR
) {
    override fun debugEnabled(): Boolean = Config.Debug.meleeInteractions()
}
