package me.muksc.tacztweaks.data.core

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.muksc.tacztweaks.data.codec.DispatchCodec
import me.muksc.tacztweaks.data.codec.dispatchBy
import me.muksc.tacztweaks.id
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.state.BlockState

/**
 * Structured block matcher used by `block`/`melee` bullet interactions.
 * 26.2 note: the Forge-only `tier` type and the 1.20 `predicate` type (removed in 26.2)
 * are dropped from the dispatch.
 */
sealed class BlockTarget(
    val type: EBlockTargetType
) : BlockTestable {
    enum class EBlockTargetType(
        override val key: String,
        override val codecProvider: () -> Codec<out BlockTarget>
    ) : DispatchCodec<BlockTarget> {
        ALL_OF("all_of", { AllOf.CODEC }),
        ANY_OF("any_of", { AnyOf.CODEC }),
        INVERTED("inverted", { Inverted.CODEC }),
        BLOCK("block", { Block.CODEC }),
        BLOCK_TAG("block_tag", { BlockTag.CODEC }),
        REGEX("regex", { RegexPattern.CODEC }),
        HARDNESS("hardness", { Hardness.CODEC });

        companion object {
            private val map = EBlockTargetType.entries.associateBy(EBlockTargetType::key)
            val CODEC = DispatchCodec.getCodec(map::getValue)
        }
    }

    class AllOf(val terms: List<BlockTarget>) : BlockTarget(EBlockTargetType.ALL_OF) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
            terms.all { it.test(level, pos, state) }

        companion object {
            val CODEC: Codec<AllOf> = RecordCodecBuilder.create<AllOf> { it.group(
                Codec.list(BlockTarget.CODEC).fieldOf("terms").forGetter(AllOf::terms)
            ).apply(it, ::AllOf) }
        }
    }

    class AnyOf(val terms: List<BlockTarget>) : BlockTarget(EBlockTargetType.ANY_OF) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
            terms.any { it.test(level, pos, state) }

        companion object {
            val CODEC: Codec<AnyOf> = RecordCodecBuilder.create<AnyOf> { it.group(
                Codec.list(BlockTarget.CODEC).fieldOf("terms").forGetter(AnyOf::terms)
            ).apply(it, ::AnyOf) }
        }
    }

    class Inverted(val term: BlockTarget) : BlockTarget(EBlockTargetType.INVERTED) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean = !term.test(level, pos, state)

        companion object {
            val CODEC: Codec<Inverted> = RecordCodecBuilder.create<Inverted> { it.group(
                BlockTarget.CODEC.fieldOf("term").forGetter(Inverted::term)
            ).apply(it, ::Inverted) }
        }
    }

    class Block(val values: List<net.minecraft.world.level.block.Block>) : BlockTarget(EBlockTargetType.BLOCK) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
            values.any { state.`is`(it) }

        companion object {
            val CODEC: Codec<Block> = RecordCodecBuilder.create<Block> { it.group(
                Codec.list(BuiltInRegistries.BLOCK.byNameCodec()).fieldOf("values").forGetter(Block::values)
            ).apply(it, ::Block) }
        }
    }

    class BlockTag(val values: List<TagKey<net.minecraft.world.level.block.Block>>) : BlockTarget(EBlockTargetType.BLOCK_TAG) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
            values.any { state.`is`(it) }

        companion object {
            val CODEC: Codec<BlockTag> = RecordCodecBuilder.create<BlockTag> { it.group(
                Codec.list(TagKey.hashedCodec(Registries.BLOCK)).fieldOf("values").forGetter(BlockTag::values)
            ).apply(it, ::BlockTag) }
        }
    }

    class RegexPattern(val regex: Regex) : BlockTarget(EBlockTargetType.REGEX) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
            regex.matches(state.block.id.toString())

        companion object {
            val CODEC: Codec<RegexPattern> = RecordCodecBuilder.create<RegexPattern> { it.group(
                Codec.STRING.xmap(::Regex, Regex::pattern).fieldOf("regex").forGetter(RegexPattern::regex)
            ).apply(it, ::RegexPattern) }
        }
    }

    class Hardness(val range: ValueRange) : BlockTarget(EBlockTargetType.HARDNESS) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
            state.getDestroySpeed(level, pos) in range

        companion object {
            val CODEC: Codec<Hardness> = RecordCodecBuilder.create<Hardness> { it.group(
                ValueRange.CODEC.fieldOf("range").forGetter(Hardness::range)
            ).apply(it, ::Hardness) }
        }
    }

    companion object {
        val CODEC: Codec<BlockTarget> = EBlockTargetType.CODEC.dispatchBy(BlockTarget::type) { it.codecProvider() }
    }
}
