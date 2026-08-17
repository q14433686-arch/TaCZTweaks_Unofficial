@file:Suppress("FunctionName")
package me.muksc.tacztweaks.core

import net.minecraft.resources.Identifier

// 26.2 versions: MC Identifier's public two-arg constructor was removed in 1.21.x,
// so we wrap the static factories. Same as the existing <1.21 / >=1.21 conditional
// in the shared file, but resolved for 26.2 (no `/*?` blocks here — overlay is 26.2-only).

fun Identifier(namespace: String, path: String): Identifier =
    Identifier.fromNamespaceAndPath(namespace, path)

fun Identifier(path: String): Identifier =
    Identifier.withDefaultNamespace(path)
