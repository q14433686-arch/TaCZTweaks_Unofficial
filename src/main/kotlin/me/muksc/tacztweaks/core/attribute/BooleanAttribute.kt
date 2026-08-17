package me.muksc.tacztweaks.core.attribute

import net.minecraft.core.Holder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute

import me.muksc.tacztweaks.TaCZTweaks
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.entity.ai.attributes.AttributeModifier

class BooleanAttribute(
    descriptionId: String,
    private val defaultValue: () -> Boolean
) : Attribute(descriptionId, if (defaultValue()) 1.0 else 0.0) {
    constructor(descriptionId: String, defaultValue: Boolean) : this(descriptionId, { defaultValue })

    override fun sanitizeValue(value: Double): Double {
        if (value.isNaN()) return 0.0
        return if (value > 0.0) 1.0 else 0.0
    }

    override fun getDefaultValue(): Double = if (defaultValue()) 1.0 else 0.0


    fun toComponent(modifier: AttributeModifier): MutableComponent {
        val value = modifier.amount
        val color = if (value > 0.0) {
            ChatFormatting.BLUE
        } else {
            ChatFormatting.RED
        }
        return TaCZTweaks.translatable(
            "attribute.modifier.bool",
            toValueComponent(modifier.operation, value),
            Component.translatable(descriptionId)
        ).withStyle(color)
    }
}

fun LivingEntity.getBooleanAttributeValue(attribute: Holder<Attribute>): Boolean = getAttributeValue(attribute) > 0

