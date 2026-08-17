@file:JvmName("DeferredHolderExt")
package me.muksc.tacztweaks.core.extension

import me.muksc.tacztweaks.core.registry.DeferredHolder
//import net.minecraft.core.Holder

fun <R, T : R> DeferredHolder<R, T>.valueOrDelegate(): Holder<R> = getDelegate()
