package me.muksc.tacztweaks.core

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState

/** Shared permission/event chain for data-driven bullet and melee block destruction. */
object ProtectedBlockBreaking {
    /**
     * Attempts one server-side block break without bypassing vanilla interaction checks or
     * Fabric protection listeners. Non-player projectiles still use Level.destroyBlock,
     * but player-owned actions additionally run mayInteract and the complete event chain.
     */
    fun destroy(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        owner: Entity?,
        drop: Boolean,
        updateFlags: Int
    ): Boolean {
        val player = owner as? ServerPlayer
        val blockEntity = level.getBlockEntity(pos)
        if (player != null) {
            val permitted = level.mayInteract(player, pos) &&
                PlayerBlockBreakEvents.BEFORE.invoker()
                    .beforeBlockBreak(level, player, pos, state, blockEntity)
            if (!permitted) {
                PlayerBlockBreakEvents.CANCELED.invoker()
                    .onBlockBreakCanceled(level, player, pos, state, blockEntity)
                return false
            }
        }

        val destroyed = level.destroyBlock(pos, drop, owner, updateFlags)
        if (!destroyed) {
            if (player != null) {
                PlayerBlockBreakEvents.CANCELED.invoker()
                    .onBlockBreakCanceled(level, player, pos, state, blockEntity)
            }
            return false
        }

        if (player != null) {
            PlayerBlockBreakEvents.AFTER.invoker()
                .afterBlockBreak(level, player, pos, state, blockEntity)
        }
        return true
    }
}
