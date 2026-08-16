package me.muksc.tacztweaks.core.extension

//? if <1.20.5 {
//~ if >=1.21.11 'net.minecraft.advancements.critereon' -> 'net.minecraft.advancements.criterion'
import net.minecraft.advancements.critereon.ItemPredicate
import net.minecraft.world.item.ItemStack

fun ItemPredicate.test(stack: ItemStack): Boolean = matches(stack)
//?}