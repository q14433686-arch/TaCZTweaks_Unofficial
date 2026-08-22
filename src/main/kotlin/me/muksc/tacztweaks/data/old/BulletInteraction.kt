package me.muksc.tacztweaks.data.old

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.muksc.tacztweaks.data.codec.DispatchCodec
import me.muksc.tacztweaks.data.codec.dispatchBy
import me.muksc.tacztweaks.data.core.BlockOrBlockTag
import net.minecraft.resources.Identifier
import net.minecraft.util.StringRepresentable

/** Legacy v2 bullet_interactions schema retained for load-time conversion tests. */
class BulletInteraction(
    val blocks: List<BlockOrBlockTag>,
    val guns: List<Identifier>,
    val blockBreak: BlockBreak,
    val pierce: Pierce,
    val drop: Boolean
) {
    sealed interface BlockBreak {
        val type: EBlockBreakType

        enum class EBlockBreakType(
            override val key: String,
            override val codecProvider: () -> Codec<out BlockBreak>
        ) : DispatchCodec<BlockBreak> {
            NEVER("never", { Never.CODEC }),
            COUNT("count", { Count.CODEC }),
            FIXED_DAMAGE("fixed_damage", { FixedDamage.CODEC }),
            DYNAMIC_DAMAGE("dynamic_damage", { DynamicDamage.CODEC });

            companion object {
                private val map = entries.associateBy(EBlockBreakType::key)
                val CODEC = DispatchCodec.getCodec(map::getValue)
            }
        }

        class Never : BlockBreak {
            override val type: EBlockBreakType = EBlockBreakType.NEVER

            companion object {
                val CODEC: Codec<Never> = com.mojang.serialization.MapCodec.unitCodec(Never())
            }
        }

        class Count(
            val count: Int
        ) : BlockBreak {
            override val type: EBlockBreakType = EBlockBreakType.COUNT

            companion object {
                val CODEC: Codec<Count> = RecordCodecBuilder.create<Count> { instance -> instance.group(
                    Codec.intRange(1, 32_768).fieldOf("count").forGetter(Count::count)
                ).apply(instance, ::Count) }
            }
        }

        class FixedDamage(
            val damage: Float,
            val accumulate: Boolean
        ) : BlockBreak {
            override val type: EBlockBreakType = EBlockBreakType.FIXED_DAMAGE

            companion object {
                val CODEC: Codec<FixedDamage> = RecordCodecBuilder.create<FixedDamage> { instance -> instance.group(
                    Codec.FLOAT.fieldOf("damage").forGetter(FixedDamage::damage),
                    Codec.BOOL.optionalFieldOf("accumulate", true).forGetter(FixedDamage::accumulate)
                ).apply(instance, ::FixedDamage) }
            }
        }

        class DynamicDamage(
            val modifier: Float,
            val multiplier: Float,
            val accumulate: Boolean
        ) : BlockBreak {
            override val type: EBlockBreakType = EBlockBreakType.DYNAMIC_DAMAGE

            companion object {
                val CODEC: Codec<DynamicDamage> = RecordCodecBuilder.create<DynamicDamage> { instance -> instance.group(
                    Codec.FLOAT.optionalFieldOf("modifier", 0.0F).forGetter(DynamicDamage::modifier),
                    Codec.FLOAT.optionalFieldOf("multiplier", 1.0F).forGetter(DynamicDamage::multiplier),
                    Codec.BOOL.optionalFieldOf("accumulate", true).forGetter(DynamicDamage::accumulate)
                ).apply(instance, ::DynamicDamage) }
            }
        }

        companion object {
            val CODEC: Codec<BlockBreak> = EBlockBreakType.CODEC.dispatchBy(BlockBreak::type) { it.codecProvider() }
        }
    }

    sealed interface Pierce {
        val type: EPierceType

        enum class EPierceType(
            override val key: String,
            override val codecProvider: () -> Codec<out Pierce>
        ) : DispatchCodec<Pierce> {
            NEVER("never", { Never.CODEC }),
            COUNT("count", { Count.CODEC }),
            DAMAGE("damage", { Damage.CODEC });

            companion object {
                private val map = EPierceType.entries.associateBy(EPierceType::key)
                val CODEC = DispatchCodec.getCodec(map::getValue)
            }
        }

        class Never : Pierce {
            override val type: EPierceType = EPierceType.NEVER

            companion object {
                val CODEC: Codec<Never> = com.mojang.serialization.MapCodec.unitCodec(Never())
            }
        }

        class Count(
            val condition: ECondition,
            val count: Int,
            val damageFalloff: Float,
            val damageMultiplier: Float,
            val requireGunPierce: Boolean
        ) : Pierce {
            override val type: EPierceType = EPierceType.COUNT

            companion object {
                val CODEC: Codec<Count> = RecordCodecBuilder.create<Count> { instance -> instance.group(
                    ECondition.CODEC.fieldOf("condition").forGetter(Count::condition),
                    Codec.intRange(1, 32_768).fieldOf("count").forGetter(Count::count),
                    Codec.FLOAT.optionalFieldOf("damage_falloff", 0.0F).forGetter(Count::damageFalloff),
                    Codec.FLOAT.optionalFieldOf("damage_multiplier", 1.0F).forGetter(Count::damageMultiplier),
                    Codec.BOOL.optionalFieldOf("require_gun_pierce", false).forGetter(Count::requireGunPierce)
                ).apply(instance, ::Count) }
            }
        }

        class Damage(
            val condition: ECondition,
            val damageFalloff: Float,
            val damageMultiplier: Float,
            val requireGunPierce: Boolean
        ) : Pierce {
            override val type: EPierceType = EPierceType.DAMAGE

            companion object {
                val CODEC: Codec<Damage> = RecordCodecBuilder.create<Damage> { instance -> instance.group(
                    ECondition.CODEC.fieldOf("condition").forGetter(Damage::condition),
                    Codec.FLOAT.optionalFieldOf("damage_falloff", 0.0F).forGetter(Damage::damageFalloff),
                    Codec.FLOAT.optionalFieldOf("damage_multiplier", 1.0F).forGetter(Damage::damageMultiplier),
                    Codec.BOOL.optionalFieldOf("require_gun_pierce", false).forGetter(Damage::requireGunPierce)
                ).apply(instance, ::Damage) }
            }
        }

        enum class ECondition : StringRepresentable {
            ALWAYS,
            ON_BREAK;

            override fun getSerializedName(): String = name.lowercase()

            companion object {
                val CODEC: Codec<ECondition> = StringRepresentable.fromEnum(::values)
            }
        }

        companion object {
            val CODEC: Codec<Pierce> = EPierceType.CODEC.dispatchBy(Pierce::type) { it.codecProvider() }
        }
    }

    companion object {
        val CODEC: Codec<BulletInteraction> = RecordCodecBuilder.create<BulletInteraction> { instance -> instance.group(
            Codec.list(BlockOrBlockTag.CODEC).fieldOf("blocks").forGetter(BulletInteraction::blocks),
            Codec.list(Identifier.CODEC).optionalFieldOf("guns", emptyList()).forGetter(BulletInteraction::guns),
            BlockBreak.CODEC.fieldOf("block_break").forGetter(BulletInteraction::blockBreak),
            Pierce.CODEC.fieldOf("pierce").forGetter(BulletInteraction::pierce),
            Codec.BOOL.optionalFieldOf("drop", false).forGetter(BulletInteraction::drop)
        ).apply(instance, ::BulletInteraction) }
    }
}
