@file:Suppress("FunctionName")
package me.muksc.tacztweaks.core

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
fun Identifier(namespace: String, path: String): ResourceLocation = /*? if <1.21 {*/ ResourceLocation(namespace, path) /*?} else {*/ /*ResourceLocation.fromNamespaceAndPath(namespace, path) *//*?}*/

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
fun Identifier(path: String): ResourceLocation = /*? if <1.21 {*/ ResourceLocation(path) /*?} else {*/ /*ResourceLocation.withDefaultNamespace(path) *//*?}*/