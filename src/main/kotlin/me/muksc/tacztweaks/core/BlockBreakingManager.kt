package me.muksc.tacztweaks.core

import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import java.util.Objects.hash

/** Tracks accumulated bullet/melee block-break progress and crack rendering. */
object BlockBreakingManager {
    private val blockBreakProgress = Object2ObjectOpenHashMap<ServerLevel, Long2ObjectMap<Progress>>()
    private const val STALE_TICKS = 400L

    fun addCurrentProgress(level: ServerLevel, pos: BlockPos, delta: Float): Float {
        val progress = blockBreakProgress.computeIfAbsent(level) { Long2ObjectOpenHashMap() }
            .compute(pos.asLong()) { _, value ->
                (value ?: Progress()).apply {
                    this.delta = (this.delta + delta).coerceAtLeast(0.0F)
                    this.lastUpdated = level.gameTime
                }
            }!!

        val stage = if (progress.delta >= 1.0F) {
            blockBreakProgress[level]?.remove(pos.asLong())
            -1
        } else {
            progress.stage
        }
        level.destroyBlockProgress(hash(level, pos), pos, stage)
        return progress.delta
    }

    fun onLevelTick(level: ServerLevel) {
        val progressMap = blockBreakProgress[level] ?: return
        val iterator = progressMap.iterator()
        while (iterator.hasNext()) {
            val (pos, progress) = iterator.next()
            if (level.gameTime < progress.lastUpdated + STALE_TICKS) continue
            level.destroyBlockProgress(hash(level, pos), BlockPos.of(pos), -1)
            iterator.remove()
        }
        if (progressMap.isEmpty()) blockBreakProgress.remove(level)
    }

    fun onBlockBreak(level: ServerLevel, pos: BlockPos) {
        val progressMap = blockBreakProgress[level] ?: return
        progressMap.remove(pos.asLong())
        if (progressMap.isEmpty()) blockBreakProgress.remove(level)
        level.destroyBlockProgress(hash(level, pos), pos, -1)
    }

    class Progress {
        var delta: Float = 0.0F
        var lastUpdated: Long = 0L

        val stage: Int
            get() = if (delta > 0.0F) (delta * 10.0F).toInt().coerceAtMost(9) else -1
    }
}
