package me.muksc.tacztweaks.fabric

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import me.muksc.tacztweaks.core.registry.DeferredRegister
import me.muksc.tacztweaks.core.resource.IdentifiableResourceReloadListener
import me.muksc.tacztweaks.platform.Platform
import me.muksc.tacztweaks.platform.PlatformEvents
import me.muksc.tacztweaks.platform.PlatformNetwork
import me.muksc.tacztweaks.registry.ModRegistries
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.registry.DynamicRegistries
//? if >=1.21.9 {
/*import net.fabricmc.fabric.api.resource.v1.ResourceLoader
*///?} else {
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
//?}
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.packs.PackType

object FabricPlatform : Platform {
    override val events: PlatformEvents get() = FabricPlatformEvents
    override val network: PlatformNetwork get() = FabricPlatformNetwork

    override fun registerDeferredRegistries(registries: List<DeferredRegister<*>>) = Unit // Not deferred in Fabric

    @Suppress("UNCHECKED_CAST")
    override fun registerDatapackRegistries(registries: List<ModRegistries.DataPackRegistry<*>>) {
        for (registry in registries) {
            registerDatapackRegistry(registry as ModRegistries.DataPackRegistry<Any>)
        }
    }

    private fun <T : Any> registerDatapackRegistry(registry: ModRegistries.DataPackRegistry<T>) {
        DynamicRegistries.register(registry.key, registry.codec)
    }

    override fun registerReloadListeners(listeners: List<IdentifiableResourceReloadListener>) {
        for (listener in listeners) {
            //? if >=1.21.9 {
            /*ResourceLoader.get(PackType.SERVER_DATA).registerReloader(listener.id(), listener)
            *///?} else {
            ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(listener)
            //?}
        }
    }

    override fun registerCommands(commands: List<LiteralArgumentBuilder<CommandSourceStack>>) {
        CommandRegistrationCallback.EVENT.register { dispatcher, context, environment ->
            for (command in commands) {
                dispatcher.register(command)
            }
        }
    }
}
