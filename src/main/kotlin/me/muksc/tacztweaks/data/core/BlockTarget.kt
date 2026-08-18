package me.muksc.tacztweaks.data.core

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.muksc.tacztweaks.data.codec.DispatchCodec
import me.muksc.tacztweaks.data.codec.dispatchBy
import me.muksc.tacztweaks.id
import net.minecraft.advancements.predicates.BlockPredicate
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ToolMaterial
import net.minecraft.world.level.block.state.BlockState

/**
 * Structured block matcher used by `block`/`melee` bullet interactions.
 * 26.2 moved [BlockPredicate] and replaced Forge's tier registry with vanilla
 * [ToolMaterial] incorrect-block tags; both legacy matcher types remain representable.
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
        PREDICATE("predicate", { Predicate.CODEC }),
        TIER("tier", { HardnessTier.CODEC }),
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

    class Predicate(val predicate: BlockPredicate) : BlockTarget(EBlockTargetType.PREDICATE) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
            predicate.matches(level, pos)

        companion object {
            val CODEC: Codec<Predicate> = RecordCodecBuilder.create<Predicate> { it.group(
                BlockPredicate.CODEC.fieldOf("predicate").forGetter(Predicate::predicate)
            ).apply(it, ::Predicate) }
        }
    }

    class HardnessTier(val tier: TierDefinition) : BlockTarget(EBlockTargetType.TIER) {
        override fun test(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
            !state.`is`(tier.material.incorrectBlocksForDrops())

        companion object {
            val CODEC: Codec<HardnessTier> = RecordCodecBuilder.create<HardnessTier> { it.group(
                TierDefinition.CODEC.fieldOf("tier").forGetter(HardnessTier::tier)
            ).apply(it, ::HardnessTier) }
        }
    }

    data class TierDefinition(val id: Identifier, val material: ToolMaterial) {
        companion object {
            private val VALUES = listOf(
                TierDefinition(Identifier.fromNamespaceAndPath("minecraft", "wood"), ToolMaterial.WOOD),
                TierDefinition(Identifier.fromNamespaceAndPath("minecraft", "stone"), ToolMaterial.STONE),
                TierDefinition(Identifier.fromNamespaceAndPath("minecraft", "copper"), ToolMaterial.COPPER),
                TierDefinition(Identifier.fromNamespaceAndPath("minecraft", "iron"), ToolMaterial.IRON),
                TierDefinition(Identifier.fromNamespaceAndPath("minecraft", "diamond"), ToolMaterial.DIAMOND),
                TierDefinition(Identifier.fromNamespaceAndPath("minecraft", "gold"), ToolMaterial.GOLD),
                TierDefinition(Identifier.fromNamespaceAndPath("minecraft", "netherite"), ToolMaterial.NETHERITE)
            )
            private val BY_ID = VALUES.associateBy(TierDefinition::id)
            val CODEC: Codec<TierDefinition> = Identifier.CODEC.comapFlatMap(
                { id -> BY_ID[id]?.let { DataResult.success(it) } ?: DataResult.error { "Unknown tool tier: $id" } },
                TierDefinition::id
            )
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
