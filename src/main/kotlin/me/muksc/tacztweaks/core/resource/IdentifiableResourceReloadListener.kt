package me.muksc.tacztweaks.core.resource

//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
//? if fabric && <1.21.9 {
/*import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener as FabricIdentifiableResourceReloadListener
*///?}

interface IdentifiableResourceReloadListener : PreparableReloadListener/*? if fabric && <1.21.9 {*/, FabricIdentifiableResourceReloadListener/*?}*/ {
    //? if fabric && <1.21.9 {
    /*override fun getFabricId(): ResourceLocation = id()
    *///?}

    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    fun id(): ResourceLocation
}
