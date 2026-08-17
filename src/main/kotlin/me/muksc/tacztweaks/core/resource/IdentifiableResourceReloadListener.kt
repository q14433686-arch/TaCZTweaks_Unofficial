package me.muksc.tacztweaks.core.resource

import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener as FabricIdentifiableResourceReloadListener

interface IdentifiableResourceReloadListener : FabricIdentifiableResourceReloadListener, PreparableReloadListener {
    override fun getFabricId(): Identifier = id()

    fun id(): Identifier
}
