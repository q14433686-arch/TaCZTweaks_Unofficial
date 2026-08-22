package me.muksc.tacztweaks

import net.neoforged.fml.loading.FMLLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

/**
 * Prevents optional compatibility targets from being resolved when their mod is absent.
 *
 * Loader note: mixin plugins run before the mod registry exists, so `ModList.get()` is not
 * usable here. NeoForge's equivalent of `FabricLoader#isModLoaded` at this stage is the
 * loading mod list (same pattern as TaCZ: Renovated's own compat mixin plugins).
 */
class ModMixinPlugin : IMixinConfigPlugin {
    override fun onLoad(mixinPackage: String) = Unit

    /** NeoForge has no refmap mechanism (26.2.x is unobfuscated). */
    override fun getRefMapperConfig(): String? = null

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean = when {
        mixinClassName.startsWith("me.muksc.tacztweaks.mixin.compat.soundphysics.") ->
            isModPresent("sound_physics_remastered")
        mixinClassName.startsWith("me.muksc.tacztweaks.mixin.compat.firstaid.") ->
            isModPresent("firstaid")
        else -> true
    }

    private fun isModPresent(modId: String): Boolean =
        FMLLoader.getCurrent().loadingModList.getModFileById(modId) != null

    override fun acceptTargets(myTargets: Set<String>, otherTargets: Set<String>) = Unit
    override fun getMixins(): List<String>? = null
    override fun preApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) = Unit
    override fun postApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) = Unit
}
