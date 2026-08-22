package me.muksc.tacztweaks.core

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.level.BlockEvent

/** 数据驱动的子弹/近战方块破坏共享权限链（NeoForge 版）。 */
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
