package me.muksc.tacztweaks.core

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.level.BlockEvent

/**
 * Shared permission/event chain for data-driven bullet and melee block destruction.
 *
 * Loader note: Fabric exposes a three-stage chain (`PlayerBlockBreakEvents.BEFORE / CANCELED /
 * AFTER`). NeoForge only has the cancellable [BlockEvent.BreakEvent], so player-owned breaks run
 * `Level#mayInteract` plus that event, and there is **no** "canceled"/"after" notification for
 * protection mods that rely on one. This is a semantic downgrade versus the Fabric build; see
 * docs/KNOWN_ISSUES.md. Non-player projectiles still go straight through `Level#destroyBlock`.
 */
object ProtectedBlockBreaking {
    fun destroy(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        owner: Entity?,
        drop: Boolean,
        updateFlags: Int
    ): Boolean {
        val player = owner as? ServerPlayer
        if (player != null) {
            if (!level.mayInteract(player, pos)) return false
            val event = BlockEvent.BreakEvent(level, pos, state, player)
            NeoForge.EVENT_BUS.post(event)
            if (event.isCanceled) return false
        }

        return level.destroyBlock(pos, drop, owner, updateFlags)
    }
}
