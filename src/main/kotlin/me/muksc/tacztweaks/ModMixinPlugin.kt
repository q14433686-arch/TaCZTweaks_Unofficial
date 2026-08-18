package me.muksc.tacztweaks

import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

/**
 * Optional-compat gate. Sound Physics mixins only apply when the Fabric 26.2
 * `sound_physics_remastered` mod is actually loaded — they are string-targeted
 * so they compile without the SPR jar on the classpath.
 */
class ModMixinPlugin : IMixinConfigPlugin {
    override fun onLoad(mixinPackage: String) { /* Nothing */ }

    override fun getRefMapperConfig(): String? = null

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean =
        when {
            mixinClassName.startsWith("me.muksc.tacztweaks.mixin.compat.soundphysics.") ->
                FabricLoader.getInstance().isModLoaded("sound_physics_remastered")
            else -> true
        }

    override fun acceptTargets(myTargets: Set<String>, otherTargets: Set<String>) { /* Nothing */ }

    override fun getMixins(): List<String>? = null

    override fun preApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) { /* Nothing */ }

    override fun postApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) { /* Nothing */ }
}
