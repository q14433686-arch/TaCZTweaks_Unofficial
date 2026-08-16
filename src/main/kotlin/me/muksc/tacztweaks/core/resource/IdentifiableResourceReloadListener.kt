package me.muksc.tacztweaks.core.resource

//? if forge || neoforge {
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener

interface IdentifiableResourceReloadListener : PreparableReloadListener {
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    fun id(): ResourceLocation
}
//?} else if fabric {
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
/*import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener as FabricIdentifiableResourceReloadListener

interface IdentifiableResourceReloadListener : FabricIdentifiableResourceReloadListener, PreparableReloadListener {
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    override fun getFabricId(): ResourceLocation = id()

    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    fun id(): ResourceLocation
}
*///?}