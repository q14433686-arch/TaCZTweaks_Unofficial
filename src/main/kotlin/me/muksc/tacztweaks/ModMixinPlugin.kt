package me.muksc.tacztweaks

import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

/** Prevents optional compatibility targets from being resolved when their mod is absent. */
class ModMixinPlugin : IMixinConfigPlugin {
    override fun onLoad(mixinPackage: String) = Unit
    override fun getRefMapperConfig(): String? = null

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean = when {
        mixinClassName.startsWith("me.muksc.tacztweaks.mixin.compat.soundphysics.") ->
            FabricLoader.getInstance().isModLoaded("sound_physics_remastered")
        mixinClassName.startsWith("me.muksc.tacztweaks.mixin.compat.firstaid.") ->
            FabricLoader.getInstance().isModLoaded("firstaid")
        else -> true
    }

    override fun acceptTargets(myTargets: Set<String>, otherTargets: Set<String>) = Unit
    override fun getMixins(): List<String>? = null
    override fun preApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) = Unit
    override fun postApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) = Unit
}
