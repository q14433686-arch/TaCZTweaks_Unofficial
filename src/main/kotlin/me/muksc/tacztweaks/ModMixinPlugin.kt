package me.muksc.tacztweaks

import net.neoforged.fml.loading.FMLLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

/** Prevents optional compatibility targets from being resolved when their mod is absent. */
class ModMixinPlugin : IMixinConfigPlugin {
    override fun onLoad(mixinPackage: String) = Unit
    override fun getRefMapperConfig(): String? = null

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean = when {
        mixinClassName.startsWith("me.muksc.tacztweaks.mixin.compat.soundphysics.") ->
            FMLLoader.getCurrent().getLoadingModList().getModFileById("sound_physics_remastered") != null
        mixinClassName.startsWith("me.muksc.tacztweaks.mixin.compat.firstaid.") ->
            FMLLoader.getCurrent().getLoadingModList().getModFileById("firstaid") != null
        else -> true
    }

    override fun acceptTargets(myTargets: Set<String>, otherTargets: Set<String>) = Unit
    override fun getMixins(): List<String>? = null
    override fun preApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) = Unit
    override fun postApply(targetClassName: String, targetClass: ClassNode, mixinClassName: String, mixinInfo: IMixinInfo) = Unit
}
