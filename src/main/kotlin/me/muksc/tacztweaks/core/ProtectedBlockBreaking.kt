package me.muksc.tacztweaks.core

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.level.block.BreakBlockEvent

/**
 * Shared permission/event chain for data-driven bullet and melee block destruction.
 *
 * Loader note: Fabric exposes a three-stage chain (`PlayerBlockBreakEvents.BEFORE / CANCELED /
 * AFTER`). NeoForge 26.1.x only has the cancellable [BreakBlockEvent]
 * (`net.neoforged.neoforge.event.level.block`, which replaced the old `BlockEvent.BreakEvent`),
 * so player-owned breaks run `Level#mayInteract` plus that event, and there is **no**
 * "canceled"/"after" notification for protection mods that rely on one. This is a semantic
 * downgrade versus the Fabric build; see docs/KNOWN_ISSUES.md. Non-player projectiles still go
 * straight through `Level#destroyBlock`.
 *
 * The event is posted directly rather than through `CommonHooks#fireBlockBreak`, because that
 * helper also pre-cancels based on the held item's `canDestroyBlock` — a bullet impact is not
 * the held item mining the block.
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
            val event = BreakBlockEvent(level, pos, state, player)
            NeoForge.EVENT_BUS.post(event)
            if (event.isCanceled) return false
        }

        return level.destroyBlock(pos, drop, owner, updateFlags)
    }
}
