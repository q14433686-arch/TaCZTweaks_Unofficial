package me.muksc.tacztweaks.core

import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import java.util.Objects.hash

/**
 * Tracks accumulated block-break progress from bullets (for `count` / `fixed_damage` /
 * `dynamic_damage` break types). Each bullet hit adds a delta; once the total reaches 1.0
 * the block breaks.
 *
 * 1.21.11 note: `Level#gameTime` exists, so stale entries are expired against game time
 * (400 ticks = 20 seconds). (26.x removed `gameTime`, hence the wall-clock fallback there.)
 */
object BlockBreakingManager {
    private val blockBreakProgress = Object2ObjectOpenHashMap<ServerLevel, Long2ObjectMap<Progress>>()

    private const val STALE_TICKS = 400L

    fun addCurrentProgress(level: ServerLevel, pos: BlockPos, delta: Float): Float {
        val progress = blockBreakProgress.computeIfAbsent(level) { Long2ObjectOpenHashMap() }
            .compute(pos.asLong()) { _, value ->
                (value ?: Progress()).apply {
                    this.delta += delta
                    this.lastUpdated = level.gameTime
                }
            }!!

        val stage = run {
            if (progress.delta >= 1.0F) {
                blockBreakProgress[level]?.remove(pos.asLong())
                return@run -1
            }
            progress.stage
        }
        level.destroyBlockProgress(hash(level, pos), pos, stage)
        return progress.delta
    }

    fun onLevelTick(level: ServerLevel) {
        val iterator = blockBreakProgress[level]?.iterator() ?: return
        while (iterator.hasNext()) {
            val (pos, progress) = iterator.next()
            if (level.gameTime < (progress.lastUpdated + STALE_TICKS)) continue
            if (progress.stage > 0) level.destroyBlockProgress(hash(level, pos), BlockPos.of(pos), -1)
            iterator.remove()
        }
    }

    fun onBlockBreak(level: ServerLevel, pos: BlockPos) {
        blockBreakProgress[level]?.remove(pos.asLong())
        level.destroyBlockProgress(hash(level, pos), pos, -1)
    }

    class Progress {
        var delta: Float = 0.0F
        var lastUpdated: Long = 0L

        val stage: Int
            get() = if (delta > 0.0F) (delta * 10.0F).toInt() else -1
    }
}
