package me.muksc.tacztweaks.fabric

import me.muksc.tacztweaks.core.resource.IdentifiableResourceReloadListener
import me.muksc.tacztweaks.platform.PlatformClient
import me.muksc.tacztweaks.platform.PlatformEventsClient
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
//? if >=1.21.9 {
/*import net.fabricmc.fabric.api.resource.v1.ResourceLoader
*///?} else {
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
//?}
import net.minecraft.client.KeyMapping
import net.minecraft.server.packs.PackType

object FabricPlatformClient : PlatformClient {
    override val events: PlatformEventsClient get() = FabricPlatformEventsClient

    override fun registerKeyMappings(keys: List<KeyMapping>) {
        for (key in keys) {
            KeyBindingHelper.registerKeyBinding(key)
        }
    }

    override fun registerClientReloadListeners(listeners: List<IdentifiableResourceReloadListener>) {
        for (listener in listeners) {
            //? if >=1.21.9 {
            /*ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(listener.id(), listener)
            *///?} else {
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(listener)
            //?}
        }
    }
}
