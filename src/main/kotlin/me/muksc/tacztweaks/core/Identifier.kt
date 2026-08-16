@file:Suppress("FunctionName")
package me.muksc.tacztweaks.core

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier as MinecraftIdentifier

fun Identifier(namespace: String, path: String): MinecraftIdentifier =
    MinecraftIdentifier.fromNamespaceAndPath(namespace, path)

fun Identifier(path: String): MinecraftIdentifier =
    MinecraftIdentifier.withDefaultNamespace(path)
*///?} else {
import net.minecraft.resources.ResourceLocation

fun Identifier(namespace: String, path: String): ResourceLocation = /*? if <1.21 {*/ ResourceLocation(namespace, path) /*?} else {*/ /*ResourceLocation.fromNamespaceAndPath(namespace, path) *//*?}*/

fun Identifier(path: String): ResourceLocation = /*? if <1.21 {*/ ResourceLocation(path) /*?} else {*/ /*ResourceLocation.withDefaultNamespace(path) *//*?}*/
//?}
