package me.muksc.tacztweaks.feature.destroy_progress

import com.mojang.authlib.GameProfile
import me.muksc.tacztweaks.mixininterface.feature.datapack.bullet_interactions.DestroySpeedModifiableBlock
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID
import kotlin.math.exp

import net.fabricmc.fabric.api.entity.FakePlayer

private val FAKE_PROFILE = GameProfile(UUID.fromString("BF8411E4-9730-4215-9AE8-1688EEDF9B72"), "[Minecraft]")

fun calculateDestroyProgressDelta(
    damage: Float,
    armorIgnore: Double,
    level: ServerLevel,
    pos: BlockPos,
    state: BlockState
): Float {
    val ext = DestroySpeedModifiableBlock.of(state.block)
    val player = object : FakePlayer(level, FAKE_PROFILE) {
        override fun getDestroySpeed(state: BlockState): Float =
            super.getDestroySpeed(state) + damage

        @Suppress("OVERRIDE_DEPRECATION")
        override fun hasCorrectToolForDrops(pState: BlockState): Boolean = true
    }
    return try {
        ext.`tacztweaks$setDestroySpeedMultiplier`(remapArmorIgnore(armorIgnore))
        state.getDestroyProgress(player, level, pos)
    } finally {
        ext.`tacztweaks$setDestroySpeedMultiplier`(1.0F)
    }
}

private fun remapArmorIgnore(armorIgnore: Double): Float =
    exp(-2 * armorIgnore).toFloat()
