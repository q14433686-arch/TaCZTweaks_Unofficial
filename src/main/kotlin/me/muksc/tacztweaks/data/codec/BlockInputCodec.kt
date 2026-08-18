package me.muksc.tacztweaks.data.codec

import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import net.minecraft.commands.arguments.blocks.BlockInput
import net.minecraft.commands.arguments.blocks.BlockStateParser
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block

/**
 * Codec for `BlockInput` (`replace_with` fields) using the vanilla block-state string
 * format (e.g. `minecraft:stone_bricks`, `minecraft:chest[waterlogged=true]`).
 *
 * 26.2 note: [BlockInput] no longer exposes its NBT tag publicly, so the encode side only
 * serializes the block state (dropping NBT on round-trip — acceptable since the example
 * pack's `replace_with` values carry no NBT).
 */
val BlockInputCodec: Codec<BlockInput> = Codec.STRING.comapFlatMap({
    val result = try {
        @Suppress("DEPRECATION")
        BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK as HolderLookup<Block>, it, true)
    } catch (e: CommandSyntaxException) {
        return@comapFlatMap DataResult.error { e.message ?: e.toString() }
    }
    DataResult.success(BlockInput(result.blockState(), result.properties().keys, result.nbt()))
}, { input ->
    BlockStateParser.serialize(input.getState())
})
